#include "nnap_main.hpp"

#include <cstdint>

// >>> NNAPGEN REMOVE
#define __NNAPGEN_CUDA_BLOCKSIZE__ 256
#define __NNAPGENS_ctype__ 1
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

static __global__ void initLammpsTypeKernel(int nlocalghost,
        int *type, int *aLmpType2NNAPType) {
    
    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocalghost) return;
    
    type[i] = aLmpType2NNAPType[type[i]];
}

static __global__ void initLammpsNeiKernel(int nlocal,
        int *nmerges, int **mergeSorted,
        flt_t **cutsq, int *nlsize, int *nlidx,
        int *rMgNlSize, int *rMgNlIdx,
        flt_t *posx, flt_t *posy, flt_t *posz, int *type) {
    
    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocal) return;
    
    const flt_t xi = posx[i];
    const flt_t yi = posy[i];
    const flt_t zi = posz[i];
    const int ctype = type[i];
    
    const int jnum = nlsize[i];
    const flt_t *cutsq_ = cutsq[ctype-1];
    const int nmerges_ = nmerges[ctype-1];
    const int *mergeSorted_ = mergeSorted[ctype-1];
    int tNlSize = 0;
    flt_t cutsqL = ZERO;
    for (int kk = 0; kk < nmerges_; ++kk) {
        const int k = mergeSorted_[kk];
        const flt_t cutsqR = cutsq_[k];
        for (int jj = 0; jj < jnum; ++jj) {
            const int j = nlidx[(size_t)jj*nlocal + i];
            // Note that dxyz in jse and lammps are defined oppositely
            const flt_t dx = posx[j] - xi;
            const flt_t dy = posy[j] - yi;
            const flt_t dz = posz[j] - zi;
            const flt_t rsq = dx*dx + dy*dy + dz*dz;
            if (rsq>=cutsqL && rsq<cutsqR) {
                rMgNlIdx[(size_t)tNlSize*nlocal + i] = j;
                ++tNlSize;
            }
        }
        rMgNlSize[(k+1)*nlocal + i] = tNlSize;
        cutsqL = cutsqR;
    }
    // total on k=0
    rMgNlSize[i] = tNlSize;
}

template <int EEITHER, int VTOTAL, int VATOM>
static __global__ void computeLammpsKernel(int nlocal, int nghost,
        int *aMgNlSize, int *aMgNlIdx,
        flt_t *posx, flt_t *posy, flt_t *posz, int *type,
        flt_t *eatom0, flt_t *f, flt_t *vatom0, flt_t *vatom1,
        flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam) {
    
    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocal) return;
    
    const int nlocalghost = nlocal + nghost;
    const int ctype = type[i];
    const int tNlSize = aMgNlSize[i];
    
    // manual clear required for backward in force
    flt_t eng = ZERO;
    for (int jj = 0; jj < tNlSize; ++jj) {
        rGradNlDx[(size_t)jj*nlocal + i] = ZERO;
        rGradNlDy[(size_t)jj*nlocal + i] = ZERO;
        rGradNlDz[(size_t)jj*nlocal + i] = ZERO;
    }
    
