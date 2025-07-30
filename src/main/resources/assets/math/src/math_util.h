#include "jniutil.h"
#include <math.h>

#ifndef MATH_UTIL_H
#define MATH_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

static inline jdouble sum_jse(jdouble *aArray, jint aLen) {
    jdouble rSum = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rSum += tBuf[j];
        }
    }
    for (; i < aLen; ++i) {
        rSum += aArray[i];
    }
    return rSum;
}
static inline jdouble prod_jse(jdouble *aArray, jint aLen) {
    jdouble rProd = 1.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rProd *= tBuf[j];
        }
    }
    for (; i < aLen; ++i) {
        rProd *= aArray[i];
    }
    return rProd;
}
static inline jdouble dot_jse(jdouble *aArray, jint aLen) {
    jdouble rDot = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rDot += tBuf[j]*tBuf[j];
        }
    }
    for (; i < aLen; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
static inline jdouble dotAB_jse(jdouble *aArrayL, jdouble *aArrayR, jint aLen) {
    jdouble rDot = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBufL = aArrayL, *tBufR = aArrayR; i < tEnd; i += JSE_BATCH_SIZE, tBufL += JSE_BATCH_SIZE, tBufR += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rDot += tBufL[j]*tBufR[j];
        }
    }
    for (; i < aLen; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}
static inline jdouble norm1_jse(jdouble *aArray, jint aLen) {
    jdouble rNorm = 0.0;
    jint tEnd = aLen - JSE_BATCH_SIZE - 1;
    jint i = 0;
    for (jdouble *tBuf = aArray; i < tEnd; i += JSE_BATCH_SIZE, tBuf += JSE_BATCH_SIZE) {
        for (jint j = 0; j < JSE_BATCH_SIZE; ++j) {
            rNorm += fabs((double)tBuf[j]);
        }
    }
    for (; i < aLen; ++i) {
        rNorm += fabs((double)aArray[i]);
    }
    return rNorm;
}
static inline void fill_jse(jdouble *rArray, jdouble aValue, jint aLen) {
    for (jint i = 0; i < aLen; ++i) {
        rArray[i] = aValue;
    }
}
static inline void fillAB_jse(jdouble *rArrayA, jdouble *aArrayB, jint aLen) {
    for (jint i = 0; i < aLen; ++i) {
        rArrayA[i] = aArrayB[i];
    }
}

static inline void matmulRC2This_jse(jdouble *rThisRowL, jdouble *aDataColR, jdouble *rBufRow, jint aRowNum, jint aColNum) {
    jdouble *tBufL = rThisRowL;
    for (jint i = 0; i < aRowNum; ++i, tBufL+=aColNum) {
        fillAB_jse(rBufRow, tBufL, aColNum);
        jdouble *tBufR = aDataColR;
        for (jint j = 0; j < aColNum; ++j, tBufR+=aColNum) {
            jdouble rDot = 0.0;
            for (jint k = 0; k < aColNum; ++k) {
                rDot += rBufRow[k]*tBufR[k];
            }
            tBufL[j] = rDot;
        }
    }
}
static inline void lmatmulCR2This_jse(jdouble *rThisColL, jdouble *aDataRowR, jdouble *rBufCol, jint aRowNum, jint aColNum) {
    jdouble *tBufL = rThisColL;
    for (jint j = 0; j < aColNum; ++j, tBufL+=aRowNum){
        fillAB_jse(rBufCol, tBufL, aRowNum);
        jdouble *tBufR = aDataRowR;
        for (jint i = 0; i < aRowNum; ++i, tBufR+=aRowNum) {
            jdouble rDot = 0.0;
            for (jint k = 0; k < aRowNum; ++k) {
                rDot += rBufCol[k]*tBufR[k];
            }
            tBufL[i] = rDot;
        }
    }
}

static inline void matmulRCR_jse(jdouble *aDataRowL, jdouble *aDataColR, jdouble *rDestRow, jint aRowNum, jint aColNum, jint aMidNum) {
    jdouble *tBufL = aDataRowL;
    for (jint i = 0; i < aRowNum; ++i, tBufL+=aMidNum) {
        jdouble *tBufR = aDataColR;
        for (jint j = 0; j < aColNum; ++j, tBufR+=aMidNum) {
            jdouble rDot = 0.0;
            for (jint k = 0; k < aMidNum; ++k) {
                rDot += tBufL[k]*tBufR[k];
            }
            rDestRow[j] = rDot;
        }
        rDestRow += aColNum;
    }
}
static inline void matmulRCC_jse(jdouble *aDataRowL, jdouble *aDataColR, jdouble *rDestCol, jint aRowNum, jint aColNum, jint aMidNum) {
    jdouble *tBufR = aDataColR;
    for (jint j = 0; j < aColNum; ++j, tBufR+=aMidNum) {
        jdouble *tBufL = aDataRowL;
        for (jint i = 0; i < aRowNum; ++i, tBufL+=aMidNum) {
            jdouble rDot = 0.0;
            for (jint k = 0; k < aMidNum; ++k) {
                rDot += tBufL[k]*tBufR[k];
            }
            rDestCol[i] = rDot;
        }
        rDestCol += aRowNum;
    }
}

