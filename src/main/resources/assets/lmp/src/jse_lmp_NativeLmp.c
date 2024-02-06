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

#include <assert.h>

#include "lammps/library.h"
#include "jse_lmp_NativeLmp.h"


/** utils */
char *parseStr(JNIEnv *aEnv, jstring aStr) {
    const char *tBuf = (*aEnv)->GetStringUTFChars(aEnv, aStr, NULL);
    char *rStr = STRDUP(tBuf);
    (*aEnv)->ReleaseStringUTFChars(aEnv, aStr, tBuf);
    return rStr;
}
char **parseStrBuf(JNIEnv *aEnv, jobjectArray aStrBuf, int *rLen) {
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aStrBuf);
    char **rStrBuf = CALLOC(char*, tLen+1);
    
    for (jsize i = 0; i < tLen; i++) {
        jstring jc = (jstring)(*aEnv)->GetObjectArrayElement(aEnv, aStrBuf, i);
        rStrBuf[i] = parseStr(aEnv, jc);
        (*aEnv)->DeleteLocalRef(aEnv, jc);
    }
    
    *rLen = tLen;
    return rStrBuf;
}
void freeStrBuf(char **aStrBuf, int aLen) {
    for(int i = 0; i < aLen; i++) FREE(aStrBuf[i]);
    FREE(aStrBuf);
}

#if defined(LAMMPS_BIGBIG)
typedef int64_t intbig;
#else
typedef int intbig;
#endif

void parsedouble2jdouble(JNIEnv *aEnv, jdoubleArray rJArray, jint aSize, const double *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    // jdouble is always double
    (*aEnv)->SetDoubleArrayRegion(aEnv, rJArray, 0, aSize, aBuf);
}
void parseint2jdouble(JNIEnv *aEnv, jdoubleArray rJArray, jint aSize, const int *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jdouble *rBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    for (jint i = 0; i < aSize; ++i) rBuf[i] = (jdouble)aBuf[i];
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseint2jint(JNIEnv *aEnv, jintArray rJArray, jint aSize, const int *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jint *rBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    for (jint i = 0; i < aSize; ++i) rBuf[i] = (jint)aBuf[i];
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}

void parsejdouble2double(JNIEnv *aEnv, jdoubleArray aJArray, jint aSize, double *rBuf) {
    if (aJArray==NULL || rBuf==NULL) return;
    // jdouble is always double
    (*aEnv)->GetDoubleArrayRegion(aEnv, aJArray, 0, aSize, rBuf);
}
void parsejdouble2int(JNIEnv *aEnv, jdoubleArray aJArray, jint aSize, int *rBuf) {
    if (aJArray==NULL || rBuf==NULL) return;
    jdouble *tBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);
    for (jint i = 0; i < aSize; ++i) rBuf[i] = (int)tBuf[i];
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, tBuf, JNI_ABORT); // read mode
}
void parsejint2int(JNIEnv *aEnv, jintArray aJArray, jint aSize, int *rBuf) {
    if (aJArray==NULL || rBuf==NULL) return;
    jint *tBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);
    for (jint i = 0; i < aSize; ++i) rBuf[i] = (int)tBuf[i];
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, tBuf, JNI_ABORT); // read mode
}
void parsejint2intbig(JNIEnv *aEnv, jintArray aJArray, jint aSize, intbig *rBuf) {
    if (aJArray==NULL || rBuf==NULL) return;
    jint *tBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, aJArray, NULL);
    for (jint i = 0; i < aSize; ++i) rBuf[i] = (intbig)tBuf[i];
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, aJArray, tBuf, JNI_ABORT); // read mode
}