// >>> NNAPGEN SWITCH
    flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
    fpForwardGpu<__NNAPGENS_ctype__>(nlocal, i, ctype,
        aMgNlSize, aMgNlIdx, rFpOrGradFp,
        posx, posy, posz, type,
        aFpHyperParam, aFpParam
    );
    {
        flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
        normedNnForwardGpu<__NNAPGENS_ctype__>(
            ctype, &eng, rFpOrGradFp,
            aNormParam, aNnParam, rNnGradCache
        );
        // manual clear required for backward in force
        fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, ZERO);
        normedNnBackwardGpu<__NNAPGENS_ctype__>(
            ctype, ONE, rFpOrGradFp,
            aNormParam, aNnParam, rNnGradCache
        );
    }
    fpBackwardGpu<__NNAPGENS_ctype__>(nlocal, i, ctype,
        aMgNlSize, aMgNlIdx, rFpOrGradFp,
        posx, posy, posz, type,
        rGradNlDx, rGradNlDy, rGradNlDz,
        aFpHyperParam, aFpParam
    );
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    
    if (EEITHER) {
        eatom0[i] += eng;
    }
    flt_t f0xi = ZERO, f0yi = ZERO, f0zi = ZERO;
    flt_t v0xxi = ZERO, v0yyi = ZERO, v0zzi = ZERO;
    flt_t v0xyi = ZERO, v0xzi = ZERO, v0yzi = ZERO;
    const flt_t xi = (VTOTAL||VATOM) ? posx[i] : ZERO;
    const flt_t yi = (VTOTAL||VATOM) ? posy[i] : ZERO;
    const flt_t zi = (VTOTAL||VATOM) ? posz[i] : ZERO;
    for (int jj = 0; jj < tNlSize; ++jj) {
        const int j = aMgNlIdx[(size_t)jj*nlocal + i];
        const flt_t fxj = rGradNlDx[(size_t)jj*nlocal + i];
        const flt_t fyj = rGradNlDy[(size_t)jj*nlocal + i];
        const flt_t fzj = rGradNlDz[(size_t)jj*nlocal + i];
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
        int *aMgNlSize, int *aMgNlIdx,
        flt_t *posx, flt_t *posy, flt_t *posz, int *type,
        int eflagEither,
        flt_t *eatom0, flt_t *f, flt_t *vatom0, flt_t *vatom1,
        flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam) {
    
    if (eflagEither) {
        computeLammpsKernel<TRUE, VTOTAL, VATOM>
                     <<<aGridSize, aBlockSize>>>(nlocal, nghost,
            aMgNlSize, aMgNlIdx,
            posx, posy, posz, type,
            eatom0, f, vatom0, vatom1,
            rGradNlDx, rGradNlDy, rGradNlDz,
            aFpHyperParam, aFpParam, aNormParam, aNnParam
        );
    } else {
        computeLammpsKernel<FALSE, VTOTAL, VATOM>
                     <<<aGridSize, aBlockSize>>>(nlocal, nghost,
            aMgNlSize, aMgNlIdx,
            posx, posy, posz, type,
            eatom0, f, vatom0, vatom1,
            rGradNlDx, rGradNlDy, rGradNlDz,
            aFpHyperParam, aFpParam, aNormParam, aNnParam
        );
    }
}
static void computeLammpsKernel_(int aGridSize, int aBlockSize,
        int nlocal, int nghost,
        int *aMgNlSize, int *aMgNlIdx,
        flt_t *posx, flt_t *posy, flt_t *posz, int *type,
        int eflagEither, int vflag, int vflagAtom,
        flt_t *eatom0, flt_t *f, flt_t *vatom0, flt_t *vatom1,
        flt_t *rGradNlDx, flt_t *rGradNlDy, flt_t *rGradNlDz,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam) {
    
    if (vflagAtom) {
        computeLammpsKernel_<TRUE, TRUE>(aGridSize, aBlockSize,
            nlocal, nghost,
            aMgNlSize, aMgNlIdx,
            posx, posy, posz, type,
            eflagEither,
            eatom0, f, vatom0, vatom1,
            rGradNlDx, rGradNlDy, rGradNlDz,
            aFpHyperParam, aFpParam, aNormParam, aNnParam
        );
    } else if (vflag) {
        computeLammpsKernel_<TRUE, FALSE>(aGridSize, aBlockSize,
            nlocal, nghost,
            aMgNlSize, aMgNlIdx,
            posx, posy, posz, type,
            eflagEither,
            eatom0, f, vatom0, vatom1,
            rGradNlDx, rGradNlDy, rGradNlDz,
            aFpHyperParam, aFpParam, aNormParam, aNnParam
        );
    } else {
        computeLammpsKernel_<FALSE, FALSE>(aGridSize, aBlockSize,
            nlocal, nghost,
            aMgNlSize, aMgNlIdx,
            posx, posy, posz, type,
            eflagEither,
            eatom0, f, vatom0, vatom1,
            rGradNlDx, rGradNlDy, rGradNlDz,
            aFpHyperParam, aFpParam, aNormParam, aNnParam
        );
    }
}


