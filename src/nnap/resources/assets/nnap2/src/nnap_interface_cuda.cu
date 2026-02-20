#include "nnap_interface_cuda.h"
#include "nnap_main.hpp"

#include <cstdint>

// >>> NNAPGEN REMOVE
#define __NNAPGEN_CUDA_BLOCKSIZE__ 256
// <<< NNAPGEN REMOVE

namespace JSE_NNAP {

#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

static __global__ void computeLammpsCudaKernel(
        int inum, int eflag, int vflag, int vflagAtom, int cvflagAtom,
        int *ilist, flt_t *x, int *type, flt_t *cutsq,
        int *numneigh, int64_t *displsneigh, int *firstneigh,
        flt_t *f0, flt_t *f1, flt_t *eatom, flt_t *vatom0, flt_t *vatom1,
        int *aLmpType2NNAPType, flt_t **aFpHyperParam, flt_t **aFpParam, flt_t **aNormParam, flt_t **aNnParam,
        flt_t *rBufNlDx, flt_t *rBufNlDy, flt_t *rBufNlDz, int *rBufNlType, int *rBufNlIdx,
        flt_t *rBufGradNlDx, flt_t *rBufGradNlDy, flt_t *rBufGradNlDz,
        flt_t *rBufFpOrGradFp, flt_t *rBufFpForwardCache, flt_t *rBufFpBackwardCache, flt_t *rBufNnGradCache, flt_t *rBufNnHiddenCache) {
    const unsigned int ii = (blockIdx.x * blockDim.x + threadIdx.x);
    if (ii >= inum) return;
    
    const int i = ilist[ii];
    const int i3 = i+i+i;
    const int i6 = i3+i3;
    const flt_t xtmp = x[i3+0];
    const flt_t ytmp = x[i3+1];
    const flt_t ztmp = x[i3+2];
    const int typei = type[i];
    const int typeiNNAP = aLmpType2NNAPType[typei];
    const int jnum = numneigh[i];
    const int64_t nlshift = displsneigh[i];
    int *jlist = firstneigh + nlshift;
    
    /// build neighbor list
    flt_t *tNlDx = rBufNlDx + nlshift;
    flt_t *tNlDy = rBufNlDy + nlshift;
    flt_t *tNlDz = rBufNlDz + nlshift;
    int *tNlType = rBufNlType + nlshift;
    int *tNlIdx = rBufNlIdx + nlshift;
    int tNeiNum = 0;
    for (int jj = 0; jj < jnum; ++jj) {
        int j = jlist[jj];
        j &= JSE_LMP_NEIGHMASK;
        const int j3 = j+j+j;
        // Note that dxyz in jse and lammps are defined oppositely
        const flt_t delx = x[j3+0] - xtmp;
        const flt_t dely = x[j3+1] - ytmp;
        const flt_t delz = x[j3+2] - ztmp;
        const flt_t rsq = delx*delx + dely*dely + delz*delz;
        if (rsq < cutsq[typei]) {
            tNlDx[tNeiNum] = delx;
            tNlDy[tNeiNum] = dely;
            tNlDz[tNeiNum] = delz;
            tNlType[tNeiNum] = aLmpType2NNAPType[type[j]];
            tNlIdx[tNeiNum] = j;
            ++tNeiNum;
        }
    }
    
    /// begin nnap here
    flt_t rEng;
    flt_t *rGradNlDx = rBufGradNlDx + nlshift;
    flt_t *rGradNlDy = rBufGradNlDy + nlshift;
    flt_t *rGradNlDz = rBufGradNlDz + nlshift;
    flt_t *rFpOrGradFp = rBufFpOrGradFp + (__NNAPGEN_MAX_FP_SIZE__*i);
    flt_t *rFpForwardCache = rBufFpForwardCache + (__NNAPGEN_MAX_FP_SIZE_CACHEF__*i);
    flt_t *rFpBackwardCache = rBufFpBackwardCache + (__NNAPGEN_MAX_FP_SIZE_CACHEB__*i);
    flt_t *rNnGradCache = rBufNnGradCache + (__NNAPGEN_MAX_NN_SIZE_CACHEG__*i);
    flt_t *rNnHiddenCache = rBufNnHiddenCache + (__NNAPGEN_MAX_NN_SIZE_CACHEH__*i);
    // manual clear required for backward in force
    for (int j = 0; j < tNeiNum; ++j) {
        rGradNlDx[j] = ZERO;
        rGradNlDy[j] = ZERO;
        rGradNlDz[j] = ZERO;
    }
// >>> NNAPGEN SWITCH
    fpForward<__NNAPGENS_typeiNNAP__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rFpOrGradFp,
        aFpHyperParam, aFpParam, rFpForwardCache
    );
    normedNnForward<__NNAPGENS_typeiNNAP__, TRUE>(
        typeiNNAP, rFpOrGradFp, aNormParam[typeiNNAP-1], aNnParam, rNnGradCache, rNnHiddenCache, &rEng
    );
    // manual clear required for backward in force
    fill<__NNAPGENX_FP_SIZE__>(rFpOrGradFp, ZERO);
    normedNnBackward<__NNAPGENS_typeiNNAP__>(
        typeiNNAP, rFpOrGradFp, aNormParam[typeiNNAP-1], aNnParam, rNnGradCache, rNnHiddenCache, ONE
    );
    fpBackward<__NNAPGENS_typeiNNAP__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rFpOrGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        aFpHyperParam, aFpParam, rFpForwardCache, rFpBackwardCache
    );
