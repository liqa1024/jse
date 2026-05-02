#include "jse_lmp_LmpPlugin_Fix.h"
#include "fix_jse.h"

#include <stdint.h>

extern "C" {

using namespace LAMMPS_NS;

JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setBoxChange0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setBoxChange(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setNoChangeBox0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setNoChangeBox(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setForceReneighbor0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setForceReneighbor(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setNextReneighbor0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jlong aTimestep) {
    ((FixJSE *)(intptr_t)aFixPtr)->setNextReneighbor(aTimestep);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_nextReneighbor0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->nextReneighbor();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setNevery0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aNevery) {
    ((FixJSE *)(intptr_t)aFixPtr)->setNevery(aNevery);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setEnergyGlobalFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setEnergyGlobalFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setEnergyPeratomFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setEnergyPeratomFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setVirialGlobalFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setVirialGlobalFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setVirialPeratomFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setVirialPeratomFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setTimeDepend0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setTimeDepend(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setDynamicGroupAllow0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setDynamicGroupAllow(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setScalarFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setScalarFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setVectorFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setVectorFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setArrayFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setArrayFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setSizeVector0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aSize) {
    ((FixJSE *)(intptr_t)aFixPtr)->setSizeVector(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setSizeArrayRows0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aRos) {
    ((FixJSE *)(intptr_t)aFixPtr)->setSizeArrayRows(aRos);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setSizeArrayCols0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aCols) {
    ((FixJSE *)(intptr_t)aFixPtr)->setSizeArrayCols(aCols);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setGlobalFreq0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aFreq) {
    ((FixJSE *)(intptr_t)aFixPtr)->setGlobalFreq(aFreq);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setExtscalar0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setExtscalar(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setExtvector0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setExtvector(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setExtarray0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setExtarray(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setPeratomFlag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jboolean aFlag) {
    ((FixJSE *)(intptr_t)aFixPtr)->setPeratomFlag(aFlag);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setSizePeratomCols0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aCols) {
    ((FixJSE *)(intptr_t)aFixPtr)->setSizePeratomCols(aCols);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setPeratomFreq0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aFreq) {
    ((FixJSE *)(intptr_t)aFixPtr)->setPeratomFreq(aFreq);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setVectorAtom0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jlong aPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->setVectorAtom(aPtr);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setArrayAtom0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jlong aPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->setArrayAtom(aPtr);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setCommForward0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aSize) {
    ((FixJSE *)(intptr_t)aFixPtr)->setCommForward(aSize);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_setCommReverse0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aSize) {
    ((FixJSE *)(intptr_t)aFixPtr)->setCommReverse(aSize);
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_findVariable0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jstring aName) {
    return ((FixJSE *)(intptr_t)aFixPtr)->findVariable(aName);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_computeVariable0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aIdx) {
    return ((FixJSE *)(intptr_t)aFixPtr)->computeVariable(aIdx);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_citemeAdd0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jstring aCite) {
    ((FixJSE *)(intptr_t)aFixPtr)->citemeAdd(aCite);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestDefault0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestDefault(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestFull0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestFull(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestOccasional0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestOccasional(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborRequestOccasionalFull0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jdouble aRCut) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborRequestOccasionalFull(aRCut);
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborBuildOne0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->neighborBuildOne();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborAgo0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborAgo();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborCutneighmin0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborCutneighmin();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborCutneighmax0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborCutneighmax();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborCuttype0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jint aType) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborCuttype(aType);
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_neighborSkin0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->neighborSkin();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_igroup0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->igroup_();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_groupbit0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->groupbit_();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomX0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomX();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomV0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomV();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomF0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomF();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomMask0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomMask();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomTag0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomTag();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomType0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomType();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomMass0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomMass();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomExtract0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr, jstring aName) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomExtract(aName);
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNatoms0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNatoms();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNtypes0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNtypes();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNlocal0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNlocal();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNmax0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNmax();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_atomNghost0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->atomNghost();
}
JNIEXPORT jboolean JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainTriclinic0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainTriclinic();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainXy0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainXy();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainXz0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainXz();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainYz0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainYz();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainXprd0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainXprd();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainYprd0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainYprd();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainZprd0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainZprd();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainH0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainH();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainHInv0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainHInv();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxlo0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxlo();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxhi0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxhi();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxloLamda0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxloLamda();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainBoxhiLamda0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainBoxhiLamda();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSublo0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainSublo();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSubhi0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainSubhi();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSubloLamda0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->domainSubloLamda();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSubhiLamda0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
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
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSetGlobalBox0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainSetGlobalBox();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_domainSetLocalBox0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->domainSetLocalBox();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listGnum0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listGnum();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listInum0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listInum();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listIlist0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listIlist();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listNumneigh0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listNumneigh();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_listFirstneigh0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->listFirstneigh();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_forceBoltz0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->forceBoltz();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_forcePairCutforce0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->forcePairCutforce();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_dt0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->dt();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_ntimestep0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->ntimestep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_firststep0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->firststep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_laststep0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->laststep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_beginstep0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->beginstep();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_endstep0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->endstep();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commMe0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commMe();
}
JNIEXPORT jint JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commNprocs0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commNprocs();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commBarrier0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    ((FixJSE *)(intptr_t)aFixPtr)->commBarrier();
}
JNIEXPORT jlong JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commWorld0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commWorld();
}
JNIEXPORT jdouble JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commCutghostuser0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commCutghostuser();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commForwardComm0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commForwardComm();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commForwardCommThis0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commForwardCommThis();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commReverseComm0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commReverseComm();
}
JNIEXPORT void JNICALL Java_jse_lmp_LmpPlugin_00024Fix_commReverseCommThis0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->commReverseCommThis();
}
JNIEXPORT jstring JNICALL Java_jse_lmp_LmpPlugin_00024Fix_unitStyle0(JNIEnv *aEnv, jclass aClazz, jlong aFixPtr) {
    return ((FixJSE *)(intptr_t)aFixPtr)->unitStyle();
}

}