void parsedouble2jdoubleWithCount(JNIEnv *aEnv, jdoubleArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    if (aCount > 1) {
        jdouble * rBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
        // now aBuf is double **
        double **tNestedBuf = (double **)aBuf;
        jdouble *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            double *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jdouble)subBuf[j];
                ++it;
            }
        }
        (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
    } else {
        // jdouble is always double
        (*aEnv)->SetDoubleArrayRegion(aEnv, rJArray, 0, aSize, (jdouble *)aBuf);
    }
}
void parseint2jdoubleWithCount(JNIEnv *aEnv, jdoubleArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jdouble * rBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is int **
        int **tNestedBuf = (int **)aBuf;
        jdouble *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            int *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jdouble)subBuf[j];
                ++it;
            }
        }
    } else {
        int *tBuf = (int *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jdouble)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseintbig2jdoubleWithCount(JNIEnv *aEnv, jdoubleArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jdouble * rBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is intbig **
        intbig **tNestedBuf = (intbig **)aBuf;
        jdouble *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            intbig *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jdouble)subBuf[j];
                ++it;
            }
        }
    } else {
        intbig *tBuf = (intbig *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jdouble)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseint64t2jdoubleWithCount(JNIEnv *aEnv, jdoubleArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jdouble * rBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is int64_t **
        int64_t **tNestedBuf = (int64_t **)aBuf;
        jdouble *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            int64_t *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jdouble)subBuf[j];
                ++it;
            }
        }
    } else {
        int64_t *tBuf = (int64_t *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jdouble)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseint2jintWithCount(JNIEnv *aEnv, jintArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jint * rBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is int **
        int **tNestedBuf = (int **)aBuf;
        jint *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            int *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jint)subBuf[j];
                ++it;
            }
        }
    } else {
        int *tBuf = (int *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jint)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parsedouble2jintWithCount(JNIEnv *aEnv, jintArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jint * rBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is double **
        double **tNestedBuf = (double **)aBuf;
        jint *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            double *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jint)subBuf[j];
                ++it;
            }
        }
    } else {
        double *tBuf = (double *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jint)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseintbig2jintWithCount(JNIEnv *aEnv, jintArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jint * rBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is intbig **
        intbig **tNestedBuf = (intbig **)aBuf;
        jint *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            intbig *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jint)subBuf[j];
                ++it;
            }
        }
    } else {
        intbig *tBuf = (intbig *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jint)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseint64t2jintWithCount(JNIEnv *aEnv, jintArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jint * rBuf = (jint *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is intbig **
        int64_t **tNestedBuf = (int64_t **)aBuf;
        jint *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            int64_t *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jint)subBuf[j];
                ++it;
            }
        }
    } else {
        int64_t *tBuf = (int64_t *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jint)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseint2jlongWithCount(JNIEnv *aEnv, jlongArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jlong * rBuf = (jlong *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is int **
        int **tNestedBuf = (int **)aBuf;
        jlong *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            int *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jlong)subBuf[j];
                ++it;
            }
        }
    } else {
        int *tBuf = (int *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jlong)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parsedouble2jlongWithCount(JNIEnv *aEnv, jlongArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jlong * rBuf = (jlong *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is double **
        double **tNestedBuf = (double **)aBuf;
        jlong *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            double *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jlong)subBuf[j];
                ++it;
            }
        }
    } else {
        double *tBuf = (double *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jlong)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseintbig2jlongWithCount(JNIEnv *aEnv, jlongArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jlong * rBuf = (jlong *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is intbig **
        intbig **tNestedBuf = (intbig **)aBuf;
        jlong *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            intbig *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jlong)subBuf[j];
                ++it;
            }
        }
    } else {
        intbig *tBuf = (intbig *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jlong)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}
void parseint64t2jlongWithCount(JNIEnv *aEnv, jlongArray rJArray, jint aSize, jint aCount, const void *aBuf) {
    if (rJArray==NULL || aBuf==NULL) return;
    jlong * rBuf = (jlong *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rJArray, NULL);
    if (aCount > 1) {
        // now aBuf is int64_t **
        int64_t **tNestedBuf = (int64_t **)aBuf;
        jlong *it = rBuf;
        for (jint i = 0; i < aSize; ++i) {
            int64_t *subBuf = tNestedBuf[i];
            if (subBuf == NULL) break;
            for (jint j = 0; j < aCount; ++j) {
                *it = (jlong)subBuf[j];
                ++it;
            }
        }
    } else {
        // Although jlong is always 64 bit, I can not guarantee it is int64_t
        // (which is '__int64' in windows, while 'int64_t' is 'long long'),
        // I can not guarantee it is ok for 'int64_t *' to '__int64 *'
        int64_t *tBuf = (int64_t *)aBuf;
        for (jint i = 0; i < aSize; ++i) rBuf[i] = (jlong)tBuf[i];
    }
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rJArray, rBuf, 0); // write mode
}


