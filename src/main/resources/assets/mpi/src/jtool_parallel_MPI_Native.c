#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#include "mpi.h"
#include "jtool_parallel_MPI_Native.h"


/** utils */
void *getJArray(JNIEnv *aEnv, jobject aJArray, MPI_Datatype aDataType) {
    if (aJArray==NULL) return NULL;
    switch (aDataType) {
    case MPI_SIGNED_CHAR:    {return (*aEnv)->GetByteArrayElements   (aEnv, aJArray, NULL);}
    case MPI_DOUBLE:         {return (*aEnv)->GetDoubleArrayElements (aEnv, aJArray, NULL);}
    case MPI_UNSIGNED_CHAR:  {return (*aEnv)->GetBooleanArrayElements(aEnv, aJArray, NULL);}
    case MPI_UNSIGNED_SHORT: {return (*aEnv)->GetCharArrayElements   (aEnv, aJArray, NULL);}
    case MPI_SHORT:          {return (*aEnv)->GetShortArrayElements  (aEnv, aJArray, NULL);}
    case MPI_INT32_T:        {return (*aEnv)->GetIntArrayElements    (aEnv, aJArray, NULL);}
    case MPI_INT64_T:        {return (*aEnv)->GetLongArrayElements   (aEnv, aJArray, NULL);}
    case MPI_FLOAT:          {return (*aEnv)->GetFloatArrayElements  (aEnv, aJArray, NULL);}
    default:                 {return NULL;}
    }
}
void releaseJArray(JNIEnv *aEnv, jobject aJArray, MPI_Datatype aDataType, void *aBuf, jint aMode) {
    if (aJArray==NULL) return;
    switch (aDataType) {
    case MPI_SIGNED_CHAR:    {(*aEnv)->ReleaseByteArrayElements   (aEnv, aJArray, aBuf, aMode); return;}
    case MPI_DOUBLE:         {(*aEnv)->ReleaseDoubleArrayElements (aEnv, aJArray, aBuf, aMode); return;}
    case MPI_UNSIGNED_CHAR:  {(*aEnv)->ReleaseBooleanArrayElements(aEnv, aJArray, aBuf, aMode); return;}
    case MPI_UNSIGNED_SHORT: {(*aEnv)->ReleaseCharArrayElements   (aEnv, aJArray, aBuf, aMode); return;}
    case MPI_SHORT:          {(*aEnv)->ReleaseShortArrayElements  (aEnv, aJArray, aBuf, aMode); return;}
    case MPI_INT32_T:        {(*aEnv)->ReleaseIntArrayElements    (aEnv, aJArray, aBuf, aMode); return;}
    case MPI_INT64_T:        {(*aEnv)->ReleaseLongArrayElements   (aEnv, aJArray, aBuf, aMode); return;}
    case MPI_FLOAT:          {(*aEnv)->ReleaseFloatArrayElements  (aEnv, aJArray, aBuf, aMode); return;}
    default:                 {return;}
    }
}


#define GEN_PARSE_JANY_TO_ANY(R, T, TF)                                             \
R *parseJ##T##2##R (JNIEnv *aEnv, j##T##Array aJArray) {                            \
    if (aJArray==NULL) return NULL;                                                 \
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aJArray);                            \
    R *rOutBuf = (R *) malloc(tLen * sizeof(R));                                    \
                                                                                    \
    j##T *tBuf = (*aEnv)->Get##TF##ArrayElements(aEnv, aJArray, NULL);              \
    for (jsize i = 0; i < tLen; ++i) rOutBuf[i] = (R)tBuf[i];                       \
    (*aEnv)->Release##TF##ArrayElements(aEnv, aJArray, tBuf, JNI_ABORT);            \
    return rOutBuf;                                                                 \
}

GEN_PARSE_JANY_TO_ANY(int, int, Int)



