#include "jse_gpu_CudaNeighborListGetter.h"

#include <cstdint>

namespace JSE_CUDANL {

static constexpr float JSE_FLT_EPSILON = 1.0e-5f;

static __device__ inline int floor2int(const float val) {
    return __float2int_rd(val);
}

static __device__ inline float mixed(
    const float ax, const float ay, const float az,
    const float bx, const float by, const float bz,
    const float cx, const float cy, const float cz) {
    
    return (ay*bz - by*az)*cx + (az*bx - bz*ax)*cy + (ax*by - bx*ay)*cz;
}

static __device__ inline void toDirect(
    float &x, float &y, float &z,
    const float ax, const float ay, const float az,
    const float bx, const float by, const float bz,
    const float cx, const float cy, const float cz) {
    
    const float vol = mixed(ax, ay, az, bx, by, bz, cx, cy, cz);
    float x0 = x, y0 = y, z0 = z;
    float x1 = mixed(bx, by, bz, cx, cy, cz, x0, y0, z0) / vol;
    float y1 = mixed(cx, cy, cz, ax, ay, az, x0, y0, z0) / vol;
    float z1 = mixed(ax, ay, az, bx, by, bz, x0, y0, z0) / vol;
    
    x0 = rint(x1); y0 = rint(y1); z0 = rint(z1);
    x = (abs(x1-x0) < JSE_FLT_EPSILON) ? x0 : x1;
    y = (abs(y1-y0) < JSE_FLT_EPSILON) ? y0 : y1;
    z = (abs(z1-z0) < JSE_FLT_EPSILON) ? z0 : z1;
}

static __device__ __host__ inline int cellIndex(
    const int sliceX, const int sliceY, const int sliceZ,
    const int ci, const int cj, const int ck) {
    return (ci+1) + (sliceX+2)*(cj+1) + (sliceX+2)*(sliceY+2)*(ck+1);
}
static __device__ __host__ inline int cellGhost(
    const int sliceX, const int sliceY, const int sliceZ,
    const int ci, const int cj, const int ck) {
    return (ci<0 || ci>=sliceX || cj<0 || cj>=sliceY || ck<0 || ck>=sliceZ);
}

template <int PRISM>
static __global__ void buildCellsKernel(const int nlocalghost,
    const float ax, const float ay, const float az,
    const float bx, const float by, const float bz,
    const float cx, const float cy, const float cz,
    const float *posx, const float *posy, const float *posz,
    const int sliceX, const int sliceY, const int sliceZ,
    int **cells, int *cellSize, int localCellCapacity, int ghostCellCapacity) {
    
    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocalghost) return;
    
    float x = posx[i], y = posy[i], z = posz[i];
    if (PRISM) {
        toDirect(x, y, z, ax, ay, az, bx, by, bz, cx, cy, cz);
    } else {
        x /= ax; y /= by; z /= cz;
    }
    int ci = floor2int(x * (float)sliceX); ci = ci<-1 ? (-1) : (ci>sliceX ? sliceX : ci);
    int cj = floor2int(y * (float)sliceY); cj = cj<-1 ? (-1) : (cj>sliceY ? sliceY : cj);
    int ck = floor2int(z * (float)sliceZ); ck = ck<-1 ? (-1) : (ck>sliceZ ? sliceZ : ck);
    
    const int idx = cellIndex(sliceX, sliceY, sliceZ, ci, cj, ck);
    const int ji = atomicAdd(cellSize+idx, 1);
    const int cghost = cellGhost(sliceX, sliceY, sliceZ, ci, cj, ck);
    const int cellCap = cghost ? ghostCellCapacity : localCellCapacity;
    if (ji < cellCap) {
        cells[idx][ji] = i;
    }
}

