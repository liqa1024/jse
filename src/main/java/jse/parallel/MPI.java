package jse.parallel;

import jse.cache.ByteArrayCache;
import jse.clib.JNIUtil;
import jse.clib.MiMalloc;
import jse.code.OS;
import jse.code.UT;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jse.clib.JNIUtil.*;
import static jse.code.OS.*;
import static jse.code.CS.VERSION;
import static jse.code.CS.ZL_STR;
import static jse.code.Conf.*;

/**
 * 基于 jni 实现的 MPI wrapper, 介绍部分基于
 * <a href="https://learn.microsoft.com/message-passing-interface/microsoft-mpi">
 * Microsoft MPI </a> 的标准；
 * 所有函数名称按照原始的 MPI 标准而不是流行的 java binding 中使用的标准，
 * 从而保证对 c 风格的 MPI 有更好的一致性；
 * 在此基础上提供一套完全按照 java 风格的接口来方便使用（而不是原本的不伦不类的风格）。
 * <p>
 * 为了保证接口简洁，这里不再返回错误码，并且暂时不进行错误抛出；
 * 为了避免错误并且加速上线，这里不实现我还没理解的功能
 * <p>
 * 使用方法：
 * <pre> {@code
 * import static jse.parallel.MPI.Native.*
 *
 * MPI_Init(args)
 * int me = MPI_Comm_rank(MPI_COMM_WORLD)
 * println('Hi from <'+me+'>')
 * MPI_Finalize()
 * } </pre>
 * <p>
 * 或者更加 java 风格的使用：
 * <pre> {@code
 * import jse.parallel.MPI
 *
 * MPI.init(args)
 * int me = MPI.Comm.WORLD.rank()
 * println('Hi from <'+me+'>')
 * MPI.shutdown() // `finalize()` has been used in java
 * } </pre>
 * <p>
 * References:
 * <a href="https://docs.open-mpi.org/en/v5.0.x/features/java.html">
 * Open MPI Java bindings </a>,
 * <a href="http://www.mpjexpress.org/">
 * MPJ Express Project </a>,
 * <a href="https://www.mpi-forum.org/docs/mpi-3.1/mpi31-report.pdf">
 * MPI: A Message-Passing Interface Standard Version 3.1 </a>
 * @author liqa
 */
public class MPI {
    private MPI() {}
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            // 此常量由于需要在 initThread 中使用，一定会在 MPI.init 之前就需要初始化，因此手动调用一定可以实现初始化
            if (!INITIALIZED) String.valueOf(Native.MPI_THREAD_SINGLE);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 mpijni 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_MPI"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_MPI", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_MPI"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_MPI"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 mpijni，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_MPI", jse.code.Conf.USE_MIMALLOC);
        
