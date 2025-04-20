#ifndef LMPPAIR_H
#define LMPPAIR_H

#include <jni.h>

namespace JSE_LMPPAIR {

extern jclass LMPPAIR_CLAZZ;
extern jclass STRING_CLAZZ;

jobject newJObject(JNIEnv *, char *, void *);

void compute(JNIEnv *, jobject);
double single(JNIEnv *, jobject, int, int, int, int, double, double, double, double &);
void coeff(JNIEnv *, jobject, int, char **);
void settings(JNIEnv *, jobject, int, char **);
void initStyle(JNIEnv *, jobject);
double initOne(JNIEnv *, jobject, int, int);
void shutdown(JNIEnv *, jobject);

/** cache jclass */
jboolean cacheJClass(JNIEnv *);
void uncacheJClass(JNIEnv *);

}

#endif //LMPPAIR_H
