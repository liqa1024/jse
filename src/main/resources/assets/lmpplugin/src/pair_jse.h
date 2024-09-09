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
    JNIEnv *env = NULL;
    jobject core = NULL;
    
    virtual void allocate();
    
public:
    void neighborRequestDefault();
    void neighborRequestFull();
    void evInit(jboolean, jboolean);
    jlong atomX();
    jlong atomF();
    jlong atomType();
    jint atomNtypes();
    jint atomNlocal();
    jint atomNghost();
    jlong forceSpecialLj();
    jboolean forceNewtonPair();
    jint listInum();
    jlong listIlist();
    jlong listNumneigh();
    jlong listFirstneigh();
    jdouble cutsq_(jint, jint);
    void evTally(jint, jint, jint, jboolean, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    jboolean evflag_();
    jboolean vflagFdotr();
    void virialFdotrCompute();
    jint commMe();
    jint commNprocs();
};

}    // namespace LAMMPS_NS

#endif
#endif
