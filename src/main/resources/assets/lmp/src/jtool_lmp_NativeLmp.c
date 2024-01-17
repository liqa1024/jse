#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "lammps/library.h"
#include "jtool_lmp_NativeLmp.h"


/** utils */
char *parseStr(JNIEnv *aEnv, jstring aStr) {
    const char *tBuf = (*aEnv)->GetStringUTFChars(aEnv, aStr, NULL);
#ifdef WIN32
    char *rStr = _strdup(tBuf);
#elif _WIN64
    char *rStr = _strdup(tBuf);
#elif _WIN32
    char *rStr = _strdup(tBuf);
#elif __unix__
    char *rStr = strdup(tBuf);
#elif __linux__
    char *rStr = strdup(tBuf);
#endif
    (*aEnv)->ReleaseStringUTFChars(aEnv, aStr, tBuf);
    return rStr;
}
char **parseStrBuf(JNIEnv *aEnv, jobjectArray aStrBuf, int *rLen) {
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aStrBuf);
    char **rStrBuf = (char**)calloc(tLen+1, sizeof(char*));
    
    for (jsize i = 0; i < tLen; i++) {
        jstring jc = (jstring)(*aEnv)->GetObjectArrayElement(aEnv, aStrBuf, i);
        rStrBuf[i] = parseStr(aEnv, jc);
        (*aEnv)->DeleteLocalRef(aEnv, jc);
    }
    
    *rLen = tLen;
    return rStrBuf;
}
void freeStrBuf(char **aStrBuf, int aLen) {
    for(int i = 0; i < aLen; i++) free(aStrBuf[i]);
    free(aStrBuf);
}

#if defined(LAMMPS_BIGBIG)
typedef int64_t intbig;
#else
typedef int intbig;
#endif

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

GEN_PARSE_JANY_TO_ANY(int, double, Double)
GEN_PARSE_JANY_TO_ANY(intbig, double, Double)


#define LMP_MAX_ERROR_STRING 512

