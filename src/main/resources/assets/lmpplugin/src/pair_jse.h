#ifdef PAIR_CLASS
// clang-format off
PairStyle(jse,PairJSE);
// clang-format on
#else

#ifndef LMP_PAIR_JSE_H
#define LMP_PAIR_JSE_H

#include <jni.h>
#include "lammps/pair.h"

namespace LAMMPS_NS {

class PairJSE : public Pair {
public:
    PairJSE(class LAMMPS *);
    ~PairJSE() override;
    void compute(int, int) override;
    void settings(int, char **) override;
    void coeff(int, char **) override;
    void init_style() override;
    double init_one(int, int) override;

protected:
    JNIEnv *mEnv = NULL;
    jobject mCore = NULL;
    
    virtual void allocate();
    
public:
    jint findVariable(jstring);
    jdouble computeVariable(jint);
    void neighborRequestDefault();
    void neighborRequestFull();
    void noVirialFdotrCompute();
    jlong atomX();
    jlong atomF();
    jlong atomType();
    jint atomNtypes();
    jint atomNlocal();
    jint atomNghost();
    jint atomNmax();
    jlong forceSpecialLj();
    jboolean forceNewtonPair();
    jint listGnum();
    jint listInum();
    jlong listIlist();
    jlong listNumneigh();
    jlong listFirstneigh();
    jdouble cutsq_(jint, jint);
    jlong engVdwl();
    jlong engCoul();
    jlong eatom_();
    jlong virial_();
    jlong vatom_();
    void evTally(jint, jint, jint, jboolean, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    void evTallyFull(jint, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    void evTallyXYZ(jint, jint, jint, jboolean, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    void evTallyXYZFull(jint, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    jboolean evflag_();
    jboolean vflagEither();
    jboolean vflagGlobal();
    jboolean vflagAtom();
    jboolean eflagEither();
    jboolean eflagGlobal();
    jboolean eflagAtom();
    jboolean vflagFdotr();
    void virialFdotrCompute();
    jint commMe();
    jint commNprocs();
    jlong commWorld();
    jstring unitStyle();
};

}    // namespace LAMMPS_NS

#endif
#endif
