#include "jniutil.h"
#include "pair_jse.h"

#include "LmpPlugin.h"
#include "LmpPair.h"

#include "lammps/atom.h"
#include "lammps/comm.h"
#include "lammps/error.h"
#include "lammps/force.h"
#include "lammps/memory.h"
#include "lammps/update.h"
#include "lammps/neigh_list.h"
#include "lammps/neighbor.h"

using namespace LAMMPS_NS;

/* ---------------------------------------------------------------------- */

PairJSE::PairJSE(LAMMPS *aLmp) : Pair(aLmp) {
    single_enable = 0;
    restartinfo = 0;
    manybody_flag = 1;
}

PairJSE::~PairJSE() {
    if (allocated) {
        memory->destroy(setflag);
        memory->destroy(cutsq);
    }
    if (mCore != NULL && mEnv != NULL) {
        JSE_LMPPAIR::shutdown(mEnv, mCore);
        // only check, no error on destructor
        JSE_LMPPLUGIN::exceptionCheck(mEnv);
        
        mEnv->DeleteGlobalRef(mCore);
        mCore = NULL;
        JSE_LMPPAIR::uncacheJClass(mEnv);
    }
}

/* ---------------------------------------------------------------------- */

void PairJSE::compute(int eflag, int vflag) {
    ev_init(eflag, vflag);
    JSE_LMPPAIR::compute(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to compute");
}

void PairJSE::allocate() {
    allocated = 1;
    int n = atom->ntypes + 1;
    
    memory->create(setflag, n, n, "pair:setflag");
    for (int i = 1; i < n; ++i) for (int j = i; j < n; ++j) setflag[i][j] = 1;
    
    memory->create(cutsq, n, n, "pair:cutsq");
}

/** global settings, init LmpPair here */
void PairJSE::settings(int aArgc, char **aArgv) {
    if (aArgc != 1) error->all(FLERR, "Illegal pair_style jse command");
    
    // init jni env
    if (mEnv == NULL) {
        jboolean tSuc = JSE_LMPPLUGIN::initJVM(&mEnv);
        if (!tSuc) error->all(FLERR, "Fail to init jvm");
        if (mEnv == NULL) error->all(FLERR, "Fail to get jni env");
    }
    // init java LmpPair object
    jboolean tSuc = JSE_LMPPAIR::cacheJClass(mEnv);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv) || !tSuc) error->all(FLERR, "Fail to cache class of java LmpPair");
    jobject tObj = JSE_LMPPAIR::newJObject(mEnv, aArgv[0], this);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv) || tObj==NULL) error->all(FLERR, "Fail to create java LmpPair object");
    if (mCore != NULL) mEnv->DeleteGlobalRef(mCore);
    mCore = mEnv->NewGlobalRef(tObj);
    mEnv->DeleteLocalRef(tObj);
}

/* ---------------------------------------------------------------------- */

void PairJSE::coeff(int aArgc, char **aArgv) {
    if (!allocated) allocate();
    JSE_LMPPAIR::coeff(mEnv, mCore, aArgc, aArgv);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to set coeff");
}

void PairJSE::init_style() {
    JSE_LMPPAIR::initStyle(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to init_style");
}

double PairJSE::init_one(int i, int j) {
    double cutij = JSE_LMPPAIR::initOne(mEnv, mCore, i, j);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv) || cutij<=0.0) error->all(FLERR, "Fail to init_one");
    return cutij;
}

/* ---------------------------------------------------------------------- */

void PairJSE::neighborRequestDefault() {
    neighbor->add_request(this, NeighConst::REQ_DEFAULT);
}
void PairJSE::neighborRequestFull() {
    neighbor->add_request(this, NeighConst::REQ_FULL);
}
void PairJSE::noVirialFdotrCompute() {
    no_virial_fdotr_compute = 1;
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
jlong PairJSE::engVdwl() {
    return (jlong)(intptr_t) &eng_vdwl;
}
jlong PairJSE::engCoul() {
    return (jlong)(intptr_t) &eng_coul;
}
jlong PairJSE::eatom_() {
    return (jlong)(intptr_t) eatom;
}
jlong PairJSE::virial_() {
    return (jlong)(intptr_t) virial;
}
jlong PairJSE::vatom_() {
    return (jlong)(intptr_t) vatom;
}
void PairJSE::evTally(jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ev_tally((int)i, (int)j, (int)nlocal, (int)newtonPair, (double)evdwl, (double)ecoul, (double)fpair, (double)delx, (double)dely, (double)delz);
}
void PairJSE::evTallyFull(jint i, jdouble evdwl, jdouble ecoul, jdouble fpair, jdouble delx, jdouble dely, jdouble delz) {
    ev_tally_full((int)i, (double)evdwl, (double)ecoul, (double)fpair, (double)delx, (double)dely, (double)delz);
}
void PairJSE::evTallyXYZ(jint i, jint j, jint nlocal, jboolean newtonPair, jdouble evdwl, jdouble ecoul, jdouble fx, jdouble fy, jdouble fz, jdouble delx, jdouble dely, jdouble delz) {
    ev_tally_xyz((int)i, (int)j, (int)nlocal, (int)newtonPair, (double)evdwl, (double)ecoul, (double)fx, (double)fy, (double)fz, (double)delx, (double)dely, (double)delz);
}
void PairJSE::evTallyXYZFull(jint i, jdouble evdwl, jdouble ecoul, jdouble fx, jdouble fy, jdouble fz, jdouble delx, jdouble dely, jdouble delz) {
    ev_tally_xyz_full((int)i, (double)evdwl, (double)ecoul, (double)fx, (double)fy, (double)fz, (double)delx, (double)dely, (double)delz);
}
jboolean PairJSE::evflag_() {
    return evflag ? JNI_TRUE : JNI_FALSE;
}
jboolean PairJSE::vflagEither() {
    return vflag_either ? JNI_TRUE : JNI_FALSE;
}
jboolean PairJSE::vflagGlobal() {
    return vflag_global ? JNI_TRUE : JNI_FALSE;
}
jboolean PairJSE::vflagAtom() {
    return vflag_atom ? JNI_TRUE : JNI_FALSE;
}
jboolean PairJSE::eflagEither() {
    return eflag_either ? JNI_TRUE : JNI_FALSE;
}
jboolean PairJSE::eflagGlobal() {
    return eflag_global ? JNI_TRUE : JNI_FALSE;
}
jboolean PairJSE::eflagAtom() {
    return eflag_atom ? JNI_TRUE : JNI_FALSE;
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
jlong PairJSE::commWorld() {
    return (jlong)(intptr_t)world;
}
jstring PairJSE::unitStyle() {
    char *tUnits = lmp->update->unit_style;
    return tUnits!=NULL ? mEnv->NewStringUTF(tUnits) : NULL;
}
