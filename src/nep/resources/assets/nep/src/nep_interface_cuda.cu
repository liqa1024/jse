#include "nep_core.hpp"

#include <cstdint>

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

static __global__ void initLammpsTypeKernel(int nlocalghost,
        int *type, int *type_map) {

    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocalghost) return;

    type[i] = type_map[type[i]];
}

static __global__ void initLammpsNeiKernel(int nlocal,
        int *rNlSizeR, int *rNlSizeA, int *rMgNlIdx,
        int *nlsize, int *nlidx,
        flt_t *posx, flt_t *posy, flt_t *posz) {
    
    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocal) return;
    
    const flt_t xi = posx[i];
    const flt_t yi = posy[i];
    const flt_t zi = posz[i];
    
    const int jnum = nlsize[i];
    constexpr flt_t cutsqA = __NEPGEN_RCUT_A__*__NEPGEN_RCUT_A__;
    constexpr flt_t cutsqR = __NEPGEN_RCUT_R__*__NEPGEN_RCUT_R__;
    int tNlSize = 0;
    for (int jj = 0; jj < jnum; ++jj) {
        const int j = nlidx[(size_t)jj*nlocal + i];
        const flt_t dx = posx[j] - xi;
        const flt_t dy = posy[j] - yi;
        const flt_t dz = posz[j] - zi;
        const flt_t rsq = dx*dx + dy*dy + dz*dz;
        if (rsq < cutsqA) {
            rMgNlIdx[(size_t)tNlSize*nlocal + i] = j;
            ++tNlSize;
        }
    }
    rNlSizeA[i] = tNlSize;
    for (int jj = 0; jj < jnum; ++jj) {
        const int j = nlidx[(size_t)jj*nlocal + i];
        const flt_t dx = posx[j] - xi;
        const flt_t dy = posy[j] - yi;
        const flt_t dz = posz[j] - zi;
        const flt_t rsq = dx*dx + dy*dy + dz*dz;
        if (rsq>=cutsqA && rsq<cutsqR) {
            rMgNlIdx[(size_t)tNlSize*nlocal + i] = j;
            ++tNlSize;
        }
    }
    rNlSizeR[i] = tNlSize;
}