template <int PRISM>
static __global__ void buildNlKernel(const int nlocal,
    const float ax, const float ay, const float az,
    const float bx, const float by, const float bz,
    const float cx, const float cy, const float cz,
    const float *posx, const float *posy, const float *posz,
    const int sliceX, const int sliceY, const int sliceZ,
    const int **cells, const int *cellSize, const float rcutsq,
    int *nl, int *nlSize, const int nlCapacity) {

    const int i = (int)(blockIdx.x * blockDim.x + threadIdx.x);
    if (i >= nlocal) return;
    
    const float x0 = posx[i], y0 = posy[i], z0 = posz[i];
    float x = x0, y = y0, z = z0;
    if (PRISM) {
        toDirect(x, y, z, ax, ay, az, bx, by, bz, cx, cy, cz);
    } else {
        x /= ax; y /= by; z /= cz;
    }
    int ci0 = floor2int(x * (float)sliceX); ci0 = ci0<0 ? 0 : (ci0>=sliceX ? (sliceX-1) : ci0);
    int cj0 = floor2int(y * (float)sliceY); cj0 = cj0<0 ? 0 : (cj0>=sliceY ? (sliceY-1) : cj0);
    int ck0 = floor2int(z * (float)sliceZ); ck0 = ck0<0 ? 0 : (ck0>=sliceZ ? (sliceZ-1) : ck0);
    
    int nlsizei = 0;
    for (int ck = ck0-1; ck <= ck0+1; ++ck) for (int cj = cj0-1; cj <= cj0+1; ++cj) for (int ci = ci0-1; ci <= ci0+1; ++ci) {
        const int idx = cellIndex(sliceX, sliceY, sliceZ, ci, cj, ck);
        const int *cell = cells[idx];
        const int csize = cellSize[idx];
        for (int ji = 0; ji < csize; ++ji) {
            const int j = cell[ji];
            if (j == i) continue; // ghost will have new index here
            const float dx = posx[j] - x0;
            const float dy = posy[j] - y0;
            const float dz = posz[j] - z0;
            const float rsq = dx*dx + dy*dy + dz*dz;
            if (rsq >= rcutsq) continue;
            if (nlsizei < nlCapacity) {
                nl[(size_t)nlsizei*nlocal + i] = j;
            }
            ++nlsizei;
        }
    }
    nlSize[i] = nlsizei;
}

}


