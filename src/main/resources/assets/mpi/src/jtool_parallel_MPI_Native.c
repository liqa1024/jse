#ifdef USE_MIMALLOC
    #include "mimalloc.h"
    #define MALLOCN(tp, n) ((tp*)mi_mallocn(n, sizeof(tp)))
    #define CALLOC(tp, n) ((tp*)mi_calloc(n, sizeof(tp)))
    #define FREE(ptr) (mi_free(ptr))
    #define STRDUP(str) (mi_strdup(str))
#else
    #include <stdlib.h>
    #define MALLOCN(tp, n) ((tp*)malloc((n) * sizeof(tp)))
    #define CALLOC(tp, n) ((tp*)calloc(n, sizeof(tp)))
    #define FREE(ptr) (free(ptr))
    
    #include <string.h>
    #ifdef WIN32
    #define STRDUP(str) (_strdup(str))
    #elif _WIN64
    #define STRDUP(str) (_strdup(str))
    #elif _WIN32
    #define STRDUP(str) (_strdup(str))
    #elif __unix__
    #define STRDUP(str) (strdup(str))
    #elif __linux__
    #define STRDUP(str) (strdup(str))
    #endif
#endif

#include <stdint.h>

#include "mpi.h"
#include "jtool_parallel_MPI_Native.h"


/** utils */
void parseBuf2JArray(JNIEnv *aEnv, jobject rJArray, jsize aStart, jsize aLen, MPI_Datatype aDataType, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    switch (aDataType) {
        case MPI_INT8_T: case MPI_SIGNED_CHAR:      {(*aEnv)->SetByteArrayRegion   (aEnv, rJArray, aStart, aLen, ((const jbyte    *)aBuf)+aStart); return;}
        case MPI_DOUBLE:                            {(*aEnv)->SetDoubleArrayRegion (aEnv, rJArray, aStart, aLen, ((const jdouble  *)aBuf)+aStart); return;}
        case MPI_UINT8_T: case MPI_UNSIGNED_CHAR:   {(*aEnv)->SetBooleanArrayRegion(aEnv, rJArray, aStart, aLen, ((const jboolean *)aBuf)+aStart); return;}
        case MPI_UINT16_T: case MPI_UNSIGNED_SHORT: {(*aEnv)->SetCharArrayRegion   (aEnv, rJArray, aStart, aLen, ((const jchar    *)aBuf)+aStart); return;}
        case MPI_INT16_T: case MPI_SHORT:           {(*aEnv)->SetShortArrayRegion  (aEnv, rJArray, aStart, aLen, ((const jshort   *)aBuf)+aStart); return;}
        case MPI_INT32_T:                           {(*aEnv)->SetIntArrayRegion    (aEnv, rJArray, aStart, aLen, ((const jint     *)aBuf)+aStart); return;}
        case MPI_INT64_T:                           {(*aEnv)->SetLongArrayRegion   (aEnv, rJArray, aStart, aLen, ((const jlong    *)aBuf)+aStart); return;}
        case MPI_FLOAT:                             {(*aEnv)->SetFloatArrayRegion  (aEnv, rJArray, aStart, aLen, ((const jfloat   *)aBuf)+aStart); return;}
        default:                                    {return;}
    }
}
void parseJArray2Buf(JNIEnv *aEnv, jobject aJArray, jsize aStart, jsize aLen, MPI_Datatype aDataType, void *rBuf) {
    if (aJArray==NULL || rBuf==NULL) return;
    switch (aDataType) {
        case MPI_INT8_T: case MPI_SIGNED_CHAR:      {(*aEnv)->GetByteArrayRegion   (aEnv, aJArray, aStart, aLen, ((jbyte    *)rBuf)+aStart); return;}
        case MPI_DOUBLE:                            {(*aEnv)->GetDoubleArrayRegion (aEnv, aJArray, aStart, aLen, ((jdouble  *)rBuf)+aStart); return;}
        case MPI_UINT8_T: case MPI_UNSIGNED_CHAR:   {(*aEnv)->GetBooleanArrayRegion(aEnv, aJArray, aStart, aLen, ((jboolean *)rBuf)+aStart); return;}
        case MPI_UINT16_T: case MPI_UNSIGNED_SHORT: {(*aEnv)->GetCharArrayRegion   (aEnv, aJArray, aStart, aLen, ((jchar    *)rBuf)+aStart); return;}
        case MPI_INT16_T: case MPI_SHORT:           {(*aEnv)->GetShortArrayRegion  (aEnv, aJArray, aStart, aLen, ((jshort   *)rBuf)+aStart); return;}
        case MPI_INT32_T:                           {(*aEnv)->GetIntArrayRegion    (aEnv, aJArray, aStart, aLen, ((jint     *)rBuf)+aStart); return;}
        case MPI_INT64_T:                           {(*aEnv)->GetLongArrayRegion   (aEnv, aJArray, aStart, aLen, ((jlong    *)rBuf)+aStart); return;}
        case MPI_FLOAT:                             {(*aEnv)->GetFloatArrayRegion  (aEnv, aJArray, aStart, aLen, ((jfloat   *)rBuf)+aStart); return;}
        default:                                    {return;}
    }
}
void *allocBuf(jsize aSize, MPI_Datatype aDataType) {
    if (aSize <= 0) return NULL;
    switch (aDataType) {
        case MPI_INT8_T: case MPI_SIGNED_CHAR:      {return MALLOCN(jbyte   , aSize);}
        case MPI_DOUBLE:                            {return MALLOCN(jdouble , aSize);}
        case MPI_UINT8_T: case MPI_UNSIGNED_CHAR:   {return MALLOCN(jboolean, aSize);}
        case MPI_UINT16_T: case MPI_UNSIGNED_SHORT: {return MALLOCN(jchar   , aSize);}
        case MPI_INT16_T: case MPI_SHORT:           {return MALLOCN(jshort  , aSize);}
        case MPI_INT32_T:                           {return MALLOCN(jint    , aSize);}
        case MPI_INT64_T:                           {return MALLOCN(jlong   , aSize);}
        case MPI_FLOAT:                             {return MALLOCN(jfloat  , aSize);}
        default:                                    {return NULL;}
    }
}
void freeBuf(void *aBuf) {
    if (aBuf != NULL) FREE(aBuf);
}

