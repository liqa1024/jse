#include "jniutil.h"
#include "pair_jse.h"

#include "LmpPair.h"
#include "lammps/atom.h"
#include "lammps/comm.h"
#include "lammps/error.h"
#include "lammps/force.h"
#include "lammps/memory.h"
#include "lammps/neigh_list.h"
#include "lammps/neighbor.h"

using namespace LAMMPS_NS;

/* ---------------------------------------------------------------------- */

PairJSE::PairJSE(LAMMPS *lmp) : Pair(lmp) {
    single_enable = 0;
    restartinfo = 0;
    manybody_flag = 1;
}

PairJSE::~PairJSE() {
    if (allocated) {
        memory->destroy(setflag);
        memory->destroy(cutsq);
    }
    if (core!=NULL && env!=NULL) {
        env->DeleteGlobalRef(core);
        JSE_LMPPAIR::uncacheJClass(env);
    }
}

/* ---------------------------------------------------------------------- */

void PairJSE::compute(int eflag, int vflag) {
    JSE_LMPPAIR::compute(env, core, eflag, vflag);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        error->all(FLERR, "Fail to compute");
    }
}

void PairJSE::allocate() {
    allocated = 1;
    int n = atom->ntypes + 1;
    
    memory->create(setflag, n, n, "pair:setflag");
    for (int i = 1; i < n; ++i) for (int j = i; j < n; ++j) setflag[i][j] = 1;
    
    memory->create(cutsq, n, n, "pair:cutsq");
}


/** global settings, init LmpPair here */
void PairJSE::settings(int narg, char **arg) {
    if (narg != 1) error->all(FLERR, "Illegal pair_style command");
    
    // init jni env
    if (env == NULL) {
        JavaVM *jvm;
        jsize nVMs;
        JNI_GetCreatedJavaVMs(&jvm, 1, &nVMs);
        if (jvm == NULL) error->all(FLERR, "pair_style jse can not run without jse yet");
        jvm->AttachCurrentThreadAsDaemon((void**)&env, NULL);
        if (env == NULL) error->all(FLERR, "Fail to get jni env");
    }
    // init java LmpPair object
    if (!JSE_LMPPAIR::cacheJClass(env)) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        error->all(FLERR, "Fail to cache class of java LmpPair");
    }
    jobject obj = JSE_LMPPAIR::newJObject(env, arg[0], this);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        error->all(FLERR, "Fail to create java LmpPair object");
    }
    if (obj == NULL) error->all(FLERR, "Fail to create java LmpPair object");
    if (core != NULL) env->DeleteGlobalRef(core);
    core = env->NewGlobalRef(obj);
    env->DeleteLocalRef(obj);
}

/* ---------------------------------------------------------------------- */

void PairJSE::coeff(int narg, char **arg) {
    if (!allocated) allocate();
    JSE_LMPPAIR::coeff(env, core, narg, arg);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        error->all(FLERR, "Fail to set coeff");
    }
}

void PairJSE::init_style() {
    JSE_LMPPAIR::initStyle(env, core);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        error->all(FLERR, "Fail to init_style");
    }
}

double PairJSE::init_one(int i, int j) {
    double cutij = JSE_LMPPAIR::initOne(env, core, i, j);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        error->all(FLERR, "Fail to init_one");
    }
    if (cutij <= 0.0) error->all(FLERR, "Fail to init_one");
    return cutij;
}

/* ---------------------------------------------------------------------- */

void PairJSE::neighborRequestDefault() {
    neighbor->add_request(this, NeighConst::REQ_DEFAULT);
}
void PairJSE::neighborRequestFull() {
    neighbor->add_request(this, NeighConst::REQ_FULL);
}
void PairJSE::evInit(jboolean eflag, jboolean vflag) {
    ev_init((int)eflag, (int)vflag);
}
jlong PairJSE::atomX() {
    return (jlong)(intptr_t) atom->x;
}
jlong PairJSE::atomF() {
    return (jlong)(intptr_t) atom->f;
}
jlong PairJSE::atomType() {
    return (jlong)(intptr_t) atom->type;
}
jint PairJSE::atomNtypes() {
    return (jint) atom->ntypes;
}
jint PairJSE::atomNlocal() {
    return (jint) atom->nlocal;
}
jint PairJSE::atomNghost() {
    return (jint) atom->nghost;
}
jlong PairJSE::forceSpecialLj() {
    return (jlong)(intptr_t) force->special_lj;
}
jboolean PairJSE::forceNewtonPair() {
    return force->newton_pair ? JNI_TRUE : JNI_FALSE;
}
jint PairJSE::listInum() {
    return (jint) list->inum;
}
jlong PairJSE::listIlist() {
    return (jlong)(intptr_t) list->ilist;
}
jlong PairJSE::listNumneigh() {
    return (jlong)(intptr_t) list->numneigh;
}
jlong PairJSE::listFirstneigh() {
    return (jlong)(intptr_t) list->firstneigh;
}
jdouble PairJSE::cutsq_(jint i, jint j) {
    return (jdouble) cutsq[i][j];
}
void PairJSE::evTally(jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ev_tally((int)i, (int)j, (int)nlocal, (int)newtonPair, (double)evdwl, (double)ecoul, (double)fpair, (double)delx, (double)dely, (double)delz);
}
jboolean PairJSE::evflag_() {
    return evflag ? JNI_TRUE : JNI_FALSE;
}
jboolean PairJSE::vflagFdotr() {
    return vflag_fdotr ? JNI_TRUE : JNI_FALSE;
}
void PairJSE::virialFdotrCompute() {
    virial_fdotr_compute();
}
jint PairJSE::commMe() {
    return (jint) comm->me;
}
jint PairJSE::commNprocs() {
    return (jint) comm->nprocs;
}
