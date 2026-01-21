#include "nnap_interface.h"

#include "basis_Chebyshev.hpp"
#include "nn_FeedForward.hpp"

// >>> NNAPGEN REMOVE
#define NNAPGEN_FP_WTYPE JSE_NNAP::WTYPE_DEFAULT
#define NNAPGEN_FP_NMAX 5
#define NNAPGEN_FP_FSIZE 0
#define NNAPGEN_FP_FSTYLE JSE_NNAP::FSTYLE_LIMITED
#define NNAPGEN_FP_SIZE 10
// #define NNAPGEN_FP_SIZE_FW 0
// #define NNAPGEN_FP_CACHE_SIZE_F0 (5+1)
#define NNAPGEN_NN_SIZE_HW (10*32 + 32*24)
#define NNAPGEN_NN_SIZE_HB (32 + 24)
#define NNAPGEN_NN_SIZE_OW 24
// <<< NNAPGEN REMOVE

extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_forward(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    double *tNlDx = (double *)tDataIn[1];
    double *tNlDy = (double *)tDataIn[2];
    double *tNlDz = (double *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    double *tFpParam = (double *)tDataIn[5];
    double *tNnParam = (double *)tDataIn[6];
    double *tNormParam = (double *)tDataIn[7];
    
    double *rOut = (double *)tDataOut[0];
    double *rFp = (double *)tDataOut[1];
    double *rFpCache = (double *)tDataOut[2];
    double *rNnCache = (double *)tDataOut[3];
    
    double *tNormMu = tNormParam;
    double *tNormSigma = tNormMu + NNAPGEN_FP_SIZE;
    double tNormMuEng = tNormSigma[NNAPGEN_FP_SIZE];
    double tNormSigmaEng = tNormSigma[NNAPGEN_FP_SIZE+1];
    
    int tNN = tNums[0];
    double tRCut = tFpParam[0];
    double *tFuseWeight = tFpParam + 1;
    JSE_NNAP::chebyForward<NNAPGEN_FP_WTYPE, NNAPGEN_FP_NMAX, NNAPGEN_FP_FSIZE, NNAPGEN_FP_FSTYLE, NNAPGEN_FP_SIZE, JSE_NNAP::CL_NONE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNN, rFp,
        rFpCache, tRCut, tFuseWeight
    );
    // norm fp here
    for (int i = 0; i < NNAPGEN_FP_SIZE; ++i) {
        rFp[i] = (rFp[i] - tNormMu[i]) / tNormSigma[i];
    }
    
    double *tHiddenWeights = tNnParam;
    double *tOutputWeights = tHiddenWeights + NNAPGEN_NN_SIZE_HW;
    double *tHiddenBiases = tOutputWeights + NNAPGEN_NN_SIZE_OW;
    double tOutputBias = tHiddenBiases[NNAPGEN_NN_SIZE_HB];
    double tEng = JSE_NNAP::nnForward(rFp, tHiddenWeights, tHiddenBiases, tOutputWeights, tOutputBias, rNnCache);
    // denorm energy here
    tEng = tEng*tNormSigmaEng + tNormMuEng;
    
    *rOut = tEng;
    return 0;
}

}


