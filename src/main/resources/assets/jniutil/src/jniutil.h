#ifdef __cplusplus
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
#include <stdarg.h>


/** jarray type stuffs */
#define JTYPE_NULL      (0)
#define JTYPE_BYTE      (1)
#define JTYPE_DOUBLE    (2)
#define JTYPE_BOOLEAN   (3)
#define JTYPE_CHAR      (4)
#define JTYPE_SHORT     (5)
#define JTYPE_INT       (6)
#define JTYPE_LONG      (7)
#define JTYPE_FLOAT     (8)

#ifdef __cplusplus
extern "C" {
#endif

/** jarray to buf stuffs */
inline void parseBuf2JArrayV(JNIEnv *aEnv, jobject rJArray, jsize aJStart, jint aJArrayType, const void *aBuf, jsize aBStart, jsize aLen) {
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
inline void parseBuf2JArray(JNIEnv *aEnv, jobject rJArray, jint aJArrayType, const void *aBuf, jsize aLen) {parseBuf2JArrayV(aEnv, rJArray, 0, aJArrayType, aBuf, 0, aLen);}

inline void parseJArray2BufV(JNIEnv *aEnv, jobject aJArray, jsize aJStart, jint aJArrayType, void *rBuf, jsize aBStart, jsize aLen) {
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
inline void parseJArray2Buf(JNIEnv *aEnv, jobject aJArray, jint aJArrayType, void *rBuf, jsize aLen) {parseJArray2BufV(aEnv, aJArray, 0, aJArrayType, rBuf, 0, aLen);}

inline void *allocBuf(jint aJArrayType, jsize aSize) {
    if (aSize <= 0) return NULL;
    switch (aJArrayType) {
        case JTYPE_BYTE:    {return MALLOCN(jbyte   , aSize);}
        case JTYPE_DOUBLE:  {return MALLOCN(jdouble , aSize);}
        case JTYPE_BOOLEAN: {return MALLOCN(jboolean, aSize);}
        case JTYPE_CHAR:    {return MALLOCN(jchar   , aSize);}
        case JTYPE_SHORT:   {return MALLOCN(jshort  , aSize);}
        case JTYPE_INT:     {return MALLOCN(jint    , aSize);}
        case JTYPE_LONG:    {return MALLOCN(jlong   , aSize);}
        case JTYPE_FLOAT:   {return MALLOCN(jfloat  , aSize);}
        default:            {return NULL;}
    }
}
inline void freeBuf(void *aBuf) {
    if (aBuf != NULL) FREE(aBuf);
}


#ifdef __cplusplus
#define GEN_PARSE_ANY_TO_JANY(CTYPE, JTYPE)                                                                                                 \
inline void parse##CTYPE##2##JTYPE##V(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const CTYPE *aBuf, jsize aBStart, jsize aLen) {    \
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                \
    JTYPE *rBuf = (JTYPE *)aEnv->GetPrimitiveArrayCritical(rJArray, NULL);                                                                  \
    JTYPE *ji = rBuf + aJStart; const CTYPE *bi = aBuf + aBStart;                                                                           \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *ji = (JTYPE)(*bi);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    aEnv->ReleasePrimitiveArrayCritical(rJArray, rBuf, 0);                                                                                  \
}                                                                                                                                           \
inline void parse##CTYPE##2##JTYPE(JNIEnv *aEnv, JTYPE##Array rJArray, const CTYPE *aBuf, jsize aLen) {                                     \
    parse##CTYPE##2##JTYPE##V(aEnv, rJArray, 0, aBuf, 0, aLen);                                                                             \
}
#else
#define GEN_PARSE_ANY_TO_JANY(CTYPE, JTYPE)                                                                                                 \
inline void parse##CTYPE##2##JTYPE##V(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const CTYPE *aBuf, jsize aBStart, jsize aLen) {    \
    if (rJArray==NULL || aBuf==NULL) return;                                                                                                \
    JTYPE *rBuf = (JTYPE *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);                                                         \
    JTYPE *ji = rBuf + aJStart; const CTYPE *bi = aBuf + aBStart;                                                                           \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *ji = (JTYPE)(*bi);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0);                                                                         \
}                                                                                                                                           \
inline void parse##CTYPE##2##JTYPE(JNIEnv *aEnv, JTYPE##Array rJArray, const CTYPE *aBuf, jsize aLen) {                                     \
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

