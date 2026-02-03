#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic push
#pragma ide diagnostic ignored "modernize-deprecated-headers"
#pragma ide diagnostic ignored "modernize-use-auto"
#pragma ide diagnostic ignored "modernize-use-nullptr"
#endif

#include <jni.h>

#ifndef JNIUTIL_H
#define JNIUTIL_H

/** mimalloc stuffs */
#ifdef USE_MIMALLOC
    #include "mimalloc.h"
    #undef MALLOCN
    #define MALLOCN(count, size) (mi_mallocn(count, size))
    #undef CALLOC
    #define CALLOC(count, size) (mi_calloc(count, size))
    #undef MALLOCN_TP
    #define MALLOCN_TP(tp, n) ((tp*)mi_mallocn(n, sizeof(tp)))
    #undef CALLOC_TP
    #define CALLOC_TP(tp, n) ((tp*)mi_calloc(n, sizeof(tp)))
    #undef FREE
    #define FREE(ptr) (mi_free(ptr))
    #undef STRDUP
    #define STRDUP(str) (mi_strdup(str))
#else
    #include <stdlib.h>
    #undef MALLOCN
    #define MALLOCN(count, size) (malloc((count) * (size)))
    #undef CALLOC
    #define CALLOC(count, size) (calloc(count, size))
    #undef MALLOCN_TP
    #define MALLOCN_TP(tp, n) ((tp*)malloc((n) * sizeof(tp)))
    #undef CALLOC_TP
    #define CALLOC_TP(tp, n) ((tp*)calloc(n, sizeof(tp)))
    #undef FREE
    #define FREE(ptr) (free(ptr))
    
    #include <string.h>
    #undef STRDUP
    #if defined(WIN32) || defined(_WIN64) || defined(_WIN32)
    #define STRDUP(str) (_strdup(str))
    #else
    #define STRDUP(str) (strdup(str))
    #endif
#endif

#include <stdint.h>


/** jarray type stuffs */
#undef JTYPE_NULL
#define JTYPE_NULL      (0)
#undef JTYPE_BYTE
#define JTYPE_BYTE      (1)
#undef JTYPE_DOUBLE
#define JTYPE_DOUBLE    (2)
#undef JTYPE_BOOLEAN
#define JTYPE_BOOLEAN   (3)
#undef JTYPE_CHAR
#define JTYPE_CHAR      (4)
#undef JTYPE_SHORT
#define JTYPE_SHORT     (5)
#undef JTYPE_INT
#define JTYPE_INT       (6)
#undef JTYPE_LONG
#define JTYPE_LONG      (7)
#undef JTYPE_FLOAT
#define JTYPE_FLOAT     (8)

#ifdef __cplusplus
extern "C" {
#endif

/** jarray to buf stuffs */
static inline void *getJArrayBuf(JNIEnv *aEnv, jarray aJArray) {
    if (aJArray == NULL) return NULL;
#ifdef __cplusplus
    return aEnv->GetPrimitiveArrayCritical(aJArray, NULL);
#else
    return (*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);
#endif
}

static inline void releaseJArrayBuf(JNIEnv *aEnv, jarray aJArray, void *aBuf, jint aMode) {
    if (aJArray==NULL || aBuf==NULL) return;
#ifdef __cplusplus
    aEnv->ReleasePrimitiveArrayCritical(aJArray, aBuf, aMode);
#else
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, aBuf, aMode);
#endif
}