template <int EEITHER, int VTOTAL, int VATOM>
static __global__ void computeLammpsKernel(int nlocal, int nghost,
        int *aNlSizeR, int *aNlSizeA, int *aMgNlIdx,
        flt_t *posx, flt_t *posy, flt_t *posz, int *type,
        flt_t *eatom0, flt_t *f, flt_t *vatom0, flt_t *vatom1,
        flt_t *g_nl_fx, flt_t *g_nl_fy, flt_t *g_nl_fz,
        const int *atomic_numbers, const flt_t *q_scaler, const flt_t *zbl_para,
        const flt_t **ann_w0, const flt_t **ann_b0, const flt_t **ann_w1, const flt_t *ann_b1, const flt_t *ann_c,
        const flt_t *gn_radial, const flt_t *gn_angular,
        const flt_t *gnp_radial, const flt_t *gnp_angular,
        flt_t *g_fp, flt_t *g_sum_fxyz) {
    
    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocal) return;
    
    const int nlocalghost = nlocal + nghost;
    const int tNlSizeR = aNlSizeR[i];
    const int tNlSizeA = aNlSizeA[i];
    // manual clear required
    flt_t eng = 0.0;
    for (int k = 0; k < __NEPGEN_ANN_DIM__; ++k) {
        g_fp[(size_t)k*nlocal + i] = (flt_t)0.0;
    }
    constexpr int size_sum_fxyz = (__NEPGEN_NMAX_A__+1) * NUM_OF_ABC;
    for (int k = 0; k < size_sum_fxyz; ++k) {
        g_sum_fxyz[(size_t)k*nlocal + i] = (flt_t)0.0;
    }
    for (int jj = 0; jj < tNlSizeR; ++jj) {
        g_nl_fx[(size_t)jj*nlocal + i] = (flt_t)0.0;
        g_nl_fy[(size_t)jj*nlocal + i] = (flt_t)0.0;
        g_nl_fz[(size_t)jj*nlocal + i] = (flt_t)0.0;
    }
    JSE_NEP::find_descriptor_gpu<__NEPGEN_USE_TABLE__, __NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                                 __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__, __NEPGEN_NUMC_R__,
                                 __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__,
                                 __NEPGEN_ANN_DIM__, __NEPGEN_NUM_NEURONS1__>(nlocal, i,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__, __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
        __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
        q_scaler,
        ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
        tNlSizeR, tNlSizeA, aMgNlIdx,
        posx, posy, posz, type,
        __NEPGEN_USE_TABLE__?gn_radial:nullptr,
        __NEPGEN_USE_TABLE__?gn_angular:nullptr,
        g_fp, g_sum_fxyz,
        &eng
    );
    JSE_NEP::find_force_radial_gpu<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                                   __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__>(nlocal, i,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__,
        __NEPGEN_RCUT_R__,
        ann_c,
        tNlSizeR, aMgNlIdx,
        posx, posy, posz, type,
        g_fp,
        __NEPGEN_USE_TABLE__?gnp_radial:nullptr,
        g_nl_fx, g_nl_fy, g_nl_fz
    );
    JSE_NEP::find_force_angular_gpu<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                                    __NEPGEN_NMAX_R__, __NEPGEN_NUMC_R__,
                                    __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__, __NEPGEN_ANN_DIM_A__>(nlocal, i,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
        __NEPGEN_RCUT_A__,
        ann_c,
        tNlSizeA, aMgNlIdx,
        posx, posy, posz, type,
        g_fp, g_sum_fxyz,
        __NEPGEN_USE_TABLE__?gn_angular:nullptr, __NEPGEN_USE_TABLE__?gnp_angular:nullptr,
        g_nl_fx, g_nl_fy, g_nl_fz
    );
    if (__NEPGEN_ZBL__) {
        JSE_NEP::find_force_ZBL_gpu<__NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF_ZBL__, __NEPGEN_ZBL_FLEXIBLED__>(nlocal, i,
            atomic_numbers,
            __NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL__,
            __NEPGEN_ZBL_FLEXIBLED__?zbl_para:nullptr,
            __NEPGEN_RCUT_INNER_ZBL__, __NEPGEN_RCUT_OUTER_ZBL__,
            tNlSizeR, aMgNlIdx,
            posx, posy, posz, type,
            g_nl_fx, g_nl_fy, g_nl_fz,
            &eng
        );
    }
    
    if (EEITHER) {
        eatom0[i] += eng;
    }
    flt_t f0xi = 0.0, f0yi = 0.0, f0zi = 0.0;
    flt_t v0xxi = 0.0, v0yyi = 0.0, v0zzi = 0.0;
    flt_t v0xyi = 0.0, v0xzi = 0.0, v0yzi = 0.0;
    const flt_t xi = posx[i];
    const flt_t yi = posy[i];
    const flt_t zi = posz[i];
    for (int jj = 0; jj < tNlSizeR; ++jj) {
        const int j = aMgNlIdx[(size_t)jj*nlocal + i];
        const flt_t fxj = g_nl_fx[(size_t)jj*nlocal + i];
        const flt_t fyj = g_nl_fy[(size_t)jj*nlocal + i];
        const flt_t fzj = g_nl_fz[(size_t)jj*nlocal + i];
        f0xi += fxj; f0yi += fyj; f0zi += fzj;
        atomicAdd(f + (0*nlocalghost + j), -fxj);
        atomicAdd(f + (1*nlocalghost + j), -fyj);
        atomicAdd(f + (2*nlocalghost + j), -fzj);
        if (VTOTAL||VATOM) {
            const flt_t dx = posx[j] - xi;
            const flt_t dy = posy[j] - yi;
            const flt_t dz = posz[j] - zi;
            if (VTOTAL) {
                v0xxi -= dx*fxj; v0yyi -= dy*fyj; v0zzi -= dz*fzj;
                v0xyi -= dx*fyj; v0xzi -= dx*fzj; v0yzi -= dy*fzj;
            }
            if (VATOM) {
                atomicAdd(vatom1 + (0*nlocalghost + j), -dx*fxj);
                atomicAdd(vatom1 + (1*nlocalghost + j), -dy*fyj);
                atomicAdd(vatom1 + (2*nlocalghost + j), -dz*fzj);
                atomicAdd(vatom1 + (3*nlocalghost + j), -dx*fyj);
                atomicAdd(vatom1 + (4*nlocalghost + j), -dx*fzj);
                atomicAdd(vatom1 + (5*nlocalghost + j), -dy*fzj);
                atomicAdd(vatom1 + (6*nlocalghost + j), -dy*fxj);
                atomicAdd(vatom1 + (7*nlocalghost + j), -dz*fxj);
                atomicAdd(vatom1 + (8*nlocalghost + j), -dz*fyj);
            }
        }
    }
    atomicAdd(f + (0*nlocalghost + i), f0xi);
    atomicAdd(f + (1*nlocalghost + i), f0yi);
    atomicAdd(f + (2*nlocalghost + i), f0zi);
    if (VTOTAL) {
        vatom0[0*nlocal + i] += v0xxi;
        vatom0[1*nlocal + i] += v0yyi;
        vatom0[2*nlocal + i] += v0zzi;
        vatom0[3*nlocal + i] += v0xyi;
        vatom0[4*nlocal + i] += v0xzi;
        vatom0[5*nlocal + i] += v0yzi;
    }
}
template <int VTOTAL, int VATOM>
static void computeLammpsKernel_(int aGridSize, int aBlockSize,
        int nlocal, int nghost,
        int *aNlSizeR, int *aNlSizeA, int *aMgNlIdx,
        flt_t *posx, flt_t *posy, flt_t *posz, int *type,
        int eflagEither,
        flt_t *eatom0, flt_t *f, flt_t *vatom0, flt_t *vatom1,
        flt_t *g_nl_fx, flt_t *g_nl_fy, flt_t *g_nl_fz,
        const int *atomic_numbers, const flt_t *q_scaler, const flt_t *zbl_para,
        const flt_t **ann_w0, const flt_t **ann_b0, const flt_t **ann_w1, const flt_t *ann_b1, const flt_t *ann_c,
        const flt_t *gn_radial, const flt_t *gn_angular,
        const flt_t *gnp_radial, const flt_t *gnp_angular,
        flt_t *g_fp, flt_t *g_sum_fxyz) {
    
    if (eflagEither) {
        computeLammpsKernel<1, VTOTAL, VATOM>
                     <<<aGridSize, aBlockSize>>>(nlocal, nghost,
            aNlSizeR, aNlSizeA, aMgNlIdx,
            posx, posy, posz, type,
            eatom0, f, vatom0, vatom1,
            g_nl_fx, g_nl_fy, g_nl_fz,
            atomic_numbers, q_scaler, zbl_para,
            ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
            gn_radial, gn_angular,
            gnp_radial, gnp_angular,
            g_fp, g_sum_fxyz
        );
    } else {
        computeLammpsKernel<0, VTOTAL, VATOM>
                     <<<aGridSize, aBlockSize>>>(nlocal, nghost,
            aNlSizeR, aNlSizeA, aMgNlIdx,
            posx, posy, posz, type,
            eatom0, f, vatom0, vatom1,
            g_nl_fx, g_nl_fy, g_nl_fz,
            atomic_numbers, q_scaler, zbl_para,
            ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
            gn_radial, gn_angular,
            gnp_radial, gnp_angular,
            g_fp, g_sum_fxyz
        );
    }
}
static void computeLammpsKernel_(int aGridSize, int aBlockSize,
        int nlocal, int nghost,
        int *aNlSizeR, int *aNlSizeA, int *aMgNlIdx,
        flt_t *posx, flt_t *posy, flt_t *posz, int *type,
        int eflagEither, int vflag, int vflagAtom,
        flt_t *eatom0, flt_t *f, flt_t *vatom0, flt_t *vatom1,
        flt_t *g_nl_fx, flt_t *g_nl_fy, flt_t *g_nl_fz,
        const int *atomic_numbers, const flt_t *q_scaler, const flt_t *zbl_para,
        const flt_t **ann_w0, const flt_t **ann_b0, const flt_t **ann_w1, const flt_t *ann_b1, const flt_t *ann_c,
        const flt_t *gn_radial, const flt_t *gn_angular,
        const flt_t *gnp_radial, const flt_t *gnp_angular,
        flt_t *g_fp, flt_t *g_sum_fxyz) {
    
    if (vflagAtom) {
        computeLammpsKernel_<1, 1>(aGridSize, aBlockSize,
            nlocal, nghost,
            aNlSizeR, aNlSizeA, aMgNlIdx,
            posx, posy, posz, type,
            eflagEither,
            eatom0, f, vatom0, vatom1,
            g_nl_fx, g_nl_fy, g_nl_fz,
            atomic_numbers, q_scaler, zbl_para,
            ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
            gn_radial, gn_angular,
            gnp_radial, gnp_angular,
            g_fp, g_sum_fxyz
        );
    } else if (vflag) {
        computeLammpsKernel_<1, 0>(aGridSize, aBlockSize,
            nlocal, nghost,
            aNlSizeR, aNlSizeA, aMgNlIdx,
            posx, posy, posz, type,
            eflagEither,
            eatom0, f, vatom0, vatom1,
            g_nl_fx, g_nl_fy, g_nl_fz,
            atomic_numbers, q_scaler, zbl_para,
            ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
            gn_radial, gn_angular,
            gnp_radial, gnp_angular,
            g_fp, g_sum_fxyz
        );
    } else {
        computeLammpsKernel_<0, 0>(aGridSize, aBlockSize,
            nlocal, nghost,
            aNlSizeR, aNlSizeA, aMgNlIdx,
            posx, posy, posz, type,
            eflagEither,
            eatom0, f, vatom0, vatom1,
            g_nl_fx, g_nl_fy, g_nl_fz,
            atomic_numbers, q_scaler, zbl_para,
            ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
            gn_radial, gn_angular,
            gnp_radial, gnp_angular,
            g_fp, g_sum_fxyz
        );
    }
}

}

