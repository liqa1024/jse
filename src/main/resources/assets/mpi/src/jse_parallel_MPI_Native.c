#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-use-auto"
#pragma ide diagnostic ignored "modernize-use-nullptr"
#endif

#include "mpi.h"
#include "jniutil.h"
#include "jse_parallel_MPI_Native.h"


#define MPI_JNULL    MPI_DATATYPE_NULL
#define MPI_JBYTE    MPI_INT8_T
#define MPI_JDOUBLE  MPI_DOUBLE
#define MPI_JBOOLEAN MPI_UNSIGNED_CHAR
#define MPI_JCHAR    MPI_UINT16_T
#define MPI_JSHORT   MPI_INT16_T
#define MPI_JINT     MPI_INT32_T
#define MPI_JLONG    MPI_INT64_T
#define MPI_JFLOAT   MPI_FLOAT

#ifdef __cplusplus
extern "C" {
#endif

/** utils for mpi */
inline jboolean exceptionCheckMPI(JNIEnv *aEnv, int aExitCode) {
    if (aExitCode == MPI_SUCCESS) return JNI_FALSE;
    
    char rErrStr[MPI_MAX_ERROR_STRING];
    int rLen;
    MPI_Error_string(aExitCode, rErrStr, &rLen);
    
    throwExceptionMPI(aEnv, rErrStr, aExitCode);
    return JNI_TRUE;
}

inline int getSizeAndRank(MPI_Comm aComm, int* rSize, int* rRank) {
    int tExitCode;
    tExitCode = MPI_Comm_size(aComm, rSize);
    if (tExitCode != MPI_SUCCESS) return tExitCode;
    tExitCode = MPI_Comm_rank(aComm, rRank);
    return tExitCode;
}


JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiGroupNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_GROUP_NULL ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiGroupEmpty_1(JNIEnv *aEnv, jclass aClazz) {return MPI_GROUP_EMPTY;}

JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiCommNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_COMM_NULL ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiCommWorld_1(JNIEnv *aEnv, jclass aClazz) {return MPI_COMM_WORLD;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiCommSelf_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_COMM_SELF ;}

JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiOpNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_OP_NULL;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiMax_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_MAX    ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiMin_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_MIN    ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiSum_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_SUM    ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiProd_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_PROD   ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiLand_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_LAND   ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiBand_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_BAND   ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiLor_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_LOR    ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiBor_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_BOR    ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiLxor_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_LXOR   ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiBxor_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_BXOR   ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiMinloc_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_MINLOC ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiMaxloc_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_MAXLOC ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiReplace_1(JNIEnv *aEnv, jclass aClazz) {return MPI_REPLACE;}

JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiDatatypeNull_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_DATATYPE_NULL     ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiChar_1            (JNIEnv *aEnv, jclass aClazz) {return MPI_CHAR              ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUnsignedChar_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_CHAR     ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiShort_1           (JNIEnv *aEnv, jclass aClazz) {return MPI_SHORT             ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUnsignedShort_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_SHORT    ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiInt_1             (JNIEnv *aEnv, jclass aClazz) {return MPI_INT               ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUnsigned_1        (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED          ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiLong_1            (JNIEnv *aEnv, jclass aClazz) {return MPI_LONG              ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUnsignedLong_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_LONG     ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiLongLong_1        (JNIEnv *aEnv, jclass aClazz) {return MPI_LONG_LONG         ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiFloat_1           (JNIEnv *aEnv, jclass aClazz) {return MPI_FLOAT             ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiDouble_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_DOUBLE            ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiByte_1            (JNIEnv *aEnv, jclass aClazz) {return MPI_BYTE              ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiSignedChar_1      (JNIEnv *aEnv, jclass aClazz) {return MPI_SIGNED_CHAR       ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUnsignedLongLong_1(JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_LONG_LONG;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiInt8T_1           (JNIEnv *aEnv, jclass aClazz) {return MPI_INT8_T            ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiInt16T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_INT16_T           ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiInt32T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_INT32_T           ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiInt64T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_INT64_T           ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUint8T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT8_T           ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUint16T_1         (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT16_T          ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUint32T_1         (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT32_T          ;}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_getMpiUint64T_1         (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT64_T          ;}

JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiThreadSingle_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_SINGLE    ;}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiThreadFunneled_1  (JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_FUNNELED  ;}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiThreadSerialized_1(JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_SERIALIZED;}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiThreadMultiple_1  (JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_MULTIPLE  ;}

JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiProcNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_PROC_NULL ;}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiAnySource_1(JNIEnv *aEnv, jclass aClazz) {return MPI_ANY_SOURCE;}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiRoot_1     (JNIEnv *aEnv, jclass aClazz) {return MPI_ROOT      ;}

JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiAnyTag_1(JNIEnv *aEnv, jclass aClazz) {return MPI_ANY_TAG;}

JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_getMpiUndefined_1(JNIEnv *aEnv, jclass aClazz) {return MPI_UNDEFINED;}



JNIEXPORT jstring JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Get_1library_1version(JNIEnv *aEnv, jclass aClazz) {
    char rVersionStr[MPI_MAX_LIBRARY_VERSION_STRING];
    int rLen;
    int tExitCode = MPI_Get_library_version(rVersionStr, &rLen);
    exceptionCheckMPI(aEnv, tExitCode);
#ifdef __cplusplus
    return aEnv->NewStringUTF((const char*)rVersionStr);
#else
    return (*aEnv)->NewStringUTF(aEnv, (const char*)rVersionStr);
#endif
}


JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Init(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs) {
    int tLen;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
    int tExitCode = MPI_Init(&tLen, &sArgs);
    exceptionCheckMPI(aEnv, tExitCode);
    freeStrBuf(sArgs, tLen);
    
    tExitCode = MPI_Comm_set_errhandler(MPI_COMM_WORLD, MPI_ERRORS_RETURN);
    exceptionCheckMPI(aEnv, tExitCode);
}
JNIEXPORT jboolean JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Initialized(JNIEnv *aEnv, jclass aClazz) {
    int tFlag;
    int tExitCode = MPI_Initialized(&tFlag);
    exceptionCheckMPI(aEnv, tExitCode);
    return tFlag ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Comm_1rank(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tRank;
    int tExitCode = MPI_Comm_rank(tComm, &tRank);
    exceptionCheckMPI(aEnv, tExitCode);
    return tRank;
}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Comm_1size(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize;
    int tExitCode = MPI_Comm_size(tComm, &tSize);
    exceptionCheckMPI(aEnv, tExitCode);
    return tSize;
}

JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Finalize(JNIEnv *aEnv, jclass aClazz) {
    int tExitCode = MPI_Finalize();
    exceptionCheckMPI(aEnv, tExitCode);
}
JNIEXPORT jboolean JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Finalized(JNIEnv *aEnv, jclass aClazz) {
    int tFlag;
    int tExitCode = MPI_Finalized(&tFlag);
    exceptionCheckMPI(aEnv, tExitCode);
    return tFlag ? JNI_TRUE : JNI_FALSE;
}



JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Allgather0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jint aSendJType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRecvJType, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    jsize tTotSize = (jsize)(aRecvCount*tSize);
    void *rRecvBuf = allocBuf(aRecvJType, tTotSize);
    if (aInPlace) {
        jsize tStart = aRecvCount*tRank;
        parseJArray2BufV(aEnv, rRecvArray, tStart, aRecvJType, rRecvBuf, tStart, aRecvCount);
        tExitCode = MPI_Allgather(MPI_IN_PLACE, 0, tSendType, rRecvBuf, aRecvCount, tRecvType, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
    } else {
        void *tSendBuf = allocBuf(aSendJType, aSendCount);
        parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, aSendCount);
        tExitCode = MPI_Allgather(tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
        freeBuf(tSendBuf);
    }
    parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, tTotSize);
    freeBuf(rRecvBuf);
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Allgatherv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jint aSendJType, jobject rRecvArray, jintArray aRecvCounts, jintArray aDispls, jlong aRecvType, jint aRecvJType, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    int *tRecvCounts = MALLOCN(int, tSize); parsejint2int(aEnv, aRecvCounts, tRecvCounts, tSize);
    int *tDispls     = MALLOCN(int, tSize); parsejint2int(aEnv, aDispls    , tDispls    , tSize);
    jsize tTotSize = 0;
    for (int i = 0; i < tSize; ++i) tTotSize += (jsize)tRecvCounts[i];
    void *rRecvBuf = allocBuf(aRecvJType, tTotSize);
    if (aInPlace) {
        jsize tStart = tDispls[tRank];
        parseJArray2BufV(aEnv, rRecvArray, tStart, aRecvJType, rRecvBuf, tStart, tRecvCounts[tRank]);
        tExitCode = MPI_Allgatherv(MPI_IN_PLACE, 0, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
    } else {
        void *tSendBuf = allocBuf(aSendJType, aSendCount);
        parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, aSendCount);
        tExitCode = MPI_Allgatherv(tSendBuf, aSendCount, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
        freeBuf(tSendBuf);
    }
    parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, tTotSize);
    freeBuf(rRecvBuf);
    FREE(tRecvCounts);
    FREE(tDispls    );
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Allreduce0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jobject rRecvArray, jint aCount, jlong aDataType, jint aJDataType, jlong aOp, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Op tOp = (MPI_Op)(intptr_t)aOp;
    void *rRecvBuf = allocBuf(aJDataType, aCount);
    if (aInPlace) {
        parseJArray2Buf(aEnv, rRecvArray, aJDataType, rRecvBuf, aCount);
        int tExitCode = MPI_Allreduce(MPI_IN_PLACE, rRecvBuf, aCount, tDataType, tOp, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
    } else {
        void *tSendBuf = allocBuf(aJDataType, aCount);
        parseJArray2Buf(aEnv, aSendArray, aJDataType, tSendBuf, aCount);
        int tExitCode = MPI_Allreduce(tSendBuf, rRecvBuf, aCount, tDataType, tOp, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
        freeBuf(tSendBuf);
    }
    parseBuf2JArray(aEnv, rRecvArray, aJDataType, rRecvBuf, aCount);
    freeBuf(rRecvBuf);
}
JNIEXPORT jbyte    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceB(JNIEnv *aEnv, jclass aClazz, jbyte    aB, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aB, 1, MPI_JBYTE   , (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aB;}
JNIEXPORT jdouble  JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceD(JNIEnv *aEnv, jclass aClazz, jdouble  aD, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aD, 1, MPI_JDOUBLE , (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aD;}
JNIEXPORT jboolean JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceZ(JNIEnv *aEnv, jclass aClazz, jboolean aZ, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aZ, 1, MPI_JBOOLEAN, (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aZ;}
JNIEXPORT jchar    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceC(JNIEnv *aEnv, jclass aClazz, jchar    aC, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aC, 1, MPI_JCHAR   , (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aC;}
JNIEXPORT jshort   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceS(JNIEnv *aEnv, jclass aClazz, jshort   aS, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aS, 1, MPI_JSHORT  , (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aS;}
JNIEXPORT jint     JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceI(JNIEnv *aEnv, jclass aClazz, jint     aI, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aI, 1, MPI_JINT    , (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aI;}
JNIEXPORT jlong    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceL(JNIEnv *aEnv, jclass aClazz, jlong    aL, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aL, 1, MPI_JLONG   , (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aL;}
JNIEXPORT jfloat   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1AllreduceF(JNIEnv *aEnv, jclass aClazz, jfloat   aF, jlong aOp, jlong aComm) {int tExitCode = MPI_Allreduce(MPI_IN_PLACE, &aF, 1, MPI_JFLOAT  , (MPI_Op)(intptr_t)aOp, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aF;}

JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Barrier(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tExitCode = MPI_Barrier(tComm);
    exceptionCheckMPI(aEnv, tExitCode);
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Bcast0(JNIEnv *aEnv, jclass aClazz, jobject rArray, jint aCount, jlong aDataType, jint aJDataType, jint aRoot, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tRank;
    int tExitCode = MPI_Comm_rank(tComm, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    void *rBuf = allocBuf(aJDataType, aCount);
    if (tRank == aRoot) {
        parseJArray2Buf(aEnv, rArray, aJDataType, rBuf, aCount);
        tExitCode = MPI_Bcast(rBuf, aCount, tDataType, aRoot, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
    } else {
        tExitCode = MPI_Bcast(rBuf, aCount, tDataType, aRoot, tComm);
        exceptionCheckMPI(aEnv, tExitCode);
        parseBuf2JArray(aEnv, rArray, aJDataType, rBuf, aCount);
    }
    freeBuf(rBuf);
}
JNIEXPORT jbyte    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastB(JNIEnv *aEnv, jclass aClazz, jbyte    aB, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aB, 1, MPI_JBYTE   , aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aB;}
JNIEXPORT jdouble  JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastD(JNIEnv *aEnv, jclass aClazz, jdouble  aD, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aD, 1, MPI_JDOUBLE , aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aD;}
JNIEXPORT jboolean JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastZ(JNIEnv *aEnv, jclass aClazz, jboolean aZ, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aZ, 1, MPI_JBOOLEAN, aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aZ;}
JNIEXPORT jchar    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastC(JNIEnv *aEnv, jclass aClazz, jchar    aC, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aC, 1, MPI_JCHAR   , aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aC;}
JNIEXPORT jshort   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastS(JNIEnv *aEnv, jclass aClazz, jshort   aS, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aS, 1, MPI_JSHORT  , aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aS;}
JNIEXPORT jint     JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastI(JNIEnv *aEnv, jclass aClazz, jint     aI, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aI, 1, MPI_JINT    , aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aI;}
JNIEXPORT jlong    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastL(JNIEnv *aEnv, jclass aClazz, jlong    aL, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aL, 1, MPI_JLONG   , aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aL;}
JNIEXPORT jfloat   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1BcastF(JNIEnv *aEnv, jclass aClazz, jfloat   aF, jint aRoot, jlong aComm) {int tExitCode = MPI_Bcast(&aF, 1, MPI_JFLOAT  , aRoot, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode); return aF;}

JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Gather0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jint aSendJType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRecvJType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        jsize tTotSize = (jsize)(aRecvCount*tSize);
        void *rRecvBuf = allocBuf(aRecvJType, tTotSize);
        if (aInPlace) {
            jsize tStart = aRecvCount*tRank;
            parseJArray2BufV(aEnv, rRecvArray, tStart, aRecvJType, rRecvBuf, tStart, aRecvCount);
            tExitCode = MPI_Gather(MPI_IN_PLACE, 0, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
        } else {
            void *tSendBuf = allocBuf(aSendJType, aSendCount);
            parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, aSendCount);
            tExitCode = MPI_Gather(tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
        parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, tTotSize);
        freeBuf(rRecvBuf);
    } else {
        if (aInPlace) {
            throwExceptionMPI(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Gather", -1);
            return;
        } else {
            void *tSendBuf = allocBuf(aSendJType, aSendCount);
            parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, aSendCount);
            tExitCode = MPI_Gather(tSendBuf, aSendCount, tSendType, NULL, 0, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
    }
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Gatherv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jint aSendJType, jobject rRecvArray, jintArray aRecvCounts, jintArray aDispls, jlong aRecvType, jint aRecvJType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        int *tRecvCounts = MALLOCN(int, tSize); parsejint2int(aEnv, aRecvCounts, tRecvCounts, tSize);
        int *tDispls     = MALLOCN(int, tSize); parsejint2int(aEnv, aDispls    , tDispls    , tSize);
        jsize tTotSize = 0;
        for (int i = 0; i < tSize; ++i) tTotSize += (jsize)tRecvCounts[i];
        void *rRecvBuf = allocBuf(aRecvJType, tTotSize);
        if (aInPlace) {
            jsize tStart = tDispls[tRank];
            parseJArray2BufV(aEnv, rRecvArray, tStart, aRecvJType, rRecvBuf, tStart, tRecvCounts[tRank]);
            tExitCode = MPI_Gatherv(MPI_IN_PLACE, 0, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
        } else {
            void *tSendBuf = allocBuf(aSendJType, aSendCount);
            parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, aSendCount);
            tExitCode = MPI_Gatherv(tSendBuf, aSendCount, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
        parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, tTotSize);
        freeBuf(rRecvBuf);
        FREE(tRecvCounts);
        FREE(tDispls    );
    } else {
        if (aInPlace) {
            throwExceptionMPI(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Gatherv", -1);
            return;
        } else {
            void *tSendBuf = allocBuf(aSendJType, aSendCount);
            parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, aSendCount);
            tExitCode = MPI_Gatherv(tSendBuf, aSendCount, tSendType, NULL, NULL, NULL, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
    }
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Reduce0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jobject rRecvArray, jint aCount, jlong aDataType, jint aJDataType, jlong aOp, jint aRoot, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Op tOp = (MPI_Op)(intptr_t)aOp;
    int tRank;
    int tExitCode = MPI_Comm_rank(tComm, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        void *rRecvBuf = allocBuf(aJDataType, aCount);
        if (aInPlace) {
            parseJArray2Buf(aEnv, rRecvArray, aJDataType, rRecvBuf, aCount);
            tExitCode = MPI_Reduce(MPI_IN_PLACE, rRecvBuf, aCount, tDataType, tOp, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
        } else {
            void *tSendBuf = allocBuf(aJDataType, aCount);
            parseJArray2Buf(aEnv, aSendArray, aJDataType, tSendBuf, aCount);
            tExitCode = MPI_Reduce(tSendBuf, rRecvBuf, aCount, tDataType, tOp, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
        parseBuf2JArray(aEnv, rRecvArray, aJDataType, rRecvBuf, aCount);
        freeBuf(rRecvBuf);
    } else {
        if (aInPlace) {
            throwExceptionMPI(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Reduce", -1);
            return;
        } else {
            void *tSendBuf = allocBuf(aJDataType, aCount);
            parseJArray2Buf(aEnv, aSendArray, aJDataType, tSendBuf, aCount);
            tExitCode = MPI_Reduce(tSendBuf, NULL, aCount, tDataType, tOp, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
    }
}
JNIEXPORT jbyte    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceB(JNIEnv *aEnv, jclass aClazz, jbyte    aB, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aB;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aB, 1, MPI_JBYTE   , tOp, aRoot, tComm);} else {MPI_Reduce(&aB, NULL, 1, MPI_JBYTE   , tOp, aRoot, tComm);} return aB;}
JNIEXPORT jdouble  JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceD(JNIEnv *aEnv, jclass aClazz, jdouble  aD, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aD;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aD, 1, MPI_JDOUBLE , tOp, aRoot, tComm);} else {MPI_Reduce(&aD, NULL, 1, MPI_JDOUBLE , tOp, aRoot, tComm);} return aD;}
JNIEXPORT jboolean JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceZ(JNIEnv *aEnv, jclass aClazz, jboolean aZ, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aZ;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aZ, 1, MPI_JBOOLEAN, tOp, aRoot, tComm);} else {MPI_Reduce(&aZ, NULL, 1, MPI_JBOOLEAN, tOp, aRoot, tComm);} return aZ;}
JNIEXPORT jchar    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceC(JNIEnv *aEnv, jclass aClazz, jchar    aC, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aC;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aC, 1, MPI_JCHAR   , tOp, aRoot, tComm);} else {MPI_Reduce(&aC, NULL, 1, MPI_JCHAR   , tOp, aRoot, tComm);} return aC;}
JNIEXPORT jshort   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceS(JNIEnv *aEnv, jclass aClazz, jshort   aS, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aS;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aS, 1, MPI_JSHORT  , tOp, aRoot, tComm);} else {MPI_Reduce(&aS, NULL, 1, MPI_JSHORT  , tOp, aRoot, tComm);} return aS;}
JNIEXPORT jint     JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceI(JNIEnv *aEnv, jclass aClazz, jint     aI, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aI;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aI, 1, MPI_JINT    , tOp, aRoot, tComm);} else {MPI_Reduce(&aI, NULL, 1, MPI_JINT    , tOp, aRoot, tComm);} return aI;}
JNIEXPORT jlong    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceL(JNIEnv *aEnv, jclass aClazz, jlong    aL, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aL;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aL, 1, MPI_JLONG   , tOp, aRoot, tComm);} else {MPI_Reduce(&aL, NULL, 1, MPI_JLONG   , tOp, aRoot, tComm);} return aL;}
JNIEXPORT jfloat   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1ReduceF(JNIEnv *aEnv, jclass aClazz, jfloat   aF, jlong aOp, jint aRoot, jlong aComm) {MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm; MPI_Op tOp = (MPI_Op)(intptr_t)aOp; int tRank; int tExitCode = MPI_Comm_rank(tComm, &tRank); if (exceptionCheckMPI(aEnv, tExitCode)) {return aF;} if (tRank == aRoot) {MPI_Reduce(MPI_IN_PLACE, &aF, 1, MPI_JFLOAT  , tOp, aRoot, tComm);} else {MPI_Reduce(&aF, NULL, 1, MPI_JFLOAT  , tOp, aRoot, tComm);} return aF;}

JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Scatter0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jint aSendJType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRecvJType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        jsize tTotSize = (jsize)(aSendCount*tSize);
        void *tSendBuf = allocBuf(aSendJType, tTotSize);
        parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, tTotSize);
        if (aInPlace) {
            tExitCode = MPI_Scatter(tSendBuf, aSendCount, tSendType, MPI_IN_PLACE, 0, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
        } else {
            void *rRecvBuf = allocBuf(aRecvJType, aRecvCount);
            tExitCode = MPI_Scatter(tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, aRecvCount);
            freeBuf(rRecvBuf);
        }
        freeBuf(tSendBuf);
    } else {
        if (aInPlace) {
            throwExceptionMPI(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Scatter", -1);
            return;
        } else {
            void *rRecvBuf = allocBuf(aRecvJType, aRecvCount);
            tExitCode = MPI_Scatter(NULL, 0, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, aRecvCount);
            freeBuf(rRecvBuf);
        }
    }
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Scatterv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jintArray aSendCounts, jintArray aDispls, jlong aSendType, jint aSendJType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRecvJType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheckMPI(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        int *tSendCounts = MALLOCN(int, tSize); parsejint2int(aEnv, aSendCounts, tSendCounts, tSize);
        int *tDispls     = MALLOCN(int, tSize); parsejint2int(aEnv, aDispls    , tDispls    , tSize);
        jsize tTotSize = 0;
        for (int i = 0; i < tSize; ++i) tTotSize += (jsize)tSendCounts[i];
        void *tSendBuf = allocBuf(aSendJType, tTotSize);
        parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, tTotSize);
        if (aInPlace) {
            tExitCode = MPI_Scatterv(tSendBuf, tSendCounts, tDispls, tSendType, MPI_IN_PLACE, 0, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
        } else {
            void *rRecvBuf = allocBuf(aRecvJType, aRecvCount);
            tExitCode = MPI_Scatterv(tSendBuf, tSendCounts, tDispls, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, aRecvCount);
            freeBuf(rRecvBuf);
        }
        freeBuf(tSendBuf);
        FREE(tSendCounts);
        FREE(tDispls    );
    } else {
        if (aInPlace) {
            throwExceptionMPI(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Scatterv", -1);
            return;
        } else {
            void *rRecvBuf = allocBuf(aRecvJType, aRecvCount);
            tExitCode = MPI_Scatterv(NULL, NULL, NULL, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheckMPI(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, aRecvCount);
            freeBuf(rRecvBuf);
        }
    }
}


JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Comm_1create(JNIEnv *aEnv, jclass aClazz, jlong aComm, jlong aGroup) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Group tGroup = (MPI_Group)(intptr_t)aGroup;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_create(tComm, tGroup, &nComm);
    exceptionCheckMPI(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Comm_1dup(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_dup(tComm, &nComm);
    exceptionCheckMPI(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Comm_1free(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tExitCode = MPI_Comm_free(&tComm);
    exceptionCheckMPI(aEnv, tExitCode);
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Comm_1split(JNIEnv *aEnv, jclass aClazz, jlong aComm, jint aColor, jint aKey) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_split(tComm, aColor, aKey, &nComm);
    exceptionCheckMPI(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Comm_1group(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Group nGroup;
    int tExitCode = MPI_Comm_group(tComm, &nGroup);
    exceptionCheckMPI(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1difference(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group)(intptr_t)aGroup1;
    MPI_Group tGroup2 = (MPI_Group)(intptr_t)aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_difference(tGroup1, tGroup2, &nGroup);
    exceptionCheckMPI(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1excl(JNIEnv *aEnv, jclass aClazz, jlong aGroup, jint aN, jintArray aRanks) {
    MPI_Group tGroup = (MPI_Group)(intptr_t)aGroup;
    int *tRanks = (aRanks==NULL || aN<=0) ? NULL : MALLOCN(int, aN); parsejint2int(aEnv, aRanks, tRanks, aN);
    MPI_Group nGroup;
    int tExitCode = MPI_Group_excl(tGroup, aN, tRanks, &nGroup);
    exceptionCheckMPI(aEnv, tExitCode);
    if (tRanks != NULL) FREE(tRanks);
    return nGroup;
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1free(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm)(intptr_t)aGroup;
    int tExitCode = MPI_Group_free(&tGroup);
    exceptionCheckMPI(aEnv, tExitCode);
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1incl(JNIEnv *aEnv, jclass aClazz, jlong aGroup, jint aN, jintArray aRanks) {
    MPI_Group tGroup = (MPI_Group)(intptr_t)aGroup;
    int *tRanks = (aRanks==NULL || aN<=0) ? NULL : MALLOCN(int, aN); parsejint2int(aEnv, aRanks, tRanks, aN);
    MPI_Group nGroup;
    int tExitCode = MPI_Group_incl(tGroup, aN, tRanks, &nGroup);
    exceptionCheckMPI(aEnv, tExitCode);
    if (tRanks != NULL) FREE(tRanks);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1intersection(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group)(intptr_t)aGroup1;
    MPI_Group tGroup2 = (MPI_Group)(intptr_t)aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_intersection(tGroup1, tGroup2, &nGroup);
    exceptionCheckMPI(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1rank(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm)(intptr_t)aGroup;
    int tRank;
    int tExitCode = MPI_Group_rank(tGroup, &tRank);
    exceptionCheckMPI(aEnv, tExitCode);
    return tRank;
}
JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1size(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm)(intptr_t)aGroup;
    int tSize;
    int tExitCode = MPI_Group_size(tGroup, &tSize);
    exceptionCheckMPI(aEnv, tExitCode);
    return tSize;
}
JNIEXPORT jlong JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Group_1union(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group)(intptr_t)aGroup1;
    MPI_Group tGroup2 = (MPI_Group)(intptr_t)aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_union(tGroup1, tGroup2, &nGroup);
    exceptionCheckMPI(aEnv, tExitCode);
    return nGroup;
}


JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Send0(JNIEnv *aEnv, jclass aClazz, jobject aArray, jint aCount, jlong aDataType, jint aJDataType, jint aDest, jint aTag, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    void *tBuf = allocBuf(aJDataType, aCount);
    parseJArray2Buf(aEnv, aArray, aJDataType, tBuf, aCount);
    int tExitCode = MPI_Send(tBuf, aCount, tDataType, aDest, aTag, tComm);
    exceptionCheckMPI(aEnv, tExitCode);
    freeBuf(tBuf);
}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendB(JNIEnv *aEnv, jclass aClazz, jbyte    aB, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aB, 1, MPI_JBYTE   , aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendD(JNIEnv *aEnv, jclass aClazz, jdouble  aD, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aD, 1, MPI_JDOUBLE , aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendZ(JNIEnv *aEnv, jclass aClazz, jboolean aZ, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aZ, 1, MPI_JBOOLEAN, aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendC(JNIEnv *aEnv, jclass aClazz, jchar    aC, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aC, 1, MPI_JCHAR   , aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendS(JNIEnv *aEnv, jclass aClazz, jshort   aS, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aS, 1, MPI_JSHORT  , aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendI(JNIEnv *aEnv, jclass aClazz, jint     aI, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aI, 1, MPI_JINT    , aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendL(JNIEnv *aEnv, jclass aClazz, jlong    aL, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aL, 1, MPI_JLONG   , aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}
JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1SendF(JNIEnv *aEnv, jclass aClazz, jfloat   aF, jint aDest, jint aTag, jlong aComm) {int tExitCode = MPI_Send(&aF, 1, MPI_JFLOAT  , aDest, aTag, (MPI_Comm)(intptr_t)aComm); exceptionCheckMPI(aEnv, tExitCode);}

JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Recv0(JNIEnv *aEnv, jclass aClazz, jobject rArray, jint aCount, jlong aDataType, jint aJDataType, jint aSource, jint aTag, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    void *rBuf = allocBuf(aJDataType, aCount);
    int tExitCode = MPI_Recv(rBuf, aCount, tDataType, aSource, aTag, tComm, MPI_STATUS_IGNORE); // no return Status, because its field name is unstable
    exceptionCheckMPI(aEnv, tExitCode);
    parseBuf2JArray(aEnv, rArray, aJDataType, rBuf, aCount);
    freeBuf(rBuf);
}
JNIEXPORT jbyte    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvB(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jbyte    tB; int tExitCode = MPI_Recv(&tB, 1, MPI_JBYTE   , aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tB;}
JNIEXPORT jdouble  JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvD(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jdouble  tD; int tExitCode = MPI_Recv(&tD, 1, MPI_JDOUBLE , aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tD;}
JNIEXPORT jboolean JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvZ(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jboolean tZ; int tExitCode = MPI_Recv(&tZ, 1, MPI_JBOOLEAN, aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tZ;}
JNIEXPORT jchar    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvC(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jchar    tC; int tExitCode = MPI_Recv(&tC, 1, MPI_JCHAR   , aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tC;}
JNIEXPORT jshort   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvS(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jshort   tS; int tExitCode = MPI_Recv(&tS, 1, MPI_JSHORT  , aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tS;}
JNIEXPORT jint     JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvI(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jint     tI; int tExitCode = MPI_Recv(&tI, 1, MPI_JINT    , aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tI;}
JNIEXPORT jlong    JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvL(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jlong    tL; int tExitCode = MPI_Recv(&tL, 1, MPI_JLONG   , aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tL;}
JNIEXPORT jfloat   JNICALL Java_jse_parallel_MPI_00024Native_MPI_1RecvF(JNIEnv *aEnv, jclass aClazz, jint aSource, jint aTag, jlong aComm) {jfloat   tF; int tExitCode = MPI_Recv(&tF, 1, MPI_JFLOAT  , aSource, aTag, (MPI_Comm)(intptr_t)aComm, MPI_STATUS_IGNORE); exceptionCheckMPI(aEnv, tExitCode); return tF;}

JNIEXPORT void JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Sendrecv0(JNIEnv *aEnv, jclass aClazz, jobject aSendArray, jint aSendCount, jlong aSendType, jint aSendJType, jint aDest, jint aSendTag, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRecvJType, jint aSource, jint aRecvTag, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    void *tSendBuf = allocBuf(aSendJType, aSendCount);
    parseJArray2Buf(aEnv, aSendArray, aSendJType, tSendBuf, aSendCount);
    void *rRecvBuf = allocBuf(aRecvJType, aRecvCount);
    int tExitCode = MPI_Sendrecv(tSendBuf, aSendCount, tSendType, aDest, aSendTag, rRecvBuf, aRecvCount, tRecvType, aSource, aRecvTag, tComm, MPI_STATUS_IGNORE); // no return Status, because its field name is unstable
    exceptionCheckMPI(aEnv, tExitCode);
    parseBuf2JArray(aEnv, rRecvArray, aRecvJType, rRecvBuf, aRecvCount);
    freeBuf(tSendBuf);
    freeBuf(rRecvBuf);
}


JNIEXPORT jint JNICALL Java_jse_parallel_MPI_00024Native_MPI_1Init_1thread(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs, jint aRequired) {
    int tLen, tProvided;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
    int tExitCode = MPI_Init_thread(&tLen, &sArgs, aRequired, &tProvided);
    exceptionCheckMPI(aEnv, tExitCode);
    freeStrBuf(sArgs, tLen);
    
    tExitCode = MPI_Comm_set_errhandler(MPI_COMM_WORLD, MPI_ERRORS_RETURN);
    exceptionCheckMPI(aEnv, tExitCode);
    
    return tProvided;
}

#ifdef __cplusplus
}
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
