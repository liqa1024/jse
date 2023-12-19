package jtool.parallel;

import jtool.code.UT;
import org.jetbrains.annotations.ApiStatus;

import static jtool.code.CS.*;
import static jtool.code.CS.Exec.EXE;
import static jtool.code.CS.Exec.JAR_DIR;

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
    
    public enum Comm {
          NULL (Native.MPI_COMM_NULL )
        , WORLD(Native.MPI_COMM_WORLD)
        , SELF (Native.MPI_COMM_SELF )
        ;
        
        private final long mPtr;
        Comm(long aPtr) {mPtr = aPtr;}
        @ApiStatus.Internal public long ptr_() {return mPtr;}
        
        /** @return the number of calling process within the group of the communicator. */
        public int rank() {return Native.MPI_Comm_rank(mPtr);}
        /** @return the number of processes in the group for the communicator. */
        public int size() {return Native.MPI_Comm_size(mPtr);}
        
        
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
        public <S, R> void allgather(S aSendBuf, int aSendCount, R rRecvBuf, int aRecvCount) {
            Native.MPI_Allgather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, mPtr);
        }
        public <R> void allgather(R rRecvBuf, int aRecvCount) {
            Native.MPI_Allgather(Native.MPI_IN_PLACE, 0, rRecvBuf, aRecvCount, mPtr);
        }
        
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
        public <S, R> void allgatherv(S aSendBuf, int aSendCount, R rRecvBuf, int[] aRecvCounts, int[] aDispls) {
            Native.MPI_Allgatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, mPtr);
        }
        public <R> void allgatherv(R rRecvBuf, int[] aRecvCounts, int[] aDispls) {
            Native.MPI_Allgatherv(Native.MPI_IN_PLACE, 0, rRecvBuf, aRecvCounts, aDispls, mPtr);
        }
        
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
        public <T> void allreduce(T aSendBuf, T rRecvBuf, int aCount, Op aOp) {
            Native.MPI_Allreduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, mPtr);
        }
        public <T> void allreduce(T rRecvBuf, int aCount, Op aOp) {
            Native.MPI_Allreduce(Native.MPI_IN_PLACE, rRecvBuf, aCount, aOp.mPtr, mPtr);
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
        public <T> void bcast(T rBuf, int aCount, int aRoot) {
            Native.MPI_Bcast(rBuf, aCount, aRoot, mPtr);
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
         * @see <a href="https://learn.microsoft.com/en-us/message-passing-interface/mpi-gather-function"> MPI_Gather function </a>
         */
        public <S, R> void gather(S aSendBuf, int aSendCount, R rRecvBuf, int aRecvCount, int aRoot) {
            Native.MPI_Gather(aSendBuf, aSendCount, rRecvBuf, aRecvCount, aRoot, mPtr);
        }
        public <R> void gather(R rRecvBuf, int aRecvCount, int aRoot) {
            Native.MPI_Gather(Native.MPI_IN_PLACE, 0, rRecvBuf, aRecvCount, aRoot, mPtr);
        }
        
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
        public <S, R> void gatherv(S aSendBuf, int aSendCount, R rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) {
            Native.MPI_Gatherv(aSendBuf, aSendCount, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);
        }
        public <R> void gatherv(R rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot) {
            Native.MPI_Gatherv(Native.MPI_IN_PLACE, 0, rRecvBuf, aRecvCounts, aDispls, aRoot, mPtr);
        }
        
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
        public <T> void reduce(T aSendBuf, T rRecvBuf, int aCount, Op aOp, int aRoot) {
            Native.MPI_Reduce(aSendBuf, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);
        }
        public <T> void reduce(T rRecvBuf, int aCount, Op aOp, int aRoot) {
            Native.MPI_Reduce(Native.MPI_IN_PLACE, rRecvBuf, aCount, aOp.mPtr, aRoot, mPtr);
        }
        
        
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
        public <T> void send(T aBuf, int aCount, int aDest, int aTag) {
            Native.MPI_Send(aBuf, aCount, aDest, aTag, mPtr);
        }
        public <T> void send(T aBuf, int aCount, int aDest) {
            Native.MPI_Send(aBuf, aCount, aDest, 0, mPtr);
        }
        
        /**
         * Performs a receive operation and does not return until a matching message is received.
         *
         * @param aBuf The data array to be received
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
        public <T> void recv(T aBuf, int aCount, int aSource, int aTag) {
            Native.MPI_Recv(aBuf, aCount, aSource, aTag, mPtr);
        }
        public <T> void recv(T aBuf, int aCount, int aSource) {
            Native.MPI_Recv(aBuf, aCount, aSource, Tag.ANY, mPtr);
        }
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
    
    
    
    
    /// 基础功能
    /**
     * Initializes the calling MPI process’s execution environment for single threaded execution.
     * @param aArgs the argument list for the program
     */
    public static void init(String[] aArgs) {Native.MPI_Init(aArgs);}
    /**
     * Indicates whether {@link MPI#init} has been called.
     * @return true if {@link MPI#init} or {@link MPI#initThread} has been called and false otherwise.
     */
    public static boolean initialized() {return Native.MPI_Initialized();}
    
    /**
     * Terminates the calling MPI process’s execution environment.
     */
    public static void shutdown() {Native.MPI_Finalize();}
    /**
     * Indicates whether {@link MPI#shutdown} has been called.
     * @return true if MPI_Finalize has been called and false otherwise.
     */
    public static boolean isShutdown() {return Native.MPI_Finalized();}
    
    
    
    
    /// MPI External Functions
    /**
     * Initializes the calling MPI process’s execution environment for threaded execution.
     * @param aArgs The argument list for the program
     * @param aRequired The level of desired thread support. Multiple MPI processes
     *                  in the same job may use different values.
     * @return The level of provided thread support.
     */
    public static int initThread(String[] aArgs, int aRequired) {return Native.MPI_Init_thread(aArgs, aRequired);}
    
    
    
    
    /**
     * 提供按照原始的 MPI 标准格式的接口以及对应的 native 实现
     * @author liqa
     */
    public static class Native {
        private Native() {}
        
        private final static String MPILIB_DIR = JAR_DIR+"mpi/";
        private final static String MPILIB_PATH = MPILIB_DIR + (IS_WINDOWS ? "mpi.dll" : (IS_MAC ? "mpi.jnilib" : "mpi.so"));
        private final static String[] MPISRC_NAME = {
              "CMakeLists.txt"
            , "jtool_parallel_MPI_Native.c"
            , "jtool_parallel_MPI_Native.h"
        };
        
        private static void initMPI_() throws Exception {
            // 如果没有 cmake，在 windows 上，尝试直接用预编译的库
            EXE.setNoSTDOutput().setNoERROutput();
            boolean tNoCmake = EXE.system("cmake --version") != 0;
            EXE.setNoSTDOutput(false).setNoERROutput(false);
            if (tNoCmake) {
                if (IS_WINDOWS) {
                    System.err.println("MPI BUILD WARNING: No camke environment, using the pre-build libraries,");
                    System.err.println("  which may have some problems.");
                    UT.IO.copy(UT.IO.getResource("mpi/lib/mpi.dll"), MPILIB_PATH);
                    return;
                } else {
                    throw new Exception("MPI BUILD ERROR: No camke environment.");
                }
            }
            // 从内部资源解压到临时目录
            String tWorkingDir = WORKING_DIR.replaceAll("%n", "mpisrc");
            for (String tName : MPISRC_NAME) {
                UT.IO.copy(UT.IO.getResource("mpi/src/"+tName), tWorkingDir+tName);
            }
            System.out.println("MPI INIT INFO: Building mpijni from source code...");
            String tBuildDir = tWorkingDir+"build/";
            UT.IO.makeDir(tBuildDir);
            // 直接通过系统指令来编译 mpijni 的库，关闭输出
            EXE.setNoSTDOutput();
            EXE.system(String.format("cd %s; cmake ..; cmake --build . --config Release", tBuildDir));
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
            // 如果不存在 jni lib 则需要重新通过源码编译
            if (!UT.IO.isFile(MPILIB_PATH)) {
                System.out.println("MPI INIT INFO: mpijni libraries not found. Reinstalling...");
                try {initMPI_();}
                catch (Exception e) {throw new RuntimeException(e);}
            }
            // 设置库路径
            System.load(UT.IO.toAbsolutePath(MPILIB_PATH));
            
            // 初始化 final 常量
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
        }
        
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
        
        private static long datatypeOf_(Object aBuf) {
            if (aBuf == null) {
                return MPI_DATATYPE_NULL;
            } else
            if (aBuf instanceof byte[]) {
                return MPI_SIGNED_CHAR;
            } else
            if (aBuf instanceof double[]) {
                return MPI_DOUBLE;
            } else
            if (aBuf instanceof boolean[]) {
                return MPI_UNSIGNED_CHAR;
            } else
            if (aBuf instanceof char[]) {
                return MPI_UNSIGNED_SHORT;
            } else
            if (aBuf instanceof short[]) {
                return MPI_SHORT;
            } else
            if (aBuf instanceof int[]) {
                return MPI_INT32_T;
            } else
            if (aBuf instanceof long[]) {
                return MPI_INT64_T;
            } else
            if (aBuf instanceof float[]) {
                return MPI_FLOAT;
            } else {
                throw new RuntimeException("Unexpected datatype: "+aBuf.getClass().getName());
            }
        }
        
        public final static byte[] MPI_IN_PLACE = new byte[0];
        
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
        
        /* Used in: Tag */
        public final static int MPI_ANY_TAG;
        private native static int getMpiAnyTag_();
        
        
        /// 基础功能
        /**
         * Initializes the calling MPI process’s execution environment for single threaded execution.
         * @param aArgs the argument list for the program
         */
        public native static void MPI_Init(String[] aArgs);
        /**
         * Indicates whether {@link #MPI_Init} has been called.
         * @return true if {@link #MPI_Init} or {@link #MPI_Init_thread} has been called and false otherwise.
         */
        public native static boolean MPI_Initialized();
        
        /**
         * Retrieves the rank of the calling process in the group of the specified communicator.
         * @param aComm The communicator.
         * @return the number of calling process within the group of the communicator.
         */
        public native static int MPI_Comm_rank(long aComm);
        
        /**
         * Retrieves the number of processes involved in a communicator, or the total number of
         * processes available.
         * @param aComm The communicator to evaluate. Specify the {@link #MPI_COMM_WORLD} constant to retrieve
         *              the total number of processes available.
         * @return the number of processes in the group for the communicator.
         */
        public native static int MPI_Comm_size(long aComm);
        
        /**
         * Terminates the calling MPI process’s execution environment.
         */
        public native static void MPI_Finalize();
        /**
         * Indicates whether {@link #MPI_Finalize} has been called.
         * @return true if MPI_Finalize has been called and false otherwise.
         */
        public native static boolean MPI_Finalized();
        
        
        
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
         *                 <p>
         *                 If the comm parameter references an intracommunicator, you can specify an
         *                 in-place option by specifying {@link #MPI_IN_PLACE} in all processes.
         *                 The aSendCount parameter and the type of data are ignored. Each process enters
         *                 data in the corresponding receive buffer element.
         *                 The nth process sends data to the nth element of the receive buffer.
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
        public static <S, R> void MPI_Allgather(S aSendBuf, int aSendCount, R rRecvBuf, int aRecvCount, long aComm) {
            MPI_Allgather0(aSendBuf==MPI_IN_PLACE, aSendBuf, aSendCount, datatypeOf_(aSendBuf), rRecvBuf, aRecvCount, datatypeOf_(rRecvBuf), aComm);
        }
        private native static void MPI_Allgather0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int aRecvCount, long aRecvType, long aComm);
        
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
         *                 <p>
         *                 If the comm parameter references an intracommunicator, you can specify an
         *                 in-place option by specifying {@link #MPI_IN_PLACE} in all processes.
         *                 The aSendCount parameter and the type of data are ignored. Each process enters
         *                 data in the corresponding receive buffer element.
         *                 The nth process sends data to the nth element of the receive buffer.
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
        public static <S, R> void MPI_Allgatherv(S aSendBuf, int aSendCount, R rRecvBuf, int[] aRecvCounts, int[] aDispls, long aComm) {
            MPI_Allgatherv0(aSendBuf==MPI_IN_PLACE, aSendBuf, aSendCount, datatypeOf_(aSendBuf), rRecvBuf, aRecvCounts, aDispls, datatypeOf_(rRecvBuf), aComm);
        }
        private native static void MPI_Allgatherv0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int[] aRecvCounts, int[] aDispls, long aRecvType, long aComm);
        
        /**
         * Combines values from all processes and distributes the result back to all processes.
         *
         * @param aSendBuf The data array to be sent to all processes in the group. The number
         *                 of the elements in the buffer are specified in the aSendCount and
         *                 the data type will be detected automatically.
         *                 Each element in the buffer corresponds to a process in the group.
         *                 <p>
         *                 If the comm parameter references an intracommunicator, you can specify an
         *                 in-place option by specifying {@link #MPI_IN_PLACE} in all processes.
         *                 In this case, the input data is taken at each process from the rRecvBuf buffer,
         *                 where it will be replaced by the output data.
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
        public static <T> void MPI_Allreduce(T aSendBuf, T rRecvBuf, int aCount, long aOp, long aComm) {
            MPI_Allreduce0(aSendBuf==MPI_IN_PLACE, aSendBuf, rRecvBuf, aCount, datatypeOf_(aSendBuf), aOp, aComm);
        }
        private native static void MPI_Allreduce0(boolean aInPlace, Object aSendBuf, Object rRecvBuf, int aCount, long aDataType, long aOp, long aComm);
        
        
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
        public static <T> void MPI_Bcast(T rBuf, int aCount, int aRoot, long aComm) {
            MPI_Bcast0(rBuf, aCount, datatypeOf_(rBuf), aRoot, aComm);
        }
        private native static void MPI_Bcast0(Object rBuf, int aCount, long aDataType, int aRoot, long aComm);
        
        
        /**
         * Gathers data from all members of a group to one member.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *                 <p>
         *                 If the comm parameter references an intracommunicator, you can specify an
         *                 in-place option by specifying {@link #MPI_IN_PLACE} in all processes.
         *                 The aSendCount parameter and the type of data are ignored. Each process enters
         *                 data in the corresponding receive buffer element.
         *                 The nth process sends data to the nth element of the receive buffer.
         *                 The data that is sent by the root process is assumed to be in the correct place
         *                 in the receive buffer.
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
        public static <S, R> void MPI_Gather(S aSendBuf, int aSendCount, R rRecvBuf, int aRecvCount, int aRoot, long aComm) {
            MPI_Gather0(aSendBuf==MPI_IN_PLACE, aSendBuf, aSendCount, datatypeOf_(aSendBuf), rRecvBuf, aRecvCount, datatypeOf_(rRecvBuf), aRoot, aComm);
        }
        private native static void MPI_Gather0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int aRecvCount, long aRecvType, int aRoot, long aComm);
        
        /**
         * Gathers variable data from all members of a group to one member.
         * The MPI_Gatherv function adds flexibility to the {@link #MPI_Gather} function by
         * allowing a varying count of data from each process.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *                 <p>
         *                 If the comm parameter references an intracommunicator, you can specify an
         *                 in-place option by specifying {@link #MPI_IN_PLACE} in all processes.
         *                 The aSendCount parameter and the type of data are ignored. Each process enters
         *                 data in the corresponding receive buffer element.
         *                 The nth process sends data to the nth element of the receive buffer.
         *                 The data that is sent by the root process is assumed to be in the correct place
         *                 in the receive buffer.
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
        public static <S, R> void MPI_Gatherv(S aSendBuf, int aSendCount, R rRecvBuf, int[] aRecvCounts, int[] aDispls, int aRoot, long aComm) {
            MPI_Gatherv0(aSendBuf==MPI_IN_PLACE, aSendBuf, aSendCount, datatypeOf_(aSendBuf), rRecvBuf, aRecvCounts, aDispls, datatypeOf_(rRecvBuf), aRoot, aComm);
        }
        private native static void MPI_Gatherv0(boolean aInPlace, Object aSendBuf, int aSendCount, long aSendType, Object rRecvBuf, int[] aRecvCounts, int[] aDispls, long aRecvType, int aRoot, long aComm);
        
        /**
         * Performs a global reduce operation across all members of a group. You can specify
         * a predefined mathematical or logical operation or an application-defined operation.
         *
         * @param aSendBuf The data array to be sent to the root process.
         *                 <p>
         *                 If the comm parameter references an intracommunicator, you can specify an
         *                 in-place option by specifying {@link #MPI_IN_PLACE} in all processes.
         *                 The aSendCount parameter and the type of data are ignored. Each process enters
         *                 data in the corresponding receive buffer element.
         *                 The nth process sends data to the nth element of the receive buffer.
         *                 The data that is sent by the root process is assumed to be in the correct place
         *                 in the receive buffer.
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
        public static <T> void MPI_Reduce(T aSendBuf, T rRecvBuf, int aCount, long aOp, int aRoot, long aComm) {
            MPI_Reduce0(aSendBuf==MPI_IN_PLACE, aSendBuf, rRecvBuf, aCount, datatypeOf_(aSendBuf), aOp, aRoot, aComm);
        }
        private native static void MPI_Reduce0(boolean aInPlace, Object aSendBuf, Object rRecvBuf, int aCount, long aDataType, long aOp, int aRoot, long aComm);
        
        
        
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
        public static <T> void MPI_Send(T aBuf, int aCount, int aDest, int aTag, long aComm) {
            MPI_Send0(aBuf, aCount, datatypeOf_(aBuf), aDest, aTag, aComm);
        }
        private native static void MPI_Send0(Object aBuf, int aCount, long aDataType, int aDest, int aTag, long aComm);
        
        /**
         * Performs a receive operation and does not return until a matching message is received.
         *
         * @param aBuf The data array to be received
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
        public static <T> void MPI_Recv(T aBuf, int aCount, int aSource, int aTag, long aComm) {
            MPI_Recv0(aBuf, aCount, datatypeOf_(aBuf), aSource, aTag, aComm);
        }
        private native static void MPI_Recv0(Object aBuf, int aCount, long aDataType, int aSource, int aTag, long aComm);
        
        
        
        
        /// MPI External Functions
        /**
         * Initializes the calling MPI process’s execution environment for threaded execution.
         * @param aArgs The argument list for the program
         * @param aRequired The level of desired thread support. Multiple MPI processes
         *                  in the same job may use different values.
         * @return The level of provided thread support.
         */
        public native static int MPI_Init_thread(String[] aArgs, int aRequired);
        
        
        
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