inline void parsedouble2jdoubleV(JNIEnv *aEnv, jdoubleArray rJArray, jsize aJStart, const double *aBuf, jsize aBStart, jsize aLen) {
    if (rJArray==NULL || aBuf==NULL) return;
#ifdef __cplusplus
    // jdouble is always double
    aEnv->SetDoubleArrayRegion(rJArray, aJStart, aLen, (aBuf+aBStart));
#else
    // jdouble is always double
    (*aEnv)->SetDoubleArrayRegion(aEnv, rJArray, aJStart, aLen, (aBuf+aBStart));
#endif
}
inline void parsedouble2jdouble(JNIEnv *aEnv, jdoubleArray rJArray, const double *aBuf, jsize aLen) {parsedouble2jdoubleV(aEnv, rJArray, 0, aBuf, 0, aLen);}


#ifdef __cplusplus
#define GEN_PARSE_JANY_TO_ANY(JTYPE, CTYPE)                                                                                                 \
inline void parse##JTYPE##2##CTYPE##V(JNIEnv *aEnv, JTYPE##Array aJArray, jsize aJStart, CTYPE *rBuf, jsize aBStart, jsize aLen) {          \
    if (aJArray==NULL || rBuf==NULL) return;                                                                                                \
    JTYPE *tBuf = (JTYPE *)aEnv->GetPrimitiveArrayCritical(aJArray, NULL);                                                                  \
    JTYPE *ji = tBuf + aJStart; CTYPE *bi = rBuf + aBStart;                                                                                 \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *bi = (CTYPE)(*ji);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    aEnv->ReleasePrimitiveArrayCritical(aJArray, tBuf, JNI_ABORT);                                                                          \
}                                                                                                                                           \
inline void parse##JTYPE##2##CTYPE(JNIEnv *aEnv, JTYPE##Array aJArray, CTYPE *rBuf, jsize aLen) {                                           \
    parse##JTYPE##2##CTYPE##V(aEnv, aJArray, 0, rBuf, 0, aLen);                                                                             \
}
#else
#define GEN_PARSE_JANY_TO_ANY(JTYPE, CTYPE)                                                                                                 \
inline void parse##JTYPE##2##CTYPE##V(JNIEnv *aEnv, JTYPE##Array aJArray, jsize aJStart, CTYPE *rBuf, jsize aBStart, jsize aLen) {          \
    if (aJArray==NULL || rBuf==NULL) return;                                                                                                \
    JTYPE *tBuf = (JTYPE *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);                                                         \
    JTYPE *ji = tBuf + aJStart; CTYPE *bi = rBuf + aBStart;                                                                                 \
    for (jsize i = 0; i < aLen; ++i) {                                                                                                      \
        *bi = (CTYPE)(*ji);                                                                                                                 \
        ++ji; ++bi;                                                                                                                         \
    }                                                                                                                                       \
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, tBuf, JNI_ABORT);                                                                 \
}                                                                                                                                           \
inline void parse##JTYPE##2##CTYPE(JNIEnv *aEnv, JTYPE##Array aJArray, CTYPE *rBuf, jsize aLen) {                                           \
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

inline void parsejdouble2doubleV(JNIEnv *aEnv, jdoubleArray aJArray, jsize aJStart, double *rBuf, jsize aBStart, jsize aLen) {
    if (aJArray==NULL || rBuf==NULL) return;
#ifdef __cplusplus
    // jdouble is always double
    aEnv->GetDoubleArrayRegion(aJArray, aJStart, aLen, (rBuf+aBStart));
#else
    // jdouble is always double
    (*aEnv)->GetDoubleArrayRegion(aEnv, aJArray, aJStart, aLen, (rBuf+aBStart));
#endif
}
inline void parsejdouble2double(JNIEnv *aEnv, jdoubleArray aJArray, double *rBuf, jsize aLen) {parsejdouble2doubleV(aEnv, aJArray, 0, rBuf, 0, aLen);}