static __global__ void initGpumdNeiKernel(int number_of_particles, int N1, int N2,
        const int *g_neighbor_number, const int *g_neighbor_list,
        const float *nl_dx, const float *nl_dy, const float *nl_dz, const int *g_type,
        const int *nmerges, const int **mergeSorted, const flt_t **cutsq,
        flt_t *rBufNlDx, flt_t *rBufNlDy, flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx,
        int *rBufNeiNum) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x + N1);
    if (ii >= N2) return;
    
    const int ctypeMM = g_type[ii]; // GPUMD start from 0
    const int jnum = g_neighbor_number[ii];
    const flt_t *cutsq_ = cutsq[ctypeMM];
    const int nmerges_ = nmerges[ctypeMM];
    const int *mergeSorted_ = mergeSorted[ctypeMM];
    int tNeiNum = 0;
    flt_t cutsqL = ZERO;
    for (int kk = 0; kk < nmerges_; ++kk) {
        const int k = mergeSorted_[kk];
        const flt_t cutsqR = cutsq_[k];
        for (int jj = 0; jj < jnum; ++jj) {
            const int j = g_neighbor_list[(size_t)jj*number_of_particles + ii];
            const flt_t delx = (flt_t)nl_dx[(size_t)jj*number_of_particles + ii];
            const flt_t dely = (flt_t)nl_dy[(size_t)jj*number_of_particles + ii];
            const flt_t delz = (flt_t)nl_dz[(size_t)jj*number_of_particles + ii];
            const flt_t rsq = delx*delx + dely*dely + delz*delz;
            if (rsq>=cutsqL && rsq<cutsqR) {
                rBufNlDx[(size_t)tNeiNum*number_of_particles + ii] = delx;
                rBufNlDy[(size_t)tNeiNum*number_of_particles + ii] = dely;
                rBufNlDz[(size_t)tNeiNum*number_of_particles + ii] = delz;
                rBufNlType[(size_t)tNeiNum*number_of_particles + ii] = g_type[j] + 1; // GPUMD start from 0
                rBufNlIdx[(size_t)tNeiNum*number_of_particles + ii] = j;
                ++tNeiNum;
            }
        }
        rBufNeiNum[(k+1)*number_of_particles + ii] = tNeiNum;
        cutsqL = cutsqR;
    }
    // total on k=0
    rBufNeiNum[ii] = tNeiNum;
}

static __global__ void computeGpumdKernel(int number_of_particles, int N1, int N2,
        flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlType,
        double *g_potential, int *aBufNeiNum, const int *g_type,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x + N1);
    if (ii >= N2) return;
    
    const int ctype = g_type[ii] + 1; // GPUMD start from 0
    const int tNeiNum = aBufNeiNum[ii];
    // manual clear required for backward in force
    flt_t rEng = ZERO;
    for (int jj = 0; jj < tNeiNum; ++jj) {
        rBufGradNlDx[(size_t)jj*number_of_particles + ii] = ZERO;
        rBufGradNlDy[(size_t)jj*number_of_particles + ii] = ZERO;
        rBufGradNlDz[(size_t)jj*number_of_particles + ii] = ZERO;
    }
// >>> NNAPGEN SWITCH
    flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
    // fpForwardGpu<__NNAPGENS_ctype__>(number_of_particles, ii,
    //     aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, aBufNeiNum, ctype, rFpOrGradFp,
    //     aFpHyperParam, aFpParam
    // );
    {
        flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
        normedNnForwardGpu<__NNAPGENS_ctype__>(
            ctype, &rEng, rFpOrGradFp,
            aNormParam, aNnParam, rNnGradCache
        );
        // manual clear required for backward in force
        fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, ZERO);
        normedNnBackwardGpu<__NNAPGENS_ctype__>(
            ctype, ONE, rFpOrGradFp,
            aNormParam, aNnParam, rNnGradCache
        );
    }
    // fpBackwardGpu<__NNAPGENS_ctype__>(number_of_particles, ii,
    //     aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, aBufNeiNum, ctype, rFpOrGradFp,
    //     rBufGradNlDx, rBufGradNlDy, rBufGradNlDz,
    //     aFpHyperParam, aFpParam
    // );
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    g_potential[ii] += rEng;
}

