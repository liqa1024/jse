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
    double single(int, int, int, int, double, double, double, double &) override;
    void settings(int, char **) override;
    void coeff(int, char **) override;
    void init_style() override;
    double init_one(int, int) override;

    int pack_forward_comm(int, int *, double *, int, int *) override;
    void unpack_forward_comm(int, int, double *) override;
    int pack_reverse_comm(int, int, double *) override;
    void unpack_reverse_comm(int, int *, double *) override;

protected:
    JNIEnv *mEnv = NULL;
    jobject mCore = NULL;
    
    virtual void allocate();
    
public:
    void setSingleEnable(jboolean);
    void setOneCoeff(jboolean);
    void setManybodyFlag(jboolean);
    void setUnitConvertFlag(jint);
    void setNoVirialFdotrCompute(jboolean);
    void setFinitecutflag(jboolean);
    void setGhostneigh(jboolean);
    void setCentroidstressflag(jint);
    void setCommForward(jint);
    void setCommReverse(jint);
    void setCommReverseOff(jint);

    jint findVariable(jstring);
    jdouble computeVariable(jint);
    void neighborRequestDefault();
    void neighborRequestFull();
    jlong atomX();
    jlong atomV();
    jlong atomF();
    jlong atomTag();
    jlong atomType();
    jlong atomMass();
    jlong atomExtract(jstring);
    jlong atomNatoms();
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
    jlong cvatom_();
    void evTally(jint, jint, jint, jboolean, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    void evTallyFull(jint, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    void evTallyXYZ(jint, jint, jint, jboolean, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    void evTallyXYZFull(jint, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble, jdouble);
    jboolean evflag_();
    jboolean vflagEither();
    jboolean vflagGlobal();
    jboolean vflagAtom();
    jboolean cvflagAtom();
    jboolean eflagEither();
    jboolean eflagGlobal();
    jboolean eflagAtom();
    jboolean vflagFdotr();
    void virialFdotrCompute();
    jint commMe();
    jint commNprocs();
    jlong commWorld();
    void commForwardComm();
    void commForwardCommThis();
    void commReverseComm();
    void commReverseCommThis();
    jstring unitStyle();
};

}    // namespace LAMMPS_NS

#endif
#endif
