#ifndef LMPFIX_H
#define LMPFIX_H

#include <jni.h>

namespace JSE_LMPFIX {

extern jclass LMPFIX_CLAZZ;
extern jclass STRING_CLAZZ;

jobject newJObject(JNIEnv *, char *, void *, int, char **);

int setMask(JNIEnv *, jobject);
void init(JNIEnv *, jobject);
void setup(JNIEnv *, jobject, int);
void minSetup(JNIEnv *, jobject, int);
void shutdown(JNIEnv *, jobject);

void initialIntegrate(JNIEnv *, jobject, int);
void postIntegrate(JNIEnv *, jobject);
void preExchange(JNIEnv *, jobject);
void preNeighbor(JNIEnv *, jobject);
void postNeighbor(JNIEnv *, jobject);
void preForce(JNIEnv *, jobject, int);
void preReverse(JNIEnv *, jobject, int, int);
void postForce(JNIEnv *, jobject, int);
void finalIntegrate(JNIEnv *, jobject);
void endOfStep(JNIEnv *, jobject);
void postRun(JNIEnv *, jobject);
void minPreExchange(JNIEnv *, jobject);
void minPreNeighbor(JNIEnv *, jobject);
void minPostNeighbor(JNIEnv *, jobject);
void minPreForce(JNIEnv *, jobject, int);
void minPreReverse(JNIEnv *, jobject, int, int);
void minPostForce(JNIEnv *, jobject, int);

double computeScalar(JNIEnv *, jobject);
double computeVector(JNIEnv *, jobject, int);
double computeArray(JNIEnv *, jobject, int, int);

/** cache jclass */
jboolean cacheJClass(JNIEnv *);
void uncacheJClass(JNIEnv *);

}

#endif //LMPFIX_H