static __global__ void collectGpumdResultsKernel(int number_of_particles, int N1, int N2,
        double *g_fx, double *g_fy, double *g_fz, double *g_virial,
        const flt_t *aBufNlDx, const flt_t *aBufNlDy, const flt_t *aBufNlDz,
        const int *aBufNlIdx, const int *aBufNeiNum,
        const flt_t *rBufGradNlDx, const flt_t *rBufGradNlDy, const flt_t *rBufGradNlDz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x + N1);
    if (ii >= N2) return;
    
    const int tNeiNum = aBufNeiNum[ii];
    
    flt_t f0x = ZERO;
    flt_t f0y = ZERO;
    flt_t f0z = ZERO;
    for (int jj = 0; jj < tNeiNum; ++jj) {
        const int j = aBufNlIdx[(size_t)jj*number_of_particles + ii];
        const flt_t fx = rBufGradNlDx[(size_t)jj*number_of_particles + ii];
        const flt_t fy = rBufGradNlDy[(size_t)jj*number_of_particles + ii];
        const flt_t fz = rBufGradNlDz[(size_t)jj*number_of_particles + ii];
        f0x += fx;
        f0y += fy;
        f0z += fz;
        atomicAdd(g_fx + j, -fx);
        atomicAdd(g_fy + j, -fy);
        atomicAdd(g_fz + j, -fz);
        const flt_t dx = aBufNlDx[(size_t)jj*number_of_particles + ii];
        const flt_t dy = aBufNlDy[(size_t)jj*number_of_particles + ii];
        const flt_t dz = aBufNlDz[(size_t)jj*number_of_particles + ii];
        atomicAdd(g_virial + (0*number_of_particles + j), -dx*fx);
        atomicAdd(g_virial + (1*number_of_particles + j), -dy*fy);
        atomicAdd(g_virial + (2*number_of_particles + j), -dz*fz);
        atomicAdd(g_virial + (3*number_of_particles + j), -dx*fy);
        atomicAdd(g_virial + (4*number_of_particles + j), -dx*fz);
        atomicAdd(g_virial + (5*number_of_particles + j), -dy*fz);
        atomicAdd(g_virial + (6*number_of_particles + j), -dy*fx);
        atomicAdd(g_virial + (7*number_of_particles + j), -dz*fx);
        atomicAdd(g_virial + (8*number_of_particles + j), -dz*fy);
    }
    atomicAdd(g_fx + ii, f0x);
    atomicAdd(g_fy + ii, f0y);
    atomicAdd(g_fz + ii, f0z);
}

}

#define __jsefunc__

