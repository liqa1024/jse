package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.MathEX;
import jse.math.vector.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.math.MathEX.SQRT2_INV;

/**
 * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
 * 这里提供一个简单的纯 java 实现，用于测试或者高兼容性的方案
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * Reference:
 * <a href="https://link.springer.com/article/10.1007/s40843-024-2953-9">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * @author Su Rui, liqa
 */
public class SimpleSphericalChebyshev extends Basis {
    final int mTypeNum;
    final int mNMax;
    final int mSizeN, mSizeWt;
    
    final String @Nullable[] mSymbols;
    final int mLMax, mL3Max;
    final double mRCut, mRCutS;
    final boolean mFCutS;
    
    final int mSizeL;
    final int mLMaxMax, mLMAll;
    final int mSize;
    
    final Vector mRFuncScale, mRFuncShift;
    final Vector mSystemScale;
    final boolean[] mPolyScale, mSphScale;
    public SimpleSphericalChebyshev setRFuncScale(double... aRFuncScale) {
        mRFuncScale.fill(aRFuncScale);
        mSphScale[0] = true;
        return this;
    }
    public SimpleSphericalChebyshev setRFuncShift(double... aRFuncShift) {
        mRFuncShift.fill(aRFuncShift);
        mSphScale[0] = true;
        return this;
    }
    public SimpleSphericalChebyshev setSystemScale(double... aSystemScale) {
        mSystemScale.fill(aSystemScale);
        mSphScale[0] = true;
        return this;
    }
    public SimpleSphericalChebyshev setPolyScale(boolean aFlag) {
        mPolyScale[0] = aFlag;
        mSphScale[0] = true;
        return this;
    }
    