static inline void blockMatmul_jse(jdouble *aBlockL, jdouble *aBlockR, jdouble *rBlockD, jint aColNum, jint aMidNum) {
    jdouble *tBufL = aBlockL;
    for (jint i = 0; i < JSE_BLOCK_SIZE; ++i, tBufL+=aMidNum) {
        jdouble *tBufR = aBlockR;
        for (jint j = 0; j < JSE_BLOCK_SIZE; ++j, tBufR+=aMidNum) {
            jdouble rDot = 0.0;
            for (jint k = 0; k < JSE_BLOCK_SIZE; ++k) {
                rDot += tBufL[k]*tBufR[k];
            }
            rBlockD[j] += rDot;
        }
        rBlockD += aColNum;
    }
}
static inline void blockMatmulV_jse(jdouble *aBlockL, jdouble *aBlockR, jdouble *rBlockD, jint aColNum, jint aMidNum, jint aBlockRowNum, jint aBlockColNum, jint aBlockMidNum) {
    jdouble *tBufL = aBlockL;
    for (jint i = 0; i < aBlockRowNum; ++i, tBufL+=aMidNum) {
        jdouble *tBufR = aBlockR;
        for (jint j = 0; j < aBlockColNum; ++j, tBufR+=aMidNum) {
            jdouble rDot = 0.0;
            for (jint k = 0; k < aBlockMidNum; ++k) {
                rDot += tBufL[k]*tBufR[k];
            }
            rBlockD[j] += rDot;
        }
        rBlockD += aColNum;
    }
}


static inline void matmulBlockRC_jse(jdouble *aDataRowL, jdouble *aDataColR, jdouble *rDestRow, jboolean aRestDest, jint aRowNum, jint aColNum, jint aMidNum) {
    if (aRestDest) {
        fill_jse(rDestRow, 0.0, aRowNum*aColNum);
    }
    const jint blockRowNum = aRowNum / JSE_BLOCK_SIZE;
    const jint blockColNum = aColNum / JSE_BLOCK_SIZE;
    const jint blockMidNum = aMidNum / JSE_BLOCK_SIZE;
    const jint restRowNum = aRowNum % JSE_BLOCK_SIZE;
    const jint restColNum = aColNum % JSE_BLOCK_SIZE;
    const jint restMidNum = aMidNum % JSE_BLOCK_SIZE;
    
    jdouble *tBufL = aDataRowL;
    jdouble *tBufD = rDestRow;
    for (jint rowB = 0; rowB <= blockRowNum; ++rowB, tBufL+=JSE_BLOCK_SIZE*aMidNum, tBufD+=JSE_BLOCK_SIZE*aColNum) {
        const jint tBlockRowNum = rowB==blockRowNum ? restRowNum : JSE_BLOCK_SIZE;
        if (tBlockRowNum == 0) continue;
        jdouble *tBufR = aDataColR;
        jdouble *tBufBufD = tBufD;
        for (jint colB = 0; colB <= blockColNum; ++colB, tBufR+=JSE_BLOCK_SIZE*aMidNum, tBufBufD+=JSE_BLOCK_SIZE) {
            const jint tBlockColNum = colB==blockColNum ? restColNum : JSE_BLOCK_SIZE;
            if (tBlockColNum == 0) continue;
            jdouble *tBufBufL = tBufL;
            jdouble *tBufBufR = tBufR;
            for (jint midB = 0; midB <= blockMidNum; ++midB, tBufBufL+=JSE_BLOCK_SIZE, tBufBufR+=JSE_BLOCK_SIZE) {
                const jint tBlockMidNum = midB==blockMidNum ? restMidNum : JSE_BLOCK_SIZE;
                if (tBlockMidNum == 0) continue;
                if (colB==blockColNum || rowB==blockRowNum || midB==blockMidNum) {
                    blockMatmulV_jse(tBufBufL, tBufBufR, tBufBufD, aColNum, aMidNum, tBlockRowNum, tBlockColNum, tBlockMidNum);
                } else {
                    blockMatmul_jse(tBufBufL, tBufBufR, tBufBufD, aColNum, aMidNum);
                }
            }
        }
    }
}

#ifdef __cplusplus
}
#endif

#endif //MATH_UTIL_H
