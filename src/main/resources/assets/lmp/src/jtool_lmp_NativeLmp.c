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

void exceptionCheck(JNIEnv *aEnv, void *aLmpPtr) {
    if (!lammps_has_error(aLmpPtr)) return;
    
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
    
    char rErrStr[LMP_MAX_ERROR_STRING];
    lammps_get_last_error_message(aLmpPtr, rErrStr, LMP_MAX_ERROR_STRING);
    
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, (const char*)rErrStr);
    jobject tLmpError = (*aEnv)->NewObject(aEnv, tLmpErrorClazz, tLmpErrorInit, tJErrStr);
    (*aEnv)->Throw(aEnv, tLmpError);
    (*aEnv)->DeleteLocalRef(aEnv, tLmpError);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
}


JNIEXPORT jlong JNICALL Java_jtool_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2JJ(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs, jlong aComm, jlong aPtr) {
    int tLen;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
#if defined(LAMMPS_LIB_MPI)
    void *tLmpPtr;
    int tMpiInit;
    MPI_Initialized(&tMpiInit);
    if (tMpiInit) {
        MPI_Comm tComm = (MPI_Comm) (intptr_t) aComm;
        tLmpPtr = lammps_open(tLen, sArgs, tComm, (void *)aPtr);
    } else {
        tLmpPtr = lammps_open_no_mpi(tLen, sArgs, (void *)aPtr);
    }
#else
    void *tLmpPtr = lammps_open_no_mpi(tLen, sArgs, (void *)aPtr);
#endif
    exceptionCheck(aEnv, tLmpPtr);
    freeStrBuf(sArgs, tLen);
    return (intptr_t)tLmpPtr;
}

