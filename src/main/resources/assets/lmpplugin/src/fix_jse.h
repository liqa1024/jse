#ifdef FIX_CLASS
// clang-format off
FixStyle(jse,FixJSE);
// clang-format on
#else

#ifndef LMP_FIX_JSE_H
#define LMP_FIX_JSE_H

#include <jni.h>
#include "lammps/fix.h"

namespace LAMMPS_NS {

class FixJSE : public Fix {
public:
    FixJSE(class LAMMPS *, int, char **);
    ~FixJSE() override;
    int setmask() override;
    void init() override;
    void init_list(int, NeighList *) override;
    void setup(int) override;
    void min_setup(int) override;
    
    void initial_integrate(int) override;
    void post_integrate() override;
    void pre_exchange() override;
    void pre_neighbor() override;
    void post_neighbor() override;
    void pre_force(int) override;
    void pre_reverse(int, int) override;
    void post_force(int) override;
    void final_integrate() override;
    void end_of_step() override;
    void post_run() override;
    void min_pre_exchange() override;
    void min_pre_neighbor() override;
    void min_post_neighbor() override;
    void min_pre_force(int) override;
    void min_pre_reverse(int, int) override;
    void min_post_force(int) override;
    
    double compute_scalar() override;
    double compute_vector(int) override;
    double compute_array(int, int) override;

protected:
    JNIEnv *mEnv = NULL;
    jobject mCore = NULL;
    NeighList *mNL = NULL;

public:
    void setForceReneighbor(jboolean);
    void setNextReneighbor(jlong);
    jlong nextReneighbor();
    void setNevery(jint);
    void setEnergyGlobalFlag(jboolean);
    void setEnergyPeratomFlag(jboolean);
    void setVirialGlobalFlag(jboolean);
    void setVirialPeratomFlag(jboolean);
    void setTimeDepend(jboolean);
    void setDynamicGroupAllow(jboolean);
    void setScalarFlag(jboolean);
    void setVectorFlag(jboolean);
    void setArrayFlag(jboolean);
    void setSizeVector(jint);
    void setSizeArrayRows(jint);
    void setSizeArrayCols(jint);
    void setGlobalFreq(jint);
    void setExtscalar(jboolean);
    void setExtvector(jboolean);
    void setExtarray(jboolean);
    
    void neighborRequestDefault(jdouble);
    void neighborRequestFull(jdouble);
    void neighborRequestOccasional(jdouble);
    void neighborRequestOccasionalFull(jdouble);
    void neighborBuildOne();
    jdouble neighborCutneighmin();
    jdouble neighborCutneighmax();
    jdouble neighborCuttype(jint);
    jdouble neighborSkin();
    jlong atomX();
    jlong atomF();
    jlong atomType();
    jint atomNtypes();
    jint atomNlocal();
    jint atomNghost();
    jint listInum();
    jlong listIlist();
    jlong listNumneigh();
    jlong listFirstneigh();
    jdouble forceBoltz();
    jdouble dt();
    jlong ntimestep();
    jlong firststep();
    jlong laststep();
    jlong beginstep();
    jlong endstep();
    jint commMe();
    jint commNprocs();
    jlong commWorld();
    jdouble commCutghostuser();
    jstring unitStyle();
};

}    // namespace LAMMPS_NS

#endif
#endif
