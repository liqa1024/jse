#ifndef LMPPLUGIN_LMPPLUGIN_H
#define LMPPLUGIN_LMPPLUGIN_H

#include <jni.h>

namespace JSE_LMPPLUGIN {
    jboolean initJVM(JNIEnv **);
    jboolean exceptionCheck(JNIEnv *);
}

#endif //LMPPLUGIN_LMPPLUGIN_H
