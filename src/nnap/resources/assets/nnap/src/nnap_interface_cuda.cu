#include "nnap_main.hpp"

#include <cstdint>

// >>> NNAPGEN REMOVE
#define __NNAPGEN_CUDA_BLOCKSIZE__ 256
#define __NNAPGENS_ctype__ 1
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

static __global__ void initLammpsNeiKernel(int inum, int nlocalghost,
        int *ilist, flt_t *x, int *type, int *nmerges, int **mergeSorted,
        flt_t **cutsq, int *numneigh, int *firstneigh, int *aLmpType2NNAPType,
        flt_t *rBufNlDx, flt_t *rBufNlDy, flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx,
        int *rBufNeiNum, int *rBufCType) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int i = ilist[ii];
    const flt_t xtmp = x[0*nlocalghost + i];
    const flt_t ytmp = x[1*nlocalghost + i];
    const flt_t ztmp = x[2*nlocalghost + i];
    const int ctype = aLmpType2NNAPType[type[i]];
    rBufCType[ii] = ctype;
    
    const int jnum = numneigh[ii];
    const flt_t *cutsq_ = cutsq[ctype-1];
    const int nmerges_ = nmerges[ctype-1];
    const int *mergeSorted_ = mergeSorted[ctype-1];
    int tNeiNum = 0;
    flt_t cutsqL = ZERO;
    for (int kk = 0; kk < nmerges_; ++kk) {
        const int k = mergeSorted_[kk];
        const flt_t cutsqR = cutsq_[k];
        for (int jj = 0; jj < jnum; ++jj) {
            const int j = firstneigh[jj*inum + ii];
            // Note that dxyz in jse and lammps are defined oppositely
            const flt_t delx = x[0*nlocalghost + j] - xtmp;
            const flt_t dely = x[1*nlocalghost + j] - ytmp;
            const flt_t delz = x[2*nlocalghost + j] - ztmp;
            const flt_t rsq = delx*delx + dely*dely + delz*delz;
            if (rsq>=cutsqL && rsq<cutsqR) {
                rBufNlDx[tNeiNum*inum + ii] = delx;
                rBufNlDy[tNeiNum*inum + ii] = dely;
                rBufNlDz[tNeiNum*inum + ii] = delz;
                rBufNlType[tNeiNum*inum + ii] = aLmpType2NNAPType[type[j]];
                rBufNlIdx[tNeiNum*inum + ii] = j;
                ++tNeiNum;
            }
        }
        rBufNeiNum[(k+1)*inum + ii] = tNeiNum;
        cutsqL = cutsqR;
    }
    // total on k=0
    rBufNeiNum[ii] = tNeiNum;
}

static __global__ void computeLammpsKernel(int inum,
        flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlType,
        int eflag, flt_t *eatom0, int *aBufNeiNum, int *aBufCType,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int ctype = aBufCType[ii];
    const int tNeiNum = aBufNeiNum[ii];
    // manual clear required for backward in force
    flt_t rEng = ZERO;
    for (int jj = 0; jj < tNeiNum; ++jj) {
        rBufGradNlDx[jj*inum + ii] = ZERO;
        rBufGradNlDy[jj*inum + ii] = ZERO;
        rBufGradNlDz[jj*inum + ii] = ZERO;
    }
// >>> NNAPGEN SWITCH
    flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
    fpForwardGpu<__NNAPGENS_ctype__>(inum, ii,
        aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, aBufNeiNum, ctype, rFpOrGradFp,
        aFpHyperParam, aFpParam
    );
    {
        flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
        normedNnForwardGpu<__NNAPGENS_ctype__>(
            ctype, &rEng, rFpOrGradFp,
            aNormParam[ctype-1], aNnParam, rNnGradCache
        );
        // manual clear required for backward in force
        fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, ZERO);
        normedNnBackwardGpu<__NNAPGENS_ctype__>(
            ctype, ONE, rFpOrGradFp,
            aNormParam[ctype-1], aNnParam, rNnGradCache
        );
    }
    fpBackwardGpu<__NNAPGENS_ctype__>(inum, ii,
        aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, aBufNeiNum, ctype, rFpOrGradFp,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz,
        aFpHyperParam, aFpParam
    );
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    if (eflag) {
        eatom0[ii] += rEng;
    }
}