static inline void parseBuf2JArrayV(JNIEnv *aEnv, jobject rJArray, jsize aJStart, jint aJArrayType, const void *aBuf, jsize aBStart, jsize aLen) {
    if (rJArray==NULL || aBuf==NULL) return;
    switch (aJArrayType) {
#ifdef __cplusplus
        case JTYPE_BYTE:    {aEnv->SetByteArrayRegion   ((jbyteArray   )rJArray, aJStart, aLen, ((const jbyte    *)aBuf)+aBStart); return;}
        case JTYPE_DOUBLE:  {aEnv->SetDoubleArrayRegion ((jdoubleArray )rJArray, aJStart, aLen, ((const jdouble  *)aBuf)+aBStart); return;}
        case JTYPE_BOOLEAN: {aEnv->SetBooleanArrayRegion((jbooleanArray)rJArray, aJStart, aLen, ((const jboolean *)aBuf)+aBStart); return;}
        case JTYPE_CHAR:    {aEnv->SetCharArrayRegion   ((jcharArray   )rJArray, aJStart, aLen, ((const jchar    *)aBuf)+aBStart); return;}
        case JTYPE_SHORT:   {aEnv->SetShortArrayRegion  ((jshortArray  )rJArray, aJStart, aLen, ((const jshort   *)aBuf)+aBStart); return;}
        case JTYPE_INT:     {aEnv->SetIntArrayRegion    ((jintArray    )rJArray, aJStart, aLen, ((const jint     *)aBuf)+aBStart); return;}
        case JTYPE_LONG:    {aEnv->SetLongArrayRegion   ((jlongArray   )rJArray, aJStart, aLen, ((const jlong    *)aBuf)+aBStart); return;}
        case JTYPE_FLOAT:   {aEnv->SetFloatArrayRegion  ((jfloatArray  )rJArray, aJStart, aLen, ((const jfloat   *)aBuf)+aBStart); return;}
#else
        case JTYPE_BYTE:    {(*aEnv)->SetByteArrayRegion   (aEnv, (jbyteArray   )rJArray, aJStart, aLen, ((const jbyte    *)aBuf)+aBStart); return;}
        case JTYPE_DOUBLE:  {(*aEnv)->SetDoubleArrayRegion (aEnv, (jdoubleArray )rJArray, aJStart, aLen, ((const jdouble  *)aBuf)+aBStart); return;}
        case JTYPE_BOOLEAN: {(*aEnv)->SetBooleanArrayRegion(aEnv, (jbooleanArray)rJArray, aJStart, aLen, ((const jboolean *)aBuf)+aBStart); return;}
        case JTYPE_CHAR:    {(*aEnv)->SetCharArrayRegion   (aEnv, (jcharArray   )rJArray, aJStart, aLen, ((const jchar    *)aBuf)+aBStart); return;}
        case JTYPE_SHORT:   {(*aEnv)->SetShortArrayRegion  (aEnv, (jshortArray  )rJArray, aJStart, aLen, ((const jshort   *)aBuf)+aBStart); return;}
        case JTYPE_INT:     {(*aEnv)->SetIntArrayRegion    (aEnv, (jintArray    )rJArray, aJStart, aLen, ((const jint     *)aBuf)+aBStart); return;}
        case JTYPE_LONG:    {(*aEnv)->SetLongArrayRegion   (aEnv, (jlongArray   )rJArray, aJStart, aLen, ((const jlong    *)aBuf)+aBStart); return;}
        case JTYPE_FLOAT:   {(*aEnv)->SetFloatArrayRegion  (aEnv, (jfloatArray  )rJArray, aJStart, aLen, ((const jfloat   *)aBuf)+aBStart); return;}
#endif
        default:            {return;}
    }
}
static inline void parseBuf2JArray(JNIEnv *aEnv, jobject rJArray, jint aJArrayType, const void *aBuf, jsize aLen) {parseBuf2JArrayV(aEnv, rJArray, 0, aJArrayType, aBuf, 0, aLen);}

static inline void parseJArray2BufV(JNIEnv *aEnv, jobject aJArray, jsize aJStart, jint aJArrayType, void *rBuf, jsize aBStart, jsize aLen) {
    if (aJArray==NULL || rBuf==NULL) return;
    switch (aJArrayType) {
#ifdef __cplusplus
        case JTYPE_BYTE:    {aEnv->GetByteArrayRegion   ((jbyteArray   )aJArray, aJStart, aLen, ((jbyte    *)rBuf)+aBStart); return;}
        case JTYPE_DOUBLE:  {aEnv->GetDoubleArrayRegion ((jdoubleArray )aJArray, aJStart, aLen, ((jdouble  *)rBuf)+aBStart); return;}
        case JTYPE_BOOLEAN: {aEnv->GetBooleanArrayRegion((jbooleanArray)aJArray, aJStart, aLen, ((jboolean *)rBuf)+aBStart); return;}
        case JTYPE_CHAR:    {aEnv->GetCharArrayRegion   ((jcharArray   )aJArray, aJStart, aLen, ((jchar    *)rBuf)+aBStart); return;}
        case JTYPE_SHORT:   {aEnv->GetShortArrayRegion  ((jshortArray  )aJArray, aJStart, aLen, ((jshort   *)rBuf)+aBStart); return;}
        case JTYPE_INT:     {aEnv->GetIntArrayRegion    ((jintArray    )aJArray, aJStart, aLen, ((jint     *)rBuf)+aBStart); return;}
        case JTYPE_LONG:    {aEnv->GetLongArrayRegion   ((jlongArray   )aJArray, aJStart, aLen, ((jlong    *)rBuf)+aBStart); return;}
        case JTYPE_FLOAT:   {aEnv->GetFloatArrayRegion  ((jfloatArray  )aJArray, aJStart, aLen, ((jfloat   *)rBuf)+aBStart); return;}
#else
        case JTYPE_BYTE:    {(*aEnv)->GetByteArrayRegion   (aEnv, (jbyteArray   )aJArray, aJStart, aLen, ((jbyte    *)rBuf)+aBStart); return;}
        case JTYPE_DOUBLE:  {(*aEnv)->GetDoubleArrayRegion (aEnv, (jdoubleArray )aJArray, aJStart, aLen, ((jdouble  *)rBuf)+aBStart); return;}
        case JTYPE_BOOLEAN: {(*aEnv)->GetBooleanArrayRegion(aEnv, (jbooleanArray)aJArray, aJStart, aLen, ((jboolean *)rBuf)+aBStart); return;}
        case JTYPE_CHAR:    {(*aEnv)->GetCharArrayRegion   (aEnv, (jcharArray   )aJArray, aJStart, aLen, ((jchar    *)rBuf)+aBStart); return;}
        case JTYPE_SHORT:   {(*aEnv)->GetShortArrayRegion  (aEnv, (jshortArray  )aJArray, aJStart, aLen, ((jshort   *)rBuf)+aBStart); return;}
        case JTYPE_INT:     {(*aEnv)->GetIntArrayRegion    (aEnv, (jintArray    )aJArray, aJStart, aLen, ((jint     *)rBuf)+aBStart); return;}
        case JTYPE_LONG:    {(*aEnv)->GetLongArrayRegion   (aEnv, (jlongArray   )aJArray, aJStart, aLen, ((jlong    *)rBuf)+aBStart); return;}
        case JTYPE_FLOAT:   {(*aEnv)->GetFloatArrayRegion  (aEnv, (jfloatArray  )aJArray, aJStart, aLen, ((jfloat   *)rBuf)+aBStart); return;}
#endif
        default:            {return;}
    }
}
static inline void parseJArray2Buf(JNIEnv *aEnv, jobject aJArray, jint aJArrayType, void *rBuf, jsize aLen) {parseJArray2BufV(aEnv, aJArray, 0, aJArrayType, rBuf, 0, aLen);}

