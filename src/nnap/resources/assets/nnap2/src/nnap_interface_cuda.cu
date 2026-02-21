#include "nnap_interface_cuda.h"
#include "nnap_main.hpp"

#include <cstdint>

// >>> NNAPGEN REMOVE
#define __NNAPGEN_CUDA_BLOCKSIZE__ 256
#define __NNAPGENS_ctype__ 1
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

static __global__ void initLammpsNeiKernel(int inum, int nlocalghost,
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
            rBufNlDx[tNeiNum*inum + ii] = delx;
            rBufNlDy[tNeiNum*inum + ii] = dely;
            rBufNlDz[tNeiNum*inum + ii] = delz;
            rBufNlType[tNeiNum*inum + ii] = aLmpType2NNAPType[type[j]];
            rBufNlIdx[tNeiNum*inum + ii] = j;
            ++tNeiNum;
        }
    }
    rBufNeiNum[ii] = tNeiNum;
    rBufCType[ii] = aLmpType2NNAPType[typei];
}

static __global__ void collectLammpsResultsKernel(int inum, int nlocalghost,
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
        const int j = aBufNlIdx[jj*inum + ii];
        const flt_t fx = rBufGradNlDx[jj*inum + ii];
        const flt_t fy = rBufGradNlDy[jj*inum + ii];
        const flt_t fz = rBufGradNlDz[jj*inum + ii];
        f0x -= fx;
        f0y -= fy;
        f0z -= fz;
        atomicAdd(f1 + (0*nlocalghost + j), fx);
        atomicAdd(f1 + (1*nlocalghost + j), fy);
        atomicAdd(f1 + (2*nlocalghost + j), fz);
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

static __global__ void fpForwardKernel(int inum,
        flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlType,
        int *aBufNeiNum, int *aBufCType, flt_t *rBufFp,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t *rBufFpForwardCache) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int ctype = aBufCType[ii];
    const int tNeiNum = aBufNeiNum[ii];
// >>> NNAPGEN SWITCH
    fpForwardBatch<__NNAPGENS_ctype__>(ii, inum,
        aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, tNeiNum, ctype, rBufFp,
        aFpHyperParam, aFpParam, rBufFpForwardCache
    );
// <<< NNAPGEN SWITCH (ctype) [FP TYPE]
}
static __global__ void normedNnForwardKernel(int inum,
        int eflag, flt_t *eatom0, int *aBufCType, flt_t *rBufFp,
        flt_t **aNormParam, flt_t **aNnParam, flt_t *rBufNnGradCache, flt_t *rBufNnHiddenCache) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int ctype = aBufCType[ii];
    flt_t rEng;
// >>> NNAPGEN SWITCH
    normedNnForwardBatch<__NNAPGENS_ctype__, TRUE>(ii, inum,
        ctype, rBufFp, aNormParam[ctype-1], aNnParam,
        rBufNnGradCache, rBufNnHiddenCache, &rEng
    );
// <<< NNAPGEN SWITCH (ctype) [NN TYPE]
    if (eflag) {
        eatom0[ii] += rEng;
    }
}
static __global__ void normedNnBackwardKernel(int inum,
        int *aBufCType, flt_t *rBufGradFp,
        flt_t **aNormParam, flt_t **aNnParam, flt_t *aBufNnGradCache, flt_t *rBufNnHiddenCache) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int ctype = aBufCType[ii];
// >>> NNAPGEN SWITCH
    // manual clear required for backward in force
    fillBatch<__NNAPGENX_FP_SIZE__>(ii, inum, rBufGradFp, ZERO);
    normedNnBackwardBatch<__NNAPGENS_ctype__>(ii, inum,
        ctype, rBufGradFp, aNormParam[ctype-1], aNnParam,
        aBufNnGradCache, rBufNnHiddenCache, ONE
    );
// <<< NNAPGEN SWITCH (ctype) [NN TYPE]
}
static __global__ void fpBackwardKernel(int inum,
        flt_t *aBufNlDx, flt_t *aBufNlDy, flt_t *aBufNlDz, int *aBufNlType,
        int *aBufNeiNum, int *aBufCType, flt_t *aBufGradFp,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t *rBufFpForwardCache, flt_t *rBufFpBackwardCache) {
    const int ii = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int ctype = aBufCType[ii];
    const int tNeiNum = aBufNeiNum[ii];
    // manual clear required for backward in force
    for (int jj = 0; jj < tNeiNum; ++jj) {
        rBufGradNlDx[jj*inum + ii] = ZERO;
        rBufGradNlDy[jj*inum + ii] = ZERO;
        rBufGradNlDz[jj*inum + ii] = ZERO;
    }
// >>> NNAPGEN SWITCH
    fpBackwardBatch<__NNAPGENS_ctype__>(ii, inum,
        aBufNlDx, aBufNlDy, aBufNlDz, aBufNlType, tNeiNum, ctype, aBufGradFp,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz,
        aFpHyperParam, aFpParam, rBufFpForwardCache, rBufFpBackwardCache
    );
// <<< NNAPGEN SWITCH (ctype) [FP TYPE]
}