static __global__ void collectLammpsResultsKernel(int inum, int nlocalghost,
        int vflag, int vflagAtom, int cvflagAtom,
        flt_t *f, flt_t *vatom0, flt_t *vatom1,
        flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlIdx, int *aBufNeiNum,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int tNeiNum = aBufNeiNum[ii];
    
    flt_t f0x = ZERO;
    flt_t f0y = ZERO;
    flt_t f0z = ZERO;
    for (int jj = 0; jj < tNeiNum; ++jj) {
        const int j = aBufNlIdx[jj*inum + ii];
        const flt_t fx = rBufGradNlDx[jj*inum + ii];
        const flt_t fy = rBufGradNlDy[jj*inum + ii];
        const flt_t fz = rBufGradNlDz[jj*inum + ii];
        f0x -= fx;
        f0y -= fy;
        f0z -= fz;
        atomicAdd(f + (0*nlocalghost + j), fx);
        atomicAdd(f + (1*nlocalghost + j), fy);
        atomicAdd(f + (2*nlocalghost + j), fz);
        if (vflag) {
            const flt_t dx = aBufNlDx[jj*inum + ii];
            const flt_t dy = aBufNlDy[jj*inum + ii];
            const flt_t dz = aBufNlDz[jj*inum + ii];
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
    atomicAdd(f + (0*nlocalghost + ii), f0x);
    atomicAdd(f + (1*nlocalghost + ii), f0y);
    atomicAdd(f + (2*nlocalghost + ii), f0z);
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
            const int j = g_neighbor_list[jj*number_of_particles + ii];
            const flt_t delx = (flt_t)nl_dx[jj*number_of_particles + ii];
            const flt_t dely = (flt_t)nl_dy[jj*number_of_particles + ii];
            const flt_t delz = (flt_t)nl_dz[jj*number_of_particles + ii];
            const flt_t rsq = delx*delx + dely*dely + delz*delz;
            if (rsq>=cutsqL && rsq<cutsqR) {
                rBufNlDx[tNeiNum*number_of_particles + ii] = delx;
                rBufNlDy[tNeiNum*number_of_particles + ii] = dely;
                rBufNlDz[tNeiNum*number_of_particles + ii] = delz;
                rBufNlType[tNeiNum*number_of_particles + ii] = g_type[j] + 1; // GPUMD start from 0
                rBufNlIdx[tNeiNum*number_of_particles + ii] = j;
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
        rBufGradNlDx[jj*number_of_particles + ii] = ZERO;
        rBufGradNlDy[jj*number_of_particles + ii] = ZERO;
        rBufGradNlDz[jj*number_of_particles + ii] = ZERO;
    }
// >>> NNAPGEN SWITCH
    flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
    fpForwardGpu<__NNAPGENS_ctype__>(number_of_particles, ii,
        aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, aBufNeiNum, ctype, rFpOrGradFp,
        aFpHyperParam, aFpParam
    );
    {
        flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
        normedNnForwardGpu<__NNAPGENS_ctype__>(
            ctype, &rEng, rFpOrGradFp,
            aNormParam[ctype-1], aNnParam, rNnGradCache
        );
        // manual clear required for backward in force
        fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, ZERO);
        normedNnBackwardGpu<__NNAPGENS_ctype__>(
            ctype, ONE, rFpOrGradFp,
            aNormParam[ctype-1], aNnParam, rNnGradCache
        );
    }
    fpBackwardGpu<__NNAPGENS_ctype__>(number_of_particles, ii,
        aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, aBufNeiNum, ctype, rFpOrGradFp,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz,
        aFpHyperParam, aFpParam
    );
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
        const int j = aBufNlIdx[jj*number_of_particles + ii];
        const flt_t fx = rBufGradNlDx[jj*number_of_particles + ii];
        const flt_t fy = rBufGradNlDy[jj*number_of_particles + ii];
        const flt_t fz = rBufGradNlDz[jj*number_of_particles + ii];
        f0x -= fx;
        f0y -= fy;
        f0z -= fz;
        atomicAdd(g_fx + j, fx);
        atomicAdd(g_fy + j, fy);
        atomicAdd(g_fz + j, fz);
        const flt_t dx = aBufNlDx[jj*number_of_particles + ii];
        const flt_t dy = aBufNlDy[jj*number_of_particles + ii];
        const flt_t dz = aBufNlDz[jj*number_of_particles + ii];
        atomicAdd(g_virial + (0*number_of_particles + j), dx*fx);
        atomicAdd(g_virial + (1*number_of_particles + j), dy*fy);
        atomicAdd(g_virial + (2*number_of_particles + j), dz*fz);
        atomicAdd(g_virial + (3*number_of_particles + j), dx*fy);
        atomicAdd(g_virial + (4*number_of_particles + j), dx*fz);
        atomicAdd(g_virial + (5*number_of_particles + j), dy*fz);
        atomicAdd(g_virial + (6*number_of_particles + j), dy*fx);
        atomicAdd(g_virial + (7*number_of_particles + j), dz*fx);
        atomicAdd(g_virial + (8*number_of_particles + j), dz*fy);
    }
    atomicAdd(g_fx + ii, f0x);
    atomicAdd(g_fy + ii, f0y);
    atomicAdd(g_fz + ii, f0z);
}

}