        /** 重定向 mpijni 动态库的路径，用于自定义编译这个库的过程，或者重新实现 mpijni 的接口 */
        public static @Nullable String REDIRECT_MPIJNI_LIB = OS.env("JSE_REDIRECT_MPIJNI_LIB");
    }
    
    public static String libraryVersion() throws MPIException {return MPI.Native.MPI_Get_library_version();}
    
    public static final int UNDEFINED = Native.MPI_UNDEFINED;
    
    public static class Group implements IAutoShutdown {
        public final static Group
          NULL  = new Group(Native.MPI_GROUP_NULL )
        , EMPTY = new Group(Native.MPI_GROUP_EMPTY)
        ;
        @ApiStatus.Internal public static Group of(long aPtr) {
            if (aPtr == Native.MPI_GROUP_NULL ) return NULL ;
            if (aPtr == Native.MPI_GROUP_EMPTY) return EMPTY;
            return new Group(aPtr);
        }
        
        private long mPtr;
        private Group(long aPtr) {mPtr = aPtr;}
        @ApiStatus.Internal public long ptr_() {return mPtr;}
        
        
        /**
         * @return The rank of the calling process in the specified group.
         * A value of {@link #UNDEFINED} that the calling process is not a member of the specified group.
         */
        public int rank() throws MPIException {return Native.MPI_Group_rank(mPtr);}
        /**
         * @return The size of the specified group.
         */
        public int size() throws MPIException {return Native.MPI_Group_size(mPtr);}
        
        /** Free the group. */
        @Override public void shutdown() {
            if (mPtr != Native.MPI_GROUP_NULL) {
                try {Native.MPI_Group_free(mPtr);} catch (MPIException ignored) {}
                mPtr = Native.MPI_GROUP_NULL;
            }
        }
        
        
        /// MPI Group Functions
        /**
         * Creates a new group from the difference between two existing groups.
         * @param aRHS The second group.
         * @return A new {@link MPI.Group} that contains all elements in the first group that
         * are not present in the second group.
         * The function returns {@link MPI.Group#EMPTY} if the new group is empty.
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-difference-function"> MPI_Group_difference function </a>
         */
        public Group difference(Group aRHS) throws MPIException {return of(Native.MPI_Group_difference(mPtr, aRHS.mPtr));}
        
        /**
         * A group constructor that is used to define a new group by deleting ranks from an existing group.
         *
         * @param aN The number of elements in the ranks parameter.
         *
         * @param aRanks The arrays of processes in group that are not to appear in newgroup.
         *               The specified ranks must be valid in the existing group.
         *               Each element in the array must be distinct.
         *               If the array is empty then the new group will be identical to the existing group.
         *
         * @return A new {@link MPI.Group} that is derived from the existing group.
         * The order of the existing group is preserved in the new group.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-excl-function"> MPI_Group_excl function </a>
         */
        public Group excl(int aN, int[] aRanks) throws MPIException {return of(Native.MPI_Group_excl(mPtr, aN, aRanks));}
        public Group excl(int[] aRanks) throws MPIException {return excl(aRanks.length, aRanks);}
        
        /**
         * Creates a new group that contains a subset of the processes in an existing group.
         *
         * @param aN The number of elements in the ranks parameter and the size of the new group.
         *
         * @param aRanks The processes to be included in the new group.
         *
         * @return A new {@link MPI.Group}, which contains the included
         * processes in the order that they are specified in the ranks parameter.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-incl-function"> MPI_Group_incl function </a>
         */
        public Group incl(int aN, int[] aRanks) throws MPIException {return of(Native.MPI_Group_incl(mPtr, aN, aRanks));}
        public Group incl(int[] aRanks) throws MPIException {return incl(aRanks.length, aRanks);}
        
        /**
         * Creates a new group from the intersection of two existing groups.
         * @param aRHS The second group.
         * @return A new {@link MPI.Group} with those elements that are present in both groups.
         * The function returns {@link MPI.Group#EMPTY} if the new group is empty.
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-intersection-function"> MPI_Group_intersection function </a>
         */
        public Group intersection(Group aRHS) throws MPIException {return of(Native.MPI_Group_intersection(mPtr, aRHS.mPtr));}
        
        /**
         * Creates a new group from the union of two existing groups.
         * @param aRHS The second group.
         * @return A new {@link MPI.Group} that represents all elements in either group.
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-union-function"> MPI_Group_union function </a>
         */
        public Group union(Group aRHS) throws MPIException {return of(Native.MPI_Group_union(mPtr, aRHS.mPtr));}
    }
    
    public static class Comm implements IAutoShutdown {
        public final static Comm
          NULL  = new Comm(Native.MPI_COMM_NULL )
        , WORLD = new Comm(Native.MPI_COMM_WORLD)
        , SELF  = new Comm(Native.MPI_COMM_SELF )
        ;
        @ApiStatus.Internal public static Comm of(long aPtr) {
            if (aPtr==0 || aPtr==-1) return null;
            if (aPtr == Native.MPI_COMM_NULL ) return NULL ;
            if (aPtr == Native.MPI_COMM_WORLD) return WORLD;
            if (aPtr == Native.MPI_COMM_SELF ) return SELF ;
            return new Comm(aPtr);
        }
        
        private long mPtr;
        private Comm(long aPtr) {mPtr = aPtr;}
        @ApiStatus.Internal public long ptr_() {return mPtr;}
        
        /** @return the number of calling process within the group of the communicator. */
        public int rank() throws MPIException {return Native.MPI_Comm_rank(mPtr);}
        /** @return the number of processes in the group for the communicator. */
        public int size() throws MPIException {return Native.MPI_Comm_size(mPtr);}
        
        /** Duplicate the communicator. */
        public Comm copy() throws MPIException {return of(Native.MPI_Comm_dup(mPtr));}
        
        /**
         * Frees the communicator that is allocated with the {@link MPI.Comm#copy}, {@link MPI.Comm#create},
         * or {@link MPI.Comm#split} functions.
         */
        @Override public void shutdown() {
            if (mPtr != Native.MPI_COMM_NULL) {
                try {Native.MPI_Comm_free(mPtr);} catch (MPIException ignored) {}
                mPtr = Native.MPI_COMM_NULL;
            }
        }
        
        
        /// MPI Collective Functions
        /**
         * Gathers data from all members of a group and sends the data to all members of the group.
         * The allgather function is similar to the {@link MPI.Comm#gather} function, except that it sends
         * the data to all processes instead of only to the root. The usage rules for allgather
         * correspond to the rules for {@link MPI.Comm#gather}.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer are specified in the aSendCount and
         *                 the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCount parameters and the data type will be
         *                 detected automatically.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgather-function"> MPI_Allgather function </a>
         */
        public void allgather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(double[]  aSendBuf, double[]  rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(char[]    aSendBuf, char[]    rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(short[]   aSendBuf, short[]   rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(int[]     aSendBuf, int[]     rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(long[]    aSendBuf, long[]    rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(float[]   aSendBuf, float[]   rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(byte[]    rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(double[]  rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(boolean[] rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(char[]    rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(short[]   rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(int[]     rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(long[]    rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(float[]   rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        
        /**
         * Gathers a variable amount of data from each member of a group and sends the data to all members of the group.
         * The allgatherv function is like the {@link MPI.Comm#gatherv}, except that all processes receive the result,
         * instead of just the root. The block of data that is sent from the jth process is received by every process
         * and placed in the jth block of the buffer rRecvBuf. These blocks do not all have to be the same size.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer are specified in the aSendCount and
         *                 the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCounts parameters and the data type will be
         *                 detected automatically.
         *
         * @param aRecvCounts The number of data elements from each communicator process in the receive buffer.
         *
         * @param aDispls The location, relative to the recvbuf parameter, of the data from each communicator process.
         *                <p>
         *                In the rRecvBuf, aRecvCounts, and aDispls parameter arrays, the nth element of each
         *                array refers to the data that is received from the nth communicator process.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgatherv-function"> MPI_Allgatherv function </a>
         */
        public void allgatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(byte[]    rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(double[]  rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(boolean[] rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(char[]    rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(short[]   rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(int[]     rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(long[]    rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(float[]   rBuf, int[] aCounts, int[] aDispls) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        
        /**
         * Combines values from all processes and distributes the result back to all processes.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer are specified in the aSendCount and
         *                 the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCounts parameters and the data type will be
         *                 detected automatically.
         *
         * @param aCount The number of elements to send from this process.
         *
         * @param aOp The MPI_Op handle indicating the global reduction operation to perform.
         *            The handle can indicate a built-in or application-defined operation.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allreduce-function"> MPI_Allreduce function </a>
         */
        public void allreduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(byte[]    rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(double[]  rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(boolean[] rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(char[]    rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(short[]   rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(int[]     rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(long[]    rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(float[]   rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public byte    allreduceB(byte    aB, Op aOp) throws MPIException {return Native.MPI_AllreduceB(aB, aOp.mPtr, mPtr);}
        public double  allreduceD(double  aD, Op aOp) throws MPIException {return Native.MPI_AllreduceD(aD, aOp.mPtr, mPtr);}
        public boolean allreduceZ(boolean aZ, Op aOp) throws MPIException {return Native.MPI_AllreduceZ(aZ, aOp.mPtr, mPtr);}
        public char    allreduceC(char    aC, Op aOp) throws MPIException {return Native.MPI_AllreduceC(aC, aOp.mPtr, mPtr);}
        public short   allreduceS(short   aS, Op aOp) throws MPIException {return Native.MPI_AllreduceS(aS, aOp.mPtr, mPtr);}
        public int     allreduceI(int     aI, Op aOp) throws MPIException {return Native.MPI_AllreduceI(aI, aOp.mPtr, mPtr);}
        public long    allreduceL(long    aL, Op aOp) throws MPIException {return Native.MPI_AllreduceL(aL, aOp.mPtr, mPtr);}
        public float   allreduceF(float   aF, Op aOp) throws MPIException {return Native.MPI_AllreduceF(aF, aOp.mPtr, mPtr);}
        
        /**
         * Initiates barrier synchronization across all members of a group.
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-barrier-function"> MPI_Barrier function </a>
         */
        public void barrier() throws MPIException {
            Native.MPI_Barrier(mPtr);
        }
        
        /**
         * Broadcasts data from one member of a group to all members of the group.
         *
         * @param rBuf The data array. On the process that is specified by the root parameter,
         *             the buffer contains the data to be broadcast. On all other processes
         *             in the communicator that is specified by the comm parameter,
         *             the buffer receives the data broadcast by the root process.
         *
         * @param aCount The number of data elements in the buffer.
         *               If the count parameter is zero, the data part of the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-bcast-function"> MPI_Bcast function </a>
         */
        public void bcast(byte[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(double[]  rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(boolean[] rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(char[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(short[]   rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(int[]     rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(long[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(float[]   rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public byte    bcastB(byte    aB, int aRoot) throws MPIException {return Native.MPI_BcastB(aB, aRoot, mPtr);}
        public double  bcastD(double  aD, int aRoot) throws MPIException {return Native.MPI_BcastD(aD, aRoot, mPtr);}
        public boolean bcastZ(boolean aZ, int aRoot) throws MPIException {return Native.MPI_BcastZ(aZ, aRoot, mPtr);}
        public char    bcastC(char    aC, int aRoot) throws MPIException {return Native.MPI_BcastC(aC, aRoot, mPtr);}
        public short   bcastS(short   aS, int aRoot) throws MPIException {return Native.MPI_BcastS(aS, aRoot, mPtr);}
        public int     bcastI(int     aI, int aRoot) throws MPIException {return Native.MPI_BcastI(aI, aRoot, mPtr);}
        public long    bcastL(long    aL, int aRoot) throws MPIException {return Native.MPI_BcastL(aL, aRoot, mPtr);}
        public float   bcastF(float   aF, int aRoot) throws MPIException {return Native.MPI_BcastF(aF, aRoot, mPtr);}
        public String bcastStr(String aStr, int aRoot) throws MPIException {
            if (rank() == aRoot) {
                // java 中至今没有提供将 String 内容输出到已有的 byte[] 中的方法，因此这里不能使用缓存加速；
                // 当然这也让实现更加简洁了
                byte[] tBytes = UT.Serial.str2bytes(aStr);
                bcastI(tBytes.length, aRoot);
                bcast(tBytes, tBytes.length, aRoot);
                return aStr;
            } else {
                final int tLen = bcastI(-1, aRoot);
                byte[] rBytes = ByteArrayCache.getArray(tLen);
                try {
                    bcast(rBytes, tLen, aRoot);
                    return UT.Serial.bytes2str(rBytes, 0, tLen);
                } finally {
                    ByteArrayCache.returnArray(rBytes);
                }
            }
        }
        /** 提供内部类型的支持，统一进行类型优化（例如后续对 shift 的支持）*/
        public void bcast(IVector rVector, int aRoot) throws MPIException {
            final boolean tIsRoot = (rank() == aRoot);
            Vector rBuf = rVector.toBuf(!tIsRoot); // 不是 root 则只需要写入，原本数据不用读取
            try {bcast(rBuf.internalData(), rBuf.internalDataSize(), aRoot);}
            finally {rVector.releaseBuf(rBuf, tIsRoot);} // 是 root 则只需要读取，不用写入到原本数据
        }
        public void bcast(IIntVector rVector, int aRoot) throws MPIException {
            final boolean tIsRoot = (rank() == aRoot);
            IntVector rBuf = rVector.toBuf(!tIsRoot); // 不是 root 则只需要写入，原本数据不用读取
            try {bcast(rBuf.internalData(), rBuf.internalDataSize(), aRoot);}
            finally {rVector.releaseBuf(rBuf, tIsRoot);} // 是 root 则只需要读取，不用写入到原本数据
        }
        
        /**
         * Gathers data from all members of a group to one member.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gather-function"> MPI_Gather function </a>
         */
        public void gather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(double[]  aSendBuf, double[]  rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(char[]    aSendBuf, char[]    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(short[]   aSendBuf, short[]   rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(int[]     aSendBuf, int[]     rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(long[]    aSendBuf, long[]    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(float[]   aSendBuf, float[]   rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(byte[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(double[]  rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(boolean[] rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(char[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(short[]   rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(int[]     rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(long[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(float[]   rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        
        /**
         * Gathers variable data from all members of a group to one member.
         * The gatherv function adds flexibility to the {@link MPI.Comm#gather} function by
         * allowing a varying count of data from each process.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aRecvCounts The number of elements that is received from each process. Each element in the array
         *                    corresponds to the rank of the sending process. If the count is zero, the data part of
         *                    the message is empty. This parameter is significant only at the root process.
         *
         * @param aDispls The location, relative to the recvbuf parameter, of the data from each communicator process.
         *                The data that is received from process j is placed into the receive buffer of the root
         *                process offset displs[j] elements from the sendbuf pointer.
         *                <p>
         *                In the recvbuf, recvcounts, and displs parameter arrays, the nth element of each array
         *                refers to the data that is received from the nth communicator process.
         *                <p>
         *                This parameter is significant only at the root process.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gatherv-function"> MPI_Gatherv function </a>
         */
        public void gatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        
        /**
         * Performs a global reduce operation across all members of a group. You can specify
         * a predefined mathematical or logical operation or an application-defined operation.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param rRecvBuf The data array to receive the result of the reduction operation.
         *                 This parameter is significant only at the root process.
         *
         * @param aCount The number of elements to send from this process.
         *
         * @param aOp The MPI_Op handle indicating the global reduction operation to perform.
         *            The handle can indicate a built-in or application-defined operation.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-reduce-function"> MPI_Reduce function </a>
         */
        public void reduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(byte[]    rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(double[]  rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(boolean[] rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(char[]    rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(short[]   rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(int[]     rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(long[]    rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(float[]   rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public byte    reduceB(byte    aB, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceB(aB, aOp.mPtr, aRoot, mPtr);}
        public double  reduceD(double  aD, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceD(aD, aOp.mPtr, aRoot, mPtr);}
        public boolean reduceZ(boolean aZ, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceZ(aZ, aOp.mPtr, aRoot, mPtr);}
        public char    reduceC(char    aC, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceC(aC, aOp.mPtr, aRoot, mPtr);}
        public short   reduceS(short   aS, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceS(aS, aOp.mPtr, aRoot, mPtr);}
        public int     reduceI(int     aI, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceI(aI, aOp.mPtr, aRoot, mPtr);}
        public long    reduceL(long    aL, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceL(aL, aOp.mPtr, aRoot, mPtr);}
        public float   reduceF(float   aF, Op aOp, int aRoot) throws MPIException {return Native.MPI_ReduceF(aF, aOp.mPtr, aRoot, mPtr);}
        
        /**
         * Scatters data from one member across all members of a group.
         * The MPI_Scatter function performs the inverse of the operation
         * that is performed by the {@link #gather} function.
         *
         * @param aSendBuf The data array to be sent by the root process.
         *                 <p>
         *                 The aSendBuf parameter is ignored for all non-root processes.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *                   <p>
         *                   The aSendCount parameter is ignored for all non-root processes.
         *
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @param aRoot The rank of the sending process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatter-function"> MPI_Scatter function </a>
         */
        public void scatter(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(double[]  aSendBuf, double[]  rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(char[]    aSendBuf, char[]    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(short[]   aSendBuf, short[]   rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(int[]     aSendBuf, int[]     rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(long[]    aSendBuf, long[]    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(float[]   aSendBuf, float[]   rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(byte[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(double[]  rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(boolean[] rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(char[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(short[]   rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(int[]     rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(long[]    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(float[]   rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        
        /**
         * Scatters data from one member across all members of a group.
         * The MPI_Scatterv function performs the inverse of the operation
         * that is performed by the {@link #gatherv} function.
         *
         * @param aSendBuf The data array to be sent by the root process.
         *                 <p>
         *                 The aSendBuf parameter is ignored for all non-root processes.
         *
         * @param aSendCounts The number of elements in the buffer that is specified in the sendbuf parameter.
         *                    If sendcount[i] is zero, the data part of the message for that process is empty.
         *                    <p>
         *                    The aSendCount parameter is ignored for all non-root processes.
         *
         * @param aDispls The locations of the data to send to each communicator process.
         *                Each location in the array is relative to the corresponding element of the sendbuf array.
         *                <p>
         *                In the sendbuf, sendcounts, and displs parameter arrays, the nth element of
         *                each array refers to the data to be sent to the nth communicator process.
         *                <p>
         *                This parameter is significant only at the root process.
         *
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatterv-function"> MPI_Scatterv function </a>
         */
        public void scatterv(byte[]    aSendBuf, int[] aSendCounts, int[] aDispls, byte[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(double[]  aSendBuf, int[] aSendCounts, int[] aDispls, double[]  rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(boolean[] aSendBuf, int[] aSendCounts, int[] aDispls, boolean[] rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(char[]    aSendBuf, int[] aSendCounts, int[] aDispls, char[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(short[]   aSendBuf, int[] aSendCounts, int[] aDispls, short[]   rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(int[]     aSendBuf, int[] aSendCounts, int[] aDispls, int[]     rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(long[]    aSendBuf, int[] aSendCounts, int[] aDispls, long[]    rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(float[]   aSendBuf, int[] aSendCounts, int[] aDispls, float[]   rRecvBuf, int aRecvCount, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        
        
        /// MPI Communicator Functions
        /**
         * Extracts a subset a group of processes for the purpose of separate Multiple Instruction
         * Multiple Data (MIMD) computation in a separate communicator.
         *
         * @param aGroup The group that defines the requested subset of the processes in the
         *               source communicator.
         * @return A new {@link MPI.Comm}.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-create-function"> MPI_Comm_create function </a>
         */
        public Comm create(Group aGroup) throws MPIException {return of(Native.MPI_Comm_create(mPtr, aGroup.mPtr));}
        
        /**
         * Partitions the group that is associated with the specified communicator into
         * a specified number of disjoint subgroups.
         *
         * @param aColor The new communicator that the calling process is to be assigned to.
         *               The value of color must be non-negative.
         *               <p>
         *               If a process specifies the color value {@link #UNDEFINED},
         *               the function returns {@link MPI.Comm#NULL} in the newcomm parameter to the calling process.
         *
         * @param aKey The relative rank of the calling process in the group of the new communicator.
         *             For details on using the key and color parameters, see Remarks.
         *
         * @return A new {@link MPI.Comm}.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-split-function"> MPI_Comm_split function </a>
         */
        public Comm split(int aColor, int aKey) throws MPIException {return of(Native.MPI_Comm_split(mPtr, aColor, aKey));}
        public Comm split(int aColor) throws MPIException {return split(aColor, rank());}
        
        
        /// MPI Group Functions
        /**
         * Retrieves the group that is associated with a communicator.
         * @return The new {@link MPI.Group} that is associated with the specified communicator.
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-group-function"> MPI_Comm_group function </a>
         */
        public Group group() throws MPIException {return Group.of(Native.MPI_Comm_group(mPtr));}
        
        
        /// MPI Point to Point Functions
        /**
         * Performs a standard mode send operation and returns when the send buffer can be safely reused.
         *
         * @param aBuf The data array to be sent
         *
         * @param aCount The number of elements in the buffer. If the data part of the message
         *               is empty, set the count parameter to 0.
         *
         * @param aDest The rank of the destination process within the communicator that is
         *              specified by the comm parameter.
         *
         * @param aTag The message tag that can be used to distinguish different types of messages.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-send-function"> MPI_Send function </a>
         */
        public void send(byte[]    aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(double[]  aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(boolean[] aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(char[]    aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(short[]   aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(int[]     aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(long[]    aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(float[]   aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(byte[]    aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(double[]  aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(boolean[] aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(char[]    aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(short[]   aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(int[]     aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(long[]    aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(float[]   aBuf, int aCount, int aDest) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(int aDest, int aTag) throws MPIException {Native.MPI_Send(aDest, aTag, mPtr);}
        public void send(int aDest) throws MPIException {Native.MPI_Send(aDest, 0, mPtr);}
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public void sendB(byte    aB, int aDest, int aTag) throws MPIException {Native.MPI_SendB(aB, aDest, aTag, mPtr);}
        public void sendD(double  aD, int aDest, int aTag) throws MPIException {Native.MPI_SendD(aD, aDest, aTag, mPtr);}
        public void sendZ(boolean aZ, int aDest, int aTag) throws MPIException {Native.MPI_SendZ(aZ, aDest, aTag, mPtr);}
        public void sendC(char    aC, int aDest, int aTag) throws MPIException {Native.MPI_SendC(aC, aDest, aTag, mPtr);}
        public void sendS(short   aS, int aDest, int aTag) throws MPIException {Native.MPI_SendS(aS, aDest, aTag, mPtr);}
        public void sendI(int     aI, int aDest, int aTag) throws MPIException {Native.MPI_SendI(aI, aDest, aTag, mPtr);}
        public void sendL(long    aL, int aDest, int aTag) throws MPIException {Native.MPI_SendL(aL, aDest, aTag, mPtr);}
        public void sendF(float   aF, int aDest, int aTag) throws MPIException {Native.MPI_SendF(aF, aDest, aTag, mPtr);}
        public void sendStr(String aStr, int aDest, int aTag) throws MPIException {
            // java 中至今没有提供将 String 内容输出到已有的 byte[] 中的方法，因此这里不能使用缓存加速；
            // 当然这也让实现更加简洁了
            byte[] tBytes = UT.Serial.str2bytes(aStr);
            sendI(tBytes.length, aDest, aTag);
            send(tBytes, tBytes.length, aDest, aTag);
        }
        public void sendB(byte    aB, int aDest) throws MPIException {Native.MPI_SendB(aB, aDest, 0, mPtr);}
        public void sendD(double  aD, int aDest) throws MPIException {Native.MPI_SendD(aD, aDest, 0, mPtr);}
        public void sendZ(boolean aZ, int aDest) throws MPIException {Native.MPI_SendZ(aZ, aDest, 0, mPtr);}
        public void sendC(char    aC, int aDest) throws MPIException {Native.MPI_SendC(aC, aDest, 0, mPtr);}
        public void sendS(short   aS, int aDest) throws MPIException {Native.MPI_SendS(aS, aDest, 0, mPtr);}
        public void sendI(int     aI, int aDest) throws MPIException {Native.MPI_SendI(aI, aDest, 0, mPtr);}
        public void sendL(long    aL, int aDest) throws MPIException {Native.MPI_SendL(aL, aDest, 0, mPtr);}
        public void sendF(float   aF, int aDest) throws MPIException {Native.MPI_SendF(aF, aDest, 0, mPtr);}
        public void sendStr(String aStr, int aDest) throws MPIException {sendStr(aStr, aDest, 0);}
        /** 提供内部类型的支持，统一进行类型优化（例如后续对 shift 的支持）*/
        public void send(IVector aVector, int aDest, int aTag) throws MPIException {
            Vector tBuf = aVector.toBuf();
            try {send(tBuf.internalData(), tBuf.internalDataSize(), aDest, aTag);}
            finally {aVector.releaseBuf(tBuf, true);} // send 只需要读取数据
        }
        public void send(IIntVector aVector, int aDest, int aTag) throws MPIException {
            IntVector tBuf = aVector.toBuf();
            try {send(tBuf.internalData(), tBuf.internalDataSize(), aDest, aTag);}
            finally {aVector.releaseBuf(tBuf, true);} // send 只需要读取数据
        }
        public void send(IVector aVector, int aDest) throws MPIException {send(aVector, aDest, 0);}
        public void send(IIntVector aVector, int aDest) throws MPIException {send(aVector, aDest, 0);}
        
        /**
         * Performs a receive operation and does not return until a matching message is received.
         *
         * @param rBuf The data array to be received
         *
         * @param aCount The number of elements in the buffer. If the data part of the message
         *               is empty, set the count parameter to 0.
         *
         * @param aSource The rank of the sending process within the specified communicator.
         *                Specify the {@link MPI.Rank#ANY} constant to specify that any source
         *                is acceptable.
         *
         * @param aTag The message tag that is used to distinguish different types of messages.
         *             Specify the {@link MPI.Tag#ANY} constant to indicate that any tag is
         *             acceptable.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-recv-function"> MPI_Recv function </a>
         */
        public void recv(byte[]    rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(double[]  rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(boolean[] rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(char[]    rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(short[]   rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(int[]     rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(long[]    rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(float[]   rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(byte[]    rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(double[]  rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(boolean[] rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(char[]    rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(short[]   rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(int[]     rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(long[]    rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(float[]   rBuf, int aCount, int aSource) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(int aSource, int aTag) throws MPIException {Native.MPI_Recv(aSource, aTag, mPtr);}
        public void recv(int aSource) throws MPIException {Native.MPI_Recv(aSource, Tag.ANY, mPtr);}
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public byte    recvB(int aSource, int aTag) throws MPIException {return Native.MPI_RecvB(aSource, aTag, mPtr);}
        public double  recvD(int aSource, int aTag) throws MPIException {return Native.MPI_RecvD(aSource, aTag, mPtr);}
        public boolean recvZ(int aSource, int aTag) throws MPIException {return Native.MPI_RecvZ(aSource, aTag, mPtr);}
        public char    recvC(int aSource, int aTag) throws MPIException {return Native.MPI_RecvC(aSource, aTag, mPtr);}
        public short   recvS(int aSource, int aTag) throws MPIException {return Native.MPI_RecvS(aSource, aTag, mPtr);}
        public int     recvI(int aSource, int aTag) throws MPIException {return Native.MPI_RecvI(aSource, aTag, mPtr);}
        public long    recvL(int aSource, int aTag) throws MPIException {return Native.MPI_RecvL(aSource, aTag, mPtr);}
        public float   recvF(int aSource, int aTag) throws MPIException {return Native.MPI_RecvF(aSource, aTag, mPtr);}
        public String recvStr(int aSource, int aTag) throws MPIException {
            final int tLen = recvI(aSource, aTag);
            byte[] rBytes = ByteArrayCache.getArray(tLen);
            try {
                recv(rBytes, tLen, aSource, aTag);
                return UT.Serial.bytes2str(rBytes, 0, tLen);
            } finally {
                ByteArrayCache.returnArray(rBytes);
            }
        }
        public byte    recvB(int aSource) throws MPIException {return Native.MPI_RecvB(aSource, Tag.ANY, mPtr);}
        public double  recvD(int aSource) throws MPIException {return Native.MPI_RecvD(aSource, Tag.ANY, mPtr);}
        public boolean recvZ(int aSource) throws MPIException {return Native.MPI_RecvZ(aSource, Tag.ANY, mPtr);}
        public char    recvC(int aSource) throws MPIException {return Native.MPI_RecvC(aSource, Tag.ANY, mPtr);}
        public short   recvS(int aSource) throws MPIException {return Native.MPI_RecvS(aSource, Tag.ANY, mPtr);}
        public int     recvI(int aSource) throws MPIException {return Native.MPI_RecvI(aSource, Tag.ANY, mPtr);}
        public long    recvL(int aSource) throws MPIException {return Native.MPI_RecvL(aSource, Tag.ANY, mPtr);}
        public float   recvF(int aSource) throws MPIException {return Native.MPI_RecvF(aSource, Tag.ANY, mPtr);}
        public String recvStr(int aSource) throws MPIException {return recvStr(aSource, Tag.ANY);}
        /** 提供内部类型的支持，统一进行类型优化（例如后续对 shift 的支持）*/
        public void recv(IVector rVector, int aSource, int aTag) throws MPIException {
            Vector rBuf = rVector.toBuf(true); // recv 只需要写入
            try {recv(rBuf.internalData(), rBuf.internalDataSize(), aSource, aTag);}
            finally {rVector.releaseBuf(rBuf);}
        }
        public void recv(IIntVector rVector, int aSource, int aTag) throws MPIException {
            IntVector rBuf = rVector.toBuf(true); // recv 只需要写入
            try {recv(rBuf.internalData(), rBuf.internalDataSize(), aSource, aTag);}
            finally {rVector.releaseBuf(rBuf);}
        }
        public void recv(IVector rVector, int aSource) throws MPIException {recv(rVector, aSource, Tag.ANY);}
        public void recv(IIntVector rVector, int aSource) throws MPIException {recv(rVector, aSource, Tag.ANY);}
        
        /**
         * Sends and receives a message.
         *
         * @param aSendBuf The data array to be sent
         * @param aSendCount Number of elements in send buffer.
         * @param aDest Rank of destination.
         * @param aSendTag Send tag.
         * @param rRecvBuf The data array to be received
         * @param aRecvCount Number of elements in receive buffer.
         * @param aSource Rank of source.
         * @param aRecvTag Receive tag.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-sendrecv-function"> MPI_Sendrecv function </a>
         */
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
    }
    
    public enum Op {
          NULL   (Native.MPI_OP_NULL)
        , MAX    (Native.MPI_MAX    )
        , MIN    (Native.MPI_MIN    )
        , SUM    (Native.MPI_SUM    )
        , PROD   (Native.MPI_PROD   )
        , LAND   (Native.MPI_LAND   )
        , BAND   (Native.MPI_BAND   )
        , LOR    (Native.MPI_LOR    )
        , BOR    (Native.MPI_BOR    )
        , LXOR   (Native.MPI_LXOR   )
        , BXOR   (Native.MPI_BXOR   )
        , MINLOC (Native.MPI_MINLOC )
        , MAXLOC (Native.MPI_MAXLOC )
        , REPLACE(Native.MPI_REPLACE)
        ;
        
        private final long mPtr;
        Op(long aPtr) {mPtr = aPtr;}
        @ApiStatus.Internal public long ptr_() {return mPtr;}
    }
    
    public enum Datatype {
          NULL   (Native.MPI_JNULL   )
        , BYTE   (Native.MPI_JBYTE   )
        , DOUBLE (Native.MPI_JDOUBLE )
        , BOOLEAN(Native.MPI_JBOOLEAN)
        , CHAR   (Native.MPI_JCHAR   )
        , SHORT  (Native.MPI_JSHORT  )
        , INT    (Native.MPI_JINT    )
        , LONG   (Native.MPI_JLONG   )
        , FLOAT  (Native.MPI_JFLOAT  )
        ;
        
        private final long mPtr;
        Datatype(long aPtr) {mPtr = aPtr;}
        @ApiStatus.Internal public long ptr_() {return mPtr;}
    }
    
    public final static class Thread {
        private Thread() {}
        
        public final static int
              SINGLE     = Native.MPI_THREAD_SINGLE
            , FUNNELED   = Native.MPI_THREAD_FUNNELED
            , SERIALIZED = Native.MPI_THREAD_SERIALIZED
            , MULTIPLE   = Native.MPI_THREAD_MULTIPLE
            ;
    }
    
    public final static class Rank {
        private Rank() {}
        
        public final static int
              NULL = Native.MPI_PROC_NULL
            , ANY  = Native.MPI_ANY_SOURCE
            , ROOT = Native.MPI_ROOT
            ;
    }
    public final static class Tag {
        private Tag() {}
        
        public final static int ANY = Native.MPI_ANY_TAG;
    }
    
    
    /// 基础功能
    /**
     * Initializes the calling MPI process’s execution environment for single threaded execution.
     * @param aArgs the argument list for the program
     */
    public static void init(String[] aArgs) throws MPIException {Native.MPI_Init(aArgs);}
    public static void init() throws MPIException {Native.MPI_Init(ZL_STR);}
    /**
     * Indicates whether {@link MPI#init} has been called.
     * @return true if {@link MPI#init} or {@link MPI#initThread} has been called and false otherwise.
     */
    public static boolean initialized() throws MPIException {return Native.MPI_Initialized();}
    
    /**
     * Terminates the calling MPI process’s execution environment.
     */
    public static void shutdown() throws MPIException {Native.MPI_Finalize();}
    /**
     * Indicates whether {@link MPI#shutdown} has been called.
     * @return true if MPI_Finalize has been called and false otherwise.
     */
    public static boolean isShutdown() throws MPIException {return Native.MPI_Finalized();}
    
    
    
    
    /// MPI External Functions
    /**
     * Initializes the calling MPI process’s execution environment for threaded execution.
     * @param aArgs The argument list for the program
     * @param aRequired The level of desired thread support. Multiple MPI processes
     *                  in the same job may use different values.
     * @return The level of provided thread support.
     * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-init-thread-function"> MPI_Init_thread function </a>
     */
    public static int initThread(String[] aArgs, int aRequired) throws MPIException {return Native.MPI_Init_thread(aArgs, aRequired);}
    public static int initThread(int aRequired) throws MPIException {return Native.MPI_Init_thread(ZL_STR, aRequired);}
    
    
    
    /**
     * 提供按照原始的 MPI 标准格式的接口以及对应的 native 实现
     * @author liqa
     */
    public static class Native {
        private Native() {}
        
        private final static String MPIJNI_LIB_DIR = JAR_DIR+"mpi/" + UT.Code.uniqueID(VERSION, Conf.USE_MIMALLOC) + "/";
        private final static String MPIJNI_LIB_PATH;
        private final static String[] MPIJNI_SRC_NAME = {
              "jse_parallel_MPI_Native.c"
            , "jse_parallel_MPI_Native.h"
        };
        
        private static String cmakeInitCmd_() {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cmake");
            // 这里设置 C/C++ 编译器（如果有）
            if (Conf.CMAKE_C_COMPILER   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + Conf.CMAKE_C_COMPILER   );}
            if (Conf.CMAKE_CXX_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ Conf.CMAKE_CXX_COMPILER );}
            if (Conf.CMAKE_C_FLAGS      != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"    + Conf.CMAKE_C_FLAGS  +"'");}
            if (Conf.CMAKE_CXX_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + Conf.CMAKE_CXX_FLAGS+"'");}
            // 初始化使用上一个目录的 CMakeList.txt
            rCommand.add("..");
            return String.join(" ", rCommand);
        }
        private static String cmakeSettingCmd_() throws IOException {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cmake");
            rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="+(Conf.USE_MIMALLOC?"ON":"OFF"));
            // 设置构建输出目录为 lib
            UT.IO.makeDir(MPIJNI_LIB_DIR); // 初始化一下这个目录避免意料外的问题
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ MPIJNI_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ MPIJNI_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ MPIJNI_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ MPIJNI_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ MPIJNI_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ MPIJNI_LIB_DIR +"'");
            rCommand.add(".");
            return String.join(" ", rCommand);
        }
        
        private static @NotNull String initMPI_() throws Exception {
            // 检测 cmake，为了简洁并避免问题，现在要求一定要有 cmake 环境
            EXEC.setNoSTDOutput().setNoERROutput();
            boolean tNoCmake = EXEC.system("cmake --version") != 0;
            EXEC.setNoSTDOutput(false).setNoERROutput(false);
            if (tNoCmake) throw new Exception("MPI BUILD ERROR: No cmake environment.");
            // 从内部资源解压到临时目录
            String tWorkingDir = WORKING_DIR_OF("mpijni");
            // 如果已经存在则先删除
            UT.IO.removeDir(tWorkingDir);
            for (String tName : MPIJNI_SRC_NAME) {
                UT.IO.copy(UT.IO.getResource("mpi/src/"+tName), tWorkingDir+tName);
            }
            // 这里对 CMakeLists.txt 特殊处理
            UT.IO.map(UT.IO.getResource("mpi/src/CMakeLists.txt"), tWorkingDir+"CMakeLists.txt", line -> {
                // 替换其中的 jniutil 库路径为设置好的路径
                line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                // 替换其中的 mimalloc 库路径为设置好的路径
                if (Conf.USE_MIMALLOC) {
                line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
                }
                return line;
            });
            System.out.println("MPI INIT INFO: Building mpijni from source code...");
            String tBuildDir = tWorkingDir+"build/";
            UT.IO.makeDir(tBuildDir);
            // 直接通过系统指令来编译 mpijni 的库，关闭输出
            EXEC.setNoSTDOutput().setWorkingDir(tBuildDir);
            // 初始化 cmake
            EXEC.system(cmakeInitCmd_());
            // 设置参数
            EXEC.system(cmakeSettingCmd_());
            // 最后进行构造操作
            EXEC.system("cmake --build . --config Release");
            EXEC.setNoSTDOutput(false).setWorkingDir(null);
            // 简单检测一下是否编译成功
            @Nullable String tLibName = LIB_NAME_IN(MPIJNI_LIB_DIR, "mpijni");
            if (tLibName == null) throw new Exception("MPI BUILD ERROR: Build Failed, No mpijni lib in '"+MPIJNI_LIB_DIR+"'");
            // 完事后移除临时解压得到的源码
            UT.IO.removeDir(tWorkingDir);
            System.out.println("MPI INIT INFO: mpijni successfully installed.");
            // 输出安装完成后的库名称
            return tLibName;
        }
        
        // 直接进行初始化，虽然原则上会在 MPI_Init() 之前获取，
        // 但是得到的是 final 值，可以避免意外的修改，并且简化代码；
        // 这对于一般的 MPI 实现应该都是没有问题的
        static {
            InitHelper.INITIALIZED = true;
            // 依赖 jniutil
            JNIUtil.InitHelper.init();
            // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
            if (Conf.USE_MIMALLOC) MiMalloc.InitHelper.init();
            
            if (Conf.REDIRECT_MPIJNI_LIB == null) {
                @Nullable String tLibName = LIB_NAME_IN(MPIJNI_LIB_DIR, "mpijni");
                // 如果不存在 jni lib 则需要重新通过源码编译
                if (tLibName == null) {
                    System.out.println("MPI INIT INFO: mpijni libraries not found. Reinstalling...");
                    try {tLibName = initMPI_();} catch (Exception e) {throw new RuntimeException(e);}
                }
                MPIJNI_LIB_PATH = MPIJNI_LIB_DIR+tLibName;
            } else {
                if (DEBUG) System.out.println("MPI INIT INFO: mpijni libraries are redirected to '"+Conf.REDIRECT_MPIJNI_LIB+"'");
                MPIJNI_LIB_PATH = Conf.REDIRECT_MPIJNI_LIB;
            }
            // 设置库路径
            System.load(UT.IO.toAbsolutePath(MPIJNI_LIB_PATH));
            
            // 初始化 final 常量
            MPI_GROUP_NULL  = getMpiGroupNull_();
            MPI_GROUP_EMPTY = getMpiGroupEmpty_();
            
            MPI_COMM_NULL  = getMpiCommNull_();
            MPI_COMM_WORLD = getMpiCommWorld_();
            MPI_COMM_SELF  = getMpiCommSelf_();
            
            MPI_OP_NULL  = getMpiOpNull_();
            MPI_MAX      = getMpiMax_();
            MPI_MIN      = getMpiMin_();
            MPI_SUM      = getMpiSum_();
            MPI_PROD     = getMpiProd_();
            MPI_LAND     = getMpiLand_();
            MPI_BAND     = getMpiBand_();
            MPI_LOR      = getMpiLor_();
            MPI_BOR      = getMpiBor_();
            MPI_LXOR     = getMpiLxor_();
            MPI_BXOR     = getMpiBxor_();
            MPI_MINLOC   = getMpiMinloc_();
            MPI_MAXLOC   = getMpiMaxloc_();
            MPI_REPLACE  = getMpiReplace_();
            
            MPI_DATATYPE_NULL      = getMpiDatatypeNull_();
            MPI_CHAR               = getMpiChar_();
            MPI_UNSIGNED_CHAR      = getMpiUnsignedChar_();
            MPI_SHORT              = getMpiShort_();
            MPI_UNSIGNED_SHORT     = getMpiUnsignedShort_();
            MPI_INT                = getMpiInt_();
            MPI_UNSIGNED           = getMpiUnsigned_();
            MPI_LONG               = getMpiLong_();
            MPI_UNSIGNED_LONG      = getMpiUnsignedLong_();
            MPI_LONG_LONG          = getMpiLongLong_();
            MPI_FLOAT              = getMpiFloat_();
            MPI_DOUBLE             = getMpiDouble_();
            MPI_BYTE               = getMpiByte_();
            MPI_SIGNED_CHAR        = getMpiSignedChar_();
            MPI_UNSIGNED_LONG_LONG = getMpiUnsignedLongLong_();
            MPI_INT8_T             = getMpiInt8T_();
            MPI_INT16_T            = getMpiInt16T_();
            MPI_INT32_T            = getMpiInt32T_();
            MPI_INT64_T            = getMpiInt64T_();
            MPI_UINT8_T            = getMpiUint8T_();
            MPI_UINT16_T           = getMpiUint16T_();
            MPI_UINT32_T           = getMpiUint32T_();
            MPI_UINT64_T           = getMpiUint64T_();
            
            MPI_THREAD_SINGLE     = getMpiThreadSingle_();
            MPI_THREAD_FUNNELED   = getMpiThreadFunneled_();
            MPI_THREAD_SERIALIZED = getMpiThreadSerialized_();
            MPI_THREAD_MULTIPLE   = getMpiThreadMultiple_();
            
            MPI_PROC_NULL  = getMpiProcNull_();
            MPI_ANY_SOURCE = getMpiAnySource_();
            MPI_ROOT       = getMpiRoot_();
            
            MPI_ANY_TAG = getMpiAnyTag_();
            
            MPI_UNDEFINED = getMpiUndefined_();
        }
        
        public final static long MPI_GROUP_NULL, MPI_GROUP_EMPTY;
        private native static long getMpiGroupNull_();
        private native static long getMpiGroupEmpty_();
        
        public final static long MPI_COMM_NULL, MPI_COMM_WORLD, MPI_COMM_SELF;
        private native static long getMpiCommNull_();
        private native static long getMpiCommWorld_();
        private native static long getMpiCommSelf_();
        
        public final static long MPI_OP_NULL, MPI_MAX, MPI_MIN, MPI_SUM, MPI_PROD, MPI_LAND, MPI_BAND, MPI_LOR, MPI_BOR, MPI_LXOR, MPI_BXOR, MPI_MINLOC, MPI_MAXLOC, MPI_REPLACE;
        private native static long getMpiOpNull_();
        private native static long getMpiMax_();
        private native static long getMpiMin_();
        private native static long getMpiSum_();
        private native static long getMpiProd_();
        private native static long getMpiLand_();
        private native static long getMpiBand_();
        private native static long getMpiLor_();
        private native static long getMpiBor_();
        private native static long getMpiLxor_();
        private native static long getMpiBxor_();
        private native static long getMpiMinloc_();
        private native static long getMpiMaxloc_();
        private native static long getMpiReplace_();
        
        // 只保留部分必要的，因为 MPI 实现中都不尽相同
        public final static long MPI_DATATYPE_NULL, MPI_CHAR, MPI_UNSIGNED_CHAR, MPI_SHORT, MPI_UNSIGNED_SHORT, MPI_INT, MPI_UNSIGNED, MPI_LONG, MPI_UNSIGNED_LONG, MPI_LONG_LONG, MPI_FLOAT, MPI_DOUBLE, MPI_BYTE, MPI_SIGNED_CHAR, MPI_UNSIGNED_LONG_LONG, MPI_INT8_T, MPI_INT16_T, MPI_INT32_T, MPI_INT64_T, MPI_UINT8_T, MPI_UINT16_T, MPI_UINT32_T, MPI_UINT64_T;
        private native static long getMpiDatatypeNull_();
        private native static long getMpiChar_();
        private native static long getMpiUnsignedChar_();
        private native static long getMpiShort_();
        private native static long getMpiUnsignedShort_();
        private native static long getMpiInt_();
        private native static long getMpiUnsigned_();
        private native static long getMpiLong_();
        private native static long getMpiUnsignedLong_();
        private native static long getMpiLongLong_();
        private native static long getMpiFloat_();
        private native static long getMpiDouble_();
        private native static long getMpiByte_();
        private native static long getMpiSignedChar_();
        private native static long getMpiUnsignedLongLong_();
        private native static long getMpiInt8T_();
        private native static long getMpiInt16T_();
        private native static long getMpiInt32T_();
        private native static long getMpiInt64T_();
        private native static long getMpiUint8T_();
        private native static long getMpiUint16T_();
        private native static long getMpiUint32T_();
        private native static long getMpiUint64T_();
        /** 这里增加 java 原始类型的定义，基本和 open mpi 保持一致 */
        public final static long
          MPI_JNULL    = MPI_DATATYPE_NULL
        , MPI_JBYTE    = MPI_INT8_T
        , MPI_JDOUBLE  = MPI_DOUBLE
        , MPI_JBOOLEAN = MPI_UNSIGNED_CHAR // 这里和 jni 的头文件类型保持一致，因为似乎 boolean 不一定要 8 位
        , MPI_JCHAR    = MPI_UINT16_T
        , MPI_JSHORT   = MPI_INT16_T
        , MPI_JINT     = MPI_INT32_T
        , MPI_JLONG    = MPI_INT64_T
        , MPI_JFLOAT   = MPI_FLOAT
        ;
        
        public final static int MPI_THREAD_SINGLE, MPI_THREAD_FUNNELED, MPI_THREAD_SERIALIZED, MPI_THREAD_MULTIPLE;
        private native static int getMpiThreadSingle_();
        private native static int getMpiThreadFunneled_();
        private native static int getMpiThreadSerialized_();
        private native static int getMpiThreadMultiple_();
        
        /** Used in: Rank */
        public final static int MPI_PROC_NULL, MPI_ANY_SOURCE, MPI_ROOT;
        private native static int getMpiProcNull_();
        private native static int getMpiAnySource_();
        private native static int getMpiRoot_();
        
        /** Used in: Tag */
        public final static int MPI_ANY_TAG;
        private native static int getMpiAnyTag_();
        
        /** Used in: Count, Index, Rank, Color, Toplogy, Precision, Exponent range  */
        public final static int MPI_UNDEFINED;
        private native static int getMpiUndefined_();
        
        
        /** Debug, 输出此 mpi 实例的版本，可以用来检测 mpi 是否一致 */
        public native static String MPI_Get_library_version() throws MPIException;
        
        
        /// 基础功能
        /**
         * Initializes the calling MPI process’s execution environment for single threaded execution.
         * @param aArgs the argument list for the program
         */
        public native static void MPI_Init(String[] aArgs) throws MPIException;
        /**
         * Indicates whether {@link #MPI_Init} has been called.
         * @return true if {@link #MPI_Init} or {@link #MPI_Init_thread} has been called and false otherwise.
         */
        public native static boolean MPI_Initialized() throws MPIException;
        
        /**
         * Retrieves the rank of the calling process in the group of the specified communicator.
         * @param aComm The communicator.
         * @return the number of calling process within the group of the communicator.
         */
        public native static int MPI_Comm_rank(long aComm) throws MPIException;
        
        /**
         * Retrieves the number of processes involved in a communicator, or the total number of
         * processes available.
         * @param aComm The communicator to evaluate. Specify the {@link #MPI_COMM_WORLD} constant to retrieve
         *              the total number of processes available.
         * @return the number of processes in the group for the communicator.
         */
        public native static int MPI_Comm_size(long aComm) throws MPIException;
        
        /**
         * Terminates the calling MPI process’s execution environment.
         */
        public native static void MPI_Finalize() throws MPIException;
        /**
         * Indicates whether {@link #MPI_Finalize} has been called.
         * @return true if MPI_Finalize has been called and false otherwise.
         */
        public native static boolean MPI_Finalized() throws MPIException;
        
        
        
        /// MPI Collective Functions
        /**
         * Gathers data from all members of a group and sends the data to all members of the group.
         * The MPI_Allgather function is similar to the {@link #MPI_Gather} function, except that it sends
         * the data to all processes instead of only to the root. The usage rules for MPI_Allgather
         * correspond to the rules for {@link #MPI_Gather}.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer are specified in the aSendCount and
         *                 the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCount parameters and the data type will be
         *                 detected automatically.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgather-function"> MPI_Allgather function </a>
         */
        public static void MPI_Allgather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, long aComm) throws MPIException {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        public static void MPI_Allgather(byte[]    rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_BYTE   , rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgather(double[]  rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_DOUBLE , rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgather(boolean[] rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_BOOLEAN, rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgather(char[]    rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_CHAR   , rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgather(short[]   rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_SHORT  , rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgather(int[]     rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_INT    , rBuf, aCount, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgather(long[]    rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_LONG   , rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgather(float[]   rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather0(true, null, 0, MPI_JNULL, JTYPE_FLOAT  , rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        private native static void MPI_Allgather0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRecvJType, long aComm) throws MPIException;
        
        /**
         * Gathers a variable amount of data from each member of a group and sends the data to all members of the group.
         * The MPI_Allgatherv function is like the {@link #MPI_Gatherv}, except that all processes receive the result,
         * instead of just the root. The block of data that is sent from the jth process is received by every process
         * and placed in the jth block of the buffer rRecvBuf. These blocks do not all have to be the same size.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer are specified in the aSendCount and
         *                 the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCounts parameters and the data type will be
         *                 detected automatically.
         *
         * @param aRecvCounts The number of data elements from each communicator process in the receive buffer.
         *
         * @param aDispls The location, relative to the recvbuf parameter, of the data from each communicator process.
         *                <p>
         *                In the rRecvBuf, aRecvCounts, and aDispls parameter arrays, the nth element of each
         *                array refers to the data that is received from the nth communicator process.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgatherv-function"> MPI_Allgatherv function </a>
         */
        public static void MPI_Allgatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvCounts, aDispls, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvCounts, aDispls, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvCounts, aDispls, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvCounts, aDispls, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvCounts, aDispls, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvCounts, aDispls, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvCounts, aDispls, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvCounts, aDispls, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        public static void MPI_Allgatherv(byte[]    rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgatherv(double[]  rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgatherv(boolean[] rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgatherv(char[]    rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgatherv(short[]   rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgatherv(int[]     rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgatherv(long[]    rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgatherv(float[]   rBuf, int[] aCounts, int[] aDispls, long aComm) throws MPIException {MPI_Allgatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        private native static void MPI_Allgatherv0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int[] aRecvCounts, int[] aDispls, long aRecvType, int aRecvJType, long aComm) throws MPIException;
        
        /**
         * Combines values from all processes and distributes the result back to all processes.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer are specified in the aSendCount and
         *                 the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCounts parameters and the data type will be
         *                 detected automatically.
         *
         * @param aCount The number of elements to send from this process.
         *
         * @param aOp The MPI_Op handle indicating the global reduction operation to perform.
         *            The handle can indicate a built-in or application-defined operation.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allreduce-function"> MPI_Allreduce function </a>
         */
        public static void MPI_Allreduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aComm);}
        public static void MPI_Allreduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aComm);}
        public static void MPI_Allreduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aComm);}
        public static void MPI_Allreduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aComm);}
        public static void MPI_Allreduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aComm);}
        public static void MPI_Allreduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JINT    , JTYPE_INT    , aOp, aComm);}
        public static void MPI_Allreduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aComm);}
        public static void MPI_Allreduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aComm);}
        public static void MPI_Allreduce(byte[]    rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aComm);}
        public static void MPI_Allreduce(double[]  rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aComm);}
        public static void MPI_Allreduce(boolean[] rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aComm);}
        public static void MPI_Allreduce(char[]    rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aComm);}
        public static void MPI_Allreduce(short[]   rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aComm);}
        public static void MPI_Allreduce(int[]     rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JINT    , JTYPE_INT    , aOp, aComm);}
        public static void MPI_Allreduce(long[]    rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aComm);}
        public static void MPI_Allreduce(float[]   rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce0(true, null, rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aComm);}
        private native static void MPI_Allreduce0(boolean aInPlace, Object aSendBuf, Object rRecvBuf, int aCount, long aDataType, int aJDataType, long aOp, long aComm) throws MPIException;
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public native static byte    MPI_AllreduceB(byte    aB, long aOp, long aComm) throws MPIException;
        public native static double  MPI_AllreduceD(double  aD, long aOp, long aComm) throws MPIException;
        public native static boolean MPI_AllreduceZ(boolean aZ, long aOp, long aComm) throws MPIException;
        public native static char    MPI_AllreduceC(char    aC, long aOp, long aComm) throws MPIException;
        public native static short   MPI_AllreduceS(short   aS, long aOp, long aComm) throws MPIException;
        public native static int     MPI_AllreduceI(int     aI, long aOp, long aComm) throws MPIException;
        public native static long    MPI_AllreduceL(long    aL, long aOp, long aComm) throws MPIException;
        public native static float   MPI_AllreduceF(float   aF, long aOp, long aComm) throws MPIException;
        
        /**
         * Initiates barrier synchronization across all members of a group.
         *
         * @param aComm The communicator to synchronize.
         *              <p>
         *              If this is an intracommunicator, the MPI_Barrier function blocks the caller
         *              until all group members have called it.
         *              The function does not return on any process until all group processes have
         *              called the function.
         *              <p>
         *              If this is an intercommunicator, the MPI_Barrier function involves two groups.
         *              The function returns on processes in one group, group A, only after all members
         *              of the other group, group B, have called the function, and vice versa.
         *              The function can return for a process before all processes in its own group
         *              have called the function.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-barrier-function"> MPI_Barrier function </a>
         */
        public native static void MPI_Barrier(long aComm) throws MPIException;
        
        /**
         * Broadcasts data from one member of a group to all members of the group.
         *
         * @param rBuf The data array. On the process that is specified by the root parameter,
         *             the buffer contains the data to be broadcast. On all other processes
         *             in the communicator that is specified by the comm parameter,
         *             the buffer receives the data broadcast by the root process.
         *
         * @param aCount The number of data elements in the buffer.
         *               If the count parameter is zero, the data part of the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-bcast-function"> MPI_Bcast function </a>
         */
        public static void MPI_Bcast(byte[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Bcast(double[]  rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Bcast(boolean[] rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Bcast(char[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Bcast(short[]   rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Bcast(int[]     rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Bcast(long[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Bcast(float[]   rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast0(rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        private native static void MPI_Bcast0(Object rBuf, int aCount, long aDataType, int aJDataType, int aRoot, long aComm) throws MPIException;
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public native static byte    MPI_BcastB(byte    aB, int aRoot, long aComm) throws MPIException;
        public native static double  MPI_BcastD(double  aD, int aRoot, long aComm) throws MPIException;
        public native static boolean MPI_BcastZ(boolean aZ, int aRoot, long aComm) throws MPIException;
        public native static char    MPI_BcastC(char    aC, int aRoot, long aComm) throws MPIException;
        public native static short   MPI_BcastS(short   aS, int aRoot, long aComm) throws MPIException;
        public native static int     MPI_BcastI(int     aI, int aRoot, long aComm) throws MPIException;
        public native static long    MPI_BcastL(long    aL, int aRoot, long aComm) throws MPIException;
        public native static float   MPI_BcastF(float   aF, int aRoot, long aComm) throws MPIException;
        
        /**
         * Gathers data from all members of a group to one member.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gather-function"> MPI_Gather function </a>
         */
        public static void MPI_Gather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Gather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Gather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Gather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Gather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Gather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Gather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Gather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Gather0(false, aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Gather(byte[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(double[]  rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(boolean[] rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(char[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(short[]   rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(int[]     rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JINT    , JTYPE_INT    , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(long[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(float[]   rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gather0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        
        /**
         * Gathers variable data from all members of a group to one member.
         * The MPI_Gatherv function adds flexibility to the {@link #MPI_Gather} function by
         * allowing a varying count of data from each process.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aRecvCounts The number of elements that is received from each process. Each element in the array
         *                    corresponds to the rank of the sending process. If the count is zero, the data part of
         *                    the message is empty. This parameter is significant only at the root process.
         *
         * @param aDispls The location, relative to the recvbuf parameter, of the data from each communicator process.
         *                The data that is received from process j is placed into the receive buffer of the root
         *                process offset displs[j] elements from the sendbuf pointer.
         *                <p>
         *                In the recvbuf, recvcounts, and displs parameter arrays, the nth element of each array
         *                refers to the data that is received from the nth communicator process.
         *                <p>
         *                This parameter is significant only at the root process.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gatherv-function"> MPI_Gatherv function </a>
         */
        public static void MPI_Gatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvCounts, aDispls, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Gatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvCounts, aDispls, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Gatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvCounts, aDispls, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Gatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvCounts, aDispls, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Gatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvCounts, aDispls, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Gatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvCounts, aDispls, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Gatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvCounts, aDispls, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Gatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvCounts, aDispls, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Gatherv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JBYTE   , JTYPE_BYTE   , null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JDOUBLE , JTYPE_DOUBLE , null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JBOOLEAN, JTYPE_BOOLEAN, null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JCHAR   , JTYPE_CHAR   , null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JSHORT  , JTYPE_SHORT  , null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JINT    , JTYPE_INT    , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JINT    , JTYPE_INT    , null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JLONG   , JTYPE_LONG   , null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCounts, aDispls, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_JFLOAT  , JTYPE_FLOAT  , null, null, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gatherv0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int[] aRecvCounts, int[] aDispls, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        
        /**
         * Performs a global reduce operation across all members of a group. You can specify
         * a predefined mathematical or logical operation or an application-defined operation.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param rRecvBuf The data array to receive the result of the reduction operation.
         *                 This parameter is significant only at the root process.
         *
         * @param aCount The number of elements to send from this process.
         *
         * @param aOp The MPI_Op handle indicating the global reduction operation to perform.
         *            The handle can indicate a built-in or application-defined operation.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-reduce-function"> MPI_Reduce function </a>
         */
        public static void MPI_Reduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aRoot, aComm);}
        public static void MPI_Reduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aRoot, aComm);}
        public static void MPI_Reduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aRoot, aComm);}
        public static void MPI_Reduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JINT    , JTYPE_INT    , aOp, aRoot, aComm);}
        public static void MPI_Reduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aRoot, aComm);}
        public static void MPI_Reduce(byte[]    rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(double[]  rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(boolean[] rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aRoot, aComm);}}
        public static void MPI_Reduce(char[]    rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(short[]   rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(int[]     rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JINT    , JTYPE_INT    , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JINT    , JTYPE_INT    , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(long[]    rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(float[]   rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aRoot, aComm);}}
        private native static void MPI_Reduce0(boolean aInPlace, Object aSendBuf, Object rRecvBuf, int aCount, long aDataType, int aJDataType, long aOp, int aRoot, long aComm) throws MPIException;
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public native static byte    MPI_ReduceB(byte    aB, long aOp, int aRoot, long aComm) throws MPIException;
        public native static double  MPI_ReduceD(double  aD, long aOp, int aRoot, long aComm) throws MPIException;
        public native static boolean MPI_ReduceZ(boolean aZ, long aOp, int aRoot, long aComm) throws MPIException;
        public native static char    MPI_ReduceC(char    aC, long aOp, int aRoot, long aComm) throws MPIException;
        public native static short   MPI_ReduceS(short   aS, long aOp, int aRoot, long aComm) throws MPIException;
        public native static int     MPI_ReduceI(int     aI, long aOp, int aRoot, long aComm) throws MPIException;
        public native static long    MPI_ReduceL(long    aL, long aOp, int aRoot, long aComm) throws MPIException;
        public native static float   MPI_ReduceF(float   aF, long aOp, int aRoot, long aComm) throws MPIException;
        
        /**
         * Scatters data from one member across all members of a group.
         * The MPI_Scatter function performs the inverse of the operation
         * that is performed by the {@link #MPI_Gather} function.
         *
         * @param aSendBuf The data array to be sent by the root process.
         *                 <p>
         *                 The aSendBuf parameter is ignored for all non-root processes.
         *
         * @param aSendCount The number of elements in the buffer that is specified in the sendbuf parameter.
         *                   If sendcount is zero, the data part of the message is empty.
         *                   <p>
         *                   The aSendCount parameter is ignored for all non-root processes.
         *
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @param aRoot The rank of the sending process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatter-function"> MPI_Scatter function </a>
         */
        public static void MPI_Scatter(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Scatter(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Scatter(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Scatter(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Scatter(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Scatter(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Scatter(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Scatter(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Scatter(byte[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}}
        public static void MPI_Scatter(double[]  rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}}
        public static void MPI_Scatter(boolean[] rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}}
        public static void MPI_Scatter(char[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}}
        public static void MPI_Scatter(short[]   rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}}
        public static void MPI_Scatter(int[]     rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JINT    , JTYPE_INT    , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}}
        public static void MPI_Scatter(long[]    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}}
        public static void MPI_Scatter(float[]   rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_JNULL, JTYPE_NULL, rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}}
        private native static void MPI_Scatter0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        
        /**
         * Scatters data from one member across all members of a group.
         * The MPI_Scatterv function performs the inverse of the operation
         * that is performed by the {@link #MPI_Gatherv} function.
         *
         * @param aSendBuf The data array to be sent by the root process.
         *                 <p>
         *                 The aSendBuf parameter is ignored for all non-root processes.
         *
         * @param aSendCounts The number of elements in the buffer that is specified in the sendbuf parameter.
         *                    If sendcount[i] is zero, the data part of the message for that process is empty.
         *                    <p>
         *                    The aSendCount parameter is ignored for all non-root processes.
         *
         * @param aDispls The locations of the data to send to each communicator process.
         *                Each location in the array is relative to the corresponding element of the sendbuf array.
         *                <p>
         *                In the sendbuf, sendcounts, and displs parameter arrays, the nth element of
         *                each array refers to the data to be sent to the nth communicator process.
         *                <p>
         *                This parameter is significant only at the root process.
         *
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aRecvCount The number of elements in the receive buffer. If the count is zero,
         *                   the data part of the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatterv-function"> MPI_Scatterv function </a>
         */
        public static void MPI_Scatterv(byte[]    aSendBuf, int[] aSendCounts, int[] aDispls, byte[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Scatterv(double[]  aSendBuf, int[] aSendCounts, int[] aDispls, double[]  rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Scatterv(boolean[] aSendBuf, int[] aSendCounts, int[] aDispls, boolean[] rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Scatterv(char[]    aSendBuf, int[] aSendCounts, int[] aDispls, char[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Scatterv(short[]   aSendBuf, int[] aSendCounts, int[] aDispls, short[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Scatterv(int[]     aSendBuf, int[] aSendCounts, int[] aDispls, int[]     rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Scatterv(long[]    aSendBuf, int[] aSendCounts, int[] aDispls, long[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Scatterv(float[]   aSendBuf, int[] aSendCounts, int[] aDispls, float[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws MPIException {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Scatterv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JBYTE   , JTYPE_BYTE   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}}
        public static void MPI_Scatterv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JDOUBLE , JTYPE_DOUBLE , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}}
        public static void MPI_Scatterv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JBOOLEAN, JTYPE_BOOLEAN, null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}}
        public static void MPI_Scatterv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JCHAR   , JTYPE_CHAR   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}}
        public static void MPI_Scatterv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JSHORT  , JTYPE_SHORT  , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}}
        public static void MPI_Scatterv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JINT    , JTYPE_INT    , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JINT    , JTYPE_INT    , aRoot, aComm);}}
        public static void MPI_Scatterv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JLONG   , JTYPE_LONG   , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}}
        public static void MPI_Scatterv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_JFLOAT  , JTYPE_FLOAT  , null, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_JNULL, JTYPE_NULL, rBuf, aCounts[tRank], MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}}
        private native static void MPI_Scatterv0(boolean aInPlace, Object aSendBuf, int[] aSendCounts, int[] aDispls, long aSendType, int aSendJType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        
        
        
        /// MPI Communicator Functions
        /**
         * Extracts a subset a group of processes for the purpose of separate Multiple Instruction
         * Multiple Data (MIMD) computation in a separate communicator.
         *
         * @param aComm The source communicator.
         * @param aGroup The group that defines the requested subset of the processes in the
         *               source communicator.
         * @return An MPI_Comm handle to a new communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-create-function"> MPI_Comm_create function </a>
         */
        public native static long MPI_Comm_create(long aComm, long aGroup) throws MPIException;
        
        /**
         * Duplicates an existing communicator with associated key values. For each key value,
         * the respective copy callback function determines the attribute value that is associated
         * with this key in the new communicator.
         * The copy callback can, for example, delete the attribute from the new communicator.
         *
         * @param aComm The communicator to duplicate.
         * @return An MPI_Comm handle to a new communicator. The new communicator has the same
         * group or groups and any copied cached information from the source, but it contains
         * new context information.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-dup-function"> MPI_Comm_dup function </a>
         */
        public native static long MPI_Comm_dup(long aComm) throws MPIException;
        
        /**
         * Frees a communicator that is allocated with the {@link #MPI_Comm_dup}, {@link #MPI_Comm_create},
         * or {@link #MPI_Comm_split} functions.
         *
         * @param aComm The communicator handle to free.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-free-function"> MPI_Comm_free function </a>
         */
        public native static void MPI_Comm_free(long aComm) throws MPIException;
        
        /**
         * Partitions the group that is associated with the specified communicator into
         * a specified number of disjoint subgroups.
         *
         * @param aComm The communicator to split.
         *
         * @param aColor The new communicator that the calling process is to be assigned to.
         *               The value of color must be non-negative.
         *               <p>
         *               If a process specifies the color value {@link #MPI_UNDEFINED},
         *               the function returns {@link #MPI_COMM_NULL} in the newcomm parameter to the calling process.
         *
         * @param aKey The relative rank of the calling process in the group of the new communicator.
         *             For details on using the key and color parameters, see Remarks.
         *
         * @return An MPI_Comm handle to a new communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-split-function"> MPI_Comm_split function </a>
         */
        public native static long MPI_Comm_split(long aComm, int aColor, int aKey) throws MPIException;
        
        
        
        /// MPI Group Functions
        /**
         * Retrieves the group that is associated with a communicator.
         *
         * @param aComm The communicator on which to base the group.
         * @return The MPI_Group handle to the group that is associated with the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-comm-group-function"> MPI_Comm_group function </a>
         */
        public native static long MPI_Comm_group(long aComm) throws MPIException;
        
        /**
         * Creates a new group from the difference between two existing groups.
         *
         * @param aGroup1 The first group.
         * @param aGroup2 The second group.
         * @return A pointer to a handle that represents a new group that contains all elements in the first group that
         * are not present in the second group. The function returns {@link #MPI_GROUP_EMPTY} if the new group is empty.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-difference-function"> MPI_Group_difference function </a>
         */
        public native static long MPI_Group_difference(long aGroup1, long aGroup2) throws MPIException;
        
        /**
         * A group constructor that is used to define a new group by deleting ranks from an existing group.
         *
         * @param aGroup The existing group.
         *
         * @param aN The number of elements in the ranks parameter.
         *
         * @param aRanks The arrays of processes in group that are not to appear in newgroup.
         *               The specified ranks must be valid in the existing group.
         *               Each element in the array must be distinct.
         *               If the array is empty then the new group will be identical to the existing group.
         *
         * @return A pointer to a handle that represents the new group that is derived from the existing group.
         * The order of the existing group is preserved in the new group.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-excl-function"> MPI_Group_excl function </a>
         */
        public native static long MPI_Group_excl(long aGroup, int aN, int[] aRanks) throws MPIException;
        
        /**
         * Frees a group.
         * @param aGroup Group to free.
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-free-function"> MPI_Group_free function </a>
         */
        public native static void MPI_Group_free(long aGroup) throws MPIException;
        
        /**
         * Creates a new group that contains a subset of the processes in an existing group.
         *
         * @param aGroup The existing group.
         *
         * @param aN The number of elements in the ranks parameter and the size of the new group.
         *
         * @param aRanks The processes to be included in the new group.
         *
         * @return A pointer to a handle that represents the new group, which contains the included
         * processes in the order that they are specified in the ranks parameter.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-incl-function"> MPI_Group_incl function </a>
         */
        public native static long MPI_Group_incl(long aGroup, int aN, int[] aRanks) throws MPIException;
        
        /**
         * Creates a new group from the intersection of two existing groups.
         *
         * @param aGroup1 The first group.
         * @param aGroup2 The second group.
         * @return A pointer to a handle that represents a new group with those elements that are present in both groups.
         * The function returns {@link #MPI_GROUP_EMPTY} if the new group is empty.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-intersection-function"> MPI_Group_intersection function </a>
         */
        public native static long MPI_Group_intersection(long aGroup1, long aGroup2) throws MPIException;
        
        /**
         * Returns the rank of the calling process in the specified group.
         *
         * @param aGroup Specifies the group to query.
         * @return An integer contains the rank of the calling process in the specified group.
         * A value of {@link #MPI_UNDEFINED} that the calling process is not a member of the specified group.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-rank-function"> MPI_Group_rank function </a>
         */
        public native static int MPI_Group_rank(long aGroup) throws MPIException;
        
        /**
         * Retrieves the size of the specified group.
         *
         * @param aGroup The group to evaluate.
         * @return The size of the specified group.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-size-function"> MPI_Group_size function </a>
         */
        public native static int MPI_Group_size(long aGroup) throws MPIException;
        
        /**
         * Creates a new group from the union of two existing groups.
         *
         * @param aGroup1 The first group.
         * @param aGroup2 The second group.
         * @return A pointer to a handle that represents a new group that represents all elements in either group.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-group-union-function"> MPI_Group_union function </a>
         */
        public native static long MPI_Group_union(long aGroup1, long aGroup2) throws MPIException;
        
        
        
        /// MPI Point to Point Functions
        /**
         * Performs a standard mode send operation and returns when the send buffer can be safely reused.
         *
         * @param aBuf The data array to be sent
         *
         * @param aCount The number of elements in the buffer. If the data part of the message
         *               is empty, set the count parameter to 0.
         *
         * @param aDest The rank of the destination process within the communicator that is
         *              specified by the comm parameter.
         *
         * @param aTag The message tag that can be used to distinguish different types of messages.
         *
         * @param aComm The handle to the communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-send-function"> MPI_Send function </a>
         */
        public static void MPI_Send(byte[]    aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aTag, aComm);}
        public static void MPI_Send(double[]  aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aTag, aComm);}
        public static void MPI_Send(boolean[] aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aTag, aComm);}
        public static void MPI_Send(char[]    aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aTag, aComm);}
        public static void MPI_Send(short[]   aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aTag, aComm);}
        public static void MPI_Send(int[]     aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JINT    , JTYPE_INT    , aDest, aTag, aComm);}
        public static void MPI_Send(long[]    aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aDest, aTag, aComm);}
        public static void MPI_Send(float[]   aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(aBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aTag, aComm);}
        public static void MPI_Send(int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(null, 0, MPI_BYTE, JTYPE_NULL, aDest, aTag, aComm);}
        private native static void MPI_Send0(Object aBuf, int aCount, long aDataType, int aJDataType, int aDest, int aTag, long aComm) throws MPIException;
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public native static void MPI_SendB(byte    aB, int aDest, int aTag, long aComm) throws MPIException;
        public native static void MPI_SendD(double  aD, int aDest, int aTag, long aComm) throws MPIException;
        public native static void MPI_SendZ(boolean aZ, int aDest, int aTag, long aComm) throws MPIException;
        public native static void MPI_SendC(char    aC, int aDest, int aTag, long aComm) throws MPIException;
        public native static void MPI_SendS(short   aS, int aDest, int aTag, long aComm) throws MPIException;
        public native static void MPI_SendI(int     aI, int aDest, int aTag, long aComm) throws MPIException;
        public native static void MPI_SendL(long    aL, int aDest, int aTag, long aComm) throws MPIException;
        public native static void MPI_SendF(float   aF, int aDest, int aTag, long aComm) throws MPIException;
        
        /**
         * Performs a receive operation and does not return until a matching message is received.
         *
         * @param rBuf The data array to be received
         *
         * @param aCount The number of elements in the buffer. If the data part of the message
         *               is empty, set the count parameter to 0.
         *
         * @param aSource The rank of the sending process within the specified communicator.
         *                Specify the {@link #MPI_ANY_SOURCE} constant to specify that any source
         *                is acceptable.
         *
         * @param aTag The message tag that is used to distinguish different types of messages.
         *             Specify the {@link #MPI_ANY_TAG} constant to indicate that any tag is
         *             acceptable.
         *
         * @param aComm The handle to the communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-recv-function"> MPI_Recv function </a>
         */
        public static void MPI_Recv(byte[]    rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aTag, aComm);}
        public static void MPI_Recv(double[]  rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aTag, aComm);}
        public static void MPI_Recv(boolean[] rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aTag, aComm);}
        public static void MPI_Recv(char[]    rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aTag, aComm);}
        public static void MPI_Recv(short[]   rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aTag, aComm);}
        public static void MPI_Recv(int[]     rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JINT    , JTYPE_INT    , aSource, aTag, aComm);}
        public static void MPI_Recv(long[]    rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JLONG   , JTYPE_LONG   , aSource, aTag, aComm);}
        public static void MPI_Recv(float[]   rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(rBuf, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aTag, aComm);}
        public static void MPI_Recv(int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(null, 0, MPI_BYTE, JTYPE_NULL, aSource, aTag, aComm);}
        private native static void MPI_Recv0(Object rBuf, int aCount, long aDataType, int aJDataType, int aSource, int aTag, long aComm) throws MPIException;
        /** 常用操作提供一个基础类型的收发，可以避免冗余的数组创建 */
        public native static byte    MPI_RecvB(int aSource, int aTag, long aComm) throws MPIException;
        public native static double  MPI_RecvD(int aSource, int aTag, long aComm) throws MPIException;
        public native static boolean MPI_RecvZ(int aSource, int aTag, long aComm) throws MPIException;
        public native static char    MPI_RecvC(int aSource, int aTag, long aComm) throws MPIException;
        public native static short   MPI_RecvS(int aSource, int aTag, long aComm) throws MPIException;
        public native static int     MPI_RecvI(int aSource, int aTag, long aComm) throws MPIException;
        public native static long    MPI_RecvL(int aSource, int aTag, long aComm) throws MPIException;
        public native static float   MPI_RecvF(int aSource, int aTag, long aComm) throws MPIException;
        
        /**
         * Sends and receives a message.
         *
         * @param aSendBuf The data array to be sent
         * @param aSendCount Number of elements in send buffer.
         * @param aDest Rank of destination.
         * @param aSendTag Send tag.
         * @param rRecvBuf The data array to be received
         * @param aRecvCount Number of elements in receive buffer.
         * @param aSource Rank of source.
         * @param aRecvTag Receive tag.
         * @param aComm Communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-sendrecv-function"> MPI_Sendrecv function </a>
         */
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        private native static void MPI_Sendrecv0(Object aSendBuf, int aSendCount, long aSendType, int aSendJType, int aDest, int aSendTag, Object rRecvBuf, int aRecvCount, long aRecvType, int aRecvJType, int aSource, int aRecvTag, long aComm) throws MPIException;
        
        
        
        /// MPI External Functions
        /**
         * Initializes the calling MPI process’s execution environment for threaded execution.
         * @param aArgs The argument list for the program
         * @param aRequired The level of desired thread support. Multiple MPI processes
         *                  in the same job may use different values.
         * @return The level of provided thread support.
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-init-thread-function"> MPI_Init_thread function </a>
         */
        public native static int MPI_Init_thread(String[] aArgs, int aRequired) throws MPIException;
        
        
        
//        /// MPI Caching Functions
//        @FunctionalInterface public interface MPI_Comm_copy_attr_function {
//            /**
//             * a placeholder for the application-defined function name.
//             * @param aOldComm Original communicator.
//             * @param aCommKeyval Key value.
//             * @param aExtraState Extra state.
//             * @param aAttributeValIn Source attribute value.
//             * @param aAttributeValOut Destination attribute value.
//             * @return if false, then the attribute is deleted in the duplicated communicator.
//             * Otherwise (true), the new attribute value is set to the value returned in
//             * aAttributeValOut.
//             */
//            boolean call(MPI_Comm aOldComm, int aCommKeyval, Object aExtraState, Object aAttributeValIn, Object aAttributeValOut);
//        }
//
//        @FunctionalInterface public interface MPI_Comm_delete_attr_function {
//            /**
//             * a placeholder for the application-defined function name.
//             * @param aComm Communicator.
//             * @param aCommKeyval Key value.
//             * @param aAttributeVal Attribute value.
//             * @param aExtraState Extra state.
//             */
//            void call(MPI_Comm aComm, int aCommKeyval, Object aAttributeVal, Object aExtraState);
//        }
//
//        /**
//         * Creates a new attribute key.
//         * @param aCommCopyAttrFn Copy callback function for keyval.
//         * @param aCommDeleteAttrFn Delete callback function for keyval.
//         * @param aExtraState Extra state for callback functions.
//         * @return Key value for future access.
//         */
//        public native static int MPI_Comm_create_keyval(MPI_Comm_copy_attr_function aCommCopyAttrFn, MPI_Comm_delete_attr_function aCommDeleteAttrFn, Object aExtraState);
    }
}
