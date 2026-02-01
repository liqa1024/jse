#include "LmpPlugin.h"

#include "lammps/platform.h"

#include "jniutil.h"
#include <mpi.h>

using namespace LAMMPS_NS;

#ifdef JVM_LIB_PATH
static void *sJvmDllHandle = NULL;
#endif

jboolean JSE_LMPPLUGIN::initJVM(JNIEnv **rEnv) {
#ifdef JVM_LIB_PATH
    if (sJvmDllHandle == NULL) {
        sJvmDllHandle = platform::dlopen(JVM_LIB_PATH);
        if (sJvmDllHandle == NULL) {
            fprintf(stderr, "Fail to open jvm lib in %s\n", JVM_LIB_PATH);
            return JNI_FALSE;
        }
    }
#endif
    JavaVM *tJVM = NULL;
    jsize tNVMs;
    JNI_GetCreatedJavaVMs(&tJVM, 1, &tNVMs);
    if (tJVM != NULL) {
        (tJVM)->AttachCurrentThreadAsDaemon((void**)rEnv, NULL);
        return JNI_TRUE;
    }
    JavaVMInitArgs tVMArgs;
    JavaVMOption tOptions[3];
    tOptions[0].optionString = (char *)JVM_CLASS_PATH;
    tOptions[1].optionString = (char *)JVM_XMX;
    tOptions[2].optionString = (char *)"--enable-native-access=ALL-UNNAMED";
    tVMArgs.version = JNI_VERSION_1_6;
    tVMArgs.nOptions = 3;
    tVMArgs.options = tOptions;
    tVMArgs.ignoreUnrecognized = JNI_TRUE;
    
    jint tOut = JNI_CreateJavaVM(&tJVM, (void**)rEnv, &tVMArgs);
    return tOut==JNI_OK ? JNI_TRUE : JNI_FALSE;
}

jboolean JSE_LMPPLUGIN::exceptionCheck(JNIEnv *aEnv) {
    if (aEnv->ExceptionCheck()) {
        aEnv->ExceptionDescribe();
        aEnv->ExceptionClear();
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

jboolean JSE_LMPPLUGIN::exceptionCheckMPI(JNIEnv *aEnv, int aExitCode) {
#ifdef LAMMPS_LIB_MPI
    if (aExitCode==MPI_SUCCESS) return JNI_FALSE;
    char rErrStr[MPI_MAX_ERROR_STRING];
    int rLen;
    MPI_Error_string(aExitCode, rErrStr, &rLen);
    throwExceptionMPI(aEnv, rErrStr, aExitCode);
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}


