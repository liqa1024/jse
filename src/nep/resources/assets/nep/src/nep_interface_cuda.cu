#include "nep_interface_cuda.h"
#include "nep_core.hpp"


// >>> NEPGEN REMOVE
#define __NEPGEN_CUDA_BLOCKSIZE__ 256
#define __NEPGEN_USE_TABLE__ 0
#define __NEPGEN_VERSION__ 4
#define __NEPGEN_NTYPES__ 2
#define __NEPGEN_USE_TYPEWISE_CUTOFF__ 0
#define __NEPGEN_USE_TYPEWISE_CUTOFF_ZBL__ 0
#define __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__ 3.0
#define __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__ 2.0
#define __NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL__ 0.5
#define __NEPGEN_RCUT_R__ 6.0
#define __NEPGEN_RCUT_A__ 4.0
#define __NEPGEN_RCUT_INNER_ZBL__ 1.0
#define __NEPGEN_RCUT_OUTER_ZBL__ 2.0
#define __NEPGEN_NMAX_R__ 5
#define __NEPGEN_NMAX_A__ 5
#define __NEPGEN_BSIZE_R__ 8
#define __NEPGEN_BSIZE_A__ 8
#define __NEPGEN_NUMC_R__ 64
#define __NEPGEN_LMAX__ 4
#define __NEPGEN_NUML__ 6
#define __NEPGEN_ANN_DIM_A__ 20
#define __NEPGEN_ANN_DIM__ 30
#define __NEPGEN_NUM_NEURONS1__ 32
#define __NEPGEN_NUM_PARA_ANN__ 100
#define __NEPGEN_ZBL__ 1
#define __NEPGEN_ZBL_FLEXIBLED__ 0
// <<< NEPGEN REMOVE

namespace JSE_NEP {

static __global__ void initLammpsNeiKernel(int inum, int nlocalghost,
        int *ilist, flt_t *x, int *type,
        flt_t cutsq, int *numneigh, int *firstneigh, const int *type_map,
        flt_t *g_nl_dx, flt_t *g_nl_dy, flt_t *g_nl_dz, int *g_nl_type, int *g_nl_idx,
        int *g_num_neigh, int *g_ctype) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int i = ilist[ii];
    const flt_t xtmp = x[0*nlocalghost + i];
    const flt_t ytmp = x[1*nlocalghost + i];
    const flt_t ztmp = x[2*nlocalghost + i];
    const int typei = type[i];
    
    const int jnum = numneigh[ii];
    int num_neigh = 0;
    for (int jj = 0; jj < jnum; ++jj) {
        const int j = firstneigh[jj*inum + ii];
        // Note that dxyz in jse and lammps are defined oppositely
        const flt_t delx = x[0*nlocalghost + j] - xtmp;
        const flt_t dely = x[1*nlocalghost + j] - ytmp;
        const flt_t delz = x[2*nlocalghost + j] - ztmp;
        const flt_t rsq = delx*delx + dely*dely + delz*delz;
        if (rsq < cutsq) {
            g_nl_dx[num_neigh*inum + ii] = delx;
            g_nl_dy[num_neigh*inum + ii] = dely;
            g_nl_dz[num_neigh*inum + ii] = delz;
            g_nl_type[num_neigh*inum + ii] = type_map[type[j]];
            g_nl_idx[num_neigh*inum + ii] = j;
            ++num_neigh;
        }
    }
    g_num_neigh[ii] = num_neigh;
    g_ctype[ii] = type_map[typei];
}

