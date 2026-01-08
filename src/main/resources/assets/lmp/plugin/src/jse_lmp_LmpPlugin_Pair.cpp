#include "jse_lmp_LmpPlugin_Pair.h"
#include "pair_jse.h"

#include <stdint.h>

extern "C" {

using namespace LAMMPS_NS;

JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_findVariable_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jstring aName) {
    return ((PairJSE *)(intptr_t)aPairPtr)->findVariable(aName);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Pair_computeVariable_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aIdx) {
    return ((PairJSE *)(intptr_t)aPairPtr)->computeVariable(aIdx);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setSingleEnable_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setSingleEnable(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setOneCoeff_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setOneCoeff(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setManybodyFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setManybodyFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setUnitConvertFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setUnitConvertFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setNoVirialFdotrCompute_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setNoVirialFdotrCompute(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setFinitecutflag_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setFinitecutflag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setGhostneigh_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setGhostneigh(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCentroidstressflag_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCentroidstressflag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCommForward_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aSize) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCommForward(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCommReverse_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aSize) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCommReverse(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCommReverseOff_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aSize) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCommReverseOff(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_neighborRequestDefault_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->neighborRequestDefault();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_neighborRequestFull_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->neighborRequestFull();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomX_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomX();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomV_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomV();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomF_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomF();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomTag_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomTag();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomType_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomType();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomMass_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomMass();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomExtract_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jstring aName) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomExtract(aName);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNatoms_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNatoms();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNtypes_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNtypes();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNlocal_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNlocal();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNghost_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNghost();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNmax_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNmax();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_forceSpecialLj_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->forceSpecialLj();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_forceNewtonPair_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->forceNewtonPair();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listGnum_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listGnum();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listInum_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listInum();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listIlist_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listIlist();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listNumneigh_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listNumneigh();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listFirstneigh_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listFirstneigh();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Pair_cutsq_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jint j) {
    return ((PairJSE *)(intptr_t)aPairPtr)->cutsq_(i, j);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_engVdwl_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->engVdwl();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_engCoul_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->engCoul();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eatom_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eatom_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_virial_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->virial_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vatom_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vatom_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_cvatom_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->cvatom_();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTally_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTally(i, j, nlocal, newtonPair, evdwl, ecoul, fpair, delx, dely, delz);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTallyFull_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTallyFull(i, evdwl, ecoul, fpair, delx, dely, delz);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTallyXYZ_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fx, jdouble fy, jdouble fz, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTallyXYZ(i, j, nlocal, newtonPair, evdwl, ecoul, fx, fy, fz, delx, dely, delz);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTallyXYZFull_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jdouble evdwl, jdouble ecoul, jdouble fx, jdouble fy, jdouble fz, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTallyXYZFull(i, evdwl, ecoul, fx, fy, fz, delx, dely, delz);
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evflag_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->evflag_();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagEither_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagEither();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagGlobal_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagGlobal();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagAtom_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagAtom();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_cvflagAtom_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->cvflagAtom();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eflagEither_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eflagEither();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eflagGlobal_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eflagGlobal();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eflagAtom_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eflagAtom();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagFdotr_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagFdotr();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_virialFdotrCompute_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->virialFdotrCompute();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commMe_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commMe();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commNprocs_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commNprocs();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commWorld_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commWorld();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commForwardComm_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commForwardComm();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commForwardCommThis_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commForwardCommThis();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commReverseComm_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commReverseComm();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commReverseCommThis_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commReverseCommThis();
}
JNIEXPORT jstring JNICALL Java_jse_lmp_LmpPlugin_00024Pair_unitStyle_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->unitStyle();
}

}