extern "C" {

__jsefunc__ int jse_nnap_cuda2lammps(
    int nlocal, int nghost, int eflag, int eflagAtom, int vflag, int vflagAtom, int cvflagAtom,
    double **f, double *engVdwl, double *eatom, double *virial, double **vatom, double **cvatom,
    int *ilist, JSE_NNAP::flt_t *fltBuf, JSE_NNAP::flt_t *cudaF,
    JSE_NNAP::flt_t *cudaEatom0, JSE_NNAP::flt_t *cudaVatom0, JSE_NNAP::flt_t *cudaVatom1) {
    
    const int nlocalghost = nlocal + nghost;
    cudaError_t tErr;
    tErr = cudaMemcpy(fltBuf, cudaF, nlocalghost*3L*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
    if (tErr!=cudaSuccess) return (int)tErr;
    for (int ii = 0; ii < nlocalghost; ++ii) {
        const int i = ilist[ii];
        f[i][0] += (double)fltBuf[0L*nlocalghost + ii];
        f[i][1] += (double)fltBuf[1L*nlocalghost + ii];
        f[i][2] += (double)fltBuf[2L*nlocalghost + ii];
    }
    
    if (eflag || eflagAtom) {
        tErr = cudaMemcpy(fltBuf, cudaEatom0, nlocal*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        for (int ii = 0; ii < nlocal; ++ii) {
            const double tEng = (double)fltBuf[ii];
            *engVdwl += tEng;
            if (eflagAtom) eatom[ilist[ii]] += tEng;
        }
    }
    if (vflag) {
        tErr = cudaMemcpy(fltBuf, cudaVatom0, nlocal*6L*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
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
        tErr = cudaMemcpy(fltBuf, cudaVatom1, nlocalghost*9L*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
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


__jsefunc__ int jse_nnap_computeLammpsCuda(
    int nlocal, int nghost, int eflagEither, int vflag, int vflagAtom,
    JSE_NNAP::flt_t *posx, JSE_NNAP::flt_t *posy, JSE_NNAP::flt_t *posz, int *type,
    int *nmerges, int **mergeSorted, JSE_NNAP::flt_t **cutsq, int *nlsize, int *nlidx, int *aLmpType2NNAPType,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t **aNormParam,
    JSE_NNAP::flt_t *f, JSE_NNAP::flt_t *eatom0, JSE_NNAP::flt_t *vatom0, JSE_NNAP::flt_t *vatom1,
    JSE_NNAP::flt_t *rGradNlDx, JSE_NNAP::flt_t *rGradNlDy, JSE_NNAP::flt_t *rGradNlDz,
    int *rMgNlSize, int *rMgNlIdx) {
    
    const int nlocalghost = nlocal + nghost;
    cudaError_t tErr;
    tErr = cudaMemset(f, 0, nlocalghost*3L*sizeof(JSE_NNAP::flt_t));
    if (tErr!=cudaSuccess) return (int)tErr;
    if (eflagEither) {
        tErr = cudaMemset(eatom0, 0, nlocal*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    if (vflag) {
        tErr = cudaMemset(vatom0, 0, nlocal*6L*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    if (vflagAtom) {
        tErr = cudaMemset(vatom1, 0, nlocalghost*9L*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    
    /// begin compute here
    constexpr int tBlockSize = __NNAPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (nlocal + tBlockSize-1) / tBlockSize;
    const int tGridSizeLG = (nlocal+nghost + tBlockSize-1) / tBlockSize;
    
    JSE_NNAP::initLammpsTypeKernel<<<tGridSizeLG, tBlockSize>>>(nlocal+nghost,
        type, aLmpType2NNAPType
    );
    JSE_NNAP::initLammpsNeiKernel<<<tGridSize, tBlockSize>>>(nlocal,
        nmerges, mergeSorted,
        cutsq, nlsize, nlidx,
        rMgNlSize, rMgNlIdx,
        posx, posy, posz, type
    );
    JSE_NNAP::computeLammpsKernel_(tGridSize, tBlockSize,
        nlocal, nghost,
        rMgNlSize, rMgNlIdx,
        posx, posy, posz, type,
        eflagEither, vflag, vflagAtom,
        eatom0, f, vatom0, vatom1,
        rGradNlDx, rGradNlDy, rGradNlDz,
        aFpHyperParam, aFpParam, aNormParam, aNnParam
    );
    
    return (int)cudaDeviceSynchronize();
}

__jsefunc__ int jse_nnap_computeGPUMD(
    int number_of_particles, int N1, int N2,
    const int *g_neighbor_number, const int *g_neighbor_list,
    const float *nl_dx, const float *nl_dy, const float *nl_dz, const int *g_type,
    const int *nmerges, const int **mergeSorted, const JSE_NNAP::flt_t **cutsq,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t **aNormParam,
    double *g_fx, double *g_fy, double *g_fz, double *g_virial, double *g_potential,
    JSE_NNAP::flt_t *rBufNlDx, JSE_NNAP::flt_t *rBufNlDy, JSE_NNAP::flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx, int *rBufNeiNum,
    JSE_NNAP::flt_t *rBufGradNlDx, JSE_NNAP::flt_t *rBufGradNlDy, JSE_NNAP::flt_t *rBufGradNlDz) {
    
    constexpr int tBlockSize = __NNAPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (N2 - N1 - 1) / tBlockSize + 1; // copy from gpumd
    
    JSE_NNAP::initGpumdNeiKernel<<<tGridSize, tBlockSize>>>(number_of_particles, N1, N2,
        g_neighbor_number, g_neighbor_list,
        nl_dx, nl_dy, nl_dz, g_type,
        nmerges, mergeSorted, cutsq,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx,
        rBufNeiNum
    );
    JSE_NNAP::computeGpumdKernel<<<tGridSize, tBlockSize>>>(number_of_particles, N1, N2,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType,
        g_potential, rBufNeiNum, g_type,
        aFpHyperParam, aFpParam, aNormParam, aNnParam,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    JSE_NNAP::collectGpumdResultsKernel<<<tGridSize, tBlockSize>>>(number_of_particles, N1, N2,
        g_fx, g_fy, g_fz, g_virial,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlIdx, rBufNeiNum,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    
    return (int)cudaDeviceSynchronize();
}

}