static void computeLammpsCuda0(int inum, int nlocalghost,
        int eflag, int vflag, int vflagAtom, int cvflagAtom,
        int *ilist, flt_t *x, int *type,
        flt_t *cutsq, int *numneigh, int *firstneigh, int *aLmpType2NNAPType,
        flt_t *rBufNlDx, flt_t *rBufNlDy, flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx,
        int *rBufNeiNum, int *rBufCType,
        flt_t *f0, flt_t *f1, flt_t *eatom0, flt_t *vatom0, flt_t *vatom1,
        flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz,
        flt_t *rBufFpOrGradFp, flt_t *rBufFpForwardCache, flt_t *rBufFpBackwardCache,
        flt_t *rBufNnGradCache, flt_t *rBufNnHiddenCache) {
    constexpr int tBlockSize = __NNAPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (inum + tBlockSize-1) / tBlockSize;
    
    initLammpsNeiKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        ilist, x, type,
        cutsq, numneigh, firstneigh, aLmpType2NNAPType,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx,
        rBufNeiNum, rBufCType
    );
    
    fpForwardKernel<<<tGridSize, tBlockSize>>>(inum,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType,
        rBufNeiNum, rBufCType, rBufFpOrGradFp,
        aFpHyperParam, aFpParam, rBufFpForwardCache
    );
    normedNnForwardKernel<<<tGridSize, tBlockSize>>>(inum,
        eflag, eatom0, rBufCType, rBufFpOrGradFp,
        aNormParam, aNnParam, rBufNnGradCache, rBufNnHiddenCache
    );
    normedNnBackwardKernel<<<tGridSize, tBlockSize>>>(inum,
        rBufCType, rBufFpOrGradFp,
        aNormParam, aNnParam, rBufNnGradCache, rBufNnHiddenCache
    );
    fpBackwardKernel<<<tGridSize, tBlockSize>>>(inum,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType,
        rBufNeiNum, rBufCType, rBufFpOrGradFp,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz,
        aFpHyperParam, aFpParam, rBufFpForwardCache, rBufFpBackwardCache
    );
    
    collectLammpsResultsKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        vflag, vflagAtom, cvflagAtom,
        f0, f1, vatom0, vatom1,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlIdx, rBufNeiNum,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz
    );
    postLammpsResultsKernel<<<tGridSize, tBlockSize>>>(inum, nlocalghost,
        ilist, f0, f1
    );
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
    int eflag = tCpuNums[2];
    int vflag = tCpuNums[3];
    int eflagAtom = tCpuNums[4];
    int vflagAtom = tCpuNums[5];
    int cvflagAtom = tCpuNums[6];
    
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
    JSE_NNAP::flt_t *rBufFpOrGradFp = (JSE_NNAP::flt_t *)tDataOut[15];
    JSE_NNAP::flt_t *rBufFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[16];
    JSE_NNAP::flt_t *rBufFpBackwardCache = (JSE_NNAP::flt_t *)tDataOut[17];
    JSE_NNAP::flt_t *rBufNnGradCache = (JSE_NNAP::flt_t *)tDataOut[18];
    JSE_NNAP::flt_t *rBufNnHiddenCache = (JSE_NNAP::flt_t *)tDataOut[19];
    
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
    JSE_NNAP::computeLammpsCuda0(inum, nlocalghost,
        eflag||eflagAtom, vflag, vflagAtom, cvflagAtom,
        ilist, x, type,
        cutsq, numneigh, firstneigh, tLmpType2NNAPType,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx,
        rBufNeiNum, rBufCType,
        f0, f1, eatom0, vatom0, vatom1,
        tFpHyperParam, tFpParam, tNormParam, tNnParam,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz,
        rBufFpOrGradFp, rBufFpForwardCache, rBufFpBackwardCache,
        rBufNnGradCache, rBufNnHiddenCache
    );
    
    return (int)cudaDeviceSynchronize();
}

}