static __global__ void collectLammpsResultsKernel(int inum, int nlocalghost,
        int vflag, int vflagAtom, int cvflagAtom,
        flt_t *f0, flt_t *f1, flt_t *vatom0, flt_t *vatom1,
        flt_t *g_nl_dx, flt_t *g_nl_dy, flt_t *g_nl_dz, int *g_nl_idx, int *g_num_neigh,
        flt_t *g_nl_fx, flt_t *g_nl_fy, flt_t *g_nl_fz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int num_neigh = g_num_neigh[ii];
    
    flt_t f0x = 0.0;
    flt_t f0y = 0.0;
    flt_t f0z = 0.0;
    for (int jj = 0; jj < num_neigh; ++jj) {
        const int j = g_nl_idx[jj*inum + ii];
        const flt_t fx = g_nl_fx[jj*inum + ii];
        const flt_t fy = g_nl_fy[jj*inum + ii];
        const flt_t fz = g_nl_fz[jj*inum + ii];
        f0x -= fx;
        f0y -= fy;
        f0z -= fz;
        atomicAdd(f1 + (0*nlocalghost + j), fx);
        atomicAdd(f1 + (1*nlocalghost + j), fy);
        atomicAdd(f1 + (2*nlocalghost + j), fz);
        if (vflag) {
            const flt_t dx = g_nl_dx[jj*inum + ii];
            const flt_t dy = g_nl_dy[jj*inum + ii];
            const flt_t dz = g_nl_dz[jj*inum + ii];
            const flt_t vxx = dx*fx;
            const flt_t vyy = dy*fy;
            const flt_t vzz = dz*fz;
            const flt_t vxy = dx*fy;
            const flt_t vxz = dx*fz;
            const flt_t vyz = dy*fz;
            vatom0[0*inum + ii] += vxx;
            vatom0[1*inum + ii] += vyy;
            vatom0[2*inum + ii] += vzz;
            vatom0[3*inum + ii] += vxy;
            vatom0[4*inum + ii] += vxz;
            vatom0[5*inum + ii] += vyz;
            if (cvflagAtom) {
                atomicAdd(vatom1 + (0*nlocalghost + j), vxx);
                atomicAdd(vatom1 + (1*nlocalghost + j), vyy);
                atomicAdd(vatom1 + (2*nlocalghost + j), vzz);
                atomicAdd(vatom1 + (3*nlocalghost + j), vxy);
                atomicAdd(vatom1 + (4*nlocalghost + j), vxz);
                atomicAdd(vatom1 + (5*nlocalghost + j), vyz);
                atomicAdd(vatom1 + (6*nlocalghost + j), dy*fx);
                atomicAdd(vatom1 + (7*nlocalghost + j), dz*fx);
                atomicAdd(vatom1 + (8*nlocalghost + j), dz*fy);
            } else if (vflagAtom) {
                atomicAdd(vatom1 + (0*nlocalghost + j), vxx);
                atomicAdd(vatom1 + (1*nlocalghost + j), vyy);
                atomicAdd(vatom1 + (2*nlocalghost + j), vzz);
                atomicAdd(vatom1 + (3*nlocalghost + j), vxy);
                atomicAdd(vatom1 + (4*nlocalghost + j), vxz);
                atomicAdd(vatom1 + (5*nlocalghost + j), vyz);
            }
        }
    }
    f0[0*inum + ii] += f0x;
    f0[1*inum + ii] += f0y;
    f0[2*inum + ii] += f0z;
}

static __global__ void postLammpsResultsKernel(int inum, int nlocalghost,
        int *ilist, flt_t *f0, flt_t *f1) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int i = ilist[ii];
    f1[0*nlocalghost + i] += f0[0*inum + ii];
    f1[1*nlocalghost + i] += f0[1*inum + ii];
    f1[2*nlocalghost + i] += f0[2*inum + ii];
}