#define LMP_MAX_ERROR_STRING 512

void throwException(JNIEnv *aEnv, const char *aErrStr) {
    // find class runtime due to asm
    jclass tLmpErrorClazz = (*aEnv)->FindClass(aEnv, "jse/lmp/NativeLmp$Error");
    if (tLmpErrorClazz == NULL) {
        fprintf(stderr, "Couldn't find jse/lmp/NativeLmp$Error\n");
        return;
    }
    jmethodID tLmpErrorInit = (*aEnv)->GetMethodID(aEnv, tLmpErrorClazz, "<init>", "(Ljava/lang/String;)V");
    if (tLmpErrorInit == NULL) {
        fprintf(stderr, "Couldn't find jse/lmp/NativeLmp$Error.<init>(Ljava/lang/String;)V\n");
        return;
    }
    
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, aErrStr);
    jobject tLmpError = (*aEnv)->NewObject(aEnv, tLmpErrorClazz, tLmpErrorInit, tJErrStr);
    (*aEnv)->Throw(aEnv, tLmpError);
    (*aEnv)->DeleteLocalRef(aEnv, tLmpError);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tLmpErrorClazz);
}
jboolean exceptionCheck(JNIEnv *aEnv, void *aLmpPtr) {
#ifdef LAMMPS_EXCEPTIONS
#ifndef LAMMPS_EXCEPTIONS_NULL_SUPPORT
    if (aLmpPtr == NULL) return JNI_FALSE;
#endif
    if (lammps_has_error(aLmpPtr) == 0) return JNI_FALSE;
    
    char rErrStr[LMP_MAX_ERROR_STRING];
    lammps_get_last_error_message(aLmpPtr, rErrStr, LMP_MAX_ERROR_STRING);
    
    throwException(aEnv, rErrStr);
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}
#ifdef LAMMPS_LIB_MPI
jboolean exceptionCheckMPI(JNIEnv *aEnv, int aExitCode) {
    if (aExitCode == MPI_SUCCESS) return JNI_FALSE;
    
    // find class runtime due to asm
    jclass tMPIErrorClazz = (*aEnv)->FindClass(aEnv, "jse/parallel/MPI$Error");
    if (tMPIErrorClazz == NULL) {
        fprintf(stderr, "Couldn't find jse/parallel/MPI$Error\n");
        return JNI_TRUE;
    }
    jmethodID tMPIErrorInit = (*aEnv)->GetMethodID(aEnv, tMPIErrorClazz, "<init>", "(ILjava/lang/String;)V");
    if (tMPIErrorInit == NULL) {
        fprintf(stderr, "Couldn't find jse/parallel/MPI$Error.<init>(ILjava/lang/String;)V\n");
        return JNI_TRUE;
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
    return JNI_TRUE;
}
#endif



JNIEXPORT jlong JNICALL Java_jse_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2J(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs, jlong aComm) {
    int tLen;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
#ifdef LAMMPS_LIB_MPI
    void *tLmpPtr;
    int tMpiInit;
    MPI_Initialized(&tMpiInit);
    if (tMpiInit) {
        MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
#ifdef LAMMPS_OLD
        lammps_open(tLen, sArgs, tComm, &tLmpPtr);
#else
        tLmpPtr = lammps_open(tLen, sArgs, tComm, NULL);
#endif
    } else {
#ifdef LAMMPS_OLD
        lammps_open_no_mpi(tLen, sArgs, &tLmpPtr);
#else
        tLmpPtr = lammps_open_no_mpi(tLen, sArgs, NULL);
#endif
    }
#else
#ifdef LAMMPS_OLD
    void *tLmpPtr;
    lammps_open_no_mpi(tLen, sArgs, &tLmpPtr);
#else
    void *tLmpPtr = lammps_open_no_mpi(tLen, sArgs, NULL);
#endif
#endif
    exceptionCheck(aEnv, tLmpPtr);
    freeStrBuf(sArgs, tLen);
    return (intptr_t)tLmpPtr;
}

JNIEXPORT jlong JNICALL Java_jse_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs) {
    int tLen;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
#ifdef LAMMPS_LIB_MPI
    void *tLmpPtr;
    int tMpiInit;
    MPI_Initialized(&tMpiInit);
    if (tMpiInit) {
#ifdef LAMMPS_OLD
        lammps_open(tLen, sArgs, MPI_COMM_WORLD, &tLmpPtr);
#else
        tLmpPtr = lammps_open(tLen, sArgs, MPI_COMM_WORLD, NULL);
#endif
    } else {
#ifdef LAMMPS_OLD
        lammps_open_no_mpi(tLen, sArgs, &tLmpPtr);
#else
        tLmpPtr = lammps_open_no_mpi(tLen, sArgs, NULL);
#endif
    }
#else
#ifdef LAMMPS_OLD
    void *tLmpPtr;
    lammps_open_no_mpi(tLen, sArgs, &tLmpPtr);
#else
    void *tLmpPtr = lammps_open_no_mpi(tLen, sArgs, NULL);
#endif
#endif
    exceptionCheck(aEnv, tLmpPtr);
    freeStrBuf(sArgs, tLen);
    return (intptr_t)tLmpPtr;
}

