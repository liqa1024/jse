#include "nnap_interface_cuda.h"
#include "nnap_main.hpp"

#include <cstdint>

// >>> NNAPGEN REMOVE
#define __NNAPGEN_CUDA_BLOCKSIZE__ 256
#define __NNAPGENS_ctype__ 1
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

static __global__ void initLammpsNeiKernel(int inum, int nlocalghost, int neighnumMax,
        int *ilist, flt_t *x, int *type,
        flt_t *cutsq, int *numneigh, int *firstneigh, int *aLmpType2NNAPType,
        flt_t *rBufNlDx, flt_t *rBufNlDy, flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx,
        int *rBufNeiNum, int *rBufCType) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int i = ilist[ii];
    const flt_t xtmp = x[0*nlocalghost + i];
    const flt_t ytmp = x[1*nlocalghost + i];
    const flt_t ztmp = x[2*nlocalghost + i];
    const int typei = type[i];
    
    const int jnum = numneigh[ii];
    int tNeiNum = 0;
    for (int jj = 0; jj < jnum; ++jj) {
        const int j = firstneigh[jj*inum + ii];
        // Note that dxyz in jse and lammps are defined oppositely
        const flt_t delx = x[0*nlocalghost + j] - xtmp;
        const flt_t dely = x[1*nlocalghost + j] - ytmp;
        const flt_t delz = x[2*nlocalghost + j] - ztmp;
        const flt_t rsq = delx*delx + dely*dely + delz*delz;
        if (rsq < cutsq[typei]) {
            // TODO: tNeiNum*inum + ii
            rBufNlDx[ii*neighnumMax + tNeiNum] = delx;
            rBufNlDy[ii*neighnumMax + tNeiNum] = dely;
            rBufNlDz[ii*neighnumMax + tNeiNum] = delz;
            rBufNlType[ii*neighnumMax + tNeiNum] = aLmpType2NNAPType[type[j]];
            rBufNlIdx[ii*neighnumMax + tNeiNum] = j;
            ++tNeiNum;
        }
    }
    rBufNeiNum[ii] = tNeiNum;
    rBufCType[ii] = aLmpType2NNAPType[typei];
}

static __global__ void collectLammpsResultsKernel(int inum, int nlocalghost, int neighnumMax,
        int vflag, int vflagAtom, int cvflagAtom,
        flt_t *f0, flt_t *f1, flt_t *vatom0, flt_t *vatom1,
        flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlIdx, int *aBufNeiNum,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int tNeiNum = aBufNeiNum[ii];
    
    flt_t f0x = ZERO;
    flt_t f0y = ZERO;
    flt_t f0z = ZERO;
    for (int jj = 0; jj < tNeiNum; ++jj) {
        // TODO: jj*inum + ii
        const int j = aBufNlIdx[ii*neighnumMax + jj];
        const flt_t fx = rBufGradNlDx[ii*neighnumMax + jj];
        const flt_t fy = rBufGradNlDy[ii*neighnumMax + jj];
        const flt_t fz = rBufGradNlDz[ii*neighnumMax + jj];
        f0x -= fx;
        f0y -= fy;
        f0z -= fz;
        atomicAdd(f1 + (0*nlocalghost + j), fx);
        atomicAdd(f1 + (1*nlocalghost + j), fy);
        atomicAdd(f1 + (2*nlocalghost + j), fz);
        if (vflag) {
            // TODO: jj*inum + ii
            const flt_t dx = aBufNlDx[ii*neighnumMax + jj];
            const flt_t dy = aBufNlDy[ii*neighnumMax + jj];
            const flt_t dz = aBufNlDz[ii*neighnumMax + jj];
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

static __global__ void computeLammpsKernel(int inum, int neighnumMax,
        flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlType,
        int eflag, flt_t *eatom0, int *aBufNeiNum, int *aBufCType,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int ctype = aBufCType[ii];
    const int tNeiNum = aBufNeiNum[ii];
    // TODO: jj*inum + ii
    flt_t *tNlDx = aBufNlDx + ii*neighnumMax;
    flt_t *tNlDy = aBufNlDy + ii*neighnumMax;
    flt_t *tNlDz = aBufNlDz + ii*neighnumMax;
    int *tNlType = aBufNlType + ii*neighnumMax;
    flt_t *rGradNlDx = rBufGradNlDx + ii*neighnumMax;
    flt_t *rGradNlDy = rBufGradNlDy + ii*neighnumMax;
    flt_t *rGradNlDz = rBufGradNlDz + ii*neighnumMax;
    flt_t rEng;
    // manual clear required for backward in force
    // TODO: jj*inum + ii
    for (int jj = 0; jj < tNeiNum; ++jj) {
        rGradNlDx[jj] = ZERO;
        rGradNlDy[jj] = ZERO;
        rGradNlDz[jj] = ZERO;
    }
// >>> NNAPGEN SWITCH
    flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
    flt_t rFpForwardCache[__NNAPGENX_FP_SIZE_CACHEF__];
    fpForward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFpOrGradFp,
        aFpHyperParam, aFpParam, rFpForwardCache
    );
    flt_t rNnGradCache[__NNAPGENX_NN_SIZE_CACHEG__];
    flt_t rNnHiddenCache[__NNAPGENX_NN_SIZE_CACHEH__];
    normedNnForward<__NNAPGENS_ctype__, TRUE>(
        ctype, rFpOrGradFp, aNormParam[ctype-1], aNnParam,
        rNnGradCache, rNnHiddenCache, &rEng
    );
    // manual clear required for backward in force
    fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, ZERO);
    normedNnBackward<__NNAPGENS_ctype__>(
        ctype, rFpOrGradFp, aNormParam[ctype-1], aNnParam,
        rNnGradCache, rNnHiddenCache, ONE
    );
    flt_t rFpBackwardCache[__NNAPGENX_FP_SIZE_CACHEB__];
    fpBackward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFpOrGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        aFpHyperParam, aFpParam, rFpForwardCache, rFpBackwardCache
    );
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    if (eflag) {
        eatom0[ii] += rEng;
    }
}

