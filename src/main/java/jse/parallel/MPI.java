package jse.parallel;

import jse.cache.ByteArrayCache;
import jse.clib.*;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.math.IDataShell;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static jse.clib.JNIUtil.*;
import static jse.code.OS.*;
import static jse.code.CS.VERSION;
import static jse.code.CS.ZL_STR;

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
         * 自定义构建 mpijni 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_MPI");
        
        /**
         * 自定义构建 mpijni 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_MPI"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_MPI", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_MPI"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_MPI"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 是否在通讯之前拷贝一份 java array，在 {@code 3.8.0} 到 {@code 3.12.2}
         * 期间为了性能会关闭拷贝，这在某些时候似乎会导致错误或死锁，因此现在默认保持开启。
         */
        public static boolean COPY_JARRAY = OS.envZ("JSE_COPY_JARRAY_MPI", true);
        
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
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCount parameters and the data type will be
         *                 detected automatically.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgather-function"> MPI_Allgather function </a>
         */
        public void allgather(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf) throws MPIException {Native.MPI_Allgather(aSendBuf, rRecvBuf, mPtr);}
        public void allgather(IDataShell<?> rBuf) throws MPIException {Native.MPI_Allgather(rBuf, mPtr);}
        public void allgather(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, mPtr);}
        public void allgather(byte[]    rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        public void allgather(double[]  rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        public void allgather(boolean[] rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        public void allgather(char[]    rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        public void allgather(short[]   rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        public void allgather(int[]     rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        public void allgather(long[]    rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        public void allgather(float[]   rBuf, int aStart, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aStart, aCount, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void allgather(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, rRecvBuf, aCount, mPtr);}
        public void allgather(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount) throws MPIException {Native.MPI_Allgather(aSendBuf, rRecvBuf, aCount, mPtr);}
        public void allgather(CPointer       aSendBuf, DoubleCPointer rRecvBuf, int aCount, Datatype aDataType) throws MPIException {Native.MPI_Allgather(aSendBuf, rRecvBuf, aCount, aDataType.mPtr, mPtr);}
        public void allgather(DoubleCPointer rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(IntCPointer    rBuf, int aCount) throws MPIException {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(CPointer       rBuf, int aCount, Datatype aDataType) throws MPIException {Native.MPI_Allgather(rBuf, aCount, aDataType.mPtr, mPtr);}
        
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
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aRecvCounts parameters and the data type will be
         *                 detected automatically.
         *
         * @param aCounts The number of data elements from each communicator process in the send or receive buffer.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgatherv-function"> MPI_Allgatherv function </a>
         */
        public void allgatherv(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, rRecvBuf, aCounts, mPtr);}
        public void allgatherv(IDataShell<?> rBuf, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, mPtr);}
        public void allgatherv(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, mPtr);}
        public void allgatherv(byte[]    rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        public void allgatherv(double[]  rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        public void allgatherv(boolean[] rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        public void allgatherv(char[]    rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        public void allgatherv(short[]   rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        public void allgatherv(int[]     rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        public void allgatherv(long[]    rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        public void allgatherv(float[]   rBuf, int aStart, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aStart, aCounts, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void allgatherv(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, rRecvBuf, aCounts, mPtr);}
        public void allgatherv(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(aSendBuf, rRecvBuf, aCounts, mPtr);}
        public void allgatherv(CPointer       aSendBuf, CPointer       rRecvBuf, int[] aCounts, Datatype aDataType) throws MPIException {Native.MPI_Allgatherv(aSendBuf, rRecvBuf, aCounts, aDataType.mPtr, mPtr);}
        public void allgatherv(DoubleCPointer rBuf, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, mPtr);}
        public void allgatherv(IntCPointer    rBuf, int[] aCounts) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, mPtr);}
        public void allgatherv(CPointer       rBuf, int[] aCounts, Datatype aDataType) throws MPIException {Native.MPI_Allgatherv(rBuf, aCounts, aDataType.mPtr, mPtr);}
        
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
         * @param aOp The MPI_Op handle indicating the global reduction operation to perform.
         *            The handle can indicate a built-in or application-defined operation.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allreduce-function"> MPI_Allreduce function </a>
         */
        public void allreduce(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aOp.mPtr, mPtr);}
        public void allreduce(IDataShell<?> rBuf, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aOp.mPtr, mPtr);}
        public void allreduce(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(byte[]    rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(double[]  rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(boolean[] rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(char[]    rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(short[]   rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(int[]     rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(long[]    rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        public void allreduce(float[]   rBuf, int aStart, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aStart, aCount, aOp.mPtr, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void allreduce(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, Datatype aDataType, Op aOp) throws MPIException {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aDataType.mPtr, aOp.mPtr, mPtr);}
        public void allreduce(DoubleCPointer rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(IntCPointer    rBuf, int aCount, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(CPointer       rBuf, int aCount, Datatype aDataType, Op aOp) throws MPIException {Native.MPI_Allreduce(rBuf, aCount, aDataType.mPtr, aOp.mPtr, mPtr);}
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
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-bcast-function"> MPI_Bcast function </a>
         */
        public void bcast(IDataShell<?> rBuf, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aRoot, mPtr);}
        public void bcast(byte[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        public void bcast(double[]  rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        public void bcast(boolean[] rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        public void bcast(char[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        public void bcast(short[]   rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        public void bcast(int[]     rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        public void bcast(long[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        public void bcast(float[]   rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aStart, aCount, aRoot, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void bcast(DoubleCPointer rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(IntCPointer    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(CPointer       rBuf, int aCount, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Bcast(rBuf, aCount, aDataType.mPtr, aRoot, mPtr);}
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
                bcast(tBytes, 0, tBytes.length, aRoot);
                return aStr;
            } else {
                final int tLen = bcastI(-1, aRoot);
                byte[] rBytes = ByteArrayCache.getArray(tLen);
                try {
                    bcast(rBytes, 0, tLen, aRoot);
                    return UT.Serial.bytes2str(rBytes, 0, tLen);
                } finally {
                    ByteArrayCache.returnArray(rBytes);
                }
            }
        }
        
        /**
         * Gathers data from all members of a group to one member.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gather-function"> MPI_Gather function </a>
         */
        public void gather(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, rRecvBuf, aRoot, mPtr);}
        public void gather(IDataShell<?> rBuf, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aRoot, mPtr);}
        public void gather(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void gather(byte[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        public void gather(double[]  rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        public void gather(boolean[] rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        public void gather(char[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        public void gather(short[]   rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        public void gather(int[]     rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        public void gather(long[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        public void gather(float[]   rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aStart, aCount, aRoot, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void gather(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Gather(aSendBuf, rRecvBuf, aCount, aDataType.mPtr, aRoot, mPtr);}
        public void gather(DoubleCPointer rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(IntCPointer    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(CPointer       rBuf, int aCount, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Gather(rBuf, aCount, aDataType.mPtr, aRoot, mPtr);}
        
        /**
         * Gathers variable data from all members of a group to one member.
         * The gatherv function adds flexibility to the {@link MPI.Comm#gather} function by
         * allowing a varying count of data from each process.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aCounts The number of elements that is received from each process. Each element in the array
         *                corresponds to the rank of the sending process. If the count is zero, the data part of
         *                the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gatherv-function"> MPI_Gatherv function </a>
         */
        public void gatherv(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, rRecvBuf, aCounts, aRoot, mPtr);}
        public void gatherv(IDataShell<?> rBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aRoot, mPtr);}
        public void gatherv(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void gatherv(byte[]    rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void gatherv(double[]  rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void gatherv(boolean[] rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void gatherv(char[]    rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void gatherv(short[]   rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void gatherv(int[]     rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void gatherv(long[]    rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void gatherv(float[]   rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aStart, aCounts, aRoot, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void gatherv(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, rRecvBuf, aCounts, aRoot, mPtr);}
        public void gatherv(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, rRecvBuf, aCounts, aRoot, mPtr);}
        public void gatherv(CPointer       aSendBuf, CPointer       rRecvBuf, int[] aCounts, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Gatherv(aSendBuf, rRecvBuf, aCounts, aDataType.mPtr, aRoot, mPtr);}
        public void gatherv(DoubleCPointer rBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aRoot, mPtr);}
        public void gatherv(IntCPointer    rBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aRoot, mPtr);}
        public void gatherv(CPointer       rBuf, int[] aCounts, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Gatherv(rBuf, aCounts, aDataType.mPtr, aRoot, mPtr);}
        
        /**
         * Performs a global reduce operation across all members of a group. You can specify
         * a predefined mathematical or logical operation or an application-defined operation.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param rRecvBuf The data array to receive the result of the reduction operation.
         *                 This parameter is significant only at the root process.
         *
         * @param aOp The MPI_Op handle indicating the global reduction operation to perform.
         *            The handle can indicate a built-in or application-defined operation.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-reduce-function"> MPI_Reduce function </a>
         */
        public void reduce(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aOp.mPtr, aRoot, mPtr);}
        public void reduce(IDataShell<?> rBuf, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aOp.mPtr, aRoot, mPtr);}
        public void reduce(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(byte[]    rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(double[]  rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(boolean[] rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(char[]    rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(short[]   rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(int[]     rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(long[]    rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(float[]   rBuf, int aStart, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aStart, aCount, aOp.mPtr, aRoot, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void reduce(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, Datatype aDataType, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aDataType.mPtr, aOp.mPtr, aRoot, mPtr);}
        public void reduce(DoubleCPointer rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(IntCPointer    rBuf, int aCount, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(CPointer       rBuf, int aCount, Datatype aDataType, Op aOp, int aRoot) throws MPIException {Native.MPI_Reduce(rBuf, aCount, aDataType.mPtr, aOp.mPtr, aRoot, mPtr);}
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
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aRoot The rank of the sending process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatter-function"> MPI_Scatter function </a>
         */
        public void scatter(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, rRecvBuf, aRoot, mPtr);}
        public void scatter(IDataShell<?> rBuf, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aRoot, mPtr);}
        public void scatter(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, aRoot, mPtr);}
        public void scatter(byte[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        public void scatter(double[]  rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        public void scatter(boolean[] rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        public void scatter(char[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        public void scatter(short[]   rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        public void scatter(int[]     rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        public void scatter(long[]    rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        public void scatter(float[]   rBuf, int aStart, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aStart, aCount, aRoot, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void scatter(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(CPointer       aSendBuf, CPointer rRecvBuf, int aCount, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Scatter(aSendBuf, rRecvBuf, aCount, aDataType.mPtr, aRoot, mPtr);}
        public void scatter(DoubleCPointer rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(IntCPointer    rBuf, int aCount, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(CPointer       rBuf, int aCount, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Scatter(rBuf, aCount, aDataType.mPtr, aRoot, mPtr);}
        
        /**
         * Scatters data from one member across all members of a group.
         * The MPI_Scatterv function performs the inverse of the operation
         * that is performed by the {@link #gatherv} function.
         *
         * @param aSendBuf The data array to be sent by the root process.
         *                 <p>
         *                 The aSendBuf parameter is ignored for all non-root processes.
         *
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aCounts The number of elements in the buffer that is specified in the sendbuf parameter.
         *                If counts[i] is zero, the data part of the message for that process is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatterv-function"> MPI_Scatterv function </a>
         */
        public void scatterv(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, rRecvBuf, aCounts, aRoot, mPtr);}
        public void scatterv(IDataShell<?> rBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aRoot, mPtr);}
        public void scatterv(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCounts, aRoot, mPtr);}
        public void scatterv(byte[]    rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void scatterv(double[]  rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void scatterv(boolean[] rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void scatterv(char[]    rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void scatterv(short[]   rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void scatterv(int[]     rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void scatterv(long[]    rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        public void scatterv(float[]   rBuf, int aStart, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aStart, aCounts, aRoot, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void scatterv(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, rRecvBuf, aCounts, aRoot, mPtr);}
        public void scatterv(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, rRecvBuf, aCounts, aRoot, mPtr);}
        public void scatterv(CPointer       aSendBuf, CPointer       rRecvBuf, int[] aCounts, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Scatterv(aSendBuf, rRecvBuf, aCounts, aDataType.mPtr, aRoot, mPtr);}
        public void scatterv(DoubleCPointer rBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aRoot, mPtr);}
        public void scatterv(IntCPointer    rBuf, int[] aCounts, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aRoot, mPtr);}
        public void scatterv(CPointer       rBuf, int[] aCounts, Datatype aDataType, int aRoot) throws MPIException {Native.MPI_Scatterv(rBuf, aCounts, aDataType.mPtr, aRoot, mPtr);}
        
        
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
         * @param aDest The rank of the destination process within the communicator that is
         *              specified by the comm parameter.
         *
         * @param aTag The message tag that can be used to distinguish different types of messages.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-send-function"> MPI_Send function </a>
         */
        public void send(IDataShell<?> aBuf, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aDest, aTag, mPtr);}
        public void send(byte[]    aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(double[]  aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(boolean[] aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(char[]    aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(short[]   aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(int[]     aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(long[]    aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(float[]   aBuf, int aStart, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aStart, aCount, aDest, aTag, mPtr);}
        public void send(int aDest, int aTag) throws MPIException {Native.MPI_Send(aDest, aTag, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void send(DoubleCPointer aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(IntCPointer    aBuf, int aCount, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(CPointer       aBuf, int aCount, Datatype aDataType, int aDest, int aTag) throws MPIException {Native.MPI_Send(aBuf, aCount, aDataType.mPtr, aDest, aTag, mPtr);}
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
            send(tBytes, 0, tBytes.length, aDest, aTag);
        }
        
        /**
         * Performs a receive operation and does not return until a matching message is received.
         *
         * @param rBuf The data array to be received
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
        public void recv(IDataShell<?> rBuf, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aSource, aTag, mPtr);}
        public void recv(byte[]    rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(double[]  rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(boolean[] rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(char[]    rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(short[]   rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(int[]     rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(long[]    rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(float[]   rBuf, int aStart, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aStart, aCount, aSource, aTag, mPtr);}
        public void recv(int aSource, int aTag) throws MPIException {Native.MPI_Recv(aSource, aTag, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void recv(DoubleCPointer rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(IntCPointer    rBuf, int aCount, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(CPointer       rBuf, int aCount, Datatype aDataType, int aSource, int aTag) throws MPIException {Native.MPI_Recv(rBuf, aCount, aDataType.mPtr, aSource, aTag, mPtr);}
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
                recv(rBytes, 0, tLen, aSource, aTag);
                return UT.Serial.bytes2str(rBytes, 0, tLen);
            } finally {
                ByteArrayCache.returnArray(rBytes);
            }
        }
        
        /**
         * Sends and receives a message.
         *
         * @param aSendBuf The data array to be sent
         * @param aDest Rank of destination.
         * @param aSendTag Send tag.
         * @param rRecvBuf The data array to be received
         * @param aSource Rank of source.
         * @param aRecvTag Receive tag.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-sendrecv-function"> MPI_Sendrecv function </a>
         */
        public void sendrecv(IDataShell<?> aSendBuf, int aDest, int aSendTag, IDataShell<?> rRecvBuf, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aDest, aSendTag, rRecvBuf, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendStart, aSendCount, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, aSource, aRecvTag, mPtr);}
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public void sendrecv(DoubleCPointer aSendBuf, int aSendCount, int aDest, int aSendTag, DoubleCPointer rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(DoubleCPointer aSendBuf, int aSendCount, int aDest, int aSendTag, IntCPointer    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(IntCPointer    aSendBuf, int aSendCount, int aDest, int aSendTag, DoubleCPointer rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(IntCPointer    aSendBuf, int aSendCount, int aDest, int aSendTag, IntCPointer    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(CPointer       aSendBuf, int aSendCount, Datatype aSendType, int aDest, int aSendTag, CPointer rRecvBuf, int aRecvCount, Datatype aRecvType, int aSource, int aRecvTag) throws MPIException {Native.MPI_Sendrecv(aSendBuf, aSendCount, aSendType.mPtr, aDest, aSendTag, rRecvBuf, aRecvCount, aRecvType.mPtr, aSource, aRecvTag, mPtr);}
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
          NULL          (Native.MPI_DATATYPE_NULL )
        , JBYTE         (Native.MPI_JBYTE         )
        , JDOUBLE       (Native.MPI_JDOUBLE       )
        , JBOOLEAN      (Native.MPI_JBOOLEAN      )
        , JCHAR         (Native.MPI_JCHAR         )
        , JSHORT        (Native.MPI_JSHORT        )
        , JINT          (Native.MPI_JINT          )
        , JLONG         (Native.MPI_JLONG         )
        , JFLOAT        (Native.MPI_JFLOAT        )
        , CHAR          (Native.MPI_CHAR          )
        , UNSIGNED_CHAR (Native.MPI_UNSIGNED_CHAR )
        , SHORT         (Native.MPI_SHORT         )
        , UNSIGNED_SHORT(Native.MPI_UNSIGNED_SHORT)
        , INT           (Native.MPI_INT           )
        , UNSIGNED      (Native.MPI_UNSIGNED      )
        , LONG          (Native.MPI_LONG          )
        , UNSIGNED_LONG (Native.MPI_UNSIGNED_LONG )
        , LONG_LONG     (Native.MPI_LONG_LONG     )
        , FLOAT         (Native.MPI_FLOAT         )
        , DOUBLE        (Native.MPI_DOUBLE        )
        , BYTE          (Native.MPI_BYTE          )
        , SIGNED_CHAR   (Native.MPI_SIGNED_CHAR   )
        , UNSIGNED_LONG_LONG(Native.MPI_UNSIGNED_LONG_LONG)
        , INT8_T        (Native.MPI_INT8_T        )
        , INT16_T       (Native.MPI_INT16_T       )
        , INT32_T       (Native.MPI_INT32_T       )
        , INT64_T       (Native.MPI_INT64_T       )
        , UINT8_T       (Native.MPI_UINT8_T       )
        , UINT16_T      (Native.MPI_UINT16_T      )
        , UINT32_T      (Native.MPI_UINT32_T      )
        , UINT64_T      (Native.MPI_UINT64_T      )
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
        
        public final static String MPIJNI_LIB_DIR;
        public final static String MPIJNI_LIB_PATH;
        private final static String[] MPIJNI_SRC_NAME = {
              "jse_parallel_MPI_Native.c"
            , "jse_parallel_MPI_Native.h"
        };
        
        // 直接进行初始化，虽然原则上会在 MPI_Init() 之前获取，
        // 但是得到的是 final 值，可以避免意外的修改，并且简化代码；
        // 这对于一般的 MPI 实现应该都是没有问题的
        static {
            InitHelper.INITIALIZED = true;
            
            // 依赖 MPICore
            MPICore.InitHelper.init();
            if (!MPICore.VALID) throw new RuntimeException("No MPI support.");
            // 先添加 Conf.CMAKE_SETTING，这样保证确定的优先级
            Map<String, String> rCmakeSetting = new LinkedHashMap<>(Conf.CMAKE_SETTING);
            rCmakeSetting.put("JSE_COPY_JARRAY", Conf.COPY_JARRAY ? "ON" : "OFF");
            // 不同 MPI 路径采用独立库
            MPIJNI_LIB_DIR = JAR_DIR+"mpi/" + UT.Code.uniqueID(OS.OS_NAME, JAVA_HOME, VERSION, MPICore.EXE_PATH, Conf.USE_MIMALLOC, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, rCmakeSetting) + "/";
            // 现在直接使用 JNIUtil.buildLib 来统一初始化
            MPIJNI_LIB_PATH = new JNIUtil.LibBuilder("mpijni", "MPI", MPIJNI_LIB_DIR, rCmakeSetting)
                .setSrc("mpi", MPIJNI_SRC_NAME)
                .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
                .setUseMiMalloc(Conf.USE_MIMALLOC).setRedirectLibPath(Conf.REDIRECT_MPIJNI_LIB)
                .get();
            // 设置库路径
            System.load(IO.toAbsolutePath(MPIJNI_LIB_PATH));
            
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
        public final static long[] JTYPE_TO_MPI_TYPE = {MPI_JNULL, MPI_JBYTE, MPI_JDOUBLE, MPI_JBOOLEAN, MPI_JCHAR, MPI_JSHORT, MPI_JINT, MPI_JLONG, MPI_JFLOAT};
        
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
        
        
        static void rangeCheck(int jArraySize, int aCount) {
            if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
        }
        static int totalCount(int[] aCounts, int aCommSize) {
            int tCount = 0;
            for (int i = 0; i < aCommSize; ++i) tCount += aCounts[i];
            return tCount;
        }
        
        /// MPI Collective Functions
        /**
         * Gathers data from all members of a group and sends the data to all members of the group.
         * The MPI_Allgather function is similar to the {@link #MPI_Gather} function, except that it sends
         * the data to all processes instead of only to the root. The usage rules for MPI_Allgather
         * correspond to the rules for {@link #MPI_Gather}.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer and the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer and the data type will be detected automatically.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgather-function"> MPI_Allgather function </a>
         */
        public static void MPI_Allgather(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, long aComm) throws MPIException {
            int tSendSize = aSendBuf.internalDataSize();
            Object tSendBuf = aSendBuf.internalDataWithLengthCheck();
            int tSendJType = jarrayType(tSendBuf);
            final int tCommSize = MPI_Comm_size(aComm);
            Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck(tSendSize*tCommSize);
            int tRecvJType = jarrayType(tRecvBuf);
            if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
            MPI_Allgather0(false, tSendBuf, aSendBuf.internalDataShift(), tSendSize, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                           tRecvBuf, rRecvBuf.internalDataShift(), tSendSize, JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aComm);
        }
        public static void MPI_Allgather(IDataShell<?> rBuf, long aComm) throws MPIException {
            int tSize = rBuf.internalDataSize();
            final int tCommSize = MPI_Comm_size(aComm);
            int tCount = tSize / tCommSize;
            if (tCount*tCommSize != tSize) throw new IllegalArgumentException("Buf size ("+tSize+") cannot be divided by comm size ("+tCommSize+")");
            Object tBuf = rBuf.internalDataWithLengthCheck();
            int tJType = jarrayType(tBuf);
            MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL,
                           tBuf, rBuf.internalDataShift(), tCount, JTYPE_TO_MPI_TYPE[tJType], tJType, aComm);
        }
        public static void MPI_Allgather(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgather(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgather(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgather(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgather(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgather(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvStart, aCount, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgather(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvStart, aCount, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgather(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart); MPI_Allgather0(false, aSendBuf, aSendStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        public static void MPI_Allgather(byte[]    rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgather(double[]  rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgather(boolean[] rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgather(char[]    rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgather(short[]   rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgather(int[]     rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgather(long[]    rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgather(float[]   rBuf, int aStart, int aCount, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Allgather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        private native static void MPI_Allgather0(boolean aInPlace, Object aSendBuf, int aSendStart, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvStart, int aRecvCount, long aRecvType, int aRecvJType, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Allgather(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, long aComm) throws MPIException {MPI_Allgather1(false, aSendBuf.ptr_(), aCount, MPI_DOUBLE, rRecvBuf.ptr_(), aCount, MPI_DOUBLE, aComm);}
        public static void MPI_Allgather(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, long aComm) throws MPIException {MPI_Allgather1(false, aSendBuf.ptr_(), aCount, MPI_INT   , rRecvBuf.ptr_(), aCount, MPI_INT   , aComm);}
        public static void MPI_Allgather(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, long aDataType, long aComm) throws MPIException {MPI_Allgather1(false, aSendBuf.ptr_(), aCount, aDataType, rRecvBuf.ptr_(), aCount, aDataType, aComm);}
        public static void MPI_Allgather(DoubleCPointer rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, MPI_DOUBLE, aComm);}
        public static void MPI_Allgather(IntCPointer    rBuf, int aCount, long aComm) throws MPIException {MPI_Allgather1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, MPI_INT   , aComm);}
        public static void MPI_Allgather(CPointer       rBuf, int aCount, long aDataType, long aComm) throws MPIException {MPI_Allgather1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, aDataType, aComm);}
        private native static void MPI_Allgather1(boolean aInPlace, long aSendBuf, int aSendCount, long aSendType, long rRecvBuf, int aRecvCount, long aRecvType, long aComm) throws MPIException;
        
        /**
         * Gathers a variable amount of data from each member of a group and sends the data to all members of the group.
         * The MPI_Allgatherv function is like the {@link #MPI_Gatherv}, except that all processes receive the result,
         * instead of just the root. The block of data that is sent from the jth process is received by every process
         * and placed in the jth block of the buffer rRecvBuf. These blocks do not all have to be the same size.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer and the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *
         * @param rRecvBuf The data array that is received from each process. The number of the elements in
         *                 the buffer are specified in the aCounts parameters and the data type will be
         *                 detected automatically.
         *
         * @param aCounts The number of data elements from each communicator process in the send or receive buffer.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allgatherv-function"> MPI_Allgatherv function </a>
         */
        public static void MPI_Allgatherv(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int[] aCounts, long aComm) throws MPIException {
            final int tCommRank = MPI_Comm_rank(aComm);
            Object tSendBuf = aSendBuf.internalDataWithLengthCheck(aCounts[tCommRank]);
            int tSendJType = jarrayType(tSendBuf);
            Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck(totalCount(aCounts, MPI_Comm_size(aComm)));
            int tRecvJType = jarrayType(tRecvBuf);
            if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
            MPI_Allgatherv0(false, tSendBuf, aSendBuf.internalDataShift(), aCounts[tCommRank], JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                            tRecvBuf, rRecvBuf.internalDataShift(), aCounts, JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aComm);
        }
        public static void MPI_Allgatherv(IDataShell<?> rBuf, int[] aCounts, long aComm) throws MPIException {
            Object tBuf = rBuf.internalDataWithLengthCheck(totalCount(aCounts, MPI_Comm_size(aComm)));
            int tJType = jarrayType(tBuf);
            MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL,
                            tBuf, rBuf.internalDataShift(), aCounts, JTYPE_TO_MPI_TYPE[tJType], tJType, aComm);
        }
        public static void MPI_Allgatherv(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvStart, aCounts, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgatherv(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvStart, aCounts, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgatherv(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvStart, aCounts, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgatherv(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvStart, aCounts, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgatherv(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvStart, aCounts, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgatherv(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvStart, aCounts, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgatherv(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvStart, aCounts, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgatherv(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCounts[MPI_Comm_rank(aComm)]+aSendStart); rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart); MPI_Allgatherv0(false, aSendBuf, aSendStart, aCounts[MPI_Comm_rank(aComm)], MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvStart, aCounts, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        public static void MPI_Allgatherv(byte[]    rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JBYTE   , JTYPE_BYTE   , aComm);}
        public static void MPI_Allgatherv(double[]  rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JDOUBLE , JTYPE_DOUBLE , aComm);}
        public static void MPI_Allgatherv(boolean[] rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JBOOLEAN, JTYPE_BOOLEAN, aComm);}
        public static void MPI_Allgatherv(char[]    rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JCHAR   , JTYPE_CHAR   , aComm);}
        public static void MPI_Allgatherv(short[]   rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JSHORT  , JTYPE_SHORT  , aComm);}
        public static void MPI_Allgatherv(int[]     rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JINT    , JTYPE_INT    , aComm);}
        public static void MPI_Allgatherv(long[]    rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JLONG   , JTYPE_LONG   , aComm);}
        public static void MPI_Allgatherv(float[]   rBuf, int aStart, int[] aCounts, long aComm) throws MPIException {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Allgatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JFLOAT  , JTYPE_FLOAT  , aComm);}
        private native static void MPI_Allgatherv0(boolean aInPlace, Object aSendBuf, int aSendStart, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvStart, int[] aRecvCounts, long aRecvType, int aRecvJType, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Allgatherv(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int[] aCounts, long aComm) throws MPIException {MPI_Allgatherv1(false, aSendBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], MPI_DOUBLE, rRecvBuf.ptr_(), aCounts, MPI_DOUBLE, aComm);}
        public static void MPI_Allgatherv(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int[] aCounts, long aComm) throws MPIException {MPI_Allgatherv1(false, aSendBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], MPI_INT   , rRecvBuf.ptr_(), aCounts, MPI_INT   , aComm);}
        public static void MPI_Allgatherv(CPointer       aSendBuf, CPointer       rRecvBuf, int[] aCounts, long aDataType, long aComm) throws MPIException {MPI_Allgatherv1(false, aSendBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], aDataType, rRecvBuf.ptr_(), aCounts, aDataType, aComm);}
        public static void MPI_Allgatherv(DoubleCPointer rBuf, int[] aCounts, long aComm) throws MPIException {MPI_Allgatherv1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts, MPI_DOUBLE, aComm);}
        public static void MPI_Allgatherv(IntCPointer    rBuf, int[] aCounts, long aComm) throws MPIException {MPI_Allgatherv1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts, MPI_INT   , aComm);}
        public static void MPI_Allgatherv(CPointer       rBuf, int[] aCounts, long aDataType, long aComm) throws MPIException {MPI_Allgatherv1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts, aDataType, aComm);}
        private native static void MPI_Allgatherv1(boolean aInPlace, long aSendBuf, int aSendCount, long aSendType, long rRecvBuf, int[] aRecvCounts, long aRecvType, long aComm) throws MPIException;
        
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
         * @param aOp The MPI_Op handle indicating the global reduction operation to perform.
         *            The handle can indicate a built-in or application-defined operation.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-allreduce-function"> MPI_Allreduce function </a>
         */
        public static void MPI_Allreduce(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, long aOp, long aComm) throws MPIException {
            int tSendSize = aSendBuf.internalDataSize();
            Object tSendBuf = aSendBuf.internalDataWithLengthCheck();
            int tSendJType = jarrayType(tSendBuf);
            Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck(tSendSize);
            int tRecvJType = jarrayType(tRecvBuf);
            if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
            MPI_Allreduce0(false, tSendBuf, aSendBuf.internalDataShift(), tRecvBuf, rRecvBuf.internalDataShift(),
                           tSendSize, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType, aOp, aComm);
        }
        public static void MPI_Allreduce(IDataShell<?> rBuf, long aOp, long aComm) throws MPIException {
            Object tBuf = rBuf.internalDataWithLengthCheck();
            int tJType = jarrayType(tBuf);
            MPI_Allreduce0(true, null, 0, tBuf, rBuf.internalDataShift(),
                           rBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType, aOp, aComm);
        }
        public static void MPI_Allreduce(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aComm);}
        public static void MPI_Allreduce(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aComm);}
        public static void MPI_Allreduce(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aComm);}
        public static void MPI_Allreduce(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aComm);}
        public static void MPI_Allreduce(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aComm);}
        public static void MPI_Allreduce(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JINT    , JTYPE_INT    , aOp, aComm);}
        public static void MPI_Allreduce(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aComm);}
        public static void MPI_Allreduce(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Allreduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aComm);}
        public static void MPI_Allreduce(byte[]    rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aComm);}
        public static void MPI_Allreduce(double[]  rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aComm);}
        public static void MPI_Allreduce(boolean[] rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aComm);}
        public static void MPI_Allreduce(char[]    rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aComm);}
        public static void MPI_Allreduce(short[]   rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aComm);}
        public static void MPI_Allreduce(int[]     rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aOp, aComm);}
        public static void MPI_Allreduce(long[]    rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aComm);}
        public static void MPI_Allreduce(float[]   rBuf, int aStart, int aCount, long aOp, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Allreduce0(true, null, 0, rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aComm);}
        private native static void MPI_Allreduce0(boolean aInPlace, Object aSendBuf, int aSendStart, Object rRecvBuf, int aRecvStart, int aCount, long aDataType, int aJDataType, long aOp, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Allreduce(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce1(false, aSendBuf.ptr_(), rRecvBuf.ptr_(), aCount, MPI_DOUBLE, aOp, aComm);}
        public static void MPI_Allreduce(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce1(false, aSendBuf.ptr_(), rRecvBuf.ptr_(), aCount, MPI_INT   , aOp, aComm);}
        public static void MPI_Allreduce(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, long aDataType, long aOp, long aComm) throws MPIException {MPI_Allreduce1(false, aSendBuf.ptr_(), rRecvBuf.ptr_(), aCount, aDataType, aOp, aComm);}
        public static void MPI_Allreduce(DoubleCPointer rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce1(true, 0, rBuf.ptr_(), aCount, MPI_DOUBLE, aOp, aComm);}
        public static void MPI_Allreduce(IntCPointer    rBuf, int aCount, long aOp, long aComm) throws MPIException {MPI_Allreduce1(true, 0, rBuf.ptr_(), aCount, MPI_INT   , aOp, aComm);}
        public static void MPI_Allreduce(CPointer       rBuf, int aCount, long aDataType, long aOp, long aComm) throws MPIException {MPI_Allreduce1(true, 0, rBuf.ptr_(), aCount, aDataType, aOp, aComm);}
        private native static void MPI_Allreduce1(boolean aInPlace, long aSendBuf, long rRecvBuf, int aCount, long aDataType, long aOp, long aComm) throws MPIException;
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
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-bcast-function"> MPI_Bcast function </a>
         */
        public static void MPI_Bcast(IDataShell<?> rBuf, int aRoot, long aComm) throws MPIException {
            Object tBuf = rBuf.internalDataWithLengthCheck();
            int tJType = jarrayType(tBuf);
            MPI_Bcast0(tBuf, rBuf.internalDataShift(), rBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType, aRoot, aComm);
        }
        public static void MPI_Bcast(byte[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Bcast(double[]  rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Bcast(boolean[] rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Bcast(char[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Bcast(short[]   rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Bcast(int[]     rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Bcast(long[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Bcast(float[]   rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Bcast0(rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        private native static void MPI_Bcast0(Object rBuf, int aStart, int aCount, long aDataType, int aJDataType, int aRoot, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Bcast(DoubleCPointer rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast1(rBuf.ptr_(), aCount, MPI_DOUBLE, aRoot, aComm);}
        public static void MPI_Bcast(IntCPointer    rBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Bcast1(rBuf.ptr_(), aCount, MPI_INT   , aRoot, aComm);}
        public static void MPI_Bcast(CPointer       rBuf, int aCount, long aDataType, int aRoot, long aComm) throws MPIException {MPI_Bcast1(rBuf.ptr_(), aCount, aDataType, aRoot, aComm);}
        private native static void MPI_Bcast1(long rBuf, int aCount, long aDataType, int aRoot, long aComm) throws MPIException;
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
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gather-function"> MPI_Gather function </a>
         */
        public static void MPI_Gather(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int aRoot, long aComm) throws MPIException {
            int tSendSize = aSendBuf.internalDataSize();
            Object tSendBuf = aSendBuf.internalDataWithLengthCheck();
            int tSendJType = jarrayType(tSendBuf);
            if (MPI_Comm_rank(aComm) == aRoot) {
                final int tCommSize = MPI_Comm_size(aComm);
                Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck(tSendSize*tCommSize);
                int tRecvJType = jarrayType(tRecvBuf);
                if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
                MPI_Gather0(false, tSendBuf, aSendBuf.internalDataShift(), tSendSize, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                            tRecvBuf, rRecvBuf.internalDataShift(), tSendSize, JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aRoot, aComm);
            } else {
                MPI_Gather0(false, tSendBuf, aSendBuf.internalDataShift(), tSendSize, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                            null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);
            }
        }
        public static void MPI_Gather(IDataShell<?> rBuf, int aRoot, long aComm) throws MPIException {
            if (MPI_Comm_rank(aComm) == aRoot) {
                int tSize = rBuf.internalDataSize();
                final int tCommSize = MPI_Comm_size(aComm);
                int tCount = tSize / tCommSize;
                if (tCount*tCommSize != tSize) throw new IllegalArgumentException("Buf size ("+tSize+") cannot be divided by comm size ("+tCommSize+")");
                Object tBuf = rBuf.internalDataWithLengthCheck();
                int tJType = jarrayType(tBuf);
                MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL,
                            tBuf, rBuf.internalDataShift(), tCount, JTYPE_TO_MPI_TYPE[tJType], tJType, aRoot, aComm);
            } else {
                Object tBuf = rBuf.internalDataWithLengthCheck();
                int tJType = jarrayType(tBuf);
                MPI_Gather0(false, tBuf, rBuf.internalDataShift(), rBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType,
                            null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);
            }
        }
        public static void MPI_Gather(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Gather(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Gather(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Gather(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Gather(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Gather(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvStart, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Gather(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvStart, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Gather(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rRecvBuf.length, aCount*MPI_Comm_size(aComm)+aRecvStart);} MPI_Gather0(false, aSendBuf, aSendStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Gather(byte[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(double[]  rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(boolean[] rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(char[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(short[]   rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(int[]     rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(long[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(float[]   rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Gather0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Gather0(false, rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gather0(boolean aInPlace, Object aSendBuf, int aSendStart, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvStart, int aRecvCount, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Gather(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Gather1(false, aSendBuf.ptr_(), aCount, MPI_DOUBLE, rRecvBuf.ptr_(), aCount, MPI_DOUBLE, aRoot, aComm);}
        public static void MPI_Gather(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Gather1(false, aSendBuf.ptr_(), aCount, MPI_INT   , rRecvBuf.ptr_(), aCount, MPI_INT   , aRoot, aComm);}
        public static void MPI_Gather(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, long aDataType, int aRoot, long aComm) throws MPIException {MPI_Gather1(false, aSendBuf.ptr_(), aCount, aDataType, rRecvBuf.ptr_(), aCount, aDataType, aRoot, aComm);}
        public static void MPI_Gather(DoubleCPointer rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, MPI_DOUBLE, aRoot, aComm);} else {MPI_Gather1(false, rBuf.ptr_(), aCount, MPI_DOUBLE, 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(IntCPointer    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, MPI_INT   , aRoot, aComm);} else {MPI_Gather1(false, rBuf.ptr_(), aCount, MPI_INT   , 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(CPointer       rBuf, int aCount, long aDataType, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, aDataType, aRoot, aComm);} else {MPI_Gather1(false, rBuf.ptr_(), aCount, aDataType, 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gather1(boolean aInPlace, long aSendBuf, int aSendCount, long aSendType, long rRecvBuf, int aRecvCount, long aRecvType, int aRoot, long aComm) throws MPIException;
        
        /**
         * Gathers variable data from all members of a group to one member.
         * The MPI_Gatherv function adds flexibility to the {@link #MPI_Gather} function by
         * allowing a varying count of data from each process.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param rRecvBuf The data array on the root process that is received from each process.
         *                 It includes data that is sent by the root process. This parameter is significant only at the
         *                 root process. The recvbuf parameter is ignored for all non-root processes.
         *
         * @param aCounts The number of elements that is received from each process. Each element in the array
         *                corresponds to the rank of the sending process. If the count is zero, the data part of
         *                the message is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-gatherv-function"> MPI_Gatherv function </a>
         */
        public static void MPI_Gatherv(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {
            final int tCommRank = MPI_Comm_rank(aComm);
            Object tSendBuf = aSendBuf.internalDataWithLengthCheck(aCounts[tCommRank]);
            int tSendJType = jarrayType(tSendBuf);
            if (tCommRank == aRoot) {
                Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck(totalCount(aCounts, MPI_Comm_size(aComm)));
                int tRecvJType = jarrayType(tRecvBuf);
                if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
                MPI_Gatherv0(false, tSendBuf, aSendBuf.internalDataShift(), aCounts[tCommRank], JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                             tRecvBuf, rRecvBuf.internalDataShift(), aCounts, JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aRoot, aComm);
            } else {
                MPI_Gatherv0(false, tSendBuf, aSendBuf.internalDataShift(), aCounts[tCommRank], JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                             null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);
            }
        }
        public static void MPI_Gatherv(IDataShell<?> rBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {
            final int tCommRank = MPI_Comm_rank(aComm);
            if (tCommRank == aRoot) {
                Object tBuf = rBuf.internalDataWithLengthCheck(totalCount(aCounts, MPI_Comm_size(aComm)));
                int tJType = jarrayType(tBuf);
                MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL,
                             tBuf, rBuf.internalDataShift(), aCounts, JTYPE_TO_MPI_TYPE[tJType], tJType, aRoot, aComm);
            } else {
                Object tBuf = rBuf.internalDataWithLengthCheck(aCounts[tCommRank]);
                int tJType = jarrayType(tBuf);
                MPI_Gatherv0(false, tBuf, rBuf.internalDataShift(), aCounts[tCommRank], JTYPE_TO_MPI_TYPE[tJType], tJType,
                             null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);
            }
        }
        public static void MPI_Gatherv(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvStart, aCounts, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Gatherv(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvStart, aCounts, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Gatherv(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvStart, aCounts, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Gatherv(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvStart, aCounts, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Gatherv(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvStart, aCounts, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Gatherv(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvStart, aCounts, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Gatherv(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvStart, aCounts, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Gatherv(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); rangeCheck(aSendBuf.length, aCounts[tRank]+aSendStart); if (tRank == aRoot) {rangeCheck(rRecvBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aRecvStart);} MPI_Gatherv0(false, aSendBuf, aCounts[tRank], aSendStart, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvStart, aCounts, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Gatherv(byte[]    rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JBYTE   , JTYPE_BYTE   , null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(double[]  rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JDOUBLE , JTYPE_DOUBLE , null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(boolean[] rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JBOOLEAN, JTYPE_BOOLEAN, null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(char[]    rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JCHAR   , JTYPE_CHAR   , null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(short[]   rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JSHORT  , JTYPE_SHORT  , null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(int[]     rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JINT    , JTYPE_INT    , aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JINT    , JTYPE_INT    , null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(long[]    rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JLONG   , JTYPE_LONG   , null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(float[]   rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Gatherv0(true, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Gatherv0(false, rBuf, aStart, aCounts[tRank], MPI_JFLOAT  , JTYPE_FLOAT  , null, 0, null, MPI_JNULL, JTYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gatherv0(boolean aInPlace, Object aSendBuf, int aSendStart, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvStart, int[] aRecvCounts, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Gatherv(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {MPI_Gatherv1(false, aSendBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], MPI_DOUBLE, rRecvBuf.ptr_(), aCounts, MPI_DOUBLE, aRoot, aComm);}
        public static void MPI_Gatherv(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {MPI_Gatherv1(false, aSendBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], MPI_INT   , rRecvBuf.ptr_(), aCounts, MPI_INT   , aRoot, aComm);}
        public static void MPI_Gatherv(CPointer       aSendBuf, CPointer       rRecvBuf, int[] aCounts, long aDataType, int aRoot, long aComm) throws MPIException {MPI_Gatherv1(false, aSendBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], aDataType, rRecvBuf.ptr_(), aCounts, aDataType, aRoot, aComm);}
        public static void MPI_Gatherv(DoubleCPointer rBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts, MPI_DOUBLE, aRoot, aComm);} else {MPI_Gatherv1(false, rBuf.ptr_(), aCounts[tRank], MPI_DOUBLE, 0, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(IntCPointer    rBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts, MPI_INT   , aRoot, aComm);} else {MPI_Gatherv1(false, rBuf.ptr_(), aCounts[tRank], MPI_INT   , 0, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(CPointer       rBuf, int[] aCounts, long aDataType, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv1(true, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts, aDataType, aRoot, aComm);} else {MPI_Gatherv1(false, rBuf.ptr_(), aCounts[tRank], aDataType, 0, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gatherv1(boolean aInPlace, long aSendBuf, int aSendCount, long aSendType, long rRecvBuf, int[] aRecvCounts, long aRecvType, int aRoot, long aComm) throws MPIException;
        
        /**
         * Performs a global reduce operation across all members of a group. You can specify
         * a predefined mathematical or logical operation or an application-defined operation.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *
         * @param rRecvBuf The data array to receive the result of the reduction operation.
         *                 This parameter is significant only at the root process.
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
        public static void MPI_Reduce(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, long aOp, int aRoot, long aComm) throws MPIException {
            int tSendSize = aSendBuf.internalDataSize();
            Object tSendBuf = aSendBuf.internalDataWithLengthCheck();
            int tSendJType = jarrayType(tSendBuf);
            if (MPI_Comm_rank(aComm) == aRoot) {
                Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck(tSendSize);
                int tRecvJType = jarrayType(tRecvBuf);
                if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
                MPI_Reduce0(false, tSendBuf, aSendBuf.internalDataShift(), tRecvBuf, rRecvBuf.internalDataShift(),
                            tSendSize, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType, aOp, aRoot, aComm);
            } else {
                MPI_Reduce0(false, tSendBuf, aSendBuf.internalDataShift(), null, 0,
                            tSendSize, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType, aOp, aRoot, aComm);
            }
        }
        public static void MPI_Reduce(IDataShell<?> rBuf, long aOp, int aRoot, long aComm) throws MPIException {
            Object tBuf = rBuf.internalDataWithLengthCheck();
            int tJType = jarrayType(tBuf);
            if (MPI_Comm_rank(aComm) == aRoot) {
                MPI_Reduce0(true, null, 0, tBuf, rBuf.internalDataShift(),
                            rBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType, aOp, aRoot, aComm);
            } else {
                MPI_Reduce0(false, tBuf, rBuf.internalDataShift(), null, 0,
                            rBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType, aOp, aRoot, aComm);
            }
        }
        public static void MPI_Reduce(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aRoot, aComm);}
        public static void MPI_Reduce(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aRoot, aComm);}
        public static void MPI_Reduce(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aRoot, aComm);}
        public static void MPI_Reduce(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JINT    , JTYPE_INT    , aOp, aRoot, aComm);}
        public static void MPI_Reduce(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Reduce0(false, aSendBuf, aSendStart, rRecvBuf, aRecvStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aRoot, aComm);}
        public static void MPI_Reduce(byte[]    rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JBYTE   , JTYPE_BYTE   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(double[]  rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(boolean[] rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aOp, aRoot, aComm);}}
        public static void MPI_Reduce(char[]    rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JCHAR   , JTYPE_CHAR   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(short[]   rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JSHORT  , JTYPE_SHORT  , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(int[]     rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JINT    , JTYPE_INT    , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(long[]    rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JLONG   , JTYPE_LONG   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(float[]   rBuf, int aStart, int aCount, long aOp, int aRoot, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, 0, rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, aStart, null, 0, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aOp, aRoot, aComm);}}
        private native static void MPI_Reduce0(boolean aInPlace, Object aSendBuf, int aSendStart, Object rRecvBuf, int aRecvStart, int aCount, long aDataType, int aJDataType, long aOp, int aRoot, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Reduce(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce1(false, aSendBuf.ptr_(), rRecvBuf.ptr_(), aCount, MPI_DOUBLE, aOp, aRoot, aComm);}
        public static void MPI_Reduce(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce1(false, aSendBuf.ptr_(), rRecvBuf.ptr_(), aCount, MPI_INT   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, long aDataType, long aOp, int aRoot, long aComm) throws MPIException {MPI_Reduce1(false, aSendBuf.ptr_(), rRecvBuf.ptr_(), aCount, aDataType, aOp, aRoot, aComm);}
        public static void MPI_Reduce(DoubleCPointer rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce1(true, 0, rBuf.ptr_(), aCount, MPI_DOUBLE, aOp, aRoot, aComm);} else {MPI_Reduce1(false, rBuf.ptr_(), 0, aCount, MPI_DOUBLE, aOp, aRoot, aComm);}}
        public static void MPI_Reduce(IntCPointer    rBuf, int aCount, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce1(true, 0, rBuf.ptr_(), aCount, MPI_INT   , aOp, aRoot, aComm);} else {MPI_Reduce1(false, rBuf.ptr_(), 0, aCount, MPI_INT   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(CPointer       rBuf, int aCount, long aDataType, long aOp, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce1(true, 0, rBuf.ptr_(), aCount, aDataType, aOp, aRoot, aComm);} else {MPI_Reduce1(false, rBuf.ptr_(), 0, aCount, aDataType, aOp, aRoot, aComm);}}
        private native static void MPI_Reduce1(boolean aInPlace, long aSendBuf, long rRecvBuf, int aCount, long aDataType, long aOp, int aRoot, long aComm) throws MPIException;
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
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aRoot The rank of the sending process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatter-function"> MPI_Scatter function </a>
         */
        public static void MPI_Scatter(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int aRoot, long aComm) throws MPIException {
            int tRecvSize = rRecvBuf.internalDataSize();
            Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck();
            int tRecvJType = jarrayType(tRecvBuf);
            if (MPI_Comm_rank(aComm) == aRoot) {
                Object tSendBuf = aSendBuf.internalDataWithLengthCheck(tRecvSize*MPI_Comm_size(aComm));
                int tSendJType = jarrayType(tSendBuf);
                if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
                MPI_Scatter0(false, tSendBuf, aSendBuf.internalDataShift(), tRecvSize, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                             tRecvBuf, rRecvBuf.internalDataShift(), tRecvSize, JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aRoot, aComm);
            } else {
                MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL,
                             tRecvBuf, rRecvBuf.internalDataShift(), tRecvSize, JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aRoot, aComm);
            }
        }
        public static void MPI_Scatter(IDataShell<?> rBuf, int aRoot, long aComm) throws MPIException {
            if (MPI_Comm_rank(aComm) == aRoot) {
                int tSize = rBuf.internalDataSize();
                final int tCommSize = MPI_Comm_size(aComm);
                int tCount = tSize / tCommSize;
                if (tCount*tCommSize != tSize) throw new IllegalArgumentException("Buf size ("+tSize+") cannot be divided by comm size ("+tCommSize+")");
                Object tBuf = rBuf.internalDataWithLengthCheck();
                int tJType = jarrayType(tBuf);
                MPI_Scatter0(true, tBuf, rBuf.internalDataShift(), tCount, JTYPE_TO_MPI_TYPE[tJType], tJType,
                             null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);
            } else {
                Object tBuf = rBuf.internalDataWithLengthCheck();
                int tJType = jarrayType(tBuf);
                MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL,
                             tBuf, rBuf.internalDataShift(), rBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType, aRoot, aComm);
            }
        }
        public static void MPI_Scatter(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Scatter(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Scatter(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Scatter(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Scatter(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Scatter(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvStart, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Scatter(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvStart, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Scatter(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int aCount, int aRoot, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aCount*MPI_Comm_size(aComm)+aSendStart); rangeCheck(rRecvBuf.length, aCount+aRecvStart); MPI_Scatter0(false, aSendBuf, aSendStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Scatter(byte[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}}
        public static void MPI_Scatter(double[]  rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}}
        public static void MPI_Scatter(boolean[] rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}}
        public static void MPI_Scatter(char[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}}
        public static void MPI_Scatter(short[]   rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}}
        public static void MPI_Scatter(int[]     rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aRoot, aComm);}}
        public static void MPI_Scatter(long[]    rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}}
        public static void MPI_Scatter(float[]   rBuf, int aStart, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {rangeCheck(rBuf.length, aCount*MPI_Comm_size(aComm)+aStart); MPI_Scatter0(true, rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCount+aStart); MPI_Scatter0(false, null, 0, 0, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}}
        private native static void MPI_Scatter0(boolean aInPlace, Object aSendBuf, int aSendStart, int aSendCount, long aSendType, int aSendJType, Object rRecvBuf, int aRecvStart, int aRecvCount, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Scatter(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Scatter1(false, aSendBuf.ptr_(), aCount, MPI_DOUBLE, rRecvBuf.ptr_(), aCount, MPI_DOUBLE, aRoot, aComm);}
        public static void MPI_Scatter(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int aCount, int aRoot, long aComm) throws MPIException {MPI_Scatter1(false, aSendBuf.ptr_(), aCount, MPI_INT   , rRecvBuf.ptr_(), aCount, MPI_INT   , aRoot, aComm);}
        public static void MPI_Scatter(CPointer       aSendBuf, CPointer       rRecvBuf, int aCount, long aDataType, int aRoot, long aComm) throws MPIException {MPI_Scatter1(false, aSendBuf.ptr_(), aCount, aDataType, rRecvBuf.ptr_(), aCount, aDataType, aRoot, aComm);}
        public static void MPI_Scatter(DoubleCPointer rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter1(true, rBuf.ptr_(), aCount, MPI_DOUBLE, 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter1(false, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, MPI_DOUBLE, aRoot, aComm);}}
        public static void MPI_Scatter(IntCPointer    rBuf, int aCount, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter1(true, rBuf.ptr_(), aCount, MPI_INT   , 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter1(false, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, MPI_INT   , aRoot, aComm);}}
        public static void MPI_Scatter(CPointer       rBuf, int aCount, long aDataType, int aRoot, long aComm) throws MPIException {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter1(true, rBuf.ptr_(), aCount, aDataType, 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter1(false, 0, 0, MPI_DATATYPE_NULL, rBuf.ptr_(), aCount, aDataType, aRoot, aComm);}}
        private native static void MPI_Scatter1(boolean aInPlace, long aSendBuf, int aSendCount, long aSendType, long rRecvBuf, int aRecvCount, long aRecvType, int aRoot, long aComm) throws MPIException;
        
        /**
         * Scatters data from one member across all members of a group.
         * The MPI_Scatterv function performs the inverse of the operation
         * that is performed by the {@link #MPI_Gatherv} function.
         *
         * @param aSendBuf The data array to be sent by the root process.
         *                 <p>
         *                 The aSendBuf parameter is ignored for all non-root processes.
         *
         * @param rRecvBuf The data array that is received on each process. The number and data type of
         *                 the elements in the buffer are specified in the recvcount and recvtype parameters.
         *
         * @param aCounts The number of elements in the buffer that is specified in the sendbuf parameter.
         *                If counts[i] is zero, the data part of the message for that process is empty.
         *
         * @param aRoot The rank of the receiving process within the specified communicator.
         *
         * @param aComm The MPI_Comm communicator handle.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-scatterv-function"> MPI_Scatterv function </a>
         */
        public static void MPI_Scatterv(IDataShell<?> aSendBuf, IDataShell<?> rRecvBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {
            final int tCommRank = MPI_Comm_rank(aComm);
            Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck(aCounts[tCommRank]);
            int tRecvJType = jarrayType(tRecvBuf);
            if (tCommRank == aRoot) {
                Object tSendBuf = aSendBuf.internalDataWithLengthCheck(totalCount(aCounts, MPI_Comm_size(aComm)));
                int tSendJType = jarrayType(tSendBuf);
                if (tSendJType != tRecvJType) throw new IllegalArgumentException("Send & Recv array type mismatch: "+tSendBuf.getClass().getName()+"!="+tRecvBuf.getClass().getName());
                MPI_Scatterv0(false, tSendBuf, aSendBuf.internalDataShift(), aCounts, JTYPE_TO_MPI_TYPE[tSendJType], tSendJType,
                              tRecvBuf, rRecvBuf.internalDataShift(), aCounts[tCommRank], JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aRoot, aComm);
            } else {
                MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL,
                              tRecvBuf, rRecvBuf.internalDataShift(), aCounts[tCommRank], JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aRoot, aComm);
            }
        }
        public static void MPI_Scatterv(IDataShell<?> rBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {
            final int tCommRank = MPI_Comm_rank(aComm);
            if (tCommRank == aRoot) {
                Object tBuf = rBuf.internalDataWithLengthCheck(totalCount(aCounts, MPI_Comm_size(aComm)));
                int tJType = jarrayType(tBuf);
                MPI_Scatterv0(true, tBuf, rBuf.internalDataShift(), aCounts, JTYPE_TO_MPI_TYPE[tJType], tJType,
                              null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);
            } else {
                Object tBuf = rBuf.internalDataWithLengthCheck(aCounts[tCommRank]);
                int tJType = jarrayType(tBuf);
                MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL,
                              tBuf, rBuf.internalDataShift(), aCounts[tCommRank], JTYPE_TO_MPI_TYPE[tJType], tJType, aRoot, aComm);
            }
        }
        public static void MPI_Scatterv(byte[]    aSendBuf, int aSendStart, byte[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JBYTE   , JTYPE_BYTE   , rRecvBuf, aRecvStart, aCounts[tRank], MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}
        public static void MPI_Scatterv(double[]  aSendBuf, int aSendStart, double[]  rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JDOUBLE , JTYPE_DOUBLE , rRecvBuf, aRecvStart, aCounts[tRank], MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}
        public static void MPI_Scatterv(boolean[] aSendBuf, int aSendStart, boolean[] rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JBOOLEAN, JTYPE_BOOLEAN, rRecvBuf, aRecvStart, aCounts[tRank], MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}
        public static void MPI_Scatterv(char[]    aSendBuf, int aSendStart, char[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JCHAR   , JTYPE_CHAR   , rRecvBuf, aRecvStart, aCounts[tRank], MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}
        public static void MPI_Scatterv(short[]   aSendBuf, int aSendStart, short[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JSHORT  , JTYPE_SHORT  , rRecvBuf, aRecvStart, aCounts[tRank], MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}
        public static void MPI_Scatterv(int[]     aSendBuf, int aSendStart, int[]     rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JINT    , JTYPE_INT    , rRecvBuf, aRecvStart, aCounts[tRank], MPI_JINT    , JTYPE_INT    , aRoot, aComm);}
        public static void MPI_Scatterv(long[]    aSendBuf, int aSendStart, long[]    rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JLONG   , JTYPE_LONG   , rRecvBuf, aRecvStart, aCounts[tRank], MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}
        public static void MPI_Scatterv(float[]   aSendBuf, int aSendStart, float[]   rRecvBuf, int aRecvStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(aSendBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aSendStart);} rangeCheck(rRecvBuf.length, aCounts[tRank]+aRecvStart); MPI_Scatterv0(false, aSendBuf, aSendStart, aCounts, MPI_JFLOAT  , JTYPE_FLOAT  , rRecvBuf, aRecvStart, aCounts[tRank], MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}
        public static void MPI_Scatterv(byte[]    rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JBYTE   , JTYPE_BYTE   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JBYTE   , JTYPE_BYTE   , aRoot, aComm);}}
        public static void MPI_Scatterv(double[]  rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JDOUBLE , JTYPE_DOUBLE , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JDOUBLE , JTYPE_DOUBLE , aRoot, aComm);}}
        public static void MPI_Scatterv(boolean[] rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JBOOLEAN, JTYPE_BOOLEAN, null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JBOOLEAN, JTYPE_BOOLEAN, aRoot, aComm);}}
        public static void MPI_Scatterv(char[]    rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JCHAR   , JTYPE_CHAR   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JCHAR   , JTYPE_CHAR   , aRoot, aComm);}}
        public static void MPI_Scatterv(short[]   rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JSHORT  , JTYPE_SHORT  , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JSHORT  , JTYPE_SHORT  , aRoot, aComm);}}
        public static void MPI_Scatterv(int[]     rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JINT    , JTYPE_INT    , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JINT    , JTYPE_INT    , aRoot, aComm);}}
        public static void MPI_Scatterv(long[]    rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JLONG   , JTYPE_LONG   , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JLONG   , JTYPE_LONG   , aRoot, aComm);}}
        public static void MPI_Scatterv(float[]   rBuf, int aStart, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {rangeCheck(rBuf.length, totalCount(aCounts, MPI_Comm_size(aComm))+aStart); MPI_Scatterv0(true, rBuf, aStart, aCounts, MPI_JFLOAT  , JTYPE_FLOAT  , null, 0, 0, MPI_JNULL, JTYPE_NULL, aRoot, aComm);} else {rangeCheck(rBuf.length, aCounts[tRank]+aStart); MPI_Scatterv0(false, null, 0, null, MPI_JNULL, JTYPE_NULL, rBuf, aStart, aCounts[tRank], MPI_JFLOAT  , JTYPE_FLOAT  , aRoot, aComm);}}
        private native static void MPI_Scatterv0(boolean aInPlace, Object aSendBuf, int aSendStart, int[] aSendCounts, long aSendType, int aSendJType, Object rRecvBuf, int aRecvStart, int aRecvCount, long aRecvType, int aRecvJType, int aRoot, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Scatterv(DoubleCPointer aSendBuf, DoubleCPointer rRecvBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {MPI_Scatterv1(false, aSendBuf.ptr_(), aCounts, MPI_DOUBLE, rRecvBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], MPI_DOUBLE, aRoot, aComm);}
        public static void MPI_Scatterv(IntCPointer    aSendBuf, IntCPointer    rRecvBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {MPI_Scatterv1(false, aSendBuf.ptr_(), aCounts, MPI_INT   , rRecvBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], MPI_INT   , aRoot, aComm);}
        public static void MPI_Scatterv(CPointer       aSendBuf, CPointer       rRecvBuf, int[] aCounts, long aDataType, int aRoot, long aComm) throws MPIException {MPI_Scatterv1(false, aSendBuf.ptr_(), aCounts, aDataType, rRecvBuf.ptr_(), aCounts[MPI_Comm_rank(aComm)], aDataType, aRoot, aComm);}
        public static void MPI_Scatterv(DoubleCPointer rBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv1(true, rBuf.ptr_(), aCounts, MPI_DOUBLE, 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv1(false, 0, null, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts[tRank], MPI_DOUBLE, aRoot, aComm);}}
        public static void MPI_Scatterv(IntCPointer    rBuf, int[] aCounts, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv1(true, rBuf.ptr_(), aCounts, MPI_INT   , 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv1(false, 0, null, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts[tRank], MPI_INT   , aRoot, aComm);}}
        public static void MPI_Scatterv(CPointer       rBuf, int[] aCounts, long aDataType, int aRoot, long aComm) throws MPIException {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv1(true, rBuf.ptr_(), aCounts, aDataType, 0, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv1(false, 0, null, MPI_DATATYPE_NULL, rBuf.ptr_(), aCounts[tRank], aDataType, aRoot, aComm);}}
        private native static void MPI_Scatterv1(boolean aInPlace, long aSendBuf, int[] aSendCounts, long aSendType, long rRecvBuf, int aRecvCount, long aRecvType, int aRoot, long aComm) throws MPIException;
        
        
        
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
         * @param aDest The rank of the destination process within the communicator that is
         *              specified by the comm parameter.
         *
         * @param aTag The message tag that can be used to distinguish different types of messages.
         *
         * @param aComm The handle to the communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-send-function"> MPI_Send function </a>
         */
        public static void MPI_Send(IDataShell<?> aBuf, int aDest, int aTag, long aComm) throws MPIException {
            Object tBuf = aBuf.internalDataWithLengthCheck();
            int tJType = jarrayType(tBuf);
            MPI_Send0(tBuf, aBuf.internalDataShift(), aBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType, aDest, aTag, aComm);
        }
        public static void MPI_Send(byte[]    aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aTag, aComm);}
        public static void MPI_Send(double[]  aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aTag, aComm);}
        public static void MPI_Send(boolean[] aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aTag, aComm);}
        public static void MPI_Send(char[]    aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aTag, aComm);}
        public static void MPI_Send(short[]   aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aTag, aComm);}
        public static void MPI_Send(int[]     aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aDest, aTag, aComm);}
        public static void MPI_Send(long[]    aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aDest, aTag, aComm);}
        public static void MPI_Send(float[]   aBuf, int aStart, int aCount, int aDest, int aTag, long aComm) throws MPIException {rangeCheck(aBuf.length, aCount+aStart); MPI_Send0(aBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aTag, aComm);}
        public static void MPI_Send(int aDest, int aTag, long aComm) throws MPIException {MPI_Send0(null, 0, 0, MPI_BYTE, JTYPE_NULL, aDest, aTag, aComm);}
        private native static void MPI_Send0(Object aBuf, int aStart, int aCount, long aDataType, int aJDataType, int aDest, int aTag, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Send(DoubleCPointer aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send1(aBuf.ptr_(), aCount, MPI_DOUBLE, aDest, aTag, aComm);}
        public static void MPI_Send(IntCPointer    aBuf, int aCount, int aDest, int aTag, long aComm) throws MPIException {MPI_Send1(aBuf.ptr_(), aCount, MPI_INT   , aDest, aTag, aComm);}
        public static void MPI_Send(CPointer       aBuf, int aCount, long aDataType, int aDest, int aTag, long aComm) throws MPIException {MPI_Send1(aBuf.ptr_(), aCount, aDataType, aDest, aTag, aComm);}
        private native static void MPI_Send1(long aBuf, int aCount, long aDataType, int aDest, int aTag, long aComm) throws MPIException;
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
        public static void MPI_Recv(IDataShell<?> rBuf, int aSource, int aTag, long aComm) throws MPIException {
            Object tBuf = rBuf.internalDataWithLengthCheck();
            int tJType = jarrayType(tBuf);
            MPI_Recv0(tBuf, rBuf.internalDataShift(), rBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tJType], tJType, aSource, aTag, aComm);
        }
        public static void MPI_Recv(byte[]    rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aTag, aComm);}
        public static void MPI_Recv(double[]  rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aTag, aComm);}
        public static void MPI_Recv(boolean[] rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aTag, aComm);}
        public static void MPI_Recv(char[]    rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aTag, aComm);}
        public static void MPI_Recv(short[]   rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aTag, aComm);}
        public static void MPI_Recv(int[]     rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JINT    , JTYPE_INT    , aSource, aTag, aComm);}
        public static void MPI_Recv(long[]    rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JLONG   , JTYPE_LONG   , aSource, aTag, aComm);}
        public static void MPI_Recv(float[]   rBuf, int aStart, int aCount, int aSource, int aTag, long aComm) throws MPIException {rangeCheck(rBuf.length, aCount+aStart); MPI_Recv0(rBuf, aStart, aCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aTag, aComm);}
        public static void MPI_Recv(int aSource, int aTag, long aComm) throws MPIException {MPI_Recv0(null, 0, 0, MPI_BYTE, JTYPE_NULL, aSource, aTag, aComm);}
        private native static void MPI_Recv0(Object rBuf, int aStart, int aCount, long aDataType, int aJDataType, int aSource, int aTag, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Recv(DoubleCPointer rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv1(rBuf.ptr_(), aCount, MPI_DOUBLE, aSource, aTag, aComm);}
        public static void MPI_Recv(IntCPointer    rBuf, int aCount, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv1(rBuf.ptr_(), aCount, MPI_INT   , aSource, aTag, aComm);}
        public static void MPI_Recv(CPointer       rBuf, int aCount, long aDataType, int aSource, int aTag, long aComm) throws MPIException {MPI_Recv1(rBuf.ptr_(), aCount, aDataType, aSource, aTag, aComm);}
        private native static void MPI_Recv1(long rBuf, int aCount, long aDataType, int aSource, int aTag, long aComm) throws MPIException;
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
         * @param aDest Rank of destination.
         * @param aSendTag Send tag.
         * @param rRecvBuf The data array to be received
         * @param aSource Rank of source.
         * @param aRecvTag Receive tag.
         * @param aComm Communicator.
         *
         * @see <a href="https://learn.microsoft.com/message-passing-interface/mpi-sendrecv-function"> MPI_Sendrecv function </a>
         */
        public static void MPI_Sendrecv(IDataShell<?> aSendBuf, int aDest, int aSendTag, IDataShell<?> rRecvBuf, int aSource, int aRecvTag, long aComm) throws MPIException {
            Object tSendBuf = aSendBuf.internalDataWithLengthCheck();
            Object tRecvBuf = rRecvBuf.internalDataWithLengthCheck();
            int tSendJType = jarrayType(tSendBuf);
            int tRecvJType = jarrayType(tRecvBuf);
            MPI_Sendrecv0(tSendBuf, aSendBuf.internalDataShift(), aSendBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tSendJType], tSendJType, aDest, aSendTag,
                          tRecvBuf, rRecvBuf.internalDataShift(), rRecvBuf.internalDataSize(), JTYPE_TO_MPI_TYPE[tRecvJType], tRecvJType, aSource, aRecvTag, aComm);
        }
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBYTE   , JTYPE_BYTE   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JDOUBLE , JTYPE_DOUBLE , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JCHAR   , JTYPE_CHAR   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JSHORT  , JTYPE_SHORT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JINT    , JTYPE_INT    , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JLONG   , JTYPE_LONG   , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBYTE   , JTYPE_BYTE   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JDOUBLE , JTYPE_DOUBLE , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JBOOLEAN, JTYPE_BOOLEAN, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JCHAR   , JTYPE_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JSHORT  , JTYPE_SHORT  , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JINT    , JTYPE_INT    , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JLONG   , JTYPE_LONG   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendStart, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvStart, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {rangeCheck(aSendBuf.length, aSendCount+aSendStart); rangeCheck(rRecvBuf.length, aRecvCount+aRecvStart); MPI_Sendrecv0(aSendBuf, aSendStart, aSendCount, MPI_JFLOAT  , JTYPE_FLOAT  , aDest, aSendTag, rRecvBuf, aRecvStart, aRecvCount, MPI_JFLOAT  , JTYPE_FLOAT  , aSource, aRecvTag, aComm);}
        private native static void MPI_Sendrecv0(Object aSendBuf, int aSendStart, int aSendCount, long aSendType, int aSendJType, int aDest, int aSendTag, Object rRecvBuf, int aRecvStart, int aRecvCount, long aRecvType, int aRecvJType, int aSource, int aRecvTag, long aComm) throws MPIException;
        /** 提供直接使用指针的收发，可以避免类型转换的损耗 */
        public static void MPI_Sendrecv(DoubleCPointer aSendBuf, int aSendCount, int aDest, int aSendTag, DoubleCPointer rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv1(aSendBuf.ptr_(), aSendCount, MPI_DOUBLE, aDest, aSendTag, rRecvBuf.ptr_(), aRecvCount, MPI_DOUBLE, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(DoubleCPointer aSendBuf, int aSendCount, int aDest, int aSendTag, IntCPointer    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv1(aSendBuf.ptr_(), aSendCount, MPI_DOUBLE, aDest, aSendTag, rRecvBuf.ptr_(), aRecvCount, MPI_INT   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(IntCPointer    aSendBuf, int aSendCount, int aDest, int aSendTag, DoubleCPointer rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv1(aSendBuf.ptr_(), aSendCount, MPI_INT   , aDest, aSendTag, rRecvBuf.ptr_(), aRecvCount, MPI_DOUBLE, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(IntCPointer    aSendBuf, int aSendCount, int aDest, int aSendTag, IntCPointer    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv1(aSendBuf.ptr_(), aSendCount, MPI_INT   , aDest, aSendTag, rRecvBuf.ptr_(), aRecvCount, MPI_INT   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(CPointer       aSendBuf, int aSendCount, long aSendType, int aDest, int aSendTag, CPointer rRecvBuf, int aRecvCount, long aRecvType, int aSource, int aRecvTag, long aComm) throws MPIException {MPI_Sendrecv1(aSendBuf.ptr_(), aSendCount, aSendType, aDest, aSendTag, rRecvBuf.ptr_(), aRecvCount, aRecvType, aSource, aRecvTag, aComm);}
        private native static void MPI_Sendrecv1(long aSendBuf, int aSendCount, long aSendType, int aDest, int aSendTag, long rRecvBuf, int aRecvCount, long aRecvType, int aSource, int aRecvTag, long aComm) throws MPIException;
        
        
        
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
    }
}
