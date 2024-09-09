#include "jse_lmp_LmpPlugin_Pair.h"
#include "pair_jse.h"

extern "C" {

using namespace LAMMPS_NS;

JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_neighborRequestDefault_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->neighborRequestDefault();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_neighborRequestFull_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    ((PairJSE *)(intptr_t)aPairPtr)->neighborRequestFull();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evInit_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jboolean eflag, jboolean vflag) {
    ((PairJSE *)(intptr_t)aPairPtr)->evInit(eflag, vflag);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomX_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomX();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomF_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomF();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_atomType_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->atomType();
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
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Pair_forceSpecialLj_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->forceSpecialLj();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_forceNewtonPair_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->forceNewtonPair();
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
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evTally_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr, jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ((PairJSE *)(intptr_t)aPairPtr)->evTally(i, j, nlocal, newtonPair, evdwl, ecoul, fpair, delx, dely, delz);
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Pair_evflag_1(JNIEnv *aEnv, jclass aClazz, jlong aPairPtr) {
    return ((PairJSE *)(intptr_t)aPairPtr)->evflag_();
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

}
