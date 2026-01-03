#include "lammps/library.h"
#include "lmpjni_util.hpp"
#include "jse_lmp_NativeLmp.h"
#include "jse_lmp_NativeLmpPointer.h"

extern "C" {

/** utils for lmp */
#ifdef LAMMPS_BIGBIG
typedef int64_t intbig;
#else
typedef int intbig;
#endif

GEN_PARSE_JANY_TO_ANY(jint, intbig)

GEN_PARSE_ANY_TO_JANY_WITH_COUNT(intbig, jint)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(intbig, jlong)
GEN_PARSE_ANY_TO_JANY_WITH_COUNT(intbig, jdouble)


#define LMP_MAX_ERROR_STRING (512)

static inline jboolean exceptionCheckLMP(JNIEnv *aEnv, void *aLmpPtr) {
#ifdef LAMMPS_EXCEPTIONS
#ifndef LAMMPS_EXCEPTIONS_NULL_SUPPORT
    if (aLmpPtr == NULL) return JNI_FALSE;
#endif
    if (lammps_has_error(aLmpPtr) == 0) return JNI_FALSE;
    
    char rErrStr[LMP_MAX_ERROR_STRING];
    lammps_get_last_error_message(aLmpPtr, rErrStr, LMP_MAX_ERROR_STRING);
    
    throwExceptionLMP(aEnv, rErrStr);
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}
#ifdef LAMMPS_LIB_MPI
static inline jboolean exceptionCheckMPI(JNIEnv *aEnv, int aExitCode) {
    if (aExitCode == MPI_SUCCESS) return JNI_FALSE;
    
    char rErrStr[MPI_MAX_ERROR_STRING];
    int rLen;
    MPI_Error_string(aExitCode, rErrStr, &rLen);
    
    throwExceptionMPI(aEnv, rErrStr, aExitCode);
    return JNI_TRUE;
}

static inline jlong lammpsGetMpiComm(void *aLmpPtr) {
#ifdef LAMMPS_OLD
    throwExceptionLMP(aEnv, "Cannot access `lammps_get_mpi_comm` when LAMMPS_IS_OLD");
    return 0;
#else
    return (jlong)MPI_Comm_f2c(lammps_get_mpi_comm(aLmpPtr));
#endif
}
#endif



JNIEXPORT jlong JNICALL Java_jse_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2J(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs, jlong aComm) {
    // Now, if no LAMMPS_LIB_MPI, input aComm will simply throw an exception,
    // because in theory it should have been thrown at the time of the mpijni initialization
    // in jse and should not have arrived here.
#ifndef LAMMPS_LIB_MPI
    throwExceptionLMP(aEnv, "Input an MPI_Comm when NO LAMMPS_LIB_MPI");
    return 0;
#else
    int tLen;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
    void *tLmpPtr;
    MPI_Comm tComm = (MPI_Comm)(intptr_t)aComm;
#ifdef LAMMPS_OLD
    lammps_open(tLen, sArgs, tComm, &tLmpPtr);
#else
    tLmpPtr = lammps_open(tLen, sArgs, tComm, NULL);
#endif
    exceptionCheckLMP(aEnv, tLmpPtr);
    freeStrBuf(sArgs, tLen);
    return (jlong)(intptr_t)tLmpPtr;
#endif
}

JNIEXPORT jlong JNICALL Java_jse_lmp_NativeLmp_lammpsOpen_1___3Ljava_lang_String_2(JNIEnv *aEnv, jclass aClazz, jobjectArray aArgs) {
    int tLen;
    char **sArgs = parseStrBuf(aEnv, aArgs, &tLen);
    void *tLmpPtr;
#ifdef LAMMPS_OLD
    lammps_open_no_mpi(tLen, sArgs, &tLmpPtr);
#else
    tLmpPtr = lammps_open_no_mpi(tLen, sArgs, NULL);
#endif
    exceptionCheckLMP(aEnv, tLmpPtr);
    freeStrBuf(sArgs, tLen);
    return (jlong)(intptr_t)tLmpPtr;
}

JNIEXPORT jboolean JNICALL Java_jse_lmp_NativeLmp_lammpsHasStyle_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aCategory, jstring aName) {
#ifdef LAMMPS_OLD
    throwExceptionLMP(aEnv, "Cannot access `lammps_has_style` when LAMMPS_IS_OLD");
    return JNI_FALSE;
#else
    char *tCategory = parseStr(aEnv, aCategory);
    char *tName = parseStr(aEnv, aName);
    int tOut = lammps_has_style((void *)(intptr_t)aLmpPtr, tCategory, tName);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    FREE(tCategory);
    return tOut ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jint JNICALL Java_jse_lmp_NativeLmp_lammpsVersion_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    int tOut = lammps_version((void *)(intptr_t)aLmpPtr);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    return (jint)tOut;
}
JNIEXPORT jstring JNICALL Java_jse_lmp_NativeLmp_lammpsVersionStr_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    char *tVersionStr = (char *)lammps_extract_global((void *)(intptr_t)aLmpPtr, "lammps_version");
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    return tVersionStr!=NULL ? aEnv->NewStringUTF(tVersionStr) : NULL;
}