static void computeLammpsCuda0(int inum, int nlocalghost, int neighnumMax,
        int eflag, int vflag, int vflagAtom, int cvflagAtom,
        int *ilist, flt_t *x, int *type,
        flt_t *cutsq, int *numneigh, int *firstneigh, int *aLmpType2NNAPType,
        flt_t *rBufNlDx, flt_t *rBufNlDy, flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx,
        int *rBufNeiNum, int *rBufCType,
        flt_t *f0, flt_t *f1, flt_t *eatom0, flt_t *vatom0, flt_t *vatom1,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz) {
    constexpr int tBlockSize = __NNAPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (inum + tBlockSize-1) / tBlockSize;
    
    initLammpsNeiKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost, neighnumMax,
        ilist, x, type,
        cutsq, numneigh, firstneigh, aLmpType2NNAPType,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx,
        rBufNeiNum, rBufCType
    );
    
    computeLammpsKernel<<<tGridSize, tBlockSize>>>(inum, neighnumMax,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType,
        eflag, eatom0, rBufNeiNum, rBufCType,
        aFpHyperParam, aFpParam, aNormParam, aNnParam,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    
    collectLammpsResultsKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost, neighnumMax,
        vflag, vflagAtom, cvflagAtom,
        f0, f1, vatom0, vatom1,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlIdx, rBufNeiNum,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    postLammpsResultsKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        ilist, f0, f1
    );
}


static __global__ void initGpumdNeiKernel(int number_of_particles, int N1, int N2, int neighnumMax,
        const int *g_neighbor_number, const int *g_neighbor_list,
        const float *nl_dx, const float *nl_dy, const float *nl_dz, const int *g_type,
        flt_t *rBufNlDx, flt_t *rBufNlDy, flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx) {
    const int n1 = (int)(blockIdx.x * blockDim.x + threadIdx.x + N1);
    if (n1 >= N2) return;
    
    const int neighbor_number = g_neighbor_number[n1];
    for (int i1 = 0; i1 < neighbor_number; ++i1) {
        int n2 = g_neighbor_list[n1 + number_of_particles*i1];
        const flt_t delx = nl_dx[n1 + number_of_particles*i1];
        const flt_t dely = nl_dy[n1 + number_of_particles*i1];
        const flt_t delz = nl_dz[n1 + number_of_particles*i1];
        // TODO: i1*inum + n1
        rBufNlDx[n1*neighnumMax + i1] = delx;
        rBufNlDy[n1*neighnumMax + i1] = dely;
        rBufNlDz[n1*neighnumMax + i1] = delz;
        rBufNlType[n1*neighnumMax + i1] = g_type[n2] + 1; // GPUMD start from 0
        rBufNlIdx[n1*neighnumMax + i1] = n2;
    }
}

