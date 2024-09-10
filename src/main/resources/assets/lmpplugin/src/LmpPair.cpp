#include "jniutil.h"
#include "LmpPair.h"


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
static jmethodID sCoeff = 0;
static jmethodID sInitStyle = 0;
static jmethodID sInitOne = 0;

jobject JSE_LMPPAIR::newJObject(JNIEnv *aEnv, char *aArg, void *aPtr) {
    jobject rOut = NULL;

    jstring tJArg = aEnv->NewStringUTF(aArg);
    if (sOf || (sOf = aEnv->GetStaticMethodID(LMPPAIR_CLAZZ, "of", "(Ljava/lang/String;J)Ljse/lmp/LmpPlugin$Pair;"))) {
        rOut = aEnv->CallStaticObjectMethod(LMPPAIR_CLAZZ, sOf, tJArg, (jlong)(intptr_t)aPtr);
    }
    aEnv->DeleteLocalRef(tJArg);
    
    return rOut;
}

void JSE_LMPPAIR::compute(JNIEnv *aEnv, jobject aSelf, int eflag, int vflag) {
    if (sCompute || (sCompute = aEnv->GetMethodID(LMPPAIR_CLAZZ, "compute", "(ZZ)V"))) {
        aEnv->CallVoidMethod(aSelf, sCompute, eflag ? JNI_TRUE : JNI_FALSE, vflag ? JNI_TRUE : JNI_FALSE);
    }
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
void JSE_LMPPAIR::initStyle(JNIEnv *aEnv, jobject aSelf) {
    if (sInitStyle || (sInitStyle = aEnv->GetMethodID(LMPPAIR_CLAZZ, "initStyle", "()V"))) {
        aEnv->CallVoidMethod(aSelf, sInitStyle);
    }
}
double JSE_LMPPAIR::initOne(JNIEnv *aEnv, jobject aSelf, int i, int j) {
    if (sInitOne || (sInitOne = aEnv->GetMethodID(LMPPAIR_CLAZZ, "initOne", "(II)D"))) {
        return aEnv->CallDoubleMethod(aSelf, sInitOne, i, j);
    }
    return -1.0;
}
