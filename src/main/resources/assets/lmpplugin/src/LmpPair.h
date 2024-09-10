#ifndef LMPPAIR_H
#define LMPPAIR_H

namespace JSE_LMPPAIR {

extern jclass LMPPAIR_CLAZZ;
extern jclass STRING_CLAZZ;

jobject newJObject(JNIEnv *, char *, void *);

void compute(JNIEnv *, jobject, int, int);
void coeff(JNIEnv *, jobject, int, char **);
void initStyle(JNIEnv *, jobject);
double initOne(JNIEnv *, jobject, int, int);

/** cache jclass */
jboolean cacheJClass(JNIEnv *);
void uncacheJClass(JNIEnv *);

}

#endif //LMPPAIR_H