static __global__ void computeGpumdKernel(int N1, int N2, int neighnumMax,
    flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlType,
    double *g_potential, const int *aBufNeiNum, const int *aBufCType,
    flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam,
    flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x + N1);
    if (ii >= N2) return;
    
    const int ctype = aBufCType[ii] + 1;// GPUMD start from 0
    const int tNeiNum = aBufNeiNum[ii];
    // TODO: jj*inum + ii
    flt_t *tNlDx = aBufNlDx + ii*neighnumMax;
    flt_t *tNlDy = aBufNlDy + ii*neighnumMax;
    flt_t *tNlDz = aBufNlDz + ii*neighnumMax;
    int *tNlType = aBufNlType + ii*neighnumMax;
    flt_t *rGradNlDx = rBufGradNlDx + ii*neighnumMax;
    flt_t *rGradNlDy = rBufGradNlDy + ii*neighnumMax;
    flt_t *rGradNlDz = rBufGradNlDz + ii*neighnumMax;
    flt_t rEng;
    // manual clear required for backward in force
    // TODO: jj*inum + ii
    for (int jj = 0; jj < tNeiNum; ++jj) {
        rGradNlDx[jj] = ZERO;
        rGradNlDy[jj] = ZERO;
        rGradNlDz[jj] = ZERO;
    }
// >>> NNAPGEN SWITCH
    flt_t rFpOrGradFp[__NNAPGENX_FP_SIZE__];
    flt_t rFpForwardCache[__NNAPGENX_FP_SIZE_CACHEF__];
    fpForward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFpOrGradFp,
        aFpHyperParam, aFpParam, rFpForwardCache
    );
    flt_t rNnGradCache[__NNAPGENX_NN_SIZE_CACHEG__];
    flt_t rNnHiddenCache[__NNAPGENX_NN_SIZE_CACHEH__];
    normedNnForward<__NNAPGENS_ctype__, TRUE>(
        ctype, rFpOrGradFp, aNormParam[ctype-1], aNnParam,
        rNnGradCache, rNnHiddenCache, &rEng
    );
    // manual clear required for backward in force
    fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, ZERO);
    normedNnBackward<__NNAPGENS_ctype__>(
        ctype, rFpOrGradFp, aNormParam[ctype-1], aNnParam,
        rNnGradCache, rNnHiddenCache, ONE
    );
    flt_t rFpBackwardCache[__NNAPGENX_FP_SIZE_CACHEB__];
    fpBackward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFpOrGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        aFpHyperParam, aFpParam, rFpForwardCache, rFpBackwardCache
    );
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    g_potential[ii] += rEng;
}

