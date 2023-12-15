package jtool.parallel;

import jtool.code.UT;

import static jtool.code.CS.Exec.JAR_DIR;
import static jtool.code.CS.IS_WINDOWS;

/**
 * 基于 jni 实现的 MPI wrapper, 介绍部分基于
 * <a href="https://learn.microsoft.com/zh-cn/message-passing-interface/microsoft-mpi">
 * Microsoft MPI </a> 的标准；
 * 所有函数名称按照原始的 MPI 标准而不是流行的 java binding 中使用的标准，
 * 从而保证对 c 风格的 MPI 有更好的一致性；
 * 在此基础上提供一套完全按照 java 风格的接口来方便使用（而不是原本的不伦不类的风格）。
 * <p>
 * 为了保证接口简洁，这里不再返回错误码，并且暂时不进行错误抛出；
 * 为了避免错误并且加速上线，这里不实现我还没理解的功能
 * <p>
 * 使用：
 * <pre> {@code
 * import static jtool.parallel.MPI.*;
 *
 * MPI_Init(args);
 * int me = MPI_Comm_rank(MPI_COMM_WORLD);
 * System.out.println("Hi from <"+me+">");
 * MPI_Finalize();
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
    
    private final static String MPILIB_DIR = JAR_DIR+"mpi/";
    private final static String MPILIB_PATH = MPILIB_DIR + (IS_WINDOWS ? "mpi.dll" : "mpi.so");
    
    public final static class MPI_Comm {
        private final long mPtr;
        MPI_Comm(long aPtr) {mPtr = aPtr;}
        
        /** @return the number of calling process within the group of the communicator. */
        public int getRank() {return MPI_Comm_rank0(mPtr);}
        /** @return the number of processes in the group for the communicator. */
        public int getSize() {return MPI_Comm_size0(mPtr);}
    }
    public static MPI_Comm MPI_COMM_NULL, MPI_COMM_WORLD, MPI_COMM_SELF;
    
    private native static long getMpiCommNull();
    private native static long getMpiCommWorld();
    private native static long getMpiCommSelf();
    
    public final static class MPI_Datatype {
        private final long mPtr;
        MPI_Datatype(long aPtr) {mPtr = aPtr;}
    }
    public static MPI_Datatype MPI_DATATYPE_NULL, MPI_CHAR, MPI_UNSIGNED_CHAR, MPI_SHORT, MPI_UNSIGNED_SHORT, MPI_INT, MPI_UNSIGNED, MPI_LONG, MPI_UNSIGNED_LONG, MPI_LONG_LONG_INT, MPI_LONG_LONG, MPI_FLOAT, MPI_DOUBLE, MPI_LONG_DOUBLE, MPI_BYTE, MPI_WCHAR, MPI_PACKED, MPI_LB, MPI_UB, MPI_C_COMPLEX, MPI_C_FLOAT_COMPLEX, MPI_C_DOUBLE_COMPLEX, MPI_C_LONG_DOUBLE_COMPLEX, MPI_2INT, MPI_C_BOOL, MPI_SIGNED_CHAR, MPI_UNSIGNED_LONG_LONG, MPI_CHARACTER, MPI_INTEGER, MPI_REAL, MPI_LOGICAL, MPI_COMPLEX, MPI_DOUBLE_PRECISION, MPI_2INTEGER, MPI_2REAL, MPI_DOUBLE_COMPLEX, MPI_2DOUBLE_PRECISION, MPI_2COMPLEX, MPI_2DOUBLE_COMPLEX, MPI_REAL2, MPI_REAL4, MPI_COMPLEX8, MPI_REAL8, MPI_COMPLEX16, MPI_REAL16, MPI_COMPLEX32, MPI_INTEGER1, MPI_COMPLEX4, MPI_INTEGER2, MPI_INTEGER4, MPI_INTEGER8, MPI_INTEGER16, MPI_INT8_T, MPI_INT16_T, MPI_INT32_T, MPI_INT64_T, MPI_UINT8_T, MPI_UINT16_T, MPI_UINT32_T, MPI_UINT64_T, MPI_AINT, MPI_OFFSET, MPI_FLOAT_INT, MPI_DOUBLE_INT, MPI_LONG_INT, MPI_SHORT_INT, MPI_LONG_DOUBLE_INT;
    
    private static MPI_Datatype datatypeOf(Object aBuf) {
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
    
    private native static long getMpiDatatypeNull();
    private native static long getMpiChar();
    private native static long getMpiUnsignedChar();
    private native static long getMpiShort();
    private native static long getMpiUnsignedShort();
    private native static long getMpiInt();
    private native static long getMpiUnsigned();
    private native static long getMpiLong();
    private native static long getMpiUnsignedLong();
    private native static long getMpiLongLongInt();
    private native static long getMpiLongLong();
    private native static long getMpiFloat();
    private native static long getMpiDouble();
    private native static long getMpiLongDouble();
    private native static long getMpiByte();
    private native static long getMpiWchar();
    private native static long getMpiPacked();
    private native static long getMpiLb();
    private native static long getMpiUb();
    private native static long getMpiCComplex();
    private native static long getMpiCFloatComplex();
    private native static long getMpiCDoubleComplex();
    private native static long getMpiCLongDoubleComplex();
    private native static long getMpi2Int();
    private native static long getMpiCBool();
    private native static long getMpiSignedChar();
    private native static long getMpiUnsignedLongLong();
    private native static long getMpiCharacter();
    private native static long getMpiInteger();
    private native static long getMpiReal();
    private native static long getMpiLogical();
    private native static long getMpiComplex();
    private native static long getMpiDoublePrecision();
    private native static long getMpi2Integer();
    private native static long getMpi2Real();
    private native static long getMpiDoubleComplex();
    private native static long getMpi2DoublePrecision();
    private native static long getMpi2Complex();
    private native static long getMpi2DoubleComplex();
    private native static long getMpiReal2();
    private native static long getMpiReal4();
    private native static long getMpiComplex8();
    private native static long getMpiReal8();
    private native static long getMpiComplex16();
    private native static long getMpiReal16();
    private native static long getMpiComplex32();
    private native static long getMpiInteger1();
    private native static long getMpiComplex4();
    private native static long getMpiInteger2();
    private native static long getMpiInteger4();
    private native static long getMpiInteger8();
    private native static long getMpiInteger16();
    private native static long getMpiInt8T();
    private native static long getMpiInt16T();
    private native static long getMpiInt32T();
    private native static long getMpiInt64T();
    private native static long getMpiUint8T();
    private native static long getMpiUint16T();
    private native static long getMpiUint32T();
    private native static long getMpiUint64T();
    private native static long getMpiAint();
    private native static long getMpiOffset();
    private native static long getMpiFloatInt();
    private native static long getMpiDoubleInt();
    private native static long getMpiLongInt();
    private native static long getMpiShortInt();
    private native static long getMpiLongDoubleInt();
    
    
    
    /// 基础功能
    /**
     * Initializes the calling MPI process’s execution environment for single threaded execution.
     * @param aArgs the argument list for the program
     */
    public static void MPI_Init(String[] aArgs) {
        // 检测 jni lib 以及编译相关操作
        System.load(UT.IO.toAbsolutePath(MPILIB_PATH));
        
        MPI_Init0(aArgs);
        
        // 在初始化后再获取 Comm，避免意料外的问题
        MPI_COMM_NULL  = new MPI_Comm(getMpiCommNull());
        MPI_COMM_WORLD = new MPI_Comm(getMpiCommWorld());
        MPI_COMM_SELF  = new MPI_Comm(getMpiCommSelf());
        
        // 在初始化后再获取 Datatype，避免意料外的问题
        MPI_DATATYPE_NULL          = new MPI_Datatype(getMpiDatatypeNull());
        MPI_CHAR                   = new MPI_Datatype(getMpiChar());
        MPI_UNSIGNED_CHAR          = new MPI_Datatype(getMpiUnsignedChar());
        MPI_SHORT                  = new MPI_Datatype(getMpiShort());
        MPI_UNSIGNED_SHORT         = new MPI_Datatype(getMpiUnsignedShort());
        MPI_INT                    = new MPI_Datatype(getMpiInt());
        MPI_UNSIGNED               = new MPI_Datatype(getMpiUnsigned());
        MPI_LONG                   = new MPI_Datatype(getMpiLong());
        MPI_UNSIGNED_LONG          = new MPI_Datatype(getMpiUnsignedLong());
        MPI_LONG_LONG_INT          = new MPI_Datatype(getMpiLongLongInt());
        MPI_LONG_LONG              = new MPI_Datatype(getMpiLongLong());
        MPI_FLOAT                  = new MPI_Datatype(getMpiFloat());
        MPI_DOUBLE                 = new MPI_Datatype(getMpiDouble());
        MPI_LONG_DOUBLE            = new MPI_Datatype(getMpiLongDouble());
        MPI_BYTE                   = new MPI_Datatype(getMpiByte());
        MPI_WCHAR                  = new MPI_Datatype(getMpiWchar());
        MPI_PACKED                 = new MPI_Datatype(getMpiPacked());
        MPI_LB                     = new MPI_Datatype(getMpiLb());
        MPI_UB                     = new MPI_Datatype(getMpiUb());
        MPI_C_COMPLEX              = new MPI_Datatype(getMpiCComplex());
        MPI_C_FLOAT_COMPLEX        = new MPI_Datatype(getMpiCFloatComplex());
        MPI_C_DOUBLE_COMPLEX       = new MPI_Datatype(getMpiCDoubleComplex());
        MPI_C_LONG_DOUBLE_COMPLEX  = new MPI_Datatype(getMpiCLongDoubleComplex());
        MPI_2INT                   = new MPI_Datatype(getMpi2Int());
        MPI_C_BOOL                 = new MPI_Datatype(getMpiCBool());
        MPI_SIGNED_CHAR            = new MPI_Datatype(getMpiSignedChar());
        MPI_UNSIGNED_LONG_LONG     = new MPI_Datatype(getMpiUnsignedLongLong());
        MPI_CHARACTER              = new MPI_Datatype(getMpiCharacter());
        MPI_INTEGER                = new MPI_Datatype(getMpiInteger());
        MPI_REAL                   = new MPI_Datatype(getMpiReal());
        MPI_LOGICAL                = new MPI_Datatype(getMpiLogical());
        MPI_COMPLEX                = new MPI_Datatype(getMpiComplex());
        MPI_DOUBLE_PRECISION       = new MPI_Datatype(getMpiDoublePrecision());
        MPI_2INTEGER               = new MPI_Datatype(getMpi2Integer());
        MPI_2REAL                  = new MPI_Datatype(getMpi2Real());
        MPI_DOUBLE_COMPLEX         = new MPI_Datatype(getMpiDoubleComplex());
        MPI_2DOUBLE_PRECISION      = new MPI_Datatype(getMpi2DoublePrecision());
        MPI_2COMPLEX               = new MPI_Datatype(getMpi2Complex());
        MPI_2DOUBLE_COMPLEX        = new MPI_Datatype(getMpi2DoubleComplex());
        MPI_REAL2                  = new MPI_Datatype(getMpiReal2());
        MPI_REAL4                  = new MPI_Datatype(getMpiReal4());
        MPI_COMPLEX8               = new MPI_Datatype(getMpiComplex8());
        MPI_REAL8                  = new MPI_Datatype(getMpiReal8());
        MPI_COMPLEX16              = new MPI_Datatype(getMpiComplex16());
        MPI_REAL16                 = new MPI_Datatype(getMpiReal16());
        MPI_COMPLEX32              = new MPI_Datatype(getMpiComplex32());
        MPI_INTEGER1               = new MPI_Datatype(getMpiInteger1());
        MPI_COMPLEX4               = new MPI_Datatype(getMpiComplex4());
        MPI_INTEGER2               = new MPI_Datatype(getMpiInteger2());
        MPI_INTEGER4               = new MPI_Datatype(getMpiInteger4());
        MPI_INTEGER8               = new MPI_Datatype(getMpiInteger8());
        MPI_INTEGER16              = new MPI_Datatype(getMpiInteger16());
        MPI_INT8_T                 = new MPI_Datatype(getMpiInt8T());
        MPI_INT16_T                = new MPI_Datatype(getMpiInt16T());
        MPI_INT32_T                = new MPI_Datatype(getMpiInt32T());
        MPI_INT64_T                = new MPI_Datatype(getMpiInt64T());
        MPI_UINT8_T                = new MPI_Datatype(getMpiUint8T());
        MPI_UINT16_T               = new MPI_Datatype(getMpiUint16T());
        MPI_UINT32_T               = new MPI_Datatype(getMpiUint32T());
        MPI_UINT64_T               = new MPI_Datatype(getMpiUint64T());
        MPI_AINT                   = new MPI_Datatype(getMpiAint());
        MPI_OFFSET                 = new MPI_Datatype(getMpiOffset());
        MPI_FLOAT_INT              = new MPI_Datatype(getMpiFloatInt());
        MPI_DOUBLE_INT             = new MPI_Datatype(getMpiDoubleInt());
        MPI_LONG_INT               = new MPI_Datatype(getMpiLongInt());
        MPI_SHORT_INT              = new MPI_Datatype(getMpiShortInt());
        MPI_LONG_DOUBLE_INT        = new MPI_Datatype(getMpiLongDoubleInt());
    }
    private native static void MPI_Init0(String[] aArgs);
    
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
    public static int MPI_Comm_rank(MPI_Comm aComm) {return MPI_Comm_rank0(aComm.mPtr);}
    private native static int MPI_Comm_rank0(long aCommPtr);
    
    /**
     * Retrieves the number of processes involved in a communicator, or the total number of
     * processes available.
     * @param aComm The communicator to evaluate. Specify the {@link #MPI_COMM_WORLD} constant to retrieve
     *              the total number of processes available.
     * @return the number of processes in the group for the communicator.
     */
    public static int MPI_Comm_size(MPI_Comm aComm) {return MPI_Comm_size0(aComm.mPtr);}
    private native static int MPI_Comm_size0(long aCommPtr);
    
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
    public static <R, S> void MPI_Allgather(S aSendBuf, int aSendCount, R aRecvBuf, int aRecvCount, MPI_Comm aComm) {
        MPI_Allgather0(aSendBuf, aSendCount, datatypeOf(aSendBuf).mPtr, aRecvBuf, aRecvCount, datatypeOf(aRecvBuf).mPtr, aComm.mPtr);
    }
    private native static void MPI_Allgather0(Object aSendBuf, int aSendCount, long aSendTypePtr, Object aRecvBuf, int aRecvCount, long aRecvTypePtr, long aCommPtr);
    
    
    
//    /// MPI Caching Functions
//    @FunctionalInterface public interface MPI_Comm_copy_attr_function {
//        /**
//         * a placeholder for the application-defined function name.
//         * @param aOldComm Original communicator.
//         * @param aCommKeyval Key value.
//         * @param aExtraState Extra state.
//         * @param aAttributeValIn Source attribute value.
//         * @param aAttributeValOut Destination attribute value.
//         * @return if false, then the attribute is deleted in the duplicated communicator.
//         * Otherwise (true), the new attribute value is set to the value returned in
//         * aAttributeValOut.
//         */
//        boolean call(MPI_Comm aOldComm, int aCommKeyval, Object aExtraState, Object aAttributeValIn, Object aAttributeValOut);
//    }
//
//    @FunctionalInterface public interface MPI_Comm_delete_attr_function {
//        /**
//         * a placeholder for the application-defined function name.
//         * @param aComm Communicator.
//         * @param aCommKeyval Key value.
//         * @param aAttributeVal Attribute value.
//         * @param aExtraState Extra state.
//         */
//        void call(MPI_Comm aComm, int aCommKeyval, Object aAttributeVal, Object aExtraState);
//    }
//
//    /**
//     * Creates a new attribute key.
//     * @param aCommCopyAttrFn Copy callback function for keyval.
//     * @param aCommDeleteAttrFn Delete callback function for keyval.
//     * @param aExtraState Extra state for callback functions.
//     * @return Key value for future access.
//     */
//    public native static int MPI_Comm_create_keyval(MPI_Comm_copy_attr_function aCommCopyAttrFn, MPI_Comm_delete_attr_function aCommDeleteAttrFn, Object aExtraState);
}
