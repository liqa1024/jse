#include "jse_lmp_LmpPlugin_Pair.h"
#include "pair_jse.h"

#include <stdint.h>

extern "C" {

using namespace LAMMPS_NS;

JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_findVariable0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jstring aName) {
    return ((PairJSE *)(intptr_t)aPairPtr)->findVariable(aName);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Pair_computeVariable0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aIdx) {
    return ((PairJSE *)(intptr_t)aPairPtr)->computeVariable(aIdx);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_citemeAdd0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jstring aCite) {
    ((PairJSE *)(intptr_t)aPairPtr)->citemeAdd(aCite);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setSingleEnable0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setSingleEnable(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setOneCoeff0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setOneCoeff(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setManybodyFlag0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setManybodyFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setUnitConvertFlag0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setUnitConvertFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setNoVirialFdotrCompute0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setNoVirialFdotrCompute(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setFinitecutflag0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setFinitecutflag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setGhostneigh0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setGhostneigh(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCentroidstressflag0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aFlag) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCentroidstressflag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCommForward0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aSize) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCommForward(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCommReverse0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aSize) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCommReverse(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_setCommReverseOff0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint aSize) {
    ((PairJSE *)(intptr_t)aPairPtr)->setCommReverseOff(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_neighborRequestDefault0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->neighborRequestDefault();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_neighborRequestFull0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->neighborRequestFull();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_neighborAgo0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->neighborAgo();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomX0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomX();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomV0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomV();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomF0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomF();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomTag0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomTag();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomType0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomType();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomMass0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomMass();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomExtract0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jstring aName) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomExtract(aName);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNatoms0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNatoms();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNtypes0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNtypes();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNlocal0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNlocal();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNghost0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNghost();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomNmax0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomNmax();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_forceSpecialLj0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->forceSpecialLj();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_forceNewtonPair0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->forceNewtonPair();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listGnum0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listGnum();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listInum0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listInum();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listIlist0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listIlist();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listNumneigh0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listNumneigh();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_listFirstneigh0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->listFirstneigh();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Pair_cutsq0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jint j) {
    return ((PairJSE *)(intptr_t)aPairPtr)->cutsq_(i, j);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_engVdwl0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->engVdwl();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_engCoul0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->engCoul();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eatom0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eatom_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_virial0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->virial_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vatom0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vatom_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_cvatom0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->cvatom_();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTally0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTally(i, j, nlocal, newtonPair, evdwl, ecoul, fpair, delx, dely, delz);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTallyFull0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTallyFull(i, evdwl, ecoul, fpair, delx, dely, delz);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTallyXYZ0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fx, jdouble fy, jdouble fz, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTallyXYZ(i, j, nlocal, newtonPair, evdwl, ecoul, fx, fy, fz, delx, dely, delz);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTallyXYZFull0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jdouble evdwl, jdouble ecoul, jdouble fx, jdouble fy, jdouble fz, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTallyXYZFull(i, evdwl, ecoul, fx, fy, fz, delx, dely, delz);
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evflag0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->evflag_();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagEither0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagEither();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagGlobal0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagGlobal();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagAtom0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagAtom();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_cvflagAtom0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->cvflagAtom();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eflagEither0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eflagEither();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eflagGlobal0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eflagGlobal();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_eflagAtom0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->eflagAtom();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_vflagFdotr0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->vflagFdotr();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_virialFdotrCompute0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->virialFdotrCompute();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commMe0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commMe();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commNprocs0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commNprocs();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commBarrier0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commBarrier();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commWorld0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commWorld();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commForwardComm0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commForwardComm();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commForwardCommThis0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commForwardCommThis();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commReverseComm0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commReverseComm();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_commReverseCommThis0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->commReverseCommThis();
}
JNIEXPORT jstring JNICALL Java_jse_lmp_LmpPlugin_00024Pair_unitStyle0(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->unitStyle();
}

}
