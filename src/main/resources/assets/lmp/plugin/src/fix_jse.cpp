#include "fix_jse.h"

#include "LmpPlugin.h"
#include "LmpFix.h"

#include "lammps/atom.h"
#include "lammps/comm.h"
#include "lammps/citeme.h"
#include "lammps/domain.h"
#include "lammps/error.h"
#include "lammps/force.h"
#include "lammps/input.h"
#include "lammps/pair.h"
#include "lammps/update.h"
#include "lammps/neigh_list.h"
#include "lammps/neighbor.h"
#include "lammps/variable.h"
#include "neigh_request.h"

#include "jniutil.h"

using namespace LAMMPS_NS;

/* ---------------------------------------------------------------------- */

FixJSE::FixJSE(LAMMPS *aLmp, int aArgc, char **aArgv) : Fix(aLmp, aArgc, aArgv) {
    if (aArgc < 4) error->all(FLERR, "Illegal fix jse command");
    
    // init jni env
    if (mEnv == NULL) {
        jboolean tSuc = JSE_LMPPLUGIN::initJVM(&mEnv);
        if (!tSuc) error->all(FLERR, "Fail to init jvm");
        if (mEnv == NULL) error->all(FLERR, "Fail to get jni env");
    }
    // init java LmpFix object
    jboolean tSuc = JSE_LMPFIX::cacheJClass(mEnv);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv) || !tSuc) error->all(FLERR, "Fail to cache class of java LmpFix");
    jobject tObj = JSE_LMPFIX::newJObject(mEnv, aArgv[3], this, aArgc, aArgv);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv) || tObj==NULL) error->all(FLERR, "Fail to create java LmpFix object");
    if (mCore != NULL) mEnv->DeleteGlobalRef(mCore);
    mCore = mEnv->NewGlobalRef(tObj);
    mEnv->DeleteLocalRef(tObj);
    
    // fix setting now in java LmpFix constructor
}

FixJSE::~FixJSE() {
    if (mCore != NULL && mEnv != NULL) {
        JSE_LMPFIX::shutdown(mEnv, mCore);
        // only check, no error on destructor
        JSE_LMPPLUGIN::exceptionCheck(mEnv);
        
        mEnv->DeleteGlobalRef(mCore);
        mCore = NULL;
        JSE_LMPFIX::uncacheJClass(mEnv);
    }
}

/* ---------------------------------------------------------------------- */