#define __jsefunc__

extern "C" {

#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

__jsefunc__ int jse_nnap_statNeiNumLammps(int *ilist, int *numneigh, int inum, int *numneighMax) {
    int numneighMax_ = 0;
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int jnum = numneigh[i];
        if (jnum > numneighMax_) numneighMax_ = jnum;
    }
    numneighMax[0] = numneighMax_;
    return 0;
}

__jsefunc__ int jse_nnap_lammps2cuda(
    int inum, int nlocalghost, int nlflag, int neighnumMax,
    double **x, int *type,
    int *ilist, int *numneigh, int **firstneigh,
    JSE_NNAP::flt_t *fltBuf, int *intBuf,
    JSE_NNAP::flt_t *cudaX, int *cudaType,
    int *cudaIlist, int *cudaNumneigh, int *cudaFirstneigh) {
    
    cudaError_t tErr;
    for (int i = 0; i < nlocalghost; ++i) {
        fltBuf[0*nlocalghost + i] = (JSE_NNAP::flt_t)x[i][0];
        fltBuf[1*nlocalghost + i] = (JSE_NNAP::flt_t)x[i][1];
        fltBuf[2*nlocalghost + i] = (JSE_NNAP::flt_t)x[i][2];
    }
    tErr = cudaMemcpy(cudaX, fltBuf, nlocalghost*3*sizeof(JSE_NNAP::flt_t), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemcpy(cudaType, type, nlocalghost*sizeof(int), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    if (nlflag) {
        tErr = cudaMemcpy(cudaIlist, ilist, inum*sizeof(int), cudaMemcpyHostToDevice);
        if (tErr!=cudaSuccess) return (int)tErr;
        // reorder in ilist
        for (int ii = 0; ii < inum; ++ii) {
            intBuf[ii] = numneigh[ilist[ii]];
        }
        tErr = cudaMemcpy(cudaNumneigh, intBuf, inum*sizeof(int), cudaMemcpyHostToDevice);
        if (tErr!=cudaSuccess) return (int)tErr;
        // reorder in ilist
        for (int ii = 0; ii < inum; ++ii) {
            int i = ilist[ii];
            int jnum = numneigh[i];
            int *jlist = firstneigh[i];
            for (int jj = 0; jj < jnum; ++jj) {
                intBuf[jj*inum + ii] = jlist[jj] & JSE_LMP_NEIGHMASK;
            }
        }
        tErr = cudaMemcpy(cudaFirstneigh, intBuf, inum*neighnumMax*sizeof(int), cudaMemcpyHostToDevice);
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    return (int)cudaSuccess;
}

__jsefunc__ int jse_nnap_cuda2lammps(
    int inum, int nlocalghost, int eflag, int vflag, int eflagAtom, int vflagAtom, int cvflagAtom,
    double **f, double *engVdwl, double *eatom, double *virial, double **vatom, double **cvatom,
    JSE_NNAP::flt_t *fltBuf, const int *ilist,
    JSE_NNAP::flt_t *cudaF, JSE_NNAP::flt_t *cudaEatom0, JSE_NNAP::flt_t *cudaVatom0, JSE_NNAP::flt_t *cudaVatom1) {
    
    cudaError_t tErr;
    tErr = cudaMemcpy(fltBuf, cudaF, nlocalghost*3*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
    if (tErr!=cudaSuccess) return (int)tErr;
    for (int i = 0; i < nlocalghost; ++i) {
        f[i][0] += (double)fltBuf[0*nlocalghost + i];
        f[i][1] += (double)fltBuf[1*nlocalghost + i];
        f[i][2] += (double)fltBuf[2*nlocalghost + i];
    }
    
    if (eflag) {
        tErr = cudaMemcpy(fltBuf, cudaEatom0, inum*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        // reorder in ilist
        for (int ii = 0; ii < inum; ++ii) {
            const double tEng = (double)fltBuf[ii];
            *engVdwl += tEng;
            if (eflagAtom) eatom[ilist[ii]] += tEng;
        }
    }
    if (vflag) {
        tErr = cudaMemcpy(fltBuf, cudaVatom0, inum*6*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
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
        tErr = cudaMemcpy(fltBuf, cudaVatom1, nlocalghost*9*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
    } else if (vflagAtom) {
        tErr = cudaMemcpy(fltBuf, cudaVatom1, nlocalghost*6*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
    }
    if (tErr!=cudaSuccess) return (int)tErr;
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


__jsefunc__ int jse_nnap_computeLammpsCuda(
    int inum, int nlocalghost, int eflag, int vflag, int eflagAtom, int vflagAtom, int cvflagAtom,
    JSE_NNAP::flt_t *x, int *type, int *ilist, int *nmerges, int **mergeSorted,
    JSE_NNAP::flt_t **cutsq, int *numneigh, int *firstneigh, int *aLmpType2NNAPType,
    JSE_NNAP::flt_t **aFpHyperParam, JSE_NNAP::flt_t **aFpParam, JSE_NNAP::flt_t **aNnParam, JSE_NNAP::flt_t **aNormParam,
    JSE_NNAP::flt_t *f, JSE_NNAP::flt_t *eatom0, JSE_NNAP::flt_t *vatom0, JSE_NNAP::flt_t *vatom1,
    JSE_NNAP::flt_t *rBufNlDx, JSE_NNAP::flt_t *rBufNlDy, JSE_NNAP::flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx, int *rBufNeiNum, int *rBufCType,
    JSE_NNAP::flt_t *rBufGradNlDx, JSE_NNAP::flt_t *rBufGradNlDy, JSE_NNAP::flt_t *rBufGradNlDz) {
    
    cudaError_t tErr;
    tErr = cudaMemset(f, 0, nlocalghost*3*sizeof(JSE_NNAP::flt_t));
    if (tErr!=cudaSuccess) return (int)tErr;
    if (eflag||eflagAtom) {
        tErr = cudaMemset(eatom0, 0, inum*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    if (vflag) {
        tErr = cudaMemset(vatom0, 0, inum*6*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    if (cvflagAtom) {
        tErr = cudaMemset(vatom1, 0, nlocalghost*9*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    } else if (vflagAtom) {
        tErr = cudaMemset(vatom1, 0, nlocalghost*6*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    
    /// begin compute here
    constexpr int tBlockSize = __NNAPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (inum + tBlockSize-1) / tBlockSize;
    
    JSE_NNAP::initLammpsNeiKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        ilist, x, type, nmerges, mergeSorted,
        cutsq, numneigh, firstneigh, aLmpType2NNAPType,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx,
        rBufNeiNum, rBufCType
    );
    JSE_NNAP::computeLammpsKernel<<<tGridSize, tBlockSize>>>(inum,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType,
        eflag||eflagAtom, eatom0, rBufNeiNum, rBufCType,
        aFpHyperParam, aFpParam, aNormParam, aNnParam,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    JSE_NNAP::collectLammpsResultsKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        vflag, vflagAtom, cvflagAtom,
        f, vatom0, vatom1,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlIdx, rBufNeiNum,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
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
