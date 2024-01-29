package jtool.parallel;

import jtool.clib.MiMalloc;
import jtool.code.UT;
import org.jetbrains.annotations.ApiStatus;

import java.io.BufferedReader;

import static jtool.code.CS.Exec.EXE;
import static jtool.code.CS.Exec.JAR_DIR;
import static jtool.code.CS.*;

/**
 * 基于 jni 实现的 MPI wrapper, 介绍部分基于
 * <a href="https://learn.microsoft.com/en-us/message-passing-interface/microsoft-mpi">
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
 * import static jtool.parallel.MPI.Native.*;
 *
 * MPI_Init(args);
 * int me = MPI_Comm_rank(MPI_COMM_WORLD);
 * System.out.println("Hi from <"+me+">");
 * MPI_Finalize();
 * } </pre>
 * <p>
 * 或者更加 java 风格的使用：
 * <pre> {@code
 * import jtool.parallel.MPI;
 *
 * MPI.init(args);
 * int me = MPI.Comm.WORLD.rank();
 * System.out.println("Hi from <"+me+">");
 * MPI.shutdown(); // "finalize()" has been used in java
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
    
    public static String libraryVersion() throws Error {return MPI.Native.MPI_Get_library_version();}
    
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
        public int rank() throws Error {return Native.MPI_Group_rank(mPtr);}
        /**
         * @return The size of the specified group.
         */
        public int size() throws Error {return Native.MPI_Group_size(mPtr);}
        
        /** Free the group. */
        @Override public void shutdown() {
            if (mPtr != Native.MPI_GROUP_NULL) {
                try {Native.MPI_Group_free(mPtr);} catch (Error ignored) {}
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-difference-function"> MPI_Group_difference function </a>
         */
        public Group difference(Group aRHS) throws Error {return of(Native.MPI_Group_difference(mPtr, aRHS.mPtr));}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-excl-function"> MPI_Group_excl function </a>
         */
        public Group excl(int aN, int[] aRanks) throws Error {return of(Native.MPI_Group_excl(mPtr, aN, aRanks));}
        public Group excl(int[] aRanks) throws Error {return excl(aRanks.length, aRanks);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-incl-function"> MPI_Group_incl function </a>
         */
        public Group incl(int aN, int[] aRanks) throws Error {return of(Native.MPI_Group_incl(mPtr, aN, aRanks));}
        public Group incl(int[] aRanks) throws Error {return incl(aRanks.length, aRanks);}
        
        /**
         * Creates a new group from the intersection of two existing groups.
         * @param aRHS The second group.
         * @return A new {@link MPI.Group} with those elements that are present in both groups.
         * The function returns {@link MPI.Group#EMPTY} if the new group is empty.
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-intersection-function"> MPI_Group_intersection function </a>
         */
        public Group intersection(Group aRHS) throws Error {return of(Native.MPI_Group_intersection(mPtr, aRHS.mPtr));}
        
        /**
         * Creates a new group from the union of two existing groups.
         * @param aRHS The second group.
         * @return A new {@link MPI.Group} that represents all elements in either group.
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-union-function"> MPI_Group_union function </a>
         */
        public Group union(Group aRHS) throws Error {return of(Native.MPI_Group_union(mPtr, aRHS.mPtr));}
    }
    
    public static class Comm implements IAutoShutdown {
        public final static Comm
          NULL  = new Comm(Native.MPI_COMM_NULL )
        , WORLD = new Comm(Native.MPI_COMM_WORLD)
        , SELF  = new Comm(Native.MPI_COMM_SELF )
        ;
        @ApiStatus.Internal public static Comm of(long aPtr) {
            if (aPtr == Native.MPI_COMM_NULL ) return NULL ;
            if (aPtr == Native.MPI_COMM_WORLD) return WORLD;
            if (aPtr == Native.MPI_COMM_SELF ) return SELF ;
            return new Comm(aPtr);
        }
        
        private long mPtr;
        private Comm(long aPtr) {mPtr = aPtr;}
        @ApiStatus.Internal public long ptr_() {return mPtr;}
        
        /** @return the number of calling process within the group of the communicator. */
        public int rank() throws Error {return Native.MPI_Comm_rank(mPtr);}
        /** @return the number of processes in the group for the communicator. */
        public int size() throws Error {return Native.MPI_Comm_size(mPtr);}
        
        /** Duplicate the communicator. */
        public Comm copy() throws Error {return of(Native.MPI_Comm_dup(mPtr));}
        
        /**
         * Frees the communicator that is allocated with the {@link MPI.Comm#copy}, {@link MPI.Comm#create},
         * or {@link MPI.Comm#split} functions.
         */
        @Override public void shutdown() {
            if (mPtr != Native.MPI_COMM_NULL) {
                try {Native.MPI_Comm_free(mPtr);} catch (Error ignored) {}
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-allgather-function"> MPI_Allgather function </a>
         */
        public void allgather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount) throws Error {Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);}
        public void allgather(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(double[]  aSendBuf, double[]  rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(char[]    aSendBuf, char[]    rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(short[]   aSendBuf, short[]   rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(int[]     aSendBuf, int[]     rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(long[]    aSendBuf, long[]    rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(float[]   aSendBuf, float[]   rRecvBuf, int aCount) throws Error {Native.MPI_Allgather(aSendBuf, aCount, rRecvBuf, aCount, mPtr);}
        public void allgather(byte[]    rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(double[]  rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(boolean[] rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(char[]    rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(short[]   rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(int[]     rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(long[]    rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        public void allgather(float[]   rBuf, int aCount) throws Error {Native.MPI_Allgather(rBuf, aCount, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-allgatherv-function"> MPI_Allgatherv function </a>
         */
        public void allgatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);}
        public void allgatherv(byte[]    rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(double[]  rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(boolean[] rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(char[]    rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(short[]   rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(int[]     rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(long[]    rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        public void allgatherv(float[]   rBuf, int[] aCounts, int[] aDispls) throws Error {Native.MPI_Allgatherv(rBuf, aCounts, aDispls, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-allreduce-function"> MPI_Allreduce function </a>
         */
        public void allreduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(byte[]    rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(double[]  rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(boolean[] rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(char[]    rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(short[]   rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(int[]     rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(long[]    rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        public void allreduce(float[]   rBuf, int aCount, Op aOp) throws Error {Native.MPI_Allreduce(rBuf, aCount, aOp.mPtr, mPtr);}
        
        /**
         * Initiates barrier synchronization across all members of a group.
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-barrier-function"> MPI_Barrier function </a>
         */
        public void barrier() throws Error {
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-bcast-function"> MPI_Bcast function </a>
         */
        public void bcast(byte[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(double[]  rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(boolean[] rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(char[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(short[]   rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(int[]     rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(long[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        public void bcast(float[]   rBuf, int aCount, int aRoot) throws Error {Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-gather-function"> MPI_Gather function </a>
         */
        public void gather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void gather(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(double[]  aSendBuf, double[]  rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(char[]    aSendBuf, char[]    rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(short[]   aSendBuf, short[]   rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(int[]     aSendBuf, int[]     rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(long[]    aSendBuf, long[]    rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(float[]   aSendBuf, float[]   rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void gather(byte[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(double[]  rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(boolean[] rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(char[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(short[]   rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(int[]     rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(long[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        public void gather(float[]   rBuf, int aCount, int aRoot) throws Error {Native.MPI_Gather(rBuf, aCount, aRoot, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-gatherv-function"> MPI_Gatherv function </a>
         */
        public void gatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);}
        public void gatherv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void gatherv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Gatherv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-reduce-function"> MPI_Reduce function </a>
         */
        public void reduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(byte[]    rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(double[]  rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(boolean[] rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(char[]    rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(short[]   rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(int[]     rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(long[]    rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        public void reduce(float[]   rBuf, int aCount, Op aOp, int aRoot) throws Error {Native.MPI_Reduce(rBuf, aCount, aOp.mPtr, aRoot, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-scatter-function"> MPI_Scatter function </a>
         */
        public void scatter(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatter(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(double[]  aSendBuf, double[]  rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(char[]    aSendBuf, char[]    rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(short[]   aSendBuf, short[]   rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(int[]     aSendBuf, int[]     rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(long[]    aSendBuf, long[]    rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(float[]   aSendBuf, float[]   rRecvBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(aSendBuf, aCount, rRecvBuf, aCount, aRoot, mPtr);}
        public void scatter(byte[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(double[]  rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(boolean[] rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(char[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(short[]   rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(int[]     rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(long[]    rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        public void scatter(float[]   rBuf, int aCount, int aRoot) throws Error {Native.MPI_Scatter(rBuf, aCount, aRoot, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-scatterv-function"> MPI_Scatterv function </a>
         */
        public void scatterv(byte[]    aSendBuf, int[] aSendCounts, int[] aDispls, byte[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(double[]  aSendBuf, int[] aSendCounts, int[] aDispls, double[]  rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(boolean[] aSendBuf, int[] aSendCounts, int[] aDispls, boolean[] rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(char[]    aSendBuf, int[] aSendCounts, int[] aDispls, char[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(short[]   aSendBuf, int[] aSendCounts, int[] aDispls, short[]   rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(int[]     aSendBuf, int[] aSendCounts, int[] aDispls, int[]     rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(long[]    aSendBuf, int[] aSendCounts, int[] aDispls, long[]    rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(float[]   aSendBuf, int[] aSendCounts, int[] aDispls, float[]   rRecvBuf, int aRecvCount, int aRoot) throws Error {Native.MPI_Scatterv(aSendBuf, aSendCounts, aDispls, rRecvBuf, aRecvCount, aRoot, mPtr);}
        public void scatterv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        public void scatterv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot) throws Error {Native.MPI_Scatterv(rBuf, aCounts, aDispls, aRoot, mPtr);}
        
        
        /// MPI Communicator Functions
        /**
         * Extracts a subset a group of processes for the purpose of separate Multiple Instruction
         * Multiple Data (MIMD) computation in a separate communicator.
         *
         * @param aGroup The group that defines the requested subset of the processes in the
         *               source communicator.
         * @return A new {@link MPI.Comm}.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-create-function"> MPI_Comm_create function </a>
         */
        public Comm create(Group aGroup) throws Error {return of(Native.MPI_Comm_create(mPtr, aGroup.mPtr));}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-split-function"> MPI_Comm_split function </a>
         */
        public Comm split(int aColor, int aKey) throws Error {return of(Native.MPI_Comm_split(mPtr, aColor, aKey));}
        public Comm split(int aColor) throws Error {return split(aColor, rank());}
        
        
        /// MPI Group Functions
        /**
         * Retrieves the group that is associated with a communicator.
         * @return The new {@link MPI.Group} that is associated with the specified communicator.
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-group-function"> MPI_Comm_group function </a>
         */
        public Group group() throws Error {return Group.of(Native.MPI_Comm_group(mPtr));}
        
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-send-function"> MPI_Send function </a>
         */
        public void send(byte[]    aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(double[]  aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(boolean[] aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(char[]    aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(short[]   aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(int[]     aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(long[]    aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(float[]   aBuf, int aCount, int aDest, int aTag) throws Error {Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);}
        public void send(byte[]    aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(double[]  aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(boolean[] aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(char[]    aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(short[]   aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(int[]     aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(long[]    aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(float[]   aBuf, int aCount, int aDest) throws Error {Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);}
        public void send(int aDest, int aTag) throws Error {Native.MPI_Send(aDest, aTag, mPtr);}
        public void send(int aDest) throws Error {Native.MPI_Send(aDest, 0, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-recv-function"> MPI_Recv function </a>
         */
        public void recv(byte[]    rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(double[]  rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(boolean[] rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(char[]    rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(short[]   rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(int[]     rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(long[]    rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(float[]   rBuf, int aCount, int aSource, int aTag) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, aTag, mPtr);}
        public void recv(byte[]    rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(double[]  rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(boolean[] rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(char[]    rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(short[]   rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(int[]     rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(long[]    rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(float[]   rBuf, int aCount, int aSource) throws Error {Native.MPI_Recv(rBuf, aCount, aSource, Tag.ANY, mPtr);}
        public void recv(int aSource, int aTag) throws Error {Native.MPI_Recv(aSource, aTag, mPtr);}
        public void recv(int aSource) throws Error {Native.MPI_Recv(aSource, Tag.ANY, mPtr);}
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-sendrecv-function"> MPI_Sendrecv function </a>
         */
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, aSendTag, rRecvBuf, aRecvCount, aSource, aRecvTag, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(double[]  aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(char[]    aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(short[]   aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(int[]     aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(long[]    aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, byte[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, double[]  rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, boolean[] rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, char[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, short[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int[]     rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, long[]    rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
        public void sendrecv(float[]   aSendBuf, int aSendCount, int aDest, float[]   rRecvBuf, int aRecvCount, int aSource) throws Error {Native.MPI_Sendrecv(aSendBuf, aSendCount, aDest, 0, rRecvBuf, aRecvCount, aSource, Tag.ANY, mPtr);}
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
          NULL          (Native.MPI_DATATYPE_NULL)
        , SIGNED_CHAR   (Native.MPI_SIGNED_CHAR   )
        , DOUBLE        (Native.MPI_DOUBLE        )
        , UNSIGNED_CHAR (Native.MPI_UNSIGNED_CHAR )
        , UNSIGNED_SHORT(Native.MPI_UNSIGNED_SHORT)
        , SHORT         (Native.MPI_SHORT         )
        , INT32_T       (Native.MPI_INT32_T       )
        , INT64_T       (Native.MPI_INT64_T       )
        , FLOAT         (Native.MPI_FLOAT         )
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
    
    public final static class Error extends Exception {
        public final int mErrCode;
        public Error(int aErrCode, String aMessage) {
            super(aMessage);
            mErrCode = aErrCode;
        }
    }
    
    
    /// 基础功能
    /**
     * Initializes the calling MPI process’s execution environment for single threaded execution.
     * @param aArgs the argument list for the program
     */
    public static void init(String[] aArgs) throws Error {Native.MPI_Init(aArgs);}
    /**
     * Indicates whether {@link MPI#init} has been called.
     * @return true if {@link MPI#init} or {@link MPI#initThread} has been called and false otherwise.
     */
    public static boolean initialized() throws Error {return Native.MPI_Initialized();}
    
    /**
     * Terminates the calling MPI process’s execution environment.
     */
    public static void shutdown() throws Error {Native.MPI_Finalize();}
    /**
     * Indicates whether {@link MPI#shutdown} has been called.
     * @return true if MPI_Finalize has been called and false otherwise.
     */
    public static boolean isShutdown() throws Error {return Native.MPI_Finalized();}
    
    
    
    
    /// MPI External Functions
    /**
     * Initializes the calling MPI process’s execution environment for threaded execution.
     * @param aArgs The argument list for the program
     * @param aRequired The level of desired thread support. Multiple MPI processes
     *                  in the same job may use different values.
     * @return The level of provided thread support.
     * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-init-thread-function"> MPI_Init_thread function </a>
     */
    public static int initThread(String[] aArgs, int aRequired) throws Error {return Native.MPI_Init_thread(aArgs, aRequired);}
    
    
    
    
    /**
     * 提供按照原始的 MPI 标准格式的接口以及对应的 native 实现
     * @author liqa
     */
    public static class Native {
        private Native() {}
        
        private final static String MPILIB_DIR = JAR_DIR+"mpi/";
        private final static String MPILIB_PATH = MPILIB_DIR + "mpijni@"+UT.Code.uniqueID(VERSION) + JNILIB_EXTENSION;
        private final static String[] MPISRC_NAME = {
              "jtool_parallel_MPI_Native.c"
            , "jtool_parallel_MPI_Native.h"
        };
        
        private static void initMPI_() throws Exception {
            // 检测 cmake，为了简洁并避免问题，现在要求一定要有 cmake 环境
            EXE.setNoSTDOutput().setNoERROutput();
            boolean tNoCmake = EXE.system("cmake --version") != 0;
            EXE.setNoSTDOutput(false).setNoERROutput(false);
            if (tNoCmake) throw new Exception("MPI BUILD ERROR: No camke environment.");
            // 从内部资源解压到临时目录
            String tWorkingDir = WORKING_DIR.replaceAll("%n", "mpisrc");
            // 如果已经存在则先删除
            UT.IO.removeDir(tWorkingDir);
            for (String tName : MPISRC_NAME) {
                UT.IO.copy(UT.IO.getResource("mpi/src/"+tName), tWorkingDir+tName);
            }
            // 这里对 CMakeLists.txt 特殊处理，替换其中的 mimalloc 库路径为设置好的路径
            try (BufferedReader tReader = UT.IO.toReader(UT.IO.getResource("mpi/src/CMakeLists.txt")); UT.IO.IWriteln tWriter = UT.IO.toWriteln(tWorkingDir+"CMakeLists.txt")) {
                String tLine;
                while ((tLine = tReader.readLine()) != null) {
                    tLine = tLine.replace("$ENV{MIMALLOC_HOME}", MiMalloc.MIMALLOC_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                    tWriter.writeln(tLine);
                }
            }
            System.out.println("MPI INIT INFO: Building mpijni from source code...");
            String tBuildDir = tWorkingDir+"build/";
            UT.IO.makeDir(tBuildDir);
            // 直接通过系统指令来编译 mpijni 的库，关闭输出
            EXE.setNoSTDOutput();
            EXE.system(String.format("cd \"%s\"; cmake ..; cmake --build . --config Release", tBuildDir));
            EXE.setNoSTDOutput(false);
            // 获取 build 目录下的 lib 文件
            String tLibDir = tBuildDir+"lib/";
            if (!UT.IO.isDir(tLibDir)) throw new Exception("MPI BUILD ERROR: Build Failed, No mpijni lib in "+tBuildDir);
            String[] tList = UT.IO.list(tLibDir);
            String tLibPath = null;
            for (String tName : tList) if (tName.contains("mpi") && (tName.endsWith(".dll") || tName.endsWith(".so") || tName.endsWith(".jnilib") || tName.endsWith(".dylib"))) {
                tLibPath = tName;
            }
            if (tLibPath == null) throw new Exception("MPI BUILD ERROR: Build Failed, No mpijni lib in "+tLibDir);
            tLibPath = tLibDir+tLibPath;
            // 将 build 的输出拷贝到 lib 目录下
            UT.IO.copy(tLibPath, MPILIB_PATH);
            // 完事后移除临时解压得到的源码
            UT.IO.removeDir(tWorkingDir);
            System.out.println("MPI INIT INFO: mpijni successfully installed.");
        }
        
        // 直接进行初始化，虽然原则上会在 MPI_Init() 之前获取，
        // 但是得到的是 final 值，可以避免意外的修改，并且简化代码；
        // 这对于一般的 MPI 实现应该都是没有问题的
        static {
            InitHelper.INITIALIZED = true;
            // 不管怎样都会依赖 MiMalloc
            MiMalloc.InitHelper.init();
            
            // 如果不存在 jni lib 则需要重新通过源码编译
            if (!UT.IO.isFile(MPILIB_PATH)) {
                System.out.println("MPI INIT INFO: mpijni libraries not found. Reinstalling...");
                try {initMPI_();}
                catch (Exception e) {throw new RuntimeException(e);}
            }
            // 设置库路径
            System.load(UT.IO.toAbsolutePath(MPILIB_PATH));
            
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
        public native static String MPI_Get_library_version() throws Error;
        
        
        /// 基础功能
        /**
         * Initializes the calling MPI process’s execution environment for single threaded execution.
         * @param aArgs the argument list for the program
         */
        public native static void MPI_Init(String[] aArgs) throws Error;
        /**
         * Indicates whether {@link #MPI_Init} has been called.
         * @return true if {@link #MPI_Init} or {@link #MPI_Init_thread} has been called and false otherwise.
         */
        public native static boolean MPI_Initialized() throws Error;
        
        /**
         * Retrieves the rank of the calling process in the group of the specified communicator.
         * @param aComm The communicator.
         * @return the number of calling process within the group of the communicator.
         */
        public native static int MPI_Comm_rank(long aComm) throws Error;
        
        /**
         * Retrieves the number of processes involved in a communicator, or the total number of
         * processes available.
         * @param aComm The communicator to evaluate. Specify the {@link #MPI_COMM_WORLD} constant to retrieve
         *              the total number of processes available.
         * @return the number of processes in the group for the communicator.
         */
        public native static int MPI_Comm_size(long aComm) throws Error;
        
        /**
         * Terminates the calling MPI process’s execution environment.
         */
        public native static void MPI_Finalize() throws Error;
        /**
         * Indicates whether {@link #MPI_Finalize} has been called.
         * @return true if MPI_Finalize has been called and false otherwise.
         */
        public native static boolean MPI_Finalized() throws Error;
        
        
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-allgather-function"> MPI_Allgather function </a>
         */
        public static void MPI_Allgather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_SIGNED_CHAR   , rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aComm);}
        public static void MPI_Allgather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_DOUBLE        , rRecvBuf, aRecvCount, MPI_DOUBLE        , aComm);}
        public static void MPI_Allgather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aComm);}
        public static void MPI_Allgather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aComm);}
        public static void MPI_Allgather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_SHORT         , rRecvBuf, aRecvCount, MPI_SHORT         , aComm);}
        public static void MPI_Allgather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_INT32_T       , rRecvBuf, aRecvCount, MPI_INT32_T       , aComm);}
        public static void MPI_Allgather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_INT64_T       , rRecvBuf, aRecvCount, MPI_INT64_T       , aComm);}
        public static void MPI_Allgather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, long aComm) throws Error {MPI_Allgather0(false, aSendBuf, aSendCount, MPI_FLOAT         , rRecvBuf, aRecvCount, MPI_FLOAT         , aComm);}
        public static void MPI_Allgather(byte[]    rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_SIGNED_CHAR   , aComm);}
        public static void MPI_Allgather(double[]  rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_DOUBLE        , aComm);}
        public static void MPI_Allgather(boolean[] rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_UNSIGNED_CHAR , aComm);}
        public static void MPI_Allgather(char[]    rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_UNSIGNED_SHORT, aComm);}
        public static void MPI_Allgather(short[]   rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_SHORT         , aComm);}
        public static void MPI_Allgather(int[]     rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_INT32_T       , aComm);}
        public static void MPI_Allgather(long[]    rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_INT64_T       , aComm);}
        public static void MPI_Allgather(float[]   rBuf, int aCount, long aComm) throws Error {MPI_Allgather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_FLOAT         , aComm);}
        private native static void MPI_Allgather0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int aRecvCount, long aRecvType, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-allgatherv-function"> MPI_Allgatherv function </a>
         */
        public static void MPI_Allgatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_SIGNED_CHAR   , rRecvBuf, aRecvCounts, aDispls, MPI_SIGNED_CHAR   , aComm);}
        public static void MPI_Allgatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_DOUBLE        , rRecvBuf, aRecvCounts, aDispls, MPI_DOUBLE        , aComm);}
        public static void MPI_Allgatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , rRecvBuf, aRecvCounts, aDispls, MPI_UNSIGNED_CHAR , aComm);}
        public static void MPI_Allgatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, rRecvBuf, aRecvCounts, aDispls, MPI_UNSIGNED_SHORT, aComm);}
        public static void MPI_Allgatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_SHORT         , rRecvBuf, aRecvCounts, aDispls, MPI_SHORT         , aComm);}
        public static void MPI_Allgatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_INT32_T       , rRecvBuf, aRecvCounts, aDispls, MPI_INT32_T       , aComm);}
        public static void MPI_Allgatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_INT64_T       , rRecvBuf, aRecvCounts, aDispls, MPI_INT64_T       , aComm);}
        public static void MPI_Allgatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(false, aSendBuf, aSendCount, MPI_FLOAT         , rRecvBuf, aRecvCounts, aDispls, MPI_FLOAT         , aComm);}
        public static void MPI_Allgatherv(byte[]    rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_SIGNED_CHAR   , aComm);}
        public static void MPI_Allgatherv(double[]  rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_DOUBLE        , aComm);}
        public static void MPI_Allgatherv(boolean[] rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_UNSIGNED_CHAR , aComm);}
        public static void MPI_Allgatherv(char[]    rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_UNSIGNED_SHORT, aComm);}
        public static void MPI_Allgatherv(short[]   rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_SHORT         , aComm);}
        public static void MPI_Allgatherv(int[]     rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_INT32_T       , aComm);}
        public static void MPI_Allgatherv(long[]    rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_INT64_T       , aComm);}
        public static void MPI_Allgatherv(float[]   rBuf, int[] aCounts, int[] aDispls, long aComm) throws Error {MPI_Allgatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_FLOAT         , aComm);}
        private native static void MPI_Allgatherv0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int[] aRecvCounts, int[] aDispls, long aRecvType, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-allreduce-function"> MPI_Allreduce function </a>
         */
        public static void MPI_Allreduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_SIGNED_CHAR   , aOp, aComm);}
        public static void MPI_Allreduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_DOUBLE        , aOp, aComm);}
        public static void MPI_Allreduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_UNSIGNED_CHAR , aOp, aComm);}
        public static void MPI_Allreduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_UNSIGNED_SHORT, aOp, aComm);}
        public static void MPI_Allreduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_SHORT         , aOp, aComm);}
        public static void MPI_Allreduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_INT32_T       , aOp, aComm);}
        public static void MPI_Allreduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_INT64_T       , aOp, aComm);}
        public static void MPI_Allreduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(false, aSendBuf, rRecvBuf, aCount, MPI_FLOAT         , aOp, aComm);}
        public static void MPI_Allreduce(byte[]    rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_SIGNED_CHAR   , aOp, aComm);}
        public static void MPI_Allreduce(double[]  rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_DOUBLE        , aOp, aComm);}
        public static void MPI_Allreduce(boolean[] rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_UNSIGNED_CHAR , aOp, aComm);}
        public static void MPI_Allreduce(char[]    rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_UNSIGNED_SHORT, aOp, aComm);}
        public static void MPI_Allreduce(short[]   rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_SHORT         , aOp, aComm);}
        public static void MPI_Allreduce(int[]     rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_INT32_T       , aOp, aComm);}
        public static void MPI_Allreduce(long[]    rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_INT64_T       , aOp, aComm);}
        public static void MPI_Allreduce(float[]   rBuf, int aCount, long aOp, long aComm) throws Error {MPI_Allreduce0(true, null, rBuf, aCount, MPI_FLOAT         , aOp, aComm);}
        private native static void MPI_Allreduce0(boolean aInPlace, Object aSendBuf, Object rRecvBuf, int aCount, long aDataType, long aOp, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-barrier-function"> MPI_Barrier function </a>
         */
        public native static void MPI_Barrier(long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-bcast-function"> MPI_Bcast function </a>
         */
        public static void MPI_Bcast(byte[]    rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_SIGNED_CHAR   , aRoot, aComm);}
        public static void MPI_Bcast(double[]  rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_DOUBLE        , aRoot, aComm);}
        public static void MPI_Bcast(boolean[] rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_UNSIGNED_CHAR , aRoot, aComm);}
        public static void MPI_Bcast(char[]    rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_UNSIGNED_SHORT, aRoot, aComm);}
        public static void MPI_Bcast(short[]   rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_SHORT         , aRoot, aComm);}
        public static void MPI_Bcast(int[]     rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_INT32_T       , aRoot, aComm);}
        public static void MPI_Bcast(long[]    rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_INT64_T       , aRoot, aComm);}
        public static void MPI_Bcast(float[]   rBuf, int aCount, int aRoot, long aComm) throws Error {MPI_Bcast0(rBuf, aCount, MPI_FLOAT         , aRoot, aComm);}
        private native static void MPI_Bcast0(Object rBuf, int aCount, long aDataType, int aRoot, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-gather-function"> MPI_Gather function </a>
         */
        public static void MPI_Gather(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_SIGNED_CHAR   , rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aRoot, aComm);}
        public static void MPI_Gather(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_DOUBLE        , rRecvBuf, aRecvCount, MPI_DOUBLE        , aRoot, aComm);}
        public static void MPI_Gather(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aRoot, aComm);}
        public static void MPI_Gather(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aRoot, aComm);}
        public static void MPI_Gather(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_SHORT         , rRecvBuf, aRecvCount, MPI_SHORT         , aRoot, aComm);}
        public static void MPI_Gather(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_INT32_T       , rRecvBuf, aRecvCount, MPI_INT32_T       , aRoot, aComm);}
        public static void MPI_Gather(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_INT64_T       , rRecvBuf, aRecvCount, MPI_INT64_T       , aRoot, aComm);}
        public static void MPI_Gather(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Gather0(false, aSendBuf, aSendCount, MPI_FLOAT         , rRecvBuf, aRecvCount, MPI_FLOAT         , aRoot, aComm);}
        public static void MPI_Gather(byte[]    rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_SIGNED_CHAR   , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_SIGNED_CHAR   , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(double[]  rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_DOUBLE        , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_DOUBLE        , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(boolean[] rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_UNSIGNED_CHAR , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_UNSIGNED_CHAR , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(char[]    rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_UNSIGNED_SHORT, aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_UNSIGNED_SHORT, null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(short[]   rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_SHORT         , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_SHORT         , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(int[]     rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_INT32_T       , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_INT32_T       , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(long[]    rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_INT64_T       , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_INT64_T       , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gather(float[]   rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Gather0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_FLOAT         , aRoot, aComm);} else {MPI_Gather0(false, rBuf, aCount, MPI_FLOAT         , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gather0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRoot, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-gatherv-function"> MPI_Gatherv function </a>
         */
        public static void MPI_Gatherv(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_SIGNED_CHAR   , rRecvBuf, aRecvCounts, aDispls, MPI_SIGNED_CHAR   , aRoot, aComm);}
        public static void MPI_Gatherv(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_DOUBLE        , rRecvBuf, aRecvCounts, aDispls, MPI_DOUBLE        , aRoot, aComm);}
        public static void MPI_Gatherv(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , rRecvBuf, aRecvCounts, aDispls, MPI_UNSIGNED_CHAR , aRoot, aComm);}
        public static void MPI_Gatherv(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, rRecvBuf, aRecvCounts, aDispls, MPI_UNSIGNED_SHORT, aRoot, aComm);}
        public static void MPI_Gatherv(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_SHORT         , rRecvBuf, aRecvCounts, aDispls, MPI_SHORT         , aRoot, aComm);}
        public static void MPI_Gatherv(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_INT32_T       , rRecvBuf, aRecvCounts, aDispls, MPI_INT32_T       , aRoot, aComm);}
        public static void MPI_Gatherv(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_INT64_T       , rRecvBuf, aRecvCounts, aDispls, MPI_INT64_T       , aRoot, aComm);}
        public static void MPI_Gatherv(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) throws Error {MPI_Gatherv0(false, aSendBuf, aSendCount, MPI_FLOAT         , rRecvBuf, aRecvCounts, aDispls, MPI_FLOAT         , aRoot, aComm);}
        public static void MPI_Gatherv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_SIGNED_CHAR   , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_SIGNED_CHAR   , null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_DOUBLE        , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_DOUBLE        , null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_UNSIGNED_CHAR , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_UNSIGNED_CHAR , null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_UNSIGNED_SHORT, aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_UNSIGNED_SHORT, null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_SHORT         , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_SHORT         , null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_INT32_T       , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_INT32_T       , null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_INT64_T       , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_INT64_T       , null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        public static void MPI_Gatherv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Gatherv0(true, null, 0, MPI_DATATYPE_NULL, rBuf, aCounts, aDispls, MPI_FLOAT         , aRoot, aComm);} else {MPI_Gatherv0(false, rBuf, aCounts[tRank], MPI_FLOAT         , null, null, null, MPI_DATATYPE_NULL, aRoot, aComm);}}
        private native static void MPI_Gatherv0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int[] aRecvCounts, int[] aDispls, long aRecvType, int aRoot, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-reduce-function"> MPI_Reduce function </a>
         */
        public static void MPI_Reduce(byte[]    aSendBuf, byte[]    rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_SIGNED_CHAR   , aOp, aRoot, aComm);}
        public static void MPI_Reduce(double[]  aSendBuf, double[]  rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_DOUBLE        , aOp, aRoot, aComm);}
        public static void MPI_Reduce(boolean[] aSendBuf, boolean[] rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_UNSIGNED_CHAR , aOp, aRoot, aComm);}
        public static void MPI_Reduce(char[]    aSendBuf, char[]    rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_UNSIGNED_SHORT, aOp, aRoot, aComm);}
        public static void MPI_Reduce(short[]   aSendBuf, short[]   rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_SHORT         , aOp, aRoot, aComm);}
        public static void MPI_Reduce(int[]     aSendBuf, int[]     rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_INT32_T       , aOp, aRoot, aComm);}
        public static void MPI_Reduce(long[]    aSendBuf, long[]    rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_INT64_T       , aOp, aRoot, aComm);}
        public static void MPI_Reduce(float[]   aSendBuf, float[]   rRecvBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {MPI_Reduce0(false, aSendBuf, rRecvBuf, aCount, MPI_FLOAT         , aOp, aRoot, aComm);}
        public static void MPI_Reduce(byte[]    rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_SIGNED_CHAR   , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_SIGNED_CHAR   , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(double[]  rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_DOUBLE        , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_DOUBLE        , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(boolean[] rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_UNSIGNED_CHAR , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_UNSIGNED_CHAR , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(char[]    rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_UNSIGNED_SHORT, aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_UNSIGNED_SHORT, aOp, aRoot, aComm);}}
        public static void MPI_Reduce(short[]   rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_SHORT         , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_SHORT         , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(int[]     rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_INT32_T       , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_INT32_T       , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(long[]    rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_INT64_T       , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_INT64_T       , aOp, aRoot, aComm);}}
        public static void MPI_Reduce(float[]   rBuf, int aCount, long aOp, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Reduce0(true, null, rBuf, aCount, MPI_FLOAT         , aOp, aRoot, aComm);} else {MPI_Reduce0(false, rBuf, null, aCount, MPI_FLOAT         , aOp, aRoot, aComm);}}
        private native static void MPI_Reduce0(boolean aInPlace, Object aSendBuf, Object rRecvBuf, int aCount, long aDataType, long aOp, int aRoot, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-scatter-function"> MPI_Scatter function </a>
         */
        public static void MPI_Scatter(byte[]    aSendBuf, int aSendCount, byte[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_SIGNED_CHAR   , rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aRoot, aComm);}
        public static void MPI_Scatter(double[]  aSendBuf, int aSendCount, double[]  rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_DOUBLE        , rRecvBuf, aRecvCount, MPI_DOUBLE        , aRoot, aComm);}
        public static void MPI_Scatter(boolean[] aSendBuf, int aSendCount, boolean[] rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aRoot, aComm);}
        public static void MPI_Scatter(char[]    aSendBuf, int aSendCount, char[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aRoot, aComm);}
        public static void MPI_Scatter(short[]   aSendBuf, int aSendCount, short[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_SHORT         , rRecvBuf, aRecvCount, MPI_SHORT         , aRoot, aComm);}
        public static void MPI_Scatter(int[]     aSendBuf, int aSendCount, int[]     rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_INT32_T       , rRecvBuf, aRecvCount, MPI_INT32_T       , aRoot, aComm);}
        public static void MPI_Scatter(long[]    aSendBuf, int aSendCount, long[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_INT64_T       , rRecvBuf, aRecvCount, MPI_INT64_T       , aRoot, aComm);}
        public static void MPI_Scatter(float[]   aSendBuf, int aSendCount, float[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatter0(false, aSendBuf, aSendCount, MPI_FLOAT         , rRecvBuf, aRecvCount, MPI_FLOAT         , aRoot, aComm);}
        public static void MPI_Scatter(byte[]    rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_SIGNED_CHAR   , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_SIGNED_CHAR   , aRoot, aComm);}}
        public static void MPI_Scatter(double[]  rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_DOUBLE        , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_DOUBLE        , aRoot, aComm);}}
        public static void MPI_Scatter(boolean[] rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_UNSIGNED_CHAR , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_UNSIGNED_CHAR , aRoot, aComm);}}
        public static void MPI_Scatter(char[]    rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_UNSIGNED_SHORT, null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_UNSIGNED_SHORT, aRoot, aComm);}}
        public static void MPI_Scatter(short[]   rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_SHORT         , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_SHORT         , aRoot, aComm);}}
        public static void MPI_Scatter(int[]     rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_INT32_T       , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_INT32_T       , aRoot, aComm);}}
        public static void MPI_Scatter(long[]    rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_INT64_T       , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_INT64_T       , aRoot, aComm);}}
        public static void MPI_Scatter(float[]   rBuf, int aCount, int aRoot, long aComm) throws Error {if (MPI_Comm_rank(aComm) == aRoot) {MPI_Scatter0(true, rBuf, aCount, MPI_FLOAT         , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatter0(false, null, 0, MPI_DATATYPE_NULL, rBuf, aCount, MPI_FLOAT         , aRoot, aComm);}}
        private native static void MPI_Scatter0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRoot, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-scatterv-function"> MPI_Scatterv function </a>
         */
        public static void MPI_Scatterv(byte[]    aSendBuf, int[] aSendCounts, int[] aDispls, byte[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_SIGNED_CHAR   , rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aRoot, aComm);}
        public static void MPI_Scatterv(double[]  aSendBuf, int[] aSendCounts, int[] aDispls, double[]  rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_DOUBLE        , rRecvBuf, aRecvCount, MPI_DOUBLE        , aRoot, aComm);}
        public static void MPI_Scatterv(boolean[] aSendBuf, int[] aSendCounts, int[] aDispls, boolean[] rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_UNSIGNED_CHAR , rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aRoot, aComm);}
        public static void MPI_Scatterv(char[]    aSendBuf, int[] aSendCounts, int[] aDispls, char[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_UNSIGNED_SHORT, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aRoot, aComm);}
        public static void MPI_Scatterv(short[]   aSendBuf, int[] aSendCounts, int[] aDispls, short[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_SHORT         , rRecvBuf, aRecvCount, MPI_SHORT         , aRoot, aComm);}
        public static void MPI_Scatterv(int[]     aSendBuf, int[] aSendCounts, int[] aDispls, int[]     rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_INT32_T       , rRecvBuf, aRecvCount, MPI_INT32_T       , aRoot, aComm);}
        public static void MPI_Scatterv(long[]    aSendBuf, int[] aSendCounts, int[] aDispls, long[]    rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_INT64_T       , rRecvBuf, aRecvCount, MPI_INT64_T       , aRoot, aComm);}
        public static void MPI_Scatterv(float[]   aSendBuf, int[] aSendCounts, int[] aDispls, float[]   rRecvBuf, int aRecvCount, int aRoot, long aComm) throws Error {MPI_Scatterv0(false, aSendBuf, aSendCounts, aDispls, MPI_FLOAT         , rRecvBuf, aRecvCount, MPI_FLOAT         , aRoot, aComm);}
        public static void MPI_Scatterv(byte[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_SIGNED_CHAR   , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_SIGNED_CHAR   , aRoot, aComm);}}
        public static void MPI_Scatterv(double[]  rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_DOUBLE        , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_DOUBLE        , aRoot, aComm);}}
        public static void MPI_Scatterv(boolean[] rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_UNSIGNED_CHAR , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_UNSIGNED_CHAR , aRoot, aComm);}}
        public static void MPI_Scatterv(char[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_UNSIGNED_SHORT, null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_UNSIGNED_SHORT, aRoot, aComm);}}
        public static void MPI_Scatterv(short[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_SHORT         , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_SHORT         , aRoot, aComm);}}
        public static void MPI_Scatterv(int[]     rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_INT32_T       , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_INT32_T       , aRoot, aComm);}}
        public static void MPI_Scatterv(long[]    rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_INT64_T       , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_INT64_T       , aRoot, aComm);}}
        public static void MPI_Scatterv(float[]   rBuf, int[] aCounts, int[] aDispls, int aRoot, long aComm) throws Error {int tRank = MPI_Comm_rank(aComm); if (tRank == aRoot) {MPI_Scatterv0(true, rBuf, aCounts, aDispls, MPI_FLOAT         , null, 0, MPI_DATATYPE_NULL, aRoot, aComm);} else {MPI_Scatterv0(false, null, null, null, MPI_DATATYPE_NULL, rBuf, aCounts[tRank], MPI_FLOAT         , aRoot, aComm);}}
        private native static void MPI_Scatterv0(boolean aInPlace, Object aSendBuf, int[] aSendCounts, int[] aDispls, long aSendType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRoot, long aComm) throws Error;
        
        
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-create-function"> MPI_Comm_create function </a>
         */
        public native static long MPI_Comm_create(long aComm, long aGroup) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-dup-function"> MPI_Comm_dup function </a>
         */
        public native static long MPI_Comm_dup(long aComm) throws Error;
        
        /**
         * Frees a communicator that is allocated with the {@link #MPI_Comm_dup}, {@link #MPI_Comm_create},
         * or {@link #MPI_Comm_split} functions.
         *
         * @param aComm The communicator handle to free.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-free-function"> MPI_Comm_free function </a>
         */
        public native static void MPI_Comm_free(long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-split-function"> MPI_Comm_split function </a>
         */
        public native static long MPI_Comm_split(long aComm, int aColor, int aKey) throws Error;
        
        
        
        /// MPI Group Functions
        /**
         * Retrieves the group that is associated with a communicator.
         *
         * @param aComm The communicator on which to base the group.
         * @return The MPI_Group handle to the group that is associated with the specified communicator.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-comm-group-function"> MPI_Comm_group function </a>
         */
        public native static long MPI_Comm_group(long aComm) throws Error;
        
        /**
         * Creates a new group from the difference between two existing groups.
         *
         * @param aGroup1 The first group.
         * @param aGroup2 The second group.
         * @return A pointer to a handle that represents a new group that contains all elements in the first group that
         * are not present in the second group. The function returns {@link #MPI_GROUP_EMPTY} if the new group is empty.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-difference-function"> MPI_Group_difference function </a>
         */
        public native static long MPI_Group_difference(long aGroup1, long aGroup2) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-excl-function"> MPI_Group_excl function </a>
         */
        public native static long MPI_Group_excl(long aGroup, int aN, int[] aRanks) throws Error;
        
        /**
         * Frees a group.
         * @param aGroup Group to free.
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-free-function"> MPI_Group_free function </a>
         */
        public native static void MPI_Group_free(long aGroup) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-incl-function"> MPI_Group_incl function </a>
         */
        public native static long MPI_Group_incl(long aGroup, int aN, int[] aRanks) throws Error;
        
        /**
         * Creates a new group from the intersection of two existing groups.
         *
         * @param aGroup1 The first group.
         * @param aGroup2 The second group.
         * @return A pointer to a handle that represents a new group with those elements that are present in both groups.
         * The function returns {@link #MPI_GROUP_EMPTY} if the new group is empty.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-intersection-function"> MPI_Group_intersection function </a>
         */
        public native static long MPI_Group_intersection(long aGroup1, long aGroup2) throws Error;
        
        /**
         * Returns the rank of the calling process in the specified group.
         *
         * @param aGroup Specifies the group to query.
         * @return An integer contains the rank of the calling process in the specified group.
         * A value of {@link #MPI_UNDEFINED} that the calling process is not a member of the specified group.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-rank-function"> MPI_Group_rank function </a>
         */
        public native static int MPI_Group_rank(long aGroup) throws Error;
        
        /**
         * Retrieves the size of the specified group.
         *
         * @param aGroup The group to evaluate.
         * @return The size of the specified group.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-size-function"> MPI_Group_size function </a>
         */
        public native static int MPI_Group_size(long aGroup) throws Error;
        
        /**
         * Creates a new group from the union of two existing groups.
         *
         * @param aGroup1 The first group.
         * @param aGroup2 The second group.
         * @return A pointer to a handle that represents a new group that represents all elements in either group.
         *
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-group-union-function"> MPI_Group_union function </a>
         */
        public native static long MPI_Group_union(long aGroup1, long aGroup2) throws Error;
        
        
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-send-function"> MPI_Send function </a>
         */
        public static void MPI_Send(byte[]    aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_SIGNED_CHAR   , aDest, aTag, aComm);}
        public static void MPI_Send(double[]  aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_DOUBLE        , aDest, aTag, aComm);}
        public static void MPI_Send(boolean[] aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_UNSIGNED_CHAR , aDest, aTag, aComm);}
        public static void MPI_Send(char[]    aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_UNSIGNED_SHORT, aDest, aTag, aComm);}
        public static void MPI_Send(short[]   aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_SHORT         , aDest, aTag, aComm);}
        public static void MPI_Send(int[]     aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_INT32_T       , aDest, aTag, aComm);}
        public static void MPI_Send(long[]    aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_INT64_T       , aDest, aTag, aComm);}
        public static void MPI_Send(float[]   aBuf, int aCount, int aDest, int aTag, long aComm) throws Error {MPI_Send0(aBuf, aCount, MPI_FLOAT         , aDest, aTag, aComm);}
        public static void MPI_Send(int aDest, int aTag, long aComm) throws Error {MPI_Send0(null, 0, MPI_BYTE, aDest, aTag, aComm);}
        private native static void MPI_Send0(Object aBuf, int aCount, long aDataType, int aDest, int aTag, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-recv-function"> MPI_Recv function </a>
         */
        public static void MPI_Recv(byte[]    rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_SIGNED_CHAR   , aSource, aTag, aComm);}
        public static void MPI_Recv(double[]  rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_DOUBLE        , aSource, aTag, aComm);}
        public static void MPI_Recv(boolean[] rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_UNSIGNED_CHAR , aSource, aTag, aComm);}
        public static void MPI_Recv(char[]    rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_UNSIGNED_SHORT, aSource, aTag, aComm);}
        public static void MPI_Recv(short[]   rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_SHORT         , aSource, aTag, aComm);}
        public static void MPI_Recv(int[]     rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_INT32_T       , aSource, aTag, aComm);}
        public static void MPI_Recv(long[]    rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_INT64_T       , aSource, aTag, aComm);}
        public static void MPI_Recv(float[]   rBuf, int aCount, int aSource, int aTag, long aComm) throws Error {MPI_Recv0(rBuf, aCount, MPI_FLOAT         , aSource, aTag, aComm);}
        public static void MPI_Recv(int aSource, int aTag, long aComm) throws Error {MPI_Recv0(null, 0, MPI_BYTE, aSource, aTag, aComm);}
        private native static void MPI_Recv0(Object rBuf, int aCount, long aDataType, int aSource, int aTag, long aComm) throws Error;
        
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-sendrecv-function"> MPI_Sendrecv function </a>
         */
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(byte[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SIGNED_CHAR   , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(double[]  aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_DOUBLE        , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(boolean[] aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_CHAR , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(char[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_UNSIGNED_SHORT, aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(short[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_SHORT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(int[]     aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT32_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(long[]    aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_INT64_T       , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, byte[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SIGNED_CHAR   , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, double[]  rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_DOUBLE        , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, boolean[] rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_CHAR , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, char[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_UNSIGNED_SHORT, aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, short[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_SHORT         , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, int[]     rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT32_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, long[]    rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_INT64_T       , aSource, aRecvTag, aComm);}
        public static void MPI_Sendrecv(float[]   aSendBuf, int aSendCount, int aDest, int aSendTag, float[]   rRecvBuf, int aRecvCount, int aSource, int aRecvTag, long aComm) throws Error {MPI_Sendrecv0(aSendBuf, aSendCount, MPI_FLOAT         , aDest, aSendTag, rRecvBuf, aRecvCount, MPI_FLOAT         , aSource, aRecvTag, aComm);}
        private native static void MPI_Sendrecv0(Object aSendBuf, int aSendCount, long aSendType, int aDest, int aSendTag, Object rRecvBuf, int aRecvCount, long aRecvType, int aSource, int aRecvTag, long aComm) throws Error;
        
        
        
        /// MPI External Functions
        /**
         * Initializes the calling MPI process’s execution environment for threaded execution.
         * @param aArgs The argument list for the program
         * @param aRequired The level of desired thread support. Multiple MPI processes
         *                  in the same job may use different values.
         * @return The level of provided thread support.
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-init-thread-function"> MPI_Init_thread function </a>
         */
        public native static int MPI_Init_thread(String[] aArgs, int aRequired) throws Error;
        
        
        
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