#ifdef __cplusplus
#define GEN_PARSE_ANY_TO_JANY_WITH_COUNT(CTYPE, JTYPE)                                                                                                          \
inline void parse##CTYPE##2##JTYPE##WithCountV(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const void *aBuf, jsize aBStart, jsize aSize, jsize aCount) { \
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
inline void parse##CTYPE##2##JTYPE##WithCount(JNIEnv *aEnv, JTYPE##Array rJArray, const void *aBuf, jsize aSize, jsize aCount) {                                \
    parse##CTYPE##2##JTYPE##WithCountV(aEnv, rJArray, 0, aBuf, 0, aSize, aCount);                                                                               \
}
#else
#define GEN_PARSE_ANY_TO_JANY_WITH_COUNT(CTYPE, JTYPE)                                                                                                          \
inline void parse##CTYPE##2##JTYPE##WithCountV(JNIEnv *aEnv, JTYPE##Array rJArray, jsize aJStart, const void *aBuf, jsize aBStart, jsize aSize, jsize aCount) { \
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
inline void parse##CTYPE##2##JTYPE##WithCount(JNIEnv *aEnv, JTYPE##Array rJArray, const void *aBuf, jsize aSize, jsize aCount) {                                \
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

inline void parsedouble2jdoubleWithCountV(JNIEnv *aEnv, jdoubleArray rJArray, jsize aJStart, const void *aBuf, jsize aBStart, jsize aSize, jsize aCount) {
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
inline void parsedouble2jdoubleWithCount(JNIEnv *aEnv, jdoubleArray rJArray, const void *aBuf, jsize aSize, jsize aCount) {
    parsedouble2jdoubleWithCountV(aEnv, rJArray, 0, aBuf, 0, aSize, aCount);
}


/** string stuffs */
inline char *parseStr(JNIEnv *aEnv, jstring aStr) {
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
inline char **parseStrBuf(JNIEnv *aEnv, jobjectArray aStrBuf, int *rLen) {
#ifdef __cplusplus
    jsize tLen = aEnv->GetArrayLength(aStrBuf);
    char **rStrBuf = CALLOC(char*, tLen+1);
    
    for (jsize i = 0; i < tLen; ++i) {
        jstring jc = (jstring)aEnv->GetObjectArrayElement(aStrBuf, i);
        rStrBuf[i] = parseStr(aEnv, jc);
        aEnv->DeleteLocalRef(jc);
    }
#else
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aStrBuf);
    char **rStrBuf = CALLOC(char*, tLen+1);
    
    for (jsize i = 0; i < tLen; ++i) {
        jstring jc = (jstring)(*aEnv)->GetObjectArrayElement(aEnv, aStrBuf, i);
        rStrBuf[i] = parseStr(aEnv, jc);
        (*aEnv)->DeleteLocalRef(aEnv, jc);
    }
#endif
    *rLen = tLen;
    return rStrBuf;
}
inline void freeStrBuf(char **aStrBuf, int aLen) {
    for(int i = 0; i < aLen; ++i) FREE(aStrBuf[i]);
    FREE(aStrBuf);
}


/** exception stuffs */
inline void throwException(JNIEnv *aEnv, const char *aClazzName, const char *aInitSig, ...) {
#ifdef __cplusplus
    // find class runtime due to asm
    jclass tClazz = aEnv->FindClass(aClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", aClazzName);
        return;
    }
    jmethodID tInit = aEnv->GetMethodID(tClazz, "<init>", aInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", aClazzName, aInitSig);
        return;
    }
    va_list aArgs;
    va_start(aArgs, aInitSig);
    jthrowable tException = (jthrowable)aEnv->NewObjectV(tClazz, tInit, aArgs);
    va_end(aArgs);
    aEnv->Throw(tException);
    aEnv->DeleteLocalRef(tException);
    aEnv->DeleteLocalRef(tClazz);
#else
    // find class runtime due to asm
    jclass tClazz = (*aEnv)->FindClass(aEnv, aClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", aClazzName);
        return;
    }
    jmethodID tInit = (*aEnv)->GetMethodID(aEnv, tClazz, "<init>", aInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", aClazzName, aInitSig);
        return;
    }
    va_list aArgs;
    va_start(aArgs, aInitSig);
    jthrowable tException = (jthrowable)(*aEnv)->NewObjectV(aEnv, tClazz, tInit, aArgs);
    va_end(aArgs);
    (*aEnv)->Throw(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tClazz);
#endif
}


#ifdef __cplusplus
}
#endif
#endif

#ifdef __cplusplus
#pragma clang diagnostic pop
#endif