// <<< NNAPGEN SWITCH (typeiNNAP) [FP NN TYPE]
    
    /// collect results
    if (eflag) {
        eatom[i] += rEng;
    }
    for (int jj = 0; jj < tNeiNum; ++jj) {
        const int j = tNlIdx[jj];
        const int j3 = j+j+j;
        const flt_t fx = rGradNlDx[jj];
        const flt_t fy = rGradNlDy[jj];
        const flt_t fz = rGradNlDz[jj];
        f0[i3+0] -= fx;
        f0[i3+1] -= fy;
        f0[i3+2] -= fz;
        atomicAdd(f1 + (j3+0), fx);
        atomicAdd(f1 + (j3+1), fy);
        atomicAdd(f1 + (j3+2), fz);
        if (vflag) {
            const flt_t dx = tNlDx[jj];
            const flt_t dy = tNlDy[jj];
            const flt_t dz = tNlDz[jj];
            const flt_t vxx = dx*fx;
            const flt_t vyy = dy*fy;
            const flt_t vzz = dz*fz;
            const flt_t vxy = dx*fy;
            const flt_t vxz = dx*fz;
            const flt_t vyz = dy*fz;
            vatom0[i6+0] += vxx;
            vatom0[i6+1] += vyy;
            vatom0[i6+2] += vzz;
            vatom0[i6+3] += vxy;
            vatom0[i6+4] += vxz;
            vatom0[i6+5] += vyz;
            if (cvflagAtom) {
                const int j9 = j3+j3+j3;
                atomicAdd(vatom1 + (j9+0), vxx);
                atomicAdd(vatom1 + (j9+1), vyy);
                atomicAdd(vatom1 + (j9+2), vzz);
                atomicAdd(vatom1 + (j9+3), vxy);
                atomicAdd(vatom1 + (j9+4), vxz);
                atomicAdd(vatom1 + (j9+5), vyz);
                atomicAdd(vatom1 + (j9+6), dy*fx);
                atomicAdd(vatom1 + (j9+7), dz*fx);
                atomicAdd(vatom1 + (j9+8), dz*fy);
            } else if (vflagAtom) {
                const int j6 = j3+j3;
                atomicAdd(vatom1 + (j6+0), vxx);
                atomicAdd(vatom1 + (j6+1), vyy);
                atomicAdd(vatom1 + (j6+2), vzz);
                atomicAdd(vatom1 + (j6+3), vxy);
                atomicAdd(vatom1 + (j6+4), vxz);
                atomicAdd(vatom1 + (j6+5), vyz);
            }
        }
    }
}

}

extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_statDisplsneighLammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    
    int *tNums = (int *)tDataIn[0];
    int *numneigh = (int *)tDataIn[1];
    int64_t *displsneigh = (int64_t *)rDataOut;
    
    int nlocal = tNums[0];
    displsneigh[0] = 0;
    for (int i = 0; i < nlocal; ++i) {
        displsneigh[i+1] = displsneigh[i] + numneigh[i];
    }
    return 0;
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_lammps2cuda(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    
    int inum = tNums[0];
    int nlocal = tNums[1];
    int nghost = tNums[2];
    int ntot = nlocal + nghost;
    
    double **x = (double **)tDataIn[1];
    int *type = (int *)tDataIn[2];
    int *ilist = (int *)tDataIn[3];
    int *numneigh = (int *)tDataIn[4];
    int64_t *displsneigh = (int64_t *)tDataIn[5];
    int **firstneigh = (int **)tDataIn[6];
    
    JSE_NNAP::flt_t *fltBuf = (JSE_NNAP::flt_t *)tDataOut[0];
    int *intBuf = (int *)tDataOut[1];
    JSE_NNAP::flt_t *cudaX = (JSE_NNAP::flt_t *)tDataOut[2];
    int *cudaType = (int *)tDataOut[3];
    int *cudaIlist = (int *)tDataOut[4];
    int *cudaNumneigh = (int *)tDataOut[5];
    int64_t *cudaDisplsneigh = (int64_t *)tDataOut[6];
    int *cudaFirstneigh = (int *)tDataOut[7];
    
    cudaError_t tErr;
    for (int i = 0, ii = 0; i < ntot; ++i) {
        fltBuf[ii] = (JSE_NNAP::flt_t)x[i][0]; ++ii;
        fltBuf[ii] = (JSE_NNAP::flt_t)x[i][1]; ++ii;
        fltBuf[ii] = (JSE_NNAP::flt_t)x[i][2]; ++ii;
    }
    tErr = cudaMemcpy(cudaX, fltBuf, ntot*3*sizeof(JSE_NNAP::flt_t), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemcpy(cudaType, type, ntot*sizeof(int), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemcpy(cudaIlist, ilist, inum*sizeof(int), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemcpy(cudaNumneigh, numneigh, nlocal*sizeof(int), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemcpy(cudaDisplsneigh, displsneigh, (nlocal+1)*sizeof(int64_t), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    for (int i = 0, ii = 0; i < nlocal; ++i) {
        int jnum = numneigh[i];
        int *jlist = firstneigh[i];
        for (int j = 0; j < jnum; ++j, ++ii) {
            intBuf[ii] = jlist[j];
        }
    }
    tErr = cudaMemcpy(cudaFirstneigh, intBuf, displsneigh[nlocal]*sizeof(int), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    return (int)cudaSuccess;
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_cuda2lammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    
    int nlocal = tNums[0];
    int nghost = tNums[1];
    int eflag = tNums[2];
    int vflag = tNums[3];
    int eflagAtom = tNums[4];
    int vflagAtom = tNums[5];
    int cvflagAtom = tNums[6];
    int ntot = nlocal + nghost;
    
    double **f = (double **)tDataOut[0];
    double *engVdwl = (double *)tDataOut[1];
    double *eatom = (double *)tDataOut[2];
    double *virial = (double *)tDataOut[3];
    double **vatom = (double **)tDataOut[4];
    double **cvatom = (double **)tDataOut[5];
    
    JSE_NNAP::flt_t *fltBuf = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *cudaF0 = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *cudaF1 = (JSE_NNAP::flt_t *)tDataIn[3];
    JSE_NNAP::flt_t *cudaEatom = (JSE_NNAP::flt_t *)tDataIn[4];
    JSE_NNAP::flt_t *cudaVatom0 = (JSE_NNAP::flt_t *)tDataIn[5];
    JSE_NNAP::flt_t *cudaVatom1 = (JSE_NNAP::flt_t *)tDataIn[6];
    
    cudaError_t tErr;
    tErr = cudaMemcpy(fltBuf, cudaF0, nlocal*3*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
    if (tErr!=cudaSuccess) return (int)tErr;
    for (int i = 0, ii = 0; i < nlocal; ++i) {
        f[i][0] += (double)fltBuf[ii]; ++ii;
        f[i][1] += (double)fltBuf[ii]; ++ii;
        f[i][2] += (double)fltBuf[ii]; ++ii;
    }
    tErr = cudaMemcpy(fltBuf, cudaF1, ntot*3*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
    if (tErr!=cudaSuccess) return (int)tErr;
    for (int i = 0, ii = 0; i < ntot; ++i) {
        f[i][0] += (double)fltBuf[ii]; ++ii;
        f[i][1] += (double)fltBuf[ii]; ++ii;
        f[i][2] += (double)fltBuf[ii]; ++ii;
    }
    
    if (eflag) {
        tErr = cudaMemcpy(fltBuf, cudaEatom, nlocal*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        for (int i = 0; i < nlocal; ++i) {
            const double tEng = (double)fltBuf[i];
            *engVdwl += tEng;
            if (eflagAtom) eatom[i] += tEng;
        }
    }
    if (vflag) {
        tErr = cudaMemcpy(fltBuf, cudaVatom0, nlocal*6*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        for (int i = 0, ii = 0; i < nlocal; ++i) {
            virial[0] += (double)fltBuf[ii]; ++ii;
            virial[1] += (double)fltBuf[ii]; ++ii;
            virial[2] += (double)fltBuf[ii]; ++ii;
            virial[3] += (double)fltBuf[ii]; ++ii;
            virial[4] += (double)fltBuf[ii]; ++ii;
            virial[5] += (double)fltBuf[ii]; ++ii;
        }
    }
    if (cvflagAtom) {
        tErr = cudaMemcpy(fltBuf, cudaVatom1, ntot*9*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        for (int i = 0, ii = 0; i < ntot; ++i) {
            cvatom[i][0] += (double)fltBuf[ii]; ++ii;
            cvatom[i][1] += (double)fltBuf[ii]; ++ii;
            cvatom[i][2] += (double)fltBuf[ii]; ++ii;
            cvatom[i][3] += (double)fltBuf[ii]; ++ii;
            cvatom[i][4] += (double)fltBuf[ii]; ++ii;
            cvatom[i][5] += (double)fltBuf[ii]; ++ii;
            cvatom[i][6] += (double)fltBuf[ii]; ++ii;
            cvatom[i][7] += (double)fltBuf[ii]; ++ii;
            cvatom[i][8] += (double)fltBuf[ii]; ++ii;
        }
        if (vflagAtom) {
            for (int i = 0, ii = 0; i < ntot; ++i) {
                vatom[i][0] += (double)fltBuf[ii]; ++ii;
                vatom[i][1] += (double)fltBuf[ii]; ++ii;
                vatom[i][2] += (double)fltBuf[ii]; ++ii;
                vatom[i][3] += (double)fltBuf[ii]; ++ii;
                vatom[i][4] += (double)fltBuf[ii]; ++ii;
                vatom[i][5] += (double)fltBuf[ii]; ++ii;
                ii += 3;
            }
        }
    } else if (vflagAtom) {
        tErr = cudaMemcpy(fltBuf, cudaVatom1, ntot*6*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        for (int i = 0, ii = 0; i < ntot; ++i) {
            vatom[i][0] += (double)fltBuf[ii]; ++ii;
            vatom[i][1] += (double)fltBuf[ii]; ++ii;
            vatom[i][2] += (double)fltBuf[ii]; ++ii;
            vatom[i][3] += (double)fltBuf[ii]; ++ii;
            vatom[i][4] += (double)fltBuf[ii]; ++ii;
            vatom[i][5] += (double)fltBuf[ii]; ++ii;
        }
    }
    return (int)cudaSuccess;
}


JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_computeLammpsCuda(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;

    int *tCpuNums = (int *)tDataIn[0];
    
    int inum = tCpuNums[0];
    int nlocal = tCpuNums[1];
    int nghost = tCpuNums[2];
    int eflag = tCpuNums[3];
    int vflag = tCpuNums[4];
    int eflagAtom = tCpuNums[5];
    int vflagAtom = tCpuNums[6];
    int cvflagAtom = tCpuNums[7];
    int ntot = nlocal + nghost;
    
    JSE_NNAP::flt_t *x = (JSE_NNAP::flt_t *)tDataIn[1];
    int *type = (int *)tDataIn[2];
    int *ilist = (int *)tDataIn[3];
    int *numneigh = (int *)tDataIn[4];
    int64_t *displsneigh = (int64_t *)tDataIn[5];
    int *firstneigh = (int *)tDataIn[6];
    JSE_NNAP::flt_t *cutsq = (JSE_NNAP::flt_t *)tDataIn[7];
    int *tLmpType2NNAPType = (int *)tDataIn[8];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[9];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[10];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[11];
    JSE_NNAP::flt_t **tNormParam = (JSE_NNAP::flt_t **)tDataIn[12];
    
    JSE_NNAP::flt_t *f0 = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *f1 = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *eatom = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *vatom0 = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *vatom1 = (JSE_NNAP::flt_t *)tDataOut[4];
    JSE_NNAP::flt_t *rBufNlDx = (JSE_NNAP::flt_t *)tDataOut[5];
    JSE_NNAP::flt_t *rBufNlDy = (JSE_NNAP::flt_t *)tDataOut[6];
    JSE_NNAP::flt_t *rBufNlDz = (JSE_NNAP::flt_t *)tDataOut[7];
    int *rBufNlType = (int *)tDataOut[8];
    int *rBufNlIdx = (int *)tDataOut[9];
    JSE_NNAP::flt_t *rBufGradNlDx = (JSE_NNAP::flt_t *)tDataOut[10];
    JSE_NNAP::flt_t *rBufGradNlDy = (JSE_NNAP::flt_t *)tDataOut[11];
    JSE_NNAP::flt_t *rBufGradNlDz = (JSE_NNAP::flt_t *)tDataOut[12];
    JSE_NNAP::flt_t *rBufFpOrGradFp = (JSE_NNAP::flt_t *)tDataOut[13];
    JSE_NNAP::flt_t *rBufFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[14];
    JSE_NNAP::flt_t *rBufFpBackwardCache = (JSE_NNAP::flt_t *)tDataOut[15];
    JSE_NNAP::flt_t *rBufNnGradCache = (JSE_NNAP::flt_t *)tDataOut[16];
    JSE_NNAP::flt_t *rBufNnHiddenCache = (JSE_NNAP::flt_t *)tDataOut[17];
    
    cudaError_t tErr;
    tErr = cudaMemset(f0, 0, nlocal*3*sizeof(JSE_NNAP::flt_t));
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemset(f1, 0, ntot*3*sizeof(JSE_NNAP::flt_t));
    if (tErr!=cudaSuccess) return (int)tErr;
    if (eflag||eflagAtom) {
        tErr = cudaMemset(eatom, 0, nlocal*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    if (vflag) {
        tErr = cudaMemset(vatom0, 0, nlocal*6*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    if (cvflagAtom) {
        tErr = cudaMemset(vatom1, 0, ntot*9*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    } else if (vflagAtom) {
        tErr = cudaMemset(vatom1, 0, ntot*6*sizeof(JSE_NNAP::flt_t));
        if (tErr!=cudaSuccess) return (int)tErr;
    }
    
    /// begin compute here
    constexpr int tBlockSize = __NNAPGEN_CUDA_BLOCKSIZE__;
    const int tGridSize = (inum + tBlockSize-1) / tBlockSize;
    JSE_NNAP::computeLammpsCudaKernel<<<tGridSize, tBlockSize>>>(
        inum, eflag||eflagAtom, vflag, vflagAtom, cvflagAtom,
        ilist, x, type, cutsq,
        numneigh, displsneigh, firstneigh,
        f0, f1, eatom, vatom0, vatom1,
        tLmpType2NNAPType, tFpHyperParam, tFpParam, tNormParam, tNnParam,
        rBufNlDx, rBufNlDy, rBufNlDz, rBufNlType, rBufNlIdx,
        rBufGradNlDx, rBufGradNlDy, rBufGradNlDz,
        rBufFpOrGradFp, rBufFpForwardCache, rBufFpBackwardCache, rBufNnGradCache, rBufNnHiddenCache
    );
    
    return (int)cudaDeviceSynchronize();
}

}
