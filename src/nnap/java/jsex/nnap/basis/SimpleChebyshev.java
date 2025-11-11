package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.MathEX;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 一种仅使用 Chebyshev 多项式将原子局域环境展开成一个基组的方法，
 * 这里提供一个简单的纯 java 实现，用于测试或者高兼容性的方案
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
public class SimpleChebyshev extends Basis {
    final int mTypeNum;
    final int mNMax;
    final int mSizeN, mSizeWt;
    
    final String @Nullable[] mSymbols;
    final double mRCut;
    
    final int mSize;
    
    final Vector mRFuncScale, mRFuncShift;
    final Vector mSystemScale;
    public SimpleChebyshev setRFuncScale(double... aRFuncScale) {
        mRFuncScale.fill(aRFuncScale);
        return this;
    }
    public SimpleChebyshev setRFuncShift(double... aRFuncShift) {
        mRFuncShift.fill(aRFuncShift);
        return this;
    }
    public SimpleChebyshev setSystemScale(double... aSystemScale) {
        mSystemScale.fill(aSystemScale);
        return this;
    }
    
    SimpleChebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, double aRCut,
                    @Nullable Vector aRFuncScale, @Nullable Vector aRFuncShift, @Nullable Vector aSystemScale) {
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mSizeN = aTypeNum>1 ? (aNMax+aNMax+2) : (aNMax+1);
        mSizeWt = aTypeNum>1 ? 1 : 0;
        
        mSymbols = aSymbols;
        mRCut = aRCut;
        
        mSize = mSizeN;
        
        mRFuncScale = aRFuncScale==null ? Vector.ones(mNMax+1) : aRFuncScale;
        mRFuncShift = aRFuncShift==null ? Vector.zeros(mNMax+1) : aRFuncShift;
        mSystemScale = aSystemScale==null ? Vector.ones(mSize) : aSystemScale;
        
        if (mRFuncScale.size()!=mNMax+1) throw new IllegalArgumentException("Size of rfunc scale mismatch");
        if (mRFuncShift.size()!=mNMax+1) throw new IllegalArgumentException("Size of rfunc shift mismatch");
        if (mSystemScale.size()!=mSize) throw new IllegalArgumentException("Size of system scale mismatch");
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleChebyshev(String @NotNull[] aSymbols, int aNMax, double aRCut) {
        this(aSymbols, aSymbols.length, aNMax, aRCut, null, null, null);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleChebyshev(int aTypeNum, int aNMax, double aRCut) {
        this(null, aTypeNum, aNMax, aRCut, null, null, null);
    }
    
    @Override public SimpleChebyshev threadSafeRef() {
        return new SimpleChebyshev(mSymbols, mTypeNum, mNMax, mRCut, mRFuncScale, mRFuncShift, mSystemScale);
    }
    
    @SuppressWarnings("rawtypes")
    @Override public void save(Map rSaveTo) {
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SimpleChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        int aTypeNum = aSymbols.length;
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales", "rfunc_sigma");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tRFuncShift = (List<? extends Number>)UT.Code.get(aMap, "rfunc_shifts", "rfunc_mu");
        Vector aRFuncShift = tRFuncShift==null ? null : Vectors.from(tRFuncShift);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        return new SimpleChebyshev(
            aSymbols, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aRFuncScales, aRFuncShift, aSystemScale
        );
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SimpleChebyshev load(int aTypeNum, Map aMap) {
        List<? extends Number> tRFuncScale = (List<? extends Number>)UT.Code.get(aMap, "rfunc_scales", "rfunc_sigma");
        Vector aRFuncScales = tRFuncScale ==null ? null : Vectors.from(tRFuncScale);
        List<? extends Number> tRFuncShift = (List<? extends Number>)UT.Code.get(aMap, "rfunc_shifts", "rfunc_mu");
        Vector aRFuncShift = tRFuncShift==null ? null : Vectors.from(tRFuncShift);
        List<? extends Number> tSystemScale = (List<? extends Number>)UT.Code.get(aMap, "system_scales");
        Vector aSystemScale = tSystemScale==null ? null : Vectors.from(tSystemScale);
        return new SimpleChebyshev(
            null, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_RCUT, "rcut")).doubleValue(),
            aRFuncScales, aRFuncShift, aSystemScale
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
        validCache_(rForwardCache, mNMax+1);
        Vector rRn = new Vector(mNMax+1, rForwardCache.internalData());
        // clear fp first
        rFp.fill(0.0);
        // loop for neighbor
        final int tNN = aNlDx.size();
        for (int j = 0; j < tNN; ++j) {
            int type = aNlType.get(j);
            double dx = aNlDx.get(j), dy = aNlDy.get(j), dz = aNlDz.get(j);
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            // check rcut for merge
            if (dis >= mRCut) continue;
            // cal fc
            double fc = calFc(dis, mRCut);
            // cal Rn
            calRn(rRn, mNMax, dis, mRCut);
            // scale rfunc here
            rRn.minus2this(mRFuncShift);
            rRn.multiply2this(mRFuncScale);
            // cal fp, with rfunc scale
            if (mTypeNum==1) {
                for (int n = 0; n <= mNMax; ++n) {
                    rFp.add(n, fc*rRn.get(n));
                }
            } else {
                double wt = ((type&1)==1) ? type : -type;
                for (int n = 0, nwt = mNMax+1; n <= mNMax; ++n, ++nwt) {
                    double tRHS = fc*rRn.get(n);
                    rFp.add(n, tRHS);
                    rFp.add(nwt, wt*tRHS);
                }
            }
        }
        // system scale here
        rFp.multiply2this(mSystemScale);
    }
    static double pow2(double value) {
        return value * value;
    }
    static double pow3(double value) {
        return value * value * value;
    }
    static double pow4(double value) {
        double value2 = value * value;
        return value2 * value2;
    }
    static double calFc(double aDis, double aRCut) {
        return pow4(1.0 - pow2(aDis/aRCut));
    }
    static void calRn(IVector rRn, int aNMax, double aDis, double aRCut) {
        double tRnX = 1.0 - 2.0*aDis/aRCut;
        for (int n = 0; n <= aNMax; ++n) {
            rRn.set(n, MathEX.Func.chebyshev(n, tRnX));
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