static inline void *allocBuf(jint aJArrayType, jsize aSize) {
    if (aSize <= 0) return NULL;
    switch (aJArrayType) {
        case JTYPE_BYTE:    {return MALLOCN_TP(jbyte   , aSize);}
        case JTYPE_DOUBLE:  {return MALLOCN_TP(jdouble , aSize);}
        case JTYPE_BOOLEAN: {return MALLOCN_TP(jboolean, aSize);}
        case JTYPE_CHAR:    {return MALLOCN_TP(jchar   , aSize);}
        case JTYPE_SHORT:   {return MALLOCN_TP(jshort  , aSize);}
        case JTYPE_INT:     {return MALLOCN_TP(jint    , aSize);}
        case JTYPE_LONG:    {return MALLOCN_TP(jlong   , aSize);}
        case JTYPE_FLOAT:   {return MALLOCN_TP(jfloat  , aSize);}
        default:            {return NULL;}
    }
}
static inline void freeBuf(void *aBuf) {
    if (aBuf != NULL) FREE(aBuf);
}
static inline void *shiftBuf(void *aBuf, jint aJArrayType, jint aShift) {
    if (aBuf == NULL) return NULL;
    switch (aJArrayType) {
    case JTYPE_BYTE:    {return (jbyte    *)aBuf + aShift;}
    case JTYPE_DOUBLE:  {return (jdouble  *)aBuf + aShift;}
    case JTYPE_BOOLEAN: {return (jboolean *)aBuf + aShift;}
    case JTYPE_CHAR:    {return (jchar    *)aBuf + aShift;}
    case JTYPE_SHORT:   {return (jshort   *)aBuf + aShift;}
    case JTYPE_INT:     {return (jint     *)aBuf + aShift;}
    case JTYPE_LONG:    {return (jlong    *)aBuf + aShift;}
    case JTYPE_FLOAT:   {return (jfloat   *)aBuf + aShift;}
    default:            {return NULL;}
    }
}


#undef GEN_PARSE_ANY_TO_JANY
#ifdef __cplusplus
#define GEN_PARSE_ANY_TO_JANY(CTYPE, JTYPE)                                                                                                 \
static inline void parse##CTYPE##2##JTYPE##V(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const CTYPE *aBuf, jsize aBStart, jsize aLen) {\
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                \
    JTYPE *rBuf = (JTYPE *)aEnv->GetPrimitiveArrayCritical(rJArray, NULL);                                                                  \
    JTYPE *ji = rBuf + aJStart; const CTYPE *bi = aBuf + aBStart;                                                                           \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *ji = (JTYPE)(*bi);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    aEnv->ReleasePrimitiveArrayCritical(rJArray, rBuf, 0);                                                                                  \
}                                                                                                                                           \
static inline void parse##CTYPE##2##JTYPE(JNIEnv *aEnv, JTYPE##Array rJArray, const CTYPE *aBuf, jsize aLen) {                              \
    parse##CTYPE##2##JTYPE##V(aEnv, rJArray, 0, aBuf, 0, aLen);                                                                             \
}
#else
#define GEN_PARSE_ANY_TO_JANY(CTYPE, JTYPE)                                                                                                 \
static inline void parse##CTYPE##2##JTYPE##V(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const CTYPE *aBuf, jsize aBStart, jsize aLen) {\
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                \
    JTYPE *rBuf = (JTYPE *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);                                                         \
    JTYPE *ji = rBuf + aJStart; const CTYPE *bi = aBuf + aBStart;                                                                           \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *ji = (JTYPE)(*bi);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0);                                                                         \
}                                                                                                                                           \
static inline void parse##CTYPE##2##JTYPE(JNIEnv *aEnv, JTYPE##Array rJArray, const CTYPE *aBuf, jsize aLen) {                              \
    parse##CTYPE##2##JTYPE##V(aEnv, rJArray, 0, aBuf, 0, aLen);                                                                             \
}
#endif

GEN_PARSE_ANY_TO_JANY(int, jint)
GEN_PARSE_ANY_TO_JANY(int64_t, jint)
GEN_PARSE_ANY_TO_JANY(double, jint)
GEN_PARSE_ANY_TO_JANY(int, jlong)
GEN_PARSE_ANY_TO_JANY(int64_t, jlong)
GEN_PARSE_ANY_TO_JANY(double, jlong)
GEN_PARSE_ANY_TO_JANY(int, jdouble)
GEN_PARSE_ANY_TO_JANY(int64_t, jdouble)
GEN_PARSE_ANY_TO_JANY(float, jdouble)

