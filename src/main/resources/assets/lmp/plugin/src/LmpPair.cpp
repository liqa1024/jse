#include "LmpPair.h"

#include <stdint.h>

jclass JSE_LMPPAIR::LMPPAIR_CLAZZ = NULL;
jclass JSE_LMPPAIR::STRING_CLAZZ = NULL;

jboolean JSE_LMPPAIR::cacheJClass(JNIEnv *aEnv) {
    if (LMPPAIR_CLAZZ == NULL) {
        jclass clazz = aEnv->FindClass("jse/lmp/LmpPlugin$Pair");
        if (aEnv->ExceptionCheck()) return JNI_FALSE;
        LMPPAIR_CLAZZ = (jclass)aEnv->NewGlobalRef(clazz);
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
void JSE_LMPPAIR::uncacheJClass(JNIEnv *aEnv) {
    if (LMPPAIR_CLAZZ != NULL) {
        aEnv->DeleteGlobalRef(LMPPAIR_CLAZZ);
        LMPPAIR_CLAZZ = NULL;
    }
    if (STRING_CLAZZ != NULL) {
        aEnv->DeleteGlobalRef(STRING_CLAZZ);
        STRING_CLAZZ = NULL;
    }
}

static jmethodID sOf = 0;
static jmethodID sCompute = 0;
static jmethodID sSingle = 0;
static jmethodID sCoeff = 0;
static jmethodID sSettings = 0;
static jmethodID sInitStyle = 0;
static jmethodID sClose = 0;
static jmethodID sInitOne = 0;
static jmethodID sPackForwardComm = 0;
static jmethodID sUnpackForwardComm = 0;
static jmethodID sPackReverseComm = 0;
static jmethodID sUnpackReverseComm = 0;

jobject JSE_LMPPAIR::newJObject(JNIEnv *aEnv, char *aArg, void *aPtr) {
    jobject rOut = NULL;

    jstring tJArg = aEnv->NewStringUTF(aArg);
    if (sOf || (sOf = aEnv->GetStaticMethodID(LMPPAIR_CLAZZ, "of", "(Ljava/lang/String;J)Ljse/lmp/LmpPlugin$Pair;"))) {
        rOut = aEnv->CallStaticObjectMethod(LMPPAIR_CLAZZ, sOf, tJArg, (jlong)(intptr_t)aPtr);
    }
    aEnv->DeleteLocalRef(tJArg);
    
    return rOut;
}

void JSE_LMPPAIR::compute(JNIEnv *aEnv, jobject aSelf) {
    if (sCompute || (sCompute = aEnv->GetMethodID(LMPPAIR_CLAZZ, "compute", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sCompute);
    }
}
double JSE_LMPPAIR::single(JNIEnv *aEnv, jobject aSelf, int i, int j, int itype, int jtype, double rsq, double factor_coul, double factor_lj, double &fforce) {
    fforce = 0.0;
    double *tPtr = &fforce;
    if (sSingle || (sSingle = aEnv->GetMethodID(LMPPAIR_CLAZZ, "single_", "(IIIIDDDJ)D"))) {
        return aEnv->CallDoubleMethod(aSelf, sSingle, (jint)i, (jint)j, (jint)itype, (jint)jtype, rsq, rsq, factor_coul, factor_lj, (jlong)(intptr_t)tPtr);
    }
    return 0.0;
}
void JSE_LMPPAIR::coeff(JNIEnv *aEnv, jobject aSelf, int aArgc, char **aArgv) {
    jobjectArray tJArgs = aEnv->NewObjectArray(aArgc, STRING_CLAZZ, NULL);
    for (int i = 0; i < aArgc; ++i) {
        jstring tStr = aEnv->NewStringUTF(aArgv[i]);
        aEnv->SetObjectArrayElement(tJArgs, i, tStr);
        aEnv->DeleteLocalRef(tStr);
    }
    if (sCoeff || (sCoeff = aEnv->GetMethodID(LMPPAIR_CLAZZ, "coeff", "([Ljava/lang/String;)V"))) {
        aEnv->CallVoidMethod(aSelf, sCoeff, tJArgs);
    }
    aEnv->DeleteLocalRef(tJArgs);
}
void JSE_LMPPAIR::settings(JNIEnv *aEnv, jobject aSelf, int aArgc, char **aArgv) {
    jobjectArray tJArgs = aEnv->NewObjectArray(aArgc, STRING_CLAZZ, NULL);
    for (int i = 0; i < aArgc; ++i) {
        jstring tStr = aEnv->NewStringUTF(aArgv[i]);
        aEnv->SetObjectArrayElement(tJArgs, i, tStr);
        aEnv->DeleteLocalRef(tStr);
    }
    if (sSettings || (sSettings = aEnv->GetMethodID(LMPPAIR_CLAZZ, "settings", "([Ljava/lang/String;)V"))) {
        aEnv->CallVoidMethod(aSelf, sSettings, tJArgs);
    }
    aEnv->DeleteLocalRef(tJArgs);
}
void JSE_LMPPAIR::initStyle(JNIEnv *aEnv, jobject aSelf) {
    if (sInitStyle || (sInitStyle = aEnv->GetMethodID(LMPPAIR_CLAZZ, "initStyle", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sInitStyle);
    }
}
double JSE_LMPPAIR::initOne(JNIEnv *aEnv, jobject aSelf, int i, int j) {
    if (sInitOne || (sInitOne = aEnv->GetMethodID(LMPPAIR_CLAZZ, "initOne", "(II)D"))) {
        return aEnv->CallDoubleMethod(aSelf, sInitOne, (jint)i, (jint)j);
    }
    return -1.0;
}
void JSE_LMPPAIR::close(JNIEnv *aEnv, jobject aSelf) {
    if (sClose || (sClose = aEnv->GetMethodID(LMPPAIR_CLAZZ, "close", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sClose);
    }
}

int JSE_LMPPAIR::packForwardComm(JNIEnv *aEnv, jobject aSelf, int n, int *list, double *buf, int pbc_flag, int *pbc) {
    if (sPackForwardComm || (sPackForwardComm = aEnv->GetMethodID(LMPPAIR_CLAZZ, "packForwardComm_", "(IJJIJ)I"))) {
        return (int)aEnv->CallIntMethod(aSelf, sPackForwardComm, (jint)n, (jlong)(intptr_t)list, (jlong)(intptr_t)buf, (jint)pbc_flag, (jlong)(intptr_t)pbc);
    }
    return 0;
}
void JSE_LMPPAIR::unpackForwardComm(JNIEnv *aEnv, jobject aSelf, int n, int first, double *buf) {
    if (sUnpackForwardComm || (sUnpackForwardComm = aEnv->GetMethodID(LMPPAIR_CLAZZ, "unpackForwardComm_", "(IIJ)V"))) {
        aEnv->CallVoidMethod(aSelf, sUnpackForwardComm, (jint)n, (jint)first, (jlong)(intptr_t)buf);
    }
}
int JSE_LMPPAIR::packReverseComm(JNIEnv *aEnv, jobject aSelf, int n, int first, double *buf) {
    if (sPackReverseComm || (sPackReverseComm = aEnv->GetMethodID(LMPPAIR_CLAZZ, "packReverseComm_", "(IIJ)I"))) {
        return (int)aEnv->CallIntMethod(aSelf, sPackReverseComm, (jint)n, (jint)first, (jlong)(intptr_t)buf);
    }
    return 0;
}
void JSE_LMPPAIR::unpackReverseComm(JNIEnv *aEnv, jobject aSelf, int n, int *list, double *buf) {
    if (sUnpackReverseComm || (sUnpackReverseComm = aEnv->GetMethodID(LMPPAIR_CLAZZ, "unpackReverseComm_", "(IJJ)V"))) {
        aEnv->CallVoidMethod(aSelf, sUnpackReverseComm, (jint)n, (jlong)(intptr_t)list, (jlong)(intptr_t)buf);
    }
}

