#ifndef NNAP_UTIL_H
#define NNAP_UTIL_H

#include "jniutil.h"
#include <cmath>

namespace JSE_NNAP {

#undef JSE_DBL_MIN_NORMAL
#define JSE_DBL_MIN_NORMAL (2.2250738585072014E-308)
#undef JSE_EPS_MUL
#define JSE_EPS_MUL (8)
#undef JSE_DBL_EPSILON
#define JSE_DBL_EPSILON (1.0e-10)

#undef SQRT2
#define SQRT2 (1.4142135623730951)
#undef SQRT2_INV
#define SQRT2_INV (0.7071067811865475)
#undef SQRT3
#define SQRT3 (1.7320508075688772)
#undef SQRT3DIV2
#define SQRT3DIV2 (1.224744871391589)

#undef PI4
#define PI4 (12.566370614359172)

static inline jdouble pow2(jdouble value) noexcept {
    return value * value;
}
static inline jdouble pow3(jdouble value) noexcept {
    return value * value * value;
}
static inline jdouble pow4(jdouble value) noexcept {
    jdouble value2 = value * value;
    return value2 * value2;
}
static inline jboolean numericEqual(jdouble aLHS, jdouble aRHS) noexcept {
    jdouble tNorm = fabs((double)aLHS) + fabs((double)aRHS);
    if (tNorm < JSE_DBL_MIN_NORMAL * JSE_EPS_MUL) return JNI_TRUE;
    jdouble tDiff = fabs((double)(aLHS - aRHS));
    return (tDiff <= tNorm * JSE_DBL_EPSILON) ? JNI_TRUE : JNI_FALSE;
}

template <jint N>
static inline jdouble dot(jdouble *aArray) noexcept {
    jdouble rDot = 0.0;
    for (jint i = 0; i < N; ++i) {
        rDot += aArray[i]*aArray[i];
    }
    return rDot;
}
static inline jdouble dot(jdouble *aArray, jint aLen) noexcept {
    switch (aLen) {
    case 0: {return 0.0;}
    case 1: {return dot<1>(aArray);}
    case 2: {return dot<2>(aArray);}
    case 3: {return dot<3>(aArray);}
    case 4: {return dot<4>(aArray);}
    case 5: {return dot<5>(aArray);}
    case 6: {return dot<6>(aArray);}
    case 7: {return dot<7>(aArray);}
    case 8: {return dot<8>(aArray);}
    case 9: {return dot<9>(aArray);}
    case 10: {return dot<10>(aArray);}
    case 11: {return dot<11>(aArray);}
    case 12: {return dot<12>(aArray);}
    case 13: {return dot<13>(aArray);}
    case 14: {return dot<14>(aArray);}
    case 15: {return dot<15>(aArray);}
    case 16: {return dot<16>(aArray);}
    case 17: {return dot<17>(aArray);}
    case 18: {return dot<18>(aArray);}
    case 19: {return dot<19>(aArray);}
    case 20: {return dot<20>(aArray);}
    case 21: {return dot<21>(aArray);}
    case 22: {return dot<22>(aArray);}
    case 23: {return dot<23>(aArray);}
    case 24: {return dot<24>(aArray);}
    case 25: {return dot<25>(aArray);}
    case 26: {return dot<26>(aArray);}
    case 27: {return dot<27>(aArray);}
    case 28: {return dot<28>(aArray);}
    case 29: {return dot<29>(aArray);}
    case 30: {return dot<30>(aArray);}
    case 31: {return dot<31>(aArray);}
    case 32: {return dot<32>(aArray);}
    case 33: {return dot<33>(aArray);}
    case 34: {return dot<34>(aArray);}
    case 35: {return dot<35>(aArray);}
    case 36: {return dot<36>(aArray);}
    case 37: {return dot<37>(aArray);}
    case 38: {return dot<38>(aArray);}
    case 39: {return dot<39>(aArray);}
    case 40: {return dot<40>(aArray);}
    case 41: {return dot<41>(aArray);}
    case 42: {return dot<42>(aArray);}
    case 43: {return dot<43>(aArray);}
    case 44: {return dot<44>(aArray);}
    case 45: {return dot<45>(aArray);}
    case 46: {return dot<46>(aArray);}
    case 47: {return dot<47>(aArray);}
    case 48: {return dot<48>(aArray);}
    case 49: {return dot<49>(aArray);}
    case 50: {return dot<50>(aArray);}
    case 51: {return dot<51>(aArray);}
    case 52: {return dot<52>(aArray);}
    case 53: {return dot<53>(aArray);}
    case 54: {return dot<54>(aArray);}
    case 55: {return dot<55>(aArray);}
    case 56: {return dot<56>(aArray);}
    case 57: {return dot<57>(aArray);}
    case 58: {return dot<58>(aArray);}
    case 59: {return dot<59>(aArray);}
    case 60: {return dot<60>(aArray);}
    case 61: {return dot<61>(aArray);}
    case 62: {return dot<62>(aArray);}
    case 63: {return dot<63>(aArray);}
    case 64: {return dot<64>(aArray);}
    case 65: {return dot<65>(aArray);}
    case 66: {return dot<66>(aArray);}
    case 67: {return dot<67>(aArray);}
    case 68: {return dot<68>(aArray);}
    case 69: {return dot<69>(aArray);}
    case 70: {return dot<70>(aArray);}
    case 71: {return dot<71>(aArray);}
    case 72: {return dot<72>(aArray);}
    case 73: {return dot<73>(aArray);}
    case 74: {return dot<74>(aArray);}
    case 75: {return dot<75>(aArray);}
    case 76: {return dot<76>(aArray);}
    case 77: {return dot<77>(aArray);}
    case 78: {return dot<78>(aArray);}
    case 79: {return dot<79>(aArray);}
    case 80: {return dot<80>(aArray);}
    case 81: {return dot<81>(aArray);}
    case 82: {return dot<82>(aArray);}
    case 83: {return dot<83>(aArray);}
    case 84: {return dot<84>(aArray);}
    case 85: {return dot<85>(aArray);}
    case 86: {return dot<86>(aArray);}
    case 87: {return dot<87>(aArray);}
    case 88: {return dot<88>(aArray);}
    case 89: {return dot<89>(aArray);}
    case 90: {return dot<90>(aArray);}
    case 91: {return dot<91>(aArray);}
    case 92: {return dot<92>(aArray);}
    case 93: {return dot<93>(aArray);}
    case 94: {return dot<94>(aArray);}
    case 95: {return dot<95>(aArray);}
    case 96: {return dot<96>(aArray);}
    case 97: {return dot<97>(aArray);}
    case 98: {return dot<98>(aArray);}
    case 99: {return dot<99>(aArray);}
    case 100: {return dot<100>(aArray);}
    case 101: {return dot<101>(aArray);}
    case 102: {return dot<102>(aArray);}
    case 103: {return dot<103>(aArray);}
    case 104: {return dot<104>(aArray);}
    case 105: {return dot<105>(aArray);}
    case 106: {return dot<106>(aArray);}
    case 107: {return dot<107>(aArray);}
    case 108: {return dot<108>(aArray);}
    case 109: {return dot<109>(aArray);}
    case 110: {return dot<110>(aArray);}
    case 111: {return dot<111>(aArray);}
    case 112: {return dot<112>(aArray);}
    case 113: {return dot<113>(aArray);}
    case 114: {return dot<114>(aArray);}
    case 115: {return dot<115>(aArray);}
    case 116: {return dot<116>(aArray);}
    case 117: {return dot<117>(aArray);}
    case 118: {return dot<118>(aArray);}
    case 119: {return dot<119>(aArray);}
    case 120: {return dot<120>(aArray);}
    case 121: {return dot<121>(aArray);}
    case 122: {return dot<122>(aArray);}
    case 123: {return dot<123>(aArray);}
    case 124: {return dot<124>(aArray);}
    case 125: {return dot<125>(aArray);}
    case 126: {return dot<126>(aArray);}
    case 127: {return dot<127>(aArray);}
    case 128: {return dot<128>(aArray);}
    default: {break;}
    }
    jdouble rDot = 0.0;
    jint tEnd = aLen - (16-1);
    jint i = 0;
    jdouble *tBuf = aArray;
    for (; i < tEnd; i+=16, tBuf+=16) {
        for (jint j = 0; j < 16; ++j) {
            rDot += tBuf[j]*tBuf[j];
        }
    }
    switch (aLen-i) {
    case 0: {return rDot;}
    case 1: {return rDot+dot<1>(tBuf);}
    case 2: {return rDot+dot<2>(tBuf);}
    case 3: {return rDot+dot<3>(tBuf);}
    case 4: {return rDot+dot<4>(tBuf);}
    case 5: {return rDot+dot<5>(tBuf);}
    case 6: {return rDot+dot<6>(tBuf);}
    case 7: {return rDot+dot<7>(tBuf);}
    case 8: {return rDot+dot<8>(tBuf);}
    case 9: {return rDot+dot<9>(tBuf);}
    case 10: {return rDot+dot<10>(tBuf);}
    case 11: {return rDot+dot<11>(tBuf);}
    case 12: {return rDot+dot<12>(tBuf);}
    case 13: {return rDot+dot<13>(tBuf);}
    case 14: {return rDot+dot<14>(tBuf);}
    case 15: {return rDot+dot<15>(tBuf);}
    default: {return rDot;}
    }
}
template <jint N>
static inline jdouble dot(jdouble *aArrayL, jdouble *aArrayR) noexcept {
    jdouble rDot = 0.0;
    for (jint i = 0; i < N; ++i) {
        rDot += aArrayL[i]*aArrayR[i];
    }
    return rDot;
}
static inline jdouble dot(jdouble *aArrayL, jdouble *aArrayR, jint aLen) noexcept {
    switch (aLen) {
    case 0: {return 0.0;}
    case 1: {return dot<1>(aArrayL, aArrayR);}
    case 2: {return dot<2>(aArrayL, aArrayR);}
    case 3: {return dot<3>(aArrayL, aArrayR);}
    case 4: {return dot<4>(aArrayL, aArrayR);}
    case 5: {return dot<5>(aArrayL, aArrayR);}
    case 6: {return dot<6>(aArrayL, aArrayR);}
    case 7: {return dot<7>(aArrayL, aArrayR);}
    case 8: {return dot<8>(aArrayL, aArrayR);}
    case 9: {return dot<9>(aArrayL, aArrayR);}
    case 10: {return dot<10>(aArrayL, aArrayR);}
    case 11: {return dot<11>(aArrayL, aArrayR);}
    case 12: {return dot<12>(aArrayL, aArrayR);}
    case 13: {return dot<13>(aArrayL, aArrayR);}
    case 14: {return dot<14>(aArrayL, aArrayR);}
    case 15: {return dot<15>(aArrayL, aArrayR);}
    case 16: {return dot<16>(aArrayL, aArrayR);}
    case 17: {return dot<17>(aArrayL, aArrayR);}
    case 18: {return dot<18>(aArrayL, aArrayR);}
    case 19: {return dot<19>(aArrayL, aArrayR);}
    case 20: {return dot<20>(aArrayL, aArrayR);}
    case 21: {return dot<21>(aArrayL, aArrayR);}
    case 22: {return dot<22>(aArrayL, aArrayR);}
    case 23: {return dot<23>(aArrayL, aArrayR);}
    case 24: {return dot<24>(aArrayL, aArrayR);}
    case 25: {return dot<25>(aArrayL, aArrayR);}
    case 26: {return dot<26>(aArrayL, aArrayR);}
    case 27: {return dot<27>(aArrayL, aArrayR);}
    case 28: {return dot<28>(aArrayL, aArrayR);}
    case 29: {return dot<29>(aArrayL, aArrayR);}
    case 30: {return dot<30>(aArrayL, aArrayR);}
    case 31: {return dot<31>(aArrayL, aArrayR);}
    case 32: {return dot<32>(aArrayL, aArrayR);}
    case 33: {return dot<33>(aArrayL, aArrayR);}
    case 34: {return dot<34>(aArrayL, aArrayR);}
    case 35: {return dot<35>(aArrayL, aArrayR);}
    case 36: {return dot<36>(aArrayL, aArrayR);}
    case 37: {return dot<37>(aArrayL, aArrayR);}
    case 38: {return dot<38>(aArrayL, aArrayR);}
    case 39: {return dot<39>(aArrayL, aArrayR);}
    case 40: {return dot<40>(aArrayL, aArrayR);}
    case 41: {return dot<41>(aArrayL, aArrayR);}
    case 42: {return dot<42>(aArrayL, aArrayR);}
    case 43: {return dot<43>(aArrayL, aArrayR);}
    case 44: {return dot<44>(aArrayL, aArrayR);}
    case 45: {return dot<45>(aArrayL, aArrayR);}
    case 46: {return dot<46>(aArrayL, aArrayR);}
    case 47: {return dot<47>(aArrayL, aArrayR);}
    case 48: {return dot<48>(aArrayL, aArrayR);}
    case 49: {return dot<49>(aArrayL, aArrayR);}
    case 50: {return dot<50>(aArrayL, aArrayR);}
    case 51: {return dot<51>(aArrayL, aArrayR);}
    case 52: {return dot<52>(aArrayL, aArrayR);}
    case 53: {return dot<53>(aArrayL, aArrayR);}
    case 54: {return dot<54>(aArrayL, aArrayR);}
    case 55: {return dot<55>(aArrayL, aArrayR);}
    case 56: {return dot<56>(aArrayL, aArrayR);}
    case 57: {return dot<57>(aArrayL, aArrayR);}
    case 58: {return dot<58>(aArrayL, aArrayR);}
    case 59: {return dot<59>(aArrayL, aArrayR);}
    case 60: {return dot<60>(aArrayL, aArrayR);}
    case 61: {return dot<61>(aArrayL, aArrayR);}
    case 62: {return dot<62>(aArrayL, aArrayR);}
    case 63: {return dot<63>(aArrayL, aArrayR);}
    case 64: {return dot<64>(aArrayL, aArrayR);}
    case 65: {return dot<65>(aArrayL, aArrayR);}
    case 66: {return dot<66>(aArrayL, aArrayR);}
    case 67: {return dot<67>(aArrayL, aArrayR);}
    case 68: {return dot<68>(aArrayL, aArrayR);}
    case 69: {return dot<69>(aArrayL, aArrayR);}
    case 70: {return dot<70>(aArrayL, aArrayR);}
    case 71: {return dot<71>(aArrayL, aArrayR);}
    case 72: {return dot<72>(aArrayL, aArrayR);}
    case 73: {return dot<73>(aArrayL, aArrayR);}
    case 74: {return dot<74>(aArrayL, aArrayR);}
    case 75: {return dot<75>(aArrayL, aArrayR);}
    case 76: {return dot<76>(aArrayL, aArrayR);}
    case 77: {return dot<77>(aArrayL, aArrayR);}
    case 78: {return dot<78>(aArrayL, aArrayR);}
    case 79: {return dot<79>(aArrayL, aArrayR);}
    case 80: {return dot<80>(aArrayL, aArrayR);}
    case 81: {return dot<81>(aArrayL, aArrayR);}
    case 82: {return dot<82>(aArrayL, aArrayR);}
    case 83: {return dot<83>(aArrayL, aArrayR);}
    case 84: {return dot<84>(aArrayL, aArrayR);}
    case 85: {return dot<85>(aArrayL, aArrayR);}
    case 86: {return dot<86>(aArrayL, aArrayR);}
    case 87: {return dot<87>(aArrayL, aArrayR);}
    case 88: {return dot<88>(aArrayL, aArrayR);}
    case 89: {return dot<89>(aArrayL, aArrayR);}
    case 90: {return dot<90>(aArrayL, aArrayR);}
    case 91: {return dot<91>(aArrayL, aArrayR);}
    case 92: {return dot<92>(aArrayL, aArrayR);}
    case 93: {return dot<93>(aArrayL, aArrayR);}
    case 94: {return dot<94>(aArrayL, aArrayR);}
    case 95: {return dot<95>(aArrayL, aArrayR);}
    case 96: {return dot<96>(aArrayL, aArrayR);}
    case 97: {return dot<97>(aArrayL, aArrayR);}
    case 98: {return dot<98>(aArrayL, aArrayR);}
    case 99: {return dot<99>(aArrayL, aArrayR);}
    case 100: {return dot<100>(aArrayL, aArrayR);}
    case 101: {return dot<101>(aArrayL, aArrayR);}
    case 102: {return dot<102>(aArrayL, aArrayR);}
    case 103: {return dot<103>(aArrayL, aArrayR);}
    case 104: {return dot<104>(aArrayL, aArrayR);}
    case 105: {return dot<105>(aArrayL, aArrayR);}
    case 106: {return dot<106>(aArrayL, aArrayR);}
    case 107: {return dot<107>(aArrayL, aArrayR);}
    case 108: {return dot<108>(aArrayL, aArrayR);}
    case 109: {return dot<109>(aArrayL, aArrayR);}
    case 110: {return dot<110>(aArrayL, aArrayR);}
    case 111: {return dot<111>(aArrayL, aArrayR);}
    case 112: {return dot<112>(aArrayL, aArrayR);}
    case 113: {return dot<113>(aArrayL, aArrayR);}
    case 114: {return dot<114>(aArrayL, aArrayR);}
    case 115: {return dot<115>(aArrayL, aArrayR);}
    case 116: {return dot<116>(aArrayL, aArrayR);}
    case 117: {return dot<117>(aArrayL, aArrayR);}
    case 118: {return dot<118>(aArrayL, aArrayR);}
    case 119: {return dot<119>(aArrayL, aArrayR);}
    case 120: {return dot<120>(aArrayL, aArrayR);}
    case 121: {return dot<121>(aArrayL, aArrayR);}
    case 122: {return dot<122>(aArrayL, aArrayR);}
    case 123: {return dot<123>(aArrayL, aArrayR);}
    case 124: {return dot<124>(aArrayL, aArrayR);}
    case 125: {return dot<125>(aArrayL, aArrayR);}
    case 126: {return dot<126>(aArrayL, aArrayR);}
    case 127: {return dot<127>(aArrayL, aArrayR);}
    case 128: {return dot<128>(aArrayL, aArrayR);}
    default: {break;}
    }
    jdouble rDot = 0.0;
    jint tEnd = aLen - (16-1);
    jint i = 0;
    jdouble *tBufL = aArrayL, *tBufR = aArrayR;
    for (; i < tEnd; i+=16, tBufL+=16, tBufR+=16) {
        for (jint j = 0; j < 16; ++j) {
            rDot += tBufL[j]*tBufR[j];
        }
    }
    switch (aLen-i) {
    case 0: {return rDot;}
    case 1: {return rDot+dot<1>(tBufL, tBufR);}
    case 2: {return rDot+dot<2>(tBufL, tBufR);}
    case 3: {return rDot+dot<3>(tBufL, tBufR);}
    case 4: {return rDot+dot<4>(tBufL, tBufR);}
    case 5: {return rDot+dot<5>(tBufL, tBufR);}
    case 6: {return rDot+dot<6>(tBufL, tBufR);}
    case 7: {return rDot+dot<7>(tBufL, tBufR);}
    case 8: {return rDot+dot<8>(tBufL, tBufR);}
    case 9: {return rDot+dot<9>(tBufL, tBufR);}
    case 10: {return rDot+dot<10>(tBufL, tBufR);}
    case 11: {return rDot+dot<11>(tBufL, tBufR);}
    case 12: {return rDot+dot<12>(tBufL, tBufR);}
    case 13: {return rDot+dot<13>(tBufL, tBufR);}
    case 14: {return rDot+dot<14>(tBufL, tBufR);}
    case 15: {return rDot+dot<15>(tBufL, tBufR);}
    default: {return rDot;}
    }
}

template <jint N>
static inline void chebyshevFull(jdouble aX, jdouble *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = 1.0;
    if (N == 0) return;
    rDest[1] = aX;
    for (jint n = 2; n <= N; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}
static inline void chebyshevFull(jint aN, jdouble aX, jdouble *rDest) noexcept {
    if (aN < 0) return;
    rDest[0] = 1.0;
    if (aN == 0) return;
    rDest[1] = aX;
    for (jint n = 2; n <= aN; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}
template <jint N>
static inline void chebyshev2Full(jdouble aX, jdouble *rDest) noexcept {
    if (N < 0) return;
    rDest[0] = 1.0;
    if (N == 0) return;
    rDest[1] = 2.0*aX;
    for (jint n = 2; n <= N; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}
static inline void chebyshev2Full(jint aN, jdouble aX, jdouble *rDest) noexcept {
    if (aN < 0) return;
    rDest[0] = 1.0;
    if (aN == 0) return;
    rDest[1] = 2.0*aX;
    for (jint n = 2; n <= aN; ++n) {
        rDest[n] = 2.0*aX*rDest[n-1] - rDest[n-2];
    }
}

template <jint N>
static inline void calRnPxyz(jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *aCheby2,
                             jdouble aDis, jdouble aRCut, jdouble aDx, jdouble aDy, jdouble aDz) noexcept {
    const jdouble tRnPMul = 2.0 / (aDis*aRCut);
    rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
    for (jint n = 1; n <= N; ++n) {
        const jdouble tRnP = n*tRnPMul*aCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}
static inline void calRnPxyz(jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *aCheby2, jint aNMax,
                             jdouble aDis, jdouble aRCut, jdouble aDx, jdouble aDy, jdouble aDz) noexcept {
    const jdouble tRnPMul = 2.0 / (aDis*aRCut);
    rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
    for (jint n = 1; n <= aNMax; ++n) {
        const jdouble tRnP = n*tRnPMul*aCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}

}

#endif //NNAP_UTIL_H