static inline void parsedouble2jdoubleV(JNIEnv *aEnv, jdoubleArray rJArray, jsize aJStart, const double *aBuf, jsize aBStart, jsize aLen) {
    if (rJArray==NULL || aBuf==NULL) return;
#ifdef __cplusplus
    // jdouble is always double
    aEnv->SetDoubleArrayRegion(rJArray, aJStart, aLen, (aBuf+aBStart));
#else
    // jdouble is always double
    (*aEnv)->SetDoubleArrayRegion(aEnv, rJArray, aJStart, aLen, (aBuf+aBStart));
#endif
}
static inline void parsedouble2jdouble(JNIEnv *aEnv, jdoubleArray rJArray, const double *aBuf, jsize aLen) {
    parsedouble2jdoubleV(aEnv, rJArray, 0, aBuf, 0, aLen);
}

static inline void parsefloat2jfloatV(JNIEnv *aEnv, jfloatArray rJArray, jsize aJStart, const float *aBuf, jsize aBStart, jsize aLen) {
    if (rJArray==NULL || aBuf==NULL) return;
#ifdef __cplusplus
    // jfloat is always float
    aEnv->SetFloatArrayRegion(rJArray, aJStart, aLen, (aBuf+aBStart));
#else
    // jfloat is always float
    (*aEnv)->SetFloatArrayRegion(aEnv, rJArray, aJStart, aLen, (aBuf+aBStart));
#endif
}
static inline void parsefloat2jfloat(JNIEnv *aEnv, jfloatArray rJArray, const float *aBuf, jsize aLen) {
    parsefloat2jfloatV(aEnv, rJArray, 0, aBuf, 0, aLen);
}

#undef GEN_PARSE_JANY_TO_ANY
#ifdef __cplusplus
#define GEN_PARSE_JANY_TO_ANY(JTYPE, CTYPE)                                                                                                 \
static inline void parse##JTYPE##2##CTYPE##V(JNIEnv *aEnv, JTYPE##Array aJArray, jsize aJStart, CTYPE *rBuf, jsize aBStart, jsize aLen) {   \
    if (aJArray==NULL || rBuf==NULL) return;                                                                                                \
    JTYPE *tBuf = (JTYPE *)aEnv->GetPrimitiveArrayCritical(aJArray, NULL);                                                                  \
    JTYPE *ji = tBuf + aJStart; CTYPE *bi = rBuf + aBStart;                                                                                 \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *bi = (CTYPE)(*ji);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    aEnv->ReleasePrimitiveArrayCritical(aJArray, tBuf, JNI_ABORT);                                                                          \
}                                                                                                                                           \
static inline void parse##JTYPE##2##CTYPE(JNIEnv *aEnv, JTYPE##Array aJArray, CTYPE *rBuf, jsize aLen) {                                    \
    parse##JTYPE##2##CTYPE##V(aEnv, aJArray, 0, rBuf, 0, aLen);                                                                             \
}
#else
#define GEN_PARSE_JANY_TO_ANY(JTYPE, CTYPE)                                                                                                 \
static inline void parse##JTYPE##2##CTYPE##V(JNIEnv *aEnv, JTYPE##Array aJArray, jsize aJStart, CTYPE *rBuf, jsize aBStart, jsize aLen) {   \
    if (aJArray==NULL || rBuf==NULL) return;                                                                                                \
    JTYPE *tBuf = (JTYPE *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);                                                         \
    JTYPE *ji = tBuf + aJStart; CTYPE *bi = rBuf + aBStart;                                                                                 \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *bi = (CTYPE)(*ji);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, tBuf, JNI_ABORT);                                                                 \
}                                                                                                                                           \
static inline void parse##JTYPE##2##CTYPE(JNIEnv *aEnv, JTYPE##Array aJArray, CTYPE *rBuf, jsize aLen) {                                    \
    parse##JTYPE##2##CTYPE##V(aEnv, aJArray, 0, rBuf, 0, aLen);                                                                             \
}
#endif

GEN_PARSE_JANY_TO_ANY(jint, int)
GEN_PARSE_JANY_TO_ANY(jint, int64_t)
GEN_PARSE_JANY_TO_ANY(jint, double)
GEN_PARSE_JANY_TO_ANY(jlong, int)
GEN_PARSE_JANY_TO_ANY(jlong, int64_t)
GEN_PARSE_JANY_TO_ANY(jlong, double)
GEN_PARSE_JANY_TO_ANY(jdouble, int)
GEN_PARSE_JANY_TO_ANY(jdouble, int64_t)
GEN_PARSE_JANY_TO_ANY(jdouble, float)

static inline void parsejdouble2doubleV(JNIEnv *aEnv, jdoubleArray aJArray, jsize aJStart, double *rBuf, jsize aBStart, jsize aLen) {
    if (aJArray==NULL || rBuf==NULL) return;
#ifdef __cplusplus
    // jdouble is always double
    aEnv->GetDoubleArrayRegion(aJArray, aJStart, aLen, (rBuf+aBStart));
#else
    // jdouble is always double
    (*aEnv)->GetDoubleArrayRegion(aEnv, aJArray, aJStart, aLen, (rBuf+aBStart));
#endif
}
static inline void parsejdouble2double(JNIEnv *aEnv, jdoubleArray aJArray, double *rBuf, jsize aLen) {
    parsejdouble2doubleV(aEnv, aJArray, 0, rBuf, 0, aLen);
}