JNIEXPORT jint JNICALL Java_jse_lmp_NativeLmp_lammpsVersion_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    return lammps_version((void *)aLmpPtr);
}

JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsFile_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aPath) {
    char *tPath = parseStr(aEnv, aPath);
    lammps_file((void *)aLmpPtr, tPath);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tPath);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCommand_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aCmd) {
    char *tCmd = parseStr(aEnv, aCmd);
    lammps_command((void *)aLmpPtr, tCmd);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tCmd);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCommandsList_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jobjectArray aCmds) {
    int tLen;
    char **tCmds = parseStrBuf(aEnv, aCmds, &tLen);
#ifdef LAMMPS_OLD
    lammps_commands_list((void *)aLmpPtr, tLen, tCmds);
#else
    lammps_commands_list((void *)aLmpPtr, tLen, (const char **)tCmds);
#endif
    exceptionCheck(aEnv, (void *)aLmpPtr);
    freeStrBuf(tCmds, tLen);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCommandsString_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aMultiCmd) {
    char *tMultiCmd = parseStr(aEnv, aMultiCmd);
    lammps_commands_string((void *)aLmpPtr, tMultiCmd);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tMultiCmd);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_NativeLmp_lammpsGetNatoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    double tAtomNum = lammps_get_natoms((void *)aLmpPtr);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    return tAtomNum;
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractBox_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdoubleArray rBox) {
    double rBoxlo[3];
    double rBoxhi[3];
    double rXY, rYZ, rXZ;
    int rPflags[3];
    int rBoxflag[3];
    lammps_extract_box((void *)aLmpPtr, rBoxlo, rBoxhi, &rXY, &rYZ, &rXZ, rPflags, rBoxflag);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    jdouble *rBoxBuf = (jdouble *)(*aEnv)->GetPrimitiveArrayCritical(aEnv, rBox, NULL);
    rBoxBuf[0 ] = rBoxlo[0];
    rBoxBuf[1 ] = rBoxlo[1];
    rBoxBuf[2 ] = rBoxlo[2];
    rBoxBuf[3 ] = rBoxhi[0];
    rBoxBuf[4 ] = rBoxhi[1];
    rBoxBuf[5 ] = rBoxhi[2];
    rBoxBuf[6 ] = rXY;
    rBoxBuf[7 ] = rYZ;
    rBoxBuf[8 ] = rXZ;
    rBoxBuf[9 ] = rPflags[0];
    rBoxBuf[10] = rPflags[1];
    rBoxBuf[11] = rPflags[2];
    rBoxBuf[12] = rBoxflag[0];
    rBoxBuf[13] = rBoxflag[1];
    rBoxBuf[14] = rBoxflag[2];
    (*aEnv)->ReleasePrimitiveArrayCritical(aEnv, rBox, rBoxBuf, 0); // write mode
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsResetBox_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdouble aXlo, jdouble aYlo, jdouble aZlo, jdouble aXhi, jdouble aYhi, jdouble aZhi, jdouble aXY, jdouble aYZ, jdouble aXZ) {
    double tBoxLo[] = {aXlo, aYlo, aZlo};
    double tBoxHi[] = {aXhi, aYhi, aZhi};
    lammps_reset_box((void *)aLmpPtr, tBoxLo, tBoxHi, aXY, aYZ, aXZ);
    exceptionCheck(aEnv, (void *)aLmpPtr);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_NativeLmp_lammpsGetThermo_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    double tThermo = lammps_get_thermo((void *)aLmpPtr, tName);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tName);
    return tThermo;
}
JNIEXPORT jint JNICALL Java_jse_lmp_NativeLmp_lammpsExtractSetting_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    int tSetting = lammps_extract_setting((void *)aLmpPtr, tName);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tName);
    return tSetting;
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsGatherConcat_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aCount, jdoubleArray rData) {
    // The implementation of `lammps_gather_concat` is just a piece of shit which actually causes memory leakage,
    // so I can only implement it myself, while also providing support for non MPI.
#ifdef LAMMPS_BIGBIG
    throwException(aEnv, "Library function lammps_gather_concat() is not compatible with -DLAMMPS_BIGBIG");
#endif
    void *tLmpPtr = (void *)aLmpPtr;
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom(tLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheck(aEnv, tLmpPtr);
    FREE(tName);
    if (tHasException || tRef==NULL) return;
    int tLocalAtomNum = lammps_extract_setting(tLmpPtr, "nlocal"); if (exceptionCheck(aEnv, tLmpPtr)) return;
#ifdef LAMMPS_LIB_MPI
    // init MPI_Comm stuffs
    MPI_Comm tLmpComm = (MPI_Comm)lammps_get_mpi_comm(tLmpPtr);
    int tLmpMe, tLmpNP;
    int tExitCodeMPI;
    tExitCodeMPI = MPI_Comm_rank(tLmpComm, &tLmpMe); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    tExitCodeMPI = MPI_Comm_size(tLmpComm, &tLmpNP); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    // allgather counts (tLocalAtomNum * aCount)
    int *tCounts = MALLOCN(int, tLmpNP);
    tCounts[tLmpMe] = tLocalAtomNum * aCount;
    tExitCodeMPI = MPI_Allgather(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, tCounts, 1, MPI_INT, tLmpComm);
    if (exceptionCheckMPI(aEnv, tExitCodeMPI)) {FREE(tCounts); return;}
    // cal displs
    int *tDispls = MALLOCN(int, tLmpNP);
    tDispls[0] = 0;
    for (int i = 1; i < tLmpNP; ++i) {
        tDispls[i] = tDispls[i-1] + tCounts[i-1];
    }
    // gather atom data by allgatherv
    int tDataSize = tDispls[tLmpNP-1] + tCounts[tLmpNP-1];
    if (aIsDouble) {
        double *rDataBuf = MALLOCN(double, tDataSize);
        if (aCount > 1) {
            double **tDoubleRef = (double **)tRef;
            double *it = rDataBuf + tDispls[tLmpMe];
            for (int i = 0; i < tLocalAtomNum; ++i) {
                double *subDoubleRef = tDoubleRef[i];
                if (subDoubleRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = subDoubleRef[j];
                    ++it;
                }
            }
        } else {
            double *tDoubleRef = (double *)tRef;
            double *it = rDataBuf + tDispls[tLmpMe];
            for (int i = 0; i < tLocalAtomNum; ++i) {
                *it = tDoubleRef[i];
                ++it;
            }
        }
        tExitCodeMPI = MPI_Allgatherv(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, rDataBuf, tCounts, tDispls, MPI_DOUBLE, tLmpComm);
        exceptionCheckMPI(aEnv, tExitCodeMPI);
        parsedouble2jdouble(aEnv, rData, tDataSize, rDataBuf);
        FREE(rDataBuf);
    } else {
        int *rDataBuf = MALLOCN(int, tDataSize);
        if (aCount > 1) {
            int **tIntRef = (int **)tRef;
            int *it = rDataBuf + tDispls[tLmpMe];
            for (int i = 0; i < tLocalAtomNum; ++i) {
                int *subIntRef = tIntRef[i];
                if (subIntRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = subIntRef[j];
                    ++it;
                }
            }
        } else {
            int *tIntRef = (int *)tRef;
            int *it = rDataBuf + tDispls[tLmpMe];
            for (int i = 0; i < tLocalAtomNum; ++i) {
                *it = tIntRef[i];
                ++it;
            }
        }
        // NO need to free due to it is lammps internal data
        tExitCodeMPI = MPI_Allgatherv(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, rDataBuf, tCounts, tDispls, MPI_INT, tLmpComm);
        exceptionCheckMPI(aEnv, tExitCodeMPI);
        parseint2jdouble(aEnv, rData, tDataSize, rDataBuf);
        FREE(rDataBuf);
    }
    FREE(tCounts);
    FREE(tDispls);
#else
    if (aIsDouble) {
        parsedouble2jdoubleWithCount(aEnv, rData, tLocalAtomNum, aCount, tRef);
    } else {
        parseint2jdoubleWithCount(aEnv, rData, tLocalAtomNum, aCount, tRef);
    }
#endif
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsGatherConcatInt_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aCount, jintArray rData) {
    // The implementation of `lammps_gather_concat` is just a piece of shit which actually causes memory leakage,
    // so I can only implement it myself, while also providing support for non MPI.
#ifdef LAMMPS_BIGBIG
    throwException(aEnv, "Library function lammps_gather_concat() is not compatible with -DLAMMPS_BIGBIG");
#endif
    void *tLmpPtr = (void *)aLmpPtr;
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom(tLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheck(aEnv, tLmpPtr);
    FREE(tName);
    if (tHasException || tRef==NULL) return;
    int tLocalAtomNum = lammps_extract_setting(tLmpPtr, "nlocal"); if (exceptionCheck(aEnv, tLmpPtr)) return;
#ifdef LAMMPS_LIB_MPI
    // init MPI_Comm stuffs
    MPI_Comm tLmpComm = (MPI_Comm)lammps_get_mpi_comm(tLmpPtr);
    int tLmpMe, tLmpNP;
    int tExitCodeMPI;
    tExitCodeMPI = MPI_Comm_rank(tLmpComm, &tLmpMe); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    tExitCodeMPI = MPI_Comm_size(tLmpComm, &tLmpNP); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    // allgather counts (tLocalAtomNum * aCount)
    int *tCounts = MALLOCN(int, tLmpNP);
    tCounts[tLmpMe] = tLocalAtomNum * aCount;
    tExitCodeMPI = MPI_Allgather(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, tCounts, 1, MPI_INT, tLmpComm);
    if (exceptionCheckMPI(aEnv, tExitCodeMPI)) {FREE(tCounts); return;}
    // cal displs
    int *tDispls = MALLOCN(int, tLmpNP);
    tDispls[0] = 0;
    for (int i = 1; i < tLmpNP; ++i) {
        tDispls[i] = tDispls[i-1] + tCounts[i-1];
    }
    // gather atom data by allgatherv
    int tDataSize = tDispls[tLmpNP-1] + tCounts[tLmpNP-1];
    int *rDataBuf = MALLOCN(int, tDataSize);
    if (aCount > 1) {
        int **tIntRef = (int **)tRef;
        int *it = rDataBuf + tDispls[tLmpMe];
        for (int i = 0; i < tLocalAtomNum; ++i) {
            int *subIntRef = tIntRef[i];
            if (subIntRef == NULL) break;
            for (int j = 0; j < aCount; ++j) {
                *it = subIntRef[j];
                ++it;
            }
        }
    } else {
        int *tIntRef = (int *)tRef;
        int *it = rDataBuf + tDispls[tLmpMe];
        for (int i = 0; i < tLocalAtomNum; ++i) {
            *it = tIntRef[i];
            ++it;
        }
    }
    tExitCodeMPI = MPI_Allgatherv(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, rDataBuf, tCounts, tDispls, MPI_INT, tLmpComm);
    exceptionCheckMPI(aEnv, tExitCodeMPI);
    FREE(tCounts);
    FREE(tDispls);
    parseint2jint(aEnv, rData, tDataSize, rDataBuf);
    FREE(rDataBuf);
#else
    parseint2jintWithCount(aEnv, rData, tLocalAtomNum, aCount, tRef);
#endif
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractAtom_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jdoubleArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)aLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tName);
    if (tHasException || tRef==NULL) return;
    switch (aDataType) {
    case 0: {
        parseint2jdoubleWithCount(aEnv, rData, aAtomNum, aCount, tRef);
        return;
    }
    case 1: {
        parsedouble2jdoubleWithCount(aEnv, rData, aAtomNum, aCount, tRef);
        return;
    }
    case 2: {
        parseintbig2jdoubleWithCount(aEnv, rData, aAtomNum, aCount, tRef);
        return;
    }
    case 3: {
        parseint64t2jdoubleWithCount(aEnv, rData, aAtomNum, aCount, tRef);
        return;
    }
    default: {
        throwException(aEnv, "DataType of lammpsExtractAtom can ONLY be 0, 1, 2 or 3");
        return;
    }}
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractAtomInt_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jintArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)aLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tName);
    if (tHasException || tRef==NULL) return;
    switch (aDataType) {
        case 0: {
            parseint2jintWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        case 1: {
            parsedouble2jintWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        case 2: {
            parseintbig2jintWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        case 3: {
            parseint64t2jintWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        default: {
            throwException(aEnv, "DataType of lammpsExtractAtomInt can ONLY be 0, 1, 2 or 3");
            return;
        }}
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractAtomLong_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jlongArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)aLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tName);
    if (tHasException || tRef==NULL) return;
    switch (aDataType) {
        case 0: {
            parseint2jlongWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        case 1: {
            parsedouble2jlongWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        case 2: {
            parseintbig2jlongWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        case 3: {
            parseint64t2jlongWithCount(aEnv, rData, aAtomNum, aCount, tRef);
            return;
        }
        default: {
            throwException(aEnv, "DataType of lammpsExtractAtomLong can ONLY be 0, 1, 2 or 3");
            return;
        }}
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsScatter_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aAtomNum, jint aCount, jdoubleArray aData) {
    void *tDataBuf;
    if (aIsDouble) {
        jint tLen = aCount * aAtomNum;
        tDataBuf = MALLOCN(double, tLen);
        parsejdouble2double(aEnv, aData, tLen, (double *)tDataBuf);
    } else {
        jint tLen = aCount * aAtomNum;
        tDataBuf = MALLOCN(int, tLen);
        parsejdouble2int(aEnv, aData, tLen, (int *)tDataBuf);
    }
    char *tName = parseStr(aEnv, aName);
#ifdef LAMMPS_OLD
    lammps_scatter_atoms((void *)aLmpPtr, tName, 1, aCount, tDataBuf);
#else
    lammps_scatter((void *)aLmpPtr, tName, 1, aCount, tDataBuf);
#endif
    exceptionCheck(aEnv, (void *)aLmpPtr);
    FREE(tName);
    if (tDataBuf != NULL) FREE(tDataBuf);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCreateAtoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jint aAtomNum, jintArray aID, jintArray aType, jdoubleArray aXYZ, jdoubleArray aVel, jintArray aImage, jboolean aShrinkExceed) {
    jsize tN = (*aEnv)->GetArrayLength(aEnv, aID);
    jint tAtomNum3 = aAtomNum * 3;
    intbig  *tID    = aID   ==NULL ? NULL : MALLOCN(intbig , aAtomNum ); parsejint2intbig   (aEnv, aID   , aAtomNum , tID   );
    int     *tType  = aType ==NULL ? NULL : MALLOCN(int    , aAtomNum ); parsejint2int      (aEnv, aType , aAtomNum , tType );
    intbig  *tImage = aImage==NULL ? NULL : MALLOCN(intbig , aAtomNum ); parsejint2intbig   (aEnv, aImage, aAtomNum , tImage);
    jdouble *tXYZ   = aXYZ  ==NULL ? NULL : MALLOCN(jdouble, tAtomNum3); parsejdouble2double(aEnv, aXYZ  , tAtomNum3, tXYZ  );
    jdouble *tVel   = aVel  ==NULL ? NULL : MALLOCN(jdouble, tAtomNum3); parsejdouble2double(aEnv, aVel  , tAtomNum3, tVel  );
    lammps_create_atoms((void *)aLmpPtr, tN, tID, tType, tXYZ, tVel, tImage, aShrinkExceed);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    if (tID    != NULL) FREE(tID   );
    if (tType  != NULL) FREE(tType );
    if (tImage != NULL) FREE(tImage);
    if (tXYZ   != NULL) FREE(tXYZ  );
    if (tVel   != NULL) FREE(tVel  );
}

JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsClose_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    lammps_close((void *)aLmpPtr);
    exceptionCheck(aEnv, NULL);
}

