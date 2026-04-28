#include "nnap_interface.h"
#include "nnap_main.hpp"

// >>> NNAPGEN REMOVE
#define __NNAPGENS_typeiNNAP__ 1
// <<< NNAPGEN REMOVE

extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calFp(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    
    JSE_NNAP::flt_t *rFp = (JSE_NNAP::flt_t *)tDataOut[0];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    int code;
    // >>> NNAPGEN SWITCH
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rFp,
        tFpHyperParam, tFpParam, NULL
    );
    if (code!=0) return code;
    // <<< NNAPGEN SWITCH (ctype) [FP TYPE]
    return 0;
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calEnergy(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    
    JSE_NNAP::flt_t *rOutEng = (JSE_NNAP::flt_t *)tDataOut[0];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t rLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__];
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rLayers,
        tFpHyperParam, tFpParam, NULL
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        ctype, rOutEng, rLayers,
        tNormParam, tNnParam, NULL, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_calEnergyForce(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    
    JSE_NNAP::flt_t *rOutEng = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *rGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *rGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *rGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[4];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    // manual clear required for backward in force
    for (int j = 0; j < tNeiNum; ++j) {
        rGradNlDx[j] = JSE_NNAP::ZERO;
        rGradNlDy[j] = JSE_NNAP::ZERO;
        rGradNlDz[j] = JSE_NNAP::ZERO;
    }
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t rLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__];
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rLayers,
        tFpHyperParam, tFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        ctype, rOutEng, rLayers,
        tNormParam, tNnParam, rNnGradCache, NULL
    );
    if (code!=0) return code;
    // manual clear required for backward in force
    JSE_NNAP::fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rLayers, JSE_NNAP::ZERO);
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        ctype, JSE_NNAP::ONE, NULL, rLayers, NULL,
        tNormParam, tNnParam, NULL, rNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rLayers,
        rGradNlDx, rGradNlDy, rGradNlDz, tFpHyperParam,
        tFpParam, NULL, rFpForwardCache, NULL, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}


JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_forwardEnergy(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    
    JSE_NNAP::flt_t *rOutEngRaw = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *rNnForwardCache = (JSE_NNAP::flt_t *)tDataOut[2];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    int code;
    // >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *rLayers = rNnForwardCache;
    JSE_NNAP::flt_t *rNnGradCache = rLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rLayers,
        tFpHyperParam, tFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        ctype, rOutEngRaw, rLayers,
        tNormParam, tNnParam, rNnGradCache, NULL
    );
    if (code!=0) return code;
    // <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}
JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_backwardEnergy(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    JSE_NNAP::flt_t *tFpForwardCache = (JSE_NNAP::flt_t *)tDataIn[9];
    JSE_NNAP::flt_t *tNnForwardCache = (JSE_NNAP::flt_t *)tDataIn[10];
    
    JSE_NNAP::flt_t *tGradEng = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t **rGradFpParam = (JSE_NNAP::flt_t **)tDataOut[1];
    JSE_NNAP::flt_t **rGradNnParam = (JSE_NNAP::flt_t **)tDataOut[2];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    int code;
    // >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *tLayers = tNnForwardCache;
    JSE_NNAP::flt_t *tNnGradCache = tLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t rGradLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__] = {0};
    
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        ctype, tGradEng[0], tLayers, rGradLayers, NULL,
        tNormParam, tNnParam, rGradNnParam, tNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rGradLayers,
        NULL, NULL, NULL, tFpHyperParam,
        tFpParam, rGradFpParam, tFpForwardCache, NULL, NULL
    );
    if (code!=0) return code;
    // <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_forwardEnergyForce(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    
    JSE_NNAP::flt_t *rOutEng = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *rGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *rGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *rGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[4];
    JSE_NNAP::flt_t *rNnForwardCache = (JSE_NNAP::flt_t *)tDataOut[5];
    JSE_NNAP::flt_t *rFpBackwardCache = (JSE_NNAP::flt_t *)tDataOut[6];
    JSE_NNAP::flt_t *rNnBackwardCache = (JSE_NNAP::flt_t *)tDataOut[7];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    // manual clear required for backward in force
    for (int j = 0; j < tNeiNum; ++j) {
        rGradNlDx[j] = JSE_NNAP::ZERO;
        rGradNlDy[j] = JSE_NNAP::ZERO;
        rGradNlDz[j] = JSE_NNAP::ZERO;
    }
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *rLayers = rNnForwardCache;
    JSE_NNAP::flt_t *rNnGradCache = rLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t *rNnGradGradCache = rNnBackwardCache;
    JSE_NNAP::flt_t *rAGradLayers = rNnGradGradCache + __NNAPGENX_NN_SIZE_HB__;
    JSE_NNAP::flt_t *rAGradLayersZ = rAGradLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    // manual clear required for backward in force
    JSE_NNAP::fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rAGradLayers, JSE_NNAP::ZERO);
    JSE_NNAP::fill<__NNAPGENX_NN_SIZE_HB__>(rAGradLayersZ, JSE_NNAP::ZERO);
    
    code = JSE_NNAP::fpForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rLayers,
        tFpHyperParam, tFpParam, rFpForwardCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnForward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
        ctype, rOutEng, rLayers,
        tNormParam, tNnParam, rNnGradCache, rNnGradGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
        ctype, JSE_NNAP::ONE, NULL, rAGradLayers, rAGradLayersZ,
        tNormParam, tNnParam, NULL, rNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE, JSE_NNAP::FALSE, JSE_NNAP::TRUE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rAGradLayers,
        rGradNlDx, rGradNlDy, rGradNlDz, tFpHyperParam,
        tFpParam, NULL, rFpForwardCache, rFpBackwardCache, NULL
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}
JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_backwardEnergyForce(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[5];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t *tNormParam = (JSE_NNAP::flt_t *)tDataIn[8];
    JSE_NNAP::flt_t *tFpForwardCache = (JSE_NNAP::flt_t *)tDataIn[9];
    JSE_NNAP::flt_t *tNnForwardCache = (JSE_NNAP::flt_t *)tDataIn[10];
    JSE_NNAP::flt_t *tFpBackwardCache = (JSE_NNAP::flt_t *)tDataIn[11];
    JSE_NNAP::flt_t *tNnBackwardCache = (JSE_NNAP::flt_t *)tDataIn[12];
    
    JSE_NNAP::flt_t *tBGradEng = (JSE_NNAP::flt_t *)tDataOut[0];
    JSE_NNAP::flt_t *tBGradAGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *tBGradAGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *tBGradAGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t **rBGradFpParam = (JSE_NNAP::flt_t **)tDataOut[4];
    JSE_NNAP::flt_t **rBGradNnParam = (JSE_NNAP::flt_t **)tDataOut[5];
    JSE_NNAP::flt_t *rFpBackwardBackwardCache = (JSE_NNAP::flt_t *)tDataOut[6];
    
    int tNeiNum = tNums[0];
    int ctype = tNums[1];
    
    int code;
// >>> NNAPGEN SWITCH
    JSE_NNAP::flt_t *tLayers = tNnForwardCache;
    JSE_NNAP::flt_t *tNnGradCache = tLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t *tNnGradGradCache = tNnBackwardCache;
    JSE_NNAP::flt_t *tAGradLayers = tNnGradGradCache + __NNAPGENX_NN_SIZE_HB__;
    JSE_NNAP::flt_t *tAGradLayersZ = tAGradLayers + (__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__);
    JSE_NNAP::flt_t rBGradAGradLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__] = {0};
    JSE_NNAP::flt_t rBGradLayersZ[__NNAPGENX_NN_SIZE_HB__] = {0};
    JSE_NNAP::flt_t rBGradLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__] = {0};
    
    code = JSE_NNAP::fpBackwardBackward<__NNAPGENS_ctype__>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, tAGradLayers, rBGradAGradLayers,
        tBGradAGradNlDx, tBGradAGradNlDy, tBGradAGradNlDz,
        tFpHyperParam, rBGradFpParam, tFpForwardCache, tFpBackwardCache, rFpBackwardBackwardCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnBackwardBackward<__NNAPGENS_ctype__, JSE_NNAP::FALSE>(
        ctype, JSE_NNAP::ONE, NULL, tAGradLayers, rBGradAGradLayers, tAGradLayersZ, rBGradLayersZ,
        tNormParam, tNnParam, rBGradNnParam, tNnGradCache, tNnGradGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::normedNnBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::TRUE>(
        ctype, tBGradEng[0], tLayers, rBGradLayers, rBGradLayersZ,
        tNormParam, tNnParam, rBGradNnParam, tNnGradCache
    );
    if (code!=0) return code;
    code = JSE_NNAP::fpBackward<__NNAPGENS_ctype__, JSE_NNAP::TRUE, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
        tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, ctype, rBGradLayers,
        NULL, NULL, NULL, tFpHyperParam,
        tFpParam, rBGradFpParam, tFpForwardCache, NULL, rFpBackwardBackwardCache
    );
    if (code!=0) return code;