static __global__ void computeLammpsKernel(int inum,
        flt_t *g_nl_dx, flt_t *g_nl_dy, flt_t *g_nl_dz, int *g_nl_type,
        int eflag, flt_t *eatom0, int *g_num_neigh, int *g_ctype,
        const int *atomic_numbers, const flt_t *q_scaler, const flt_t *zbl_para,
        const flt_t **ann_w0, const flt_t **ann_b0, const flt_t **ann_w1, const flt_t *ann_b1, const flt_t *ann_c,
        const flt_t *gn_radial, const flt_t *gn_angular,
        const flt_t *gnp_radial, const flt_t *gnp_angular,
        flt_t *g_fp, flt_t *g_sum_fxyz,
        flt_t *g_nl_fx, flt_t *g_nl_fy, flt_t *g_nl_fz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int ctype = g_ctype[ii];
    const int num_neigh = g_num_neigh[ii];
    // manual clear required
    flt_t eng = 0.0;
    for (int k = 0; k < __NEPGEN_ANN_DIM__; ++k) {
        g_fp[k*inum + ii] = (flt_t)0.0;
    }
    constexpr int size_sum_fxyz = (__NEPGEN_NMAX_A__+1) * NUM_OF_ABC;
    for (int k = 0; k < size_sum_fxyz; ++k) {
        g_sum_fxyz[k*inum + ii] = (flt_t)0.0;
    }
    for (int jj = 0; jj < num_neigh; ++jj) {
        g_nl_fx[jj*inum + ii] = (flt_t)0.0;
        g_nl_fy[jj*inum + ii] = (flt_t)0.0;
        g_nl_fz[jj*inum + ii] = (flt_t)0.0;
    }
    JSE_NEP::find_descriptor<__NEPGEN_USE_TABLE__, __NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                             __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__, __NEPGEN_NUMC_R__,
                             __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__,
                             __NEPGEN_ANN_DIM__, __NEPGEN_NUM_NEURONS1__>(inum, ii,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__, __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
        __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
        q_scaler,
        ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
        ctype, num_neigh,
        g_nl_type,
        g_nl_dx, g_nl_dy, g_nl_dz,
        __NEPGEN_USE_TABLE__?gn_radial:nullptr,
        __NEPGEN_USE_TABLE__?gn_angular:nullptr,
        g_fp, g_sum_fxyz,
        &eng
    );
    JSE_NEP::find_force_radial<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                               __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__>(inum, ii,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__,
        __NEPGEN_RCUT_R__,
        ann_c,
        ctype, num_neigh,
        g_nl_type,
        g_nl_dx, g_nl_dy, g_nl_dz,
        g_fp,
        __NEPGEN_USE_TABLE__?gnp_radial:nullptr,
        g_nl_fx, g_nl_fy, g_nl_fz
    );
    JSE_NEP::find_force_angular<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                               __NEPGEN_NMAX_R__, __NEPGEN_NUMC_R__,
                               __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__, __NEPGEN_ANN_DIM_A__>(inum, ii,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
        __NEPGEN_RCUT_A__,
        ann_c,
        ctype, num_neigh,
        g_nl_type,
        g_nl_dx, g_nl_dy, g_nl_dz,
        g_fp, g_sum_fxyz,
        __NEPGEN_USE_TABLE__?gn_angular:nullptr, __NEPGEN_USE_TABLE__?gnp_angular:nullptr,
        g_nl_fx, g_nl_fy, g_nl_fz
    );
    if (__NEPGEN_ZBL__) {
        JSE_NEP::find_force_ZBL<__NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF_ZBL__, __NEPGEN_ZBL_FLEXIBLED__>(inum, ii,
            atomic_numbers,
            __NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL__,
            __NEPGEN_ZBL_FLEXIBLED__?zbl_para:nullptr,
            __NEPGEN_RCUT_INNER_ZBL__, __NEPGEN_RCUT_OUTER_ZBL__,
            ctype, num_neigh,
            g_nl_type,
            g_nl_dx, g_nl_dy, g_nl_dz,
            g_nl_fx, g_nl_fy, g_nl_fz,
            &eng
        );
    }
    if (eflag) {
        eatom0[ii] += eng;
    }
}