char **parseArgs(JNIEnv *aEnv, jobjectArray aArgs, int *rLen) {
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aArgs);
    char **sArgs = (char**)calloc(tLen+1, sizeof(char*));
    
    for (jsize i = 0; i < tLen; i++) {
        jstring jc = (jstring)(*aEnv)->GetObjectArrayElement(aEnv, aArgs, i);
        const char *s = (*aEnv)->GetStringUTFChars(aEnv, jc, NULL);
#ifdef WIN32
        sArgs[i] = _strdup(s);
#elif _WIN64
        sArgs[i] = _strdup(s);
#elif _WIN32
        sArgs[i] = _strdup(s);
#elif __unix__
        sArgs[i] = strdup(s);
#elif __linux__
        sArgs[i] = strdup(s);
#endif
        (*aEnv)->ReleaseStringUTFChars(aEnv, jc, s);
        (*aEnv)->DeleteLocalRef(aEnv, jc);
    }
    
    *rLen = tLen;
    return sArgs;
}
void freeArgs(char **aArgs, int aLen) {
    for(int i = 0; i < aLen; i++) free(aArgs[i]);
    free(aArgs);
}

void exceptionCheck(JNIEnv *aEnv, int aExitCode) {
    if (aExitCode == MPI_SUCCESS) return;
    
    // find class runtime due to asm
    jclass tMPIErrorClazz = (*aEnv)->FindClass(aEnv, "jtool/parallel/MPI$Error");
    if (tMPIErrorClazz == NULL) {
        fprintf(stderr, "Couldn't find jtool/parallel/MPI$Error\n");
        return;
    }
    jmethodID tMPIErrorInit = (*aEnv)->GetMethodID(aEnv, tMPIErrorClazz, "<init>", "(ILjava/lang/String;)V");
    if (tMPIErrorInit == NULL) {
        fprintf(stderr, "Couldn't find jtool/parallel/MPI$Error.<init>(ILjava/lang/String;)V\n");
        return;
    }
    
    char rErrStr[MPI_MAX_ERROR_STRING];
    int rLen;
    MPI_Error_string(aExitCode, rErrStr, &rLen);
    
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, (const char*)rErrStr);
    jobject tMPIError = (*aEnv)->NewObject(aEnv, tMPIErrorClazz, tMPIErrorInit, aExitCode, tJErrStr);
    (*aEnv)->Throw(aEnv, tMPIError);
    (*aEnv)->DeleteLocalRef(aEnv, tMPIError);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tMPIErrorClazz);
}


JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiGroupNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_GROUP_NULL ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiGroupEmpty_1(JNIEnv *aEnv, jclass aClazz) {return MPI_GROUP_EMPTY;}

JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiCommNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_COMM_NULL ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiCommWorld_1(JNIEnv *aEnv, jclass aClazz) {return MPI_COMM_WORLD;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiCommSelf_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_COMM_SELF ;}

JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiOpNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_OP_NULL;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiMax_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_MAX    ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiMin_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_MIN    ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiSum_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_SUM    ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiProd_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_PROD   ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiLand_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_LAND   ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiBand_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_BAND   ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiLor_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_LOR    ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiBor_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_BOR    ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiLxor_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_LXOR   ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiBxor_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_BXOR   ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiMinloc_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_MINLOC ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiMaxloc_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_MAXLOC ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiReplace_1(JNIEnv *aEnv, jclass aClazz) {return MPI_REPLACE;}

JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiDatatypeNull_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_DATATYPE_NULL     ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiChar_1            (JNIEnv *aEnv, jclass aClazz) {return MPI_CHAR              ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUnsignedChar_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_CHAR     ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiShort_1           (JNIEnv *aEnv, jclass aClazz) {return MPI_SHORT             ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUnsignedShort_1   (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_SHORT    ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiInt_1             (JNIEnv *aEnv, jclass aClazz) {return MPI_INT               ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUnsigned_1        (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED          ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiLong_1            (JNIEnv *aEnv, jclass aClazz) {return MPI_LONG              ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUnsignedLong_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_LONG     ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiLongLong_1        (JNIEnv *aEnv, jclass aClazz) {return MPI_LONG_LONG         ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiFloat_1           (JNIEnv *aEnv, jclass aClazz) {return MPI_FLOAT             ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiDouble_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_DOUBLE            ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiByte_1            (JNIEnv *aEnv, jclass aClazz) {return MPI_BYTE              ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiSignedChar_1      (JNIEnv *aEnv, jclass aClazz) {return MPI_SIGNED_CHAR       ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUnsignedLongLong_1(JNIEnv *aEnv, jclass aClazz) {return MPI_UNSIGNED_LONG_LONG;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiInt8T_1           (JNIEnv *aEnv, jclass aClazz) {return MPI_INT8_T            ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiInt16T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_INT16_T           ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiInt32T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_INT32_T           ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiInt64T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_INT64_T           ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUint8T_1          (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT8_T           ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUint16T_1         (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT16_T          ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUint32T_1         (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT32_T          ;}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUint64T_1         (JNIEnv *aEnv, jclass aClazz) {return MPI_UINT64_T          ;}

JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiThreadSingle_1    (JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_SINGLE    ;}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiThreadFunneled_1  (JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_FUNNELED  ;}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiThreadSerialized_1(JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_SERIALIZED;}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiThreadMultiple_1  (JNIEnv *aEnv, jclass aClazz) {return MPI_THREAD_MULTIPLE  ;}

JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiProcNull_1 (JNIEnv *aEnv, jclass aClazz) {return MPI_PROC_NULL ;}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiAnySource_1(JNIEnv *aEnv, jclass aClazz) {return MPI_ANY_SOURCE;}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiRoot_1     (JNIEnv *aEnv, jclass aClazz) {return MPI_ROOT      ;}

JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiAnyTag_1(JNIEnv *aEnv, jclass aClazz) {return MPI_ANY_TAG;}

JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_getMpiUndefined_1(JNIEnv *aEnv, jclass aClazz) {return MPI_UNDEFINED;}



JNIEXPORT jstring JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Get_1library_1version(JNIEnv *aEnv, jclass aClazz) {
    char rVersionStr[MPI_MAX_LIBRARY_VERSION_STRING];
    int rLen;
    int tExitCode = MPI_Get_library_version(rVersionStr, &rLen);
    exceptionCheck(aEnv, tExitCode);
    return (*aEnv)->NewStringUTF(aEnv, (const char*)rVersionStr);
}


JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Init(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs) {
    int tLen;
    char **sArgs = parseArgs(aEnv, aArgs, &tLen);
    int tExitCode = MPI_Init(&tLen, &sArgs);
    exceptionCheck(aEnv, tExitCode);
    freeArgs(sArgs, tLen);
    
    tExitCode = MPI_Comm_set_errhandler(MPI_COMM_WORLD, MPI_ERRORS_RETURN);
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT jboolean JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Initialized(JNIEnv *aEnv, jclass aClazz) {
    int tFlag;
    int tExitCode = MPI_Initialized(&tFlag);
    exceptionCheck(aEnv, tExitCode);
    return tFlag ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1rank(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    int tRank;
    int tExitCode = MPI_Comm_rank(tComm, &tRank);
    exceptionCheck(aEnv, tExitCode);
    return tRank;
}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1size(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    int tSize;
    int tExitCode = MPI_Comm_size(tComm, &tSize);
    exceptionCheck(aEnv, tExitCode);
    return tSize;
}

JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Finalize(JNIEnv *aEnv, jclass aClazz) {
    int tExitCode = MPI_Finalize();
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT jboolean JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Finalized(JNIEnv *aEnv, jclass aClazz) {
    int tFlag;
    int tExitCode = MPI_Finalized(&tFlag);
    exceptionCheck(aEnv, tExitCode);
    return tFlag ? JNI_TRUE : JNI_FALSE;
}



JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Allgather0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype) (intptr_t) aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype) (intptr_t) aRecvType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tSendBuf = getJArray(aEnv, aSendArray, tSendType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tRecvType);
    int tExitCode = MPI_Allgather(aInPlace ? MPI_IN_PLACE : tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tSendType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tRecvType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Allgatherv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jintArray aRecvCounts, jintArray aDispls, jlong aRecvType, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype) (intptr_t) aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype) (intptr_t) aRecvType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tSendBuf = getJArray(aEnv, aSendArray, tSendType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tRecvType);
    int *tRecvCounts = parseJint2int(aEnv, aRecvCounts);
    int *tDispls     = parseJint2int(aEnv, aDispls    );
    int tExitCode = MPI_Allgatherv(aInPlace ? MPI_IN_PLACE : tSendBuf, aSendCount, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tSendType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tRecvType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
    if (tRecvCounts != NULL) free(tRecvCounts);
    if (tDispls     != NULL) free(tDispls    );
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Allreduce0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jobject rRecvArray, jint aCount, jlong aDataType, jlong aOp, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype) (intptr_t) aDataType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    MPI_Op tOp = (MPI_Op) (intptr_t) aOp;
    void *tSendBuf = getJArray(aEnv, aSendArray, tDataType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tDataType);
    int tExitCode = MPI_Allreduce(aInPlace ? MPI_IN_PLACE : tSendBuf, rRecvBuf, aCount, tDataType, tOp, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tDataType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tDataType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Barrier(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    int tExitCode = MPI_Barrier(tComm);
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Bcast0(JNIEnv *aEnv, jclass aClazz, jobject rArray, jint aCount, jlong aDataType, jint aRoot, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype) (intptr_t) aDataType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *rBuf = getJArray(aEnv, rArray, tDataType);
    int tExitCode = MPI_Bcast(rBuf, aCount, tDataType, aRoot, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, rArray, tDataType, rBuf, 0); // write mode, Update the data on the Java heap. Free the space used by the copy.
}

JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Gather0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype) (intptr_t) aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype) (intptr_t) aRecvType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tSendBuf = getJArray(aEnv, aSendArray, tSendType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tRecvType);
    int tExitCode = MPI_Gather(aInPlace ? MPI_IN_PLACE : tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tSendType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tRecvType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Gatherv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jintArray aRecvCounts, jintArray aDispls, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype) (intptr_t) aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype) (intptr_t) aRecvType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tSendBuf = getJArray(aEnv, aSendArray, tSendType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tRecvType);
    int *tRecvCounts = parseJint2int(aEnv, aRecvCounts);
    int *tDispls     = parseJint2int(aEnv, aDispls    );
    int tExitCode = MPI_Gatherv(aInPlace ? MPI_IN_PLACE : tSendBuf, aSendCount, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, aRoot, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tSendType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tRecvType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
    if (tRecvCounts != NULL) free(tRecvCounts);
    if (tDispls     != NULL) free(tDispls    );
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Reduce0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jobject rRecvArray, jint aCount, jlong aDataType, jlong aOp, jint aRoot, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype) (intptr_t) aDataType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    MPI_Op tOp = (MPI_Op) (intptr_t) aOp;
    void *tSendBuf = getJArray(aEnv, aSendArray, tDataType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tDataType);
    int tExitCode = MPI_Reduce(aInPlace ? MPI_IN_PLACE : tSendBuf, rRecvBuf, aCount, tDataType, tOp, aRoot, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tDataType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tDataType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Scatter0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype) (intptr_t) aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype) (intptr_t) aRecvType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tSendBuf = getJArray(aEnv, aSendArray, tSendType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tRecvType);
    int tExitCode = MPI_Scatter(tSendBuf, aSendCount, tSendType, aInPlace ? MPI_IN_PLACE : rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tSendType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tRecvType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Scatterv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jintArray aSendCounts, jintArray aDispls, jlong aSendType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype) (intptr_t) aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype) (intptr_t) aRecvType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tSendBuf = getJArray(aEnv, aSendArray, tSendType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tRecvType);
    int *tSendCounts = parseJint2int(aEnv, aSendCounts);
    int *tDispls     = parseJint2int(aEnv, aDispls    );
    int tExitCode = MPI_Scatterv(tSendBuf, tSendCounts, tDispls, tSendType, aInPlace ? MPI_IN_PLACE : rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tSendType, tSendBuf, JNI_ABORT); // read  mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tRecvType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
    if (tSendCounts != NULL) free(tSendCounts);
    if (tDispls     != NULL) free(tDispls    );
}


JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1create(JNIEnv *aEnv, jclass aClazz, jlong aComm, jlong aGroup) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    MPI_Group tGroup = (MPI_Group) (intptr_t) aGroup;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_create(tComm, tGroup, &nComm);
    exceptionCheck(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1dup(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_dup(tComm, &nComm);
    exceptionCheck(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1free(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    int tExitCode = MPI_Comm_free(&tComm);
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1split(JNIEnv *aEnv, jclass aClazz, jlong aComm, jint aColor, jint aKey) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_split(tComm, aColor, aKey, &nComm);
    exceptionCheck(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1group(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    MPI_Group nGroup;
    int tExitCode = MPI_Comm_group(tComm, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1difference(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group) (intptr_t) aGroup1;
    MPI_Group tGroup2 = (MPI_Group) (intptr_t) aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_difference(tGroup1, tGroup2, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1excl(JNIEnv *aEnv, jclass aClazz, jlong aGroup, jintArray aRanks) {
    MPI_Group tGroup = (MPI_Group) (intptr_t) aGroup;
    jsize tLen = aRanks==NULL ? 0 : (*aEnv)->GetArrayLength(aEnv, aRanks);
    int *tRanks = parseJint2int(aEnv, aRanks);
    MPI_Group nGroup;
    int tExitCode = MPI_Group_excl(tGroup, tLen, tRanks, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    if (tRanks != NULL) free(tRanks);
    return nGroup;
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1free(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm) (intptr_t) aGroup;
    int tExitCode = MPI_Group_free(&tGroup);
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1incl(JNIEnv *aEnv, jclass aClazz, jlong aGroup, jintArray aRanks) {
    MPI_Group tGroup = (MPI_Group) (intptr_t) aGroup;
    jsize tLen = aRanks==NULL ? 0 : (*aEnv)->GetArrayLength(aEnv, aRanks);
    int *tRanks = parseJint2int(aEnv, aRanks);
    MPI_Group nGroup;
    int tExitCode = MPI_Group_incl(tGroup, tLen, tRanks, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    if (tRanks != NULL) free(tRanks);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1intersection(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group) (intptr_t) aGroup1;
    MPI_Group tGroup2 = (MPI_Group) (intptr_t) aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_intersection(tGroup1, tGroup2, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1rank(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm) (intptr_t) aGroup;
    int tRank;
    int tExitCode = MPI_Group_rank(tGroup, &tRank);
    exceptionCheck(aEnv, tExitCode);
    return tRank;
}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1size(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm) (intptr_t) aGroup;
    int tSize;
    int tExitCode = MPI_Group_size(tGroup, &tSize);
    exceptionCheck(aEnv, tExitCode);
    return tSize;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1union(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group) (intptr_t) aGroup1;
    MPI_Group tGroup2 = (MPI_Group) (intptr_t) aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_union(tGroup1, tGroup2, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}


JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Send0(JNIEnv *aEnv, jclass aClazz, jobject aArray, jint aCount, jlong aDataType, jint aDest, jint aTag, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype) (intptr_t) aDataType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tBuf = getJArray(aEnv, aArray, tDataType);
    int tExitCode = MPI_Send(tBuf, aCount, tDataType, aDest, aTag, tComm);
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aArray, tDataType, tBuf, JNI_ABORT); // read mode, Do not update the data on the Java heap. Free the space used by the copy.
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Recv0(JNIEnv *aEnv, jclass aClazz, jobject rArray, jint aCount, jlong aDataType, jint aSource, jint aTag, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype) (intptr_t) aDataType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *rBuf = getJArray(aEnv, rArray, tDataType);
    int tExitCode = MPI_Recv(rBuf, aCount, tDataType, aSource, aTag, tComm, MPI_STATUS_IGNORE); // no return Status, because its field name is unstable
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, rArray, tDataType, rBuf, 0); // write mode, Update the data on the Java heap. Free the space used by the copy.
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Sendrecv0(JNIEnv *aEnv, jclass aClazz, jobject aSendArray, jint aSendCount, jlong aSendType, jint aDest, jint aSendTag, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aSource, jint aRecvTag, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype) (intptr_t) aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype) (intptr_t) aRecvType;
    MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
    void *tSendBuf = getJArray(aEnv, aSendArray, tSendType);
    void *rRecvBuf = getJArray(aEnv, rRecvArray, tRecvType);
    int tExitCode = MPI_Sendrecv(tSendBuf, aSendCount, tSendType, aDest, aSendTag, rRecvBuf, aRecvCount, tRecvType, aSource, aRecvTag, tComm, MPI_STATUS_IGNORE); // no return Status, because its field name is unstable
    exceptionCheck(aEnv, tExitCode);
    releaseJArray(aEnv, aSendArray, tSendType, tSendBuf, JNI_ABORT); // read mode, Do not update the data on the Java heap. Free the space used by the copy.
    releaseJArray(aEnv, rRecvArray, tRecvType, rRecvBuf, 0);         // write mode, Update the data on the Java heap. Free the space used by the copy.
}


JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Init_1thread(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs, jint aRequired) {
    int tLen, tProvided;
    char **sArgs = parseArgs(aEnv, aArgs, &tLen);
    int tExitCode = MPI_Init_thread(&tLen, &sArgs, aRequired, &tProvided);
    exceptionCheck(aEnv, tExitCode);
    freeArgs(sArgs, tLen);
    
    tExitCode = MPI_Comm_set_errhandler(MPI_COMM_WORLD, MPI_ERRORS_RETURN);
    exceptionCheck(aEnv, tExitCode);
    
    return tProvided;
}