static inline void parsejfloat2floatV(JNIEnv *aEnv, jfloatArray aJArray, jsize aJStart, float *rBuf, jsize aBStart, jsize aLen) {
    if (aJArray==NULL || rBuf==NULL) return;
#ifdef __cplusplus
    // jfloat is always float
    aEnv->GetFloatArrayRegion(aJArray, aJStart, aLen, (rBuf+aBStart));
#else
    // jfloat is always float
    (*aEnv)->GetFloatArrayRegion(aEnv, aJArray, aJStart, aLen, (rBuf+aBStart));
#endif
}
static inline void parsejfloat2float(JNIEnv *aEnv, jfloatArray aJArray, float *rBuf, jsize aLen) {
    parsejfloat2floatV(aEnv, aJArray, 0, rBuf, 0, aLen);
}


#undef GEN_PARSE_ANY_TO_JANY_WITH_COUNT
#ifdef __cplusplus
#define GEN_PARSE_ANY_TO_JANY_WITH_COUNT(CTYPE, JTYPE)                                                                                                          \
static inline void parse##CTYPE##2##JTYPE##WithCountV(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const void *aBuf, jsize aBStart, jsize aSize, jsize aCount) {\
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                                    \
    JTYPE *rBuf = (JTYPE *)aEnv->GetPrimitiveArrayCritical(rJArray, NULL);                                                                                      \
    JTYPE *ji = rBuf + aJStart;                                                                                                                                 \
    if (aCount > 1) {                                                                                                                                           \
        CTYPE **bbi = ((CTYPE **)aBuf) + aBStart;                                                                                                               \
        for (jsize i = 0; i < aSize; ++i) {                                                                                                                     \
            CTYPE *bi = *bbi;                                                                                                                                   \
            if (bi == NULL) break;                                                                                                                              \
            for (jsize j = 0; j < aCount; ++j) {                                                                                                                \
                *ji = (JTYPE)(*bi);                                                                                                                             \
                ++ji; ++bi;                                                                                                                                     \
            }                                                                                                                                                   \
            ++bbi;                                                                                                                                              \
        }                                                                                                                                                       \
    } else {                                                                                                                                                    \
        CTYPE *bi = ((CTYPE *)aBuf) + aBStart;                                                                                                                  \
        for (jsize i = 0; i < aSize; ++i) {                                                                                                                     \
            *ji = (JTYPE)(*bi);                                                                                                                                 \
            ++ji; ++bi;                                                                                                                                         \
        }                                                                                                                                                       \
    }                                                                                                                                                           \
    aEnv->ReleasePrimitiveArrayCritical(rJArray, rBuf, 0);                                                                                                      \
}                                                                                                                                                               \
static inline void parse##CTYPE##2##JTYPE##WithCount(JNIEnv *aEnv, JTYPE##Array rJArray, const void *aBuf, jsize aSize, jsize aCount) {                         \
    parse##CTYPE##2##JTYPE##WithCountV(aEnv, rJArray, 0, aBuf, 0, aSize, aCount);                                                                               \
}
#else
#define GEN_PARSE_ANY_TO_JANY_WITH_COUNT(CTYPE, JTYPE)                                                                                                          \
static inline void parse##CTYPE##2##JTYPE##WithCountV(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const void *aBuf, jsize aBStart, jsize aSize, jsize aCount) {\
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                                    \
    JTYPE *rBuf = (JTYPE *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);                                                                             \
    JTYPE *ji = rBuf + aJStart;                                                                                                                                 \
    if (aCount > 1) {                                                                                                                                           \
        CTYPE **bbi = ((CTYPE **)aBuf) + aBStart;                                                                                                               \
        for (jsize i = 0; i < aSize; ++i) {                                                                                                                     \
            CTYPE *bi = *bbi;                                                                                                                                   \
            if (bi == NULL) break;                                                                                                                              \
            for (jsize j = 0; j < aCount; ++j) {                                                                                                                \
                *ji = (JTYPE)(*bi);                                                                                                                             \
                ++ji; ++bi;                                                                                                                                     \
            }                                                                                                                                                   \
            ++bbi;                                                                                                                                              \
        }                                                                                                                                                       \
    } else {                                                                                                                                                    \
        CTYPE *bi = ((CTYPE *)aBuf) + aBStart;                                                                                                                  \
        for (jsize i = 0; i < aSize; ++i) {                                                                                                                     \
            *ji = (JTYPE)(*bi);                                                                                                                                 \
            ++ji; ++bi;                                                                                                                                         \
        }                                                                                                                                                       \
    }                                                                                                                                                           \
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0);                                                                                             \
}                                                                                                                                                               \
static inline void parse##CTYPE##2##JTYPE##WithCount(JNIEnv *aEnv, JTYPE##Array rJArray, const void *aBuf, jsize aSize, jsize aCount) {                         \
    parse##CTYPE##2##JTYPE##WithCountV(aEnv, rJArray, 0, aBuf, 0, aSize, aCount);                                                                               \
}
#endif

GEN_PARSE_ANY_TO_JANY_WITH_COUNT(int, jint)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(double, jint)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(int64_t, jint)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(int, jlong)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(double, jlong)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(int64_t, jlong)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(int, jdouble)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(int64_t, jdouble)