void parsejint2int(JNIEnv *aEnv, jintArray aJArray, jint aSize, int *rBuf) {
    if (aJArray==NULL || rBuf==NULL) return;
    jint *tBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);
    for (jint i = 0; i < aSize; ++i) rBuf[i] = (int)tBuf[i];
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, tBuf, JNI_ABORT); // read mode
}

char **parseArgs(JNIEnv *aEnv, jobjectArray aArgs, int *rLen) {
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aArgs);
    char **sArgs = CALLOC(char*, tLen+1);
    
    for (jsize i = 0; i < tLen; i++) {
        jstring jc = (jstring)(*aEnv)->GetObjectArrayElement(aEnv, aArgs, i);
        const char *s = (*aEnv)->GetStringUTFChars(aEnv, jc, NULL);
        sArgs[i] = STRDUP(s);
        (*aEnv)->ReleaseStringUTFChars(aEnv, jc, s);
        (*aEnv)->DeleteLocalRef(aEnv, jc);
    }
    
    *rLen = tLen;
    return sArgs;
}
void freeArgs(char **aArgs, int aLen) {
    for(int i = 0; i < aLen; i++) FREE(aArgs[i]);
    FREE(aArgs);
}

void throwException(JNIEnv *aEnv, const char *aErrStr, int aExitCode) {
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
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, aErrStr);
    jobject tMPIError = (*aEnv)->NewObject(aEnv, tMPIErrorClazz, tMPIErrorInit, aExitCode, tJErrStr);
    (*aEnv)->Throw(aEnv, tMPIError);
    (*aEnv)->DeleteLocalRef(aEnv, tMPIError);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tMPIErrorClazz);
}
jboolean exceptionCheck(JNIEnv *aEnv, int aExitCode) {
    if (aExitCode == MPI_SUCCESS) return JNI_FALSE;
    
    char rErrStr[MPI_MAX_ERROR_STRING];
    int rLen;
    MPI_Error_string(aExitCode, rErrStr, &rLen);
    
    throwException(aEnv, rErrStr, aExitCode);
    return JNI_TRUE;
}