JNIEXPORT jlong JNICALL Java_jse_lmp_NativeLmp_lammpsComm_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    jlong tComm = lammpsGetMpiComm((void *)(intptr_t)aLmpPtr);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    return tComm;
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_NativeLmp_lammpsLibMpi_1(JNIEnv *aEnv, jclass aClazz) {
#ifdef LAMMPS_LIB_MPI
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_NativeLmp_lammpsBigbig_1(JNIEnv *aEnv, jclass aClazz) {
#ifdef LAMMPS_BIGBIG
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsMpiFinalize_1(JNIEnv *aEnv, jclass aClazz) {
#ifdef LAMMPS_OLD
    throwExceptionLMP(aEnv, "Function `lammpsMpiFinalize` is INVALID when LAMMPS_IS_OLD");
#else
    lammps_mpi_finalize();
#endif
}

JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsFile_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aPath) {
    char *tPath = parseStr(aEnv, aPath);
    lammps_file((void *)(intptr_t)aLmpPtr, tPath);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tPath);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsInputFile_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    // i will use C++ interface here
#ifdef LAMMPS_OLD
    throwExceptionLMP(aEnv, "Never try to access C++ interface when LAMMPS_IS_OLD");
#else
    JSE_LMPJNI::lammpsInputFile(aEnv, (void *)(intptr_t)aLmpPtr);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
#endif
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCommand_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aCmd) {
    char *tCmd = parseStr(aEnv, aCmd);
    lammps_command((void *)(intptr_t)aLmpPtr, tCmd);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tCmd);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCommandsList_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jobjectArray aCmds) {
    int tLen;
    char **tCmds = parseStrBuf(aEnv, aCmds, &tLen);
#ifdef LAMMPS_OLD
    lammps_commands_list((void *)(intptr_t)aLmpPtr, tLen, tCmds);
#else
    lammps_commands_list((void *)(intptr_t)aLmpPtr, tLen, (const char **)tCmds);
#endif
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    freeStrBuf(tCmds, tLen);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCommandsString_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aMultiCmd) {
    char *tMultiCmd = parseStr(aEnv, aMultiCmd);
    lammps_commands_string((void *)(intptr_t)aLmpPtr, tMultiCmd);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tMultiCmd);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_NativeLmp_lammpsGetNatoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    double tAtomNum = lammps_get_natoms((void *)(intptr_t)aLmpPtr);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    return tAtomNum;
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractBox_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdoubleArray rBox) {
    double rBoxlo[3];
    double rBoxhi[3];
    double rXY, rYZ, rXZ;
    int rPflags[3];
    int rBoxflag[3];
    lammps_extract_box((void *)(intptr_t)aLmpPtr, rBoxlo, rBoxhi, &rXY, &rYZ, &rXZ, rPflags, rBoxflag);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    jdouble *rBoxBuf = (jdouble *)getJArrayBuf(aEnv, rBox);
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
    releaseJArrayBuf(aEnv, rBox, rBoxBuf, 0); // write mode
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsResetBox_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jdouble aXlo, jdouble aYlo, jdouble aZlo, jdouble aXhi, jdouble aYhi, jdouble aZhi, jdouble aXY, jdouble aYZ, jdouble aXZ) {
    double tBoxLo[] = {aXlo, aYlo, aZlo};
    double tBoxHi[] = {aXhi, aYhi, aZhi};
    lammps_reset_box((void *)(intptr_t)aLmpPtr, tBoxLo, tBoxHi, aXY, aYZ, aXZ);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_NativeLmp_lammpsGetThermo_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    double tThermo = lammps_get_thermo((void *)(intptr_t)aLmpPtr, tName);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    return tThermo;
}
JNIEXPORT jint JNICALL Java_jse_lmp_NativeLmp_lammpsExtractSetting_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    int tSetting = lammps_extract_setting((void *)(intptr_t)aLmpPtr, tName);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    return (jint)tSetting;
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsGatherConcat_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aCount, jdoubleArray rData) {
    // The implementation of `lammps_gather_concat` is just a piece of shit which actually causes memory leakage,
    // so I can only implement it myself, while also providing support for non MPI.
#ifdef LAMMPS_BIGBIG
    throwExceptionLMP(aEnv, "Library function lammps_gather_concat() is not compatible with -DLAMMPS_BIGBIG");
#else
    void *tLmpPtr = (void *)(intptr_t)aLmpPtr;
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom(tLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, tLmpPtr);
    FREE(tName);
    if (tHasException) return;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract atom"); return;}
    int tLocalAtomNum = lammps_extract_setting(tLmpPtr, "nlocal"); if (exceptionCheckLMP(aEnv, tLmpPtr)) return;
#ifdef LAMMPS_LIB_MPI
    // init MPI_Comm stuffs
    jlong tLmpComm_ = lammpsGetMpiComm(tLmpPtr);
    if (tLmpComm_ == 0) return;
    MPI_Comm tLmpComm = (MPI_Comm)(intptr_t)tLmpComm_;
    int tLmpMe, tLmpNP;
    int tExitCodeMPI;
    tExitCodeMPI = MPI_Comm_rank(tLmpComm, &tLmpMe); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    tExitCodeMPI = MPI_Comm_size(tLmpComm, &tLmpNP); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    // allgather counts (tLocalAtomNum * aCount)
    int *tCounts = MALLOCN_TP(int, tLmpNP);
    tCounts[tLmpMe] = tLocalAtomNum * aCount;
    tExitCodeMPI = MPI_Allgather(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, tCounts, 1, MPI_INT, tLmpComm);
    if (exceptionCheckMPI(aEnv, tExitCodeMPI)) {FREE(tCounts); return;}
    // cal displs
    int *tDispls = MALLOCN_TP(int, tLmpNP);
    tDispls[0] = 0;
    for (int i = 1; i < tLmpNP; ++i) {
        tDispls[i] = tDispls[i-1] + tCounts[i-1];
    }
    // gather atom data by allgatherv
    int tDataSize = tDispls[tLmpNP-1] + tCounts[tLmpNP-1];
    if (aIsDouble) {
        double *rDataBuf = MALLOCN_TP(double, tDataSize);
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
        parsedouble2jdouble(aEnv, rData, rDataBuf, tDataSize);
        FREE(rDataBuf);
    } else {
        int *rDataBuf = MALLOCN_TP(int, tDataSize);
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
        parseint2jdouble(aEnv, rData, rDataBuf, tDataSize);
        FREE(rDataBuf);
    }
    FREE(tCounts);
    FREE(tDispls);
#else
    if (aIsDouble) {
        parsedouble2jdoubleWithCount(aEnv, rData, tRef, tLocalAtomNum, aCount);
    } else {
        parseint2jdoubleWithCount(aEnv, rData, tRef, tLocalAtomNum, aCount);
    }
#endif
#endif
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsGatherConcatInt_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aCount, jintArray rData) {
    // The implementation of `lammps_gather_concat` is just a piece of shit which actually causes memory leakage,
    // so I can only implement it myself, while also providing support for non MPI.
#ifdef LAMMPS_BIGBIG
    throwExceptionLMP(aEnv, "Library function lammps_gather_concat() is not compatible with -DLAMMPS_BIGBIG");
#else
    void *tLmpPtr = (void *)(intptr_t)aLmpPtr;
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom(tLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, tLmpPtr);
    FREE(tName);
    if (tHasException) return;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract atom"); return;}
    int tLocalAtomNum = lammps_extract_setting(tLmpPtr, "nlocal"); if (exceptionCheckLMP(aEnv, tLmpPtr)) return;
#ifdef LAMMPS_LIB_MPI
    // init MPI_Comm stuffs
    jlong tLmpComm_ = lammpsGetMpiComm(tLmpPtr);
    if (tLmpComm_ == 0) return;
    MPI_Comm tLmpComm = (MPI_Comm)(intptr_t)tLmpComm_;
    int tLmpMe, tLmpNP;
    int tExitCodeMPI;
    tExitCodeMPI = MPI_Comm_rank(tLmpComm, &tLmpMe); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    tExitCodeMPI = MPI_Comm_size(tLmpComm, &tLmpNP); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    // allgather counts (tLocalAtomNum * aCount)
    int *tCounts = MALLOCN_TP(int, tLmpNP);
    tCounts[tLmpMe] = tLocalAtomNum * aCount;
    tExitCodeMPI = MPI_Allgather(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, tCounts, 1, MPI_INT, tLmpComm);
    if (exceptionCheckMPI(aEnv, tExitCodeMPI)) {FREE(tCounts); return;}
    // cal displs
    int *tDispls = MALLOCN_TP(int, tLmpNP);
    tDispls[0] = 0;
    for (int i = 1; i < tLmpNP; ++i) {
        tDispls[i] = tDispls[i-1] + tCounts[i-1];
    }
    // gather atom data by allgatherv
    int tDataSize = tDispls[tLmpNP-1] + tCounts[tLmpNP-1];
    int *rDataBuf = MALLOCN_TP(int, tDataSize);
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
    parseint2jint(aEnv, rData, rDataBuf, tDataSize);
    FREE(rDataBuf);
#else
    parseint2jintWithCount(aEnv, rData, tRef, tLocalAtomNum, aCount);
#endif
#endif
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractAtom_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jdoubleArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)(intptr_t)aLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tHasException) return;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract atom"); return;}
    switch (aDataType) {
    case 0: {
        parseint2jdoubleWithCount(aEnv, rData, tRef, aAtomNum, aCount);
        return;
    }
    case 1: {
        parsedouble2jdoubleWithCount(aEnv, rData, tRef, aAtomNum, aCount);
        return;
    }
    case 2: {
        parseintbig2jdoubleWithCount(aEnv, rData, tRef, aAtomNum, aCount);
        return;
    }
    case 3: {
        parseint64_t2jdoubleWithCount(aEnv, rData, tRef, aAtomNum, aCount);
        return;
    }
    default: {
        throwExceptionLMP(aEnv, "DataType of lammpsExtractAtom can ONLY be 0, 1, 2 or 3");
        return;
    }}
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractAtomInt_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jintArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)(intptr_t)aLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tHasException) return;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract atom"); return;}
    switch (aDataType) {
        case 0: {
            parseint2jintWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        case 1: {
            parsedouble2jintWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        case 2: {
            parseintbig2jintWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        case 3: {
            parseint64_t2jintWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        default: {
            throwExceptionLMP(aEnv, "DataType of lammpsExtractAtomInt can ONLY be 0, 1, 2 or 3");
            return;
        }}
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractAtomLong_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataType, jint aAtomNum, jint aCount, jlongArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)(intptr_t)aLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tHasException) return;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract atom"); return;}
    switch (aDataType) {
        case 0: {
            parseint2jlongWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        case 1: {
            parsedouble2jlongWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        case 2: {
            parseintbig2jlongWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        case 3: {
            parseint64_t2jlongWithCount(aEnv, rData, tRef, aAtomNum, aCount);
            return;
        }
        default: {
            throwExceptionLMP(aEnv, "DataType of lammpsExtractAtomLong can ONLY be 0, 1, 2 or 3");
            return;
        }}
}
JNIEXPORT jlong JNICALL Java_jse_lmp_NativeLmp_lammpsExtractAtomCPointer_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_atom((void *)(intptr_t)aLmpPtr, tName); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tHasException) return 0;
    return (jlong)(intptr_t)tRef;
}

JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsGatherCompute_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aColNum, jdoubleArray rData) {
    void *tLmpPtr = (void *)(intptr_t)aLmpPtr;
    char *tName = parseStr(aEnv, aName);
    int aCount = aColNum==0?1:aColNum;
    void *tRef = lammps_extract_compute(tLmpPtr, tName, (int)jse_lmp_NativeLmp_LMP_STYLE_ATOM, aColNum==0?(int)jse_lmp_NativeLmp_LMP_TYPE_VECTOR:(int)jse_lmp_NativeLmp_LMP_TYPE_ARRAY); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, tLmpPtr);
    FREE(tName);
    if (tHasException) return;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract compute"); return;}
    int tLocalAtomNum = lammps_extract_setting(tLmpPtr, "nlocal"); if (exceptionCheckLMP(aEnv, tLmpPtr)) return;
#ifdef LAMMPS_LIB_MPI
    // init MPI_Comm stuffs
    jlong tLmpComm_ = lammpsGetMpiComm(tLmpPtr);
    if (tLmpComm_ == 0) return;
    MPI_Comm tLmpComm = (MPI_Comm)(intptr_t)tLmpComm_;
    int tLmpMe, tLmpNP;
    int tExitCodeMPI;
    tExitCodeMPI = MPI_Comm_rank(tLmpComm, &tLmpMe); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    tExitCodeMPI = MPI_Comm_size(tLmpComm, &tLmpNP); if (exceptionCheckMPI(aEnv, tExitCodeMPI)) return;
    // allgather counts (tLocalAtomNum * aCount)
    int *tCounts = MALLOCN_TP(int, tLmpNP);
    tCounts[tLmpMe] = tLocalAtomNum * aCount;
    tExitCodeMPI = MPI_Allgather(MPI_IN_PLACE, 0, MPI_DATATYPE_NULL, tCounts, 1, MPI_INT, tLmpComm);
    if (exceptionCheckMPI(aEnv, tExitCodeMPI)) {FREE(tCounts); return;}
    // cal displs
    int *tDispls = MALLOCN_TP(int, tLmpNP);
    tDispls[0] = 0;
    for (int i = 1; i < tLmpNP; ++i) {
        tDispls[i] = tDispls[i-1] + tCounts[i-1];
    }
    // gather atom data by allgatherv
    int tDataSize = tDispls[tLmpNP-1] + tCounts[tLmpNP-1];
    double *rDataBuf = MALLOCN_TP(double, tDataSize);
    if (aColNum > 0) {
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
    parsedouble2jdouble(aEnv, rData, rDataBuf, tDataSize);
    FREE(rDataBuf);
    FREE(tCounts);
    FREE(tDispls);
#else
    parsedouble2jdoubleWithCount(aEnv, rData, tRef, tLocalAtomNum, aCount);
#endif
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsExtractCompute_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataStyle, jint aDataType, jint aRowNum, jint aColNum, jdoubleArray rData) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_compute((void *)(intptr_t)aLmpPtr, tName, (int)aDataStyle, (int)aDataType); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tHasException) return;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract compute"); return;}
    parsedouble2jdoubleWithCount(aEnv, rData, tRef, aRowNum, aColNum);
}
JNIEXPORT jint JNICALL Java_jse_lmp_NativeLmp_lammpsExtractComputeSize_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataStyle, jint aDataType) {
    char *tName = parseStr(aEnv, aName);
    int *tRef = (int *)lammps_extract_compute((void *)(intptr_t)aLmpPtr, tName, (int)aDataStyle, (int)aDataType); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tHasException) return -1;
    if (tRef == NULL) {throwExceptionLMP(aEnv, "Fail to extract compute"); return -1;}
    return (jint)*tRef;
}
JNIEXPORT jlong JNICALL Java_jse_lmp_NativeLmp_lammpsExtractComputeCPointer_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jint aDataStyle, jint aDataType) {
    char *tName = parseStr(aEnv, aName);
    void *tRef = lammps_extract_compute((void *)(intptr_t)aLmpPtr, tName, (int)aDataStyle, (int)aDataType); // NO need to free due to it is lammps internal data
    jboolean tHasException = exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tHasException) return 0;
    return (jlong)(intptr_t)tRef;
}

JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsScatter_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jstring aName, jboolean aIsDouble, jint aAtomNum, jint aCount, jdoubleArray aData) {
    void *tDataBuf;
    if (aIsDouble) {
        jint tLen = aCount * aAtomNum;
        tDataBuf = MALLOCN_TP(double, tLen);
        parsejdouble2double(aEnv, aData, (double *)tDataBuf, tLen);
    } else {
        jint tLen = aCount * aAtomNum;
        tDataBuf = MALLOCN_TP(int, tLen);
        parsejdouble2int(aEnv, aData, (int *)tDataBuf, tLen);
    }
    char *tName = parseStr(aEnv, aName);
#ifdef LAMMPS_OLD
    lammps_scatter_atoms((void *)(intptr_t)aLmpPtr, tName, 1, aCount, tDataBuf);
#else
    lammps_scatter((void *)(intptr_t)aLmpPtr, tName, 1, aCount, tDataBuf);
#endif
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    FREE(tName);
    if (tDataBuf != NULL) FREE(tDataBuf);
}
JNIEXPORT void JNICALL Java_jse_lmp_NativeLmp_lammpsCreateAtoms_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr, jint aAtomNum, jintArray aID, jintArray aType, jdoubleArray aXYZ, jdoubleArray aVel, jintArray aImage, jboolean aShrinkExceed) {
    jint tAtomNum3 = aAtomNum * 3;
    intbig *tID    = aID   ==NULL ? NULL : MALLOCN_TP(intbig, aAtomNum ); parsejint2intbig   (aEnv, aID   , tID   , aAtomNum );
    int    *tType  = aType ==NULL ? NULL : MALLOCN_TP(int   , aAtomNum ); parsejint2int      (aEnv, aType , tType , aAtomNum );
    intbig *tImage = aImage==NULL ? NULL : MALLOCN_TP(intbig, aAtomNum ); parsejint2intbig   (aEnv, aImage, tImage, aAtomNum );
    double *tXYZ   = aXYZ  ==NULL ? NULL : MALLOCN_TP(double, tAtomNum3); parsejdouble2double(aEnv, aXYZ  , tXYZ  , tAtomNum3);
    double *tVel   = aVel  ==NULL ? NULL : MALLOCN_TP(double, tAtomNum3); parsejdouble2double(aEnv, aVel  , tVel  , tAtomNum3);
    lammps_create_atoms((void *)(intptr_t)aLmpPtr, aAtomNum, tID, tType, tXYZ, tVel, tImage, aShrinkExceed);
    exceptionCheckLMP(aEnv, (void *)(intptr_t)aLmpPtr);
    if (tID    != NULL) FREE(tID   );
    if (tType  != NULL) FREE(tType );
    if (tImage != NULL) FREE(tImage);
    if (tXYZ   != NULL) FREE(tXYZ  );
    if (tVel   != NULL) FREE(tVel  );
}

JNIEXPORT void JNICALL Java_jse_lmp_NativeLmpPointer_lammpsClose_1(JNIEnv *aEnv, jclass aClazz, jlong aLmpPtr) {
    lammps_close((void *)(intptr_t)aLmpPtr);
    exceptionCheckLMP(aEnv, NULL);
}

}
