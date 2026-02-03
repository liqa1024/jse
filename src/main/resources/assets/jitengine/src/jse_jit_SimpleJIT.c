#include "jse_jit_SimpleJIT.h"
#include "jse_jit_JITLibHandle.h"

#if defined(WIN32) || defined(_WIN64) || defined(_WIN32)
#include <windows.h>
#else
#include <dlfcn.h>
#endif

#include <stdint.h>

typedef int (*plugin_method_t)(void *, void *);

static inline void throwExceptionJIT(JNIEnv *aEnv, const char *aErrStr) {
    const char *tClazzName = "jse/jit/JITException";
    const char *tInitSig = "(Ljava/lang/String;)V";
    // find class runtime due to asm
    jclass tClazz = (*aEnv)->FindClass(aEnv, tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = (*aEnv)->GetMethodID(aEnv, tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, aErrStr);
    jthrowable tException = (jthrowable)(*aEnv)->NewObject(aEnv, tClazz, tInit, tJErrStr);
    (*aEnv)->Throw(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tClazz);
}
static inline void throwExceptionJITCode(JNIEnv *aEnv, jint aErrCode, const char *aErrStr) {
    const char *tClazzName = "jse/jit/JITException";
    const char *tInitSig = "(ILjava/lang/String;)V";
    // find class runtime due to asm
    jclass tClazz = (*aEnv)->FindClass(aEnv, tClazzName);
    if (tClazz == NULL) {
        fprintf(stderr, "Couldn't find %s\n", tClazzName);
        return;
    }
    jmethodID tInit = (*aEnv)->GetMethodID(aEnv, tClazz, "<init>", tInitSig);
    if (tInit == NULL) {
        fprintf(stderr, "Couldn't find %s.<init>%s\n", tClazzName, tInitSig);
        return;
    }
    jstring tJErrStr = (*aEnv)->NewStringUTF(aEnv, aErrStr);
    jthrowable tException = (jthrowable)(*aEnv)->NewObject(aEnv, tClazz, tInit, aErrCode, tJErrStr);
    (*aEnv)->Throw(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tException);
    (*aEnv)->DeleteLocalRef(aEnv, tJErrStr);
    (*aEnv)->DeleteLocalRef(aEnv, tClazz);
}


JNIEXPORT jlong JNICALL Java_jse_jit_SimpleJIT_loadLibrary0(JNIEnv *aEnv, jclass aClazz, jstring aLibPath) {
#if defined(WIN32) || defined(_WIN64) || defined(_WIN32)
    const jchar *tLibPath = (*aEnv)->GetStringChars(aEnv, aLibPath, NULL);
    if (!tLibPath) return 0; // OOM
    HMODULE tHandle = LoadLibraryW((LPCWSTR)tLibPath);
    (*aEnv)->ReleaseStringChars(aEnv, aLibPath, tLibPath);
    if (!tHandle) {
        throwExceptionJITCode(aEnv, (jint)GetLastError(), "LoadLibraryW failed");
        return 0;
    }
    return (jlong)(intptr_t)tHandle;
#else
    const char *tLibPath = (*aEnv)->GetStringUTFChars(aEnv, aLibPath, NULL);
    if (!tLibPath) return 0; // OOM
    void *tHandle = dlopen(tLibPath, RTLD_NOW | RTLD_LOCAL);
    (*aEnv)->ReleaseStringUTFChars(aEnv, aLibPath, tLibPath);
    if (!tHandle) {
        const char *tErr = dlerror();
        throwExceptionJIT(aEnv, tErr?tErr:"dlopen failed");
        return 0;
    }
    return (jlong)(intptr_t)tHandle;
#endif
}

JNIEXPORT void JNICALL Java_jse_jit_JITLibHandle_freeLibrary0(JNIEnv *aEnv, jclass aClazz, jlong aLibHandle) {
#if defined(WIN32) || defined(_WIN64) || defined(_WIN32)
    FreeLibrary((HMODULE)(intptr_t)aLibHandle);
#else
    dlclose((void *)(intptr_t)aLibHandle);
#endif
}

JNIEXPORT jlong JNICALL Java_jse_jit_SimpleJIT_findMethod0(JNIEnv *aEnv, jclass aClazz, jlong aLibHandle, jstring aMethodName) {
#if defined(WIN32) || defined(_WIN64) || defined(_WIN32)
    const char *tMethodName = (*aEnv)->GetStringUTFChars(aEnv, aMethodName, NULL);
    if (!tMethodName) return 0; // OOM
    FARPROC tMethodPtr = GetProcAddress((HMODULE)(intptr_t)aLibHandle, (LPCSTR)tMethodName);
    (*aEnv)->ReleaseStringUTFChars(aEnv, aMethodName, tMethodName);
    if (!tMethodPtr) {
        throwExceptionJITCode(aEnv, (jint)GetLastError(), "GetProcAddress failed");
        return 0;
    }
    return (jlong)(intptr_t)tMethodPtr;
#else
    const char *tMethodName = (*aEnv)->GetStringUTFChars(aEnv, aMethodName, NULL);
    if (!tMethodName) return 0; // OOM
    void* tMethodPtr = dlsym((void *)(intptr_t)aLibHandle, tMethodName);
    (*aEnv)->ReleaseStringUTFChars(aEnv, aMethodName, tMethodName);
    if (!tMethodPtr) {
        const char *tErr = dlerror();
        throwExceptionJIT(aEnv, tErr?tErr:"dlsym failed");
        return 0;
    }
    return (jlong)(intptr_t)tMethodPtr;
#endif
}

JNIEXPORT jint JNICALL Java_jse_jit_SimpleJIT_invokeMethod0(JNIEnv *aEnv, jclass aClazz, jlong aMethodPtr, jlong aInPtr, jlong aOutPtr) {
    plugin_method_t tMethod = (plugin_method_t)(intptr_t)aMethodPtr;
    return (jint)tMethod((void *)(intptr_t)aInPtr, (void *)(intptr_t)aOutPtr);
}

