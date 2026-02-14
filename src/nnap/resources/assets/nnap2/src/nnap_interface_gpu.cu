#include "nnap_interface_gpu.h"
#include "nnap_main.hpp"

#include <cstdint>


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
    JSE_NNAP::flt_t *cudaF = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *cudaEV = (JSE_NNAP::flt_t *)tDataIn[3];
    JSE_NNAP::flt_t *cudaEatom = (JSE_NNAP::flt_t *)tDataIn[4];
    JSE_NNAP::flt_t *cudaVatom = (JSE_NNAP::flt_t *)tDataIn[5];
    
    cudaError_t tErr;
    tErr = cudaMemcpy(fltBuf, cudaF, ntot*3*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
    if (tErr!=cudaSuccess) return (int)tErr;
    for (int i = 0, ii = 0; i < ntot; ++i) {
        f[i][0] += (double)fltBuf[ii]; ++ii;
        f[i][1] += (double)fltBuf[ii]; ++ii;
        f[i][2] += (double)fltBuf[ii]; ++ii;
    }
    
    if (eflag || vflag) {
        tErr = cudaMemcpy(fltBuf, cudaEV, 7*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        if (eflag) {
            *engVdwl += (double)fltBuf[0];
        }
        if (vflag) {
            virial[0] += (double)fltBuf[1];
            virial[1] += (double)fltBuf[2];
            virial[2] += (double)fltBuf[3];
            virial[3] += (double)fltBuf[4];
            virial[4] += (double)fltBuf[5];
            virial[5] += (double)fltBuf[6];
        }
    }
    if (eflagAtom) {
        tErr = cudaMemcpy(fltBuf, cudaEatom, nlocal*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
        if (tErr!=cudaSuccess) return (int)tErr;
        for (int i = 0; i < nlocal; ++i) {
            eatom[i] += (double)fltBuf[i];
        }
    }
    if (cvflagAtom) {
        tErr = cudaMemcpy(fltBuf, cudaVatom, ntot*9*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
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
        tErr = cudaMemcpy(fltBuf, cudaVatom, ntot*6*sizeof(JSE_NNAP::flt_t), cudaMemcpyDeviceToHost);
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



#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

// JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_computeLammpsGPU(void *aDataIn, void *rDataOut) {
//     void **tDataIn = (void **)aDataIn;
//     void **tDataOut = (void **)rDataOut;
//
//     int *tNums = (int *)tDataIn[0];
//     JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
//     JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
//     JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
//     int *tNlType = (int *)tDataIn[4];
//     int *tNlIdx = (int *)tDataIn[5];
//     JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[6];
//     JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[7];
//     JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[8];
//     JSE_NNAP::flt_t **tNormParam = (JSE_NNAP::flt_t **)tDataIn[9];
//
//     JSE_NNAP::flt_t *tGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
//     JSE_NNAP::flt_t *tGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
//     JSE_NNAP::flt_t *tGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
//
//     int inum = tNums[0];
//     int ntypes = tNums[1];
//     int eflag = tNums[2];
//     int vflag = tNums[3];
//     int eflagAtom = tNums[4];
//     int vflagAtom = tNums[5];
//     int cvflagAtom = tNums[6];
//
//     double **x = (double **)tDataIn[10];
//     double **f = (double **)tDataOut[0];
//     int *type = (int *)tDataIn[11];
//
//     int *ilist = (int *)tDataIn[12];
//     int *numneigh = (int *)tDataIn[13];
//     int **firstneigh = (int **)tDataIn[14];
//     double *cutsq = (double *)tDataIn[15];
//     int *tLmpType2NNAPType = (int *)tDataIn[16];
//
//     double *engVdwl = (double *)tDataOut[4];
//     double *eatom = (double *)tDataOut[5];
//     double *virial = (double *)tDataOut[6];
//     double **vatom = (double **)tDataOut[7];
//     double **cvatom = (double **)tDataOut[8];
//
//
//
//     /// begin compute here
//     for (int ii = 0; ii < inum; ++ii) {
//         int i = ilist[ii];
//         double xtmp = x[i][0];
//         double ytmp = x[i][1];
//         double ztmp = x[i][2];
//         int typei = type[i];
//         int typeiNNAP = tLmpType2NNAPType[typei];
//         int *jlist = firstneigh[i];
//         int jnum = numneigh[i];
//
//         /// build neighbor list
//         int tNeiNum = 0;
//         for (int jj = 0; jj < jnum; ++jj) {
//             int j = jlist[jj];
//             j &= JSE_LMP_NEIGHMASK;
//             // Note that dxyz in jse and lammps are defined oppositely
//             double delx = x[j][0] - xtmp;
//             double dely = x[j][1] - ytmp;
//             double delz = x[j][2] - ztmp;
//             double rsq = delx*delx + dely*dely + delz*delz;
//             if (rsq < cutsq[typei]) {
//                 tNlDx[tNeiNum] = (JSE_NNAP::flt_t)delx;
//                 tNlDy[tNeiNum] = (JSE_NNAP::flt_t)dely;
//                 tNlDz[tNeiNum] = (JSE_NNAP::flt_t)delz;
//                 tNlType[tNeiNum] = tLmpType2NNAPType[type[j]];
//                 tNlIdx[tNeiNum] = j;
//                 ++tNeiNum;
//             }
//         }
//
//         /// begin nnap here
//         JSE_NNAP::flt_t rEng;
//         // manual clear required for backward in force
//         for (int j = 0; j < tNeiNum; ++j) {
//             tGradNlDx[j] = JSE_NNAP::ZERO;
//             tGradNlDy[j] = JSE_NNAP::ZERO;
//             tGradNlDz[j] = JSE_NNAP::ZERO;
//         }
//         int code;
// // >>> NNAPGEN SWITCH
//         JSE_NNAP::flt_t rFp[__NNAPGENX_FP_SIZE__];
//         JSE_NNAP::flt_t rGradFp[__NNAPGENX_FP_SIZE__] = {};
//         JSE_NNAP::flt_t rFpForwardCache[__NNAPGENX_FP_SIZE_CACHEF__];
//         code = JSE_NNAP::fpForward<__NNAPGENS_typeiNNAP__>(
//             tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rFp,
//             tFpHyperParam, tFpParam, rFpForwardCache
//         );
//         if (code!=0) return code;
//         JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_CACHE__];
//         code = JSE_NNAP::normedNnForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE>(
//             typeiNNAP, rFp, subNormParam, tNnParam, rNnGradCache, &rEng
//         );
//         if (code!=0) return code;
//         code = JSE_NNAP::normedNnBackward<__NNAPGENS_typeiNNAP__>(
//             typeiNNAP, rGradFp, subNormParam, tNnParam, rNnGradCache, JSE_NNAP::ONE
//         );
//         if (code!=0) return code;
//         JSE_NNAP::flt_t rFpBackwardCache[__NNAPGENX_FP_SIZE_CACHEB__] = {};
//         code = JSE_NNAP::fpBackward<__NNAPGENS_typeiNNAP__>(
//             tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rGradFp,
//             tGradNlDx, tGradNlDy, tGradNlDz,
//             tFpHyperParam, tFpParam, rFpForwardCache, rFpBackwardCache
//         );
//         if (code!=0) return code;
// // <<< NNAPGEN SWITCH (typeiNNAP) [FP NN TYPE]
//
//         /// collect results
//         if (eflag) {
//             *engVdwl += rEng;
//             if (eflagAtom) eatom[i] += rEng;
//         }
//         for (int jj = 0; jj < tNeiNum; ++jj) {
//             int j = tNlIdx[jj];
//             const JSE_NNAP::flt_t fx = tGradNlDx[jj];
//             const JSE_NNAP::flt_t fy = tGradNlDy[jj];
//             const JSE_NNAP::flt_t fz = tGradNlDz[jj];
//             f[i][0] -= fx;
//             f[i][1] -= fy;
//             f[i][2] -= fz;
//             f[j][0] += fx;
//             f[j][1] += fy;
//             f[j][2] += fz;
//             if (vflag) {
//                 const JSE_NNAP::flt_t dx = tNlDx[jj];
//                 const JSE_NNAP::flt_t dy = tNlDy[jj];
//                 const JSE_NNAP::flt_t dz = tNlDz[jj];
//                 virial[0] += dx*fx;
//                 virial[1] += dy*fy;
//                 virial[2] += dz*fz;
//                 virial[3] += dx*fy;
//                 virial[4] += dx*fz;
//                 virial[5] += dy*fz;
//                 if (vflagAtom) {
//                     vatom[j][0] += dx*fx;
//                     vatom[j][1] += dy*fy;
//                     vatom[j][2] += dz*fz;
//                     vatom[j][3] += dx*fy;
//                     vatom[j][4] += dx*fz;
//                     vatom[j][5] += dy*fz;
//                 }
//                 if (cvflagAtom) {
//                     cvatom[j][0] += dx*fx;
//                     cvatom[j][1] += dy*fy;
//                     cvatom[j][2] += dz*fz;
//                     cvatom[j][3] += dx*fy;
//                     cvatom[j][4] += dx*fz;
//                     cvatom[j][5] += dy*fz;
//                     cvatom[j][6] += dy*fx;
//                     cvatom[j][7] += dz*fx;
//                     cvatom[j][8] += dz*fy;
//                 }
//             }
//         }
//     }
//     return cudaSuccess;
// }

}