static inline void parsedouble2jdoubleWithCountV(JNIEnv *aEnv, jdoubleArray rJArray, jsize aJStart, const void *aBuf, jsize aBStart, jsize aSize, jsize aCount) {
    if (rJArray==NULL || aBuf==NULL) return;
#ifdef __cplusplus
    if (aCount > 1) {
        jdouble *rBuf = (jdouble *)aEnv->GetPrimitiveArrayCritical(rJArray, NULL);
        jdouble *ji = rBuf + aJStart;
        // now aBuf is double **
        double **bbi = ((double **)aBuf) + aBStart;
        for (jsize i = 0; i < aSize; ++i) {
            double *bi = *bbi;
            if (bi == NULL) break;
            for (jsize j = 0; j < aCount; ++j) {
                *ji = (jdouble)(*bi);
                ++ji; ++bi;
            }
            ++bbi;
        }
        aEnv->ReleasePrimitiveArrayCritical(rJArray, rBuf, 0); // write mode
    } else {
        // jdouble is always double
        aEnv->SetDoubleArrayRegion(rJArray, aJStart, aSize, ((jdouble *)aBuf)+aBStart);
    }
#else
    if (aCount > 1) {
        jdouble *rBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
        jdouble *ji = rBuf + aJStart;
        // now aBuf is double **
        double **bbi = ((double **)aBuf) + aBStart;
        for (jsize i = 0; i < aSize; ++i) {
            double *bi = *bbi;
            if (bi == NULL) break;
            for (jsize j = 0; j < aCount; ++j) {
                *ji = (jdouble)(*bi);
                ++ji; ++bi;
            }
            ++bbi;
        }
        (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
    } else {
        // jdouble is always double
        (*aEnv)->SetDoubleArrayRegion(aEnv, rJArray, aJStart, aSize, ((jdouble *)aBuf)+aBStart);
    }
#endif
}
static inline void parsedouble2jdoubleWithCount(JNIEnv *aEnv, jdoubleArray rJArray, const void *aBuf, jsize aSize, jsize aCount) {
    parsedouble2jdoubleWithCountV(aEnv, rJArray, 0, aBuf, 0, aSize, aCount);
}


#undef GEN_PARSE_NESTED_ANY_TO_JANY
#ifdef __cplusplus
#define GEN_PARSE_NESTED_ANY_TO_JANY(CTYPE, JTYPE)                                                                                          \
static inline void parsenested##CTYPE##2##JTYPE##V(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const CTYPE **aBuf, jsize aBStart, jsize aRowNum, jsize aColNum) {\
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                \
    JTYPE *rBuf = (JTYPE *)aEnv->GetPrimitiveArrayCritical(rJArray, NULL);                                                                  \
    JTYPE *ji = rBuf + aJStart; const CTYPE **bbi = aBuf + aBStart;                                                                         \
    for (jsize i = 0; i < aRowNum; ++i) {                                                                                                   \
        const CTYPE *bi = *bbi;                                                                                                             \
        if (bi == NULL) break;                                                                                                              \
        for (jsize j = 0; j < aColNum; ++j) {                                                                                               \
            *ji = (JTYPE)(*bi);                                                                                                             \
            ++ji; ++bi;                                                                                                                     \
        }                                                                                                                                   \
        ++bbi;                                                                                                                              \
    }                                                                                                                                       \
    aEnv->ReleasePrimitiveArrayCritical(rJArray, rBuf, 0);                                                                                  \
}                                                                                                                                           \
static inline void parsenested##CTYPE##2##JTYPE(JNIEnv *aEnv, JTYPE##Array rJArray, const CTYPE **aBuf, jsize aRowNum, jsize aColNum) {     \
    parsenested##CTYPE##2##JTYPE##V(aEnv, rJArray, 0, aBuf, 0, aRowNum, aColNum);                                                           \
}
#else
#define GEN_PARSE_NESTED_ANY_TO_JANY(CTYPE, JTYPE)                                                                                          \
static inline void parsenested##CTYPE##2##JTYPE##V(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const CTYPE **aBuf, jsize aBStart, jsize aRowNum, jsize aColNum) {\
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                \
    JTYPE *rBuf = (JTYPE *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);                                                         \
    JTYPE *ji = rBuf + aJStart; const CTYPE **bbi = aBuf + aBStart;                                                                         \
    for (jsize i = 0; i < aRowNum; ++i) {                                                                                                   \
        const CTYPE *bi = *bbi;                                                                                                             \
        if (bi == NULL) break;                                                                                                              \
        for (jsize j = 0; j < aColNum; ++j) {                                                                                               \
            *ji = (JTYPE)(*bi);                                                                                                             \
            ++ji; ++bi;                                                                                                                     \
        }                                                                                                                                   \
        ++bbi;                                                                                                                              \
    }                                                                                                                                       \
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0);                                                                         \
}                                                                                                                                           \
static inline void parsenested##CTYPE##2##JTYPE(JNIEnv *aEnv, JTYPE##Array rJArray, const CTYPE **aBuf, jsize aRowNum, jsize aColNum) {     \
    parsenested##CTYPE##2##JTYPE##V(aEnv, rJArray, 0, aBuf, 0, aRowNum, aColNum);                                                           \
}
#endif