void throwException(JNIEnv *aEnv, const char *aErrStr) {
    // find class runtime due to asm
    jclass tLmpErrorClazz = (*aEnv)->FindClass(aEnv, "jtool/lmp/NativeLmp$Error");
    if (tLmpErrorClazz == NULL) {
        fprintf(stderr, "Couldn't find jtool/lmp/NativeLmp$Error\n");
        return;
    }
    jmethodID tLmpErrorInit = (*aEnv)->GetMethodID(aEnv, tLmpErrorClazz, "<init>", "(Ljava/lang/String;)V");
    if (tLmpErrorInit == NULL) {
        fprintf(stderr, "Couldn't find jtool/lmp/NativeLmp$Error.<init>(Ljava/lang/String;)V\n");
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
    jclass tMPIErrorClazz = (*aEnv)->FindClass(aEnv, "jtool/parallel/MPI$Error");
    if (tMPIErrorClazz == NULL) {
        fprintf(stderr, "Couldn't find jtool/parallel/MPI$Error\n");
        return JNI_TRUE;
    }
    jmethodID tMPIErrorInit = (*aEnv)->GetMethodID(aEnv, tMPIErrorClazz, "<init>", "(ILjava/lang/String;)V");
    if (tMPIErrorInit == NULL) {
        fprintf(stderr, "Couldn't find jtool/parallel/MPI$Error.<init>(ILjava/lang/String;)V\n");
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



JNIEXPORT jlong JNICALL Java_jtool_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2J(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs, jlong aComm) {
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

JNIEXPORT jlong JNICALL Java_jtool_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs) {
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

JNIEXPORT jint JNICALL Java_jtool_lmp_NativeLmp_lammpsVersion_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    return lammps_version((void *)aLmpPtr);
}

JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsFile_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aPath) {
    char *tPath = parseStr(aEnv, aPath);
    lammps_file((void *)aLmpPtr, tPath);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    free(tPath);
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsCommand_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aCmd) {
    char *tCmd = parseStr(aEnv, aCmd);
    lammps_command((void *)aLmpPtr, tCmd);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    free(tCmd);
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsCommandsList_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jobjectArray aCmds) {
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
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsCommandsString_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aMultiCmd) {
    char *tMultiCmd = parseStr(aEnv, aMultiCmd);
    lammps_commands_string((void *)aLmpPtr, tMultiCmd);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    free(tMultiCmd);
}
JNIEXPORT jdouble JNICALL Java_jtool_lmp_NativeLmp_lammpsGetNatoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    double tAtomNum = lammps_get_natoms((void *)aLmpPtr);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    return tAtomNum;
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsExtractBox_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdoubleArray rBox) {
    double rBoxlo[3];
    double rBoxhi[3];
    double rXY, rYZ, rXZ;
    int rPflags[3];
    int rBoxflag[3];
    lammps_extract_box((void *)aLmpPtr, rBoxlo, rBoxhi, &rXY, &rYZ, &rXZ, rPflags, rBoxflag);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    jdouble *rBoxBuf = (*aEnv)->GetDoubleArrayElements(aEnv, rBox, NULL);
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
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, rBox, rBoxBuf, 0); // write mode
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsResetBox_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdouble aXlo, jdouble aYlo, jdouble aZlo, jdouble aXhi, jdouble aYhi, jdouble aZhi, jdouble aXY, jdouble aYZ, jdouble aXZ) {
    double tBoxLo[] = {aXlo, aYlo, aZlo};
    double tBoxHi[] = {aXhi, aYhi, aZhi};
    lammps_reset_box((void *)aLmpPtr, tBoxLo, tBoxHi, aXY, aYZ, aXZ);
    exceptionCheck(aEnv, (void *)aLmpPtr);
}
JNIEXPORT jdouble JNICALL Java_jtool_lmp_NativeLmp_lammpsGetThermo_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    double tThermo = lammps_get_thermo((void *)aLmpPtr, tName);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    free(tName);
    return tThermo;
}
JNIEXPORT jint JNICALL Java_jtool_lmp_NativeLmp_lammpsExtractSetting_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    int tSetting = lammps_extract_setting((void *)aLmpPtr, tName);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    free(tName);
    return tSetting;
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsGatherConcat_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aCount, jdoubleArray rData) {
    // The implementation of `lammps_gather_concat` is just a piece of shit which actually causes memory leakage,
    // so I can only implement it myself, while also providing support for non MPI.
#ifdef LAMMPS_BIGBIG
    throwException(aEnv, "Library function lammps_gather_concat() is not compatible with -DLAMMPS_BIGBIG");
#endif
    void *tLmpPtr = (void *)aLmpPtr;
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom(tLmpPtr, tName);
    jboolean tHasException = exceptionCheck(aEnv, tLmpPtr);
    free(tName);
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
    int *tCounts = malloc(tLmpNP * sizeof(int));
    tCounts[tLmpMe] = tLocalAtomNum * aCount;
    tExitCodeMPI = MPI_Allgather(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, tCounts, 1, MPI_INT, tLmpComm);
    if (exceptionCheckMPI(aEnv, tExitCodeMPI)) {free(tCounts); return;}
    // cal displs
    int *tDispls = malloc(tLmpNP * sizeof(int));
    tDispls[0] = 0;
    for (int i = 1; i < tLmpNP; ++i) {
        tDispls[i] = tDispls[i-1] + tCounts[i-1];
    }
    // gather atom data by allgatherv
    jdouble *rDataBuf = (*aEnv)->GetDoubleArrayElements(aEnv, rData, NULL);
    if (aIsDouble) {
        if (aCount > 1) {
            double **tDoubleRef = (double **)tRef;
            jdouble *it = rDataBuf + tDispls[tLmpMe];
            for (int i = 0; i < tLocalAtomNum; ++i) {
                double *subDoubleRef = tDoubleRef[i];
                if (subDoubleRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = (jdouble)subDoubleRef[j];
                    ++it;
                }
            }
        } else {
            double *tDoubleRef = (double *)tRef;
            jdouble *it = rDataBuf + tDispls[tLmpMe];
            for (int i = 0; i < tLocalAtomNum; ++i) {
                *it = (jdouble)tDoubleRef[i];
                ++it;
            }
        }
        // NO need to free due to it is lammps internal data
        tExitCodeMPI = MPI_Allgatherv(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, rDataBuf, tCounts, tDispls, MPI_DOUBLE, tLmpComm);
        exceptionCheckMPI(aEnv, tExitCodeMPI);
    } else {
        int tDataSize = tDispls[tLmpNP-1] + tCounts[tLmpNP-1];
        int *rIntDataBuf = malloc(tDataSize * sizeof(int));
        if (aCount > 1) {
            int **tIntRef = (int **)tRef;
            int *it = rIntDataBuf + tDispls[tLmpMe];
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
            int *it = rIntDataBuf + tDispls[tLmpMe];
            for (int i = 0; i < tLocalAtomNum; ++i) {
                *it = tIntRef[i];
                ++it;
            }
        }
        // NO need to free due to it is lammps internal data
        tExitCodeMPI = MPI_Allgatherv(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, rIntDataBuf, tCounts, tDispls, MPI_INT, tLmpComm);
        exceptionCheckMPI(aEnv, tExitCodeMPI);
        for (int i = 0; i < tDataSize; ++i) rDataBuf[i] = (jdouble)rIntDataBuf[i];
        free(rIntDataBuf);
    }
    free(tCounts);
    free(tDispls);
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, rData, rDataBuf, 0); // write mode
#else
    jdouble *rDataBuf = (*aEnv)->GetDoubleArrayElements(aEnv, rData, NULL);
    if (aIsDouble) {
        if (aCount > 1) {
            double **tDoubleRef = (double **)tRef;
            jdouble *it = rDataBuf;
            for (int i = 0; i < tLocalAtomNum; ++i) {
                double *subDoubleRef = tDoubleRef[i];
                if (subDoubleRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = (jdouble)subDoubleRef[j];
                    ++it;
                }
            }
        } else {
            double *tDoubleRef = (double *)tRef;
            for (int i = 0; i < tLocalAtomNum; ++i) rDataBuf[i] = (jdouble)tDoubleRef[i];
        }
        // NO need to free due to it is lammps internal data
    } else {
        if (aCount > 1) {
            int **tIntRef = (int **)tRef;
            jdouble *it = rDataBuf;
            for (int i = 0; i < tLocalAtomNum; ++i) {
                int *subIntRef = tIntRef[i];
                if (subIntRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = (jdouble)subIntRef[j];
                    ++it;
                }
            }
        } else {
            int *tIntRef = (int *)tRef;
            for (int i = 0; i < tLocalAtomNum; ++i) rDataBuf[i] = (jdouble)tIntRef[i];
        }
        // NO need to free due to it is lammps internal data
    }
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, rData, rDataBuf, 0); // write mode
#endif
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsExtractAtom_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jdoubleArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)aLmpPtr, tName);
    jboolean tHasException = exceptionCheck(aEnv, (void *)aLmpPtr);
    free(tName);
    if (tHasException || tRef==NULL) return;
    jdouble *rDataBuf = (*aEnv)->GetDoubleArrayElements(aEnv, rData, NULL);
    switch (aDataType) {
    case 0: {
        if (aCount > 1) {
            int **tIntRef = (int **)tRef;
            jdouble *it = rDataBuf;
            for (int i = 0; i < aAtomNum; ++i) {
                int *subIntRef = tIntRef[i];
                if (subIntRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = (jdouble)subIntRef[j];
                    ++it;
                }
            }
        } else {
            int *tIntRef = (int *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (jdouble)tIntRef[i];
        }
        // NO need to free due to it is lammps internal data
        break;
    }
    case 1: {
        if (aCount > 1) {
            double **tDoubleRef = (double **)tRef;
            jdouble *it = rDataBuf;
            for (int i = 0; i < aAtomNum; ++i) {
                double *subDoubleRef = tDoubleRef[i];
                if (subDoubleRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = (jdouble)subDoubleRef[j];
                    ++it;
                }
            }
        } else {
            double *tDoubleRef = (double *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (jdouble)tDoubleRef[i];
        }
        // NO need to free due to it is lammps internal data
        break;
    }
    case 2: {
        if (aCount > 1) {
            intbig **tIntbigRef = (intbig **)tRef;
            jdouble *it = rDataBuf;
            for (int i = 0; i < aAtomNum; ++i) {
                intbig *subIntbigRef = tIntbigRef[i];
                if (subIntbigRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = (jdouble)subIntbigRef[j];
                    ++it;
                }
            }
        } else {
            intbig *tIntbigRef = (intbig *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (jdouble)tIntbigRef[i];
        }
        // NO need to free due to it is lammps internal data
        break;
    }
    case 3: {
        if (aCount > 1) {
            int64_t **tInt64Ref = (int64_t **)tRef;
            jdouble *it = rDataBuf;
            for (int i = 0; i < aAtomNum; ++i) {
                int64_t *subInt64Ref = tInt64Ref[i];
                if (subInt64Ref == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    *it = (jdouble)subInt64Ref[j];
                    ++it;
                }
            }
        } else {
            int64_t *tInt64Ref = (int64_t *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (jdouble)tInt64Ref[i];
        }
        // NO need to free due to it is lammps internal data
        break;
    }
    default: {
        (*aEnv)->ReleaseDoubleArrayElements(aEnv, rData, rDataBuf, JNI_ABORT);
        assert(0); return;
    }}
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, rData, rDataBuf, 0); // write mode
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsScatter_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aAtomNum, jint aCount, jdoubleArray aData) {
    jdouble *tDataBuf = (*aEnv)->GetDoubleArrayElements(aEnv, aData, NULL);
    char *tName = parseStr(aEnv, aName);
    if (aIsDouble) {
#ifdef LAMMPS_OLD
        lammps_scatter_atoms((void *)aLmpPtr, tName, 1, aCount, tDataBuf);
#else
        lammps_scatter((void *)aLmpPtr, tName, 1, aCount, tDataBuf);
#endif
        exceptionCheck(aEnv, (void *)aLmpPtr);
    } else {
        jint tLen = aCount * aAtomNum;
        int *tIntBuf = malloc(tLen*sizeof(int));
        for (int i = 0; i < tLen; ++i) tIntBuf[i] = (int)tDataBuf[i];
#ifdef LAMMPS_OLD
        lammps_scatter_atoms((void *)aLmpPtr, tName, 0, aCount, tIntBuf);
#else
        lammps_scatter((void *)aLmpPtr, tName, 1, aCount, tDataBuf);
#endif
        exceptionCheck(aEnv, (void *)aLmpPtr);
        free(tIntBuf);
    }
    free(tName);
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, aData, tDataBuf, JNI_ABORT); // read mode
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsCreateAtoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdoubleArray aID, jdoubleArray aType, jdoubleArray aXYZ, jdoubleArray aVelocities, jdoubleArray aImage, jboolean aShrinkExceed) {
    jsize tN = (*aEnv)->GetArrayLength(aEnv, aID);
    intbig *tID    = parseJdouble2intbig(aEnv, aID   );
    int    *tType  = parseJdouble2int   (aEnv, aType );
    intbig *tImage = parseJdouble2intbig(aEnv, aImage);
    jdouble *tXYZ        = aXYZ       ==NULL ? NULL : (*aEnv)->GetDoubleArrayElements(aEnv, aXYZ       , NULL);
    jdouble *tVelocities = aVelocities==NULL ? NULL : (*aEnv)->GetDoubleArrayElements(aEnv, aVelocities, NULL);
    lammps_create_atoms((void *)aLmpPtr, tN, tID, tType, tXYZ, tVelocities, tImage, aShrinkExceed);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    if (tID    != NULL) free(tID   );
    if (tType  != NULL) free(tType );
    if (tImage != NULL) free(tImage);
    if (tXYZ        != NULL) (*aEnv)->ReleaseDoubleArrayElements(aEnv, aXYZ       , tXYZ       , JNI_ABORT); // read mode
    if (tVelocities != NULL) (*aEnv)->ReleaseDoubleArrayElements(aEnv, aVelocities, tVelocities, JNI_ABORT); // read mode
}

JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsClose_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    lammps_close((void *)aLmpPtr);
    exceptionCheck(aEnv, NULL);
}

