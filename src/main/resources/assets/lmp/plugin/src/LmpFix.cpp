#include "LmpFix.h"

#include <stdint.h>

jclass JSE_LMPFIX::LMPFIX_CLAZZ = NULL;
jclass JSE_LMPFIX::STRING_CLAZZ = NULL;

jboolean JSE_LMPFIX::cacheJClass(JNIEnv *aEnv) {
    if (LMPFIX_CLAZZ == NULL) {
        jclass clazz = aEnv->FindClass("jse/lmp/LmpPlugin$Fix");
        if (aEnv->ExceptionCheck()) return JNI_FALSE;
        LMPFIX_CLAZZ = (jclass)aEnv->NewGlobalRef(clazz);
        aEnv->DeleteLocalRef(clazz);
    }
    if (STRING_CLAZZ == NULL) {
        jclass clazz = aEnv->FindClass("java/lang/String");
        if (aEnv->ExceptionCheck()) return JNI_FALSE;
        STRING_CLAZZ = (jclass)aEnv->NewGlobalRef(clazz);
        aEnv->DeleteLocalRef(clazz);
    }
    return JNI_TRUE;
}
void JSE_LMPFIX::uncacheJClass(JNIEnv *aEnv) {
    if (LMPFIX_CLAZZ != NULL) {
        aEnv->DeleteGlobalRef(LMPFIX_CLAZZ);
        LMPFIX_CLAZZ = NULL;
    }
    if (STRING_CLAZZ != NULL) {
        aEnv->DeleteGlobalRef(STRING_CLAZZ);
        STRING_CLAZZ = NULL;
    }
}

static jmethodID sOf = 0;
static jmethodID sSetMask = 0;
static jmethodID sInit = 0;
static jmethodID sSetup = 0;
static jmethodID sMinSetup = 0;
static jmethodID sShutdown = 0;
static jmethodID sInitialIntegrate = 0;
static jmethodID sPostIntegrate = 0;
static jmethodID sPreExchange = 0;
static jmethodID sPreNeighbor = 0;
static jmethodID sPostNeighbor = 0;
static jmethodID sPreForce = 0;
static jmethodID sPreReverse = 0;
static jmethodID sPostForce = 0;
static jmethodID sFinalIntegrate = 0;
static jmethodID sEndOfStep = 0;
static jmethodID sPostRun = 0;
static jmethodID sMinPreExchange = 0;
static jmethodID sMinPreNeighbor = 0;
static jmethodID sMinPostNeighbor = 0;
static jmethodID sMinPreForce = 0;
static jmethodID sMinPreReverse = 0;
static jmethodID sMinPostForce = 0;
static jmethodID sComputeScalar = 0;
static jmethodID sComputeVector = 0;
static jmethodID sComputeArray = 0;

jobject JSE_LMPFIX::newJObject(JNIEnv *aEnv, char *aArg, void *aPtr, int aArgc, char **aArgv) {
    jobject rOut = NULL;
    
    jobjectArray tJArgs = aEnv->NewObjectArray(aArgc, STRING_CLAZZ, NULL);
    for (int i = 0; i < aArgc; ++i) {
        jstring tStr = aEnv->NewStringUTF(aArgv[i]);
        aEnv->SetObjectArrayElement(tJArgs, i, tStr);
        aEnv->DeleteLocalRef(tStr);
    }
    jstring tJArg = aEnv->NewStringUTF(aArg);
    if (sOf || (sOf = aEnv->GetStaticMethodID(LMPFIX_CLAZZ, "of", "(Ljava/lang/String;J[Ljava/lang/String;)Ljse/lmp/LmpPlugin$Fix;"))) {
        rOut = aEnv->CallStaticObjectMethod(LMPFIX_CLAZZ, sOf, tJArg, (jlong)(intptr_t)aPtr, tJArgs);
    }
    aEnv->DeleteLocalRef(tJArg);
    aEnv->DeleteLocalRef(tJArgs);
    
    return rOut;
}