int getSizeAndRank(MPI_Comm aComm, int* rSize, int* rRank) {
    int tExitCode;
    tExitCode = MPI_Comm_size(aComm, rSize);
    if (tExitCode != MPI_SUCCESS) return tExitCode;
    tExitCode = MPI_Comm_rank(aComm, rRank);
    return tExitCode;
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
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tRank;
    int tExitCode = MPI_Comm_rank(tComm, &tRank);
    exceptionCheck(aEnv, tExitCode);
    return tRank;
}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1size(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
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
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    jsize tTotSize = (jsize)(aRecvCount*tSize);
    void *rRecvBuf = allocBuf(tTotSize, tRecvType);
    if (aInPlace) {
        parseJArray2Buf(aEnv, rRecvArray, aRecvCount*tRank, aRecvCount, tRecvType, rRecvBuf);
        tExitCode = MPI_Allgather(MPI_IN_PLACE, 0, tSendType, rRecvBuf, aRecvCount, tRecvType, tComm);
        exceptionCheck(aEnv, tExitCode);
    } else {
        void *tSendBuf = allocBuf(aSendCount, tSendType);
        parseJArray2Buf(aEnv, aSendArray, 0, aSendCount, tSendType, tSendBuf);
        tExitCode = MPI_Allgather(tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, tComm);
        exceptionCheck(aEnv, tExitCode);
        freeBuf(tSendBuf);
    }
    parseBuf2JArray(aEnv, rRecvArray, 0, tTotSize, tRecvType, rRecvBuf);
    freeBuf(rRecvBuf);
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Allgatherv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jintArray aRecvCounts, jintArray aDispls, jlong aRecvType, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    int *tRecvCounts = MALLOCN(int, tSize); parsejint2int(aEnv, aRecvCounts, tSize, tRecvCounts);
    int *tDispls     = MALLOCN(int, tSize); parsejint2int(aEnv, aDispls    , tSize, tDispls    );
    jsize tTotSize = 0;
    for (int i = 0; i < tSize; ++i) tTotSize += (jsize)tRecvCounts[i];
    void *rRecvBuf = allocBuf(tTotSize  , tRecvType);
    if (aInPlace) {
        parseJArray2Buf(aEnv, rRecvArray, tDispls[tRank], tRecvCounts[tRank], tRecvType, rRecvBuf);
        tExitCode = MPI_Allgatherv(MPI_IN_PLACE, 0, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, tComm);
        exceptionCheck(aEnv, tExitCode);
    } else {
        void *tSendBuf = allocBuf(aSendCount, tSendType);
        parseJArray2Buf(aEnv, aSendArray, 0, aSendCount, tSendType, tSendBuf);
        tExitCode = MPI_Allgatherv(tSendBuf, aSendCount, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, tComm);
        exceptionCheck(aEnv, tExitCode);
        freeBuf(tSendBuf);
    }
    parseBuf2JArray(aEnv, rRecvArray, 0, tTotSize, tRecvType, rRecvBuf);
    freeBuf(rRecvBuf);
    FREE(tRecvCounts);
    FREE(tDispls    );
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Allreduce0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jobject rRecvArray, jint aCount, jlong aDataType, jlong aOp, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Op tOp = (MPI_Op)(intptr_t)aOp;
    void *rRecvBuf = allocBuf(aCount, tDataType);
    if (aInPlace) {
        parseJArray2Buf(aEnv, rRecvArray, 0, aCount, tDataType, rRecvBuf);
        int tExitCode = MPI_Allreduce(MPI_IN_PLACE, rRecvBuf, aCount, tDataType, tOp, tComm);
        exceptionCheck(aEnv, tExitCode);
    } else {
        void *tSendBuf = allocBuf(aCount, tDataType);
        parseJArray2Buf(aEnv, aSendArray, 0, aCount, tDataType, tSendBuf);
        int tExitCode = MPI_Allreduce(tSendBuf, rRecvBuf, aCount, tDataType, tOp, tComm);
        exceptionCheck(aEnv, tExitCode);
        freeBuf(tSendBuf);
    }
    parseBuf2JArray(aEnv, rRecvArray, 0, aCount, tDataType, rRecvBuf);
    freeBuf(rRecvBuf);
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Barrier(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tExitCode = MPI_Barrier(tComm);
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Bcast0(JNIEnv *aEnv, jclass aClazz, jobject rArray, jint aCount, jlong aDataType, jint aRoot, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    void *rBuf = allocBuf(aCount, tDataType);
    if (tRank == aRoot) {
        parseJArray2Buf(aEnv, rArray, 0, aCount, tDataType, rBuf);
        tExitCode = MPI_Bcast(rBuf, aCount, tDataType, aRoot, tComm);
        exceptionCheck(aEnv, tExitCode);
    } else {
        tExitCode = MPI_Bcast(rBuf, aCount, tDataType, aRoot, tComm);
        exceptionCheck(aEnv, tExitCode);
        parseBuf2JArray(aEnv, rArray, 0, aCount, tDataType, rBuf);
    }
    freeBuf(rBuf);
}

JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Gather0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        jsize tTotSize = (jsize)(aRecvCount*tSize);
        void *rRecvBuf = allocBuf(tTotSize, tRecvType);
        if (aInPlace) {
            parseJArray2Buf(aEnv, rRecvArray, aRecvCount*tRank, aRecvCount, tRecvType, rRecvBuf);
            tExitCode = MPI_Gather(MPI_IN_PLACE, 0, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
        } else {
            void *tSendBuf = allocBuf(aSendCount, tSendType);
            parseJArray2Buf(aEnv, aSendArray, 0, aSendCount, tSendType, tSendBuf);
            tExitCode = MPI_Gather(tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
        parseBuf2JArray(aEnv, rRecvArray, 0, tTotSize, tRecvType, rRecvBuf);
        freeBuf(rRecvBuf);
    } else {
        if (aInPlace) {
            throwException(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Gather", -1);
            return;
        } else {
            void *tSendBuf = allocBuf(aSendCount, tSendType);
            parseJArray2Buf(aEnv, aSendArray, 0, aSendCount, tSendType, tSendBuf);
            tExitCode = MPI_Gather(tSendBuf, aSendCount, tSendType, NULL, 0, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
    }
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Gatherv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jintArray aRecvCounts, jintArray aDispls, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        int *tRecvCounts = MALLOCN(int, tSize); parsejint2int(aEnv, aRecvCounts, tSize, tRecvCounts);
        int *tDispls     = MALLOCN(int, tSize); parsejint2int(aEnv, aDispls    , tSize, tDispls    );
        jsize tTotSize = 0;
        for (int i = 0; i < tSize; ++i) tTotSize += (jsize)tRecvCounts[i];
        void *rRecvBuf = allocBuf(tTotSize, tRecvType);
        if (aInPlace) {
            parseJArray2Buf(aEnv, rRecvArray, tDispls[tRank], tRecvCounts[tRank], tRecvType, rRecvBuf);
            tExitCode = MPI_Gatherv(MPI_IN_PLACE, 0, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
        } else {
            void *tSendBuf = allocBuf(aSendCount, tSendType);
            parseJArray2Buf(aEnv, aSendArray, 0, aSendCount, tSendType, tSendBuf);
            tExitCode = MPI_Gatherv(tSendBuf, aSendCount, tSendType, rRecvBuf, tRecvCounts, tDispls, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
        parseBuf2JArray(aEnv, rRecvArray, 0, tTotSize, tRecvType, rRecvBuf);
        freeBuf(rRecvBuf);
        FREE(tRecvCounts);
        FREE(tDispls    );
    } else {
        if (aInPlace) {
            throwException(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Gatherv", -1);
            return;
        } else {
            void *tSendBuf = allocBuf(aSendCount, tSendType);
            parseJArray2Buf(aEnv, aSendArray, 0, aSendCount, tSendType, tSendBuf);
            tExitCode = MPI_Gatherv(tSendBuf, aSendCount, tSendType, NULL, NULL, NULL, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
    }
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Reduce0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jobject rRecvArray, jint aCount, jlong aDataType, jlong aOp, jint aRoot, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Op tOp = (MPI_Op)(intptr_t)aOp;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        void *rRecvBuf = allocBuf(aCount, tDataType);
        if (aInPlace) {
            parseJArray2Buf(aEnv, rRecvArray, 0, aCount, tDataType, rRecvBuf);
            tExitCode = MPI_Reduce(MPI_IN_PLACE, rRecvBuf, aCount, tDataType, tOp, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
        } else {
            void *tSendBuf = allocBuf(aCount, tDataType);
            parseJArray2Buf(aEnv, aSendArray, 0, aCount, tDataType, tSendBuf);
            tExitCode = MPI_Reduce(tSendBuf, rRecvBuf, aCount, tDataType, tOp, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
        parseBuf2JArray(aEnv, rRecvArray, 0, aCount, tDataType, rRecvBuf);
        freeBuf(rRecvBuf);
    } else {
        if (aInPlace) {
            throwException(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Reduce", -1);
            return;
        } else {
            void *tSendBuf = allocBuf(aCount, tDataType);
            parseJArray2Buf(aEnv, aSendArray, 0, aCount, tDataType, tSendBuf);
            tExitCode = MPI_Reduce(tSendBuf, NULL, aCount, tDataType, tOp, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            freeBuf(tSendBuf);
        }
    }
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Scatter0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jint aSendCount, jlong aSendType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        jsize tTotSize = (jsize)(aSendCount*tSize);
        void *tSendBuf = allocBuf(tTotSize, tSendType);
        parseJArray2Buf(aEnv, aSendArray, 0, tTotSize, tSendType, tSendBuf);
        if (aInPlace) {
            tExitCode = MPI_Scatter(tSendBuf, aSendCount, tSendType, MPI_IN_PLACE, 0, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
        } else {
            void *rRecvBuf = allocBuf(aRecvCount, tRecvType);
            tExitCode = MPI_Scatter(tSendBuf, aSendCount, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, 0, aRecvCount, tRecvType, rRecvBuf);
            freeBuf(rRecvBuf);
        }
        freeBuf(tSendBuf);
    } else {
        if (aInPlace) {
            throwException(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Scatter", -1);
            return;
        } else {
            void *rRecvBuf = allocBuf(aRecvCount, tRecvType);
            tExitCode = MPI_Scatter(NULL, 0, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, 0, aRecvCount, tRecvType, rRecvBuf);
            freeBuf(rRecvBuf);
        }
    }
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Scatterv0(JNIEnv *aEnv, jclass aClazz, jboolean aInPlace, jobject aSendArray, jintArray aSendCounts, jintArray aDispls, jlong aSendType, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aRoot, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tSize, tRank;
    int tExitCode = getSizeAndRank(tComm, &tSize, &tRank);
    if (exceptionCheck(aEnv, tExitCode)) return;
    if (tRank == aRoot) {
        int *tSendCounts = MALLOCN(int, tSize); parsejint2int(aEnv, aSendCounts, tSize, tSendCounts);
        int *tDispls     = MALLOCN(int, tSize); parsejint2int(aEnv, aDispls    , tSize, tDispls    );
        jsize tTotSize = 0;
        for (int i = 0; i < tSize; ++i) tTotSize += (jsize)tSendCounts[i];
        void *tSendBuf = allocBuf(tTotSize, tSendType);
        parseJArray2Buf(aEnv, aSendArray, 0, tTotSize, tSendType, tSendBuf);
        if (aInPlace) {
            tExitCode = MPI_Scatterv(tSendBuf, tSendCounts, tDispls, tSendType, MPI_IN_PLACE, 0, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
        } else {
            void *rRecvBuf = allocBuf(aRecvCount, tRecvType);
            tExitCode = MPI_Scatterv(tSendBuf, tSendCounts, tDispls, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, 0, aRecvCount, tRecvType, rRecvBuf);
            freeBuf(rRecvBuf);
        }
        freeBuf(tSendBuf);
        FREE(tSendCounts);
        FREE(tDispls    );
    } else {
        if (aInPlace) {
            throwException(aEnv, "MPI_IN_PLACE can ONLY be set in Root for MPI_Scatterv", -1);
            return;
        } else {
            void *rRecvBuf = allocBuf(aRecvCount, tRecvType);
            tExitCode = MPI_Scatterv(NULL, NULL, NULL, tSendType, rRecvBuf, aRecvCount, tRecvType, aRoot, tComm);
            exceptionCheck(aEnv, tExitCode);
            parseBuf2JArray(aEnv, rRecvArray, 0, aRecvCount, tRecvType, rRecvBuf);
            freeBuf(rRecvBuf);
        }
    }
}


JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1create(JNIEnv *aEnv, jclass aClazz, jlong aComm, jlong aGroup) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Group tGroup = (MPI_Group)(intptr_t)aGroup;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_create(tComm, tGroup, &nComm);
    exceptionCheck(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1dup(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_dup(tComm, &nComm);
    exceptionCheck(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1free(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    int tExitCode = MPI_Comm_free(&tComm);
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1split(JNIEnv *aEnv, jclass aClazz, jlong aComm, jint aColor, jint aKey) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Comm nComm;
    int tExitCode = MPI_Comm_split(tComm, aColor, aKey, &nComm);
    exceptionCheck(aEnv, tExitCode);
    return nComm;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Comm_1group(JNIEnv *aEnv, jclass aClazz, jlong aComm) {
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    MPI_Group nGroup;
    int tExitCode = MPI_Comm_group(tComm, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1difference(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group)(intptr_t)aGroup1;
    MPI_Group tGroup2 = (MPI_Group)(intptr_t)aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_difference(tGroup1, tGroup2, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1excl(JNIEnv *aEnv, jclass aClazz, jlong aGroup, jint aN, jintArray aRanks) {
    MPI_Group tGroup = (MPI_Group)(intptr_t)aGroup;
    int *tRanks = (aRanks==NULL || aN<=0) ? NULL : MALLOCN(int, aN); parsejint2int(aEnv, aRanks, aN, tRanks);
    MPI_Group nGroup;
    int tExitCode = MPI_Group_excl(tGroup, aN, tRanks, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    if (tRanks != NULL) FREE(tRanks);
    return nGroup;
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1free(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm)(intptr_t)aGroup;
    int tExitCode = MPI_Group_free(&tGroup);
    exceptionCheck(aEnv, tExitCode);
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1incl(JNIEnv *aEnv, jclass aClazz, jlong aGroup, jint aN, jintArray aRanks) {
    MPI_Group tGroup = (MPI_Group)(intptr_t)aGroup;
    int *tRanks = (aRanks==NULL || aN<=0) ? NULL : MALLOCN(int, aN); parsejint2int(aEnv, aRanks, aN, tRanks);
    MPI_Group nGroup;
    int tExitCode = MPI_Group_incl(tGroup, aN, tRanks, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    if (tRanks != NULL) FREE(tRanks);
    return nGroup;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1intersection(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group)(intptr_t)aGroup1;
    MPI_Group tGroup2 = (MPI_Group)(intptr_t)aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_intersection(tGroup1, tGroup2, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1rank(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm)(intptr_t)aGroup;
    int tRank;
    int tExitCode = MPI_Group_rank(tGroup, &tRank);
    exceptionCheck(aEnv, tExitCode);
    return tRank;
}
JNIEXPORT jint JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1size(JNIEnv *aEnv, jclass aClazz, jlong aGroup) {
    MPI_Comm tGroup = (MPI_Comm)(intptr_t)aGroup;
    int tSize;
    int tExitCode = MPI_Group_size(tGroup, &tSize);
    exceptionCheck(aEnv, tExitCode);
    return tSize;
}
JNIEXPORT jlong JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Group_1union(JNIEnv *aEnv, jclass aClazz, jlong aGroup1, jlong aGroup2) {
    MPI_Group tGroup1 = (MPI_Group)(intptr_t)aGroup1;
    MPI_Group tGroup2 = (MPI_Group)(intptr_t)aGroup2;
    MPI_Group nGroup;
    int tExitCode = MPI_Group_union(tGroup1, tGroup2, &nGroup);
    exceptionCheck(aEnv, tExitCode);
    return nGroup;
}


JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Send0(JNIEnv *aEnv, jclass aClazz, jobject aArray, jint aCount, jlong aDataType, jint aDest, jint aTag, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    void *tBuf = allocBuf(aCount, tDataType);
    parseJArray2Buf(aEnv, aArray, 0, aCount, tDataType, tBuf);
    int tExitCode = MPI_Send(tBuf, aCount, tDataType, aDest, aTag, tComm);
    exceptionCheck(aEnv, tExitCode);
    freeBuf(tBuf);
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Recv0(JNIEnv *aEnv, jclass aClazz, jobject rArray, jint aCount, jlong aDataType, jint aSource, jint aTag, jlong aComm) {
    MPI_Datatype tDataType = (MPI_Datatype)(intptr_t)aDataType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    void *rBuf = allocBuf(aCount, tDataType);
    int tExitCode = MPI_Recv(rBuf, aCount, tDataType, aSource, aTag, tComm, MPI_STATUS_IGNORE); // no return Status, because its field name is unstable
    exceptionCheck(aEnv, tExitCode);
    parseBuf2JArray(aEnv, rArray, 0, aCount, tDataType, rBuf);
    freeBuf(rBuf);
}
JNIEXPORT void JNICALL Java_jtool_parallel_MPI_00024Native_MPI_1Sendrecv0(JNIEnv *aEnv, jclass aClazz, jobject aSendArray, jint aSendCount, jlong aSendType, jint aDest, jint aSendTag, jobject rRecvArray, jint aRecvCount, jlong aRecvType, jint aSource, jint aRecvTag, jlong aComm) {
    MPI_Datatype tSendType = (MPI_Datatype)(intptr_t)aSendType;
    MPI_Datatype tRecvType = (MPI_Datatype)(intptr_t)aRecvType;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
    void *tSendBuf = allocBuf(aSendCount, tSendType);
    parseJArray2Buf(aEnv, aSendArray, 0, aSendCount, tSendType, tSendBuf);
    void *rRecvBuf = allocBuf(aRecvCount, tRecvType);
    int tExitCode = MPI_Sendrecv(tSendBuf, aSendCount, tSendType, aDest, aSendTag, rRecvBuf, aRecvCount, tRecvType, aSource, aRecvTag, tComm, MPI_STATUS_IGNORE); // no return Status, because its field name is unstable
    exceptionCheck(aEnv, tExitCode);
    parseBuf2JArray(aEnv, rRecvArray, 0, aRecvCount, tRecvType, rRecvBuf);
    freeBuf(tSendBuf);
    freeBuf(rRecvBuf);
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