int FixJSE::setmask() {
    int mask = JSE_LMPFIX::setMask(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to setmask");
    return mask;
}

void FixJSE::init() {
    JSE_LMPFIX::init(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to init");
}
void FixJSE::init_list(int id, NeighList *ptr) {
    mNL = ptr;
}

void FixJSE::setup(int vflag) {
    JSE_LMPFIX::setup(mEnv, mCore, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call setup");
}
void FixJSE::min_setup(int vflag) {
    JSE_LMPFIX::minSetup(mEnv, mCore, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call min_setup");
}

/* ---------------------------------------------------------------------- */

void FixJSE::initial_integrate(int vflag) {
    JSE_LMPFIX::initialIntegrate(mEnv, mCore, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call initial_integrate");
}
void FixJSE::post_integrate() {
    JSE_LMPFIX::postIntegrate(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call post_integrate");
}
void FixJSE::pre_exchange() {
    JSE_LMPFIX::preExchange(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call pre_exchange");
}
void FixJSE::pre_neighbor() {
    JSE_LMPFIX::preNeighbor(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call pre_neighbor");
}
void FixJSE::post_neighbor() {
    JSE_LMPFIX::postNeighbor(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call post_neighbor");
}
void FixJSE::pre_force(int vflag) {
    JSE_LMPFIX::preForce(mEnv, mCore, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call pre_force");
}
void FixJSE::pre_reverse(int eflag, int vflag) {
    JSE_LMPFIX::preReverse(mEnv, mCore, eflag, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call pre_reverse");
}
void FixJSE::post_force(int vflag) {
    JSE_LMPFIX::postForce(mEnv, mCore, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call post_force");
}
void FixJSE::final_integrate() {
    JSE_LMPFIX::finalIntegrate(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call final_integrate");
}
void FixJSE::end_of_step() {
    JSE_LMPFIX::endOfStep(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call end_of_step");
}
void FixJSE::post_run() {
    JSE_LMPFIX::postRun(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call post_run");
}
void FixJSE::min_pre_exchange() {
    JSE_LMPFIX::minPreExchange(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call min_pre_exchange");
}
void FixJSE::min_pre_neighbor() {
    JSE_LMPFIX::minPreNeighbor(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call min_pre_neighbor");
}
void FixJSE::min_post_neighbor() {
    JSE_LMPFIX::minPostNeighbor(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call min_post_neighbor");
}
void FixJSE::min_pre_force(int vflag) {
    JSE_LMPFIX::minPreForce(mEnv, mCore, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call min_pre_force");
}
void FixJSE::min_pre_reverse(int eflag, int vflag) {
    JSE_LMPFIX::minPreReverse(mEnv, mCore, eflag, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call min_pre_reverse");
}
void FixJSE::min_post_force(int vflag) {
    JSE_LMPFIX::minPostForce(mEnv, mCore, vflag);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to call min_post_force");
}

int FixJSE::pack_forward_comm(int n, int *list, double *buf, int pbc_flag, int *pbc) {
    int out = JSE_LMPFIX::packForwardComm(mEnv, mCore, n, list, buf, pbc_flag, pbc);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to pack_forward_comm");
    return out;
}
void FixJSE::unpack_forward_comm(int n, int first, double *buf) {
    JSE_LMPFIX::unpackForwardComm(mEnv, mCore, n, first, buf);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to unpack_forward_comm");
}
int FixJSE::pack_reverse_comm(int n, int first, double *buf) {
    int out = JSE_LMPFIX::packReverseComm(mEnv, mCore, n, first, buf);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to pack_reverse_comm");
    return out;
}
void FixJSE::unpack_reverse_comm(int n, int *list, double *buf) {
    JSE_LMPFIX::unpackReverseComm(mEnv, mCore, n, list, buf);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to unpack_reverse_comm");
}

double FixJSE::compute_scalar() {
    double out = JSE_LMPFIX::computeScalar(mEnv, mCore);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to compute_scalar");
    return out;
}
double FixJSE::compute_vector(int i) {
    double out = JSE_LMPFIX::computeVector(mEnv, mCore, i);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to compute_vector");
    return out;
}
double FixJSE::compute_array(int i, int j) {
    double out = JSE_LMPFIX::computeArray(mEnv, mCore, i, j);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) error->all(FLERR, "Fail to compute_array");
    return out;
}

/* ---------------------------------------------------------------------- */
jint FixJSE::findVariable(jstring name) {
    const char *name_c = mEnv->GetStringUTFChars(name, NULL);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) return -1;
    int ivar = input->variable->find(name_c);
    mEnv->ReleaseStringUTFChars(name, name_c);
    return (jint)ivar;
}
jdouble FixJSE::computeVariable(jint ivar) {
    return input->variable->compute_equal(ivar);
}
void FixJSE::citemeAdd(jstring cite) {
    const char *cite_c = mEnv->GetStringUTFChars(cite, NULL);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) return;
    lmp->citeme->add(cite_c);
    mEnv->ReleaseStringUTFChars(cite, cite_c);
}

void FixJSE::setForceReneighbor(jboolean flag) {
    force_reneighbor = flag ? 1 : 0;
}
void FixJSE::setNextReneighbor(jlong timestep) {
    next_reneighbor = (bigint)timestep;
}
void FixJSE::setBoxChange(jint flag) {
    box_change = (int)flag;
}
void FixJSE::setNoChangeBox(jboolean flag) {
    no_change_box = flag ? 1 : 0;
}
jlong FixJSE::nextReneighbor() {
    return (jlong)next_reneighbor;
}
void FixJSE::setNevery(jint n) {
    nevery = n;
}
void FixJSE::setEnergyGlobalFlag(jboolean flag) {
    energy_global_flag = flag ? 1 : 0;
}
void FixJSE::setEnergyPeratomFlag(jboolean flag) {
    energy_peratom_flag = flag ? 1 : 0;
}
void FixJSE::setVirialGlobalFlag(jboolean flag) {
    virial_global_flag = flag ? 1 : 0;
}
void FixJSE::setVirialPeratomFlag(jboolean flag) {
    virial_peratom_flag = flag ? 1 : 0;
}
void FixJSE::setTimeDepend(jboolean flag) {
    time_depend = flag ? 1 : 0;
}
void FixJSE::setDynamicGroupAllow(jboolean flag) {
    dynamic_group_allow = flag ? 1 : 0;
}
void FixJSE::setScalarFlag(jboolean flag) {
    scalar_flag = flag ? 1 : 0;
}
void FixJSE::setVectorFlag(jboolean flag) {
    vector_flag = flag ? 1 : 0;
}
void FixJSE::setArrayFlag(jboolean flag) {
    array_flag = flag ? 1 : 0;
}
void FixJSE::setSizeVector(jint size) {
    size_vector = (int)size;
}
void FixJSE::setSizeArrayRows(jint size) {
    size_array_rows = (int)size;
}
void FixJSE::setSizeArrayCols(jint size) {
    size_array_cols = (int)size;
}
void FixJSE::setGlobalFreq(jint size) {
    global_freq = (int)size;
}
void FixJSE::setExtscalar(jboolean flag) {
    extscalar = flag ? 1 : 0;
}
void FixJSE::setExtvector(jboolean flag) {
    extvector = flag ? 1 : 0;
}
void FixJSE::setExtarray(jboolean flag) {
    extarray = flag ? 1 : 0;
}
void FixJSE::setCommForward(jint size) {
    comm_forward = (int)size;
}
void FixJSE::setCommReverse(jint size) {
    comm_reverse = (int)size;
}

void FixJSE::neighborRequestDefault(jdouble rcut) {
    NeighRequest *req = neighbor->add_request(this, NeighConst::REQ_DEFAULT);
    if (rcut > 0.0) req->set_cutoff(rcut);
}
void FixJSE::neighborRequestFull(jdouble rcut) {
    NeighRequest *req = neighbor->add_request(this, NeighConst::REQ_FULL);
    if (rcut > 0.0) req->set_cutoff(rcut);
}
void FixJSE::neighborRequestOccasional(jdouble rcut) {
    NeighRequest *req = neighbor->add_request(this, NeighConst::REQ_OCCASIONAL);
    if (rcut > 0.0) req->set_cutoff(rcut);
}
void FixJSE::neighborRequestOccasionalFull(jdouble rcut) {
    NeighRequest *req = neighbor->add_request(this, NeighConst::REQ_FULL | NeighConst::REQ_OCCASIONAL);
    if (rcut > 0.0) req->set_cutoff(rcut);
}
void FixJSE::neighborBuildOne() {
    if (mNL == NULL) {
        throwExceptionLMP(mEnv, "No neighbor list in this fix");
        return;
    }
    neighbor->build_one(mNL);
}
jdouble FixJSE::neighborCutneighmin() {
    return neighbor->cutneighmin;
}
jdouble FixJSE::neighborCutneighmax() {
    return neighbor->cutneighmax;
}
jdouble FixJSE::neighborCuttype(jint type) {
    return neighbor->cuttype[type];
}
jdouble FixJSE::neighborSkin() {
    return neighbor->skin;
}
jint FixJSE::igroup_() {
    return (jint)igroup;
}
jint FixJSE::groupbit_() {
    return (jint)groupbit;
}
jlong FixJSE::atomX() {
    return (jlong)(intptr_t) atom->x;
}
jlong FixJSE::atomV() {
    return (jlong)(intptr_t) atom->v;
}
jlong FixJSE::atomF() {
    return (jlong)(intptr_t) atom->f;
}
jlong FixJSE::atomMask() {
    return (jlong)(intptr_t) atom->mask;
}
jlong FixJSE::atomTag() {
    return (jlong)(intptr_t) atom->tag;
}
jlong FixJSE::atomType() {
    return (jlong)(intptr_t) atom->type;
}
jlong FixJSE::atomMass() {
    return (jlong)(intptr_t) atom->mass;
}
jlong FixJSE::atomExtract(jstring name) {
    const char *name_c = mEnv->GetStringUTFChars(name, NULL);
    if (JSE_LMPPLUGIN::exceptionCheck(mEnv)) return NULL;
    jlong ptr = (jlong)(intptr_t) atom->extract(name_c);
    mEnv->ReleaseStringUTFChars(name, name_c);
    return ptr;
}
jlong FixJSE::atomNatoms() {
    return (jlong) atom->natoms;
}
jint FixJSE::atomNtypes() {
    return (jint) atom->ntypes;
}
jint FixJSE::atomNlocal() {
    return (jint) atom->nlocal;
}
jint FixJSE::atomNghost() {
    return (jint) atom->nghost;
}
jint FixJSE::atomNmax() {
    return (jint) atom->nmax;
}
jboolean FixJSE::domainTriclinic() {
    return (domain->triclinic) ? JNI_TRUE : JNI_FALSE;
}
jlong FixJSE::domainXy() {
    return (jlong)(intptr_t) &(domain->xy);
}
jlong FixJSE::domainXz() {
    return (jlong)(intptr_t) &(domain->xz);
}
jlong FixJSE::domainYz() {
    return (jlong)(intptr_t) &(domain->yz);
}
jdouble FixJSE::domainXprd() {
    return domain->xprd;
}
jdouble FixJSE::domainYprd() {
    return domain->yprd;
}
jdouble FixJSE::domainZprd() {
    return domain->zprd;
}
jlong FixJSE::domainH() {
    return (jlong)(intptr_t) domain->h;
}
jlong FixJSE::domainHInv() {
    return (jlong)(intptr_t) domain->h_inv;
}
jlong FixJSE::domainBoxlo() {
    return (jlong)(intptr_t) domain->boxlo;
}
jlong FixJSE::domainBoxhi() {
    return (jlong)(intptr_t) domain->boxhi;
}
jlong FixJSE::domainBoxloLamda() {
    return (jlong)(intptr_t) domain->boxlo_lamda;
}
jlong FixJSE::domainBoxhiLamda() {
    return (jlong)(intptr_t) domain->boxhi_lamda;
}
jlong FixJSE::domainSublo() {
    return (jlong)(intptr_t) domain->sublo;
}
jlong FixJSE::domainSubhi() {
    return (jlong)(intptr_t) domain->subhi;
}
jlong FixJSE::domainSubloLamda() {
    return (jlong)(intptr_t) domain->sublo_lamda;
}
jlong FixJSE::domainSubhiLamda() {
    return (jlong)(intptr_t) domain->subhi_lamda;
}
void FixJSE::domainX2lamda1(jint n) {
    domain->x2lamda((int)n);
}
void FixJSE::domainX2lamda2(jlong x, jlong lamda) {
    domain->x2lamda((double *)(intptr_t)x, (double *)(intptr_t)lamda);
}
void FixJSE::domainLamda2x1(jint n) {
    domain->lamda2x((int)n);
}
void FixJSE::domainLamda2x2(jlong lamda, jlong x) {
    domain->lamda2x((double *)(intptr_t)lamda, (double *)(intptr_t)x);
}
void FixJSE::domainSetGlobalBox() {
    domain->set_global_box();
}
void FixJSE::domainSetLocalBox() {
    domain->set_local_box();
}
jint FixJSE::listGnum() {
    if (mNL == NULL) {
        throwExceptionLMP(mEnv, "No neighbor list in this fix");
        return -1;
    }
    return (jint) mNL->gnum;
}
jint FixJSE::listInum() {
    if (mNL == NULL) {
        throwExceptionLMP(mEnv, "No neighbor list in this fix");
        return -1;
    }
    return (jint) mNL->inum;
}
jlong FixJSE::listIlist() {
    if (mNL == NULL) {
        throwExceptionLMP(mEnv, "No neighbor list in this fix");
        return NULL;
    }
    return (jlong)(intptr_t) mNL->ilist;
}
jlong FixJSE::listNumneigh() {
    if (mNL == NULL) {
        throwExceptionLMP(mEnv, "No neighbor list in this fix");
        return NULL;
    }
    return (jlong)(intptr_t) mNL->numneigh;
}
jlong FixJSE::listFirstneigh() {
    if (mNL == NULL) {
        throwExceptionLMP(mEnv, "No neighbor list in this fix");
        return NULL;
    }
    return (jlong)(intptr_t) mNL->firstneigh;
}
jdouble FixJSE::forceBoltz() {
    return force->boltz;
}
jdouble FixJSE::forcePairCutforce() {
    if (force->pair == NULL) return 0.0;
    return force->pair->cutforce;
}
jdouble FixJSE::dt() {
    return update->dt;
}
jlong FixJSE::ntimestep() {
    return (jlong) update->ntimestep;
}
jlong FixJSE::firststep() {
    return (jlong) update->firststep;
}
jlong FixJSE::laststep() {
    return (jlong) update->laststep;
}
jlong FixJSE::beginstep() {
    return (jlong) update->beginstep;
}
jlong FixJSE::endstep() {
    return (jlong) update->endstep;
}
jint FixJSE::commMe() {
    return (jint) comm->me;
}
jint FixJSE::commNprocs() {
    return (jint) comm->nprocs;
}
void FixJSE::commBarrier() {
    int tExitCode = MPI_Barrier(world);
    JSE_LMPPLUGIN::exceptionCheckMPI(mEnv, tExitCode);
}
jlong FixJSE::commWorld() {
    return (jlong)(intptr_t)world;
}
jdouble FixJSE::commCutghostuser() {
    return comm->cutghostuser;
}
void FixJSE::commForwardComm() {
    comm->forward_comm();
}
void FixJSE::commForwardCommThis() {
    comm->forward_comm(this);
}
void FixJSE::commReverseComm() {
    comm->reverse_comm();
}
void FixJSE::commReverseCommThis() {
    comm->reverse_comm(this);
}
jstring FixJSE::unitStyle() {
    char *tUnits = lmp->update->unit_style;
    return tUnits!=NULL ? mEnv->NewStringUTF(tUnits) : NULL;
}