GEN_PARSE_NESTED_ANY_TO_JANY(int, jint)
GEN_PARSE_NESTED_ANY_TO_JANY(int64_t, jint)
GEN_PARSE_NESTED_ANY_TO_JANY(double, jint)
GEN_PARSE_NESTED_ANY_TO_JANY(int, jlong)
GEN_PARSE_NESTED_ANY_TO_JANY(int64_t, jlong)
GEN_PARSE_NESTED_ANY_TO_JANY(double, jlong)
GEN_PARSE_NESTED_ANY_TO_JANY(int, jdouble)
GEN_PARSE_NESTED_ANY_TO_JANY(int64_t, jdouble)
GEN_PARSE_NESTED_ANY_TO_JANY(double, jfloat)
GEN_PARSE_NESTED_ANY_TO_JANY(double, jdouble)
GEN_PARSE_NESTED_ANY_TO_JANY(float, jfloat)
GEN_PARSE_NESTED_ANY_TO_JANY(float, jdouble)

#undef GEN_PARSE_JANY_TO_NESTED_ANY
#ifdef __cplusplus
#define GEN_PARSE_JANY_TO_NESTED_ANY(JTYPE, CTYPE)                                                                                          \
static inline void parse##JTYPE##2nested##CTYPE##V(JNIEnv *aEnv, JTYPE##Array aJArray, jsize aJStart, CTYPE **rBuf, jsize aBStart, jsize aRowNum, jsize aColNum) {\
    if (aJArray==NULL || rBuf==NULL) return;                                                                                                \
    JTYPE *aBuf = (JTYPE *)aEnv->GetPrimitiveArrayCritical(aJArray, NULL);                                                                  \
    JTYPE *ji = aBuf + aJStart; CTYPE **bbi = rBuf + aBStart;                                                                               \
    for (jsize i = 0; i < aRowNum; ++i) {                                                                                                   \
        CTYPE *bi = *bbi;                                                                                                                   \
        if (bi == NULL) break;                                                                                                              \
        for (jsize j = 0; j < aColNum; ++j) {                                                                                               \
            *bi = (CTYPE)(*ji);                                                                                                             \
            ++ji; ++bi;                                                                                                                     \
        }                                                                                                                                   \
        ++bbi;                                                                                                                              \
    }                                                                                                                                       \
    aEnv->ReleasePrimitiveArrayCritical(aJArray, aBuf, JNI_ABORT);                                                                          \
}                                                                                                                                           \
static inline void parse##JTYPE##2nested##CTYPE(JNIEnv *aEnv, JTYPE##Array aJArray, CTYPE **rBuf, jsize aRowNum, jsize aColNum) {           \
    parse##JTYPE##2nested##CTYPE##V(aEnv, aJArray, 0, rBuf, 0, aRowNum, aColNum);                                                           \
}
#else
#define GEN_PARSE_JANY_TO_NESTED_ANY(JTYPE, CTYPE)                                                                                          \
static inline void parse##JTYPE##2nested##CTYPE##V(JNIEnv *aEnv, JTYPE##Array aJArray, jsize aJStart, CTYPE **rBuf, jsize aBStart, jsize aRowNum, jsize aColNum) {\
    if (aJArray==NULL || rBuf==NULL) return;                                                                                                \
    JTYPE *aBuf = (JTYPE *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);                                                         \
    JTYPE *ji = aBuf + aJStart; CTYPE **bbi = rBuf + aBStart;                                                                               \
    for (jsize i = 0; i < aRowNum; ++i) {                                                                                                   \
        CTYPE *bi = *bbi;                                                                                                                   \
        if (bi == NULL) break;                                                                                                              \
        for (jsize j = 0; j < aColNum; ++j) {                                                                                               \
            *bi = (CTYPE)(*ji);                                                                                                             \
            ++ji; ++bi;                                                                                                                     \
        }                                                                                                                                   \
        ++bbi;                                                                                                                              \
    }                                                                                                                                       \
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, aBuf, JNI_ABORT);                                                                 \
}                                                                                                                                           \
static inline void parse##JTYPE##2nested##CTYPE(JNIEnv *aEnv, JTYPE##Array aJArray, CTYPE **rBuf, jsize aRowNum, jsize aColNum) {           \
    parse##JTYPE##2nested##CTYPE##V(aEnv, aJArray, 0, rBuf, 0, aRowNum, aColNum);                                                           \
}
#endif

GEN_PARSE_JANY_TO_NESTED_ANY(jint, int)
GEN_PARSE_JANY_TO_NESTED_ANY(jint, int64_t)
GEN_PARSE_JANY_TO_NESTED_ANY(jint, double)
GEN_PARSE_JANY_TO_NESTED_ANY(jlong, int)
GEN_PARSE_JANY_TO_NESTED_ANY(jlong, int64_t)
GEN_PARSE_JANY_TO_NESTED_ANY(jlong, double)
GEN_PARSE_JANY_TO_NESTED_ANY(jdouble, int)
GEN_PARSE_JANY_TO_NESTED_ANY(jdouble, int64_t)
GEN_PARSE_JANY_TO_NESTED_ANY(jfloat, double)
GEN_PARSE_JANY_TO_NESTED_ANY(jdouble, double)
GEN_PARSE_JANY_TO_NESTED_ANY(jfloat, float)
GEN_PARSE_JANY_TO_NESTED_ANY(jdouble, float)