int JSE_LMPFIX::setMask(JNIEnv *aEnv, jobject aSelf) {
    if (sSetMask || (sSetMask = aEnv->GetMethodID(LMPFIX_CLAZZ, "setMask", "()I"))) {
        return (int)aEnv->CallIntMethod(aSelf, sSetMask);
    }
    return 0;
}
void JSE_LMPFIX::init(JNIEnv *aEnv, jobject aSelf) {
    if (sInit || (sInit = aEnv->GetMethodID(LMPFIX_CLAZZ, "init", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sInit);
    }
}
void JSE_LMPFIX::setup(JNIEnv *aEnv, jobject aSelf, int vflag) {
    if (sSetup || (sSetup = aEnv->GetMethodID(LMPFIX_CLAZZ, "setup", "(I)V"))) {
        aEnv->CallVoidMethod(aSelf, sSetup, (jint)vflag);
    }
}
void JSE_LMPFIX::minSetup(JNIEnv *aEnv, jobject aSelf, int vflag) {
    if (sMinSetup || (sMinSetup = aEnv->GetMethodID(LMPFIX_CLAZZ, "minSetup", "(I)V"))) {
        aEnv->CallVoidMethod(aSelf, sMinSetup, (jint)vflag);
    }
}
void JSE_LMPFIX::shutdown(JNIEnv *aEnv, jobject aSelf) {
    if (sShutdown || (sShutdown = aEnv->GetMethodID(LMPFIX_CLAZZ, "shutdown", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sShutdown);
    }
}

void JSE_LMPFIX::initialIntegrate(JNIEnv *aEnv, jobject aSelf, int vflag) {
    if (sInitialIntegrate || (sInitialIntegrate = aEnv->GetMethodID(LMPFIX_CLAZZ, "initialIntegrate", "(I)V"))) {
        aEnv->CallVoidMethod(aSelf, sInitialIntegrate, (jint)vflag);
    }
}
void JSE_LMPFIX::postIntegrate(JNIEnv *aEnv, jobject aSelf) {
    if (sPostIntegrate || (sPostIntegrate = aEnv->GetMethodID(LMPFIX_CLAZZ, "postIntegrate", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sPostIntegrate);
    }
}
void JSE_LMPFIX::preExchange(JNIEnv *aEnv, jobject aSelf) {
    if (sPreExchange || (sPreExchange = aEnv->GetMethodID(LMPFIX_CLAZZ, "preExchange", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sPreExchange);
    }
}
void JSE_LMPFIX::preNeighbor(JNIEnv *aEnv, jobject aSelf) {
    if (sPreNeighbor || (sPreNeighbor = aEnv->GetMethodID(LMPFIX_CLAZZ, "preNeighbor", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sPreNeighbor);
    }
}
void JSE_LMPFIX::postNeighbor(JNIEnv *aEnv, jobject aSelf) {
    if (sPostNeighbor || (sPostNeighbor = aEnv->GetMethodID(LMPFIX_CLAZZ, "postNeighbor", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sPostNeighbor);
    }
}
void JSE_LMPFIX::preForce(JNIEnv *aEnv, jobject aSelf, int vflag) {
    if (sPreForce || (sPreForce = aEnv->GetMethodID(LMPFIX_CLAZZ, "preForce", "(I)V"))) {
        aEnv->CallVoidMethod(aSelf, sPreForce, vflag);
    }
}
void JSE_LMPFIX::preReverse(JNIEnv *aEnv, jobject aSelf, int eflag, int vflag) {
    if (sPreReverse || (sPreReverse = aEnv->GetMethodID(LMPFIX_CLAZZ, "preReverse", "(II)V"))) {
        aEnv->CallVoidMethod(aSelf, sPreReverse, eflag, vflag);
    }
}
void JSE_LMPFIX::postForce(JNIEnv *aEnv, jobject aSelf, int vflag) {
    if (sPostForce || (sPostForce = aEnv->GetMethodID(LMPFIX_CLAZZ, "postForce", "(I)V"))) {
        aEnv->CallVoidMethod(aSelf, sPostForce, vflag);
    }
}
void JSE_LMPFIX::finalIntegrate(JNIEnv *aEnv, jobject aSelf) {
    if (sFinalIntegrate || (sFinalIntegrate = aEnv->GetMethodID(LMPFIX_CLAZZ, "finalIntegrate", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sFinalIntegrate);
    }
}
void JSE_LMPFIX::endOfStep(JNIEnv *aEnv, jobject aSelf) {
    if (sEndOfStep || (sEndOfStep = aEnv->GetMethodID(LMPFIX_CLAZZ, "endOfStep", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sEndOfStep);
    }
}
void JSE_LMPFIX::postRun(JNIEnv *aEnv, jobject aSelf) {
    if (sPostRun || (sPostRun = aEnv->GetMethodID(LMPFIX_CLAZZ, "postRun", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sPostRun);
    }
}
void JSE_LMPFIX::minPreExchange(JNIEnv *aEnv, jobject aSelf) {
    if (sMinPreExchange || (sMinPreExchange = aEnv->GetMethodID(LMPFIX_CLAZZ, "minPreExchange", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sMinPreExchange);
    }
}
void JSE_LMPFIX::minPreNeighbor(JNIEnv *aEnv, jobject aSelf) {
    if (sMinPreNeighbor || (sMinPreNeighbor = aEnv->GetMethodID(LMPFIX_CLAZZ, "minPreNeighbor", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sMinPreNeighbor);
    }
}
void JSE_LMPFIX::minPostNeighbor(JNIEnv *aEnv, jobject aSelf) {
    if (sMinPostNeighbor || (sMinPostNeighbor = aEnv->GetMethodID(LMPFIX_CLAZZ, "minPostNeighbor", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sMinPostNeighbor);
    }
}
void JSE_LMPFIX::minPreForce(JNIEnv *aEnv, jobject aSelf, int vflag) {
    if (sMinPreForce || (sMinPreForce = aEnv->GetMethodID(LMPFIX_CLAZZ, "minPreForce", "(I)V"))) {
        aEnv->CallVoidMethod(aSelf, sMinPreForce, vflag);
    }
}
void JSE_LMPFIX::minPreReverse(JNIEnv *aEnv, jobject aSelf, int eflag, int vflag) {
    if (sMinPreReverse || (sMinPreReverse = aEnv->GetMethodID(LMPFIX_CLAZZ, "minPreReverse", "(II)V"))) {
        aEnv->CallVoidMethod(aSelf, sMinPreReverse, eflag, vflag);
    }
}
void JSE_LMPFIX::minPostForce(JNIEnv *aEnv, jobject aSelf, int vflag) {
    if (sMinPostForce || (sMinPostForce = aEnv->GetMethodID(LMPFIX_CLAZZ, "minPostForce", "(I)V"))) {
        aEnv->CallVoidMethod(aSelf, sMinPostForce, vflag);
    }
}

double JSE_LMPFIX::computeScalar(JNIEnv *aEnv, jobject aSelf) {
    if (sComputeScalar || (sComputeScalar = aEnv->GetMethodID(LMPFIX_CLAZZ, "computeScalar", "()D"))) {
        return aEnv->CallDoubleMethod(aSelf, sComputeScalar);
    }
    return 0.0;
}
double JSE_LMPFIX::computeVector(JNIEnv *aEnv, jobject aSelf, int i) {
    if (sComputeVector || (sComputeVector = aEnv->GetMethodID(LMPFIX_CLAZZ, "computeVector", "(I)D"))) {
        return aEnv->CallDoubleMethod(aSelf, sComputeVector, (jint)i);
    }
    return 0.0;
}
double JSE_LMPFIX::computeArray(JNIEnv *aEnv, jobject aSelf, int i, int j) {
    if (sComputeArray || (sComputeArray = aEnv->GetMethodID(LMPFIX_CLAZZ, "computeArray", "(II)D"))) {
        return aEnv->CallDoubleMethod(aSelf, sComputeArray, (jint)i, (jint)j);
    }
    return 0.0;
}