    private SimpleSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aLMaxMax, int aL3Max, double aRCut, boolean aFCutS, double aRCutS,
                                     @Nullable Vector aRFuncScale, @Nullable Vector aRFuncShift, @Nullable Vector aSystemScale, boolean @Nullable[] aPolyScale, boolean @Nullable[] aSphScale) {
        if (aLMaxMax<0 || aLMaxMax>12) throw new IllegalArgumentException("Input lmax MUST be in [0, 12], input: "+aLMaxMax);
        if (aL3Max<0 || aL3Max>4) throw new IllegalArgumentException("Input l3max MUST be in [0, 4], input: "+aL3Max);
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        int tSizeN = aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        mSizeN = aFCutS ? (tSizeN+tSizeN) : tSizeN;
        mSizeWt = aTypeNum>1 ? 1 : 0;
        
        mSymbols = aSymbols;
        mLMax = aLMax;
        mL3Max = aL3Max;
        mRCut = aRCut;
        mFCutS = aFCutS;
        mRCutS = aRCutS;
        if (mRCutS > mRCut) throw new IllegalArgumentException("rcut_s cannot be greater than rcut");
        
        mSizeL = (mLMax+1) + SphericalChebyshev.L3NCOLS[mL3Max];
        mLMaxMax = aLMaxMax;
        if (mLMaxMax!=Math.max(mLMax, mL3Max)) throw new IllegalStateException();
        mLMAll = (mLMaxMax+1)*(mLMaxMax+1);
        
        mSize = mSizeN*mSizeL;
        
        mRFuncScale = aRFuncScale==null ? Vector.ones(mNMax+1) : aRFuncScale;
        mRFuncShift = aRFuncShift==null ? Vector.zeros(mNMax+1) : aRFuncShift;
        mSystemScale = aSystemScale==null ? Vector.ones(mSizeN*(mLMaxMax+1)) : aSystemScale;
        mPolyScale = aPolyScale==null ? new boolean[]{false} : aPolyScale;
        mSphScale = aSphScale==null ? new boolean[]{aRFuncScale!=null || aRFuncShift!=null || aSystemScale!=null || aPolyScale!=null} : aSphScale;
        
        if (mRFuncScale.size()!=mNMax+1) throw new IllegalArgumentException("Size of rfunc scale mismatch");
        if (mRFuncShift.size()!=mNMax+1) throw new IllegalArgumentException("Size of rfunc shift mismatch");
        if (mSystemScale.size()!=mSizeN*(mLMaxMax+1)) throw new IllegalArgumentException("Size of system scale mismatch");
        if (mPolyScale.length!=1) throw new IllegalArgumentException("Size of poly scale mismatch");
        if (mSphScale.length!=1) throw new IllegalArgumentException("Size of spherical scale mismatch");
    }
    SimpleSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aL3Max, double aRCut,  boolean aFCutS, double aRCutS,Vector aRFuncScale, Vector aRFuncShift, Vector aSystemScale, boolean[] aPolyScale, boolean[] aSphScale) {
        this(aSymbols, aTypeNum, aNMax, aLMax, Math.max(aLMax, aL3Max), aL3Max, aRCut, aFCutS, aRCutS, aRFuncScale, aRFuncShift, aSystemScale, aPolyScale, aSphScale);
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleSphericalChebyshev(String @NotNull[] aSymbols, int aNMax, int aLMax, double aRCut) {
        this(aSymbols, aSymbols.length, aNMax, aLMax, SphericalChebyshev.DEFAULT_L3MAX, aRCut, false, 0.0, null, null, null, null, null);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleSphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
        this(null, aTypeNum, aNMax, aLMax, SphericalChebyshev.DEFAULT_L3MAX, aRCut, false, 0.0, null, null, null, null, null);
    }
    
    @Override public SimpleSphericalChebyshev threadSafeRef() {
        return new SimpleSphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mL3Max, mRCut, mFCutS, mRCutS, mRFuncScale, mRFuncShift, mSystemScale, mPolyScale, mSphScale);
    }
    
    @SuppressWarnings("rawtypes")
    @Override public void save(Map rSaveTo) {
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SimpleSphericalChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales", "rfunc_sigma");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tRFuncShift = (List<? extends Number>)UT.Code.get(aMap, "rfunc_shifts", "rfunc_mu");
        Vector aRFuncShift = tRFuncShift==null ? null : Vectors.from(tRFuncShift);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        Object tPolyScale = UT.Code.get(aMap, "poly_scale");
        boolean[] aPolyScale = tPolyScale==null ? null : new boolean[]{(Boolean)tPolyScale};
        return new SimpleSphericalChebyshev(
            aSymbols, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L3MAX, "l3max")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            ((Boolean)UT.Code.getWithDefault(aMap, false, "fcut_s")),
            ((Number)UT.Code.getWithDefault(aMap, 0.0, "rcut_s")).doubleValue(),
            aRFuncScales, aRFuncShift, aSystemScale, aPolyScale, null
        );
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SimpleSphericalChebyshev load(int aTypeNum, Map aMap) {
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales", "rfunc_sigma");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tRFuncShift = (List<? extends Number>)UT.Code.get(aMap, "rfunc_shifts", "rfunc_mu");
        Vector aRFuncShift = tRFuncShift==null ? null : Vectors.from(tRFuncShift);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        Object tPolyScale = UT.Code.get(aMap, "poly_scale");
        boolean[] aPolyScale = tPolyScale==null ? null : new boolean[]{(Boolean)tPolyScale};
        return new SimpleSphericalChebyshev(
            null, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_L3MAX, "l3max")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            ((Boolean)UT.Code.getWithDefault(aMap, false, "fcut_s")),
            ((Number)UT.Code.getWithDefault(aMap, 0.0, "rcut_s")).doubleValue(),
            aRFuncScales, aRFuncShift, aSystemScale, aPolyScale, null
        );
    }
    
    /** @return {@inheritDoc} */
    @Override public double rcut() {return mRCut;}
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)(lmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)(lmax+1)}
     */
    @Override public int size() {return mSize;}
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return mTypeNum;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    /**
     * {@inheritDoc}
     * @param aType
     * @return {@inheritDoc}
     */
    @Override public @Nullable String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    
    @Override
    public void forward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rForwardCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        if (rFp.size() != size()) throw new IllegalArgumentException("data size mismatch");
        // init cache
        if (aFullCache) throw new UnsupportedOperationException("full cache in simple basis");
        validCache_(rForwardCache, mNMax+1 + mLMAll + mSizeN*mLMAll);
        Vector rCnlm = new Vector(mSizeN*mLMAll, rForwardCache.internalData());
        ShiftVector rForwardCacheElse = new ShiftVector(mNMax+1+mLMAll, mSizeN*mLMAll, rForwardCache.internalData());
        // clear cnlm first
        rCnlm.fill(0.0);
        // do cal
        calCnlm(aNlDx, aNlDy, aNlDz, aNlType, rCnlm, rForwardCacheElse);
        // system scale here
        for (int n=0, tShift=0, tShiftS=0; n<mSizeN; ++n, tShift+=mLMAll, tShiftS+=mLMaxMax+1) {
            scaleCnlm_(rCnlm.subVec(tShift, tShift+mLMAll), mSystemScale.subVec(tShiftS, tShiftS+mLMaxMax+1));
        }
        // cnlm -> Pnl
        int tSizeL2 = mLMax+1;
        int tSizeL3 = SphericalChebyshev.L3NCOLS[mL3Max];
        for (int n=0, tShift=0, tShiftFp=0; n<mSizeN; ++n, tShift+=mLMAll, tShiftFp+=mSizeL) {
            calL2_(rCnlm.subVec(tShift, tShift+mLMAll), rFp.subVec(tShiftFp, tShiftFp+tSizeL2));
            calL3_(rCnlm.subVec(tShift, tShift+mLMAll), rFp.subVec(tShiftFp+tSizeL2, tShiftFp+tSizeL2+tSizeL3));
        }
    }
    public Vector calSystemScale(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType) {
        DoubleList tCache = new DoubleList(16);
        validCache_(tCache, mNMax+1 + mLMAll + mSizeN*mLMAll);
        Vector rCnlm = new Vector(mSizeN*mLMAll, tCache.internalData());
        ShiftVector tCacheElse = new ShiftVector(mNMax+1+mLMAll, mSizeN*mLMAll, tCache.internalData());
        rCnlm.fill(0.0);
        calCnlm(aNlDx, aNlDy, aNlDz, aNlType, rCnlm, tCacheElse);
        Vector tScale = Vector.zeros(mSizeN*(mLMaxMax+1));
        for (int n=0, tShift=0, tShiftS=0; n<mSizeN; ++n, tShift+=mLMAll, tShiftS+=mLMaxMax+1) {
            calScale_(rCnlm.subVec(tShift, tShift+mLMAll), tScale.subVec(tShiftS, tShiftS+mLMaxMax+1));
        }
        return tScale;
    }
    void calCnlm(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rCnlm, DoubleArrayVector rForwardCache) {
        int tSizeBnlm = (mNMax+1)*mLMAll;
        // init cache
        ShiftVector rRn = new ShiftVector(mNMax+1, rForwardCache.internalDataShift(), rForwardCache.internalData());
        ShiftVector rY = new ShiftVector(mLMAll, rForwardCache.internalDataShift()+mNMax+1, rForwardCache.internalData());
        // loop for neighbor
        final int tNN = aNlDx.size();
        for (int j = 0; j < tNN; ++j) {
            int type = aNlType.get(j);
            double dx = aNlDx.get(j), dy = aNlDy.get(j), dz = aNlDz.get(j);
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            // check rcut for merge
            if (dis >= mRCut) continue;
            boolean tFCutS = mFCutS && dis<mRCutS;
            // cal fc
            double fc = SimpleChebyshev.calFc(dis, mRCut);
            double fcS = tFCutS ? SimpleChebyshev.calFc(dis, mRCutS) : 0.0;
            // cal Rn
            SimpleChebyshev.calRn(rRn, mNMax, dis, mRCut);
            // scale rfunc here
            rRn.minus2this(mRFuncShift);
            rRn.multiply2this(mRFuncScale);
            // cal Y
            MathEX.Func.realSphericalHarmonicsFull2DestXYZDis_(mLMaxMax, dx, dy, dz, dis, rY);
            // scale sph here
            if (mSphScale[0]) rY.multiply2this(SQRT_PI4);
            // cal cnlm with rfunc scale
            if (mTypeNum==1) {
                int ck = 0;
                for (int n = 0; n <= mNMax; ++n) {
                    double tMul = fc*rRn.get(n);
                    for (int k = 0; k < mLMAll; ++k) {
                        rCnlm.add(ck, tMul*rY.get(k));
                        ++ck;
                    }
                }
                if (tFCutS) for (int n = 0; n <= mNMax; ++n) {
                    double tMul = fcS*rRn.get(n);
                    for (int k = 0; k < mLMAll; ++k) {
                        rCnlm.add(ck, tMul*rY.get(k));
                        ++ck;
                    }
                }
            } else {
                double wt = ((type&1)==1) ? type : -type;
                int ck = 0, ckwt = tSizeBnlm;
                for (int n = 0; n <= mNMax; ++n) {
                    double tMul = fc*rRn.get(n);
                    for (int k = 0; k < mLMAll; ++k) {
                        double tValue = tMul*rY.get(k);
                        rCnlm.add(ck, tValue);
                        rCnlm.add(ckwt, wt*tValue);
                        ++ck; ++ckwt;
                    }
                }
                ck = ckwt; ckwt += tSizeBnlm;
                if (tFCutS) for (int n = 0; n <= mNMax; ++n) {
                    double tMul = fcS*rRn.get(n);
                    for (int k = 0; k < mLMAll; ++k) {
                        double tValue = tMul*rY.get(k);
                        rCnlm.add(ck, tValue);
                        rCnlm.add(ckwt, wt*tValue);
                        ++ck; ++ckwt;
                    }
                }
            }
        }
    }
    void scaleCnlm_(IVector rCnlm, IVector aScale) {
        int tShift = 0;
        for (int l = 0; l <= mLMaxMax; ++l) {
            int tLen = l+l+1;
            rCnlm.subVec(tShift, tShift+tLen).multiply2this(aScale.get(l));
            tShift += tLen;
        }
    }
    void calScale_(DoubleArrayVector aCnlm, IVector rScale) {
        int tShift = 0;
        for (int l = 0; l <= mLMaxMax; ++l) {
            int tLen = l+l+1;
            double rDot = aCnlm.subVec(tShift, tShift+tLen).operation().dot();
            rDot = MathEX.Fast.sqrt(rDot/(double)tLen);
            rScale.set(l, rDot);
            tShift += tLen;
        }
    }
    void calL2_(DoubleArrayVector aCnlm, IVector rFp) {
        int tShift = 0;
        for (int l = 0; l <= mLMax; ++l) {
            int tLen = l+l+1;
            double rDot = aCnlm.subVec(tShift, tShift+tLen).operation().dot();
            rDot /= (double)tLen;
            if (!mSphScale[0]) rDot *= MathEX.PI*4; // compat
            if (mPolyScale[0]) rDot *= MathEX.SQRT2_INV;
            rFp.set(l, rDot);
            tShift += tLen;
        }
    }
    void calL3_(DoubleArrayVector aCnlm, IVector rFp) {
        if (mL3Max <= 1) return;
        /// l1 = l2 = l3 = 2
        double c20  = aCnlm.get((2*2+2)  );
        double c21  = aCnlm.get((2*2+2)+1);
        double c2n1 = aCnlm.get((2*2+2)-1);
        double c22  = aCnlm.get((2*2+2)+2);
        double c2n2 = aCnlm.get((2*2+2)-2);
        double rFp3 = 0.0;
        rFp3 += WIGNER_222_000 * c20*c20*c20;
        rFp3 -= (3.0*WIGNER_222_011) * c20 * (c21*c21 + c2n1*c2n1);
        rFp3 += (3.0*WIGNER_222_022) * c20 * (c22*c22 + c2n2*c2n2);
        rFp3 += (3.0*SQRT2_INV*WIGNER_222_112) * c22 * (c21*c21 - c2n1*c2n1);
        rFp3 += (6.0*SQRT2_INV*WIGNER_222_112) * c21*c2n1*c2n2;
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(0, rFp3);
        /// l1 = l2 = 1, l3 = 2
        double c10  = aCnlm.get((1+1)  );
        double c11  = aCnlm.get((1+1)+1);
        double c1n1 = aCnlm.get((1+1)-1);
        rFp3 = 0.0;
        rFp3 += WIGNER_112_000 * c10*c10*c20;
        rFp3 -= WIGNER_112_110 * c20 * (c11*c11 + c1n1*c1n1);
        rFp3 -= (2.0*WIGNER_112_011) * c10 * (c11*c21 + c1n1*c2n1);
        rFp3 += (SQRT2_INV*WIGNER_112_112) * c22 * (c11*c11 - c1n1*c1n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_112_112) * c11*c1n1*c2n2;
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(1, rFp3);
        if (mL3Max == 2) return;
        /// l1 = 2, l2 = l3 = 3
        double c30  = aCnlm.get((3*3+3)  );
        double c31  = aCnlm.get((3*3+3)+1);
        double c3n1 = aCnlm.get((3*3+3)-1);
        double c32  = aCnlm.get((3*3+3)+2);
        double c3n2 = aCnlm.get((3*3+3)-2);
        double c33  = aCnlm.get((3*3+3)+3);
        double c3n3 = aCnlm.get((3*3+3)-3);
        rFp3 = 0.0;
        rFp3 += WIGNER_233_000 * c20*c30*c30;
        rFp3 -= WIGNER_233_011 * c20 * (c31*c31 + c3n1*c3n1);
        rFp3 += WIGNER_233_022 * c20 * (c32*c32 + c3n2*c3n2);
        rFp3 -= WIGNER_233_033 * c20 * (c33*c33 + c3n3*c3n3);
        rFp3 -= (2.0*WIGNER_233_110) * c30 * (c21*c31 + c2n1*c3n1);
        rFp3 += (2.0*WIGNER_233_220) * c30 * (c22*c32 + c2n2*c3n2);
        rFp3 += (SQRT2_INV*WIGNER_233_211) * c22 * (c31*c31 - c3n1*c3n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_233_211) * c2n2*c31*c3n1;
        rFp3 += (2.0*SQRT2_INV*WIGNER_233_112) * c21 * (c31*c32 + c3n1*c3n2);
        rFp3 += (2.0*SQRT2_INV*WIGNER_233_112) * c2n1 * (c31*c3n2 - c3n1*c32);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_233_123) * c21 * (c32*c33 + c3n2*c3n3);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_233_123) * c2n1 * (c32*c3n3 - c3n2*c33);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_233_213) * c22 * (c31*c33 + c3n1*c3n3);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_233_213) * c2n2 * (c31*c3n3 - c3n1*c33);
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(2, rFp3);
        /// l1 = 1, l2 = 2, l3 = 3
        rFp3 = 0.0;
        rFp3 += WIGNER_123_000 * c10*c20*c30;
        rFp3 -= WIGNER_123_011 * c10 * (c21*c31 + c2n1*c3n1);
        rFp3 += WIGNER_123_022 * c10 * (c22*c32 + c2n2*c3n2);
        rFp3 -= WIGNER_123_101 * c20 * (c11*c31 + c1n1*c3n1);
        rFp3 -= WIGNER_123_110 * c30 * (c11*c21 + c1n1*c2n1);
        rFp3 += (SQRT2_INV*WIGNER_123_112) * c11 * (c21*c32 + c2n1*c3n2);
        rFp3 += (SQRT2_INV*WIGNER_123_112) * c1n1 * (c21*c3n2 - c2n1*c32);
        rFp3 += (SQRT2_INV*WIGNER_123_121) * c11 * (c22*c31 + c2n2*c3n1);
        rFp3 += (SQRT2_INV*WIGNER_123_121) * c1n1 * (c2n2*c31 - c22*c3n1);
        rFp3 -= (SQRT2_INV*WIGNER_123_123) * c11 * (c22*c33 + c2n2*c3n3);
        rFp3 -= (SQRT2_INV*WIGNER_123_123) * c1n1 * (c22*c3n3 - c2n2*c33);
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(3, rFp3);
        if (mL3Max == 3) return;
        /// l1 = l2 = l3 = 4
        double c40  = aCnlm.get((4*4+4)  );
        double c41  = aCnlm.get((4*4+4)+1);
        double c4n1 = aCnlm.get((4*4+4)-1);
        double c42  = aCnlm.get((4*4+4)+2);
        double c4n2 = aCnlm.get((4*4+4)-2);
        double c43  = aCnlm.get((4*4+4)+3);
        double c4n3 = aCnlm.get((4*4+4)-3);
        double c44  = aCnlm.get((4*4+4)+4);
        double c4n4 = aCnlm.get((4*4+4)-4);
        rFp3 = 0.0;
        rFp3 += WIGNER_444_000 * c40*c40*c40;
        rFp3 -= (3.0*WIGNER_444_011) * c40 * (c41*c41 + c4n1*c4n1);
        rFp3 += (3.0*WIGNER_444_022) * c40 * (c42*c42 + c4n2*c4n2);
        rFp3 -= (3.0*WIGNER_444_033) * c40 * (c43*c43 + c4n3*c4n3);
        rFp3 += (3.0*WIGNER_444_044) * c40 * (c44*c44 + c4n4*c4n4);
        rFp3 += (3.0*SQRT2_INV*WIGNER_444_112) * c42 * (c41*c41 - c4n1*c4n1);
        rFp3 += (6.0*SQRT2_INV*WIGNER_444_112) * c41*c4n1*c4n2;
        rFp3 += (3.0*SQRT2_INV*WIGNER_444_224) * c44 * (c42*c42 - c4n2*c4n2);
        rFp3 += (6.0*SQRT2_INV*WIGNER_444_224) * c42*c4n2*c4n4;
        rFp3 -= (6.0*SQRT2_INV*WIGNER_444_123) * c41 * (c42*c43 + c4n2*c4n3);
        rFp3 -= (6.0*SQRT2_INV*WIGNER_444_123) * c4n1 * (c42*c4n3 - c4n2*c43);
        rFp3 += (6.0*SQRT2_INV*WIGNER_444_134) * c41 * (c43*c44 + c4n3*c4n4);
        rFp3 += (6.0*SQRT2_INV*WIGNER_444_134) * c4n1 * (c43*c4n4 - c4n3*c44);
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(4, rFp3);
        /// l1 = l2 = 2, l3 = 4
        rFp3 = 0.0;
        rFp3 += WIGNER_224_000 * c20*c20*c40;
        rFp3 -= WIGNER_224_110 * c40 * (c21*c21 + c2n1*c2n1);
        rFp3 += WIGNER_224_220 * c40 * (c22*c22 + c2n2*c2n2);
        rFp3 -= (2.0*WIGNER_224_011) * c20 * (c21*c41 + c2n1*c4n1);
        rFp3 += (2.0*WIGNER_224_022) * c20 * (c22*c42 + c2n2*c4n2);
        rFp3 += (SQRT2_INV*WIGNER_224_112) * c42 * (c21*c21 - c2n1*c2n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_224_112) * c21*c2n1*c4n2;
        rFp3 += (SQRT2_INV*WIGNER_224_224) * c44 * (c22*c22 - c2n2*c2n2);
        rFp3 += (2.0*SQRT2_INV*WIGNER_224_224) * c22*c2n2*c4n4;
        rFp3 += (2.0*SQRT2_INV*WIGNER_224_121) * c21 * (c22*c41 + c2n2*c4n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_224_121) * c2n1 * (c2n2*c41 - c22*c4n1);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_224_123) * c21 * (c22*c43 + c2n2*c4n3);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_224_123) * c2n1 * (c22*c4n3 - c2n2*c43);
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(5, rFp3);
        /// l1 = l2 = 3, l3 = 4
        rFp3 = 0.0;
        rFp3 += WIGNER_334_000 * c30*c30*c40;
        rFp3 -= WIGNER_334_110 * c40 * (c31*c31 + c3n1*c3n1);
        rFp3 += WIGNER_334_220 * c40 * (c32*c32 + c3n2*c3n2);
        rFp3 -= WIGNER_334_330 * c40 * (c33*c33 + c3n3*c3n3);
        rFp3 -= (2.0*WIGNER_334_011) * c30 * (c31*c41 + c3n1*c4n1);
        rFp3 += (2.0*WIGNER_334_022) * c30 * (c32*c42 + c3n2*c4n2);
        rFp3 -= (2.0*WIGNER_334_033) * c30 * (c33*c43 + c3n3*c4n3);
        rFp3 += (SQRT2_INV*WIGNER_334_112) * c42 * (c31*c31 - c3n1*c3n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_334_112) * c31*c3n1*c4n2;
        rFp3 += (SQRT2_INV*WIGNER_334_224) * c44 * (c32*c32 - c3n2*c3n2);
        rFp3 += (2.0*SQRT2_INV*WIGNER_334_224) * c32*c3n2*c4n4;
        rFp3 += (2.0*SQRT2_INV*WIGNER_334_121) * c31 * (c32*c41 + c3n2*c4n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_334_121) * c3n1 * (c3n2*c41 - c32*c4n1);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_334_123) * c31 * (c32*c43 + c3n2*c4n3);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_334_123) * c3n1 * (c32*c4n3 - c3n2*c43);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_334_132) * c31 * (c33*c42 + c3n3*c4n2);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_334_132) * c3n1 * (c3n3*c42 - c33*c4n2);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_334_231) * c32 * (c33*c41 + c3n3*c4n1);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_334_231) * c3n2 * (c3n3*c41 - c33*c4n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_334_134) * c31 * (c33*c44 + c3n3*c4n4);
        rFp3 += (2.0*SQRT2_INV*WIGNER_334_134) * c3n1 * (c33*c4n4 - c3n3*c44);
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(6, rFp3);
        /// l1 = 2, l2 = l3 = 4
        rFp3 = 0.0;
        rFp3 += WIGNER_244_000 * c20*c40*c40;
        rFp3 -= WIGNER_244_011 * c20 * (c41*c41 + c4n1*c4n1);
        rFp3 += WIGNER_244_022 * c20 * (c42*c42 + c4n2*c4n2);
        rFp3 -= WIGNER_244_033 * c20 * (c43*c43 + c4n3*c4n3);
        rFp3 += WIGNER_244_044 * c20 * (c44*c44 + c4n4*c4n4);
        rFp3 -= (2.0*WIGNER_244_110) * c40 * (c21*c41 + c2n1*c4n1);
        rFp3 += (2.0*WIGNER_244_220) * c40 * (c22*c42 + c2n2*c4n2);
        rFp3 += (SQRT2_INV*WIGNER_244_211) * c22 * (c41*c41 - c4n1*c4n1);
        rFp3 += (2.0*SQRT2_INV*WIGNER_244_211) * c2n2*c41*c4n1;
        rFp3 += (2.0*SQRT2_INV*WIGNER_244_112) * c21 * (c41*c42 + c4n1*c4n2);
        rFp3 += (2.0*SQRT2_INV*WIGNER_244_112) * c2n1 * (c41*c4n2 - c4n1*c42);
        rFp3 += (2.0*SQRT2_INV*WIGNER_244_224) * c22 * (c42*c44 + c4n2*c4n4);
        rFp3 += (2.0*SQRT2_INV*WIGNER_244_224) * c2n2 * (c42*c4n4 - c4n2*c44);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_244_123) * c21 * (c42*c43 + c4n2*c4n3);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_244_123) * c2n1 * (c42*c4n3 - c4n2*c43);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_244_213) * c22 * (c41*c43 + c4n1*c4n3);
        rFp3 -= (2.0*SQRT2_INV*WIGNER_244_213) * c2n2 * (c41*c4n3 - c4n1*c43);
        rFp3 += (2.0*SQRT2_INV*WIGNER_244_134) * c21 * (c43*c44 + c4n3*c4n4);
        rFp3 += (2.0*SQRT2_INV*WIGNER_244_134) * c2n1 * (c43*c4n4 - c4n3*c44);
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(7, rFp3);
        /// l1 = 1, l2 = 3, l3 = 4
        rFp3 = 0.0;
        rFp3 += WIGNER_134_000 * c10*c30*c40;
        rFp3 -= WIGNER_134_011 * c10 * (c31*c41 + c3n1*c4n1);
        rFp3 += WIGNER_134_022 * c10 * (c32*c42 + c3n2*c4n2);
        rFp3 -= WIGNER_134_033 * c10 * (c33*c43 + c3n3*c4n3);
        rFp3 -= WIGNER_134_110 * c40 * (c11*c31 + c1n1*c3n1);
        rFp3 -= WIGNER_134_101 * c30 * (c11*c41 + c1n1*c4n1);
        rFp3 += (SQRT2_INV*WIGNER_134_112) * c11 * (c31*c42 + c3n1*c4n2);
        rFp3 += (SQRT2_INV*WIGNER_134_112) * c1n1 * (c31*c4n2 - c3n1*c42);
        rFp3 += (SQRT2_INV*WIGNER_134_121) * c11 * (c32*c41 + c3n2*c4n1);
        rFp3 += (SQRT2_INV*WIGNER_134_121) * c1n1 * (c3n2*c41 - c32*c4n1);
        rFp3 -= (SQRT2_INV*WIGNER_134_123) * c11 * (c32*c43 + c3n2*c4n3);
        rFp3 -= (SQRT2_INV*WIGNER_134_123) * c1n1 * (c32*c4n3 - c3n2*c43);
        rFp3 -= (SQRT2_INV*WIGNER_134_132) * c11 * (c33*c42 + c3n3*c4n2);
        rFp3 -= (SQRT2_INV*WIGNER_134_132) * c1n1 * (c3n3*c42 - c33*c4n2);
        rFp3 += (SQRT2_INV*WIGNER_134_134) * c11 * (c33*c44 + c3n3*c4n4);
        rFp3 += (SQRT2_INV*WIGNER_134_134) * c1n1 * (c33*c4n4 - c3n3*c44);
        if (mPolyScale[0]) rFp3 *= SQRT15_INV;
        rFp.set(8, rFp3);
    }
    
    public static final double SQRT_PI4 = MathEX.Fast.sqrt(MathEX.PI*4);
    public static final double SQRT15_INV = 1.0/MathEX.Fast.sqrt(15);
    public static final double WIGNER_222_000, WIGNER_222_011, WIGNER_222_022, WIGNER_222_112;
    public static final double WIGNER_112_000, WIGNER_112_011, WIGNER_112_110, WIGNER_112_112;
    public static final double WIGNER_233_000, WIGNER_233_011, WIGNER_233_022, WIGNER_233_033, WIGNER_233_110, WIGNER_233_220, WIGNER_233_112, WIGNER_233_211, WIGNER_233_123, WIGNER_233_213;
    public static final double WIGNER_123_000, WIGNER_123_011, WIGNER_123_022, WIGNER_123_110, WIGNER_123_101, WIGNER_123_112, WIGNER_123_121, WIGNER_123_123;
    public static final double WIGNER_444_000, WIGNER_444_011, WIGNER_444_022, WIGNER_444_033, WIGNER_444_044, WIGNER_444_112, WIGNER_444_224, WIGNER_444_123, WIGNER_444_134;
    public static final double WIGNER_224_000, WIGNER_224_011, WIGNER_224_022, WIGNER_224_110, WIGNER_224_220, WIGNER_224_112, WIGNER_224_224, WIGNER_224_121, WIGNER_224_123;
    public static final double WIGNER_334_000, WIGNER_334_011, WIGNER_334_022, WIGNER_334_033, WIGNER_334_110, WIGNER_334_220, WIGNER_334_330, WIGNER_334_112, WIGNER_334_224, WIGNER_334_121, WIGNER_334_123, WIGNER_334_132, WIGNER_334_231, WIGNER_334_134;
    public static final double WIGNER_244_000, WIGNER_244_011, WIGNER_244_022, WIGNER_244_033, WIGNER_244_044, WIGNER_244_110, WIGNER_244_220, WIGNER_244_211, WIGNER_244_112, WIGNER_244_224, WIGNER_244_123, WIGNER_244_213, WIGNER_244_134;
    public static final double WIGNER_134_000, WIGNER_134_011, WIGNER_134_022, WIGNER_134_033, WIGNER_134_110, WIGNER_134_101, WIGNER_134_112, WIGNER_134_121, WIGNER_134_123, WIGNER_134_132, WIGNER_134_134;
    static {
        WIGNER_222_000 = MathEX.Func.wigner3j(2, 2, 2, 0, 0, 0);
        WIGNER_222_011 = MathEX.Func.wigner3j(2, 2, 2, 0, 1,-1);
        WIGNER_222_022 = MathEX.Func.wigner3j(2, 2, 2, 0, 2,-2);
        WIGNER_222_112 = MathEX.Func.wigner3j(2, 2, 2, 1, 1,-2);
        WIGNER_112_000 = MathEX.Func.wigner3j(1, 1, 2, 0, 0, 0);
        WIGNER_112_011 = MathEX.Func.wigner3j(1, 1, 2, 0, 1,-1);
        WIGNER_112_110 = MathEX.Func.wigner3j(1, 1, 2, 1,-1, 0);
        WIGNER_112_112 = MathEX.Func.wigner3j(1, 1, 2, 1, 1,-2);
        WIGNER_233_000 = MathEX.Func.wigner3j(2, 3, 3, 0, 0, 0);
        WIGNER_233_011 = MathEX.Func.wigner3j(2, 3, 3, 0, 1,-1);
        WIGNER_233_022 = MathEX.Func.wigner3j(2, 3, 3, 0, 2,-2);
        WIGNER_233_033 = MathEX.Func.wigner3j(2, 3, 3, 0, 3,-3);
        WIGNER_233_110 = MathEX.Func.wigner3j(2, 3, 3, 1,-1, 0);
        WIGNER_233_220 = MathEX.Func.wigner3j(2, 3, 3, 2,-2, 0);
        WIGNER_233_112 = MathEX.Func.wigner3j(2, 3, 3, 1, 1,-2);
        WIGNER_233_211 = MathEX.Func.wigner3j(2, 3, 3,-2, 1, 1);
        WIGNER_233_123 = MathEX.Func.wigner3j(2, 3, 3, 1, 2,-3);
        WIGNER_233_213 = MathEX.Func.wigner3j(2, 3, 3, 2, 1,-3);
        WIGNER_123_000 = MathEX.Func.wigner3j(1, 2, 3, 0, 0, 0);
        WIGNER_123_011 = MathEX.Func.wigner3j(1, 2, 3, 0, 1,-1);
        WIGNER_123_022 = MathEX.Func.wigner3j(1, 2, 3, 0, 2,-2);
        WIGNER_123_110 = MathEX.Func.wigner3j(1, 2, 3, 1,-1, 0);
        WIGNER_123_101 = MathEX.Func.wigner3j(1, 2, 3, 1, 0,-1);
        WIGNER_123_112 = MathEX.Func.wigner3j(1, 2, 3, 1, 1,-2);
        WIGNER_123_121 = MathEX.Func.wigner3j(1, 2, 3, 1,-2, 1);
        WIGNER_123_123 = MathEX.Func.wigner3j(1, 2, 3, 1, 2,-3);
        WIGNER_444_000 = MathEX.Func.wigner3j(4, 4, 4, 0, 0, 0);
        WIGNER_444_011 = MathEX.Func.wigner3j(4, 4, 4, 0, 1,-1);
        WIGNER_444_022 = MathEX.Func.wigner3j(4, 4, 4, 0, 2,-2);
        WIGNER_444_033 = MathEX.Func.wigner3j(4, 4, 4, 0, 3,-3);
        WIGNER_444_044 = MathEX.Func.wigner3j(4, 4, 4, 0, 4,-4);
        WIGNER_444_112 = MathEX.Func.wigner3j(4, 4, 4, 1, 1,-2);
        WIGNER_444_224 = MathEX.Func.wigner3j(4, 4, 4, 2, 2,-4);
        WIGNER_444_123 = MathEX.Func.wigner3j(4, 4, 4, 1, 2,-3);
        WIGNER_444_134 = MathEX.Func.wigner3j(4, 4, 4, 1, 3,-4);
        WIGNER_224_000 = MathEX.Func.wigner3j(2, 2, 4, 0, 0, 0);
        WIGNER_224_011 = MathEX.Func.wigner3j(2, 2, 4, 0, 1,-1);
        WIGNER_224_022 = MathEX.Func.wigner3j(2, 2, 4, 0, 2,-2);
        WIGNER_224_110 = MathEX.Func.wigner3j(2, 2, 4, 1,-1, 0);
        WIGNER_224_220 = MathEX.Func.wigner3j(2, 2, 4, 2,-2, 0);
        WIGNER_224_112 = MathEX.Func.wigner3j(2, 2, 4, 1, 1,-2);
        WIGNER_224_224 = MathEX.Func.wigner3j(2, 2, 4, 2, 2,-4);
        WIGNER_224_121 = MathEX.Func.wigner3j(2, 2, 4, 1,-2, 1);
        WIGNER_224_123 = MathEX.Func.wigner3j(2, 2, 4, 1, 2,-3);
        WIGNER_334_000 = MathEX.Func.wigner3j(3, 3, 4, 0, 0, 0);
        WIGNER_334_011 = MathEX.Func.wigner3j(3, 3, 4, 0, 1,-1);
        WIGNER_334_022 = MathEX.Func.wigner3j(3, 3, 4, 0, 2,-2);
        WIGNER_334_033 = MathEX.Func.wigner3j(3, 3, 4, 0, 3,-3);
        WIGNER_334_110 = MathEX.Func.wigner3j(3, 3, 4, 1,-1, 0);
        WIGNER_334_220 = MathEX.Func.wigner3j(3, 3, 4, 2,-2, 0);
        WIGNER_334_330 = MathEX.Func.wigner3j(3, 3, 4, 3,-3, 0);
        WIGNER_334_112 = MathEX.Func.wigner3j(3, 3, 4, 1, 1,-2);
        WIGNER_334_224 = MathEX.Func.wigner3j(3, 3, 4, 2, 2,-4);
        WIGNER_334_121 = MathEX.Func.wigner3j(3, 3, 4, 1,-2, 1);
        WIGNER_334_123 = MathEX.Func.wigner3j(3, 3, 4, 1, 2,-3);
        WIGNER_334_132 = MathEX.Func.wigner3j(3, 3, 4, 1,-3, 2);
        WIGNER_334_231 = MathEX.Func.wigner3j(3, 3, 4, 2,-3, 1);
        WIGNER_334_134 = MathEX.Func.wigner3j(3, 3, 4, 1, 3,-4);
        WIGNER_244_000 = MathEX.Func.wigner3j(2, 4, 4, 0, 0, 0);
        WIGNER_244_011 = MathEX.Func.wigner3j(2, 4, 4, 0, 1,-1);
        WIGNER_244_022 = MathEX.Func.wigner3j(2, 4, 4, 0, 2,-2);
        WIGNER_244_033 = MathEX.Func.wigner3j(2, 4, 4, 0, 3,-3);
        WIGNER_244_044 = MathEX.Func.wigner3j(2, 4, 4, 0, 4,-4);
        WIGNER_244_110 = MathEX.Func.wigner3j(2, 4, 4, 1,-1, 0);
        WIGNER_244_220 = MathEX.Func.wigner3j(2, 4, 4, 2,-2, 0);
        WIGNER_244_211 = MathEX.Func.wigner3j(2, 4, 4,-2, 1, 1);
        WIGNER_244_112 = MathEX.Func.wigner3j(2, 4, 4, 1, 1,-2);
        WIGNER_244_224 = MathEX.Func.wigner3j(2, 4, 4, 2, 2,-4);
        WIGNER_244_123 = MathEX.Func.wigner3j(2, 4, 4, 1, 2,-3);
        WIGNER_244_213 = MathEX.Func.wigner3j(2, 4, 4, 2, 1,-3);
        WIGNER_244_134 = MathEX.Func.wigner3j(2, 4, 4, 1, 3,-4);
        WIGNER_134_000 = MathEX.Func.wigner3j(1, 3, 4, 0, 0, 0);
        WIGNER_134_011 = MathEX.Func.wigner3j(1, 3, 4, 0, 1,-1);
        WIGNER_134_022 = MathEX.Func.wigner3j(1, 3, 4, 0, 2,-2);
        WIGNER_134_033 = MathEX.Func.wigner3j(1, 3, 4, 0, 3,-3);
        WIGNER_134_110 = MathEX.Func.wigner3j(1, 3, 4, 1,-1, 0);
        WIGNER_134_101 = MathEX.Func.wigner3j(1, 3, 4, 1, 0,-1);
        WIGNER_134_112 = MathEX.Func.wigner3j(1, 3, 4, 1, 1,-2);
        WIGNER_134_121 = MathEX.Func.wigner3j(1, 3, 4, 1,-2, 1);
        WIGNER_134_123 = MathEX.Func.wigner3j(1, 3, 4, 1, 2,-3);
        WIGNER_134_132 = MathEX.Func.wigner3j(1, 3, 4, 1,-3, 2);
        WIGNER_134_134 = MathEX.Func.wigner3j(1, 3, 4, 1, 3,-4);
    }
    
    @Override
    public void backward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleList aForwardCache, DoubleList rBackwardCache, boolean aKeepCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        throw new UnsupportedOperationException();
    }
    @Override
    public void forwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleList aForwardCache, DoubleList rForwardForceCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        throw new UnsupportedOperationException();
    }
    @Override
    public void backwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                              DoubleList aForwardCache, DoubleList aForwardForceCache, DoubleList rBackwardCache, DoubleList rBackwardForceCache, boolean aKeepCache, boolean aFixBasis) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        throw new UnsupportedOperationException();
    }
}
