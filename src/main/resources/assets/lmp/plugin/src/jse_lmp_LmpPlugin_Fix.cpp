#include "jse_lmp_LmpPlugin_Fix.h"
#include "fix_jse.h"

#include <stdint.h>

extern "C" {

using namespace LAMMPS_NS;

JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setBoxChange_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setBoxChange(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setNoChangeBox_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setNoChangeBox(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setForceReneighbor_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setForceReneighbor(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setNextReneighbor_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jlong aTimestep) {
    ((FixJSE *)(intptr_t)aFixPtr)->setNextReneighbor(aTimestep);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_nextReneighbor_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->nextReneighbor();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setNevery_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aNevery) {
    ((FixJSE *)(intptr_t)aFixPtr)->setNevery(aNevery);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setEnergyGlobalFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setEnergyGlobalFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setEnergyPeratomFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setEnergyPeratomFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setVirialGlobalFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setVirialGlobalFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setVirialPeratomFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setVirialPeratomFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setTimeDepend_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setTimeDepend(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setDynamicGroupAllow_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setDynamicGroupAllow(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setScalarFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setScalarFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setVectorFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setVectorFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setArrayFlag_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setArrayFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setSizeVector_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aSize) {
    ((FixJSE *)(intptr_t)aFixPtr)->setSizeVector(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setSizeArrayRows_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aRowNum) {
    ((FixJSE *)(intptr_t)aFixPtr)->setSizeArrayRows(aRowNum);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setSizeArrayCols_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aColNum) {
    ((FixJSE *)(intptr_t)aFixPtr)->setSizeArrayCols(aColNum);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setGlobalFreq_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aFreq) {
    ((FixJSE *)(intptr_t)aFixPtr)->setGlobalFreq(aFreq);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setExtscalar_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setExtscalar(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setExtvector_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setExtvector(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setExtarray_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setExtarray(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setCommForward_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aSize) {
    ((FixJSE *)(intptr_t)aFixPtr)->setCommForward(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setCommReverse_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aSize) {
    ((FixJSE *)(intptr_t)aFixPtr)->setCommReverse(aSize);
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_findVariable_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jstring aName) {
    return ((FixJSE *)(intptr_t)aFixPtr)->findVariable(aName);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_computeVariable_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aIdx) {
    return ((FixJSE *)(intptr_t)aFixPtr)->computeVariable(aIdx);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestDefault_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestDefault(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestFull_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestFull(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestOccasional_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestOccasional(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestOccasionalFull_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestOccasionalFull(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborBuildOne_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborBuildOne();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborCutneighmin_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborCutneighmin();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborCutneighmax_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborCutneighmax();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborCuttype_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aType) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborCuttype(aType);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborSkin_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborSkin();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_igroup_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->igroup_();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_groupbit_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->groupbit_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomX_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomX();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomF_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomF();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomMask_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomMask();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomType_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomType();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNatoms_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNatoms();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNtypes_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNtypes();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNlocal_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNlocal();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNmax_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNmax();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNghost_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNghost();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainTriclinic_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainTriclinic();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainXy_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainXy();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainXz_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainXz();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainYz_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainYz();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainXprd_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainXprd();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainYprd_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainYprd();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainZprd_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainZprd();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainH_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainH();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainHInv_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainHInv();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxlo_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxlo();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxhi_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxhi();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxloLamda_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxloLamda();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxhiLamda_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxhiLamda();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSublo_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainSublo();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSubhi_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainSubhi();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSubloLamda_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainSubloLamda();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSubhiLamda_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainSubhiLamda();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainX2lamda_1__JI(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aN) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainX2lamda1(aN);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainX2lamda_1__JJJ(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jlong aX, jlong rLamda) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainX2lamda2(aX, rLamda);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainLamda2x_1__JI(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aN) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainLamda2x1(aN);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainLamda2x_1__JJJ(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jlong aLamda, jlong rX) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainLamda2x2(aLamda, rX);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSetGlobalBox_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainSetGlobalBox();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSetLocalBox_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainSetLocalBox();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listGnum_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listGnum();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listInum_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listInum();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listIlist_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listIlist();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listNumneigh_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listNumneigh();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listFirstneigh_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listFirstneigh();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_forceBoltz_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->forceBoltz();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_forcePairCutforce_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->forcePairCutforce();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_dt_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->dt();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_ntimestep_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->ntimestep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_firststep_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->firststep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_laststep_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->laststep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_beginstep_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->beginstep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_endstep_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->endstep();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commMe_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commMe();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commNprocs_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commNprocs();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commWorld_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commWorld();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commCutghostuser_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commCutghostuser();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commForwardComm_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commForwardComm();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commReverseComm_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commReverseComm();
}
JNIEXPORT jstring JNICALL Java_jse_lmp_LmpPlugin_00024Fix_unitStyle_1(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->unitStyle();
}

}
