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

/**
 * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
 * 这里提供一个简单的纯 java 实现，用于测试或者高兼容性的方案
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
public class SimpleSphericalChebyshev extends Basis {
    final int mTypeNum;
    final int mNMax;
    final int mSizeN;
    
    final String @Nullable[] mSymbols;
    final int mLMax;
    final double mRCut;
    
    final int mSizeL;
    final int mLMaxMax, mLMAll;
    final int mSize;
    
    final Vector mRFuncScale;
    final double[] mNnScale;
    public SimpleSphericalChebyshev setRFuncScale(double... aRFuncScale) {
        mRFuncScale.fill(aRFuncScale);
        return this;
    }
    public SimpleSphericalChebyshev setNnScale(double aNnScale) {
        mNnScale[0] = aNnScale;
        return this;
    }
    
    private SimpleSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, int aLMaxMax, double aRCut, @Nullable Vector aRFuncScale, double @Nullable[] aNnScale) {
        
        if (aLMaxMax<0 || aLMaxMax>12) throw new IllegalArgumentException("Input lmax MUST be in [0, 12], input: "+aLMaxMax);
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mSizeN = aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        
        mSymbols = aSymbols;
        mLMax = aLMax;
        mRCut = aRCut;
        
        mSizeL = mLMax+1;
        mLMaxMax = aLMaxMax;
        if (mLMaxMax!=mLMax) throw new IllegalStateException();
        mLMAll = (mLMaxMax+1)*(mLMaxMax+1);
        
        mSize = mSizeN*mSizeL;
        
        mRFuncScale = aRFuncScale==null ? Vector.ones(mSizeN) : aRFuncScale;
        mNnScale = aNnScale==null ? new double[]{1.0} : aNnScale;
        if (mRFuncScale.size()!=mSizeN) throw new IllegalArgumentException("Size of rfunc scale mismatch");
        if (mNnScale.length!=1) throw new IllegalArgumentException("Size of nn scale mismatch");
    }
    SimpleSphericalChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, int aLMax, double aRCut, @Nullable Vector aRFuncScale, double @Nullable[] aNnScale) {
        this(aSymbols, aTypeNum, aNMax, aLMax, aLMax, aRCut, aRFuncScale, aNnScale);
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleSphericalChebyshev(String @NotNull[] aSymbols, int aNMax, int aLMax, double aRCut) {
        this(aSymbols, aSymbols.length, aNMax, aLMax, aRCut, null, null);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleSphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
        this(null, aTypeNum, aNMax, aLMax, aRCut, null, null);
    }
    
    @Override public SimpleSphericalChebyshev threadSafeRef() {
        return new SimpleSphericalChebyshev(mSymbols, mTypeNum, mNMax, mLMax, mRCut, mRFuncScale, mNnScale);
    }
    
    @SuppressWarnings("rawtypes")
    @Override public void save(Map rSaveTo) {
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SimpleSphericalChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        List<? extends Number> tRFuncScales = (List<? extends Number>)aMap.get("rfunc_scales");
        Vector aRFuncScales = tRFuncScales==null ? null : Vectors.from(tRFuncScales);
        Object tNnScale = aMap.get("nn_scale");
        double[] aNnScale = tNnScale==null ? null : new double[]{((Number)tNnScale).doubleValue()};
        return new SimpleSphericalChebyshev(
            aSymbols, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aRFuncScales, aNnScale
        );
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SimpleSphericalChebyshev load(int aTypeNum, Map aMap) {
        List<? extends Number> tRFuncScales = (List<? extends Number>)aMap.get("rfunc_scales");
        Vector aRFuncScales = tRFuncScales==null ? null : Vectors.from(tRFuncScales);
        Object tNnScale = aMap.get("nn_scale");
        double[] aNnScale = tNnScale==null ? null : new double[]{((Number)tNnScale).doubleValue()};
        return new SimpleSphericalChebyshev(
            null, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aRFuncScales, aNnScale
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
        // cnlm -> Pnl
        for (int n=0, tShift=0, tShiftFp=0; n<mSizeN; ++n, tShift+=mLMAll, tShiftFp+=mSizeL) {
            calL2_(rCnlm.subVec(tShift, tShift+mLMAll), rFp.subVec(tShiftFp, tShiftFp+mSizeL));
        }
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
            // cal fc
            double fc = SimpleChebyshev.calFc(dis, mRCut);
            // cal Rn
            SimpleChebyshev.calRn(rRn, mNMax, dis, mRCut);
            // cal Y
            MathEX.Func.realSphericalHarmonicsFull2DestXYZDis_(mLMaxMax, dx, dy, dz, dis, rY);
            // cal cnlm with rfunc scale
            if (mTypeNum==1) {
                int ck = 0;
                for (int n = 0; n <= mNMax; ++n) {
                    double tMul = fc*rRn.get(n)*mRFuncScale.get(n);
                    for (int k = 0; k < mLMAll; ++k) {
                        rCnlm.add(ck, tMul*rY.get(k));
                        ++ck;
                    }
                }
            } else {
                double wt = ((type&1)==1) ? type : -type;
                int ck = 0, ckwt = tSizeBnlm;
                for (int n = 0, nwt = mNMax+1; n <= mNMax; ++n, ++nwt) {
                    double tMul = fc*rRn.get(n);
                    double tScale = mRFuncScale.get(n);
                    double tScaleWt = mRFuncScale.get(nwt);
                    for (int k = 0; k < mLMAll; ++k) {
                        double tValue = tMul*rY.get(k);
                        rCnlm.add(ck, tValue*tScale);
                        rCnlm.add(ckwt, wt*tValue*tScaleWt);
                        ++ck; ++ckwt;
                    }
                }
            }
        }
        // scale nn here
        rCnlm.multiply2this(mNnScale[0]);
    }
    void calL2_(DoubleArrayVector aCnlm, IVector rFp) {
        int tShift = 0;
        for (int l = 0; l <= mLMaxMax; ++l) {
            int tLen = l+l+1;
            double rDot = aCnlm.subVec(tShift, tShift+tLen).operation().dot();
            rFp.set(l, (MathEX.PI*4/(double)tLen) * rDot);
            tShift += tLen;
        }
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
