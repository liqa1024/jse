package jsex.nnap.basis;

import com.google.common.collect.Lists;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.operation.ARRAY;
import jse.math.vector.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jse.math.MathEX.*;

/**
 * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这是 NNAP 中默认使用的原子基组。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * References:
 * <a href="https://arxiv.org/abs/2211.03350v3">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * @author Su Rui, liqa
 */
@SuppressWarnings("SameParameterValue")
public class SphericalChebyshev implements IBasis {
    /** 需要的固定系数存储 */
    private final static Vector SQRT_LPM_LMM1; // sqrt((l+m)(l-m+1))
    private final static Vector SQRT_LPM1_LMM; // sqrt((l+m+1)(l-m))
    private final static double WIGNER_222_000, WIGNER_222_011, WIGNER_222_022, WIGNER_222_112;
    private final static double WIGNER_112_000, WIGNER_112_011, WIGNER_112_110, WIGNER_112_112;
    private final static double WIGNER_233_000, WIGNER_233_011, WIGNER_233_022, WIGNER_233_033, WIGNER_233_110, WIGNER_233_220, WIGNER_233_112, WIGNER_233_211, WIGNER_233_123, WIGNER_233_213;
    private final static double WIGNER_123_000, WIGNER_123_011, WIGNER_123_022, WIGNER_123_110, WIGNER_123_101, WIGNER_123_112, WIGNER_123_121, WIGNER_123_123;
    private final static double WIGNER_444_000, WIGNER_444_011, WIGNER_444_022, WIGNER_444_033, WIGNER_444_044, WIGNER_444_112, WIGNER_444_224, WIGNER_444_123, WIGNER_444_134;
    private final static double WIGNER_224_000, WIGNER_224_011, WIGNER_224_022, WIGNER_224_110, WIGNER_224_220, WIGNER_224_112, WIGNER_224_224, WIGNER_224_121, WIGNER_224_123;
    private final static double WIGNER_334_000, WIGNER_334_011, WIGNER_334_022, WIGNER_334_033, WIGNER_334_110, WIGNER_334_220, WIGNER_334_330, WIGNER_334_112, WIGNER_334_224, WIGNER_334_121, WIGNER_334_123, WIGNER_334_132, WIGNER_334_231, WIGNER_334_134;
    private final static double WIGNER_244_000, WIGNER_244_011, WIGNER_244_022, WIGNER_244_033, WIGNER_244_044, WIGNER_244_110, WIGNER_244_220, WIGNER_244_211, WIGNER_244_112, WIGNER_244_224, WIGNER_244_123, WIGNER_244_213, WIGNER_244_134;
    private final static double WIGNER_134_000, WIGNER_134_011, WIGNER_134_022, WIGNER_134_033, WIGNER_134_110, WIGNER_134_101, WIGNER_134_112, WIGNER_134_121, WIGNER_134_123, WIGNER_134_132, WIGNER_134_134;
    private final static double PI4 = PI*4.0;
    private final static int[] L3NCOLS = {0, 0, 2, 4, 9}, L3NCOLS_NOCROSS = {0, 0, 1, 1, 2};
    static {
        final int tSize = (SH_LARGEST_L+1)*(SH_LARGEST_L+1);
        SQRT_LPM_LMM1 = Vectors.NaN(tSize);
        SQRT_LPM1_LMM = Vectors.NaN(tSize);
        int tStart = 0;
        for (int tL = 0; tL <= SH_LARGEST_L; ++tL) {
            for (int tM = -tL; tM <= tL; ++tM) {
                SQRT_LPM_LMM1.set(tStart+tL+tM, MathEX.Fast.sqrt((tL+tM) * (tL-tM+1)));
                SQRT_LPM1_LMM.set(tStart+tL+tM, MathEX.Fast.sqrt((tL+tM+1) * (tL-tM)));
            }
            tStart += tL+tL+1;
        }
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
    
    public final static int DEFAULT_NMAX = 5;
    public final static int DEFAULT_LMAX = 6;
    public final static int DEFAULT_L3MAX = 0;
    public final static boolean DEFAULT_L3CROSS = true;
    public final static double DEFAULT_RCUT = 6.2;
    
    private final int mTypeNum;
    private final String @Nullable[] mSymbols;
    private final int mNMax, mLMax, mL3Max;
    private final boolean mL3Cross;
    private final double mRCut;
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aL3Max 三阶基组中球谐函数 l 选取的最大阶数，目前只支持到 {@code l = 4}
     * @param aL3Cross 三阶基组中球谐函数是否考虑交叉项，默认为 {@code true}
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(String @NotNull[] aSymbols, int aNMax, int aLMax, int aL3Max, boolean aL3Cross, double aRCut) {
        if (aNMax < 0) throw new IllegalArgumentException("Input nmax MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input lmax MUST be Non-Negative, input: "+aLMax);
        if (aL3Max<0 || aL3Max>4) throw new IllegalArgumentException("Input l3max MUST be in [0, 4], input: "+aL3Max);
        mSymbols = aSymbols;
        mTypeNum = mSymbols.length;
        mNMax = aNMax;
        mLMax = aLMax;
        mL3Max = aL3Max;
        mL3Cross = aL3Cross;
        mRCut = aRCut;
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aL3Max 三阶基组中球谐函数 l 选取的最大阶数，目前只支持到 {@code l = 4}
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(String @NotNull[] aSymbols, int aNMax, int aLMax, int aL3Max, double aRCut) {
        this(aSymbols, aNMax, aLMax, aL3Max, DEFAULT_L3CROSS, aRCut);
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(String @NotNull[] aSymbols, int aNMax, int aLMax, double aRCut) {
        this(aSymbols, aNMax, aLMax, DEFAULT_L3MAX, aRCut);
    }
    
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aL3Max 三阶基组中球谐函数 l 选取的最大阶数，目前只支持到 {@code l = 4}
     * @param aL3Cross 三阶基组中球谐函数是否考虑交叉项，默认为 {@code true}
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, int aL3Max, boolean aL3Cross, double aRCut) {
        if (aNMax < 0) throw new IllegalArgumentException("Input nmax MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input lmax MUST be Non-Negative, input: "+aLMax);
        if (aL3Max<0 || aL3Max>4) throw new IllegalArgumentException("Input l3max MUST be in [0, 4], input: "+aL3Max);
        mSymbols = null;
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mLMax = aLMax;
        mL3Max = aL3Max;
        mL3Cross = aL3Cross;
        mRCut = aRCut;
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aL3Max 三阶基组中球谐函数 l 选取的最大阶数，目前只支持到 {@code l = 4}
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, int aL3Max, double aRCut) {
        this(aTypeNum, aNMax, aLMax, aL3Max, DEFAULT_L3CROSS, aRCut);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
        this(aTypeNum, aNMax, aLMax, DEFAULT_L3MAX, aRCut);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("l3max", mL3Max);
        rSaveTo.put("l3cross", mL3Cross);
        rSaveTo.put("rcut", mRCut);
    }
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(String @NotNull[] aSymbols, Map aMap) {
        return new SphericalChebyshev(
            aSymbols,
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue()
        );
    }
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(int aTypeNum, Map aMap) {
        return new SphericalChebyshev(
            aTypeNum,
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_L3MAX, "l3max")).intValue(),
            (Boolean)UT.Code.getWithDefault(aMap, DEFAULT_L3CROSS, "l3cross"),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue()
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
        return sizeN()*sizeL();
    }
    /** @return {@inheritDoc} */
    @Override public @Unmodifiable List<String> symbols() {
        return mSymbols==null ? null : AbstractCollections.from(mSymbols);
    }
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    
    protected int sizeN() {
        return mTypeNum>1 ? mNMax+mNMax+2 : mNMax+1;
    }
    protected int sizeL() {
        return mLMax+1 + (mL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[mL3Max];
    }
    private int lmax_() {
        return Math.max(mLMax, mL3Max);
    }
    private int lmAll_() {
        int tLMax = lmax_();
        return (tLMax+1)*(tLMax+1);
    }
    
    
    /** 一些缓存的中间变量，现在统一作为对象存储，对于这种大规模的缓存情况可以进一步提高效率 */
    private @Nullable RowMatrix mCnlm = null;
    @NotNull RowMatrix bufCnlm(boolean aClear) {if (mCnlm==null) {mCnlm = MatrixCache.getMatRow(sizeN(), lmAll_());} if (aClear) {mCnlm.fill(0.0);} return mCnlm;}
    private @Nullable Vector mCnlmPx = null, mCnlmPy = null, mCnlmPz = null;
    @NotNull Vector bufCnlmPx(boolean aClear) {if (mCnlmPx==null) {mCnlmPx = VectorCache.getVec(lmAll_());} if (aClear) {mCnlmPx.fill(0.0);} return mCnlmPx;}
    @NotNull Vector bufCnlmPy(boolean aClear) {if (mCnlmPy==null) {mCnlmPy = VectorCache.getVec(lmAll_());} if (aClear) {mCnlmPy.fill(0.0);} return mCnlmPy;}
    @NotNull Vector bufCnlmPz(boolean aClear) {if (mCnlmPz==null) {mCnlmPz = VectorCache.getVec(lmAll_());} if (aClear) {mCnlmPz.fill(0.0);} return mCnlmPz;}
    
    private @Nullable Vector mRn = null, mRnPx = null, mRnPy = null, mRnPz = null;
    @NotNull Vector bufRn(boolean aClear) {if (mRn==null) {mRn = VectorCache.getVec(mNMax+1);} if (aClear) {mRn.fill(0.0);} return mRn;}
    @NotNull Vector bufRnPx(boolean aClear) {if (mRnPx==null) {mRnPx = VectorCache.getVec(mNMax+1);} if (aClear) {mRnPx.fill(0.0);} return mRnPx;}
    @NotNull Vector bufRnPy(boolean aClear) {if (mRnPy==null) {mRnPy = VectorCache.getVec(mNMax+1);} if (aClear) {mRnPy.fill(0.0);} return mRnPy;}
    @NotNull Vector bufRnPz(boolean aClear) {if (mRnPz==null) {mRnPz = VectorCache.getVec(mNMax+1);} if (aClear) {mRnPz.fill(0.0);} return mRnPz;}
    
    private @Nullable Vector mY = null, mYPx = null, mYPy = null, mYPz = null, mYPphi = null, mYPtheta = null;
    @NotNull Vector bufY(boolean aClear) {if (mY==null) {mY = VectorCache.getVec(lmAll_());} if (aClear) {mY.fill(0.0);} return mY;}
    @NotNull Vector bufYPx(boolean aClear) {if (mYPx==null) {mYPx = VectorCache.getVec(lmAll_());} if (aClear) {mYPx.fill(0.0);} return mYPx;}
    @NotNull Vector bufYPy(boolean aClear) {if (mYPy==null) {mYPy = VectorCache.getVec(lmAll_());} if (aClear) {mYPy.fill(0.0);} return mYPy;}
    @NotNull Vector bufYPz(boolean aClear) {if (mYPz==null) {mYPz = VectorCache.getVec(lmAll_());} if (aClear) {mYPz.fill(0.0);} return mYPz;}
    @NotNull Vector bufYPphi(boolean aClear) {if (mYPphi==null) {mYPphi = VectorCache.getVec(lmAll_());} if (aClear) {mYPphi.fill(0.0);} return mYPphi;}
    @NotNull Vector bufYPtheta(boolean aClear) {if (mYPtheta==null) {mYPtheta = VectorCache.getVec(lmAll_());} if (aClear) {mYPtheta.fill(0.0);} return mYPtheta;}
    
    private final DoubleList mDxAll = new DoubleList(16), mDyAll = new DoubleList(16), mDzAll = new DoubleList(16);
    private final IntList mTypeAll = new IntList(16);
    private final List<Vector> mYAll = new ArrayList<>();
    @NotNull Vector bufYAll(int i, boolean aClear) {while (mYAll.size()<=i) {mYAll.add(VectorCache.getVec(lmAll_()));} Vector tY = mYAll.get(i); if (aClear) {tY.fill(0.0);} return tY;}
    private final List<Vector> mRnAll = new ArrayList<>();
    @NotNull Vector bufRnAll(int i, boolean aClear) {while (mRnAll.size()<=i) {mRnAll.add(VectorCache.getVec(mNMax+1));} Vector tRn = mRnAll.get(i); if (aClear) {tRn.fill(0.0);} return tRn;}
    
    @Override public void shutdown() {
        if (mCnlm != null) MatrixCache.returnMat(mCnlm);
        if (mCnlmPx != null) VectorCache.returnVec(mCnlmPx);
        if (mCnlmPy != null) VectorCache.returnVec(mCnlmPy);
        if (mCnlmPz != null) VectorCache.returnVec(mCnlmPz);
        if (mRn != null) VectorCache.returnVec(mRn);
        if (mRnPx != null) VectorCache.returnVec(mRnPx);
        if (mRnPy != null) VectorCache.returnVec(mRnPy);
        if (mRnPz != null) VectorCache.returnVec(mRnPz);
        if (mY != null) VectorCache.returnVec(mY);
        if (mYPx != null) VectorCache.returnVec(mYPx);
        if (mYPy != null) VectorCache.returnVec(mYPy);
        if (mYPz != null) VectorCache.returnVec(mYPz);
        if (mYPphi != null) VectorCache.returnVec(mYPphi);
        if (mYPtheta != null) VectorCache.returnVec(mYPtheta);
        for (Vector tYSub : mYAll) VectorCache.returnVec(tYSub);
        for (Vector tRnSub : mRnAll) VectorCache.returnVec(tRnSub);
    }
    
    protected RowMatrix calCnlm(IDxyzTypeIterable aNL, final boolean aBufferNL) {
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final RowMatrix cnlm = bufCnlm(true);
        // 缓存 Rn 数组
        final Vector tRn = aBufferNL ? null : bufRn(false);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final Vector tY = aBufferNL ? null : bufY(false);
        // 缓存情况需要先清空这些
        if (aBufferNL) {
            mDxAll.clear(); mDyAll.clear(); mDzAll.clear();
            mTypeAll.clear();
        }
        
        // 遍历近邻计算 Ylm, Rn, fc
        final int tLMax = lmax_();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= mRCut) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            
            final int j = aBufferNL ? mDxAll.size() : -1;
            if (aBufferNL) {
                mDxAll.add(dx); mDyAll.add(dy); mDzAll.add(dz);
                mTypeAll.add(type);
            }
            // 计算种类的权重
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc
            double fc = MathEX.Fast.powFast(1.0 - MathEX.Fast.pow2(dis/mRCut), 4);
            // 统一遍历一次计算 Rn
            final double tX = 1.0 - 2.0*dis/mRCut;
            Vector tRn_ = aBufferNL ? bufRnAll(j, false) : tRn;
            tRn_.fill(n -> MathEX.Func.chebyshev(n, tX));
            
            // 遍历求 n，l 的情况；现在采用实球谐函数进行计算
            Vector tY_ = aBufferNL ? bufYAll(j, false) : tY;
            MathEX.Func.realSphericalHarmonicsFull2DestXYZDis_(tLMax, dx, dy, dz, dis, tY_);
            if (mTypeNum > 1) {
                for (int tN = 0; tN <= mNMax; ++tN) {
                    mplusCnlm(cnlm.internalData(), cnlm.row(tN).internalDataShift(), cnlm.row(tN+mNMax+1).internalDataShift(),
                              tY_.internalData(), fc, tRn_.get(tN), wt, cnlm.columnNumber());
                }
            } else {
                for (int tN = 0; tN <= mNMax; ++tN) {
                    mplusCnlm(cnlm.internalData(), cnlm.row(tN).internalDataShift(),
                              tY_.internalData(), fc, tRn_.get(tN), cnlm.columnNumber());
                }
            }
        });
        return cnlm;
    }
    
    /**
     * {@inheritDoc}
     * @param aNL 近邻列表遍历器
     * @return {@inheritDoc}
     */
    @Override public Vector eval(IDxyzTypeIterable aNL) {
        final int tSizeN = sizeN();
        final Vector rFingerPrint = VectorCache.getVec(tSizeN*sizeL());
        
        // 统一计算 cnlm
        final RowMatrix cnlm = calCnlm(aNL, false);
        
        // 做标量积消去 m 项，得到此原子的 FP
        cnlm2fp(cnlm.internalData(), rFingerPrint.internalData(),
                tSizeN, mLMax, mL3Max, mL3Cross);
        
        return rFingerPrint;
    }
    
    /**
     * {@inheritDoc}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aNL 近邻列表遍历器
     * @return {@inheritDoc}
     */
    @Override public List<@NotNull Vector> evalPartial(boolean aCalCross, IDxyzTypeIterable aNL) {
        final int tSizeN = sizeN();
        final int tSizeL = sizeL();
        Vector rFingerPrint = VectorCache.getVec(tSizeN*tSizeL);
        // 先统一计算 cnlm 并缓存近邻
        final RowMatrix cnlm = calCnlm(aNL, true);
        final int tNN = mDxAll.size();
        // 做标量积消去 m 项，得到此原子的 FP
        cnlm2fp(cnlm.internalData(), rFingerPrint.internalData(),
                tSizeN, mLMax, mL3Max, mL3Cross);
        
        // 下面计算偏导部分
        final Vector rFingerPrintPx = VectorCache.getZeros(tSizeN*tSizeL);
        final Vector rFingerPrintPy = VectorCache.getZeros(tSizeN*tSizeL);
        final Vector rFingerPrintPz = VectorCache.getZeros(tSizeN*tSizeL);
        final @Nullable List<Vector> rFingerPrintPxCross = aCalCross ? VectorCache.getZeros(tSizeN*tSizeL, tNN) : null;
        final @Nullable List<Vector> rFingerPrintPyCross = aCalCross ? VectorCache.getZeros(tSizeN*tSizeL, tNN) : null;
        final @Nullable List<Vector> rFingerPrintPzCross = aCalCross ? VectorCache.getZeros(tSizeN*tSizeL, tNN) : null;
        // 缓存 cnlm 偏导数数据，现在只需要特定 n 下的一行即可
        final Vector cnlmPx = bufCnlmPx(false);
        final Vector cnlmPy = bufCnlmPy(false);
        final Vector cnlmPz = bufCnlmPz(false);
        // 缓存 Rn 数组
        final Vector tRnPx = bufRnPx(false);
        final Vector tRnPy = bufRnPy(false);
        final Vector tRnPz = bufRnPz(false);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final Vector tYPtheta = bufYPtheta(false);
        final Vector tYPphi = bufYPphi(false);
        final Vector tYPx = bufYPx(false);
        final Vector tYPy = bufYPy(false);
        final Vector tYPz = bufYPz(false);
        
        // 再次遍历近邻计算偏导，这里可以直接累加到基组上
        final int tLMax = lmax_();
        for (int j = 0; j < tNN; ++j) {
            double dx = mDxAll.get(j), dy = mDyAll.get(j), dz = mDzAll.get(j);
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            // 计算种类的权重
            int type = mTypeAll.get(j);
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc 以及偏导数（涉及重复计算，不过无所谓）
            double fcMul = 1.0 - MathEX.Fast.pow2(dis/mRCut);
            double fcMul3 = MathEX.Fast.pow3(fcMul);
            double fc = fcMul3 * fcMul;
            double fcPMul = 8.0 * fcMul3 / (mRCut*mRCut);
            double fcPx = dx * fcPMul;
            double fcPy = dy * fcPMul;
            double fcPz = dz * fcPMul;
            // 计算 Rn 的偏导数
            Vector tRn = bufRnAll(j, false);
            calRnPxyz(tRnPx.internalData(), tRnPy.internalData(), tRnPz.internalData(), mNMax,
                      dis, mRCut, dx, dy, dz);
            // 计算 Ylm 偏导数；这里需要使用事先计算好角度的版本
            double dxy = MathEX.Fast.hypot(dx, dy);
            final double cosTheta = dz / dis;
            final double sinTheta = dxy / dis;
            final double cosPhi;
            final double sinPhi;
            final boolean dxyCloseZero = MathEX.Code.numericEqual(dxy, 0.0);
            if (dxyCloseZero) {
                cosPhi = 1.0;
                sinPhi = 0.0;
            } else {
                cosPhi = dx / dxy;
                sinPhi = dy / dxy;
            }
            Vector tY = bufYAll(j, false); // 不再需要重复计算球谐函数值了
            if (dxyCloseZero) tYPphi.fill(0.0); // 这样来修复顶点的情况，此时另一边 tYPtheta 会恰好弥补使得全局连续
            for (int tL = 0; tL <= tLMax; ++tL) {
                if (!dxyCloseZero) {
                    calYPphi(tYPphi.internalData(), tY.internalData(), tL);
                }
                calYPtheta(cosPhi, sinPhi, tYPtheta.internalData(), tY.internalData(), tL);
            }
            // 最后转换为 xyz 的偏微分
            calYPxyz(cosTheta, sinTheta, cosPhi, sinPhi, dis, dxy, dxyCloseZero,
                     tYPx.internalData(), tYPy.internalData(), tYPz.internalData(),
                     tYPtheta.internalData(), tYPphi.internalData(), tY.size());
            
            // 遍历 n，l 求 cnlmPxyz；现在直接边求边累加到 Fp 中
            final int tColNum = cnlm.columnNumber();
            for (int tN=0, tShift=0, tShiftFP=0; tN <= mNMax; ++tN, tShift+=tColNum, tShiftFP+=tSizeL) {
                // 先统一计算当前 n 下的所有 cnlmPxyz
                calCnlmPxyz(cnlmPx.internalData(), cnlmPy.internalData(), cnlmPz.internalData(),
                            tY.internalData(), tYPx.internalData(), tYPy.internalData(), tYPz.internalData(),
                            fc, fcPx, fcPy, fcPz,
                            tRn.get(tN), tRnPx.get(tN), tRnPy.get(tN), tRnPz.get(tN),
                            tY.internalDataSize());
                // 遍历 l，m 累加到 fp 中
                cnlm2fpPxyz(cnlm.internalData(), cnlmPx.internalData(), cnlmPy.internalData(), cnlmPz.internalData(),
                            rFingerPrintPx.internalData(), rFingerPrintPy.internalData(), rFingerPrintPz.internalData(),
                            rFingerPrintPxCross==null?null:rFingerPrintPxCross.get(j).internalData(),
                            rFingerPrintPyCross==null?null:rFingerPrintPyCross.get(j).internalData(),
                            rFingerPrintPzCross==null?null:rFingerPrintPzCross.get(j).internalData(),
                            1.0, mLMax, mL3Max, mL3Cross, tShift, tShiftFP);
                if (mTypeNum > 1) {
                    cnlm2fpPxyz(cnlm.internalData(), cnlmPx.internalData(), cnlmPy.internalData(), cnlmPz.internalData(),
                                rFingerPrintPx.internalData(), rFingerPrintPy.internalData(), rFingerPrintPz.internalData(),
                                rFingerPrintPxCross==null?null:rFingerPrintPxCross.get(j).internalData(),
                                rFingerPrintPyCross==null?null:rFingerPrintPyCross.get(j).internalData(),
                                rFingerPrintPzCross==null?null:rFingerPrintPzCross.get(j).internalData(),
                                wt, mLMax, mL3Max, mL3Cross, tShift+(mNMax+1)*tColNum, tShiftFP+(mNMax+1)*tSizeL);
                }
            }
        }
        List<Vector> rOut = Lists.newArrayList(rFingerPrint, rFingerPrintPx, rFingerPrintPy, rFingerPrintPz);
        if (aCalCross) {
            assert rFingerPrintPxCross!=null && rFingerPrintPyCross!=null && rFingerPrintPzCross!=null;
            rOut.addAll(rFingerPrintPxCross);
            rOut.addAll(rFingerPrintPyCross);
            rOut.addAll(rFingerPrintPzCross);
        }
        return rOut;
    }
    
    
    protected static void calRnPxyz(double[] rRnPx, double[] rRnPy, double[] rRnPz, int aNMax,
                                    double aDis, double aRCut, double aDx, double aDy, double aDz) {
        final double tX = 1.0 - 2.0*aDis/aRCut;
        final double tRnPMul = 2.0 / (aDis*aRCut);
        rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
        for (int tN = 1; tN <= aNMax; ++tN) {
            double tRnP = tN*MathEX.Func.chebyshev2(tN-1, tX)*tRnPMul;
            rRnPx[tN] = tRnP*aDx;
            rRnPy[tN] = tRnP*aDy;
            rRnPz[tN] = tRnP*aDz;
        }
    }
    protected static void calYPphi(double[] rYPphi, double[] aY, int aL) {
        final int tStart = aL*aL;
        final int tIdx = tStart+aL;
        for (int tM = -aL; tM <= aL; ++tM) {
            rYPphi[tIdx+tM] = -tM * aY[tIdx-tM];
        }
    }
    protected static void calYPtheta(double aCosPhi, double aSinPhi, double[] rYPtheta, double[] aY, int aL) {
        switch(aL) {
        case 0: {
            rYPtheta[0] = 0.0;
            return;
        }
        case 1: {
            double tMul = SQRT_LPM_LMM1.get(2)*SQRT2_INV;
            rYPtheta[1] = -tMul * aSinPhi*aY[2];
            rYPtheta[2] =  tMul * (aCosPhi*aY[3] + aSinPhi*aY[1]);
            rYPtheta[3] = -tMul * aCosPhi*aY[2];
            return;
        }
        default: {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            final int tStart = aL*aL;
            final int tIdx = tStart+aL;
            double tMul = SQRT_LPM_LMM1.get(tIdx)*SQRT2_INV;
            rYPtheta[tIdx] = tMul * (aCosPhi*aY[tIdx+1] + aSinPhi*aY[tIdx-1]);
            rYPtheta[tIdx+1] = -tMul * aCosPhi*aY[tIdx];
            rYPtheta[tIdx-1] = -tMul * aSinPhi*aY[tIdx];
            for (int tM = 2; tM <= aL; ++tM) {
                tMul = -0.5*SQRT_LPM_LMM1.get(tIdx+tM);
                rYPtheta[tIdx+tM] = tMul * (aCosPhi*aY[tIdx+tM-1] - aSinPhi*aY[tIdx-tM+1]);
                rYPtheta[tIdx-tM] = tMul * (aCosPhi*aY[tIdx-tM+1] + aSinPhi*aY[tIdx+tM-1]);
            }
            for (int tM = 1; tM < aL; ++tM) {
                tMul = 0.5*SQRT_LPM1_LMM.get(tIdx+tM);
                rYPtheta[tIdx+tM] += tMul * (aCosPhi*aY[tIdx+tM+1] + aSinPhi*aY[tIdx-tM-1]);
                rYPtheta[tIdx-tM] += tMul * (aCosPhi*aY[tIdx-tM-1] - aSinPhi*aY[tIdx+tM+1]);
            }
            return;
        }}
    }
    protected static void calYPxyz(double aCosTheta, double aSinTheta, double aCosPhi, double aSinPhi, double aDis, double aDxy, boolean aDxyCloseZero,
                                   double[] rYPx, double[] rYPy, double[] rYPz, double[] aYPtheta, double[] aYPphi, int aLength) {
        final double thetaPx = -aCosTheta * aCosPhi / aDis;
        final double thetaPy = -aCosTheta * aSinPhi / aDis;
        final double thetaPz =  aSinTheta / aDis;
        final double phiPx = aDxyCloseZero ? 0.0 :  aSinPhi / aDxy;
        final double phiPy = aDxyCloseZero ? 0.0 : -aCosPhi / aDxy;
        for (int i = 0; i < aLength; ++i) {
            double tYPtheta = aYPtheta[i];
            double tYPphi = aYPphi[i];
            rYPx[i] = tYPtheta*thetaPx + tYPphi*phiPx;
            rYPy[i] = tYPtheta*thetaPy + tYPphi*phiPy;
            rYPz[i] = tYPtheta*thetaPz;
        }
    }
    /** 热点优化，累加 cnlm 部分放在一个循环中进行，让 java 优化整个运算 */
    protected static void mplusCnlm(double[] rCnlm, int rShift, double[] aY, double aFc, double aRn, int aLength) {
        double tMul = aFc*aRn;
        for (int i = 0, j = rShift; i < aLength; ++i, ++j) {
            rCnlm[j] += tMul*aY[i];
        }
    }
    protected static void mplusCnlm(double[] rCnlm, int rShift1, int rShift2, double[] aY, double aFc, double aRn, double aWt, int aLength) {
        double tMul = aFc*aRn;
        for (int i = 0, j = rShift1, k = rShift2; i < aLength; ++i, ++j, ++k) {
            double tCnli = tMul*aY[i];
            rCnlm[j] += tCnli; rCnlm[k] += aWt*tCnli;
        }
    }
    protected static void calCnlmPxyz(double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz,
                                      double[] aY, double[] aYPx, double[] aYPy, double[] aYPz,
                                      double aFc, double aFcPx, double aFcPy, double aFcPz,
                                      double aRn, double aRnPx, double aRnPy, double aRnPz, int aLength) {
        double tMul = aFc*aRn;
        double tMulX = aFc*aRnPx + aFcPx*aRn;
        double tMulY = aFc*aRnPy + aFcPy*aRn;
        double tMulZ = aFc*aRnPz + aFcPz*aRn;
        for (int i = 0; i < aLength; ++i) {
            double tY = aY[i];
            rCnlmPx[i] = -(tMulX*tY + tMul*aYPx[i]);
            rCnlmPy[i] = -(tMulY*tY + tMul*aYPy[i]);
            rCnlmPz[i] = -(tMulZ*tY + tMul*aYPz[i]);
        }
    }
    /** 热点优化，cnlm 转成 fp 放在一个循环中，让 java 优化整个运算 */
    protected static void cnlm2fp(double[] aCnlm, double[] rFp, int aSizeN, int aLMax, int aL3Max, boolean aL3Cross) {
        final int tLMax = Math.max(aLMax, aL3Max);
        final int tColNum = (tLMax+1)*(tLMax+1);
        final int tColNumFP = aLMax+1 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
        int tShift = 0, tShiftFP = 0;
        for (int tN = 0; tN < aSizeN; ++tN, tShift += tColNum, tShiftFP += tColNumFP) {
            calL2_(aCnlm, rFp, aLMax, tShift, tShiftFP);
            calL3_(aCnlm, rFp, aLMax, aL3Max, aL3Cross, tShift, tShiftFP);
        }
    }
    private static void calL2_(double[] aCnlm, double[] rFp, int aLMax, int aShift, int aShiftFP) {
        // l == 0
        int tIdx = aShift, tIdxFP = aShiftFP;
        double tCnl0 = aCnlm[tIdx];
        double tMul = PI4;
        rFp[tIdxFP] = tMul * (tCnl0*tCnl0);
        if (aLMax == 0) return;
        // l = 1
        tIdx = 1+aShift; tIdxFP = 1+aShiftFP;
        tCnl0 = aCnlm[tIdx]; double tCnl1 = aCnlm[tIdx+1], tCnl2 = aCnlm[tIdx+2];
        tMul = PI4/3;
        rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2);
        if (aLMax == 1) return;
        // l = 2
        tIdx = 4+aShift; tIdxFP = 2+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; double tCnl3 = aCnlm[tIdx+3], tCnl4 = aCnlm[tIdx+4];
        tMul = PI4/5;
        rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4);
        if (aLMax == 2) return;
        // l = 3
        tIdx = 9+aShift; tIdxFP = 3+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; double tCnl5 = aCnlm[tIdx+5], tCnl6 = aCnlm[tIdx+6];
        tMul = PI4/7;
        rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6);
        if (aLMax == 3) return;
        // l = 4
        tIdx = 16+aShift; tIdxFP = 4+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; double tCnl7 = aCnlm[tIdx+7], tCnl8 = aCnlm[tIdx+8];
        tMul = PI4/9;
        rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6 + tCnl7*tCnl7 + tCnl8*tCnl8);
        if (aLMax == 4) return;
        // l = 5
        tIdx = 25+aShift; tIdxFP = 5+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; tCnl7 = aCnlm[tIdx+7]; tCnl8 = aCnlm[tIdx+8]; double tCnl9 = aCnlm[tIdx+9], tCnl10 = aCnlm[tIdx+10];
        tMul = PI4/11;
        rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6 + tCnl7*tCnl7 + tCnl8*tCnl8 + tCnl9*tCnl9 + tCnl10*tCnl10);
        if (aLMax == 5) return;
        // l = 6
        tIdx = 36+aShift; tIdxFP = 6+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; tCnl7 = aCnlm[tIdx+7]; tCnl8 = aCnlm[tIdx+8]; tCnl9 = aCnlm[tIdx+9]; tCnl10 = aCnlm[tIdx+10]; double tCnl11 = aCnlm[tIdx+11], tCnl12 = aCnlm[tIdx+12];
        tMul = PI4/13;
        rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6 + tCnl7*tCnl7 + tCnl8*tCnl8 + tCnl9*tCnl9 + tCnl10*tCnl10 + tCnl11*tCnl11 + tCnl12*tCnl12);
        if (aLMax == 6) return;
        // 优化到 l = 6 主要是大部分只用到这个程度
        // else
        for (int tL = 7; tL <= aLMax; ++tL) {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            final int tStart = tL*tL + aShift;
            final int tLen = tL+tL+1;
            // 调用 ARRAY 中的 dotOfThis 方法，有做 SIMD 优化
            double rDot = ARRAY.dotOfThis(aCnlm, tStart, tLen);
            tMul = PI4/(double)tLen;
            rFp[tL+aShiftFP] = tMul * rDot;
        }
    }
    private static void calL3_(double[] aCnlm, double[] rFp, int aLMax, int aL3Max, boolean aL3Cross, int aShift, int aShiftFP) {
        if (aL3Max <= 1) return;
        int tIdxFP = aLMax+1+aShiftFP;
        /// l1 = l2 = l3 = 2
        int tIdx = (2*2+2)+aShift;
        final double c20  = aCnlm[tIdx  ];
        final double c21  = aCnlm[tIdx+1];
        final double c2n1 = aCnlm[tIdx-1];
        final double c22  = aCnlm[tIdx+2];
        final double c2n2 = aCnlm[tIdx-2];
        double rFp3 = 0.0;
        rFp3 += WIGNER_222_000 * c20*c20*c20;
        rFp3 -= (3.0*WIGNER_222_011) * c20 * (c21*c21 + c2n1*c2n1);
        rFp3 += (3.0*WIGNER_222_022) * c20 * (c22*c22 + c2n2*c2n2);
        rFp3 += (3.0*SQRT2_INV*WIGNER_222_112) * c22 * (c21*c21 - c2n1*c2n1);
        rFp3 += (6.0*SQRT2_INV*WIGNER_222_112) * c21*c2n1*c2n2;
        rFp[tIdxFP] = rFp3;
        ++tIdxFP;
        final double c10, c11, c1n1;
        if (aL3Cross) {
            /// l1 = l2 = 1, l3 = 2
            tIdx = (1+1)+aShift;
            c10  = aCnlm[tIdx  ];
            c11  = aCnlm[tIdx+1];
            c1n1 = aCnlm[tIdx-1];
            rFp3 = 0.0;
            rFp3 += WIGNER_112_000 * c10*c10*c20;
            rFp3 -= WIGNER_112_110 * c20 * (c11*c11 + c1n1*c1n1);
            rFp3 -= (2.0*WIGNER_112_011) * c10 * (c11*c21 + c1n1*c2n1);
            rFp3 += (SQRT2_INV*WIGNER_112_112) * c22 * (c11*c11 - c1n1*c1n1);
            rFp3 += (2.0*SQRT2_INV*WIGNER_112_112) * c11*c1n1*c2n2;
            rFp[tIdxFP] = rFp3;
            ++tIdxFP;
        } else {
            c10 = c11 = c1n1 = Double.NaN;
        }
        if (aL3Max == 2) return;
        final double c30, c31, c3n1, c32, c3n2, c33, c3n3;
        if (aL3Cross) {
            /// l1 = 2, l2 = l3 = 3
            tIdx = (3*3+3)+aShift;
            c30  = aCnlm[tIdx  ];
            c31  = aCnlm[tIdx+1];
            c3n1 = aCnlm[tIdx-1];
            c32  = aCnlm[tIdx+2];
            c3n2 = aCnlm[tIdx-2];
            c33  = aCnlm[tIdx+3];
            c3n3 = aCnlm[tIdx-3];
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
            rFp[tIdxFP] = rFp3;
            ++tIdxFP;
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
            rFp[tIdxFP] = rFp3;
            ++tIdxFP;
        } else {
            c30 = c31 = c3n1 = c32 = c3n2 = c33 = c3n3 = Double.NaN;
        }
        if (aL3Max == 3) return;
        /// l1 = l2 = l3 = 4
        tIdx = (4*4+4)+aShift;
        final double c40  = aCnlm[tIdx  ];
        final double c41  = aCnlm[tIdx+1];
        final double c4n1 = aCnlm[tIdx-1];
        final double c42  = aCnlm[tIdx+2];
        final double c4n2 = aCnlm[tIdx-2];
        final double c43  = aCnlm[tIdx+3];
        final double c4n3 = aCnlm[tIdx-3];
        final double c44  = aCnlm[tIdx+4];
        final double c4n4 = aCnlm[tIdx-4];
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
        rFp[tIdxFP] = rFp3;
        ++tIdxFP;
        if (aL3Cross) {
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
            rFp[tIdxFP] = rFp3;
            ++tIdxFP;
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
            rFp[tIdxFP] = rFp3;
            ++tIdxFP;
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
            rFp[tIdxFP] = rFp3;
            ++tIdxFP;
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
            rFp[tIdxFP] = rFp3;
            ++tIdxFP;
        }
    }
    protected static void cnlm2fpPxyz(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                      double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                      double @Nullable[] rFpPxCross, double @Nullable[] rFpPyCross, double @Nullable[] rFpPzCross,
                                      double aWt, int aLMax, int aL3Max, boolean aL3Cross, int aShift, int aShiftFP) {
        calL2_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz,
               rFpPx, rFpPy, rFpPz,
               rFpPxCross, rFpPyCross, rFpPzCross,
               aWt, aLMax, aShift, aShiftFP);
        calL3_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz,
               rFpPx, rFpPy, rFpPz,
               rFpPxCross, rFpPyCross, rFpPzCross,
               aWt, aLMax, aL3Max, aL3Cross, aShift, aShiftFP);
    }
    private static void putFpPxyz_(double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aSubFpPx, double aSubFpPy, double aSubFpPz, int aIdxFp) {
        rFpPx[aIdxFp] -= aSubFpPx;
        rFpPy[aIdxFp] -= aSubFpPy;
        rFpPz[aIdxFp] -= aSubFpPz;
        if (rFpPxCross != null) {
            rFpPxCross[aIdxFp] += aSubFpPx;
            rFpPyCross[aIdxFp] += aSubFpPy;
            rFpPzCross[aIdxFp] += aSubFpPz;
        }
    }
    private static void calL2_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                               double[] rFpPx, double[] rFpPy, double[] rFpPz,
                               double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                               double aWt, int aLMax, int aShift, int aShiftFP) {
        // l = 0
        int tIdx = aShift, tIdxFP = aShiftFP;
        double tCnl0 = aCnlm[tIdx];
        double tMul = aWt*(PI4+PI4);
        double subFpPx = tMul * (tCnl0*aCnlmPx[0]);
        double subFpPy = tMul * (tCnl0*aCnlmPy[0]);
        double subFpPz = tMul * (tCnl0*aCnlmPz[0]);
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tIdxFP);
        if (aLMax == 0) return;
        // l = 1
        tIdx = 1+aShift; tIdxFP = 1+aShiftFP;
        tCnl0 = aCnlm[tIdx]; double tCnl1 = aCnlm[tIdx+1], tCnl2 = aCnlm[tIdx+2];
        tMul = aWt*(PI4/3 + PI4/3);
        subFpPx = tMul * (tCnl0*aCnlmPx[1] + tCnl1*aCnlmPx[1+1] + tCnl2*aCnlmPx[1+2]);
        subFpPy = tMul * (tCnl0*aCnlmPy[1] + tCnl1*aCnlmPy[1+1] + tCnl2*aCnlmPy[1+2]);
        subFpPz = tMul * (tCnl0*aCnlmPz[1] + tCnl1*aCnlmPz[1+1] + tCnl2*aCnlmPz[1+2]);
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tIdxFP);
        if (aLMax == 1) return;
        // l = 2
        tIdx = 4+aShift; tIdxFP = 2+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; double tCnl3 = aCnlm[tIdx+3], tCnl4 = aCnlm[tIdx+4];
        tMul = aWt*(PI4/5 + PI4/5);
        subFpPx = tMul * (tCnl0*aCnlmPx[4] + tCnl1*aCnlmPx[4+1] + tCnl2*aCnlmPx[4+2] + tCnl3*aCnlmPx[4+3] + tCnl4*aCnlmPx[4+4]);
        subFpPy = tMul * (tCnl0*aCnlmPy[4] + tCnl1*aCnlmPy[4+1] + tCnl2*aCnlmPy[4+2] + tCnl3*aCnlmPy[4+3] + tCnl4*aCnlmPy[4+4]);
        subFpPz = tMul * (tCnl0*aCnlmPz[4] + tCnl1*aCnlmPz[4+1] + tCnl2*aCnlmPz[4+2] + tCnl3*aCnlmPz[4+3] + tCnl4*aCnlmPz[4+4]);
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tIdxFP);
        if (aLMax == 2) return;
        // l = 3
        tIdx = 9+aShift; tIdxFP = 3+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; double tCnl5 = aCnlm[tIdx+5], tCnl6 = aCnlm[tIdx+6];
        tMul = aWt*(PI4/7 + PI4/7);
        subFpPx = tMul * (tCnl0*aCnlmPx[9] + tCnl1*aCnlmPx[9+1] + tCnl2*aCnlmPx[9+2] + tCnl3*aCnlmPx[9+3] + tCnl4*aCnlmPx[9+4] + tCnl5*aCnlmPx[9+5] + tCnl6*aCnlmPx[9+6]);
        subFpPy = tMul * (tCnl0*aCnlmPy[9] + tCnl1*aCnlmPy[9+1] + tCnl2*aCnlmPy[9+2] + tCnl3*aCnlmPy[9+3] + tCnl4*aCnlmPy[9+4] + tCnl5*aCnlmPy[9+5] + tCnl6*aCnlmPy[9+6]);
        subFpPz = tMul * (tCnl0*aCnlmPz[9] + tCnl1*aCnlmPz[9+1] + tCnl2*aCnlmPz[9+2] + tCnl3*aCnlmPz[9+3] + tCnl4*aCnlmPz[9+4] + tCnl5*aCnlmPz[9+5] + tCnl6*aCnlmPz[9+6]);
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tIdxFP);
        if (aLMax == 3) return;
        // l = 4
        tIdx = 16+aShift; tIdxFP = 4+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; double tCnl7 = aCnlm[tIdx+7], tCnl8 = aCnlm[tIdx+8];
        tMul = aWt*(PI4/9 + PI4/9);
        subFpPx = tMul * (tCnl0*aCnlmPx[16] + tCnl1*aCnlmPx[16+1] + tCnl2*aCnlmPx[16+2] + tCnl3*aCnlmPx[16+3] + tCnl4*aCnlmPx[16+4] + tCnl5*aCnlmPx[16+5] + tCnl6*aCnlmPx[16+6] + tCnl7*aCnlmPx[16+7] + tCnl8*aCnlmPx[16+8]);
        subFpPy = tMul * (tCnl0*aCnlmPy[16] + tCnl1*aCnlmPy[16+1] + tCnl2*aCnlmPy[16+2] + tCnl3*aCnlmPy[16+3] + tCnl4*aCnlmPy[16+4] + tCnl5*aCnlmPy[16+5] + tCnl6*aCnlmPy[16+6] + tCnl7*aCnlmPy[16+7] + tCnl8*aCnlmPy[16+8]);
        subFpPz = tMul * (tCnl0*aCnlmPz[16] + tCnl1*aCnlmPz[16+1] + tCnl2*aCnlmPz[16+2] + tCnl3*aCnlmPz[16+3] + tCnl4*aCnlmPz[16+4] + tCnl5*aCnlmPz[16+5] + tCnl6*aCnlmPz[16+6] + tCnl7*aCnlmPz[16+7] + tCnl8*aCnlmPz[16+8]);
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tIdxFP);
        if (aLMax == 4) return;
        // l = 5
        tIdx = 25+aShift; tIdxFP = 5+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; tCnl7 = aCnlm[tIdx+7]; tCnl8 = aCnlm[tIdx+8]; double tCnl9 = aCnlm[tIdx+9], tCnl10 = aCnlm[tIdx+10];
        tMul = aWt*(PI4/11 + PI4/11);
        subFpPx = tMul * (tCnl0*aCnlmPx[25] + tCnl1*aCnlmPx[25+1] + tCnl2*aCnlmPx[25+2] + tCnl3*aCnlmPx[25+3] + tCnl4*aCnlmPx[25+4] + tCnl5*aCnlmPx[25+5] + tCnl6*aCnlmPx[25+6] + tCnl7*aCnlmPx[25+7] + tCnl8*aCnlmPx[25+8] + tCnl9*aCnlmPx[25+9] + tCnl10*aCnlmPx[25+10]);
        subFpPy = tMul * (tCnl0*aCnlmPy[25] + tCnl1*aCnlmPy[25+1] + tCnl2*aCnlmPy[25+2] + tCnl3*aCnlmPy[25+3] + tCnl4*aCnlmPy[25+4] + tCnl5*aCnlmPy[25+5] + tCnl6*aCnlmPy[25+6] + tCnl7*aCnlmPy[25+7] + tCnl8*aCnlmPy[25+8] + tCnl9*aCnlmPy[25+9] + tCnl10*aCnlmPy[25+10]);
        subFpPz = tMul * (tCnl0*aCnlmPz[25] + tCnl1*aCnlmPz[25+1] + tCnl2*aCnlmPz[25+2] + tCnl3*aCnlmPz[25+3] + tCnl4*aCnlmPz[25+4] + tCnl5*aCnlmPz[25+5] + tCnl6*aCnlmPz[25+6] + tCnl7*aCnlmPz[25+7] + tCnl8*aCnlmPz[25+8] + tCnl9*aCnlmPz[25+9] + tCnl10*aCnlmPz[25+10]);
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tIdxFP);
        if (aLMax == 5) return;
        // l = 6
        tIdx = 36+aShift; tIdxFP = 6+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; tCnl7 = aCnlm[tIdx+7]; tCnl8 = aCnlm[tIdx+8]; tCnl9 = aCnlm[tIdx+9]; tCnl10 = aCnlm[tIdx+10]; double tCnl11 = aCnlm[tIdx+11], tCnl12 = aCnlm[tIdx+12];
        tMul = aWt*(PI4/13 + PI4/13);
        subFpPx = tMul * (tCnl0*aCnlmPx[36] + tCnl1*aCnlmPx[36+1] + tCnl2*aCnlmPx[36+2] + tCnl3*aCnlmPx[36+3] + tCnl4*aCnlmPx[36+4] + tCnl5*aCnlmPx[36+5] + tCnl6*aCnlmPx[36+6] + tCnl7*aCnlmPx[36+7] + tCnl8*aCnlmPx[36+8] + tCnl9*aCnlmPx[36+9] + tCnl10*aCnlmPx[36+10] + tCnl11*aCnlmPx[36+11] + tCnl12*aCnlmPx[36+12]);
        subFpPy = tMul * (tCnl0*aCnlmPy[36] + tCnl1*aCnlmPy[36+1] + tCnl2*aCnlmPy[36+2] + tCnl3*aCnlmPy[36+3] + tCnl4*aCnlmPy[36+4] + tCnl5*aCnlmPy[36+5] + tCnl6*aCnlmPy[36+6] + tCnl7*aCnlmPy[36+7] + tCnl8*aCnlmPy[36+8] + tCnl9*aCnlmPy[36+9] + tCnl10*aCnlmPy[36+10] + tCnl11*aCnlmPy[36+11] + tCnl12*aCnlmPy[36+12]);
        subFpPz = tMul * (tCnl0*aCnlmPz[36] + tCnl1*aCnlmPz[36+1] + tCnl2*aCnlmPz[36+2] + tCnl3*aCnlmPz[36+3] + tCnl4*aCnlmPz[36+4] + tCnl5*aCnlmPz[36+5] + tCnl6*aCnlmPz[36+6] + tCnl7*aCnlmPz[36+7] + tCnl8*aCnlmPz[36+8] + tCnl9*aCnlmPz[36+9] + tCnl10*aCnlmPz[36+10] + tCnl11*aCnlmPz[36+11] + tCnl12*aCnlmPz[36+12]);
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tIdxFP);
        if (aLMax == 6) return;
        // 优化到 l = 6 主要是大部分只用到这个程度；在本地测试这个优化基本没效果了（可能需要 avx512 指令集更有效）
        // else
        for (int tL = 7; tL <= aLMax; ++tL) {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            final int tStart0 = tL*tL;
            final int tStart = tStart0 + aShift;
            final int tLen = tL+tL+1;
            final int tEnd = tStart+tLen;
            double rDotPx = 0.0, rDotPy = 0.0, rDotPz = 0.0;
            for (int i=tStart, j=tStart0; i < tEnd; ++i, ++j) {
                double tCnlm = aCnlm[i];
                rDotPx += tCnlm*aCnlmPx[j];
                rDotPy += tCnlm*aCnlmPy[j];
                rDotPz += tCnlm*aCnlmPz[j];
            }
            tMul = PI4/(double)tLen;
            tMul = aWt*(tMul+tMul);
            subFpPx = tMul * rDotPx;
            subFpPy = tMul * rDotPy;
            subFpPz = tMul * rDotPz;
            putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, subFpPx, subFpPy, subFpPz, tL+aShiftFP);
        }
    }
    
    private static void calL3_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                               double[] rFpPx, double[] rFpPy, double[] rFpPz,
                               double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                               double aWt, int aLMax, int aL3Max, boolean aL3Cross, int aShift, int aShiftFP) {
        // 过大的单一函数会阻止 JIT 优化，因此这里要拆分成多个函数
        if (aL3Max <= 1) return;
        int tIdxFP = aLMax+1+aShiftFP;
        calL3_222_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
        ++tIdxFP;
        if (aL3Cross) {
            calL3_112_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
            ++tIdxFP;
        }
        if (aL3Max == 2) return;
        if (aL3Cross) {
            calL3_233_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
            ++tIdxFP;
            calL3_123_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
            ++tIdxFP;
        }
        if (aL3Max == 3) return;
        calL3_444_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
        ++tIdxFP;
        if (aL3Cross) {
            calL3_224_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
            ++tIdxFP;
            calL3_334_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
            ++tIdxFP;
            calL3_244_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
            ++tIdxFP;
            calL3_134_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt, aShift, tIdxFP);
            ++tIdxFP;
        }
    }
    private static void calL3_222_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP = 2*2+2;
        final int tIdx = tIdxP+aShift;
        final double c20  = aCnlm[tIdx  ];
        final double c21  = aCnlm[tIdx+1];
        final double c2n1 = aCnlm[tIdx-1];
        final double c22  = aCnlm[tIdx+2];
        final double c2n2 = aCnlm[tIdx-2];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul1 = WIGNER_222_000 * c20*c20;
        double tMul2 = (-3.0*WIGNER_222_011)*(c21*c21 + c2n1*c2n1) + (3.0*WIGNER_222_022)*(c22*c22 + c2n2*c2n2);
        double tMul = 3.0*tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP];
        rFp3Py += tMul*aCnlmPy[tIdxP];
        rFp3Pz += tMul*aCnlmPz[tIdxP];
        
        tMul1 = (-6.0*WIGNER_222_011) * c20*c21  + (6.0*SQRT2_INV*WIGNER_222_112) * c22*c21;
        tMul2 = (-6.0*WIGNER_222_011) * c20*c2n1 - (6.0*SQRT2_INV*WIGNER_222_112) * c22*c2n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP+1] + tMul2*aCnlmPx[tIdxP-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP+1] + tMul2*aCnlmPy[tIdxP-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP+1] + tMul2*aCnlmPz[tIdxP-1];
        
        tMul1 = (6.0*WIGNER_222_022) * c20*c22;
        tMul2 = (6.0*WIGNER_222_022) * c20*c2n2;
        rFp3Px += tMul1*aCnlmPx[tIdxP+2] + tMul2*aCnlmPx[tIdxP-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP+2] + tMul2*aCnlmPy[tIdxP-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP+2] + tMul2*aCnlmPz[tIdxP-2];
        
        tMul = (3.0*SQRT2_INV*WIGNER_222_112) * (c21*c21 - c2n1*c2n1);
        rFp3Px += tMul*aCnlmPx[tIdxP+2];
        rFp3Py += tMul*aCnlmPy[tIdxP+2];
        rFp3Pz += tMul*aCnlmPz[tIdxP+2];
        
        tMul = (6.0*SQRT2_INV*WIGNER_222_112) * c2n1*c2n2;
        tMul1 = (6.0*SQRT2_INV*WIGNER_222_112) * c21*c2n2;
        tMul2 = (6.0*SQRT2_INV*WIGNER_222_112) * c21*c2n1;
        rFp3Px += tMul*aCnlmPx[tIdxP+1] + tMul1*aCnlmPx[tIdxP-1] + tMul2*aCnlmPx[tIdxP-2];
        rFp3Py += tMul*aCnlmPy[tIdxP+1] + tMul1*aCnlmPy[tIdxP-1] + tMul2*aCnlmPy[tIdxP-2];
        rFp3Pz += tMul*aCnlmPz[tIdxP+1] + tMul1*aCnlmPz[tIdxP-1] + tMul2*aCnlmPz[tIdxP-2];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_112_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP1 = 1+1;
        final int tIdxP2 = 2*2+2;
        final int tIdx1 = tIdxP1+aShift;
        final double c10  = aCnlm[tIdx1  ];
        final double c11  = aCnlm[tIdx1+1];
        final double c1n1 = aCnlm[tIdx1-1];
        final int tIdx2 = tIdxP2+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_112_000*c10*c10 - WIGNER_112_110*(c11*c11 + c1n1*c1n1);
        rFp3Px += tMul*aCnlmPx[tIdxP2];
        rFp3Py += tMul*aCnlmPy[tIdxP2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2];
        double tMul1 = (2.0*WIGNER_112_000) * c10*c20;
        double tMul2 = (-2.0*WIGNER_112_011) * (c11*c21 + c1n1*c2n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP1];
        rFp3Py += tMul*aCnlmPy[tIdxP1];
        rFp3Pz += tMul*aCnlmPz[tIdxP1];
        
        tMul1 = (-2.0*WIGNER_112_110)*c20*c11  - (2.0*WIGNER_112_011)*c10*c21  + (2.0*SQRT2_INV*WIGNER_112_112)*c22*c11;
        tMul2 = (-2.0*WIGNER_112_110)*c20*c1n1 - (2.0*WIGNER_112_011)*c10*c2n1 - (2.0*SQRT2_INV*WIGNER_112_112)*c22*c1n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP1+1] + tMul2*aCnlmPx[tIdxP1-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP1+1] + tMul2*aCnlmPy[tIdxP1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP1+1] + tMul2*aCnlmPz[tIdxP1-1];
        tMul1 = (-2.0*WIGNER_112_011) * c10*c11;
        tMul2 = (-2.0*WIGNER_112_011) * c10*c1n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        
        tMul = (SQRT2_INV*WIGNER_112_112) * (c11*c11 - c1n1*c1n1);
        rFp3Px += tMul*aCnlmPx[tIdxP2+2];
        rFp3Py += tMul*aCnlmPy[tIdxP2+2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2+2];
        
        tMul = (2.0*SQRT2_INV*WIGNER_112_112) * c1n1*c2n2;
        tMul1 = (2.0*SQRT2_INV*WIGNER_112_112) * c11*c2n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_112_112) * c11*c1n1;
        rFp3Px += tMul*aCnlmPx[tIdxP1+1] + tMul1*aCnlmPx[tIdxP1-1] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul*aCnlmPy[tIdxP1+1] + tMul1*aCnlmPy[tIdxP1-1] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul*aCnlmPz[tIdxP1+1] + tMul1*aCnlmPz[tIdxP1-1] + tMul2*aCnlmPz[tIdxP2-2];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_233_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP2 = 2*2+2;
        final int tIdxP3 = 3*3+3;
        final int tIdx2 = tIdxP2+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx3 = tIdxP3+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_233_000*c30*c30 - WIGNER_233_011*(c31*c31 + c3n1*c3n1) + WIGNER_233_022*(c32*c32 + c3n2*c3n2) - WIGNER_233_033*(c33*c33 + c3n3*c3n3);
        rFp3Px += tMul*aCnlmPx[tIdxP2];
        rFp3Py += tMul*aCnlmPy[tIdxP2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2];
        double tMul1 = (2.0*WIGNER_233_000) * c20*c30;
        double tMul2 = (-2.0*WIGNER_233_110)*(c21*c31 + c2n1*c3n1) + (2.0*WIGNER_233_220)*(c22*c32 + c2n2*c3n2);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP3];
        rFp3Py += tMul*aCnlmPy[tIdxP3];
        rFp3Pz += tMul*aCnlmPz[tIdxP3];
        
        tMul1 = (-2.0*WIGNER_233_011)*c20*c31  - (2.0*WIGNER_233_110)*c30*c21  + (2.0*SQRT2_INV*WIGNER_233_211)*c22*c31  + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c32 + c2n1*c3n2) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c33 + c2n2*c3n3);
        tMul2 = (-2.0*WIGNER_233_011)*c20*c3n1 - (2.0*WIGNER_233_110)*c30*c2n1 - (2.0*SQRT2_INV*WIGNER_233_211)*c22*c3n1 + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c3n2 - c2n1*c32) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c3n3 - c2n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+1] + tMul2*aCnlmPx[tIdxP3-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+1] + tMul2*aCnlmPy[tIdxP3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+1] + tMul2*aCnlmPz[tIdxP3-1];
        tMul1 = (2.0*WIGNER_233_022)*c20*c32  + (2.0*WIGNER_233_220)*c30*c22  + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c31 - c2n1*c3n1) - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c33 + c2n1*c3n3);
        tMul2 = (2.0*WIGNER_233_022)*c20*c3n2 + (2.0*WIGNER_233_220)*c30*c2n2 + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c3n1 + c2n1*c31) - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c3n3 - c2n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+2] + tMul2*aCnlmPx[tIdxP3-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+2] + tMul2*aCnlmPy[tIdxP3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+2] + tMul2*aCnlmPz[tIdxP3-2];
        tMul1 = (-2.0*WIGNER_233_033)*c20*c33  - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c32 - c2n1*c3n2) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c31 - c2n2*c3n1);
        tMul2 = (-2.0*WIGNER_233_033)*c20*c3n3 - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c3n2 + c2n1*c32) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c3n1 + c2n2*c31);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+3] + tMul2*aCnlmPx[tIdxP3-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+3] + tMul2*aCnlmPy[tIdxP3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+3] + tMul2*aCnlmPz[tIdxP3-3];
        
        tMul1 = (-2.0*WIGNER_233_110) * c30*c31;
        tMul2 = (-2.0*WIGNER_233_110) * c30*c3n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        tMul1 = (2.0*WIGNER_233_220) * c30*c32;
        tMul2 = (2.0*WIGNER_233_220) * c30*c3n2;
        rFp3Px += tMul1*aCnlmPx[tIdxP2+2] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+2] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+2] + tMul2*aCnlmPz[tIdxP2-2];
        
        tMul = (SQRT2_INV*WIGNER_233_211) * (c31*c31 - c3n1*c3n1);
        rFp3Px += tMul*aCnlmPx[tIdxP2+2];
        rFp3Py += tMul*aCnlmPy[tIdxP2+2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2+2];
        
        tMul = (2.0*SQRT2_INV*WIGNER_233_211) * c2n2*c3n1;
        tMul1 = (2.0*SQRT2_INV*WIGNER_233_211) * c31*c2n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_233_211) * c31*c3n1;
        rFp3Px += tMul*aCnlmPx[tIdxP3+1] + tMul1*aCnlmPx[tIdxP3-1] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul*aCnlmPy[tIdxP3+1] + tMul1*aCnlmPy[tIdxP3-1] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul*aCnlmPz[tIdxP3+1] + tMul1*aCnlmPz[tIdxP3-1] + tMul2*aCnlmPz[tIdxP2-2];
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_233_112)*(c31*c32 + c3n1*c3n2) - (2.0*SQRT2_INV*WIGNER_233_123)*(c32*c33 + c3n2*c3n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_233_112)*(c31*c3n2 - c3n1*c32) - (2.0*SQRT2_INV*WIGNER_233_123)*(c32*c3n3 - c3n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        tMul1 = (-2.0*SQRT2_INV*WIGNER_233_213)*(c31*c33 + c3n1*c3n3);
        tMul2 = (-2.0*SQRT2_INV*WIGNER_233_213)*(c31*c3n3 - c3n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+2] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+2] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+2] + tMul2*aCnlmPz[tIdxP2-2];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_123_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP1 = 1+1;
        final int tIdxP2 = 2*2+2;
        final int tIdxP3 = 3*3+3;
        final int tIdx1 = tIdxP1+aShift;
        final double c10  = aCnlm[tIdx1  ];
        final double c11  = aCnlm[tIdx1+1];
        final double c1n1 = aCnlm[tIdx1-1];
        final int tIdx2 = tIdxP2+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx3 = tIdxP3+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_123_000*c20*c30 - WIGNER_123_011*(c21*c31 + c2n1*c3n1) + WIGNER_123_022*(c22*c32 + c2n2*c3n2);
        rFp3Px += tMul*aCnlmPx[tIdxP1];
        rFp3Py += tMul*aCnlmPy[tIdxP1];
        rFp3Pz += tMul*aCnlmPz[tIdxP1];
        double tMul1 = WIGNER_123_000 * c10*c30;
        double tMul2 = (-WIGNER_123_101) * (c11*c31 + c1n1*c3n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP2];
        rFp3Py += tMul*aCnlmPy[tIdxP2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2];
        tMul1 = WIGNER_123_000 * c10*c20;
        tMul2 = (-WIGNER_123_110) * (c11*c21 + c1n1*c2n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP3];
        rFp3Py += tMul*aCnlmPy[tIdxP3];
        rFp3Pz += tMul*aCnlmPz[tIdxP3];
        
        tMul1 = (-WIGNER_123_011)*c10*c31  - WIGNER_123_110*c30*c11  + (SQRT2_INV*WIGNER_123_112)*(c11*c32 + c1n1*c3n2);
        tMul2 = (-WIGNER_123_011)*c10*c3n1 - WIGNER_123_110*c30*c1n1 + (SQRT2_INV*WIGNER_123_112)*(c11*c3n2 - c1n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        tMul1 = (-WIGNER_123_011)*c10*c21  - WIGNER_123_101*c20*c11  + (SQRT2_INV*WIGNER_123_121)*(c11*c22 + c1n1*c2n2);
        tMul2 = (-WIGNER_123_011)*c10*c2n1 - WIGNER_123_101*c20*c1n1 + (SQRT2_INV*WIGNER_123_121)*(c11*c2n2 - c1n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+1] + tMul2*aCnlmPx[tIdxP3-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+1] + tMul2*aCnlmPy[tIdxP3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+1] + tMul2*aCnlmPz[tIdxP3-1];
        tMul1 = (-WIGNER_123_101)*c20*c31  - WIGNER_123_110*c30*c21;
        tMul2 = (-WIGNER_123_101)*c20*c3n1 - WIGNER_123_110*c30*c2n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP1+1] + tMul2*aCnlmPx[tIdxP1-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP1+1] + tMul2*aCnlmPy[tIdxP1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP1+1] + tMul2*aCnlmPz[tIdxP1-1];
        tMul1 = WIGNER_123_022*c10*c32  + (SQRT2_INV*WIGNER_123_121)*(c11*c31 - c1n1*c3n1) - (SQRT2_INV*WIGNER_123_123)*(c11*c33 + c1n1*c3n3);
        tMul2 = WIGNER_123_022*c10*c3n2 + (SQRT2_INV*WIGNER_123_121)*(c11*c3n1 + c1n1*c31) - (SQRT2_INV*WIGNER_123_123)*(c11*c3n3 - c1n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+2] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+2] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+2] + tMul2*aCnlmPz[tIdxP2-2];
        tMul1 = WIGNER_123_022*c10*c22  + (SQRT2_INV*WIGNER_123_112)*(c11*c21 - c1n1*c2n1);
        tMul2 = WIGNER_123_022*c10*c2n2 + (SQRT2_INV*WIGNER_123_112)*(c11*c2n1 + c1n1*c21);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+2] + tMul2*aCnlmPx[tIdxP3-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+2] + tMul2*aCnlmPy[tIdxP3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+2] + tMul2*aCnlmPz[tIdxP3-2];
        
        tMul1 = (SQRT2_INV*WIGNER_123_112)*(c21*c32 + c2n1*c3n2) + (SQRT2_INV*WIGNER_123_121)*(c22*c31 + c2n2*c3n1) - (SQRT2_INV*WIGNER_123_123)*(c22*c33 + c2n2*c3n3);
        tMul2 = (SQRT2_INV*WIGNER_123_112)*(c21*c3n2 - c2n1*c32) + (SQRT2_INV*WIGNER_123_121)*(c2n2*c31 - c22*c3n1) - (SQRT2_INV*WIGNER_123_123)*(c22*c3n3 - c2n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP1+1] + tMul2*aCnlmPx[tIdxP1-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP1+1] + tMul2*aCnlmPy[tIdxP1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP1+1] + tMul2*aCnlmPz[tIdxP1-1];
        tMul1 = (-SQRT2_INV*WIGNER_123_123)*(c11*c22 - c1n1*c2n2);
        tMul2 = (-SQRT2_INV*WIGNER_123_123)*(c11*c2n2 + c1n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+3] + tMul2*aCnlmPx[tIdxP3-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+3] + tMul2*aCnlmPy[tIdxP3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+3] + tMul2*aCnlmPz[tIdxP3-3];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_444_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP = 4*4+4;
        final int tIdx = tIdxP+aShift;
        final double c40  = aCnlm[tIdx  ];
        final double c41  = aCnlm[tIdx+1];
        final double c4n1 = aCnlm[tIdx-1];
        final double c42  = aCnlm[tIdx+2];
        final double c4n2 = aCnlm[tIdx-2];
        final double c43  = aCnlm[tIdx+3];
        final double c4n3 = aCnlm[tIdx-3];
        final double c44  = aCnlm[tIdx+4];
        final double c4n4 = aCnlm[tIdx-4];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul1 = WIGNER_444_000 * c40*c40;
        double tMul2 = (-3.0*WIGNER_444_011)*(c41*c41 + c4n1*c4n1) + (3.0*WIGNER_444_022)*(c42*c42 + c4n2*c4n2) - (3.0*WIGNER_444_033)*(c43*c43 + c4n3*c4n3) + (3.0*WIGNER_444_044)*(c44*c44 + c4n4*c4n4);
        double tMul = 3.0*tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP];
        rFp3Py += tMul*aCnlmPy[tIdxP];
        rFp3Pz += tMul*aCnlmPz[tIdxP];
        
        tMul1 = (-6.0*WIGNER_444_011)*c40*c41  + (6.0*SQRT2_INV*WIGNER_444_112)*c42*c41;
        tMul2 = (-6.0*WIGNER_444_011)*c40*c4n1 - (6.0*SQRT2_INV*WIGNER_444_112)*c42*c4n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP+1] + tMul2*aCnlmPx[tIdxP-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP+1] + tMul2*aCnlmPy[tIdxP-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP+1] + tMul2*aCnlmPz[tIdxP-1];
        tMul1 = (6.0*WIGNER_444_022)*c40*c42  + (6.0*SQRT2_INV*WIGNER_444_224)*c44*c42  - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c43 + c4n1*c4n3);
        tMul2 = (6.0*WIGNER_444_022)*c40*c4n2 - (6.0*SQRT2_INV*WIGNER_444_224)*c44*c4n2 - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c4n3 - c4n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP+2] + tMul2*aCnlmPx[tIdxP-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP+2] + tMul2*aCnlmPy[tIdxP-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP+2] + tMul2*aCnlmPz[tIdxP-2];
        tMul1 = (-6.0*WIGNER_444_033)*c40*c43  - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c42 - c4n1*c4n2) + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c44 + c4n1*c4n4);
        tMul2 = (-6.0*WIGNER_444_033)*c40*c4n3 - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c4n2 + c4n1*c42) + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c4n4 - c4n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP+3] + tMul2*aCnlmPx[tIdxP-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP+3] + tMul2*aCnlmPy[tIdxP-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP+3] + tMul2*aCnlmPz[tIdxP-3];
        tMul1 = (6.0*WIGNER_444_044)*c40*c44  + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c43 - c4n1*c4n3);
        tMul2 = (6.0*WIGNER_444_044)*c40*c4n4 + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c4n3 + c4n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP+4] + tMul2*aCnlmPx[tIdxP-4];
        rFp3Py += tMul1*aCnlmPy[tIdxP+4] + tMul2*aCnlmPy[tIdxP-4];
        rFp3Pz += tMul1*aCnlmPz[tIdxP+4] + tMul2*aCnlmPz[tIdxP-4];
        
        tMul = (3.0*SQRT2_INV*WIGNER_444_112) * (c41*c41 - c4n1*c4n1);
        rFp3Px += tMul*aCnlmPx[tIdxP+2];
        rFp3Py += tMul*aCnlmPy[tIdxP+2];
        rFp3Pz += tMul*aCnlmPz[tIdxP+2];
        tMul = (6.0*SQRT2_INV*WIGNER_444_112) * c4n1*c4n2;
        tMul1 = (6.0*SQRT2_INV*WIGNER_444_112) * c41*c4n2;
        tMul2 = (6.0*SQRT2_INV*WIGNER_444_112) * c41*c4n1;
        rFp3Px += tMul*aCnlmPx[tIdxP+1] + tMul1*aCnlmPx[tIdxP-1] + tMul2*aCnlmPx[tIdxP-2];
        rFp3Py += tMul*aCnlmPy[tIdxP+1] + tMul1*aCnlmPy[tIdxP-1] + tMul2*aCnlmPy[tIdxP-2];
        rFp3Pz += tMul*aCnlmPz[tIdxP+1] + tMul1*aCnlmPz[tIdxP-1] + tMul2*aCnlmPz[tIdxP-2];
        
        tMul = (3.0*SQRT2_INV*WIGNER_444_224) * (c42*c42 - c4n2*c4n2);
        rFp3Px += tMul*aCnlmPx[tIdxP+4];
        rFp3Py += tMul*aCnlmPy[tIdxP+4];
        rFp3Pz += tMul*aCnlmPz[tIdxP+4];
        tMul = (6.0*SQRT2_INV*WIGNER_444_224) * c4n2*c4n4;
        tMul1 = (6.0*SQRT2_INV*WIGNER_444_224) * c42*c4n4;
        tMul2 = (6.0*SQRT2_INV*WIGNER_444_224) * c42*c4n2;
        rFp3Px += tMul*aCnlmPx[tIdxP+2] + tMul1*aCnlmPx[tIdxP-2] + tMul2*aCnlmPx[tIdxP-4];
        rFp3Py += tMul*aCnlmPy[tIdxP+2] + tMul1*aCnlmPy[tIdxP-2] + tMul2*aCnlmPy[tIdxP-4];
        rFp3Pz += tMul*aCnlmPz[tIdxP+2] + tMul1*aCnlmPz[tIdxP-2] + tMul2*aCnlmPz[tIdxP-4];
        
        tMul1 = (-6.0*SQRT2_INV*WIGNER_444_123) * (c42*c43 + c4n2*c4n3) + (6.0*SQRT2_INV*WIGNER_444_134) * (c43*c44 + c4n3*c4n4);
        tMul2 = (-6.0*SQRT2_INV*WIGNER_444_123) * (c42*c4n3 - c4n2*c43) + (6.0*SQRT2_INV*WIGNER_444_134) * (c43*c4n4 - c4n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP+1] + tMul2*aCnlmPx[tIdxP-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP+1] + tMul2*aCnlmPy[tIdxP-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP+1] + tMul2*aCnlmPz[tIdxP-1];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_224_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP2 = 2*2+2;
        final int tIdxP4 = 4*4+4;
        final int tIdx2 = tIdxP2+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx4 = tIdxP4+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_224_000*c20*c20 - WIGNER_224_110*(c21*c21 + c2n1*c2n1) + WIGNER_224_220*(c22*c22 + c2n2*c2n2);
        rFp3Px += tMul*aCnlmPx[tIdxP4];
        rFp3Py += tMul*aCnlmPy[tIdxP4];
        rFp3Pz += tMul*aCnlmPz[tIdxP4];
        double tMul1 = (2.0*WIGNER_224_000) * c20*c40;
        double tMul2 = (-2.0*WIGNER_224_011)*(c21*c41 + c2n1*c4n1) + (2.0*WIGNER_224_022)*(c22*c42 + c2n2*c4n2);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP2];
        rFp3Py += tMul*aCnlmPy[tIdxP2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2];
        
        tMul1 = (-2.0*WIGNER_224_110)*c40*c21  - (2.0*WIGNER_224_011)*c20*c41  + (2.0*SQRT2_INV*WIGNER_224_112)*c42*c21;
        tMul2 = (-2.0*WIGNER_224_110)*c40*c2n1 - (2.0*WIGNER_224_011)*c20*c4n1 - (2.0*SQRT2_INV*WIGNER_224_112)*c42*c2n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        tMul1 = (2.0*WIGNER_224_220)*c40*c22  + (2.0*WIGNER_224_022)*c20*c42  + (2.0*SQRT2_INV*WIGNER_224_224)*c44*c22  + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c41 - c2n1*c4n1) - (2.0*SQRT2_INV*WIGNER_224_123)*(c21*c43 + c2n1*c4n3);
        tMul2 = (2.0*WIGNER_224_220)*c40*c2n2 + (2.0*WIGNER_224_022)*c20*c4n2 - (2.0*SQRT2_INV*WIGNER_224_224)*c44*c2n2 + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c4n1 + c2n1*c41) - (2.0*SQRT2_INV*WIGNER_224_123)*(c21*c4n3 - c2n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+2] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+2] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+2] + tMul2*aCnlmPz[tIdxP2-2];
        tMul1 = (-2.0*WIGNER_224_011)*c20*c21  + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c22 + c2n1*c2n2);
        tMul2 = (-2.0*WIGNER_224_011)*c20*c2n1 + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c2n2 - c2n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+1] + tMul2*aCnlmPx[tIdxP4-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+1] + tMul2*aCnlmPy[tIdxP4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+1] + tMul2*aCnlmPz[tIdxP4-1];
        tMul1 = (2.0*WIGNER_224_022)*c20*c22;
        tMul2 = (2.0*WIGNER_224_022)*c20*c2n2;
        rFp3Px += tMul1*aCnlmPx[tIdxP4+2] + tMul2*aCnlmPx[tIdxP4-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+2] + tMul2*aCnlmPy[tIdxP4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+2] + tMul2*aCnlmPz[tIdxP4-2];
        
        tMul1 = (SQRT2_INV*WIGNER_224_112) * (c21*c21 - c2n1*c2n1);
        tMul2 = (SQRT2_INV*WIGNER_224_224) * (c22*c22 - c2n2*c2n2);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+2] + tMul2*aCnlmPx[tIdxP4+4];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+2] + tMul2*aCnlmPy[tIdxP4+4];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+2] + tMul2*aCnlmPz[tIdxP4+4];
        
        tMul = (2.0*SQRT2_INV*WIGNER_224_112) * c2n1*c4n2;
        tMul1 = (2.0*SQRT2_INV*WIGNER_224_112) * c21*c4n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_224_112) * c21*c2n1;
        rFp3Px += tMul*aCnlmPx[tIdxP2+1] + tMul1*aCnlmPx[tIdxP2-1] + tMul2*aCnlmPx[tIdxP4-2];
        rFp3Py += tMul*aCnlmPy[tIdxP2+1] + tMul1*aCnlmPy[tIdxP2-1] + tMul2*aCnlmPy[tIdxP4-2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2+1] + tMul1*aCnlmPz[tIdxP2-1] + tMul2*aCnlmPz[tIdxP4-2];
        tMul = (2.0*SQRT2_INV*WIGNER_224_224) * c2n2*c4n4;
        tMul1 = (2.0*SQRT2_INV*WIGNER_224_224) * c22*c4n4;
        tMul2 = (2.0*SQRT2_INV*WIGNER_224_224) * c22*c2n2;
        rFp3Px += tMul*aCnlmPx[tIdxP2+2] + tMul1*aCnlmPx[tIdxP2-2] + tMul2*aCnlmPx[tIdxP4-4];
        rFp3Py += tMul*aCnlmPy[tIdxP2+2] + tMul1*aCnlmPy[tIdxP2-2] + tMul2*aCnlmPy[tIdxP4-4];
        rFp3Pz += tMul*aCnlmPz[tIdxP2+2] + tMul1*aCnlmPz[tIdxP2-2] + tMul2*aCnlmPz[tIdxP4-4];
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_224_121)*(c22*c41 + c2n2*c4n1) - (2.0*SQRT2_INV*WIGNER_224_123)*(c22*c43 + c2n2*c4n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_224_121)*(c2n2*c41 - c22*c4n1) - (2.0*SQRT2_INV*WIGNER_224_123)*(c22*c4n3 - c2n2*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        tMul1 = (-2.0*SQRT2_INV*WIGNER_224_123)*(c21*c22 - c2n1*c2n2);
        tMul2 = (-2.0*SQRT2_INV*WIGNER_224_123)*(c21*c2n2 + c2n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+3] + tMul2*aCnlmPx[tIdxP4-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+3] + tMul2*aCnlmPy[tIdxP4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+3] + tMul2*aCnlmPz[tIdxP4-3];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_334_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP3 = 3*3+3;
        final int tIdxP4 = 4*4+4;
        final int tIdx3 = tIdxP3+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        final int tIdx4 = tIdxP4+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_334_000*c30*c30 - WIGNER_334_110*(c31*c31 + c3n1*c3n1) + WIGNER_334_220*(c32*c32 + c3n2*c3n2) - WIGNER_334_330*(c33*c33 + c3n3*c3n3);
        rFp3Px += tMul*aCnlmPx[tIdxP4];
        rFp3Py += tMul*aCnlmPy[tIdxP4];
        rFp3Pz += tMul*aCnlmPz[tIdxP4];
        double tMul1 = (2.0*WIGNER_334_000) * c30*c40;
        double tMul2 = (-2.0*WIGNER_334_011)*(c31*c41 + c3n1*c4n1) + (2.0*WIGNER_334_022)*(c32*c42 + c3n2*c4n2) - (2.0*WIGNER_334_033)*(c33*c43 + c3n3*c4n3);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP3];
        rFp3Py += tMul*aCnlmPy[tIdxP3];
        rFp3Pz += tMul*aCnlmPz[tIdxP3];
        
        tMul1 = (-2.0*WIGNER_334_110)*c40*c31  - (2.0*WIGNER_334_011)*c30*c41  + (2.0*SQRT2_INV*WIGNER_334_112)*c42*c31;
        tMul2 = (-2.0*WIGNER_334_110)*c40*c3n1 - (2.0*WIGNER_334_011)*c30*c4n1 - (2.0*SQRT2_INV*WIGNER_334_112)*c42*c3n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP3+1] + tMul2*aCnlmPx[tIdxP3-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+1] + tMul2*aCnlmPy[tIdxP3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+1] + tMul2*aCnlmPz[tIdxP3-1];
        tMul1 = (2.0*WIGNER_334_220)*c40*c32  + (2.0*WIGNER_334_022)*c30*c42  + (2.0*SQRT2_INV*WIGNER_334_224)*c44*c32  + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c41 - c3n1*c4n1) - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c43 + c3n1*c4n3);
        tMul2 = (2.0*WIGNER_334_220)*c40*c3n2 + (2.0*WIGNER_334_022)*c30*c4n2 - (2.0*SQRT2_INV*WIGNER_334_224)*c44*c3n2 + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c4n1 + c3n1*c41) - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c4n3 - c3n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+2] + tMul2*aCnlmPx[tIdxP3-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+2] + tMul2*aCnlmPy[tIdxP3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+2] + tMul2*aCnlmPz[tIdxP3-2];
        tMul1 = (-2.0*WIGNER_334_330)*c40*c33  - (2.0*WIGNER_334_033)*c30*c43  - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c42 - c3n1*c4n2) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c41 - c3n2*c4n1) + (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c44 + c3n1*c4n4);
        tMul2 = (-2.0*WIGNER_334_330)*c40*c3n3 - (2.0*WIGNER_334_033)*c30*c4n3 - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c4n2 + c3n1*c42) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c4n1 + c3n2*c41) + (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c4n4 - c3n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+3] + tMul2*aCnlmPx[tIdxP3-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+3] + tMul2*aCnlmPy[tIdxP3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+3] + tMul2*aCnlmPz[tIdxP3-3];
        tMul1 = (-2.0*WIGNER_334_011)*c30*c31  + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c32 + c3n1*c3n2) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c33 + c3n2*c3n3);
        tMul2 = (-2.0*WIGNER_334_011)*c30*c3n1 + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c3n2 - c3n1*c32) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c3n3 - c3n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+1] + tMul2*aCnlmPx[tIdxP4-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+1] + tMul2*aCnlmPy[tIdxP4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+1] + tMul2*aCnlmPz[tIdxP4-1];
        tMul1 = (2.0*WIGNER_334_022) * c30*c32  - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c33 + c3n1*c3n3);
        tMul2 = (2.0*WIGNER_334_022) * c30*c3n2 - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c3n3 - c3n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+2] + tMul2*aCnlmPx[tIdxP4-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+2] + tMul2*aCnlmPy[tIdxP4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+2] + tMul2*aCnlmPz[tIdxP4-2];
        tMul1 = (-2.0*WIGNER_334_033)*c30*c33  - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c32 - c3n1*c3n2);
        tMul2 = (-2.0*WIGNER_334_033)*c30*c3n3 - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c3n2 + c3n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+3] + tMul2*aCnlmPx[tIdxP4-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+3] + tMul2*aCnlmPy[tIdxP4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+3] + tMul2*aCnlmPz[tIdxP4-3];
        
        tMul1 = (SQRT2_INV*WIGNER_334_112) * (c31*c31 - c3n1*c3n1);
        tMul2 = (SQRT2_INV*WIGNER_334_224) * (c32*c32 - c3n2*c3n2);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+2] + tMul2*aCnlmPx[tIdxP4+4];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+2] + tMul2*aCnlmPy[tIdxP4+4];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+2] + tMul2*aCnlmPz[tIdxP4+4];
        
        tMul = (2.0*SQRT2_INV*WIGNER_334_112) * c3n1*c4n2;
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_112) * c31*c4n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_112) * c31*c3n1;
        rFp3Px += tMul*aCnlmPx[tIdxP3+1] + tMul1*aCnlmPx[tIdxP3-1] + tMul2*aCnlmPx[tIdxP4-2];
        rFp3Py += tMul*aCnlmPy[tIdxP3+1] + tMul1*aCnlmPy[tIdxP3-1] + tMul2*aCnlmPy[tIdxP4-2];
        rFp3Pz += tMul*aCnlmPz[tIdxP3+1] + tMul1*aCnlmPz[tIdxP3-1] + tMul2*aCnlmPz[tIdxP4-2];
        tMul = (2.0*SQRT2_INV*WIGNER_334_224) * c3n2*c4n4;
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_224) * c32*c4n4;
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_224) * c32*c3n2;
        rFp3Px += tMul*aCnlmPx[tIdxP3+2] + tMul1*aCnlmPx[tIdxP3-2] + tMul2*aCnlmPx[tIdxP4-4];
        rFp3Py += tMul*aCnlmPy[tIdxP3+2] + tMul1*aCnlmPy[tIdxP3-2] + tMul2*aCnlmPy[tIdxP4-4];
        rFp3Pz += tMul*aCnlmPz[tIdxP3+2] + tMul1*aCnlmPz[tIdxP3-2] + tMul2*aCnlmPz[tIdxP4-4];
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_121)*(c32*c41 + c3n2*c4n1) - (2.0*SQRT2_INV*WIGNER_334_123)*(c32*c43 + c3n2*c4n3) - (2.0*SQRT2_INV*WIGNER_334_132)*(c33*c42 + c3n3*c4n2) + (2.0*SQRT2_INV*WIGNER_334_134)*(c33*c44 + c3n3*c4n4);
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_121)*(c3n2*c41 - c32*c4n1) - (2.0*SQRT2_INV*WIGNER_334_123)*(c32*c4n3 - c3n2*c43) - (2.0*SQRT2_INV*WIGNER_334_132)*(c3n3*c42 - c33*c4n2) + (2.0*SQRT2_INV*WIGNER_334_134)*(c33*c4n4 - c3n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+1] + tMul2*aCnlmPx[tIdxP3-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+1] + tMul2*aCnlmPy[tIdxP3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+1] + tMul2*aCnlmPz[tIdxP3-1];
        tMul1 = (-2.0*SQRT2_INV*WIGNER_334_231)*(c33*c41 + c3n3*c4n1);
        tMul2 = (-2.0*SQRT2_INV*WIGNER_334_231)*(c3n3*c41 - c33*c4n1);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+2] + tMul2*aCnlmPx[tIdxP3-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+2] + tMul2*aCnlmPy[tIdxP3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+2] + tMul2*aCnlmPz[tIdxP3-2];
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c33 - c3n1*c3n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c3n3 + c3n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+4] + tMul2*aCnlmPx[tIdxP4-4];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+4] + tMul2*aCnlmPy[tIdxP4-4];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+4] + tMul2*aCnlmPz[tIdxP4-4];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_244_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP2 = 2*2+2;
        final int tIdxP4 = 4*4+4;
        final int tIdx2 = tIdxP2+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx4 = tIdxP4+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_244_000*c40*c40 - WIGNER_244_011*(c41*c41 + c4n1*c4n1) + WIGNER_244_022*(c42*c42 + c4n2*c4n2) - WIGNER_244_033*(c43*c43 + c4n3*c4n3) + WIGNER_244_044*(c44*c44 + c4n4*c4n4);
        rFp3Px += tMul*aCnlmPx[tIdxP2];
        rFp3Py += tMul*aCnlmPy[tIdxP2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2];
        double tMul1 = (2.0*WIGNER_244_000) * c20*c40;
        double tMul2 = (-2.0*WIGNER_244_110)*(c21*c41 + c2n1*c4n1) + (2.0*WIGNER_244_220)*(c22*c42 + c2n2*c4n2);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP4];
        rFp3Py += tMul*aCnlmPy[tIdxP4];
        rFp3Pz += tMul*aCnlmPz[tIdxP4];
        
        tMul1 = (-2.0*WIGNER_244_011)*c20*c41  - (2.0*WIGNER_244_110)*c40*c21  + (2.0*SQRT2_INV*WIGNER_244_211)*c22*c41  + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c42 + c2n1*c4n2) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c43 + c2n2*c4n3);
        tMul2 = (-2.0*WIGNER_244_011)*c20*c4n1 - (2.0*WIGNER_244_110)*c40*c2n1 - (2.0*SQRT2_INV*WIGNER_244_211)*c22*c4n1 + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c4n2 - c2n1*c42) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c4n3 - c2n2*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+1] + tMul2*aCnlmPx[tIdxP4-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+1] + tMul2*aCnlmPy[tIdxP4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+1] + tMul2*aCnlmPz[tIdxP4-1];
        tMul1 = (2.0*WIGNER_244_022)*c20*c42  + (2.0*WIGNER_244_220)*c40*c22  + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c41 - c2n1*c4n1) + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c44 + c2n2*c4n4) - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c43 + c2n1*c4n3);
        tMul2 = (2.0*WIGNER_244_022)*c20*c4n2 + (2.0*WIGNER_244_220)*c40*c2n2 + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c4n1 + c2n1*c41) + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c4n4 - c2n2*c44) - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c4n3 - c2n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+2] + tMul2*aCnlmPx[tIdxP4-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+2] + tMul2*aCnlmPy[tIdxP4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+2] + tMul2*aCnlmPz[tIdxP4-2];
        tMul1 = (-2.0*WIGNER_244_033)*c20*c43  - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c42 - c2n1*c4n2) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c41 - c2n2*c4n1) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c44 + c2n1*c4n4);
        tMul2 = (-2.0*WIGNER_244_033)*c20*c4n3 - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c4n2 + c2n1*c42) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c4n1 + c2n2*c41) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c4n4 - c2n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+3] + tMul2*aCnlmPx[tIdxP4-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+3] + tMul2*aCnlmPy[tIdxP4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+3] + tMul2*aCnlmPz[tIdxP4-3];
        tMul1 = (2.0*WIGNER_244_044)*c20*c44  + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c42 - c2n2*c4n2) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c43 - c2n1*c4n3);
        tMul2 = (2.0*WIGNER_244_044)*c20*c4n4 + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c4n2 + c2n2*c42) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c4n3 + c2n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+4] + tMul2*aCnlmPx[tIdxP4-4];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+4] + tMul2*aCnlmPy[tIdxP4-4];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+4] + tMul2*aCnlmPz[tIdxP4-4];
        tMul1 = (-2.0*WIGNER_244_110)*c40*c41;
        tMul2 = (-2.0*WIGNER_244_110)*c40*c4n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        tMul1 = (2.0*WIGNER_244_220)*c40*c42;
        tMul2 = (2.0*WIGNER_244_220)*c40*c4n2;
        rFp3Px += tMul1*aCnlmPx[tIdxP2+2] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+2] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+2] + tMul2*aCnlmPz[tIdxP2-2];
        
        tMul = (SQRT2_INV*WIGNER_244_211) * (c41*c41 - c4n1*c4n1);
        rFp3Px += tMul*aCnlmPx[tIdxP2+2];
        rFp3Py += tMul*aCnlmPy[tIdxP2+2];
        rFp3Pz += tMul*aCnlmPz[tIdxP2+2];
        tMul = (2.0*SQRT2_INV*WIGNER_244_211) * c2n2*c4n1;
        tMul1 = (2.0*SQRT2_INV*WIGNER_244_211) * c41*c2n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_244_211) * c41*c4n1;
        rFp3Px += tMul*aCnlmPx[tIdxP4+1] + tMul1*aCnlmPx[tIdxP4-1] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul*aCnlmPy[tIdxP4+1] + tMul1*aCnlmPy[tIdxP4-1] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul*aCnlmPz[tIdxP4+1] + tMul1*aCnlmPz[tIdxP4-1] + tMul2*aCnlmPz[tIdxP2-2];
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_244_112)*(c41*c42 + c4n1*c4n2) - (2.0*SQRT2_INV*WIGNER_244_123)*(c42*c43 + c4n2*c4n3) + (2.0*SQRT2_INV*WIGNER_244_134)*(c43*c44 + c4n3*c4n4);
        tMul2 = (2.0*SQRT2_INV*WIGNER_244_112)*(c41*c4n2 - c4n1*c42) - (2.0*SQRT2_INV*WIGNER_244_123)*(c42*c4n3 - c4n2*c43) + (2.0*SQRT2_INV*WIGNER_244_134)*(c43*c4n4 - c4n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+1] + tMul2*aCnlmPx[tIdxP2-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+1] + tMul2*aCnlmPy[tIdxP2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+1] + tMul2*aCnlmPz[tIdxP2-1];
        tMul1 = (2.0*SQRT2_INV*WIGNER_244_224)*(c42*c44 + c4n2*c4n4) - (2.0*SQRT2_INV*WIGNER_244_213)*(c41*c43 + c4n1*c4n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_244_224)*(c42*c4n4 - c4n2*c44) - (2.0*SQRT2_INV*WIGNER_244_213)*(c41*c4n3 - c4n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP2+2] + tMul2*aCnlmPx[tIdxP2-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP2+2] + tMul2*aCnlmPy[tIdxP2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP2+2] + tMul2*aCnlmPz[tIdxP2-2];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
    private static void calL3_134_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   double @Nullable[] rFpPxCross, double[] rFpPyCross, double[] rFpPzCross,
                                   double aWt, int aShift, int aIdxFP) {
        final int tIdxP1 = 1+1;
        final int tIdxP3 = 3*3+3;
        final int tIdxP4 = 4*4+4;
        final int tIdx1 = tIdxP1+aShift;
        final double c10  = aCnlm[tIdx1  ];
        final double c11  = aCnlm[tIdx1+1];
        final double c1n1 = aCnlm[tIdx1-1];
        final int tIdx3 = tIdxP3+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        final int tIdx4 = tIdxP4+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_134_000*c30*c40 - WIGNER_134_011*(c31*c41 + c3n1*c4n1) + WIGNER_134_022*(c32*c42 + c3n2*c4n2) - WIGNER_134_033*(c33*c43 + c3n3*c4n3);
        rFp3Px += tMul*aCnlmPx[tIdxP1];
        rFp3Py += tMul*aCnlmPy[tIdxP1];
        rFp3Pz += tMul*aCnlmPz[tIdxP1];
        double tMul1 = WIGNER_134_000 * c10*c30;
        double tMul2 = (-WIGNER_134_110)*(c11*c31 + c1n1*c3n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP4];
        rFp3Py += tMul*aCnlmPy[tIdxP4];
        rFp3Pz += tMul*aCnlmPz[tIdxP4];
        tMul1 = WIGNER_134_000 * c10*c40;
        tMul2 = (-WIGNER_134_101)*(c11*c41 + c1n1*c4n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdxP3];
        rFp3Py += tMul*aCnlmPy[tIdxP3];
        rFp3Pz += tMul*aCnlmPz[tIdxP3];
        
        tMul1 = (-WIGNER_134_011)*c10*c41  - WIGNER_134_110*c40*c11  + (SQRT2_INV*WIGNER_134_112)*(c11*c42 + c1n1*c4n2);
        tMul2 = (-WIGNER_134_011)*c10*c4n1 - WIGNER_134_110*c40*c1n1 + (SQRT2_INV*WIGNER_134_112)*(c11*c4n2 - c1n1*c42);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+1] + tMul2*aCnlmPx[tIdxP3-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+1] + tMul2*aCnlmPy[tIdxP3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+1] + tMul2*aCnlmPz[tIdxP3-1];
        tMul1 = WIGNER_134_022*c10*c42  + (SQRT2_INV*WIGNER_134_121)*(c11*c41 - c1n1*c4n1) - (SQRT2_INV*WIGNER_134_123)*(c11*c43 + c1n1*c4n3);
        tMul2 = WIGNER_134_022*c10*c4n2 + (SQRT2_INV*WIGNER_134_121)*(c11*c4n1 + c1n1*c41) - (SQRT2_INV*WIGNER_134_123)*(c11*c4n3 - c1n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+2] + tMul2*aCnlmPx[tIdxP3-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+2] + tMul2*aCnlmPy[tIdxP3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+2] + tMul2*aCnlmPz[tIdxP3-2];
        tMul1 = (-WIGNER_134_033)*c10*c43  - (SQRT2_INV*WIGNER_134_132)*(c11*c42 - c1n1*c4n2) + (SQRT2_INV*WIGNER_134_134)*(c11*c44 + c1n1*c4n4);
        tMul2 = (-WIGNER_134_033)*c10*c4n3 - (SQRT2_INV*WIGNER_134_132)*(c11*c4n2 + c1n1*c42) + (SQRT2_INV*WIGNER_134_134)*(c11*c4n4 - c1n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP3+3] + tMul2*aCnlmPx[tIdxP3-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP3+3] + tMul2*aCnlmPy[tIdxP3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP3+3] + tMul2*aCnlmPz[tIdxP3-3];
        tMul1 = (-WIGNER_134_011)*c10*c31  - WIGNER_134_101*c30*c11  + (SQRT2_INV*WIGNER_134_121)*(c11*c32 + c1n1*c3n2);
        tMul2 = (-WIGNER_134_011)*c10*c3n1 - WIGNER_134_101*c30*c1n1 + (SQRT2_INV*WIGNER_134_121)*(c11*c3n2 - c1n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+1] + tMul2*aCnlmPx[tIdxP4-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+1] + tMul2*aCnlmPy[tIdxP4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+1] + tMul2*aCnlmPz[tIdxP4-1];
        tMul1 = WIGNER_134_022*c10*c32  + (SQRT2_INV*WIGNER_134_112)*(c11*c31 - c1n1*c3n1) - (SQRT2_INV*WIGNER_134_132)*(c11*c33 + c1n1*c3n3);
        tMul2 = WIGNER_134_022*c10*c3n2 + (SQRT2_INV*WIGNER_134_112)*(c11*c3n1 + c1n1*c31) - (SQRT2_INV*WIGNER_134_132)*(c11*c3n3 - c1n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+2] + tMul2*aCnlmPx[tIdxP4-2];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+2] + tMul2*aCnlmPy[tIdxP4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+2] + tMul2*aCnlmPz[tIdxP4-2];
        tMul1 = (-WIGNER_134_033)*c10*c33  - (SQRT2_INV*WIGNER_134_123)*(c11*c32 - c1n1*c3n2);
        tMul2 = (-WIGNER_134_033)*c10*c3n3 - (SQRT2_INV*WIGNER_134_123)*(c11*c3n2 + c1n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+3] + tMul2*aCnlmPx[tIdxP4-3];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+3] + tMul2*aCnlmPy[tIdxP4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+3] + tMul2*aCnlmPz[tIdxP4-3];
        tMul1 = (-WIGNER_134_110)*c40*c31  - WIGNER_134_101*c30*c41 ;
        tMul2 = (-WIGNER_134_110)*c40*c3n1 - WIGNER_134_101*c30*c4n1;
        rFp3Px += tMul1*aCnlmPx[tIdxP1+1] + tMul2*aCnlmPx[tIdxP1-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP1+1] + tMul2*aCnlmPy[tIdxP1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP1+1] + tMul2*aCnlmPz[tIdxP1-1];
        
        tMul1 = (SQRT2_INV*WIGNER_134_112)*(c31*c42 + c3n1*c4n2) + (SQRT2_INV*WIGNER_134_121)*(c32*c41 + c3n2*c4n1) - (SQRT2_INV*WIGNER_134_123)*(c32*c43 + c3n2*c4n3) - (SQRT2_INV*WIGNER_134_132)*(c33*c42 + c3n3*c4n2) + (SQRT2_INV*WIGNER_134_134)*(c33*c44 + c3n3*c4n4);
        tMul2 = (SQRT2_INV*WIGNER_134_112)*(c31*c4n2 - c3n1*c42) + (SQRT2_INV*WIGNER_134_121)*(c3n2*c41 - c32*c4n1) - (SQRT2_INV*WIGNER_134_123)*(c32*c4n3 - c3n2*c43) - (SQRT2_INV*WIGNER_134_132)*(c3n3*c42 - c33*c4n2) + (SQRT2_INV*WIGNER_134_134)*(c33*c4n4 - c3n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdxP1+1] + tMul2*aCnlmPx[tIdxP1-1];
        rFp3Py += tMul1*aCnlmPy[tIdxP1+1] + tMul2*aCnlmPy[tIdxP1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdxP1+1] + tMul2*aCnlmPz[tIdxP1-1];
        tMul1 = (SQRT2_INV*WIGNER_134_134)*(c11*c33 - c1n1*c3n3);
        tMul2 = (SQRT2_INV*WIGNER_134_134)*(c11*c3n3 + c1n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdxP4+4] + tMul2*aCnlmPx[tIdxP4-4];
        rFp3Py += tMul1*aCnlmPy[tIdxP4+4] + tMul2*aCnlmPy[tIdxP4-4];
        rFp3Pz += tMul1*aCnlmPz[tIdxP4+4] + tMul2*aCnlmPz[tIdxP4-4];
        putFpPxyz_(rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross, aWt*rFp3Px, aWt*rFp3Py, aWt*rFp3Pz, aIdxFP);
    }
}