static void computeLammpsCuda0(int inum, int nlocalghost,
        int eflag, int vflag, int vflagAtom, int cvflagAtom,
        int *ilist, flt_t *x, int *type,
        flt_t cutsq, int *numneigh, int *firstneigh, const int *type_map,
        flt_t *g_nl_dx, flt_t *g_nl_dy, flt_t *g_nl_dz, int *g_nl_type, int *g_nl_idx,
        int *g_num_neigh, int *g_ctype,
        flt_t *f0, flt_t *f1, flt_t *eatom0, flt_t *vatom0, flt_t *vatom1,
        const int *atomic_numbers, const flt_t *q_scaler, const flt_t *zbl_para,
        const flt_t **ann_w0, const flt_t **ann_b0, const flt_t **ann_w1, const flt_t *ann_b1, const flt_t *ann_c,
        const flt_t *gn_radial, const flt_t *gn_angular,
        const flt_t *gnp_radial, const flt_t *gnp_angular,
        flt_t *g_fp, flt_t *g_sum_fxyz,
        flt_t *g_nl_fx, flt_t *g_nl_fy, flt_t *g_nl_fz) {
    constexpr int tBlockSize = __NEPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (inum + tBlockSize-1) / tBlockSize;
    
    initLammpsNeiKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        ilist, x, type,
        cutsq, numneigh, firstneigh, type_map,
        g_nl_dx, g_nl_dy, g_nl_dz, g_nl_type, g_nl_idx,
        g_num_neigh, g_ctype
    );
    
    computeLammpsKernel<<<tGridSize, tBlockSize>>>(inum,
        g_nl_dx, g_nl_dy, g_nl_dz, g_nl_type,
        eflag, eatom0, g_num_neigh, g_ctype,
        atomic_numbers, q_scaler, zbl_para,
        ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
        gn_radial, gn_angular,
        gnp_radial, gnp_angular,
        g_fp, g_sum_fxyz,
        g_nl_fx, g_nl_fy, g_nl_fz
    );
    
    collectLammpsResultsKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        vflag, vflagAtom, cvflagAtom,
        f0, f1, vatom0, vatom1,
        g_nl_dx, g_nl_dy, g_nl_dz, g_nl_idx, g_num_neigh,
        g_nl_fx, g_nl_fy, g_nl_fz
    );
    postLammpsResultsKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        ilist, f0, f1
    );
}

}

extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_constructTable(void *aDataIn, void *rDataOut) {
    auto tDataOut = (void **)rDataOut;
    
    auto gn_radial = (JSE_NEP::flt_t *)tDataOut[0];
    auto gn_angular = (JSE_NEP::flt_t *)tDataOut[1];
    auto gnp_radial = (JSE_NEP::flt_t *)tDataOut[2];
    auto gnp_angular = (JSE_NEP::flt_t *)tDataOut[3];
    
    JSE_NEP::construct_table<__NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_NMAX_R__, __NEPGEN_NMAX_A__,
                             __NEPGEN_BSIZE_R__, __NEPGEN_BSIZE_A__, __NEPGEN_NUMC_R__, __NEPGEN_NUM_PARA_ANN__>(
        (const double *)aDataIn,
        __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
        gn_radial, gn_angular,
        gnp_radial, gnp_angular
    );
    return 0;
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_statNeiNumLammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    int *tDataOut = (int *)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    int *ilist = (int *)tDataIn[1];
    int *numneigh = (int *)tDataIn[2];
    
    int inum = tNums[0];
    int numneighMax = 0;
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int jnum = numneigh[i];
        if (jnum > numneighMax) numneighMax = jnum;
    }
    tDataOut[0] = numneighMax;
    
    return 0;
}