// <<< NNAPGEN SWITCH (ctype) [FP NN TYPE]
    return 0;
}


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

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nnap_computeLammps(void *aDataIn, void *rDataOut) {
    void **tDataIn = (void **)aDataIn;
    void **tDataOut = (void **)rDataOut;
    
    int *tNums = (int *)tDataIn[0];
    JSE_NNAP::flt_t *tNlDx = (JSE_NNAP::flt_t *)tDataIn[1];
    JSE_NNAP::flt_t *tNlDy = (JSE_NNAP::flt_t *)tDataIn[2];
    JSE_NNAP::flt_t *tNlDz = (JSE_NNAP::flt_t *)tDataIn[3];
    int *tNlType = (int *)tDataIn[4];
    int *tNlIdx = (int *)tDataIn[5];
    JSE_NNAP::flt_t **tFpHyperParam = (JSE_NNAP::flt_t **)tDataIn[6];
    JSE_NNAP::flt_t **tFpParam = (JSE_NNAP::flt_t **)tDataIn[7];
    JSE_NNAP::flt_t **tNnParam = (JSE_NNAP::flt_t **)tDataIn[8];
    JSE_NNAP::flt_t **tNormParam = (JSE_NNAP::flt_t **)tDataIn[9];
    
    JSE_NNAP::flt_t *rGradNlDx = (JSE_NNAP::flt_t *)tDataOut[1];
    JSE_NNAP::flt_t *rGradNlDy = (JSE_NNAP::flt_t *)tDataOut[2];
    JSE_NNAP::flt_t *rGradNlDz = (JSE_NNAP::flt_t *)tDataOut[3];
    JSE_NNAP::flt_t *rFpForwardCache = (JSE_NNAP::flt_t *)tDataOut[4];
    
    int inum = tNums[0];
    int ntypes = tNums[1];
    int eflag = tNums[2];
    int vflag = tNums[3];
    int eflagAtom = tNums[4];
    int vflagAtom = tNums[5];
    int cvflagAtom = tNums[6];
    
    double **x = (double **)tDataIn[10];
    double **f = (double **)tDataOut[0];
    int *type = (int *)tDataIn[11];
    
    int *ilist = (int *)tDataIn[12];
    int *numneigh = (int *)tDataIn[13];
    int **firstneigh = (int **)tDataIn[14];
    double *cutsq = (double *)tDataIn[15];
    int *tLmpType2NNAPType = (int *)tDataIn[16];
    int **tTypeIlist = (int **)tDataIn[17];
    int *tTypeInum = (int *)tDataIn[18];
    
    double *engVdwl = (double *)tDataOut[5];
    double *eatom = (double *)tDataOut[6];
    double *virial = (double *)tDataOut[7];
    double **vatom = (double **)tDataOut[8];
    double **cvatom = (double **)tDataOut[9];
    
    /// reorder by types
    for (int typei = 1; typei <= ntypes; ++typei) {
        tTypeInum[typei] = 0;
    }
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int typei = type[i];
        tTypeIlist[typei][tTypeInum[typei]] = i;
        ++tTypeInum[typei];
    }
    
    /// begin compute here
    for (int typei = 1; typei <= ntypes; ++typei) {
        int *subIlist = tTypeIlist[typei];
        int subInum = tTypeInum[typei];
        
        const int typeiNNAP = tLmpType2NNAPType[typei];
        JSE_NNAP::flt_t *subNormParam = tNormParam[typeiNNAP-1];
        
        for (int ii = 0; ii < subInum; ++ii) {
            int i = subIlist[ii];
            double xtmp = x[i][0];
            double ytmp = x[i][1];
            double ztmp = x[i][2];
            int *jlist = firstneigh[i];
            int jnum = numneigh[i];
            
            /// build neighbor list
            int tNeiNum = 0;
            for (int jj = 0; jj < jnum; ++jj) {
                int j = jlist[jj];
                j &= JSE_LMP_NEIGHMASK;
                // Note that dxyz in jse and lammps are defined oppositely
                double delx = x[j][0] - xtmp;
                double dely = x[j][1] - ytmp;
                double delz = x[j][2] - ztmp;
                double rsq = delx*delx + dely*dely + delz*delz;
                if (rsq < cutsq[typei]) {
                    tNlDx[tNeiNum] = (JSE_NNAP::flt_t)delx;
                    tNlDy[tNeiNum] = (JSE_NNAP::flt_t)dely;
                    tNlDz[tNeiNum] = (JSE_NNAP::flt_t)delz;
                    tNlType[tNeiNum] = tLmpType2NNAPType[type[j]];
                    tNlIdx[tNeiNum] = j;
                    ++tNeiNum;
                }
            }
            
            /// begin nnap here
            JSE_NNAP::flt_t rEng;
            // manual clear required for backward in force
            for (int j = 0; j < tNeiNum; ++j) {
                rGradNlDx[j] = JSE_NNAP::ZERO;
                rGradNlDy[j] = JSE_NNAP::ZERO;
                rGradNlDz[j] = JSE_NNAP::ZERO;
            }
            int code;
// >>> NNAPGEN SWITCH
            JSE_NNAP::flt_t rLayers[__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__];
            code = JSE_NNAP::fpForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rLayers,
                tFpHyperParam, tFpParam, rFpForwardCache
            );
            if (code!=0) return code;
            JSE_NNAP::flt_t rNnGradCache[__NNAPGENX_NN_SIZE_HB__];
            code = JSE_NNAP::normedNnForward<__NNAPGENS_typeiNNAP__, JSE_NNAP::TRUE, JSE_NNAP::FALSE>(
                typeiNNAP, &rEng, rLayers,
                subNormParam, tNnParam, rNnGradCache, NULL
            );
            if (code!=0) return code;
            // manual clear required for backward in force
            JSE_NNAP::fill<__NNAPGENX_NN_SIZE_IN__+__NNAPGENX_NN_SIZE_HB__>(rLayers, JSE_NNAP::ZERO);
            code = JSE_NNAP::normedNnBackward<__NNAPGENS_typeiNNAP__, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
                typeiNNAP, JSE_NNAP::ONE, NULL, rLayers, NULL,
                subNormParam, tNnParam, NULL, rNnGradCache
            );
            if (code!=0) return code;
            code = JSE_NNAP::fpBackward<__NNAPGENS_typeiNNAP__, JSE_NNAP::FALSE, JSE_NNAP::FALSE, JSE_NNAP::FALSE>(
                tNlDx, tNlDy, tNlDz, tNlType, tNeiNum, typeiNNAP, rLayers,
                rGradNlDx, rGradNlDy, rGradNlDz, tFpHyperParam,
                tFpParam, NULL, rFpForwardCache, NULL, NULL
            );
            if (code!=0) return code;
