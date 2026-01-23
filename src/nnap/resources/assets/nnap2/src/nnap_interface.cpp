#include "nnap_interface.h"

#include "basis_Chebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define NNAPGENX_FP_WTYPE JSE_NNAP::WTYPE_DEFAULT
#define NNAPGENX_FP_NMAX 5
#define NNAPGENX_FP_FSIZE 0
#define NNAPGENX_FP_FSTYLE JSE_NNAP::FSTYLE_LIMITED
#define NNAPGENX_FP_SIZE 10
#define NNAPGENX_NN_SIZE_IN 10
#define NNAPGENX_NN_SIZE_HW (10*32)
#define NNAPGENX_NN_SIZE_HB 32
#define NNAPGENX_NN_SIZE_OW 32
#define NNAPGENS_aCType 1
// <<< NNAPGEN REMOVE


namespace JSE_NNAP {

template <int FP_FULL_CACHE, int NN_CACHE_GRAD>
static int forward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNN, int aCType,
                   double *aFpParam, double *aNnParam, double *aNormParam,
                   double *rFpCache, double *rNnCache, double *rOutEng) noexcept {
    double tNormMuEng = aNormParam[0];
    double tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    double *tNormMu = aNormParam + 2;
    double *tNormSigma = tNormMu + NNAPGENX_FP_SIZE;
    double *rFp = rNnCache; // fp from nn cache, for smooth input
    chebyForward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NMAX, NNAPGENX_FP_FSIZE, NNAPGENX_FP_FSTYLE, NNAPGENX_FP_SIZE, FP_FULL_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNN, rFp,
        rFpCache, aFpParam[0], aFpParam+1
    );
    // norm fp here
    for (int i = 0; i < NNAPGENX_FP_SIZE; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [FP TYPE]
    if (flag) return 1;
    
// >>> NNAPGEN SWITCH
    double *tHiddenWeights = aNnParam;
    double *tOutputWeights = tHiddenWeights + NNAPGENX_NN_SIZE_HW;
    double *tHiddenBiases = tOutputWeights + NNAPGENX_NN_SIZE_OW;
    double tOutputBias = tHiddenBiases[NNAPGENX_NN_SIZE_HB];
    double *rLayers = rNnCache; // first layer is fp
    double *rSiLUGrad = NN_CACHE_GRAD ? (rLayers+(NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB)) : NULL;
    double tEng = nnForward<NNAPGENS_aCType, NN_CACHE_GRAD>(rLayers, tHiddenWeights, tHiddenBiases, tOutputWeights, tOutputBias, rSiLUGrad);
    // denorm energy here
    tEng = tEng*tNormSigmaEng + tNormMuEng;
    *rOutEng = tEng;
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [NN TYPE]
    if (flag) return 1;
    
    return 0;
}


template <int FP_FULL_CACHE, int CLEAR_CACHE>
static int backward(double *aNlDx, double *aNlDy, double *aNlDz, int *aNlType, int aNN, int aCType,
                    double *rGradNlDx, double *rGradNlDy, double *rGradNlDz,
                    double *aFpParam, double *aNnParam, double *aNormParam,
                    double *rFpCache, double *rNnCache, double aInGradEng) noexcept {
    double tNormSigmaEng = aNormParam[1];
    int flag = 1;
// >>> NNAPGEN SWITCH
    // denorm energy here
    aInGradEng = aInGradEng*tNormSigmaEng;
    double *tHiddenWeights = aNnParam;
    double *tOutputWeights = tHiddenWeights + NNAPGENX_NN_SIZE_HW;
    double *rGradLayers = rNnCache; // first layer is fp
    double *tSiLUGrad = rGradLayers + (NNAPGENX_NN_SIZE_IN+NNAPGENX_NN_SIZE_HB);
    nnBackward<NNAPGENS_aCType, CLEAR_CACHE>(aInGradEng, rGradLayers, tHiddenWeights, tOutputWeights, tSiLUGrad);
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [NN TYPE]
    if (flag) return 1;

// >>> NNAPGEN SWITCH
    double *tNormMu = aNormParam + 2;
    double *tNormSigma = tNormMu + NNAPGENX_FP_SIZE;
    double *tGradFp = rNnCache; // fp from nn cache, for smooth input
    // norm fp here
    for (int i = 0; i < NNAPGENX_FP_SIZE; ++i) {
        tGradFp[i] /= tNormSigma[i];
    }
    chebyBackward<NNAPGENX_FP_WTYPE, NNAPGENX_FP_NMAX, NNAPGENX_FP_FSIZE, NNAPGENX_FP_FSTYLE, FP_FULL_CACHE, CLEAR_CACHE>(
        aNlDx, aNlDy, aNlDz, aNlType, aNN, tGradFp,
        rGradNlDx, rGradNlDy, rGradNlDz,
        rFpCache, aFpParam[0], aFpParam+1
    );
    flag = 0;
// <<< NNAPGEN SWITCH (aCType) [FP TYPE]
    if (flag) return 1;

    return 0;
}

}


extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calEnergy(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[1]; // 0 for eng inputs
    double *tNlDx = (double *)tDataIn[2];
    double *tNlDy = (double *)tDataIn[3];
    double *tNlDz = (double *)tDataIn[4];
    int *tNlType = (int *)tDataIn[5];
    double *tFpParam = (double *)tDataIn[6];
    double *tNnParam = (double *)tDataIn[7];
    double *tNormParam = (double *)tDataIn[8];
    
    double *rOutEng = (double *)tDataOut[0];
    double *rFpCache = (double *)tDataOut[4];
    double *rNnCache = (double *)tDataOut[5];
    
    int tNN = tNums[0];
    int ctype = tNums[1];
    
    return JSE_NNAP::forward<JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tFpParam, tNnParam, tNormParam,
        rFpCache, rNnCache, rOutEng
    );
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calEnergyForce(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[1]; // 0 for eng inputs
    double *tNlDx = (double *)tDataIn[2];
    double *tNlDy = (double *)tDataIn[3];
    double *tNlDz = (double *)tDataIn[4];
    int *tNlType = (int *)tDataIn[5];
    double *tFpParam = (double *)tDataIn[6];
    double *tNnParam = (double *)tDataIn[7];
    double *tNormParam = (double *)tDataIn[8];
    
    double *rOutEng = (double *)tDataOut[0];
    double *tGradNlDx = (double *)tDataOut[1];
    double *tGradNlDy = (double *)tDataOut[2];
    double *tGradNlDz = (double *)tDataOut[3];
    double *rFpCache = (double *)tDataOut[4];
    double *rNnCache = (double *)tDataOut[5];
    
    int tNN = tNums[0];
    int ctype = tNums[1];
    
    int code = JSE_NNAP::forward<JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tFpParam, tNnParam, tNormParam,
        rFpCache, rNnCache, rOutEng
    );
    if (code!=0) return code;
    
    // manual clear required for backward in force
    return JSE_NNAP::backward<JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, ctype,
        tGradNlDx, tGradNlDy, tGradNlDz,
        tFpParam, tNnParam, tNormParam,
        rFpCache, rNnCache, 1.0
    );
}

}