#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_lammps2cuda(void *aDataIn, void *rDataOut) {
    auto tDataIn = (void **)aDataIn;
    auto tDataOut = (void **)rDataOut;
    
    auto nums = (const int *)tDataIn[0];
    
    int inum = nums[0];
    int nlocalghost = nums[1];
    int nlflag = nums[2];
    int neighnumMax = nums[3];
    
    auto x = (const double **)tDataIn[1];
    auto type = (const int *)tDataIn[2];
    const int *ilist = nlflag ? (const int *)tDataIn[3] : nullptr;
    const int *numneigh = nlflag ? (const int *)tDataIn[4] : nullptr;
    const int **firstneigh = nlflag ? (const int **)tDataIn[5] : nullptr;
    
    auto fltBuf = (JSE_NEP::flt_t *)tDataOut[0];
    auto intBuf = (int *)tDataOut[1];
    auto cudaX = (JSE_NEP::flt_t *)tDataOut[2];
    auto cudaType = (int *)tDataOut[3];
    int *cudaIlist = nlflag ? (int *)tDataOut[4] : nullptr;
    int *cudaNumneigh = nlflag ? (int *)tDataOut[5] : nullptr;
    int *cudaFirstneigh = nlflag ? (int *)tDataOut[6] : nullptr;
    
    cudaError_t err;
    for (int i = 0; i < nlocalghost; ++i) {
        fltBuf[0*nlocalghost + i] = (JSE_NEP::flt_t)x[i][0];
        fltBuf[1*nlocalghost + i] = (JSE_NEP::flt_t)x[i][1];
        fltBuf[2*nlocalghost + i] = (JSE_NEP::flt_t)x[i][2];
    }
    err = cudaMemcpy(cudaX, fltBuf, nlocalghost*3*sizeof(JSE_NEP::flt_t), cudaMemcpyHostToDevice);
    if (err!=cudaSuccess) return (int)err;
    err = cudaMemcpy(cudaType, type, nlocalghost*sizeof(int), cudaMemcpyHostToDevice);
    if (err!=cudaSuccess) return (int)err;
    if (nlflag) {
        err = cudaMemcpy(cudaIlist, ilist, inum*sizeof(int), cudaMemcpyHostToDevice);
        if (err!=cudaSuccess) return (int)err;
        // reorder in ilist
        for (int ii = 0; ii < inum; ++ii) {
            intBuf[ii] = numneigh[ilist[ii]];
        }
        err = cudaMemcpy(cudaNumneigh, intBuf, inum*sizeof(int), cudaMemcpyHostToDevice);
        if (err!=cudaSuccess) return (int)err;
        // reorder in ilist
        for (int ii = 0; ii < inum; ++ii) {
            const int i = ilist[ii];
            const int jnum = numneigh[i];
            const int *jlist = firstneigh[i];
            for (int jj = 0; jj < jnum; ++jj) {
                intBuf[jj*inum + ii] = jlist[jj] & JSE_LMP_NEIGHMASK;
            }
        }
        err = cudaMemcpy(cudaFirstneigh, intBuf, inum*neighnumMax*sizeof(int), cudaMemcpyHostToDevice);
        if (err!=cudaSuccess) return (int)err;
    }
    return (int)cudaSuccess;
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_cuda2lammps(void *aDataIn, void *rDataOut) {
    auto tDataIn = (void **)aDataIn;
    auto tDataOut = (void **)rDataOut;
    
    auto nums = (const int *)tDataIn[0];
    
    int inum = nums[0];
    int nlocalghost = nums[1];
    int eflag = nums[2];
    int vflag = nums[3];
    int eflagAtom = nums[4];
    int vflagAtom = nums[5];
    int cvflagAtom = nums[6];
    
    auto f = (double **)tDataOut[0];
    auto engVdwl = (double *)tDataOut[1];
    auto eatom = (double *)tDataOut[2];
    auto virial = (double *)tDataOut[3];
    auto vatom = (double **)tDataOut[4];
    auto cvatom = (double **)tDataOut[5];
    
    auto fltBuf = (JSE_NEP::flt_t *)tDataIn[1];
    auto ilist = (const int *)tDataIn[2];
    auto cudaF1 = (const JSE_NEP::flt_t *)tDataIn[3];
    auto cudaEatom0 = (const JSE_NEP::flt_t *)tDataIn[4];
    auto cudaVatom0 = (const JSE_NEP::flt_t *)tDataIn[5];
    auto cudaVatom1 = (const JSE_NEP::flt_t *)tDataIn[6];
    
    cudaError_t err;
    err = cudaMemcpy(fltBuf, cudaF1, nlocalghost*3*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
    if (err!=cudaSuccess) return (int)err;
    for (int i = 0; i < nlocalghost; ++i) {
        f[i][0] += (double)fltBuf[0*nlocalghost + i];
        f[i][1] += (double)fltBuf[1*nlocalghost + i];
        f[i][2] += (double)fltBuf[2*nlocalghost + i];
    }
    
    if (eflag) {
        err = cudaMemcpy(fltBuf, cudaEatom0, inum*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
        if (err!=cudaSuccess) return (int)err;
        // reorder in ilist
        for (int ii = 0; ii < inum; ++ii) {
            const double eng = (double)fltBuf[ii];
            *engVdwl += eng;
            if (eflagAtom) eatom[ilist[ii]] += eng;
        }
    }
    if (vflag) {
        err = cudaMemcpy(fltBuf, cudaVatom0, inum*6*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
        if (err!=cudaSuccess) return (int)err;
        for (int ii = 0; ii < inum; ++ii) {
            virial[0] += (double)fltBuf[0*inum + ii];
            virial[1] += (double)fltBuf[1*inum + ii];
            virial[2] += (double)fltBuf[2*inum + ii];
            virial[3] += (double)fltBuf[3*inum + ii];
            virial[4] += (double)fltBuf[4*inum + ii];
            virial[5] += (double)fltBuf[5*inum + ii];
        }
    }
    if (cvflagAtom) {
        err = cudaMemcpy(fltBuf, cudaVatom1, nlocalghost*9*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
    } else if (vflagAtom) {
        err = cudaMemcpy(fltBuf, cudaVatom1, nlocalghost*6*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
    }
    if (err!=cudaSuccess) return (int)err;
    if (cvflagAtom) {
        for (int i = 0; i < nlocalghost; ++i) {
            cvatom[i][0] += (double)fltBuf[0*nlocalghost + i];
            cvatom[i][1] += (double)fltBuf[1*nlocalghost + i];
            cvatom[i][2] += (double)fltBuf[2*nlocalghost + i];
            cvatom[i][3] += (double)fltBuf[3*nlocalghost + i];
            cvatom[i][4] += (double)fltBuf[4*nlocalghost + i];
            cvatom[i][5] += (double)fltBuf[5*nlocalghost + i];
            cvatom[i][6] += (double)fltBuf[6*nlocalghost + i];
            cvatom[i][7] += (double)fltBuf[7*nlocalghost + i];
            cvatom[i][8] += (double)fltBuf[8*nlocalghost + i];
        }
    }
    if (vflagAtom) {
        for (int i = 0; i < nlocalghost; ++i) {
            vatom[i][0] += (double)fltBuf[0*nlocalghost + i];
            vatom[i][1] += (double)fltBuf[1*nlocalghost + i];
            vatom[i][2] += (double)fltBuf[2*nlocalghost + i];
            vatom[i][3] += (double)fltBuf[3*nlocalghost + i];
            vatom[i][4] += (double)fltBuf[4*nlocalghost + i];
            vatom[i][5] += (double)fltBuf[5*nlocalghost + i];
        }
    }
    return (int)cudaSuccess;
}


JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_computeLammpsCuda(void *aDataIn, void *rDataOut) {
    auto tDataIn = (void **)aDataIn;
    auto tDataOut = (void **)rDataOut;

    auto cpuNums = (const int *)tDataIn[0];
    
    int inum = cpuNums[0];
    int nlocalghost = cpuNums[1];
    int eflag = cpuNums[2];
    int vflag = cpuNums[3];
    int eflagAtom = cpuNums[4];
    int vflagAtom = cpuNums[5];
    int cvflagAtom = cpuNums[6];
    
    auto x = (JSE_NEP::flt_t *)tDataIn[1];
    auto type = (int *)tDataIn[2];
    auto ilist = (int *)tDataIn[3];
    auto numneigh = (int *)tDataIn[4];
    auto firstneigh = (int *)tDataIn[5];
    auto cpuCutsq = (const double *)tDataIn[6];
    auto type_map = (const int *)tDataIn[7];
    const double cutsq = cpuCutsq[0];
    
    auto atomic_numbers = (const int *)tDataIn[8];
    auto q_scaler = (const JSE_NEP::flt_t *)tDataIn[9];
    auto ann_w0 = (const JSE_NEP::flt_t **)tDataIn[10];
    auto ann_b0 = (const JSE_NEP::flt_t **)tDataIn[11];
    auto ann_w1 = (const JSE_NEP::flt_t **)tDataIn[12];
    auto ann_b1 = (const JSE_NEP::flt_t *)tDataIn[13];
    auto ann_c = (const JSE_NEP::flt_t *)tDataIn[14];
    auto zbl_para = (const JSE_NEP::flt_t *)tDataIn[15];
    auto gn_radial = (const JSE_NEP::flt_t *)tDataIn[16];
    auto gn_angular = (const JSE_NEP::flt_t *)tDataIn[17];
    auto gnp_radial = (const JSE_NEP::flt_t *)tDataIn[18];
    auto gnp_angular = (const JSE_NEP::flt_t *)tDataIn[19];
    
    auto f0 = (JSE_NEP::flt_t *)tDataOut[0];
    auto f1 = (JSE_NEP::flt_t *)tDataOut[1];
    auto eatom0 = (JSE_NEP::flt_t *)tDataOut[2];
    auto vatom0 = (JSE_NEP::flt_t *)tDataOut[3];
    auto vatom1 = (JSE_NEP::flt_t *)tDataOut[4];
    auto g_nl_dx = (JSE_NEP::flt_t *)tDataOut[5];
    auto g_nl_dy = (JSE_NEP::flt_t *)tDataOut[6];
    auto g_nl_dz = (JSE_NEP::flt_t *)tDataOut[7];
    auto g_nl_type = (int *)tDataOut[8];
    auto g_nl_idx = (int *)tDataOut[9];
    auto g_num_neigh = (int *)tDataOut[10];
    auto g_ctype = (int *)tDataOut[11];
    auto g_nl_fx = (JSE_NEP::flt_t *)tDataOut[12];
    auto g_nl_fy = (JSE_NEP::flt_t *)tDataOut[13];
    auto g_nl_fz = (JSE_NEP::flt_t *)tDataOut[14];
    auto g_fp = (JSE_NEP::flt_t *)tDataOut[15];
    auto g_sum_fxyz = (JSE_NEP::flt_t *)tDataOut[16];
    
    cudaError_t err;
    err = cudaMemset(f0, 0, inum*3*sizeof(JSE_NEP::flt_t));
    if (err!=cudaSuccess) return (int)err;
    err = cudaMemset(f1, 0, nlocalghost*3*sizeof(JSE_NEP::flt_t));
    if (err!=cudaSuccess) return (int)err;
    if (eflag||eflagAtom) {
        err = cudaMemset(eatom0, 0, inum*sizeof(JSE_NEP::flt_t));
        if (err!=cudaSuccess) return (int)err;
    }
    if (vflag) {
        err = cudaMemset(vatom0, 0, inum*6*sizeof(JSE_NEP::flt_t));
        if (err!=cudaSuccess) return (int)err;
    }
    if (cvflagAtom) {
        err = cudaMemset(vatom1, 0, nlocalghost*9*sizeof(JSE_NEP::flt_t));
        if (err!=cudaSuccess) return (int)err;
    } else if (vflagAtom) {
        err = cudaMemset(vatom1, 0, nlocalghost*6*sizeof(JSE_NEP::flt_t));
        if (err!=cudaSuccess) return (int)err;
    }
    
    /// begin compute here
    JSE_NEP::computeLammpsCuda0(inum, nlocalghost,
        eflag||eflagAtom, vflag, vflagAtom, cvflagAtom,
        ilist, x, type,
        (JSE_NEP::flt_t)cutsq, numneigh, firstneigh, type_map,
        g_nl_dx, g_nl_dy, g_nl_dz, g_nl_type, g_nl_idx,
        g_num_neigh, g_ctype,
        f0, f1, eatom0, vatom0, vatom1,
        atomic_numbers, q_scaler, zbl_para,
        ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
        gn_radial, gn_angular,
        gnp_radial, gnp_angular,
        g_fp, g_sum_fxyz,
        g_nl_fx, g_nl_fy, g_nl_fz
    );
    
    return (int)cudaDeviceSynchronize();
}

}