// <<< NNAPGEN SWITCH (typeiNNAP) [FP NN TYPE]
            
            /// collect results
            if (eflag) {
                *engVdwl += rEng;
                if (eflagAtom) eatom[i] += rEng;
            }
            for (int jj = 0; jj < tNeiNum; ++jj) {
                int j = tNlIdx[jj];
                const JSE_NNAP::flt_t fx = rGradNlDx[jj];
                const JSE_NNAP::flt_t fy = rGradNlDy[jj];
                const JSE_NNAP::flt_t fz = rGradNlDz[jj];
                f[i][0] -= fx;
                f[i][1] -= fy;
                f[i][2] -= fz;
                f[j][0] += fx;
                f[j][1] += fy;
                f[j][2] += fz;
                if (vflag) {
                    const JSE_NNAP::flt_t dx = tNlDx[jj];
                    const JSE_NNAP::flt_t dy = tNlDy[jj];
                    const JSE_NNAP::flt_t dz = tNlDz[jj];
                    virial[0] += dx*fx;
                    virial[1] += dy*fy;
                    virial[2] += dz*fz;
                    virial[3] += dx*fy;
                    virial[4] += dx*fz;
                    virial[5] += dy*fz;
                    if (vflagAtom) {
                        vatom[j][0] += dx*fx;
                        vatom[j][1] += dy*fy;
                        vatom[j][2] += dz*fz;
                        vatom[j][3] += dx*fy;
                        vatom[j][4] += dx*fz;
                        vatom[j][5] += dy*fz;
                    }
                    if (cvflagAtom) {
                        cvatom[j][0] += dx*fx;
                        cvatom[j][1] += dy*fy;
                        cvatom[j][2] += dz*fz;
                        cvatom[j][3] += dx*fy;
                        cvatom[j][4] += dx*fz;
                        cvatom[j][5] += dy*fz;
                        cvatom[j][6] += dy*fx;
                        cvatom[j][7] += dz*fx;
                        cvatom[j][8] += dz*fy;
                    }
                }
            }
        }
    }
    return 0;
}

}

