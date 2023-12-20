#include <stdlib.h>
#include <string.h>

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


#define GEN_PARSE_ANY_TO_ANY(R, T, TF)                                              \
R *parse##TF##2##R (JNIEnv *aEnv, j##T##Array aJArray) {                            \
    if (aJArray==NULL) return NULL;                                                 \
    jsize tLen = (*aEnv)->GetArrayLength(aEnv, aJArray);                            \
    R *rOutBuf = (R *) malloc(tLen * sizeof(R));                                    \
                                                                                    \
    j##T *tBuf = (*aEnv)->Get##TF##ArrayElements(aEnv, aJArray, NULL);              \
    for (jsize i = 0; i < tLen; ++i) rOutBuf[i] = (R)tBuf[i];                       \
    (*aEnv)->Release##TF##ArrayElements(aEnv, aJArray, tBuf, JNI_ABORT);            \
    return rOutBuf;                                                                 \
}

GEN_PARSE_ANY_TO_ANY(int, double, Double)


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
    freeStrBuf(sArgs, tLen);
    return (intptr_t)tLmpPtr;
}

JNIEXPORT jint JNICALL Java_jtool_lmp_NativeLmp_lammpsVersion_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    return lammps_version((void *)aLmpPtr);
}

JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsFile_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aPath) {
    char *tPath = parseStr(aEnv, aPath);
    lammps_file((void *)aLmpPtr, tPath);
    free(tPath);
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsCommand_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aCmd) {
    char *tCmd = parseStr(aEnv, aCmd);
    lammps_command((void *)aLmpPtr, tCmd);
    free(tCmd);
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsCommandsList_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jobjectArray aCmds) {
    int tLen;
    char **tCmds = parseStrBuf(aEnv, aCmds, &tLen);
    lammps_commands_list((void *)aLmpPtr, tLen, (const char **)tCmds);
    freeStrBuf(tCmds, tLen);
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsCommandsString_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aMultiCmd) {
    char *tMultiCmd = parseStr(aEnv, aMultiCmd);
    lammps_commands_string((void *)aLmpPtr, tMultiCmd);
    free(tMultiCmd);
}
JNIEXPORT jdouble JNICALL Java_jtool_lmp_NativeLmp_lammpsGetNatoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    return lammps_get_natoms((void *)aLmpPtr);
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsExtractBox_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdoubleArray rBox) {
    double rBoxlo[3];
    double rBoxhi[3];
    double rXY, rYZ, rXZ;
    int rPflags[3];
    int rBoxflag[3];
    lammps_extract_box((void *)aLmpPtr, rBoxlo, rBoxhi, &rXY, &rYZ, &rXZ, rPflags, rBoxflag);
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
}
JNIEXPORT jdouble JNICALL Java_jtool_lmp_NativeLmp_lammpsGetThermo_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    double tThermo = lammps_get_thermo((void *)aLmpPtr, tName);
    free(tName);
    return tThermo;
}
JNIEXPORT jint JNICALL Java_jtool_lmp_NativeLmp_lammpsExtractSetting_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    int tSetting = lammps_extract_setting((void *)aLmpPtr, tName);
    free(tName);
    return tSetting;
}
JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsGatherConcat_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aCount, jdoubleArray rData) {
    jdouble *rDataBuf = (*aEnv)->GetDoubleArrayElements(aEnv, rData, NULL);
    char *tName = parseStr(aEnv, aName);
    if (aIsDouble) {
        lammps_gather_concat((void *)aLmpPtr, tName, 1, aCount, rDataBuf);
    } else {
        size_t tLen = (size_t)(aCount * lammps_get_natoms((void *)aLmpPtr));
        int *rIntBuf = malloc(tLen*sizeof(int));
        lammps_gather_concat((void *)aLmpPtr, tName, 0, aCount, rIntBuf);
        for (int i = 0; i < tLen; ++i) rDataBuf[i] = rIntBuf[i];
        free(rIntBuf);
    }
    free(tName);
    (*aEnv)->ReleaseDoubleArrayElements(aEnv, rData, rDataBuf, 0); // write mode
}
JNIEXPORT jint JNICALL Java_jtool_lmp_NativeLmp_lammpsCreateAtoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdoubleArray aID, jdoubleArray aType, jdoubleArray aXYZ, jdoubleArray aVelocities, jdoubleArray aImage, jboolean aShrinkExceed) {
    jsize tN = (*aEnv)->GetArrayLength(aEnv, aID);
    int *tID    = parseDouble2int(aEnv, aID   );
    int *tType  = parseDouble2int(aEnv, aType );
    int *tImage = parseDouble2int(aEnv, aImage);
    jdouble *tXYZ        = aXYZ       ==NULL ? NULL : (*aEnv)->GetDoubleArrayElements(aEnv, aXYZ       , NULL);
    jdouble *tVelocities = aVelocities==NULL ? NULL : (*aEnv)->GetDoubleArrayElements(aEnv, aVelocities, NULL);
    int tOut = lammps_create_atoms((void *)aLmpPtr, tN, tID, tType, tXYZ, tVelocities, tImage, aShrinkExceed);
    if (tID    != NULL) free(tID   );
    if (tType  != NULL) free(tType );
    if (tImage != NULL) free(tImage);
    if (tXYZ        != NULL) (*aEnv)->ReleaseDoubleArrayElements(aEnv, aXYZ       , tXYZ       , JNI_ABORT); // read mode
    if (tVelocities != NULL) (*aEnv)->ReleaseDoubleArrayElements(aEnv, aVelocities, tVelocities, JNI_ABORT); // read mode
    return tOut;
}

JNIEXPORT void JNICALL Java_jtool_lmp_NativeLmp_lammpsClose_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    lammps_close((void *)aLmpPtr);
}