JNIEXPORT jlong JNICALL Java_jtool_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2J(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs, jlong aPtr) {
    int tLen;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
#if defined(LAMMPS_LIB_MPI)
    void *tLmpPtr;
    int tMpiInit;
    MPI_Initialized(&tMpiInit);
    if (tMpiInit) {
        tLmpPtr = lammps_open(tLen, sArgs, MPI_COMM_WORLD, (void *)aPtr);
    } else {
        tLmpPtr = lammps_open_no_mpi(tLen, sArgs, (void *)aPtr);
    }
#else
    void *tLmpPtr = lammps_open_no_mpi(tLen, sArgs, (void *)aPtr);
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
    lammps_commands_list((void *)aLmpPtr, tLen, (const char **)tCmds);
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
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsGatherConcat_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aAtomNum, jint aCount, jdoubleArray rData) {
    jdouble *rDataBuf = (*aEnv)->GetDoubleArrayElements(aEnv, rData, NULL);
    char *tName = parseStr(aEnv, aName);
    if (aIsDouble) {
        lammps_gather_concat((void *)aLmpPtr, tName, 1, aCount, rDataBuf);
        exceptionCheck(aEnv, (void *)aLmpPtr);
    } else {
        jint tLen = aCount * aAtomNum;
        int *rIntBuf = malloc(tLen*sizeof(int));
        lammps_gather_concat((void *)aLmpPtr, tName, 0, aCount, rIntBuf);
        exceptionCheck(aEnv, (void *)aLmpPtr);
        for (int i = 0; i < tLen; ++i) rDataBuf[i] = rIntBuf[i];
        free(rIntBuf);
    }
    free(tName);
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, rData, rDataBuf, 0); // write mode
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsExtractAtom_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jdoubleArray rData) {
    jdouble *rDataBuf = (*aEnv)->GetDoubleArrayElements(aEnv, rData, NULL);
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)aLmpPtr, tName);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    free(tName);
    if (tRef == NULL) {
        (*aEnv)->ReleaseDoubleArrayElements(aEnv, rData, rDataBuf, JNI_ABORT); return;
    }
    switch (aDataType) {
    case 0: {
        if (aCount > 1) {
            int **tIntRef = (int **)tRef;
            int idx = 0;
            for (int i = 0; i < aAtomNum; ++i) {
                int *subIntRef = tIntRef[i];
                if (subIntRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    rDataBuf[idx] = (double)subIntRef[j];
                    ++idx;
                }
            }
        } else {
            int *tIntRef = (int *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (double)tIntRef[i];
        }
        // NO need to free due to it is lammps internal data
        break;
    }
    case 1: {
        if (aCount > 1) {
            double **tDoubleRef = (double **)tRef;
            int idx = 0;
            for (int i = 0; i < aAtomNum; ++i) {
                double *subDoubleRef = tDoubleRef[i];
                if (subDoubleRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    rDataBuf[idx] = (double)subDoubleRef[j];
                    ++idx;
                }
            }
        } else {
            double *tDoubleRef = (double *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (double)tDoubleRef[i];
        }
        // NO need to free due to it is lammps internal data
        break;
    }
    case 2: {
        if (aCount > 1) {
            intbig **tIntbigRef = (intbig **)tRef;
            int idx = 0;
            for (int i = 0; i < aAtomNum; ++i) {
                intbig *subIntbigRef = tIntbigRef[i];
                if (subIntbigRef == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    rDataBuf[idx] = (double)subIntbigRef[j];
                    ++idx;
                }
            }
        } else {
            intbig *tIntbigRef = (intbig *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (double)tIntbigRef[i];
        }
        // NO need to free due to it is lammps internal data
        break;
    }
    case 3: {
        if (aCount > 1) {
            int64_t **tInt64Ref = (int64_t **)tRef;
            int idx = 0;
            for (int i = 0; i < aAtomNum; ++i) {
                int64_t *subInt64Ref = tInt64Ref[i];
                if (subInt64Ref == NULL) break;
                for (int j = 0; j < aCount; ++j) {
                    rDataBuf[idx] = (double)subInt64Ref[j]; ++idx;
                }
            }
        } else {
            int64_t *tInt64Ref = (int64_t *)tRef;
            for (int i = 0; i < aAtomNum; ++i) rDataBuf[i] = (double)tInt64Ref[i];
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
        lammps_scatter((void *)aLmpPtr, tName, 1, aCount, tDataBuf);
        exceptionCheck(aEnv, (void *)aLmpPtr);
    } else {
        jint tLen = aCount * aAtomNum;
        int *tIntBuf = malloc(tLen*sizeof(int));
        for (int i = 0; i < tLen; ++i) tIntBuf[i] = (int)tDataBuf[i];
        lammps_scatter((void *)aLmpPtr, tName, 0, aCount, tIntBuf);
        exceptionCheck(aEnv, (void *)aLmpPtr);
        free(tIntBuf);
    }
    free(tName);
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, aData, tDataBuf, JNI_ABORT); // read mode
}
JNIEXPORT jint JNICALL Java_jtool_lmp_NativeLmp_lammpsCreateAtoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdoubleArray aID, jdoubleArray aType, jdoubleArray aXYZ, jdoubleArray aVelocities, jdoubleArray aImage, jboolean aShrinkExceed) {
    jsize tN = (*aEnv)->GetArrayLength(aEnv, aID);
    intbig *tID    = parseJdouble2intbig(aEnv, aID   );
    int    *tType  = parseJdouble2int   (aEnv, aType );
    intbig *tImage = parseJdouble2intbig(aEnv, aImage);
    jdouble *tXYZ        = aXYZ       ==NULL ? NULL : (*aEnv)->GetDoubleArrayElements(aEnv, aXYZ       , NULL);
    jdouble *tVelocities = aVelocities==NULL ? NULL : (*aEnv)->GetDoubleArrayElements(aEnv, aVelocities, NULL);
    int tOut = lammps_create_atoms((void *)aLmpPtr, tN, tID, tType, tXYZ, tVelocities, tImage, aShrinkExceed);
    exceptionCheck(aEnv, (void *)aLmpPtr);
    if (tID    != NULL) free(tID   );
    if (tType  != NULL) free(tType );
    if (tImage != NULL) free(tImage);
    if (tXYZ        != NULL) (*aEnv)->ReleaseDoubleArrayElements(aEnv, aXYZ       , tXYZ       , JNI_ABORT); // read mode
    if (tVelocities != NULL) (*aEnv)->ReleaseDoubleArrayElements(aEnv, aVelocities, tVelocities, JNI_ABORT); // read mode
    return tOut;
}

JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsClose_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    lammps_close((void *)aLmpPtr);
    exceptionCheck(aEnv, (void *)aLmpPtr);
}

