package jsex.nnap.basis;

import com.google.common.collect.Lists;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
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
    private @Nullable RowMatrix mCnlm = null, mCnlmPx = null, mCnlmPy = null, mCnlmPz = null;
    @NotNull RowMatrix bufCnlm(boolean aClear) {if (mCnlm==null) {mCnlm = MatrixCache.getMatRow(sizeN(), lmAll_());} if (aClear) {mCnlm.fill(0.0);} return mCnlm;}
    @NotNull RowMatrix bufCnlmPx(boolean aClear) {if (mCnlmPx==null) {mCnlmPx = MatrixCache.getMatRow(sizeN(), lmAll_());} if (aClear) {mCnlmPx.fill(0.0);} return mCnlmPx;}
    @NotNull RowMatrix bufCnlmPy(boolean aClear) {if (mCnlmPy==null) {mCnlmPy = MatrixCache.getMatRow(sizeN(), lmAll_());} if (aClear) {mCnlmPy.fill(0.0);} return mCnlmPy;}
    @NotNull RowMatrix bufCnlmPz(boolean aClear) {if (mCnlmPz==null) {mCnlmPz = MatrixCache.getMatRow(sizeN(), lmAll_());} if (aClear) {mCnlmPz.fill(0.0);} return mCnlmPz;}
    
    private final List<RowMatrix> mCnlmPxAll = new ArrayList<>(), mCnlmPyAll = new ArrayList<>(), mCnlmPzAll = new ArrayList<>();
    @NotNull RowMatrix bufCnlmPxAll(int i, boolean aClear) {while (mCnlmPxAll.size()<=i) {mCnlmPxAll.add(MatrixCache.getMatRow(sizeN(), lmAll_()));} RowMatrix tCnlmPx = mCnlmPxAll.get(i); if (aClear) {tCnlmPx.fill(0.0);} return tCnlmPx;}
    @NotNull RowMatrix bufCnlmPyAll(int i, boolean aClear) {while (mCnlmPyAll.size()<=i) {mCnlmPyAll.add(MatrixCache.getMatRow(sizeN(), lmAll_()));} RowMatrix tCnlmPy = mCnlmPyAll.get(i); if (aClear) {tCnlmPy.fill(0.0);} return tCnlmPy;}
    @NotNull RowMatrix bufCnlmPzAll(int i, boolean aClear) {while (mCnlmPzAll.size()<=i) {mCnlmPzAll.add(MatrixCache.getMatRow(sizeN(), lmAll_()));} RowMatrix tCnlmPz = mCnlmPzAll.get(i); if (aClear) {tCnlmPz.fill(0.0);} return tCnlmPz;}
    
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
    
    @Override public void shutdown() {
        if (mCnlm != null) MatrixCache.returnMat(mCnlm);
        if (mCnlmPx != null) MatrixCache.returnMat(mCnlmPx);
        if (mCnlmPy != null) MatrixCache.returnMat(mCnlmPy);
        if (mCnlmPz != null) MatrixCache.returnMat(mCnlmPz);
        for (RowMatrix cnlmPzSub : mCnlmPzAll) MatrixCache.returnMat(cnlmPzSub);
        for (RowMatrix cnlmPySub : mCnlmPyAll) MatrixCache.returnMat(cnlmPySub);
        for (RowMatrix cnlmPxSub : mCnlmPxAll) MatrixCache.returnMat(cnlmPxSub);
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
    }
    
    /**
     * {@inheritDoc}
     * @param aNL 近邻列表遍历器
     * @return {@inheritDoc}
     */
    @Override public Vector eval(IDxyzTypeIterable aNL) {
        final int tSizeN = sizeN();
        final RowMatrix rFingerPrint = MatrixCache.getMatRow(tSizeN, sizeL());
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final RowMatrix cnlm = bufCnlm(true);
        // 缓存 Rn 数组
        final IVector tRn = bufRn(false);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final IVector tY = bufY(false);
        
        // 遍历近邻计算 Ylm, Rn, fc
        final int tLMax = lmax_();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= mRCut) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            
            // 计算种类的权重
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc
            double fc = MathEX.Fast.powFast(1.0 - MathEX.Fast.pow2(dis/mRCut), 4);
            // 统一遍历一次计算 Rn
            final double tX = 1.0 - 2.0*dis/mRCut;
            tRn.fill(n -> MathEX.Func.chebyshev(n, tX));
            
            // 遍历求 n，l 的情况；现在采用实球谐函数进行计算
            MathEX.Func.realSphericalHarmonicsFull2DestXYZDis_(tLMax, dx, dy, dz, dis, tY);
            for (int tN = 0; tN <= mNMax; ++tN) {
                // 现在统一使用 mplus2this 实现这个操作
                double tMul = fc * tRn.get(tN);
                cnlm.row(tN).operation().mplus2this(tY, tMul);
                if (mTypeNum > 1) cnlm.row(tN+mNMax+1).operation().mplus2this(tY, wt*tMul);
            }
        });
        // 做标量积消去 m 项，得到此原子的 FP
        for (int tN = 0; tN < tSizeN; ++tN) {
            // 计算二阶部分，直接这样内积即可
            for (int tL = 0; tL <= mLMax; ++tL) {
                // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
                int tStart = tL*tL;
                int tLen = tL+tL+1;
                rFingerPrint.set(tN, tL, (4.0*PI/(double)tLen) * cnlm.row(tN).subVec(tStart, tStart+tLen).operation().dot());
            }
            // 计算三阶部分，直接手动计算
            if (mL3Max <= 1) continue;
            /// l1 = l2 = l3 = 2
            int tCol = mLMax+1;
            int tShift = 2*2 + 2;
            final double c20  = cnlm.get(tN, tShift);
            final double c21  = cnlm.get(tN, tShift+1);
            final double c2n1 = cnlm.get(tN, tShift-1);
            final double c22  = cnlm.get(tN, tShift+2);
            final double c2n2 = cnlm.get(tN, tShift-2);
            double rFp3 = 0.0;
            rFp3 += WIGNER_222_000 * c20*c20*c20;
            rFp3 -= (3.0*WIGNER_222_011) * c20 * (c21*c21 + c2n1*c2n1);
            rFp3 += (3.0*WIGNER_222_022) * c20 * (c22*c22 + c2n2*c2n2);
            rFp3 += (3.0*SQRT2_INV*WIGNER_222_112) * c22 * (c21*c21 - c2n1*c2n1);
            rFp3 += (6.0*SQRT2_INV*WIGNER_222_112) * c21*c2n1*c2n2;
            rFingerPrint.set(tN, tCol, rFp3);
            
            final double c10, c11, c1n1;
            if (mL3Cross) {
                /// l1 = l2 = 1, l3 = 2
                ++tCol;
                tShift = 1 + 1;
                c10  = cnlm.get(tN, tShift);
                c11  = cnlm.get(tN, tShift+1);
                c1n1 = cnlm.get(tN, tShift-1);
                rFp3 = 0.0;
                rFp3 += WIGNER_112_000 * c10*c10*c20;
                rFp3 -= WIGNER_112_110 * c20 * (c11*c11 + c1n1*c1n1);
                rFp3 -= (2.0*WIGNER_112_011) * c10 * (c11*c21 + c1n1*c2n1);
                rFp3 += (SQRT2_INV*WIGNER_112_112) * c22 * (c11*c11 - c1n1*c1n1);
                rFp3 += (2.0*SQRT2_INV*WIGNER_112_112) * c11*c1n1*c2n2;
                rFingerPrint.set(tN, tCol, rFp3);
            } else {
                c10 = c11 = c1n1 = Double.NaN;
            }
            if (mL3Max == 2) continue;
            
            final double c30, c31, c3n1, c32, c3n2, c33, c3n3;
            if (mL3Cross) {
                /// l1 = 2, l2 = l3 = 3
                ++tCol;
                tShift = 3*3 + 3;
                c30  = cnlm.get(tN, tShift);
                c31  = cnlm.get(tN, tShift+1);
                c3n1 = cnlm.get(tN, tShift-1);
                c32  = cnlm.get(tN, tShift+2);
                c3n2 = cnlm.get(tN, tShift-2);
                c33  = cnlm.get(tN, tShift+3);
                c3n3 = cnlm.get(tN, tShift-3);
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
                rFingerPrint.set(tN, tCol, rFp3);
                /// l1 = 1, l2 = 2, l3 = 3
                ++tCol;
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
                rFingerPrint.set(tN, tCol, rFp3);
            } else {
                c30 = c31 = c3n1 = c32 = c3n2 = c33 = c3n3 = Double.NaN;
            }
            if (mL3Max == 3) continue;
            /// l1 = l2 = l3 = 4
            ++tCol;
            tShift = 4*4 + 4;
            final double c40  = cnlm.get(tN, tShift);
            final double c41  = cnlm.get(tN, tShift+1);
            final double c4n1 = cnlm.get(tN, tShift-1);
            final double c42  = cnlm.get(tN, tShift+2);
            final double c4n2 = cnlm.get(tN, tShift-2);
            final double c43  = cnlm.get(tN, tShift+3);
            final double c4n3 = cnlm.get(tN, tShift-3);
            final double c44  = cnlm.get(tN, tShift+4);
            final double c4n4 = cnlm.get(tN, tShift-4);
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
            rFingerPrint.set(tN, tCol, rFp3);
            if (mL3Cross) {
                /// l1 = l2 = 2, l3 = 4
                ++tCol;
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
                rFingerPrint.set(tN, tCol, rFp3);
                /// l1 = l2 = 3, l3 = 4
                ++tCol;
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
                rFingerPrint.set(tN, tCol, rFp3);
                /// l1 = 2, l2 = l3 = 4
                ++tCol;
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
                rFingerPrint.set(tN, tCol, rFp3);
                /// l1 = 1, l2 = 3, l3 = 4
                ++tCol;
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
                rFingerPrint.set(tN, tCol, rFp3);
            }
        }
        
        return rFingerPrint.asVecRow();
    }
    
    /**
     * {@inheritDoc}
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aNL 近邻列表遍历器
     * @return {@inheritDoc}
     */
    @Override public List<@NotNull Vector> evalPartial(boolean aCalBasis, boolean aCalCross, IDxyzTypeIterable aNL) {
        final int tSizeN = sizeN();
        final int tSizeL = sizeL();
        @Nullable RowMatrix rFingerPrint = aCalBasis ? MatrixCache.getMatRow(tSizeN, tSizeL) : null;
        RowMatrix rFingerPrintPx = MatrixCache.getMatRow(tSizeN, tSizeL);
        RowMatrix rFingerPrintPy = MatrixCache.getMatRow(tSizeN, tSizeL);
        RowMatrix rFingerPrintPz = MatrixCache.getMatRow(tSizeN, tSizeL);
        @Nullable List<RowMatrix> rFingerPrintPxCross = null;
        @Nullable List<RowMatrix> rFingerPrintPyCross = null;
        @Nullable List<RowMatrix> rFingerPrintPzCross = null;
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final RowMatrix cnlm = bufCnlm(true);
        final RowMatrix cnlmPx = bufCnlmPx(true);
        final RowMatrix cnlmPy = bufCnlmPy(true);
        final RowMatrix cnlmPz = bufCnlmPz(true);
        // 缓存 Rn 数组
        final Vector tRn = bufRn(false);
        final Vector tRnPx = bufRnPx(false);
        final Vector tRnPy = bufRnPy(false);
        final Vector tRnPz = bufRnPz(false);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final Vector tY = bufY(false);
        final Vector tYPtheta = bufYPtheta(false);
        final Vector tYPphi = bufYPphi(false);
        final Vector tYPx = bufYPx(false);
        final Vector tYPy = bufYPy(false);
        final Vector tYPz = bufYPz(false);
        // 记录一下近邻数目（对于 cross 的情况）
        final int[] tNN = {0};
        
        // 遍历近邻计算 Ylm, Rn, fc
        final int tLMax = lmax_();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= mRCut) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            
            // 计算种类的权重
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc 以及偏导数
            double fcMul = 1.0 - MathEX.Fast.pow2(dis/mRCut);
            double fcMul3 = MathEX.Fast.pow3(fcMul);
            double fc = fcMul3 * fcMul;
            double fcPMul = 8.0 * fcMul3 / (mRCut*mRCut);
            double fcPx = dx * fcPMul;
            double fcPy = dy * fcPMul;
            double fcPz = dz * fcPMul;
            // 统一遍历一次计算 Rn 以及偏导数
            calRnPxyz(tRn.internalData(), tRnPx.internalData(), tRnPy.internalData(), tRnPz.internalData(), mNMax,
                      dis, mRCut, dx, dy, dz);
            // 统一遍历一次计算 Ylm 以及偏导数；这里需要使用事先计算好角度的版本
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
            // 现在采用实球谐函数进行计算
            MathEX.Func.realSphericalHarmonicsFull2Dest4_(tLMax, cosTheta, sinTheta, cosPhi, sinPhi, tY);
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
            
            // 遍历求 n，l 的情况
            final RowMatrix cnlmPxUpdate, cnlmPyUpdate, cnlmPzUpdate;
            if (aCalCross) {
                int j = tNN[0]; ++tNN[0];
                cnlmPxUpdate = bufCnlmPxAll(j, true);
                cnlmPyUpdate = bufCnlmPyAll(j, true);
                cnlmPzUpdate = bufCnlmPzAll(j, true);
            } else {
                cnlmPxUpdate = cnlmPx;
                cnlmPyUpdate = cnlmPy;
                cnlmPzUpdate = cnlmPz;
            }
            
            if (mTypeNum > 1) {
                for (int tN = 0; tN <= mNMax; ++tN) {
                    mminusCnlmPxyz(cnlm.internalData(), cnlmPxUpdate.internalData(), cnlmPyUpdate.internalData(), cnlmPzUpdate.internalData(), cnlm.row(tN).internalDataShift(), cnlm.row(tN+mNMax+1).internalDataShift(),
                                   tY.internalData(), tYPx.internalData(), tYPy.internalData(), tYPz.internalData(),
                                   fc, fcPx, fcPy, fcPz,
                                   tRn.get(tN), tRnPx.get(tN), tRnPy.get(tN), tRnPz.get(tN),
                                   wt, cnlm.columnNumber());
                }
            } else {
                for (int tN = 0; tN <= mNMax; ++tN) {
                    mminusCnlmPxyz(cnlm.internalData(), cnlmPxUpdate.internalData(), cnlmPyUpdate.internalData(), cnlmPzUpdate.internalData(), cnlm.row(tN).internalDataShift(),
                                   tY.internalData(), tYPx.internalData(), tYPy.internalData(), tYPz.internalData(),
                                   fc, fcPx, fcPy, fcPz,
                                   tRn.get(tN), tRnPx.get(tN), tRnPy.get(tN), tRnPz.get(tN),
                                   cnlm.columnNumber());
                }
            }
        });
        if (aCalCross) {
            final int tNN_ = tNN[0];
            // 如果计算了 cross 的，则需要在这里手动累加一下 cnlm。
            // 这里为了减少计算量，偏导数统一都是负的，因此这里不需要增加负号；
            // 由于实际计算力时需要近邻的原本基组值来反向传播，因此这里结果实际会传递给近邻用于累加，所以需要的就是 基组对于近邻原子坐标的偏导值
            for (int i = 0; i < tNN_; ++i) {
                cnlmPx.plus2this(bufCnlmPxAll(i, false));
                cnlmPy.plus2this(bufCnlmPyAll(i, false));
                cnlmPz.plus2this(bufCnlmPzAll(i, false));
            }
            // 在这里初始化 cross 的 FingerPrint 偏导
            rFingerPrintPxCross = MatrixCache.getMatRow(tSizeN, tSizeL, tNN_);
            rFingerPrintPyCross = MatrixCache.getMatRow(tSizeN, tSizeL, tNN_);
            rFingerPrintPzCross = MatrixCache.getMatRow(tSizeN, tSizeL, tNN_);
        }
        // 因此在这里需要无论如何都给 cnlmPxyz 增加一个负号来翻转回来
        cnlmPx.operation().negative2this();
        cnlmPy.operation().negative2this();
        cnlmPz.operation().negative2this();
        
        // 做标量积消去 m 项，得到此原子的 FP
        cnlm2fpPxyz(cnlm.internalData(), cnlmPx.internalData(), cnlmPy.internalData(), cnlmPz.internalData(),
                    rFingerPrint==null ? null : rFingerPrint.internalData(), rFingerPrintPx.internalData(), rFingerPrintPy.internalData(), rFingerPrintPz.internalData(),
                    tSizeN, mLMax, mL3Max, mL3Cross);
        // 如果计算 cross，则需要这样设置 cross 的 FingerPrint 偏导
        if (aCalCross) {
            final int tNN_ = tNN[0];
            for (int i = 0; i < tNN_; ++i) {
                RowMatrix cnlmPxAllI = bufCnlmPxAll(i, false), cnlmPyAllI = bufCnlmPyAll(i, false), cnlmPzAllI = bufCnlmPzAll(i, false);
                RowMatrix tFingerPrintPxCrossI = rFingerPrintPxCross.get(i);
                RowMatrix tFingerPrintPyCrossI = rFingerPrintPyCross.get(i);
                RowMatrix tFingerPrintPzCrossI = rFingerPrintPzCross.get(i);
                cnlm2fpPxyz(cnlm.internalData(), cnlmPxAllI.internalData(), cnlmPyAllI.internalData(), cnlmPzAllI.internalData(),
                            null, tFingerPrintPxCrossI.internalData(), tFingerPrintPyCrossI.internalData(), tFingerPrintPzCrossI.internalData(),
                            tSizeN, mLMax, mL3Max, mL3Cross);
            }
        }
        
        List<Vector> rOut = Lists.newArrayList(rFingerPrint==null?null:rFingerPrint.asVecRow(), rFingerPrintPx.asVecRow(), rFingerPrintPy.asVecRow(), rFingerPrintPz.asVecRow());
        if (aCalCross) {
            rOut.addAll(AbstractCollections.map(rFingerPrintPxCross, RowMatrix::asVecRow));
            rOut.addAll(AbstractCollections.map(rFingerPrintPyCross, RowMatrix::asVecRow));
            rOut.addAll(AbstractCollections.map(rFingerPrintPzCross, RowMatrix::asVecRow));
        }
        return rOut;
    }
    
    
    protected static void calRnPxyz(double[] rRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, int aNMax,
                                    double aDis, double aRCut, double aDx, double aDy, double aDz) {
        final double tX = 1.0 - 2.0*aDis/aRCut;
        final double tRnPMul = 2.0 / (aDis*aRCut);
        rRn[0] = MathEX.Func.chebyshev(0, tX);
        rRnPx[0] = 0.0; rRnPy[0] = 0.0; rRnPz[0] = 0.0;
        for (int tN = 1; tN <= aNMax; ++tN) {
            rRn[tN] = MathEX.Func.chebyshev(tN, tX);
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
    protected static void mminusCnlmPxyz(double[] rCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz, int rShift,
                                         double[] aY, double[] aYPx, double[] aYPy, double[] aYPz,
                                         double aFc, double aFcPx, double aFcPy, double aFcPz,
                                         double aRn, double aRnPx, double aRnPy, double aRnPz, int aLength) {
        double tMul = aFc*aRn;
        double tMulX = aFc*aRnPx + aFcPx*aRn;
        double tMulY = aFc*aRnPy + aFcPy*aRn;
        double tMulZ = aFc*aRnPz + aFcPz*aRn;
        for (int i = 0, j = rShift; i < aLength; ++i, ++j) {
            double tY = aY[i];
            rCnlm[j] += tMul*tY;
            rCnlmPx[j] -= (tMulX*tY + tMul*aYPx[i]);
            rCnlmPy[j] -= (tMulY*tY + tMul*aYPy[i]);
            rCnlmPz[j] -= (tMulZ*tY + tMul*aYPz[i]);
        }
    }
    protected static void mminusCnlmPxyz(double[] rCnlm, double[] rCnlmPx, double[] rCnlmPy, double[] rCnlmPz, int rShift1, int rShift2,
                                         double[] aY, double[] aYPx, double[] aYPy, double[] aYPz,
                                         double aFc, double aFcPx, double aFcPy, double aFcPz,
                                         double aRn, double aRnPx, double aRnPy, double aRnPz,
                                         double aWt, int aLength) {
        double tMul = aFc*aRn;
        double tMulX = aFc*aRnPx + aFcPx*aRn;
        double tMulY = aFc*aRnPy + aFcPy*aRn;
        double tMulZ = aFc*aRnPz + aFcPz*aRn;
        for (int i = 0, j = rShift1, k = rShift2; i < aLength; ++i, ++j, ++k) {
            double tY = aY[i];
            double tCnli = tMul*tY;
            rCnlm[j] += tCnli; rCnlm[k] += aWt*tCnli;
            double tCnliPx = tMulX*tY + tMul*aYPx[i];
            rCnlmPx[j] -= tCnliPx; rCnlmPx[k] -= aWt*tCnliPx;
            double tCnliPy = tMulY*tY + tMul*aYPy[i];
            rCnlmPy[j] -= tCnliPy; rCnlmPy[k] -= aWt*tCnliPy;
            double tCnliPz = tMulZ*tY + tMul*aYPz[i];
            rCnlmPz[j] -= tCnliPz; rCnlmPz[k] -= aWt*tCnliPz;
        }
    }
    /** 热点优化，cnlm 转成 fp 放在一个循环中，让 java 优化整个运算 */
    protected static void cnlm2fpPxyz(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                      double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                      int aSizeN, int aLMax, int aL3Max, boolean aL3Cross) {
        final int tLMax = Math.max(aLMax, aL3Max);
        final int tColNum = (tLMax+1)*(tLMax+1);
        final int tColNumFP = aLMax+1 + (aL3Cross?L3NCOLS:L3NCOLS_NOCROSS)[aL3Max];
        int tShift = 0, tShiftFP = 0;
        for (int tN = 0; tN < aSizeN; ++tN, tShift += tColNum, tShiftFP += tColNumFP) {
            calL2_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz,
                   rFp, rFpPx, rFpPy, rFpPz,
                   aLMax, tShift, tShiftFP);
            calL3_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz,
                   rFp, rFpPx, rFpPy, rFpPz,
                   aLMax, aL3Max, aL3Cross, tShift, tShiftFP);
        }
    }
    private static void calL2_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                               double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                               int aLMax, int aShift, int aShiftFP) {
        // l == 0
        int tIdx = aShift, tIdxFP = aShiftFP;
        double tCnl0 = aCnlm[tIdx];
        double tMul = PI4;
        double tMul2 = tMul+tMul;
        rFpPx[tIdxFP] = tMul2 * (tCnl0*aCnlmPx[tIdx]);
        rFpPy[tIdxFP] = tMul2 * (tCnl0*aCnlmPy[tIdx]);
        rFpPz[tIdxFP] = tMul2 * (tCnl0*aCnlmPz[tIdx]);
        if (rFp != null) rFp[tIdxFP] = tMul * (tCnl0*tCnl0);
        if (aLMax == 0) return;
        // l = 1
        tIdx = 1+aShift; tIdxFP = 1+aShiftFP;
        tCnl0 = aCnlm[tIdx]; double tCnl1 = aCnlm[tIdx+1], tCnl2 = aCnlm[tIdx+2];
        tMul = PI4/3;
        tMul2 = tMul+tMul;
        rFpPx[tIdxFP] = tMul2 * (tCnl0*aCnlmPx[tIdx] + tCnl1*aCnlmPx[tIdx+1] + tCnl2*aCnlmPx[tIdx+2]);
        rFpPy[tIdxFP] = tMul2 * (tCnl0*aCnlmPy[tIdx] + tCnl1*aCnlmPy[tIdx+1] + tCnl2*aCnlmPy[tIdx+2]);
        rFpPz[tIdxFP] = tMul2 * (tCnl0*aCnlmPz[tIdx] + tCnl1*aCnlmPz[tIdx+1] + tCnl2*aCnlmPz[tIdx+2]);
        if (rFp != null) rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2);
        if (aLMax == 1) return;
        // l = 2
        tIdx = 4+aShift; tIdxFP = 2+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; double tCnl3 = aCnlm[tIdx+3], tCnl4 = aCnlm[tIdx+4];
        tMul = PI4/5;
        tMul2 = tMul+tMul;
        rFpPx[tIdxFP] = tMul2 * (tCnl0*aCnlmPx[tIdx] + tCnl1*aCnlmPx[tIdx+1] + tCnl2*aCnlmPx[tIdx+2] + tCnl3*aCnlmPx[tIdx+3] + tCnl4*aCnlmPx[tIdx+4]);
        rFpPy[tIdxFP] = tMul2 * (tCnl0*aCnlmPy[tIdx] + tCnl1*aCnlmPy[tIdx+1] + tCnl2*aCnlmPy[tIdx+2] + tCnl3*aCnlmPy[tIdx+3] + tCnl4*aCnlmPy[tIdx+4]);
        rFpPz[tIdxFP] = tMul2 * (tCnl0*aCnlmPz[tIdx] + tCnl1*aCnlmPz[tIdx+1] + tCnl2*aCnlmPz[tIdx+2] + tCnl3*aCnlmPz[tIdx+3] + tCnl4*aCnlmPz[tIdx+4]);
        if (rFp != null) rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4);
        if (aLMax == 2) return;
        // l = 3
        tIdx = 9+aShift; tIdxFP = 3+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; double tCnl5 = aCnlm[tIdx+5], tCnl6 = aCnlm[tIdx+6];
        tMul = PI4/7;
        tMul2 = tMul+tMul;
        rFpPx[tIdxFP] = tMul2 * (tCnl0*aCnlmPx[tIdx] + tCnl1*aCnlmPx[tIdx+1] + tCnl2*aCnlmPx[tIdx+2] + tCnl3*aCnlmPx[tIdx+3] + tCnl4*aCnlmPx[tIdx+4] + tCnl5*aCnlmPx[tIdx+5] + tCnl6*aCnlmPx[tIdx+6]);
        rFpPy[tIdxFP] = tMul2 * (tCnl0*aCnlmPy[tIdx] + tCnl1*aCnlmPy[tIdx+1] + tCnl2*aCnlmPy[tIdx+2] + tCnl3*aCnlmPy[tIdx+3] + tCnl4*aCnlmPy[tIdx+4] + tCnl5*aCnlmPy[tIdx+5] + tCnl6*aCnlmPy[tIdx+6]);
        rFpPz[tIdxFP] = tMul2 * (tCnl0*aCnlmPz[tIdx] + tCnl1*aCnlmPz[tIdx+1] + tCnl2*aCnlmPz[tIdx+2] + tCnl3*aCnlmPz[tIdx+3] + tCnl4*aCnlmPz[tIdx+4] + tCnl5*aCnlmPz[tIdx+5] + tCnl6*aCnlmPz[tIdx+6]);
        if (rFp != null) rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6);
        if (aLMax == 3) return;
        // l = 4
        tIdx = 16+aShift; tIdxFP = 4+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; double tCnl7 = aCnlm[tIdx+7], tCnl8 = aCnlm[tIdx+8];
        tMul = PI4/9;
        tMul2 = tMul+tMul;
        rFpPx[tIdxFP] = tMul2 * (tCnl0*aCnlmPx[tIdx] + tCnl1*aCnlmPx[tIdx+1] + tCnl2*aCnlmPx[tIdx+2] + tCnl3*aCnlmPx[tIdx+3] + tCnl4*aCnlmPx[tIdx+4] + tCnl5*aCnlmPx[tIdx+5] + tCnl6*aCnlmPx[tIdx+6] + tCnl7*aCnlmPx[tIdx+7] + tCnl8*aCnlmPx[tIdx+8]);
        rFpPy[tIdxFP] = tMul2 * (tCnl0*aCnlmPy[tIdx] + tCnl1*aCnlmPy[tIdx+1] + tCnl2*aCnlmPy[tIdx+2] + tCnl3*aCnlmPy[tIdx+3] + tCnl4*aCnlmPy[tIdx+4] + tCnl5*aCnlmPy[tIdx+5] + tCnl6*aCnlmPy[tIdx+6] + tCnl7*aCnlmPy[tIdx+7] + tCnl8*aCnlmPy[tIdx+8]);
        rFpPz[tIdxFP] = tMul2 * (tCnl0*aCnlmPz[tIdx] + tCnl1*aCnlmPz[tIdx+1] + tCnl2*aCnlmPz[tIdx+2] + tCnl3*aCnlmPz[tIdx+3] + tCnl4*aCnlmPz[tIdx+4] + tCnl5*aCnlmPz[tIdx+5] + tCnl6*aCnlmPz[tIdx+6] + tCnl7*aCnlmPz[tIdx+7] + tCnl8*aCnlmPz[tIdx+8]);
        if (rFp != null) rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6 + tCnl7*tCnl7 + tCnl8*tCnl8);
        if (aLMax == 4) return;
        // l = 5
        tIdx = 25+aShift; tIdxFP = 5+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; tCnl7 = aCnlm[tIdx+7]; tCnl8 = aCnlm[tIdx+8]; double tCnl9 = aCnlm[tIdx+9], tCnl10 = aCnlm[tIdx+10];
        tMul = PI4/11;
        tMul2 = tMul+tMul;
        rFpPx[tIdxFP] = tMul2 * (tCnl0*aCnlmPx[tIdx] + tCnl1*aCnlmPx[tIdx+1] + tCnl2*aCnlmPx[tIdx+2] + tCnl3*aCnlmPx[tIdx+3] + tCnl4*aCnlmPx[tIdx+4] + tCnl5*aCnlmPx[tIdx+5] + tCnl6*aCnlmPx[tIdx+6] + tCnl7*aCnlmPx[tIdx+7] + tCnl8*aCnlmPx[tIdx+8] + tCnl9*aCnlmPx[tIdx+9] + tCnl10*aCnlmPx[tIdx+10]);
        rFpPy[tIdxFP] = tMul2 * (tCnl0*aCnlmPy[tIdx] + tCnl1*aCnlmPy[tIdx+1] + tCnl2*aCnlmPy[tIdx+2] + tCnl3*aCnlmPy[tIdx+3] + tCnl4*aCnlmPy[tIdx+4] + tCnl5*aCnlmPy[tIdx+5] + tCnl6*aCnlmPy[tIdx+6] + tCnl7*aCnlmPy[tIdx+7] + tCnl8*aCnlmPy[tIdx+8] + tCnl9*aCnlmPy[tIdx+9] + tCnl10*aCnlmPy[tIdx+10]);
        rFpPz[tIdxFP] = tMul2 * (tCnl0*aCnlmPz[tIdx] + tCnl1*aCnlmPz[tIdx+1] + tCnl2*aCnlmPz[tIdx+2] + tCnl3*aCnlmPz[tIdx+3] + tCnl4*aCnlmPz[tIdx+4] + tCnl5*aCnlmPz[tIdx+5] + tCnl6*aCnlmPz[tIdx+6] + tCnl7*aCnlmPz[tIdx+7] + tCnl8*aCnlmPz[tIdx+8] + tCnl9*aCnlmPz[tIdx+9] + tCnl10*aCnlmPz[tIdx+10]);
        if (rFp != null) rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6 + tCnl7*tCnl7 + tCnl8*tCnl8 + tCnl9*tCnl9 + tCnl10*tCnl10);
        if (aLMax == 5) return;
        // l = 6
        tIdx = 36+aShift; tIdxFP = 6+aShiftFP;
        tCnl0 = aCnlm[tIdx]; tCnl1 = aCnlm[tIdx+1]; tCnl2 = aCnlm[tIdx+2]; tCnl3 = aCnlm[tIdx+3]; tCnl4 = aCnlm[tIdx+4]; tCnl5 = aCnlm[tIdx+5]; tCnl6 = aCnlm[tIdx+6]; tCnl7 = aCnlm[tIdx+7]; tCnl8 = aCnlm[tIdx+8]; tCnl9 = aCnlm[tIdx+9]; tCnl10 = aCnlm[tIdx+10]; double tCnl11 = aCnlm[tIdx+11], tCnl12 = aCnlm[tIdx+12];
        tMul = PI4/13;
        tMul2 = tMul+tMul;
        rFpPx[tIdxFP] = tMul2 * (tCnl0*aCnlmPx[tIdx] + tCnl1*aCnlmPx[tIdx+1] + tCnl2*aCnlmPx[tIdx+2] + tCnl3*aCnlmPx[tIdx+3] + tCnl4*aCnlmPx[tIdx+4] + tCnl5*aCnlmPx[tIdx+5] + tCnl6*aCnlmPx[tIdx+6] + tCnl7*aCnlmPx[tIdx+7] + tCnl8*aCnlmPx[tIdx+8] + tCnl9*aCnlmPx[tIdx+9] + tCnl10*aCnlmPx[tIdx+10] + tCnl11*aCnlmPx[tIdx+11] + tCnl12*aCnlmPx[tIdx+12]);
        rFpPy[tIdxFP] = tMul2 * (tCnl0*aCnlmPy[tIdx] + tCnl1*aCnlmPy[tIdx+1] + tCnl2*aCnlmPy[tIdx+2] + tCnl3*aCnlmPy[tIdx+3] + tCnl4*aCnlmPy[tIdx+4] + tCnl5*aCnlmPy[tIdx+5] + tCnl6*aCnlmPy[tIdx+6] + tCnl7*aCnlmPy[tIdx+7] + tCnl8*aCnlmPy[tIdx+8] + tCnl9*aCnlmPy[tIdx+9] + tCnl10*aCnlmPy[tIdx+10] + tCnl11*aCnlmPy[tIdx+11] + tCnl12*aCnlmPy[tIdx+12]);
        rFpPz[tIdxFP] = tMul2 * (tCnl0*aCnlmPz[tIdx] + tCnl1*aCnlmPz[tIdx+1] + tCnl2*aCnlmPz[tIdx+2] + tCnl3*aCnlmPz[tIdx+3] + tCnl4*aCnlmPz[tIdx+4] + tCnl5*aCnlmPz[tIdx+5] + tCnl6*aCnlmPz[tIdx+6] + tCnl7*aCnlmPz[tIdx+7] + tCnl8*aCnlmPz[tIdx+8] + tCnl9*aCnlmPz[tIdx+9] + tCnl10*aCnlmPz[tIdx+10] + tCnl11*aCnlmPz[tIdx+11] + tCnl12*aCnlmPz[tIdx+12]);
        if (rFp != null) rFp[tIdxFP] = tMul * (tCnl0*tCnl0 + tCnl1*tCnl1 + tCnl2*tCnl2 + tCnl3*tCnl3 + tCnl4*tCnl4 + tCnl5*tCnl5 + tCnl6*tCnl6 + tCnl7*tCnl7 + tCnl8*tCnl8 + tCnl9*tCnl9 + tCnl10*tCnl10 + tCnl11*tCnl11 + tCnl12*tCnl12);
        if (aLMax == 6) return;
        // 优化到 l = 6 主要是大部分只用到这个程度；在本地测试这个优化基本没效果了（可能需要 avx512 指令集更有效）
        // else
        for (int tL = 7; tL <= aLMax; ++tL) {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            final int tStart = tL*tL + aShift;
            final int tLen = tL+tL+1;
            final int tEnd = tStart+tLen;
            if (rFp != null) {
                double rDot = 0.0, rDotPx = 0.0, rDotPy = 0.0, rDotPz = 0.0;
                for (int i = tStart; i < tEnd; ++i) {
                    double tCnlm = aCnlm[i];
                    rDot += tCnlm*tCnlm;
                    rDotPx += tCnlm*aCnlmPx[i];
                    rDotPy += tCnlm*aCnlmPy[i];
                    rDotPz += tCnlm*aCnlmPz[i];
                }
                tMul = PI4/(double)tLen;
                tMul2 = tMul+tMul;
                rFp[tL+aShiftFP] = tMul * rDot;
                rFpPx[tL+aShiftFP] = tMul2 * rDotPx;
                rFpPy[tL+aShiftFP] = tMul2 * rDotPy;
                rFpPz[tL+aShiftFP] = tMul2 * rDotPz;
            } else {
                double rDotPx = 0.0, rDotPy = 0.0, rDotPz = 0.0;
                for (int i = tStart; i < tEnd; ++i) {
                    double tCnlm = aCnlm[i];
                    rDotPx += tCnlm*aCnlmPx[i];
                    rDotPy += tCnlm*aCnlmPy[i];
                    rDotPz += tCnlm*aCnlmPz[i];
                }
                tMul = PI4/(double)tLen;
                tMul2 = tMul+tMul;
                rFpPx[tL+aShiftFP] = tMul2 * rDotPx;
                rFpPy[tL+aShiftFP] = tMul2 * rDotPy;
                rFpPz[tL+aShiftFP] = tMul2 * rDotPz;
            }
        }
    }
    
    private static void calL3_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                               double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                               int aLMax, int aL3Max, boolean aL3Cross, int aShift, int aShiftFP) {
        // 过大的单一函数会阻止 JIT 优化，因此这里要拆分成多个函数
        if (aL3Max <= 1) return;
        int tIdxFP = aLMax+1+aShiftFP;
        calL3_222_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
        ++tIdxFP;
        if (aL3Cross) {
            calL3_112_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
            ++tIdxFP;
        }
        if (aL3Max == 2) return;
        if (aL3Cross) {
            calL3_233_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
            ++tIdxFP;
            calL3_123_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
            ++tIdxFP;
        }
        if (aL3Max == 3) return;
        calL3_444_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
        ++tIdxFP;
        if (aL3Cross) {
            calL3_224_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
            ++tIdxFP;
            calL3_334_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
            ++tIdxFP;
            calL3_244_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
            ++tIdxFP;
            calL3_134_(aCnlm, aCnlmPx, aCnlmPy, aCnlmPz, rFp, rFpPx, rFpPy, rFpPz, aShift, tIdxFP);
            ++tIdxFP;
        }
    }
    private static void calL3_222_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx = (2*2+2)+aShift;
        final double c20  = aCnlm[tIdx  ];
        final double c21  = aCnlm[tIdx+1];
        final double c2n1 = aCnlm[tIdx-1];
        final double c22  = aCnlm[tIdx+2];
        final double c2n2 = aCnlm[tIdx-2];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul1 = WIGNER_222_000 * c20*c20;
        double tMul2 = (-3.0*WIGNER_222_011)*(c21*c21 + c2n1*c2n1) + (3.0*WIGNER_222_022)*(c22*c22 + c2n2*c2n2);
        double tMul = 3.0*tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx];
        rFp3Py += tMul*aCnlmPy[tIdx];
        rFp3Pz += tMul*aCnlmPz[tIdx];
        if (rFp != null) rFp3 += (tMul1 + tMul2)*c20;
        
        tMul1 = (-6.0*WIGNER_222_011) * c20*c21  + (6.0*SQRT2_INV*WIGNER_222_112) * c22*c21;
        tMul2 = (-6.0*WIGNER_222_011) * c20*c2n1 - (6.0*SQRT2_INV*WIGNER_222_112) * c22*c2n1;
        rFp3Px += tMul1*aCnlmPx[tIdx+1] + tMul2*aCnlmPx[tIdx-1];
        rFp3Py += tMul1*aCnlmPy[tIdx+1] + tMul2*aCnlmPy[tIdx-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx+1] + tMul2*aCnlmPz[tIdx-1];
        
        tMul1 = (6.0*WIGNER_222_022) * c20*c22;
        tMul2 = (6.0*WIGNER_222_022) * c20*c2n2;
        rFp3Px += tMul1*aCnlmPx[tIdx+2] + tMul2*aCnlmPx[tIdx-2];
        rFp3Py += tMul1*aCnlmPy[tIdx+2] + tMul2*aCnlmPy[tIdx-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx+2] + tMul2*aCnlmPz[tIdx-2];
        
        tMul = (3.0*SQRT2_INV*WIGNER_222_112) * (c21*c21 - c2n1*c2n1);
        rFp3Px += tMul*aCnlmPx[tIdx+2];
        rFp3Py += tMul*aCnlmPy[tIdx+2];
        rFp3Pz += tMul*aCnlmPz[tIdx+2];
        if (rFp != null) rFp3 += tMul*c22;
        
        tMul = (6.0*SQRT2_INV*WIGNER_222_112) * c2n1*c2n2;
        tMul1 = (6.0*SQRT2_INV*WIGNER_222_112) * c21*c2n2;
        tMul2 = (6.0*SQRT2_INV*WIGNER_222_112) * c21*c2n1;
        rFp3Px += tMul*aCnlmPx[tIdx+1] + tMul1*aCnlmPx[tIdx-1] + tMul2*aCnlmPx[tIdx-2];
        rFp3Py += tMul*aCnlmPy[tIdx+1] + tMul1*aCnlmPy[tIdx-1] + tMul2*aCnlmPy[tIdx-2];
        rFp3Pz += tMul*aCnlmPz[tIdx+1] + tMul1*aCnlmPz[tIdx-1] + tMul2*aCnlmPz[tIdx-2];
        if (rFp != null) rFp3 += tMul*c21;
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_112_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx1 = (1+1)+aShift;
        final double c10  = aCnlm[tIdx1  ];
        final double c11  = aCnlm[tIdx1+1];
        final double c1n1 = aCnlm[tIdx1-1];
        final int tIdx2 = (2*2+2)+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_112_000*c10*c10 - WIGNER_112_110*(c11*c11 + c1n1*c1n1);
        rFp3Px += tMul*aCnlmPx[tIdx2];
        rFp3Py += tMul*aCnlmPy[tIdx2];
        rFp3Pz += tMul*aCnlmPz[tIdx2];
        if (rFp != null) rFp3 += tMul*c20;
        double tMul1 = (2.0*WIGNER_112_000) * c10*c20;
        double tMul2 = (-2.0*WIGNER_112_011) * (c11*c21 + c1n1*c2n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx1];
        rFp3Py += tMul*aCnlmPy[tIdx1];
        rFp3Pz += tMul*aCnlmPz[tIdx1];
        if (rFp != null) rFp3 += tMul2*c10;
        
        tMul1 = (-2.0*WIGNER_112_110)*c20*c11  - (2.0*WIGNER_112_011)*c10*c21  + (2.0*SQRT2_INV*WIGNER_112_112)*c22*c11;
        tMul2 = (-2.0*WIGNER_112_110)*c20*c1n1 - (2.0*WIGNER_112_011)*c10*c2n1 - (2.0*SQRT2_INV*WIGNER_112_112)*c22*c1n1;
        rFp3Px += tMul1*aCnlmPx[tIdx1+1] + tMul2*aCnlmPx[tIdx1-1];
        rFp3Py += tMul1*aCnlmPy[tIdx1+1] + tMul2*aCnlmPy[tIdx1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx1+1] + tMul2*aCnlmPz[tIdx1-1];
        tMul1 = (-2.0*WIGNER_112_011) * c10*c11;
        tMul2 = (-2.0*WIGNER_112_011) * c10*c1n1;
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        
        tMul = (SQRT2_INV*WIGNER_112_112) * (c11*c11 - c1n1*c1n1);
        rFp3Px += tMul*aCnlmPx[tIdx2+2];
        rFp3Py += tMul*aCnlmPy[tIdx2+2];
        rFp3Pz += tMul*aCnlmPz[tIdx2+2];
        if (rFp != null) rFp3 += tMul*c22;
        
        tMul = (2.0*SQRT2_INV*WIGNER_112_112) * c1n1*c2n2;
        tMul1 = (2.0*SQRT2_INV*WIGNER_112_112) * c11*c2n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_112_112) * c11*c1n1;
        rFp3Px += tMul*aCnlmPx[tIdx1+1] + tMul1*aCnlmPx[tIdx1-1] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul*aCnlmPy[tIdx1+1] + tMul1*aCnlmPy[tIdx1-1] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul*aCnlmPz[tIdx1+1] + tMul1*aCnlmPz[tIdx1-1] + tMul2*aCnlmPz[tIdx2-2];
        if (rFp != null) rFp3 += tMul*c11;
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_233_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx2 = (2*2+2)+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx3 = (3*3+3)+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_233_000*c30*c30 - WIGNER_233_011*(c31*c31 + c3n1*c3n1) + WIGNER_233_022*(c32*c32 + c3n2*c3n2) - WIGNER_233_033*(c33*c33 + c3n3*c3n3);
        rFp3Px += tMul*aCnlmPx[tIdx2];
        rFp3Py += tMul*aCnlmPy[tIdx2];
        rFp3Pz += tMul*aCnlmPz[tIdx2];
        if (rFp != null) rFp3 += tMul*c20;
        double tMul1 = (2.0*WIGNER_233_000) * c20*c30;
        double tMul2 = (-2.0*WIGNER_233_110)*(c21*c31 + c2n1*c3n1) + (2.0*WIGNER_233_220)*(c22*c32 + c2n2*c3n2);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx3];
        rFp3Py += tMul*aCnlmPy[tIdx3];
        rFp3Pz += tMul*aCnlmPz[tIdx3];
        if (rFp != null) rFp3 += tMul2*c30;
        
        tMul1 = (-2.0*WIGNER_233_011)*c20*c31  - (2.0*WIGNER_233_110)*c30*c21  + (2.0*SQRT2_INV*WIGNER_233_211)*c22*c31  + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c32 + c2n1*c3n2) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c33 + c2n2*c3n3);
        tMul2 = (-2.0*WIGNER_233_011)*c20*c3n1 - (2.0*WIGNER_233_110)*c30*c2n1 - (2.0*SQRT2_INV*WIGNER_233_211)*c22*c3n1 + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c3n2 - c2n1*c32) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c3n3 - c2n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx3+1] + tMul2*aCnlmPx[tIdx3-1];
        rFp3Py += tMul1*aCnlmPy[tIdx3+1] + tMul2*aCnlmPy[tIdx3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+1] + tMul2*aCnlmPz[tIdx3-1];
        tMul1 = (2.0*WIGNER_233_022)*c20*c32  + (2.0*WIGNER_233_220)*c30*c22  + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c31 - c2n1*c3n1) - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c33 + c2n1*c3n3);
        tMul2 = (2.0*WIGNER_233_022)*c20*c3n2 + (2.0*WIGNER_233_220)*c30*c2n2 + (2.0*SQRT2_INV*WIGNER_233_112)*(c21*c3n1 + c2n1*c31) - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c3n3 - c2n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx3+2] + tMul2*aCnlmPx[tIdx3-2];
        rFp3Py += tMul1*aCnlmPy[tIdx3+2] + tMul2*aCnlmPy[tIdx3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+2] + tMul2*aCnlmPz[tIdx3-2];
        tMul1 = (-2.0*WIGNER_233_033)*c20*c33  - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c32 - c2n1*c3n2) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c31 - c2n2*c3n1);
        tMul2 = (-2.0*WIGNER_233_033)*c20*c3n3 - (2.0*SQRT2_INV*WIGNER_233_123)*(c21*c3n2 + c2n1*c32) - (2.0*SQRT2_INV*WIGNER_233_213)*(c22*c3n1 + c2n2*c31);
        rFp3Px += tMul1*aCnlmPx[tIdx3+3] + tMul2*aCnlmPx[tIdx3-3];
        rFp3Py += tMul1*aCnlmPy[tIdx3+3] + tMul2*aCnlmPy[tIdx3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+3] + tMul2*aCnlmPz[tIdx3-3];
        
        tMul1 = (-2.0*WIGNER_233_110) * c30*c31;
        tMul2 = (-2.0*WIGNER_233_110) * c30*c3n1;
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        tMul1 = (2.0*WIGNER_233_220) * c30*c32;
        tMul2 = (2.0*WIGNER_233_220) * c30*c3n2;
        rFp3Px += tMul1*aCnlmPx[tIdx2+2] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul1*aCnlmPy[tIdx2+2] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+2] + tMul2*aCnlmPz[tIdx2-2];
        
        tMul = (SQRT2_INV*WIGNER_233_211) * (c31*c31 - c3n1*c3n1);
        rFp3Px += tMul*aCnlmPx[tIdx2+2];
        rFp3Py += tMul*aCnlmPy[tIdx2+2];
        rFp3Pz += tMul*aCnlmPz[tIdx2+2];
        if (rFp != null) rFp3 += tMul*c22;
        
        tMul = (2.0*SQRT2_INV*WIGNER_233_211) * c2n2*c3n1;
        tMul1 = (2.0*SQRT2_INV*WIGNER_233_211) * c31*c2n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_233_211) * c31*c3n1;
        rFp3Px += tMul*aCnlmPx[tIdx3+1] + tMul1*aCnlmPx[tIdx3-1] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul*aCnlmPy[tIdx3+1] + tMul1*aCnlmPy[tIdx3-1] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul*aCnlmPz[tIdx3+1] + tMul1*aCnlmPz[tIdx3-1] + tMul2*aCnlmPz[tIdx2-2];
        if (rFp != null) rFp3 += tMul*c31;
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_233_112)*(c31*c32 + c3n1*c3n2) - (2.0*SQRT2_INV*WIGNER_233_123)*(c32*c33 + c3n2*c3n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_233_112)*(c31*c3n2 - c3n1*c32) - (2.0*SQRT2_INV*WIGNER_233_123)*(c32*c3n3 - c3n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        if (rFp != null) rFp3 += tMul1*c21 + tMul2*c2n1;
        tMul1 = (-2.0*SQRT2_INV*WIGNER_233_213)*(c31*c33 + c3n1*c3n3);
        tMul2 = (-2.0*SQRT2_INV*WIGNER_233_213)*(c31*c3n3 - c3n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx2+2] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul1*aCnlmPy[tIdx2+2] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+2] + tMul2*aCnlmPz[tIdx2-2];
        if (rFp != null) rFp3 += tMul1*c22 + tMul2*c2n2;
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_123_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx1 = (1+1)+aShift;
        final double c10  = aCnlm[tIdx1  ];
        final double c11  = aCnlm[tIdx1+1];
        final double c1n1 = aCnlm[tIdx1-1];
        final int tIdx2 = (2*2+2)+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx3 = (3*3+3)+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_123_000*c20*c30 - WIGNER_123_011*(c21*c31 + c2n1*c3n1) + WIGNER_123_022*(c22*c32 + c2n2*c3n2);
        rFp3Px += tMul*aCnlmPx[tIdx1];
        rFp3Py += tMul*aCnlmPy[tIdx1];
        rFp3Pz += tMul*aCnlmPz[tIdx1];
        if (rFp != null) rFp3 += tMul*c10;
        double tMul1 = WIGNER_123_000 * c10*c30;
        double tMul2 = (-WIGNER_123_101) * (c11*c31 + c1n1*c3n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx2];
        rFp3Py += tMul*aCnlmPy[tIdx2];
        rFp3Pz += tMul*aCnlmPz[tIdx2];
        if (rFp != null) rFp3 += tMul2*c20;
        tMul1 = WIGNER_123_000 * c10*c20;
        tMul2 = (-WIGNER_123_110) * (c11*c21 + c1n1*c2n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx3];
        rFp3Py += tMul*aCnlmPy[tIdx3];
        rFp3Pz += tMul*aCnlmPz[tIdx3];
        if (rFp != null) rFp3 += tMul2*c30;
        
        tMul1 = (-WIGNER_123_011)*c10*c31  - WIGNER_123_110*c30*c11  + (SQRT2_INV*WIGNER_123_112)*(c11*c32 + c1n1*c3n2);
        tMul2 = (-WIGNER_123_011)*c10*c3n1 - WIGNER_123_110*c30*c1n1 + (SQRT2_INV*WIGNER_123_112)*(c11*c3n2 - c1n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        tMul1 = (-WIGNER_123_011)*c10*c21  - WIGNER_123_101*c20*c11  + (SQRT2_INV*WIGNER_123_121)*(c11*c22 + c1n1*c2n2);
        tMul2 = (-WIGNER_123_011)*c10*c2n1 - WIGNER_123_101*c20*c1n1 + (SQRT2_INV*WIGNER_123_121)*(c11*c2n2 - c1n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdx3+1] + tMul2*aCnlmPx[tIdx3-1];
        rFp3Py += tMul1*aCnlmPy[tIdx3+1] + tMul2*aCnlmPy[tIdx3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+1] + tMul2*aCnlmPz[tIdx3-1];
        tMul1 = (-WIGNER_123_101)*c20*c31  - WIGNER_123_110*c30*c21;
        tMul2 = (-WIGNER_123_101)*c20*c3n1 - WIGNER_123_110*c30*c2n1;
        rFp3Px += tMul1*aCnlmPx[tIdx1+1] + tMul2*aCnlmPx[tIdx1-1];
        rFp3Py += tMul1*aCnlmPy[tIdx1+1] + tMul2*aCnlmPy[tIdx1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx1+1] + tMul2*aCnlmPz[tIdx1-1];
        tMul1 = WIGNER_123_022*c10*c32  + (SQRT2_INV*WIGNER_123_121)*(c11*c31 - c1n1*c3n1) - (SQRT2_INV*WIGNER_123_123)*(c11*c33 + c1n1*c3n3);
        tMul2 = WIGNER_123_022*c10*c3n2 + (SQRT2_INV*WIGNER_123_121)*(c11*c3n1 + c1n1*c31) - (SQRT2_INV*WIGNER_123_123)*(c11*c3n3 - c1n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx2+2] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul1*aCnlmPy[tIdx2+2] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+2] + tMul2*aCnlmPz[tIdx2-2];
        tMul1 = WIGNER_123_022*c10*c22  + (SQRT2_INV*WIGNER_123_112)*(c11*c21 - c1n1*c2n1);
        tMul2 = WIGNER_123_022*c10*c2n2 + (SQRT2_INV*WIGNER_123_112)*(c11*c2n1 + c1n1*c21);
        rFp3Px += tMul1*aCnlmPx[tIdx3+2] + tMul2*aCnlmPx[tIdx3-2];
        rFp3Py += tMul1*aCnlmPy[tIdx3+2] + tMul2*aCnlmPy[tIdx3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+2] + tMul2*aCnlmPz[tIdx3-2];
        
        tMul1 = (SQRT2_INV*WIGNER_123_112)*(c21*c32 + c2n1*c3n2) + (SQRT2_INV*WIGNER_123_121)*(c22*c31 + c2n2*c3n1) - (SQRT2_INV*WIGNER_123_123)*(c22*c33 + c2n2*c3n3);
        tMul2 = (SQRT2_INV*WIGNER_123_112)*(c21*c3n2 - c2n1*c32) + (SQRT2_INV*WIGNER_123_121)*(c2n2*c31 - c22*c3n1) - (SQRT2_INV*WIGNER_123_123)*(c22*c3n3 - c2n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx1+1] + tMul2*aCnlmPx[tIdx1-1];
        rFp3Py += tMul1*aCnlmPy[tIdx1+1] + tMul2*aCnlmPy[tIdx1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx1+1] + tMul2*aCnlmPz[tIdx1-1];
        if (rFp != null) rFp3 += tMul1*c11 + tMul2*c1n1;
        tMul1 = (-SQRT2_INV*WIGNER_123_123)*(c11*c22 - c1n1*c2n2);
        tMul2 = (-SQRT2_INV*WIGNER_123_123)*(c11*c2n2 + c1n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdx3+3] + tMul2*aCnlmPx[tIdx3-3];
        rFp3Py += tMul1*aCnlmPy[tIdx3+3] + tMul2*aCnlmPy[tIdx3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+3] + tMul2*aCnlmPz[tIdx3-3];
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_444_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx = (4*4+4)+aShift;
        final double c40  = aCnlm[tIdx  ];
        final double c41  = aCnlm[tIdx+1];
        final double c4n1 = aCnlm[tIdx-1];
        final double c42  = aCnlm[tIdx+2];
        final double c4n2 = aCnlm[tIdx-2];
        final double c43  = aCnlm[tIdx+3];
        final double c4n3 = aCnlm[tIdx-3];
        final double c44  = aCnlm[tIdx+4];
        final double c4n4 = aCnlm[tIdx-4];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul1 = WIGNER_444_000 * c40*c40;
        double tMul2 = (-3.0*WIGNER_444_011)*(c41*c41 + c4n1*c4n1) + (3.0*WIGNER_444_022)*(c42*c42 + c4n2*c4n2) - (3.0*WIGNER_444_033)*(c43*c43 + c4n3*c4n3) + (3.0*WIGNER_444_044)*(c44*c44 + c4n4*c4n4);
        double tMul = 3.0*tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx];
        rFp3Py += tMul*aCnlmPy[tIdx];
        rFp3Pz += tMul*aCnlmPz[tIdx];
        if (rFp != null) rFp3 += (tMul1 + tMul2)*c40;
        
        tMul1 = (-6.0*WIGNER_444_011)*c40*c41  + (6.0*SQRT2_INV*WIGNER_444_112)*c42*c41;
        tMul2 = (-6.0*WIGNER_444_011)*c40*c4n1 - (6.0*SQRT2_INV*WIGNER_444_112)*c42*c4n1;
        rFp3Px += tMul1*aCnlmPx[tIdx+1] + tMul2*aCnlmPx[tIdx-1];
        rFp3Py += tMul1*aCnlmPy[tIdx+1] + tMul2*aCnlmPy[tIdx-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx+1] + tMul2*aCnlmPz[tIdx-1];
        tMul1 = (6.0*WIGNER_444_022)*c40*c42  + (6.0*SQRT2_INV*WIGNER_444_224)*c44*c42  - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c43 + c4n1*c4n3);
        tMul2 = (6.0*WIGNER_444_022)*c40*c4n2 - (6.0*SQRT2_INV*WIGNER_444_224)*c44*c4n2 - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c4n3 - c4n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx+2] + tMul2*aCnlmPx[tIdx-2];
        rFp3Py += tMul1*aCnlmPy[tIdx+2] + tMul2*aCnlmPy[tIdx-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx+2] + tMul2*aCnlmPz[tIdx-2];
        tMul1 = (-6.0*WIGNER_444_033)*c40*c43  - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c42 - c4n1*c4n2) + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c44 + c4n1*c4n4);
        tMul2 = (-6.0*WIGNER_444_033)*c40*c4n3 - (6.0*SQRT2_INV*WIGNER_444_123)*(c41*c4n2 + c4n1*c42) + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c4n4 - c4n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx+3] + tMul2*aCnlmPx[tIdx-3];
        rFp3Py += tMul1*aCnlmPy[tIdx+3] + tMul2*aCnlmPy[tIdx-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx+3] + tMul2*aCnlmPz[tIdx-3];
        tMul1 = (6.0*WIGNER_444_044)*c40*c44  + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c43 - c4n1*c4n3);
        tMul2 = (6.0*WIGNER_444_044)*c40*c4n4 + (6.0*SQRT2_INV*WIGNER_444_134)*(c41*c4n3 + c4n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx+4] + tMul2*aCnlmPx[tIdx-4];
        rFp3Py += tMul1*aCnlmPy[tIdx+4] + tMul2*aCnlmPy[tIdx-4];
        rFp3Pz += tMul1*aCnlmPz[tIdx+4] + tMul2*aCnlmPz[tIdx-4];
        
        tMul = (3.0*SQRT2_INV*WIGNER_444_112) * (c41*c41 - c4n1*c4n1);
        rFp3Px += tMul*aCnlmPx[tIdx+2];
        rFp3Py += tMul*aCnlmPy[tIdx+2];
        rFp3Pz += tMul*aCnlmPz[tIdx+2];
        if (rFp != null) rFp3 += tMul*c42;
        tMul = (6.0*SQRT2_INV*WIGNER_444_112) * c4n1*c4n2;
        tMul1 = (6.0*SQRT2_INV*WIGNER_444_112) * c41*c4n2;
        tMul2 = (6.0*SQRT2_INV*WIGNER_444_112) * c41*c4n1;
        rFp3Px += tMul*aCnlmPx[tIdx+1] + tMul1*aCnlmPx[tIdx-1] + tMul2*aCnlmPx[tIdx-2];
        rFp3Py += tMul*aCnlmPy[tIdx+1] + tMul1*aCnlmPy[tIdx-1] + tMul2*aCnlmPy[tIdx-2];
        rFp3Pz += tMul*aCnlmPz[tIdx+1] + tMul1*aCnlmPz[tIdx-1] + tMul2*aCnlmPz[tIdx-2];
        if (rFp != null) rFp3 += tMul*c41;
        
        tMul = (3.0*SQRT2_INV*WIGNER_444_224) * (c42*c42 - c4n2*c4n2);
        rFp3Px += tMul*aCnlmPx[tIdx+4];
        rFp3Py += tMul*aCnlmPy[tIdx+4];
        rFp3Pz += tMul*aCnlmPz[tIdx+4];
        if (rFp != null) rFp3 += tMul*c44;
        tMul = (6.0*SQRT2_INV*WIGNER_444_224) * c4n2*c4n4;
        tMul1 = (6.0*SQRT2_INV*WIGNER_444_224) * c42*c4n4;
        tMul2 = (6.0*SQRT2_INV*WIGNER_444_224) * c42*c4n2;
        rFp3Px += tMul*aCnlmPx[tIdx+2] + tMul1*aCnlmPx[tIdx-2] + tMul2*aCnlmPx[tIdx-4];
        rFp3Py += tMul*aCnlmPy[tIdx+2] + tMul1*aCnlmPy[tIdx-2] + tMul2*aCnlmPy[tIdx-4];
        rFp3Pz += tMul*aCnlmPz[tIdx+2] + tMul1*aCnlmPz[tIdx-2] + tMul2*aCnlmPz[tIdx-4];
        if (rFp != null) rFp3 += tMul*c42;
        
        tMul1 = (-6.0*SQRT2_INV*WIGNER_444_123) * (c42*c43 + c4n2*c4n3) + (6.0*SQRT2_INV*WIGNER_444_134) * (c43*c44 + c4n3*c4n4);
        tMul2 = (-6.0*SQRT2_INV*WIGNER_444_123) * (c42*c4n3 - c4n2*c43) + (6.0*SQRT2_INV*WIGNER_444_134) * (c43*c4n4 - c4n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx+1] + tMul2*aCnlmPx[tIdx-1];
        rFp3Py += tMul1*aCnlmPy[tIdx+1] + tMul2*aCnlmPy[tIdx-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx+1] + tMul2*aCnlmPz[tIdx-1];
        if (rFp != null) rFp3 += tMul1*c41 + tMul2*c4n1;
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_224_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx2 = (2*2+2)+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx4 = (4*4+4)+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_224_000*c20*c20 - WIGNER_224_110*(c21*c21 + c2n1*c2n1) + WIGNER_224_220*(c22*c22 + c2n2*c2n2);
        rFp3Px += tMul*aCnlmPx[tIdx4];
        rFp3Py += tMul*aCnlmPy[tIdx4];
        rFp3Pz += tMul*aCnlmPz[tIdx4];
        if (rFp != null) rFp3 += tMul*c40;
        double tMul1 = (2.0*WIGNER_224_000) * c20*c40;
        double tMul2 = (-2.0*WIGNER_224_011)*(c21*c41 + c2n1*c4n1) + (2.0*WIGNER_224_022)*(c22*c42 + c2n2*c4n2);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx2];
        rFp3Py += tMul*aCnlmPy[tIdx2];
        rFp3Pz += tMul*aCnlmPz[tIdx2];
        if (rFp != null) rFp3 += tMul2*c20;
        
        tMul1 = (-2.0*WIGNER_224_110)*c40*c21  - (2.0*WIGNER_224_011)*c20*c41  + (2.0*SQRT2_INV*WIGNER_224_112)*c42*c21;
        tMul2 = (-2.0*WIGNER_224_110)*c40*c2n1 - (2.0*WIGNER_224_011)*c20*c4n1 - (2.0*SQRT2_INV*WIGNER_224_112)*c42*c2n1;
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        tMul1 = (2.0*WIGNER_224_220)*c40*c22  + (2.0*WIGNER_224_022)*c20*c42  + (2.0*SQRT2_INV*WIGNER_224_224)*c44*c22  + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c41 - c2n1*c4n1) - (2.0*SQRT2_INV*WIGNER_224_123)*(c21*c43 + c2n1*c4n3);
        tMul2 = (2.0*WIGNER_224_220)*c40*c2n2 + (2.0*WIGNER_224_022)*c20*c4n2 - (2.0*SQRT2_INV*WIGNER_224_224)*c44*c2n2 + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c4n1 + c2n1*c41) - (2.0*SQRT2_INV*WIGNER_224_123)*(c21*c4n3 - c2n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx2+2] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul1*aCnlmPy[tIdx2+2] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+2] + tMul2*aCnlmPz[tIdx2-2];
        tMul1 = (-2.0*WIGNER_224_011)*c20*c21  + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c22 + c2n1*c2n2);
        tMul2 = (-2.0*WIGNER_224_011)*c20*c2n1 + (2.0*SQRT2_INV*WIGNER_224_121)*(c21*c2n2 - c2n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdx4+1] + tMul2*aCnlmPx[tIdx4-1];
        rFp3Py += tMul1*aCnlmPy[tIdx4+1] + tMul2*aCnlmPy[tIdx4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+1] + tMul2*aCnlmPz[tIdx4-1];
        tMul1 = (2.0*WIGNER_224_022)*c20*c22;
        tMul2 = (2.0*WIGNER_224_022)*c20*c2n2;
        rFp3Px += tMul1*aCnlmPx[tIdx4+2] + tMul2*aCnlmPx[tIdx4-2];
        rFp3Py += tMul1*aCnlmPy[tIdx4+2] + tMul2*aCnlmPy[tIdx4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+2] + tMul2*aCnlmPz[tIdx4-2];
        
        tMul1 = (SQRT2_INV*WIGNER_224_112) * (c21*c21 - c2n1*c2n1);
        tMul2 = (SQRT2_INV*WIGNER_224_224) * (c22*c22 - c2n2*c2n2);
        rFp3Px += tMul1*aCnlmPx[tIdx4+2] + tMul2*aCnlmPx[tIdx4+4];
        rFp3Py += tMul1*aCnlmPy[tIdx4+2] + tMul2*aCnlmPy[tIdx4+4];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+2] + tMul2*aCnlmPz[tIdx4+4];
        if (rFp != null) rFp3 += tMul1*c42 + tMul2*c44;
        
        tMul = (2.0*SQRT2_INV*WIGNER_224_112) * c2n1*c4n2;
        tMul1 = (2.0*SQRT2_INV*WIGNER_224_112) * c21*c4n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_224_112) * c21*c2n1;
        rFp3Px += tMul*aCnlmPx[tIdx2+1] + tMul1*aCnlmPx[tIdx2-1] + tMul2*aCnlmPx[tIdx4-2];
        rFp3Py += tMul*aCnlmPy[tIdx2+1] + tMul1*aCnlmPy[tIdx2-1] + tMul2*aCnlmPy[tIdx4-2];
        rFp3Pz += tMul*aCnlmPz[tIdx2+1] + tMul1*aCnlmPz[tIdx2-1] + tMul2*aCnlmPz[tIdx4-2];
        if (rFp != null) rFp3 += tMul*c21;
        tMul = (2.0*SQRT2_INV*WIGNER_224_224) * c2n2*c4n4;
        tMul1 = (2.0*SQRT2_INV*WIGNER_224_224) * c22*c4n4;
        tMul2 = (2.0*SQRT2_INV*WIGNER_224_224) * c22*c2n2;
        rFp3Px += tMul*aCnlmPx[tIdx2+2] + tMul1*aCnlmPx[tIdx2-2] + tMul2*aCnlmPx[tIdx4-4];
        rFp3Py += tMul*aCnlmPy[tIdx2+2] + tMul1*aCnlmPy[tIdx2-2] + tMul2*aCnlmPy[tIdx4-4];
        rFp3Pz += tMul*aCnlmPz[tIdx2+2] + tMul1*aCnlmPz[tIdx2-2] + tMul2*aCnlmPz[tIdx4-4];
        if (rFp != null) rFp3 += tMul*c22;
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_224_121)*(c22*c41 + c2n2*c4n1) - (2.0*SQRT2_INV*WIGNER_224_123)*(c22*c43 + c2n2*c4n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_224_121)*(c2n2*c41 - c22*c4n1) - (2.0*SQRT2_INV*WIGNER_224_123)*(c22*c4n3 - c2n2*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        if (rFp != null) rFp3 += tMul1*c21 + tMul2*c2n1;
        tMul1 = (-2.0*SQRT2_INV*WIGNER_224_123)*(c21*c22 - c2n1*c2n2);
        tMul2 = (-2.0*SQRT2_INV*WIGNER_224_123)*(c21*c2n2 + c2n1*c22);
        rFp3Px += tMul1*aCnlmPx[tIdx4+3] + tMul2*aCnlmPx[tIdx4-3];
        rFp3Py += tMul1*aCnlmPy[tIdx4+3] + tMul2*aCnlmPy[tIdx4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+3] + tMul2*aCnlmPz[tIdx4-3];
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_334_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx3 = (3*3+3)+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        final int tIdx4 = (4*4+4)+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_334_000*c30*c30 - WIGNER_334_110*(c31*c31 + c3n1*c3n1) + WIGNER_334_220*(c32*c32 + c3n2*c3n2) - WIGNER_334_330*(c33*c33 + c3n3*c3n3);
        rFp3Px += tMul*aCnlmPx[tIdx4];
        rFp3Py += tMul*aCnlmPy[tIdx4];
        rFp3Pz += tMul*aCnlmPz[tIdx4];
        if (rFp != null) rFp3 += tMul*c40;
        double tMul1 = (2.0*WIGNER_334_000) * c30*c40;
        double tMul2 = (-2.0*WIGNER_334_011)*(c31*c41 + c3n1*c4n1) + (2.0*WIGNER_334_022)*(c32*c42 + c3n2*c4n2) - (2.0*WIGNER_334_033)*(c33*c43 + c3n3*c4n3);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx3];
        rFp3Py += tMul*aCnlmPy[tIdx3];
        rFp3Pz += tMul*aCnlmPz[tIdx3];
        if (rFp != null) rFp3 += tMul2*c30;
        
        tMul1 = (-2.0*WIGNER_334_110)*c40*c31  - (2.0*WIGNER_334_011)*c30*c41  + (2.0*SQRT2_INV*WIGNER_334_112)*c42*c31;
        tMul2 = (-2.0*WIGNER_334_110)*c40*c3n1 - (2.0*WIGNER_334_011)*c30*c4n1 - (2.0*SQRT2_INV*WIGNER_334_112)*c42*c3n1;
        rFp3Px += tMul1*aCnlmPx[tIdx3+1] + tMul2*aCnlmPx[tIdx3-1];
        rFp3Py += tMul1*aCnlmPy[tIdx3+1] + tMul2*aCnlmPy[tIdx3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+1] + tMul2*aCnlmPz[tIdx3-1];
        tMul1 = (2.0*WIGNER_334_220)*c40*c32  + (2.0*WIGNER_334_022)*c30*c42  + (2.0*SQRT2_INV*WIGNER_334_224)*c44*c32  + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c41 - c3n1*c4n1) - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c43 + c3n1*c4n3);
        tMul2 = (2.0*WIGNER_334_220)*c40*c3n2 + (2.0*WIGNER_334_022)*c30*c4n2 - (2.0*SQRT2_INV*WIGNER_334_224)*c44*c3n2 + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c4n1 + c3n1*c41) - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c4n3 - c3n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx3+2] + tMul2*aCnlmPx[tIdx3-2];
        rFp3Py += tMul1*aCnlmPy[tIdx3+2] + tMul2*aCnlmPy[tIdx3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+2] + tMul2*aCnlmPz[tIdx3-2];
        tMul1 = (-2.0*WIGNER_334_330)*c40*c33  - (2.0*WIGNER_334_033)*c30*c43  - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c42 - c3n1*c4n2) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c41 - c3n2*c4n1) + (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c44 + c3n1*c4n4);
        tMul2 = (-2.0*WIGNER_334_330)*c40*c3n3 - (2.0*WIGNER_334_033)*c30*c4n3 - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c4n2 + c3n1*c42) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c4n1 + c3n2*c41) + (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c4n4 - c3n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx3+3] + tMul2*aCnlmPx[tIdx3-3];
        rFp3Py += tMul1*aCnlmPy[tIdx3+3] + tMul2*aCnlmPy[tIdx3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+3] + tMul2*aCnlmPz[tIdx3-3];
        tMul1 = (-2.0*WIGNER_334_011)*c30*c31  + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c32 + c3n1*c3n2) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c33 + c3n2*c3n3);
        tMul2 = (-2.0*WIGNER_334_011)*c30*c3n1 + (2.0*SQRT2_INV*WIGNER_334_121)*(c31*c3n2 - c3n1*c32) - (2.0*SQRT2_INV*WIGNER_334_231)*(c32*c3n3 - c3n2*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx4+1] + tMul2*aCnlmPx[tIdx4-1];
        rFp3Py += tMul1*aCnlmPy[tIdx4+1] + tMul2*aCnlmPy[tIdx4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+1] + tMul2*aCnlmPz[tIdx4-1];
        tMul1 = (2.0*WIGNER_334_022) * c30*c32  - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c33 + c3n1*c3n3);
        tMul2 = (2.0*WIGNER_334_022) * c30*c3n2 - (2.0*SQRT2_INV*WIGNER_334_132)*(c31*c3n3 - c3n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx4+2] + tMul2*aCnlmPx[tIdx4-2];
        rFp3Py += tMul1*aCnlmPy[tIdx4+2] + tMul2*aCnlmPy[tIdx4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+2] + tMul2*aCnlmPz[tIdx4-2];
        tMul1 = (-2.0*WIGNER_334_033)*c30*c33  - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c32 - c3n1*c3n2);
        tMul2 = (-2.0*WIGNER_334_033)*c30*c3n3 - (2.0*SQRT2_INV*WIGNER_334_123)*(c31*c3n2 + c3n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdx4+3] + tMul2*aCnlmPx[tIdx4-3];
        rFp3Py += tMul1*aCnlmPy[tIdx4+3] + tMul2*aCnlmPy[tIdx4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+3] + tMul2*aCnlmPz[tIdx4-3];
        
        tMul1 = (SQRT2_INV*WIGNER_334_112) * (c31*c31 - c3n1*c3n1);
        tMul2 = (SQRT2_INV*WIGNER_334_224) * (c32*c32 - c3n2*c3n2);
        rFp3Px += tMul1*aCnlmPx[tIdx4+2] + tMul2*aCnlmPx[tIdx4+4];
        rFp3Py += tMul1*aCnlmPy[tIdx4+2] + tMul2*aCnlmPy[tIdx4+4];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+2] + tMul2*aCnlmPz[tIdx4+4];
        if (rFp != null) rFp3 += tMul1*c42 + tMul2*c44;
        
        tMul = (2.0*SQRT2_INV*WIGNER_334_112) * c3n1*c4n2;
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_112) * c31*c4n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_112) * c31*c3n1;
        rFp3Px += tMul*aCnlmPx[tIdx3+1] + tMul1*aCnlmPx[tIdx3-1] + tMul2*aCnlmPx[tIdx4-2];
        rFp3Py += tMul*aCnlmPy[tIdx3+1] + tMul1*aCnlmPy[tIdx3-1] + tMul2*aCnlmPy[tIdx4-2];
        rFp3Pz += tMul*aCnlmPz[tIdx3+1] + tMul1*aCnlmPz[tIdx3-1] + tMul2*aCnlmPz[tIdx4-2];
        if (rFp != null) rFp3 += tMul*c31;
        tMul = (2.0*SQRT2_INV*WIGNER_334_224) * c3n2*c4n4;
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_224) * c32*c4n4;
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_224) * c32*c3n2;
        rFp3Px += tMul*aCnlmPx[tIdx3+2] + tMul1*aCnlmPx[tIdx3-2] + tMul2*aCnlmPx[tIdx4-4];
        rFp3Py += tMul*aCnlmPy[tIdx3+2] + tMul1*aCnlmPy[tIdx3-2] + tMul2*aCnlmPy[tIdx4-4];
        rFp3Pz += tMul*aCnlmPz[tIdx3+2] + tMul1*aCnlmPz[tIdx3-2] + tMul2*aCnlmPz[tIdx4-4];
        if (rFp != null) rFp3 += tMul*c32;
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_121)*(c32*c41 + c3n2*c4n1) - (2.0*SQRT2_INV*WIGNER_334_123)*(c32*c43 + c3n2*c4n3) - (2.0*SQRT2_INV*WIGNER_334_132)*(c33*c42 + c3n3*c4n2) + (2.0*SQRT2_INV*WIGNER_334_134)*(c33*c44 + c3n3*c4n4);
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_121)*(c3n2*c41 - c32*c4n1) - (2.0*SQRT2_INV*WIGNER_334_123)*(c32*c4n3 - c3n2*c43) - (2.0*SQRT2_INV*WIGNER_334_132)*(c3n3*c42 - c33*c4n2) + (2.0*SQRT2_INV*WIGNER_334_134)*(c33*c4n4 - c3n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx3+1] + tMul2*aCnlmPx[tIdx3-1];
        rFp3Py += tMul1*aCnlmPy[tIdx3+1] + tMul2*aCnlmPy[tIdx3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+1] + tMul2*aCnlmPz[tIdx3-1];
        if (rFp != null) rFp3 += tMul1*c31 + tMul2*c3n1;
        tMul1 = (-2.0*SQRT2_INV*WIGNER_334_231)*(c33*c41 + c3n3*c4n1);
        tMul2 = (-2.0*SQRT2_INV*WIGNER_334_231)*(c3n3*c41 - c33*c4n1);
        rFp3Px += tMul1*aCnlmPx[tIdx3+2] + tMul2*aCnlmPx[tIdx3-2];
        rFp3Py += tMul1*aCnlmPy[tIdx3+2] + tMul2*aCnlmPy[tIdx3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+2] + tMul2*aCnlmPz[tIdx3-2];
        if (rFp != null) rFp3 += tMul1*c32 + tMul2*c3n2;
        tMul1 = (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c33 - c3n1*c3n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_334_134)*(c31*c3n3 + c3n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx4+4] + tMul2*aCnlmPx[tIdx4-4];
        rFp3Py += tMul1*aCnlmPy[tIdx4+4] + tMul2*aCnlmPy[tIdx4-4];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+4] + tMul2*aCnlmPz[tIdx4-4];
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_244_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx2 = (2*2+2)+aShift;
        final double c20  = aCnlm[tIdx2  ];
        final double c21  = aCnlm[tIdx2+1];
        final double c2n1 = aCnlm[tIdx2-1];
        final double c22  = aCnlm[tIdx2+2];
        final double c2n2 = aCnlm[tIdx2-2];
        final int tIdx4 = (4*4+4)+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_244_000*c40*c40 - WIGNER_244_011*(c41*c41 + c4n1*c4n1) + WIGNER_244_022*(c42*c42 + c4n2*c4n2) - WIGNER_244_033*(c43*c43 + c4n3*c4n3) + WIGNER_244_044*(c44*c44 + c4n4*c4n4);
        rFp3Px += tMul*aCnlmPx[tIdx2];
        rFp3Py += tMul*aCnlmPy[tIdx2];
        rFp3Pz += tMul*aCnlmPz[tIdx2];
        if (rFp != null) rFp3 += tMul*c20;
        double tMul1 = (2.0*WIGNER_244_000) * c20*c40;
        double tMul2 = (-2.0*WIGNER_244_110)*(c21*c41 + c2n1*c4n1) + (2.0*WIGNER_244_220)*(c22*c42 + c2n2*c4n2);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx4];
        rFp3Py += tMul*aCnlmPy[tIdx4];
        rFp3Pz += tMul*aCnlmPz[tIdx4];
        if (rFp != null) rFp3 += tMul2*c40;
        
        tMul1 = (-2.0*WIGNER_244_011)*c20*c41  - (2.0*WIGNER_244_110)*c40*c21  + (2.0*SQRT2_INV*WIGNER_244_211)*c22*c41  + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c42 + c2n1*c4n2) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c43 + c2n2*c4n3);
        tMul2 = (-2.0*WIGNER_244_011)*c20*c4n1 - (2.0*WIGNER_244_110)*c40*c2n1 - (2.0*SQRT2_INV*WIGNER_244_211)*c22*c4n1 + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c4n2 - c2n1*c42) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c4n3 - c2n2*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx4+1] + tMul2*aCnlmPx[tIdx4-1];
        rFp3Py += tMul1*aCnlmPy[tIdx4+1] + tMul2*aCnlmPy[tIdx4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+1] + tMul2*aCnlmPz[tIdx4-1];
        tMul1 = (2.0*WIGNER_244_022)*c20*c42  + (2.0*WIGNER_244_220)*c40*c22  + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c41 - c2n1*c4n1) + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c44 + c2n2*c4n4) - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c43 + c2n1*c4n3);
        tMul2 = (2.0*WIGNER_244_022)*c20*c4n2 + (2.0*WIGNER_244_220)*c40*c2n2 + (2.0*SQRT2_INV*WIGNER_244_112)*(c21*c4n1 + c2n1*c41) + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c4n4 - c2n2*c44) - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c4n3 - c2n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx4+2] + tMul2*aCnlmPx[tIdx4-2];
        rFp3Py += tMul1*aCnlmPy[tIdx4+2] + tMul2*aCnlmPy[tIdx4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+2] + tMul2*aCnlmPz[tIdx4-2];
        tMul1 = (-2.0*WIGNER_244_033)*c20*c43  - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c42 - c2n1*c4n2) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c41 - c2n2*c4n1) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c44 + c2n1*c4n4);
        tMul2 = (-2.0*WIGNER_244_033)*c20*c4n3 - (2.0*SQRT2_INV*WIGNER_244_123)*(c21*c4n2 + c2n1*c42) - (2.0*SQRT2_INV*WIGNER_244_213)*(c22*c4n1 + c2n2*c41) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c4n4 - c2n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx4+3] + tMul2*aCnlmPx[tIdx4-3];
        rFp3Py += tMul1*aCnlmPy[tIdx4+3] + tMul2*aCnlmPy[tIdx4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+3] + tMul2*aCnlmPz[tIdx4-3];
        tMul1 = (2.0*WIGNER_244_044)*c20*c44  + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c42 - c2n2*c4n2) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c43 - c2n1*c4n3);
        tMul2 = (2.0*WIGNER_244_044)*c20*c4n4 + (2.0*SQRT2_INV*WIGNER_244_224)*(c22*c4n2 + c2n2*c42) + (2.0*SQRT2_INV*WIGNER_244_134)*(c21*c4n3 + c2n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx4+4] + tMul2*aCnlmPx[tIdx4-4];
        rFp3Py += tMul1*aCnlmPy[tIdx4+4] + tMul2*aCnlmPy[tIdx4-4];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+4] + tMul2*aCnlmPz[tIdx4-4];
        tMul1 = (-2.0*WIGNER_244_110)*c40*c41;
        tMul2 = (-2.0*WIGNER_244_110)*c40*c4n1;
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        tMul1 = (2.0*WIGNER_244_220)*c40*c42;
        tMul2 = (2.0*WIGNER_244_220)*c40*c4n2;
        rFp3Px += tMul1*aCnlmPx[tIdx2+2] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul1*aCnlmPy[tIdx2+2] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+2] + tMul2*aCnlmPz[tIdx2-2];
        
        tMul = (SQRT2_INV*WIGNER_244_211) * (c41*c41 - c4n1*c4n1);
        rFp3Px += tMul*aCnlmPx[tIdx2+2];
        rFp3Py += tMul*aCnlmPy[tIdx2+2];
        rFp3Pz += tMul*aCnlmPz[tIdx2+2];
        if (rFp != null) rFp3 += tMul*c22;
        tMul = (2.0*SQRT2_INV*WIGNER_244_211) * c2n2*c4n1;
        tMul1 = (2.0*SQRT2_INV*WIGNER_244_211) * c41*c2n2;
        tMul2 = (2.0*SQRT2_INV*WIGNER_244_211) * c41*c4n1;
        rFp3Px += tMul*aCnlmPx[tIdx4+1] + tMul1*aCnlmPx[tIdx4-1] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul*aCnlmPy[tIdx4+1] + tMul1*aCnlmPy[tIdx4-1] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul*aCnlmPz[tIdx4+1] + tMul1*aCnlmPz[tIdx4-1] + tMul2*aCnlmPz[tIdx2-2];
        if (rFp != null) rFp3 += tMul*c41;
        
        tMul1 = (2.0*SQRT2_INV*WIGNER_244_112)*(c41*c42 + c4n1*c4n2) - (2.0*SQRT2_INV*WIGNER_244_123)*(c42*c43 + c4n2*c4n3) + (2.0*SQRT2_INV*WIGNER_244_134)*(c43*c44 + c4n3*c4n4);
        tMul2 = (2.0*SQRT2_INV*WIGNER_244_112)*(c41*c4n2 - c4n1*c42) - (2.0*SQRT2_INV*WIGNER_244_123)*(c42*c4n3 - c4n2*c43) + (2.0*SQRT2_INV*WIGNER_244_134)*(c43*c4n4 - c4n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx2+1] + tMul2*aCnlmPx[tIdx2-1];
        rFp3Py += tMul1*aCnlmPy[tIdx2+1] + tMul2*aCnlmPy[tIdx2-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+1] + tMul2*aCnlmPz[tIdx2-1];
        if (rFp != null) rFp3 += tMul1*c21 + tMul2*c2n1;
        tMul1 = (2.0*SQRT2_INV*WIGNER_244_224)*(c42*c44 + c4n2*c4n4) - (2.0*SQRT2_INV*WIGNER_244_213)*(c41*c43 + c4n1*c4n3);
        tMul2 = (2.0*SQRT2_INV*WIGNER_244_224)*(c42*c4n4 - c4n2*c44) - (2.0*SQRT2_INV*WIGNER_244_213)*(c41*c4n3 - c4n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx2+2] + tMul2*aCnlmPx[tIdx2-2];
        rFp3Py += tMul1*aCnlmPy[tIdx2+2] + tMul2*aCnlmPy[tIdx2-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx2+2] + tMul2*aCnlmPz[tIdx2-2];
        if (rFp != null) rFp3 += tMul1*c22 + tMul2*c2n2;
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
    private static void calL3_134_(double[] aCnlm, double[] aCnlmPx, double[] aCnlmPy, double[] aCnlmPz,
                                   double @Nullable[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                   int aShift, int aIdxFP) {
        final int tIdx1 = (1+1)+aShift;
        final double c10  = aCnlm[tIdx1  ];
        final double c11  = aCnlm[tIdx1+1];
        final double c1n1 = aCnlm[tIdx1-1];
        final int tIdx3 = (3*3+3)+aShift;
        final double c30  = aCnlm[tIdx3  ];
        final double c31  = aCnlm[tIdx3+1];
        final double c3n1 = aCnlm[tIdx3-1];
        final double c32  = aCnlm[tIdx3+2];
        final double c3n2 = aCnlm[tIdx3-2];
        final double c33  = aCnlm[tIdx3+3];
        final double c3n3 = aCnlm[tIdx3-3];
        final int tIdx4 = (4*4+4)+aShift;
        final double c40  = aCnlm[tIdx4  ];
        final double c41  = aCnlm[tIdx4+1];
        final double c4n1 = aCnlm[tIdx4-1];
        final double c42  = aCnlm[tIdx4+2];
        final double c4n2 = aCnlm[tIdx4-2];
        final double c43  = aCnlm[tIdx4+3];
        final double c4n3 = aCnlm[tIdx4-3];
        final double c44  = aCnlm[tIdx4+4];
        final double c4n4 = aCnlm[tIdx4-4];
        double rFp3 = 0.0;
        double rFp3Px = 0.0, rFp3Py = 0.0, rFp3Pz = 0.0;
        // 这里做简单的合并同项优化，为了保证一定的可读性没有优化到最优
        double tMul = WIGNER_134_000*c30*c40 - WIGNER_134_011*(c31*c41 + c3n1*c4n1) + WIGNER_134_022*(c32*c42 + c3n2*c4n2) - WIGNER_134_033*(c33*c43 + c3n3*c4n3);
        rFp3Px += tMul*aCnlmPx[tIdx1];
        rFp3Py += tMul*aCnlmPy[tIdx1];
        rFp3Pz += tMul*aCnlmPz[tIdx1];
        if (rFp != null) rFp3 += tMul*c10;
        double tMul1 = WIGNER_134_000 * c10*c30;
        double tMul2 = (-WIGNER_134_110)*(c11*c31 + c1n1*c3n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx4];
        rFp3Py += tMul*aCnlmPy[tIdx4];
        rFp3Pz += tMul*aCnlmPz[tIdx4];
        if (rFp != null) rFp3 += tMul2*c40;
        tMul1 = WIGNER_134_000 * c10*c40;
        tMul2 = (-WIGNER_134_101)*(c11*c41 + c1n1*c4n1);
        tMul = tMul1 + tMul2;
        rFp3Px += tMul*aCnlmPx[tIdx3];
        rFp3Py += tMul*aCnlmPy[tIdx3];
        rFp3Pz += tMul*aCnlmPz[tIdx3];
        if (rFp != null) rFp3 += tMul2*c30;
        
        tMul1 = (-WIGNER_134_011)*c10*c41  - WIGNER_134_110*c40*c11  + (SQRT2_INV*WIGNER_134_112)*(c11*c42 + c1n1*c4n2);
        tMul2 = (-WIGNER_134_011)*c10*c4n1 - WIGNER_134_110*c40*c1n1 + (SQRT2_INV*WIGNER_134_112)*(c11*c4n2 - c1n1*c42);
        rFp3Px += tMul1*aCnlmPx[tIdx3+1] + tMul2*aCnlmPx[tIdx3-1];
        rFp3Py += tMul1*aCnlmPy[tIdx3+1] + tMul2*aCnlmPy[tIdx3-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+1] + tMul2*aCnlmPz[tIdx3-1];
        tMul1 = WIGNER_134_022*c10*c42  + (SQRT2_INV*WIGNER_134_121)*(c11*c41 - c1n1*c4n1) - (SQRT2_INV*WIGNER_134_123)*(c11*c43 + c1n1*c4n3);
        tMul2 = WIGNER_134_022*c10*c4n2 + (SQRT2_INV*WIGNER_134_121)*(c11*c4n1 + c1n1*c41) - (SQRT2_INV*WIGNER_134_123)*(c11*c4n3 - c1n1*c43);
        rFp3Px += tMul1*aCnlmPx[tIdx3+2] + tMul2*aCnlmPx[tIdx3-2];
        rFp3Py += tMul1*aCnlmPy[tIdx3+2] + tMul2*aCnlmPy[tIdx3-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+2] + tMul2*aCnlmPz[tIdx3-2];
        tMul1 = (-WIGNER_134_033)*c10*c43  - (SQRT2_INV*WIGNER_134_132)*(c11*c42 - c1n1*c4n2) + (SQRT2_INV*WIGNER_134_134)*(c11*c44 + c1n1*c4n4);
        tMul2 = (-WIGNER_134_033)*c10*c4n3 - (SQRT2_INV*WIGNER_134_132)*(c11*c4n2 + c1n1*c42) + (SQRT2_INV*WIGNER_134_134)*(c11*c4n4 - c1n1*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx3+3] + tMul2*aCnlmPx[tIdx3-3];
        rFp3Py += tMul1*aCnlmPy[tIdx3+3] + tMul2*aCnlmPy[tIdx3-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx3+3] + tMul2*aCnlmPz[tIdx3-3];
        tMul1 = (-WIGNER_134_011)*c10*c31  - WIGNER_134_101*c30*c11  + (SQRT2_INV*WIGNER_134_121)*(c11*c32 + c1n1*c3n2);
        tMul2 = (-WIGNER_134_011)*c10*c3n1 - WIGNER_134_101*c30*c1n1 + (SQRT2_INV*WIGNER_134_121)*(c11*c3n2 - c1n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdx4+1] + tMul2*aCnlmPx[tIdx4-1];
        rFp3Py += tMul1*aCnlmPy[tIdx4+1] + tMul2*aCnlmPy[tIdx4-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+1] + tMul2*aCnlmPz[tIdx4-1];
        tMul1 = WIGNER_134_022*c10*c32  + (SQRT2_INV*WIGNER_134_112)*(c11*c31 - c1n1*c3n1) - (SQRT2_INV*WIGNER_134_132)*(c11*c33 + c1n1*c3n3);
        tMul2 = WIGNER_134_022*c10*c3n2 + (SQRT2_INV*WIGNER_134_112)*(c11*c3n1 + c1n1*c31) - (SQRT2_INV*WIGNER_134_132)*(c11*c3n3 - c1n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx4+2] + tMul2*aCnlmPx[tIdx4-2];
        rFp3Py += tMul1*aCnlmPy[tIdx4+2] + tMul2*aCnlmPy[tIdx4-2];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+2] + tMul2*aCnlmPz[tIdx4-2];
        tMul1 = (-WIGNER_134_033)*c10*c33  - (SQRT2_INV*WIGNER_134_123)*(c11*c32 - c1n1*c3n2);
        tMul2 = (-WIGNER_134_033)*c10*c3n3 - (SQRT2_INV*WIGNER_134_123)*(c11*c3n2 + c1n1*c32);
        rFp3Px += tMul1*aCnlmPx[tIdx4+3] + tMul2*aCnlmPx[tIdx4-3];
        rFp3Py += tMul1*aCnlmPy[tIdx4+3] + tMul2*aCnlmPy[tIdx4-3];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+3] + tMul2*aCnlmPz[tIdx4-3];
        tMul1 = (-WIGNER_134_110)*c40*c31  - WIGNER_134_101*c30*c41 ;
        tMul2 = (-WIGNER_134_110)*c40*c3n1 - WIGNER_134_101*c30*c4n1;
        rFp3Px += tMul1*aCnlmPx[tIdx1+1] + tMul2*aCnlmPx[tIdx1-1];
        rFp3Py += tMul1*aCnlmPy[tIdx1+1] + tMul2*aCnlmPy[tIdx1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx1+1] + tMul2*aCnlmPz[tIdx1-1];
        
        tMul1 = (SQRT2_INV*WIGNER_134_112)*(c31*c42 + c3n1*c4n2) + (SQRT2_INV*WIGNER_134_121)*(c32*c41 + c3n2*c4n1) - (SQRT2_INV*WIGNER_134_123)*(c32*c43 + c3n2*c4n3) - (SQRT2_INV*WIGNER_134_132)*(c33*c42 + c3n3*c4n2) + (SQRT2_INV*WIGNER_134_134)*(c33*c44 + c3n3*c4n4);
        tMul2 = (SQRT2_INV*WIGNER_134_112)*(c31*c4n2 - c3n1*c42) + (SQRT2_INV*WIGNER_134_121)*(c3n2*c41 - c32*c4n1) - (SQRT2_INV*WIGNER_134_123)*(c32*c4n3 - c3n2*c43) - (SQRT2_INV*WIGNER_134_132)*(c3n3*c42 - c33*c4n2) + (SQRT2_INV*WIGNER_134_134)*(c33*c4n4 - c3n3*c44);
        rFp3Px += tMul1*aCnlmPx[tIdx1+1] + tMul2*aCnlmPx[tIdx1-1];
        rFp3Py += tMul1*aCnlmPy[tIdx1+1] + tMul2*aCnlmPy[tIdx1-1];
        rFp3Pz += tMul1*aCnlmPz[tIdx1+1] + tMul2*aCnlmPz[tIdx1-1];
        if (rFp != null) rFp3 += tMul1*c11 + tMul2*c1n1;
        tMul1 = (SQRT2_INV*WIGNER_134_134)*(c11*c33 - c1n1*c3n3);
        tMul2 = (SQRT2_INV*WIGNER_134_134)*(c11*c3n3 + c1n1*c33);
        rFp3Px += tMul1*aCnlmPx[tIdx4+4] + tMul2*aCnlmPx[tIdx4-4];
        rFp3Py += tMul1*aCnlmPy[tIdx4+4] + tMul2*aCnlmPy[tIdx4-4];
        rFp3Pz += tMul1*aCnlmPz[tIdx4+4] + tMul2*aCnlmPz[tIdx4-4];
        rFpPx[aIdxFP] = rFp3Px;
        rFpPy[aIdxFP] = rFp3Py;
        rFpPz[aIdxFP] = rFp3Pz;
        if (rFp != null) rFp[aIdxFP] = rFp3;
    }
}