/** string stuffs */
static inline char *parseStr(JNIEnv *aEnv, jstring aStr) {
#ifdef __cplusplus
    const char *tBuf = aEnv->GetStringUTFChars(aStr, NULL);
    char *rStr = STRDUP(tBuf);
    aEnv->ReleaseStringUTFChars(aStr, tBuf);
#else
    const char *tBuf = (*aEnv)->GetStringUTFChars(aEnv, aStr, NULL);
    char *rStr = STRDUP(tBuf);
    (*aEnv)->ReleaseStringUTFChars(aEnv, aStr, tBuf);
#endif
    return rStr;
}
static inline char **parseStrBuf(JNIEnv *aEnv, jobjectArray aStrBuf, int *rLen) {
#ifdef __cplusplus
    jsize tLen = aEnv->GetArrayLength(aStrBuf);
    char **rStrBuf = CALLOC_TP(char*, tLen+1);
    
    for (jsize i = 0; i < tLen; ++i) {
        jstring jc = (jstring)aEnv->GetObjectArrayElement(aStrBuf, i);
        rStrBuf[i] = parseStr(aEnv, jc);
        aEnv->DeleteLocalRef(jc);
    }
#else
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aStrBuf);
    char **rStrBuf = CALLOC_TP(char*, tLen+1);
    
    for (jsize i = 0; i < tLen; ++i) {
        jstring jc = (jstring)(*aEnv)->GetObjectArrayElement(aEnv, aStrBuf, i);
        rStrBuf[i] = parseStr(aEnv, jc);
        (*aEnv)->DeleteLocalRef(aEnv, jc);
    }
#endif
    *rLen = tLen;
    return rStrBuf;
}
static inline void freeStrBuf(char **aStrBuf, int aLen) {
    for(int i = 0; i < aLen; ++i) FREE(aStrBuf[i]);
    FREE(aStrBuf);
}


/** exception stuffs */
static inline void throwExceptionMPI(JNIEnv *aEnv, const char *aErrStr, int aExitCode) {
    const char *tClazzName = "jse/parallel/MPIException";
    const char *tInitSig = "(ILjava/lang/String;)V";
#ifdef __cplusplus
    // find class runtime due to asm
    jclass tClazz = aEnv->FindClass(tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = aEnv->GetMethodID(tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = aEnv->NewStringUTF(aErrStr);
    jthrowable tException = (jthrowable)aEnv->NewObject(tClazz, tInit, aExitCode, tJErrStr);
    aEnv->Throw(tException);
    aEnv->DeleteLocalRef(tException);
    aEnv->DeleteLocalRef(tJErrStr);
    aEnv->DeleteLocalRef(tClazz);
#else
    // find class runtime due to asm
    jclass tClazz = (*aEnv)->FindClass(aEnv, tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = (*aEnv)->GetMethodID(aEnv, tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, aErrStr);
    jthrowable tException = (jthrowable)(*aEnv)->NewObject(aEnv, tClazz, tInit, aExitCode, tJErrStr);
    (*aEnv)->Throw(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tClazz);
#endif
}

static inline void throwExceptionLMP(JNIEnv *aEnv, const char *aErrStr) {
    const char *tClazzName = "jse/lmp/LmpException";
    const char *tInitSig = "(Ljava/lang/String;)V";
#ifdef __cplusplus
    // find class runtime due to asm
    jclass tClazz = aEnv->FindClass(tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = aEnv->GetMethodID(tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = aEnv->NewStringUTF(aErrStr);
    jthrowable tException = (jthrowable)aEnv->NewObject(tClazz, tInit, tJErrStr);
    aEnv->Throw(tException);
    aEnv->DeleteLocalRef(tException);
    aEnv->DeleteLocalRef(tJErrStr);
    aEnv->DeleteLocalRef(tClazz);
#else
    // find class runtime due to asm
    jclass tClazz = (*aEnv)->FindClass(aEnv, tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = (*aEnv)->GetMethodID(aEnv, tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, aErrStr);
    jthrowable tException = (jthrowable)(*aEnv)->NewObject(aEnv, tClazz, tInit, tJErrStr);
    (*aEnv)->Throw(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tClazz);
#endif
}

static inline void throwExceptionTorch(JNIEnv *aEnv, const char *aErrStr) {
    const char *tClazzName = "jse/clib/TorchException";
    const char *tInitSig = "(Ljava/lang/String;)V";
#ifdef __cplusplus
    // find class runtime due to asm
    jclass tClazz = aEnv->FindClass(tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = aEnv->GetMethodID(tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = aEnv->NewStringUTF(aErrStr);
    jthrowable tException = (jthrowable)aEnv->NewObject(tClazz, tInit, tJErrStr);
    aEnv->Throw(tException);
    aEnv->DeleteLocalRef(tException);
    aEnv->DeleteLocalRef(tJErrStr);
    aEnv->DeleteLocalRef(tClazz);
#else
    // find class runtime due to asm
    jclass tClazz = (*aEnv)->FindClass(aEnv, tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = (*aEnv)->GetMethodID(aEnv, tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, aErrStr);
    jthrowable tException = (jthrowable)(*aEnv)->NewObject(aEnv, tClazz, tInit, tJErrStr);
    (*aEnv)->Throw(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tClazz);
#endif
}


#ifdef __cplusplus
}
#endif
#endif

#if defined(__cplusplus) && defined(__CLION_IDE__)
#pragma clang diagnostic pop
#endif