extern "C" {

JNIEXPORT jint JNICALL Java_jse_gpu_CudaNeighborListGetter_initPosTypeLmp0(
    JNIEnv *aEnv, jclass aClazz, jint nlocal, jint nghost,
    jfloat xlo, jfloat ylo, jfloat zlo, jlong posLmp, jlong pos, jlong posCpu,
    jlong typeLmp, jlong type, jlong typeCpu,
    jboolean sortByType, jint ntypes, jlong ilistCpu) {
    
    double **tPosLmp = (double **)(intptr_t)posLmp;
    float *rPos = (float *)(intptr_t)pos;
    float *rPosCpu = (float *)(intptr_t)posCpu;
    int *tTypeLmp = (int *)(intptr_t)typeLmp;
    int *rType = (int *)(intptr_t)type;
    int *rTypeCpu = (int *)(intptr_t)typeCpu;
    int *rIListCpu = (int *)(intptr_t)ilistCpu;
    
    const int nlocalghost = nlocal + nghost;
    if (!sortByType) {
        for (int i = 0; i < nlocalghost; ++i) {
            rIListCpu[i] = i;
            rTypeCpu[i] = tTypeLmp[i];
            rPosCpu[0L*nlocalghost + i] = (float)tPosLmp[i][0] - xlo;
            rPosCpu[1L*nlocalghost + i] = (float)tPosLmp[i][1] - ylo;
            rPosCpu[2L*nlocalghost + i] = (float)tPosLmp[i][2] - zlo;
        }
        cudaError_t tErr;
        tErr = cudaMemcpy(rPos, rPosCpu, 3L*nlocalghost*sizeof(float), cudaMemcpyHostToDevice);
        if (tErr!=cudaSuccess) return (int)tErr;
        tErr = cudaMemcpy(rType, rTypeCpu, nlocalghost*sizeof(int), cudaMemcpyHostToDevice);
        return tErr;
    }
    int ii = 0;
    for (int t = 1; t <= ntypes; ++t) {
        for (int i = 0; i < nlocal; ++i) {
            const int ti = tTypeLmp[i];
            if (ti == t) {
                rIListCpu[ii] = i;
                rTypeCpu[ii] = ti;
                rPosCpu[0L*nlocalghost + ii] = (float)tPosLmp[i][0] - xlo;
                rPosCpu[1L*nlocalghost + ii] = (float)tPosLmp[i][1] - ylo;
                rPosCpu[2L*nlocalghost + ii] = (float)tPosLmp[i][2] - zlo;
                ++ii;
            }
        }
    }
    if (ii != nlocal) return cudaErrorInvalidValue;
    for (int i = nlocal; i < nlocalghost; ++i) {
            rIListCpu[i] = i;
            rTypeCpu[i] = tTypeLmp[i];
            rPosCpu[0L*nlocalghost + i] = (float)tPosLmp[i][0] - xlo;
            rPosCpu[1L*nlocalghost + i] = (float)tPosLmp[i][1] - ylo;
            rPosCpu[2L*nlocalghost + i] = (float)tPosLmp[i][2] - zlo;
    }
    cudaError_t tErr;
    tErr = cudaMemcpy(rPos, rPosCpu, 3L*nlocalghost*sizeof(float), cudaMemcpyHostToDevice);
    if (tErr!=cudaSuccess) return (int)tErr;
    tErr = cudaMemcpy(rType, rTypeCpu, nlocalghost*sizeof(int), cudaMemcpyHostToDevice);
    return tErr;
}

JNIEXPORT jint JNICALL Java_jse_gpu_CudaNeighborListGetter_initCells0(
    JNIEnv *aEnv, jclass aClazz, jint sliceX, jint sliceY, jint sliceZ,
    jlong cellsTot, jlong cells, jlong cellsCpu,
    jint localCellCapacity, jint ghostCellCapacity) {
    
    int *tCellsPtr = (int *)(intptr_t)cellsTot;
    int **rCellsCpu = (int **)(intptr_t)cellsCpu;
    
    for (int ck = -1; ck <= sliceZ; ++ck) for (int cj = -1; cj <= sliceY; ++cj) for (int ci = -1; ci <= sliceX; ++ci) {
        const int idx = JSE_CUDANL::cellIndex(sliceX, sliceY, sliceZ, ci, cj, ck);
        rCellsCpu[idx] = tCellsPtr;
        const int cellCap = JSE_CUDANL::cellGhost(sliceX, sliceY, sliceZ, ci, cj, ck) ? ghostCellCapacity : localCellCapacity;
        tCellsPtr += cellCap;
    }
    
    cudaError_t tErr = cudaMemcpy((int **)(intptr_t)cells, rCellsCpu, (sliceX+2)*(sliceY+2)*(sliceZ+2)*sizeof(int *), cudaMemcpyHostToDevice);
    return tErr;
}


JNIEXPORT int JNICALL Java_jse_gpu_CudaNeighborListGetter_buildCells0(
    JNIEnv *aEnv, jclass aClazz, jint aBlockSize, jint nlocal, jint nghost,
    jboolean aPrism, jfloat ax, jfloat ay, jfloat az,
    jfloat bx, jfloat by, jfloat bz, jfloat cx, jfloat cy, jfloat cz,
    jlong pos, jint sliceX, jint sliceY, jint sliceZ,
    jlong cells, jlong cellSize, jlong cellSizeCpu,
    jint localCellCapacity, jint ghostCellCapacity,
    jlong localCellMax, jlong ghostCellMax) {
    
    const int nlocalghost = nlocal + nghost;
    const int tGridSize = (nlocalghost + aBlockSize-1) / aBlockSize;
    
    int *tCellSize = (int *)(intptr_t)cellSize;
    int *tCellSizeCpu = (int *)(intptr_t)cellSizeCpu;
    
    cudaError_t tErr;
    tErr = cudaMemset(tCellSize, 0, (sliceX+2)*(sliceY+2)*(sliceZ+2)*sizeof(int));
    if (tErr!=cudaSuccess) return (int)tErr;
    
    float *posx = (float *)(intptr_t)pos;
    float *posy = (float *)(intptr_t)pos + nlocalghost;
    float *posz = (float *)(intptr_t)pos + nlocalghost*2L;
    
    if (aPrism) {
        JSE_CUDANL::buildCellsKernel<JNI_TRUE><<<tGridSize, (int)aBlockSize>>>(nlocalghost,
            ax, ay, az, bx, by, bz, cx, cy, cz,
            posx, posy, posz, (int)sliceX, (int)sliceY, (int)sliceZ,
            (int **)(intptr_t)cells, tCellSize, (int)localCellCapacity, (int)ghostCellCapacity
        );
    } else {
        JSE_CUDANL::buildCellsKernel<JNI_FALSE><<<tGridSize, (int)aBlockSize>>>(nlocalghost,
            ax, ay, az, bx, by, bz, cx, cy, cz,
            posx, posy, posz, (int)sliceX, (int)sliceY, (int)sliceZ,
            (int **)(intptr_t)cells, tCellSize, (int)localCellCapacity, (int)ghostCellCapacity
        );
    }
    tErr = cudaDeviceSynchronize();
    if (tErr!=cudaSuccess) return (int)tErr;
    
    tErr = cudaMemcpy(tCellSizeCpu, tCellSize, (sliceX+2)*(sliceY+2)*(sliceZ+2)*sizeof(int), cudaMemcpyDeviceToHost);
    if (tErr!=cudaSuccess) return (int)tErr;
    
    int rLocalCellMax = 0, rGhostCellMax = 0;
    for (int ck = -1; ck <= sliceZ; ++ck) for (int cj = -1; cj <= sliceY; ++cj) for (int ci = -1; ci <= sliceX; ++ci) {
        const int idx = JSE_CUDANL::cellIndex(sliceX, sliceY, sliceZ, ci, cj, ck);
        const int cellSizei = tCellSizeCpu[idx];
        if (JSE_CUDANL::cellGhost(sliceX, sliceY, sliceZ, ci, cj, ck)) {
            if (cellSizei > rGhostCellMax) rGhostCellMax = cellSizei;
        } else {
            if (cellSizei > rLocalCellMax) rLocalCellMax = cellSizei;
        }
    }
    *((int *)(intptr_t)localCellMax) = rLocalCellMax;
    *((int *)(intptr_t)ghostCellMax) = rGhostCellMax;
    
    return cudaSuccess;
}

JNIEXPORT int JNICALL Java_jse_gpu_CudaNeighborListGetter_buildNl0(
    JNIEnv *aEnv, jclass aClazz, jint aBlockSize, jint nlocal, jint nghost,
    jboolean aPrism, jfloat ax, jfloat ay, jfloat az,
    jfloat bx, jfloat by, jfloat bz, jfloat cx, jfloat cy, jfloat cz,
    jlong pos, jint sliceX, jint sliceY, jint sliceZ,
    jlong cells, jlong cellSize, jfloat rcutsq,
    jlong nl, jlong nlSize, jlong nlSizeCpu, jint nlCapacity, jlong nlMax) {
    
    const int tGridSize = (nlocal + aBlockSize-1) / aBlockSize;
    
    int *tNlSize = (int *)(intptr_t)nlSize;
    int *tNlSizeCpu = (int *)(intptr_t)nlSizeCpu;
    
    cudaError_t tErr;
    tErr = cudaMemset(tNlSize, 0, nlocal*sizeof(int));
    if (tErr!=cudaSuccess) return (int)tErr;
    
    float *posx = (float *)(intptr_t)pos;
    float *posy = (float *)(intptr_t)pos + (nlocal+nghost);
    float *posz = (float *)(intptr_t)pos + (nlocal+nghost)*2L;
    
    if (aPrism) {
        JSE_CUDANL::buildNlKernel<JNI_TRUE><<<tGridSize, (int)aBlockSize>>>(nlocal,
            ax, ay, az, bx, by, bz, cx, cy, cz,
            posx, posy, posz, (int)sliceX, (int)sliceY, (int)sliceZ,
            (const int **)(intptr_t)cells, (int *)(intptr_t)cellSize, rcutsq,
            (int *)(intptr_t)nl, tNlSize, (int)nlCapacity
        );
    } else {
        JSE_CUDANL::buildNlKernel<JNI_FALSE><<<tGridSize, (int)aBlockSize>>>(nlocal,
            ax, ay, az, bx, by, bz, cx, cy, cz,
            posx, posy, posz, (int)sliceX, (int)sliceY, (int)sliceZ,
            (const int **)(intptr_t)cells, (int *)(intptr_t)cellSize, rcutsq,
            (int *)(intptr_t)nl, tNlSize, (int)nlCapacity
        );
    }
    tErr = cudaDeviceSynchronize();
    if (tErr!=cudaSuccess) return (int)tErr;
    
    tErr = cudaMemcpy(tNlSizeCpu, tNlSize, nlocal*sizeof(int), cudaMemcpyDeviceToHost);
    if (tErr!=cudaSuccess) return (int)tErr;
    
    int rNlMax = 0;
    for (int idx = 0; idx < nlocal; ++idx) {
        const int nlSizei = tNlSizeCpu[idx];
        if (nlSizei > rNlMax) rNlMax = nlSizei;
    }
    *((int *)(intptr_t)nlMax) = rNlMax;
    
    return cudaSuccess;
}

}