static __global__ void collectGpumdResultsKernel(int number_of_particles, int N1, int N2, int neighnumMax,
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
        // TODO: jj*inum + ii
        const int j = aBufNlIdx[ii*neighnumMax + jj];
        const flt_t fx = rBufGradNlDx[ii*neighnumMax + jj];
        const flt_t fy = rBufGradNlDy[ii*neighnumMax + jj];
        const flt_t fz = rBufGradNlDz[ii*neighnumMax + jj];
        f0x -= fx;
        f0y -= fy;
        f0z -= fz;
        atomicAdd(g_fx + j, fx);
        atomicAdd(g_fy + j, fy);
        atomicAdd(g_fz + j, fz);
        // TODO: jj*inum + ii
        const flt_t dx = aBufNlDx[ii*neighnumMax + jj];
        const flt_t dy = aBufNlDy[ii*neighnumMax + jj];
        const flt_t dz = aBufNlDz[ii*neighnumMax + jj];
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

extern "C" {

#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_statNeiNumLammps(void *aDataIn, void *rDataOut) {
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

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_lammps2cuda(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    
    int inum = tNums[0];
    int nlocalghost = tNums[1];
    int nlflag = tNums[2];
    int neighnumMax = tNums[3];
    
    double **x = (double **)tDataIn[1];
    int *type = (int *)tDataIn[2];
    int *ilist = nlflag ? (int *)tDataIn[3] : NULL;
    int *numneigh = nlflag ? (int *)tDataIn[4] : NULL;
    int **firstneigh = nlflag ? (int **)tDataIn[5] : NULL;
    
    JSE_NNAP::flt_t *fltBuf = (JSE_NNAP::flt_t *)tDataOut[0];
    int *intBuf = (int *)tDataOut[1];
    JSE_NNAP::flt_t *cudaX = (JSE_NNAP::flt_t *)tDataOut[2];
    int *cudaType = (int *)tDataOut[3];
    int *cudaIlist = nlflag ? (int *)tDataOut[4] : NULL;
    int *cudaNumneigh = nlflag ? (int *)tDataOut[5] : NULL;
    int *cudaFirstneigh = nlflag ? (int *)tDataOut[6] : NULL;
    
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

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_cuda2lammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    
    int inum = tNums[0];
    int nlocalghost = tNums[1];
    int eflag = tNums[2];
    int vflag = tNums[3];
    int eflagAtom = tNums[4];
    int vflagAtom = tNums[5];
    int cvflagAtom = tNums[6];
    
    double **f = (double **)tDataOut[0];
    double *engVdwl = (double *)tDataOut[1];
    double *eatom = (double *)tDataOut[2];
    double *virial = (double *)tDataOut[3];
    double **vatom = (double **)tDataOut[4];
    double **cvatom = (double **)tDataOut[5];
    
    JSE_NNAP::flt_t *fltBuf = (JSE_NNAP::flt_t *)tDataIn[1];
    int *ilist = (int *)tDataIn[2];
    JSE_NNAP::flt_t *cudaF1 = (JSE_NNAP::flt_t *)tDataIn[3];
    JSE_NNAP::flt_t *cudaEatom0 = (JSE_NNAP::flt_t *)tDataIn[4];
    JSE_NNAP::flt_t *cudaVatom0 = (JSE_NNAP::flt_t *)tDataIn[5];
    JSE_NNAP::flt_t *cudaVatom1 = (JSE_NNAP::flt_t *)tDataIn[6];
    
    cudaError_t tErr;
    tErr = cudaMemcpy(fltBuf, cudaF1, nlocalghost*3*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
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


JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_computeLammpsCuda(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;

    int *tCpuNums = (int *)tDataIn[0];
    
    int inum = tCpuNums[0];
    int nlocalghost = tCpuNums[1];
    int neighnumMax = tCpuNums[2];
    int eflag = tCpuNums[3];
    int vflag = tCpuNums[4];
    int eflagAtom = tCpuNums[5];
    int vflagAtom = tCpuNums[6];
    int cvflagAtom = tCpuNums[7];
    
    JSE_NNAP::flt_t *x = (JSE_NNAP::flt_t *)tDataIn[1];
    int *type = (int *)tDataIn[2];
    int *ilist = (int *)tDataIn[3];
    int *numneigh = (int *)tDataIn[4];
    int *firstneigh = (int *)tDataIn[5];
    JSE_NNAP::flt_t *cutsq = (JSE_NNAP::flt_t *)tDataIn[6];
    int *tLmpType2NNAPType = (int *)tDataIn[7];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[8];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[9];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[10];
    JSE_NNAP::flt_t **tNormParam = (JSE_NNAP::flt_t **)tDataIn[11];
    
    JSE_NNAP::flt_t *f0 = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *f1 = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *eatom0 = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *vatom0 = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *vatom1 = (JSE_NNAP::flt_t *)tDataOut[4];
    JSE_NNAP::flt_t *rBufNlDx = (JSE_NNAP::flt_t *)tDataOut[5];
    JSE_NNAP::flt_t *rBufNlDy = (JSE_NNAP::flt_t *)tDataOut[6];
    JSE_NNAP::flt_t *rBufNlDz = (JSE_NNAP::flt_t *)tDataOut[7];
    int *rBufNlType = (int *)tDataOut[8];
    int *rBufNlIdx = (int *)tDataOut[9];
    int *rBufNeiNum = (int *)tDataOut[10];
    int *rBufCType = (int *)tDataOut[11];
    JSE_NNAP::flt_t *rBufGradNlDx = (JSE_NNAP::flt_t *)tDataOut[12];
    JSE_NNAP::flt_t *rBufGradNlDy = (JSE_NNAP::flt_t *)tDataOut[13];
    JSE_NNAP::flt_t *rBufGradNlDz = (JSE_NNAP::flt_t *)tDataOut[14];
    
    cudaError_t tErr;
    tErr = cudaMemset(f0, 0, inum*3*sizeof(JSE_NNAP::flt_t));
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemset(f1, 0, nlocalghost*3*sizeof(JSE_NNAP::flt_t));
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
    JSE_NNAP::computeLammpsCuda0(inum, nlocalghost, neighnumMax,
        eflag||eflagAtom, vflag, vflagAtom, cvflagAtom,
        ilist, x, type,
        cutsq, numneigh, firstneigh, tLmpType2NNAPType,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx,
        rBufNeiNum, rBufCType,
        f0, f1, eatom0, vatom0, vatom1,
        tFpHyperParam, tFpParam, tNormParam, tNnParam,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    
    return (int)cudaDeviceSynchronize();
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_computeGPUMD(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;

    int *tCpuNums = (int *)tDataIn[0];
    
    int number_of_particles = tCpuNums[0];
    int N1 = tCpuNums[1];
    int N2 = tCpuNums[2];
    int neighnumMax = tCpuNums[3];
    
    const int *g_neighbor_number = (const int *)tDataIn[1];
    const int *g_neighbor_list = (const int *)tDataIn[2];
    const float *nl_dx = (const float *)tDataIn[3];
    const float *nl_dy = (const float *)tDataIn[4];
    const float *nl_dz = (const float *)tDataIn[5];
    const int *g_type = (const int *)tDataIn[6];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[8];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[9];
    JSE_NNAP::flt_t **tNormParam = (JSE_NNAP::flt_t **)tDataIn[10];
    
    double *g_fx = (double *)tDataOut[0];
    double *g_fy = (double *)tDataOut[1];
    double *g_fz = (double *)tDataOut[2];
    double *g_virial = (double *)tDataOut[3];
    double *g_potential = (double *)tDataOut[4];
    JSE_NNAP::flt_t *rBufNlDx = (JSE_NNAP::flt_t *)tDataOut[5];
    JSE_NNAP::flt_t *rBufNlDy = (JSE_NNAP::flt_t *)tDataOut[6];
    JSE_NNAP::flt_t *rBufNlDz = (JSE_NNAP::flt_t *)tDataOut[7];
    int *rBufNlType = (int *)tDataOut[8];
    int *rBufNlIdx = (int *)tDataOut[9];
    JSE_NNAP::flt_t *rBufGradNlDx = (JSE_NNAP::flt_t *)tDataOut[10];
    JSE_NNAP::flt_t *rBufGradNlDy = (JSE_NNAP::flt_t *)tDataOut[11];
    JSE_NNAP::flt_t *rBufGradNlDz = (JSE_NNAP::flt_t *)tDataOut[12];
    
    constexpr int tBlockSize = __NNAPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (N2 - N1 - 1) / tBlockSize + 1; // copy from gpumd
    
    JSE_NNAP::initGpumdNeiKernel<<<tGridSize, tBlockSize>>>(number_of_particles, N1, N2, neighnumMax,
        g_neighbor_number, g_neighbor_list,
        nl_dx, nl_dy, nl_dz, g_type,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx
    );
    JSE_NNAP::computeGpumdKernel<<<tGridSize, tBlockSize>>>(N1, N2, neighnumMax,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType,
        g_potential, g_neighbor_number, g_type,
        tFpHyperParam, tFpParam, tNormParam, tNnParam,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    JSE_NNAP::collectGpumdResultsKernel<<<tGridSize, tBlockSize>>>(number_of_particles, N1, N2, neighnumMax,
        g_fx, g_fy, g_fz, g_virial,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlIdx, g_neighbor_number,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    
    return (int)cudaDeviceSynchronize();
}

}
