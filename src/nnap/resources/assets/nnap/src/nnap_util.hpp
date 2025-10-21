#ifndef NNAP_UTIL_H
#define NNAP_UTIL_H

#include "jniutil.h"
#include <cmath>

namespace JSE_NNAP {

static constexpr jint WTYPE_DEFAULT = 0;
static constexpr jint WTYPE_NONE    = -1;
static constexpr jint WTYPE_SINGLE  = 1; // unused
static constexpr jint WTYPE_FULL    = 2;
static constexpr jint WTYPE_EXFULL  = 3;
static constexpr jint WTYPE_FUSE    = 4;
static constexpr jint WTYPE_RFUSE   = 5; // unused
static constexpr jint WTYPE_EXFUSE  = 6;

static constexpr jint FUSE_STYLE_LIMITED = 0;
static constexpr jint FUSE_STYLE_EXTENSIVE = 1;

static constexpr jdouble JSE_DBL_MIN_NORMAL = 2.2250738585072014E-308;
static constexpr jint JSE_EPS_MUL = 8;
static constexpr jdouble JSE_DBL_EPSILON = 1.0e-10;

static constexpr jdouble SQRT2 = 1.4142135623730951;
static constexpr jdouble SQRT2_INV = 0.7071067811865475;
static constexpr jdouble SQRT3 = 1.7320508075688772;
static constexpr jdouble SQRT3DIV2 = 1.224744871391589;
static constexpr jdouble PI4 = 12.566370614359172;

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
static inline void mplus(jdouble *rArrayL, jdouble aMul, jdouble *aArrayR) noexcept {
    for (jint i = 0; i < N; ++i) {
        rArrayL[i] += aMul*aArrayR[i];
    }
}
static inline void mplus(jdouble *rArrayL, jdouble aMul, jdouble *aArrayR, jint aLen) noexcept {
    switch (aLen) {
    case 0: {return;}
    case 1: {mplus<1>(rArrayL, aMul, aArrayR); return;}
    case 2: {mplus<2>(rArrayL, aMul, aArrayR); return;}
    case 3: {mplus<3>(rArrayL, aMul, aArrayR); return;}
    case 4: {mplus<4>(rArrayL, aMul, aArrayR); return;}
    case 5: {mplus<5>(rArrayL, aMul, aArrayR); return;}
    case 6: {mplus<6>(rArrayL, aMul, aArrayR); return;}
    case 7: {mplus<7>(rArrayL, aMul, aArrayR); return;}
    case 8: {mplus<8>(rArrayL, aMul, aArrayR); return;}
    case 9: {mplus<9>(rArrayL, aMul, aArrayR); return;}
    case 10: {mplus<10>(rArrayL, aMul, aArrayR); return;}
    case 11: {mplus<11>(rArrayL, aMul, aArrayR); return;}
    case 12: {mplus<12>(rArrayL, aMul, aArrayR); return;}
    case 13: {mplus<13>(rArrayL, aMul, aArrayR); return;}
    case 14: {mplus<14>(rArrayL, aMul, aArrayR); return;}
    case 15: {mplus<15>(rArrayL, aMul, aArrayR); return;}
    case 16: {mplus<16>(rArrayL, aMul, aArrayR); return;}
    case 17: {mplus<17>(rArrayL, aMul, aArrayR); return;}
    case 18: {mplus<18>(rArrayL, aMul, aArrayR); return;}
    case 19: {mplus<19>(rArrayL, aMul, aArrayR); return;}
    case 20: {mplus<20>(rArrayL, aMul, aArrayR); return;}
    case 21: {mplus<21>(rArrayL, aMul, aArrayR); return;}
    case 22: {mplus<22>(rArrayL, aMul, aArrayR); return;}
    case 23: {mplus<23>(rArrayL, aMul, aArrayR); return;}
    case 24: {mplus<24>(rArrayL, aMul, aArrayR); return;}
    case 25: {mplus<25>(rArrayL, aMul, aArrayR); return;}
    case 26: {mplus<26>(rArrayL, aMul, aArrayR); return;}
    case 27: {mplus<27>(rArrayL, aMul, aArrayR); return;}
    case 28: {mplus<28>(rArrayL, aMul, aArrayR); return;}
    case 29: {mplus<29>(rArrayL, aMul, aArrayR); return;}
    case 30: {mplus<30>(rArrayL, aMul, aArrayR); return;}
    case 31: {mplus<31>(rArrayL, aMul, aArrayR); return;}
    case 32: {mplus<32>(rArrayL, aMul, aArrayR); return;}
    case 33: {mplus<33>(rArrayL, aMul, aArrayR); return;}
    case 34: {mplus<34>(rArrayL, aMul, aArrayR); return;}
    case 35: {mplus<35>(rArrayL, aMul, aArrayR); return;}
    case 36: {mplus<36>(rArrayL, aMul, aArrayR); return;}
    case 37: {mplus<37>(rArrayL, aMul, aArrayR); return;}
    case 38: {mplus<38>(rArrayL, aMul, aArrayR); return;}
    case 39: {mplus<39>(rArrayL, aMul, aArrayR); return;}
    case 40: {mplus<40>(rArrayL, aMul, aArrayR); return;}
    case 41: {mplus<41>(rArrayL, aMul, aArrayR); return;}
    case 42: {mplus<42>(rArrayL, aMul, aArrayR); return;}
    case 43: {mplus<43>(rArrayL, aMul, aArrayR); return;}
    case 44: {mplus<44>(rArrayL, aMul, aArrayR); return;}
    case 45: {mplus<45>(rArrayL, aMul, aArrayR); return;}
    case 46: {mplus<46>(rArrayL, aMul, aArrayR); return;}
    case 47: {mplus<47>(rArrayL, aMul, aArrayR); return;}
    case 48: {mplus<48>(rArrayL, aMul, aArrayR); return;}
    case 49: {mplus<49>(rArrayL, aMul, aArrayR); return;}
    case 50: {mplus<50>(rArrayL, aMul, aArrayR); return;}
    case 51: {mplus<51>(rArrayL, aMul, aArrayR); return;}
    case 52: {mplus<52>(rArrayL, aMul, aArrayR); return;}
    case 53: {mplus<53>(rArrayL, aMul, aArrayR); return;}
    case 54: {mplus<54>(rArrayL, aMul, aArrayR); return;}
    case 55: {mplus<55>(rArrayL, aMul, aArrayR); return;}
    case 56: {mplus<56>(rArrayL, aMul, aArrayR); return;}
    case 57: {mplus<57>(rArrayL, aMul, aArrayR); return;}
    case 58: {mplus<58>(rArrayL, aMul, aArrayR); return;}
    case 59: {mplus<59>(rArrayL, aMul, aArrayR); return;}
    case 60: {mplus<60>(rArrayL, aMul, aArrayR); return;}
    case 61: {mplus<61>(rArrayL, aMul, aArrayR); return;}
    case 62: {mplus<62>(rArrayL, aMul, aArrayR); return;}
    case 63: {mplus<63>(rArrayL, aMul, aArrayR); return;}
    case 64: {mplus<64>(rArrayL, aMul, aArrayR); return;}
    case 65: {mplus<65>(rArrayL, aMul, aArrayR); return;}
    case 66: {mplus<66>(rArrayL, aMul, aArrayR); return;}
    case 67: {mplus<67>(rArrayL, aMul, aArrayR); return;}
    case 68: {mplus<68>(rArrayL, aMul, aArrayR); return;}
    case 69: {mplus<69>(rArrayL, aMul, aArrayR); return;}
    case 70: {mplus<70>(rArrayL, aMul, aArrayR); return;}
    case 71: {mplus<71>(rArrayL, aMul, aArrayR); return;}
    case 72: {mplus<72>(rArrayL, aMul, aArrayR); return;}
    case 73: {mplus<73>(rArrayL, aMul, aArrayR); return;}
    case 74: {mplus<74>(rArrayL, aMul, aArrayR); return;}
    case 75: {mplus<75>(rArrayL, aMul, aArrayR); return;}
    case 76: {mplus<76>(rArrayL, aMul, aArrayR); return;}
    case 77: {mplus<77>(rArrayL, aMul, aArrayR); return;}
    case 78: {mplus<78>(rArrayL, aMul, aArrayR); return;}
    case 79: {mplus<79>(rArrayL, aMul, aArrayR); return;}
    case 80: {mplus<80>(rArrayL, aMul, aArrayR); return;}
    case 81: {mplus<81>(rArrayL, aMul, aArrayR); return;}
    case 82: {mplus<82>(rArrayL, aMul, aArrayR); return;}
    case 83: {mplus<83>(rArrayL, aMul, aArrayR); return;}
    case 84: {mplus<84>(rArrayL, aMul, aArrayR); return;}
    case 85: {mplus<85>(rArrayL, aMul, aArrayR); return;}
    case 86: {mplus<86>(rArrayL, aMul, aArrayR); return;}
    case 87: {mplus<87>(rArrayL, aMul, aArrayR); return;}
    case 88: {mplus<88>(rArrayL, aMul, aArrayR); return;}
    case 89: {mplus<89>(rArrayL, aMul, aArrayR); return;}
    case 90: {mplus<90>(rArrayL, aMul, aArrayR); return;}
    case 91: {mplus<91>(rArrayL, aMul, aArrayR); return;}
    case 92: {mplus<92>(rArrayL, aMul, aArrayR); return;}
    case 93: {mplus<93>(rArrayL, aMul, aArrayR); return;}
    case 94: {mplus<94>(rArrayL, aMul, aArrayR); return;}
    case 95: {mplus<95>(rArrayL, aMul, aArrayR); return;}
    case 96: {mplus<96>(rArrayL, aMul, aArrayR); return;}
    case 97: {mplus<97>(rArrayL, aMul, aArrayR); return;}
    case 98: {mplus<98>(rArrayL, aMul, aArrayR); return;}
    case 99: {mplus<99>(rArrayL, aMul, aArrayR); return;}
    case 100: {mplus<100>(rArrayL, aMul, aArrayR); return;}
    case 101: {mplus<101>(rArrayL, aMul, aArrayR); return;}
    case 102: {mplus<102>(rArrayL, aMul, aArrayR); return;}
    case 103: {mplus<103>(rArrayL, aMul, aArrayR); return;}
    case 104: {mplus<104>(rArrayL, aMul, aArrayR); return;}
    case 105: {mplus<105>(rArrayL, aMul, aArrayR); return;}
    case 106: {mplus<106>(rArrayL, aMul, aArrayR); return;}
    case 107: {mplus<107>(rArrayL, aMul, aArrayR); return;}
    case 108: {mplus<108>(rArrayL, aMul, aArrayR); return;}
    case 109: {mplus<109>(rArrayL, aMul, aArrayR); return;}
    case 110: {mplus<110>(rArrayL, aMul, aArrayR); return;}
    case 111: {mplus<111>(rArrayL, aMul, aArrayR); return;}
    case 112: {mplus<112>(rArrayL, aMul, aArrayR); return;}
    case 113: {mplus<113>(rArrayL, aMul, aArrayR); return;}
    case 114: {mplus<114>(rArrayL, aMul, aArrayR); return;}
    case 115: {mplus<115>(rArrayL, aMul, aArrayR); return;}
    case 116: {mplus<116>(rArrayL, aMul, aArrayR); return;}
    case 117: {mplus<117>(rArrayL, aMul, aArrayR); return;}
    case 118: {mplus<118>(rArrayL, aMul, aArrayR); return;}
    case 119: {mplus<119>(rArrayL, aMul, aArrayR); return;}
    case 120: {mplus<120>(rArrayL, aMul, aArrayR); return;}
    case 121: {mplus<121>(rArrayL, aMul, aArrayR); return;}
    case 122: {mplus<122>(rArrayL, aMul, aArrayR); return;}
    case 123: {mplus<123>(rArrayL, aMul, aArrayR); return;}
    case 124: {mplus<124>(rArrayL, aMul, aArrayR); return;}
    case 125: {mplus<125>(rArrayL, aMul, aArrayR); return;}
    case 126: {mplus<126>(rArrayL, aMul, aArrayR); return;}
    case 127: {mplus<127>(rArrayL, aMul, aArrayR); return;}
    case 128: {mplus<128>(rArrayL, aMul, aArrayR); return;}
    default: {break;}
    }
    for (jint i = 0; i < aLen; ++i) {
        rArrayL[i] += aMul*aArrayR[i];
    }
}
template <jint N>
static inline void mplus2(jdouble *rArrayL1, jdouble *rArrayL2, jdouble aMul1, jdouble aMul2, jdouble *aArrayR) noexcept {
    for (jint i = 0; i < N; ++i) {
        const jdouble tRHS = aArrayR[i];
        rArrayL1[i] += aMul1*tRHS;
        rArrayL2[i] += aMul2*tRHS;
    }
}
static inline void mplus2(jdouble *rArrayL1, jdouble *rArrayL2, jdouble aMul1, jdouble aMul2, jdouble *aArrayR, jint aLen) noexcept {
    switch (aLen) {
    case 0: {return;}
    case 1: {mplus2<1>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 2: {mplus2<2>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 3: {mplus2<3>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 4: {mplus2<4>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 5: {mplus2<5>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 6: {mplus2<6>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 7: {mplus2<7>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 8: {mplus2<8>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 9: {mplus2<9>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 10: {mplus2<10>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 11: {mplus2<11>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 12: {mplus2<12>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 13: {mplus2<13>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 14: {mplus2<14>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 15: {mplus2<15>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 16: {mplus2<16>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 17: {mplus2<17>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 18: {mplus2<18>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 19: {mplus2<19>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 20: {mplus2<20>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 21: {mplus2<21>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 22: {mplus2<22>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 23: {mplus2<23>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 24: {mplus2<24>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 25: {mplus2<25>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 26: {mplus2<26>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 27: {mplus2<27>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 28: {mplus2<28>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 29: {mplus2<29>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 30: {mplus2<30>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 31: {mplus2<31>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 32: {mplus2<32>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 33: {mplus2<33>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 34: {mplus2<34>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 35: {mplus2<35>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 36: {mplus2<36>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 37: {mplus2<37>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 38: {mplus2<38>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 39: {mplus2<39>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 40: {mplus2<40>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 41: {mplus2<41>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 42: {mplus2<42>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 43: {mplus2<43>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 44: {mplus2<44>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 45: {mplus2<45>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 46: {mplus2<46>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 47: {mplus2<47>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 48: {mplus2<48>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 49: {mplus2<49>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 50: {mplus2<50>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 51: {mplus2<51>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 52: {mplus2<52>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 53: {mplus2<53>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 54: {mplus2<54>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 55: {mplus2<55>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 56: {mplus2<56>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 57: {mplus2<57>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 58: {mplus2<58>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 59: {mplus2<59>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 60: {mplus2<60>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 61: {mplus2<61>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 62: {mplus2<62>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 63: {mplus2<63>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 64: {mplus2<64>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 65: {mplus2<65>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 66: {mplus2<66>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 67: {mplus2<67>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 68: {mplus2<68>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 69: {mplus2<69>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 70: {mplus2<70>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 71: {mplus2<71>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 72: {mplus2<72>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 73: {mplus2<73>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 74: {mplus2<74>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 75: {mplus2<75>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 76: {mplus2<76>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 77: {mplus2<77>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 78: {mplus2<78>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 79: {mplus2<79>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 80: {mplus2<80>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 81: {mplus2<81>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 82: {mplus2<82>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 83: {mplus2<83>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 84: {mplus2<84>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 85: {mplus2<85>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 86: {mplus2<86>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 87: {mplus2<87>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 88: {mplus2<88>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 89: {mplus2<89>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 90: {mplus2<90>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 91: {mplus2<91>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 92: {mplus2<92>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 93: {mplus2<93>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 94: {mplus2<94>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 95: {mplus2<95>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 96: {mplus2<96>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 97: {mplus2<97>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 98: {mplus2<98>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 99: {mplus2<99>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 100: {mplus2<100>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 101: {mplus2<101>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 102: {mplus2<102>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 103: {mplus2<103>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 104: {mplus2<104>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 105: {mplus2<105>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 106: {mplus2<106>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 107: {mplus2<107>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 108: {mplus2<108>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 109: {mplus2<109>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 110: {mplus2<110>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 111: {mplus2<111>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 112: {mplus2<112>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 113: {mplus2<113>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 114: {mplus2<114>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 115: {mplus2<115>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 116: {mplus2<116>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 117: {mplus2<117>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 118: {mplus2<118>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 119: {mplus2<119>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 120: {mplus2<120>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 121: {mplus2<121>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 122: {mplus2<122>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 123: {mplus2<123>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 124: {mplus2<124>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 125: {mplus2<125>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 126: {mplus2<126>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 127: {mplus2<127>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    case 128: {mplus2<128>(rArrayL1, rArrayL2, aMul1, aMul2, aArrayR); return;}
    default: {break;}
    }
    for (jint i = 0; i < aLen; ++i) {
        const jdouble tRHS = aArrayR[i];
        rArrayL1[i] += aMul1*tRHS;
        rArrayL2[i] += aMul2*tRHS;
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

static inline jdouble calFc(jdouble aDis, jdouble aRCut) noexcept {
    return pow4(1.0 - pow2(aDis/aRCut));
}
template <jint N>
static inline void calRn(jdouble *rRn, jdouble aDis, jdouble aRCut) noexcept {
    jdouble tRnX = 1.0 - 2.0*aDis/aRCut;
    chebyshevFull<N>(tRnX, rRn);
}
static inline void calRn(jdouble *rRn, jint aNMax, jdouble aDis, jdouble aRCut) noexcept {
    switch (aNMax) {
    case 0: {calRn<0>(rRn, aDis, aRCut); return;}
    case 1: {calRn<1>(rRn, aDis, aRCut); return;}
    case 2: {calRn<2>(rRn, aDis, aRCut); return;}
    case 3: {calRn<3>(rRn, aDis, aRCut); return;}
    case 4: {calRn<4>(rRn, aDis, aRCut); return;}
    case 5: {calRn<5>(rRn, aDis, aRCut); return;}
    case 6: {calRn<6>(rRn, aDis, aRCut); return;}
    case 7: {calRn<7>(rRn, aDis, aRCut); return;}
    case 8: {calRn<8>(rRn, aDis, aRCut); return;}
    case 9: {calRn<9>(rRn, aDis, aRCut); return;}
    case 10: {calRn<10>(rRn, aDis, aRCut); return;}
    case 11: {calRn<11>(rRn, aDis, aRCut); return;}
    case 12: {calRn<12>(rRn, aDis, aRCut); return;}
    case 13: {calRn<13>(rRn, aDis, aRCut); return;}
    case 14: {calRn<14>(rRn, aDis, aRCut); return;}
    case 15: {calRn<15>(rRn, aDis, aRCut); return;}
    case 16: {calRn<16>(rRn, aDis, aRCut); return;}
    case 17: {calRn<17>(rRn, aDis, aRCut); return;}
    case 18: {calRn<18>(rRn, aDis, aRCut); return;}
    case 19: {calRn<19>(rRn, aDis, aRCut); return;}
    case 20: {calRn<20>(rRn, aDis, aRCut); return;}
    default: {return;}
    }
}

static inline void calFcPxyz(jdouble *rFcPx, jdouble *rFcPy, jdouble *rFcPz,
                             jdouble aDis, jdouble aRCut, jdouble aDx, jdouble aDy, jdouble aDz) noexcept {
    jdouble fcMul = 1.0 - pow2(aDis/aRCut);
    jdouble fcPMul = 8.0 * pow3(fcMul) / (aRCut*aRCut);
    *rFcPx = aDx * fcPMul;
    *rFcPy = aDy * fcPMul;
    *rFcPz = aDz * fcPMul;
}
template <jint N>
static inline void calRnPxyz(jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2,
                             jdouble aDis, jdouble aRCut, jdouble aDx, jdouble aDy, jdouble aDz) noexcept {
    const jdouble tRnX = 1.0 - 2.0*aDis/aRCut;
    chebyshev2Full<N-1>(tRnX, rCheby2);
    const jdouble tRnPMul = 2.0 / (aDis*aRCut);
    rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
    for (jint n = 1; n <= N; ++n) {
        const jdouble tRnP = n*tRnPMul*rCheby2[n-1];
        rRnPx[n] = tRnP*aDx;
        rRnPy[n] = tRnP*aDy;
        rRnPz[n] = tRnP*aDz;
    }
}
static inline void calRnPxyz(jdouble *rRnPx, jdouble *rRnPy, jdouble *rRnPz, jdouble *rCheby2, jint aNMax,
                             jdouble aDis, jdouble aRCut, jdouble aDx, jdouble aDy, jdouble aDz) noexcept {
    switch (aNMax) {
    case 0: {calRnPxyz<0>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 1: {calRnPxyz<1>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 2: {calRnPxyz<2>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 3: {calRnPxyz<3>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 4: {calRnPxyz<4>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 5: {calRnPxyz<5>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 6: {calRnPxyz<6>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 7: {calRnPxyz<7>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 8: {calRnPxyz<8>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 9: {calRnPxyz<9>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 10: {calRnPxyz<10>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 11: {calRnPxyz<11>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 12: {calRnPxyz<12>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 13: {calRnPxyz<13>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 14: {calRnPxyz<14>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 15: {calRnPxyz<15>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 16: {calRnPxyz<16>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 17: {calRnPxyz<17>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 18: {calRnPxyz<18>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 19: {calRnPxyz<19>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    case 20: {calRnPxyz<20>(rRnPx, rRnPy, rRnPz, rCheby2, aDis, aRCut, aDx, aDy, aDz); return;}
    default: {return;}
    }
}

}

#endif //NNAP_UTIL_H