#define __jsefunc__

extern "C" {

__jsefunc__ int jse_nep_constructTable(const double *parameters,
    JSE_NEP::flt_t *gn_radial, JSE_NEP::flt_t *gn_angular,
    JSE_NEP::flt_t *gnp_radial, JSE_NEP::flt_t *gnp_angular) {
    
    JSE_NEP::construct_table<__NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_NMAX_R__, __NEPGEN_NMAX_A__,
                             __NEPGEN_BSIZE_R__, __NEPGEN_BSIZE_A__, __NEPGEN_NUMC_R__, __NEPGEN_NUM_PARA_ANN__>(
        parameters,
        __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
        gn_radial, gn_angular,
        gnp_radial, gnp_angular
    );
    return 0;
}

__jsefunc__ int jse_nep_cuda2lammps(
    int nlocal, int nghost, int eflag, int eflagAtom, int vflag, int vflagAtom, int cvflagAtom,
    double **f, double *engVdwl, double *eatom, double *virial, double **vatom, double **cvatom,
    int *ilist, JSE_NEP::flt_t *fltBuf, JSE_NEP::flt_t *cudaF,
    JSE_NEP::flt_t *cudaEatom0, JSE_NEP::flt_t *cudaVatom0, JSE_NEP::flt_t *cudaVatom1) {
    
    const int nlocalghost = nlocal + nghost;
    cudaError_t err;
    err = cudaMemcpy(fltBuf, cudaF, nlocalghost*3L*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
    if (err!=cudaSuccess) return (int)err;
    for (int ii = 0; ii < nlocalghost; ++ii) {
        const int i = ilist[ii];
        f[i][0] += (double)fltBuf[0L*nlocalghost + ii];
        f[i][1] += (double)fltBuf[1L*nlocalghost + ii];
        f[i][2] += (double)fltBuf[2L*nlocalghost + ii];
    }
    
    if (eflag || eflagAtom) {
        err = cudaMemcpy(fltBuf, cudaEatom0, nlocal*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
        if (err!=cudaSuccess) return (int)err;
        // reorder in ilist
        for (int ii = 0; ii < nlocal; ++ii) {
            const double tEng = (double)fltBuf[ii];
            *engVdwl += tEng;
            if (eflagAtom) eatom[ilist[ii]] += tEng;
        }
    }
    if (vflag) {
        err = cudaMemcpy(fltBuf, cudaVatom0, nlocal*6L*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
        if (err!=cudaSuccess) return (int)err;
        for (int ii = 0; ii < nlocal; ++ii) {
            virial[0] += (double)fltBuf[0L*nlocal + ii];
            virial[1] += (double)fltBuf[1L*nlocal + ii];
            virial[2] += (double)fltBuf[2L*nlocal + ii];
            virial[3] += (double)fltBuf[3L*nlocal + ii];
            virial[4] += (double)fltBuf[4L*nlocal + ii];
            virial[5] += (double)fltBuf[5L*nlocal + ii];
        }
    }
    if (cvflagAtom || vflagAtom) {
        err = cudaMemcpy(fltBuf, cudaVatom1, nlocalghost*9L*sizeof(JSE_NEP::flt_t), cudaMemcpyDeviceToHost);
        if (err!=cudaSuccess) return (int)err;
        if (cvflagAtom) {
            for (int ii = 0; ii < nlocalghost; ++ii) {
                const int i = ilist[ii];
                cvatom[i][0] += (double)fltBuf[0L*nlocalghost + ii];
                cvatom[i][1] += (double)fltBuf[1L*nlocalghost + ii];
                cvatom[i][2] += (double)fltBuf[2L*nlocalghost + ii];
                cvatom[i][3] += (double)fltBuf[3L*nlocalghost + ii];
                cvatom[i][4] += (double)fltBuf[4L*nlocalghost + ii];
                cvatom[i][5] += (double)fltBuf[5L*nlocalghost + ii];
                cvatom[i][6] += (double)fltBuf[6L*nlocalghost + ii];
                cvatom[i][7] += (double)fltBuf[7L*nlocalghost + ii];
                cvatom[i][8] += (double)fltBuf[8L*nlocalghost + ii];
            }
        }
        if (vflagAtom) {
            for (int ii = 0; ii < nlocalghost; ++ii) {
                const int i = ilist[ii];
                vatom[i][0] += (double)fltBuf[0L*nlocalghost + ii];
                vatom[i][1] += (double)fltBuf[1L*nlocalghost + ii];
                vatom[i][2] += (double)fltBuf[2L*nlocalghost + ii];
                vatom[i][3] += (double)fltBuf[3L*nlocalghost + ii];
                vatom[i][4] += (double)fltBuf[4L*nlocalghost + ii];
                vatom[i][5] += (double)fltBuf[5L*nlocalghost + ii];
            }
        }
    }
    return (int)cudaSuccess;
}


__jsefunc__ int jse_nep_computeLammpsCuda(
    int nlocal, int nghost, int eflagEither, int vflag, int vflagAtom,
    JSE_NEP::flt_t *posx, JSE_NEP::flt_t *posy, JSE_NEP::flt_t *posz, int *type,
    int *rNlSizeR, int *rNlSizeA, int *rMgNlIdx, int *nlsize, int *nlidx, int *type_map,
    const int *atomic_numbers, const JSE_NEP::flt_t *q_scaler,
    const JSE_NEP::flt_t **ann_w0, const JSE_NEP::flt_t **ann_b0, const JSE_NEP::flt_t **ann_w1, const JSE_NEP::flt_t *ann_b1, const JSE_NEP::flt_t *ann_c,
    const JSE_NEP::flt_t *zbl_para, const JSE_NEP::flt_t *gn_radial, const JSE_NEP::flt_t *gn_angular, const JSE_NEP::flt_t *gnp_radial, const JSE_NEP::flt_t *gnp_angular,
    JSE_NEP::flt_t *f, JSE_NEP::flt_t *eatom0, JSE_NEP::flt_t *vatom0, JSE_NEP::flt_t *vatom1,
    JSE_NEP::flt_t *g_nl_fx, JSE_NEP::flt_t *g_nl_fy, JSE_NEP::flt_t *g_nl_fz,
    JSE_NEP::flt_t *g_fp, JSE_NEP::flt_t *g_sum_fxyz) {
    
    const int nlocalghost = nlocal + nghost;
    cudaError_t err;
    err = cudaMemset(f, 0, nlocalghost*3L*sizeof(JSE_NEP::flt_t));
    if (err!=cudaSuccess) return (int)err;
    if (eflagEither) {
        err = cudaMemset(eatom0, 0, nlocal*sizeof(JSE_NEP::flt_t));
        if (err!=cudaSuccess) return (int)err;
    }
    if (vflag) {
        err = cudaMemset(vatom0, 0, nlocal*6L*sizeof(JSE_NEP::flt_t));
        if (err!=cudaSuccess) return (int)err;
    }
    if (vflagAtom) {
        err = cudaMemset(vatom1, 0, nlocalghost*9L*sizeof(JSE_NEP::flt_t));
        if (err!=cudaSuccess) return (int)err;
    }
    
    /// begin compute here
    constexpr int tBlockSize = __NEPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (nlocal + tBlockSize-1) / tBlockSize;
    const int tGridSizeLG = (nlocal+nghost + tBlockSize-1) / tBlockSize;
    
    JSE_NEP::initLammpsTypeKernel<<<tGridSizeLG, tBlockSize>>>(nlocal+nghost,
        type, type_map
    );
    JSE_NEP::initLammpsNeiKernel<<<tGridSize, tBlockSize>>>(nlocal,
        rNlSizeR, rNlSizeA, rMgNlIdx,
        nlsize, nlidx,
        posx, posy, posz
    );
    JSE_NEP::computeLammpsKernel_(tGridSize, tBlockSize,
        nlocal, nghost,
        rNlSizeR, rNlSizeA, rMgNlIdx,
        posx, posy, posz, type,
        eflagEither, vflag, vflagAtom,
        eatom0, f, vatom0, vatom1,
        g_nl_fx, g_nl_fy, g_nl_fz,
        atomic_numbers, q_scaler, zbl_para,
        ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
        gn_radial, gn_angular,
        gnp_radial, gnp_angular,
        g_fp, g_sum_fxyz
    );
    
    return (int)cudaDeviceSynchronize();
}

}
