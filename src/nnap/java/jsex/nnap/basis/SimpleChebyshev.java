package jsex.nnap.basis;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.MathEX;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 一种仅使用 Chebyshev 多项式将原子局域环境展开成一个基组的方法，
 * 这里提供一个简单的纯 java 实现，用于测试或者高兼容性的方案
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
public class SimpleChebyshev extends SimpleBasis {
    final static BiMap<String, Integer> VALID_WTYPE = ImmutableBiMap.<String, Integer>builder()
        .put("default", WTypeBasis.WTYPE_DEFAULT)
        .put("exfull", WTypeBasis.WTYPE_EXFULL)
        .build();
    
    final int mNumTypes;
    final int mWType;
    final int mNMax;
    final int mSizeN;
    
    final String @Nullable[] mSymbols;
    final double mRCut;
    
    final int mSize;
    
    SimpleChebyshev(String @Nullable[] aSymbols, int aNumTypes, int aWType, int aNMax, double aRCut) {
        if (!VALID_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {0, 3}, input: "+ aWType);
        
        mNumTypes = aNumTypes;
        mWType = aWType;
        mNMax = aNMax;
        mSizeN = getSizeN_(aWType, aNumTypes, aNMax);
        
        mSymbols = aSymbols;
        mRCut = aRCut;
        
        mSize = mSizeN;
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleChebyshev(String @NotNull[] aSymbols, int aNMax, double aRCut) {
        this(aSymbols, aSymbols.length, WTypeBasis.WTYPE_DEFAULT, aNMax, aRCut);
    }
    /**
     * @param aNumTypes 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aRCut 截断半径
     */
    public SimpleChebyshev(int aNumTypes, int aNMax, double aRCut) {
        this(null, aNumTypes, WTypeBasis.WTYPE_DEFAULT, aNMax, aRCut);
    }
    
    static int getSizeN_(int aWType, int aNumTypes, int aNMax) {
        switch(aWType) {
        case WTypeBasis.WTYPE_EXFULL: {
            return aNumTypes>1 ? (aNumTypes+1)*(aNMax+1) : (aNMax+1);
        }
        case WTypeBasis.WTYPE_DEFAULT: {
            return aNumTypes>1 ? (aNMax+aNMax+2) : (aNMax+1);
        }
        default: {
            throw new IllegalStateException();
        }}
    }
    @SuppressWarnings("rawtypes")
    static int getWType_(Map aMap) {
        @Nullable Object tType = UT.Code.get(aMap, "wtype");
        if (tType == null) return WTypeBasis.WTYPE_DEFAULT;
        if (tType instanceof Number) return ((Number)tType).intValue();
        @Nullable Integer tOut = VALID_WTYPE.get(tType.toString());
        if (tOut == null) throw new IllegalArgumentException("Input wtype MUST be in {default, exfull}, input: "+tType);
        return tOut;
    }
    
    
    @SuppressWarnings({"rawtypes"})
    public static SimpleChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        return new SimpleChebyshev(
            aSymbols, aSymbols.length, getWType_(aMap),
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_RCUT, "rcut")).doubleValue()
        );
    }
    @SuppressWarnings({"rawtypes"})
    public static SimpleChebyshev load(int aNumTypes, Map aMap) {
        return new SimpleChebyshev(
            null, aNumTypes, getWType_(aMap),
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, Chebyshev.DEFAULT_RCUT, "rcut")).doubleValue()
        );
    }
    
    
    /** @return {@inheritDoc} */
    @Override public double rcut() {
        return mRCut;
    }
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code (nmax+1)(lmax+1)}，如果超过一个种类则为
     * {@code 2(nmax+1)(lmax+1)}
     */
    @Override public int size() {
        return mSize;
    }
    /** @return {@inheritDoc} */
    @Override public int ntypes() {
        return mNumTypes;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean hasSymbol() {
        return mSymbols!=null;
    }
    /**
     * {@inheritDoc}
     * @param aType
     * @return {@inheritDoc}
     */
    @Override public @Nullable String symbol(int aType) {
        return mSymbols==null ? null : mSymbols[aType-1];
    }
    
    
    @Override
    public void forward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rForwardCache, boolean aFullCache) {
        if (isClosed()) throw new IllegalStateException("This Basis is dead");
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
            // cal fp
            if (mNumTypes ==1) {
                for (int n = 0; n <= mNMax; ++n) {
                    rFp.add(n, fc*rRn.get(n));
                }
            } else
            if (mWType==WTypeBasis.WTYPE_DEFAULT) {
                double wt = ((type&1)==1) ? type : -type;
                for (int n = 0, nwt = mNMax+1; n <= mNMax; ++n, ++nwt) {
                    double tRHS = fc*rRn.get(n);
                    rFp.add(n, tRHS);
                    rFp.add(nwt, wt*tRHS);
                }
            } else
            if (mWType==WTypeBasis.WTYPE_EXFULL) {
                int tShiftFp = type*(mNMax+1);
                for (int n = 0; n <= mNMax; ++n) {
                    double tRHS = fc*rRn.get(n);
                    rFp.add(n, tRHS);
                    rFp.add(n+tShiftFp, tRHS);
                }
            } else {
                throw new IllegalStateException();
            }
        }
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
}
