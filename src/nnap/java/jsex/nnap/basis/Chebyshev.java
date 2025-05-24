package jsex.nnap.basis;

import jse.cache.VectorCache;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.MathEX;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jsex.nnap.basis.BASIS.lengthCheck;
import static jsex.nnap.basis.BASIS.lengthCheckI;

/**
 * 一种仅使用 Chebyshev 多项式将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这不会包含角向序，但是速度可以很快。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * 现在统一通过调用 c 并借助 avx 指令优化来得到最佳的性能
 * <p>
 * References:
 * <a href="https://link.springer.com/article/10.1007/s40843-024-2953-9">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * @author Su Rui, liqa
 */
public class Chebyshev extends NNAPWTypeBasis implements IBasis {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(_INIT_FLAG);
        }
    }
    
    private final static boolean _INIT_FLAG;
    static {
        InitHelper.INITIALIZED = true;
        // 确保 BASIS 已经确实初始化
        BASIS.InitHelper.init();
        _INIT_FLAG = false;
    }
    
    public final static int DEFAULT_NMAX = 5;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    final int mTypeNum;
    final String @Nullable[] mSymbols;
    final int mNMax;
    final double mRCut;
    final int mWType;
    
    final int mSize;
    
    /** 一些缓存的中间变量，现在统一作为对象存储，对于这种大规模的缓存情况可以进一步提高效率 */
    private final Vector mRn, mRnPx, mRnPy, mRnPz, mCheby2;
    
    final DoubleList mNlDx = new DoubleList(16), mNlDy = new DoubleList(16), mNlDz = new DoubleList(16);
    final IntList mNlType = new IntList(16);
    final DoubleList mNlRn = new DoubleList(128);
    
    Chebyshev(String @Nullable[] aSymbols, int aTypeNum, int aNMax, double aRCut, int aWType) {
        if (aTypeNum <= 0) throw new IllegalArgumentException("Inpute ntypes MUST be Positive, input: "+aTypeNum);
        if (aNMax < 0) throw new IllegalArgumentException("Input nmax MUST be Non-Negative, input: "+aNMax);
        if (!ALL_WTYPE.containsValue(aWType)) throw new IllegalArgumentException("Input wtype MUST be in {-1, 0, 1, 2, 3}, input: "+ aWType);
        mSymbols = aSymbols;
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mRCut = aRCut;
        mWType = aWType;
        
        mSize = sizeN_(mNMax, mTypeNum, mWType);
        
        mRn = VectorCache.getVec(mNMax+1);
        mRnPx = VectorCache.getVec(mNMax+1);
        mRnPy = VectorCache.getVec(mNMax+1);
        mRnPz = VectorCache.getVec(mNMax+1);
        mCheby2 = VectorCache.getVec(mNMax);
    }
    /**
     * @param aSymbols 基组需要的元素排序
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aRCut 截断半径
     */
    public Chebyshev(String @NotNull[] aSymbols, int aNMax, double aRCut) {
        this(aSymbols, aSymbols.length, aNMax, aRCut, WTYPE_DEFAULT);
    }
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aRCut 截断半径
     */
    public Chebyshev(int aTypeNum, int aNMax, double aRCut) {
        this(null, aTypeNum, aNMax, aRCut, WTYPE_DEFAULT);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("rcut", mRCut);
        rSaveTo.put("wtype", ALL_WTYPE.inverse().get(mWType));
    }
    
    @SuppressWarnings("rawtypes")
    public static Chebyshev load(String @NotNull[] aSymbols, Map aMap) {
        return new Chebyshev(
            aSymbols, aSymbols.length,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            getWType_(UT.Code.get(aMap, "wtype"))
        );
    }
    @SuppressWarnings("rawtypes")
    public static Chebyshev load(int aTypeNum, Map aMap) {
        return new Chebyshev(
            null, aTypeNum,
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue(),
            getWType_(UT.Code.get(aMap, "wtype"))
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
    
    boolean mDead = false;
    @Override public boolean isShutdown() {return mDead;}
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        VectorCache.returnVec(mRn);
        VectorCache.returnVec(mRnPx);
        VectorCache.returnVec(mRnPy);
        VectorCache.returnVec(mRnPz);
        VectorCache.returnVec(mCheby2);
    }
    
    @Override public void eval(IDxyzTypeIterable aNL, DoubleArrayVector rFp) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        int tSizeFp = rFp.size();
        if (mSize > tSizeFp) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFp);
        
        // 统一缓存近邻列表
        buildNL(aNL);
        final int tNN = mNlDx.size();
        
        // 现在直接计算基组
        eval0(tNN, rFp);
    }
    
    @Override public final void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz) {
        evalPartial(aNL, rFp, rFpPx, rFpPy, rFpPz, null, null, null);
    }
    @Override public void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz, @Nullable DoubleList rFpPxCross, @Nullable DoubleList rFpPyCross, @Nullable DoubleList rFpPzCross) {
        if (mDead) throw new IllegalStateException("This Basis is dead");
        int tSizeFp = rFp.size();
        int tSizeFpPx = rFpPx.size();
        int tSizeFpPy = rFpPy.size();
        int tSizeFpPz = rFpPz.size();
        if (mSize > tSizeFp) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFp);
        if (mSize > tSizeFpPx) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPx);
        if (mSize > tSizeFpPy) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPy);
        if (mSize > tSizeFpPz) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPz);
        
        // 统一缓存近邻列表
        buildNL(aNL);
        final int tNN = mNlDx.size();
        
        // 确保 Rn 的长度
        validSize_(mNlRn, tNN*(mNMax+1));
        
        // 初始化偏导数相关值
        int tSizeAll = tSizeFp + rFp.internalDataShift();
        if (rFpPxCross != null) {
            assert rFpPyCross!=null && rFpPzCross!=null;
            validSize_(rFpPxCross, tNN*tSizeAll);
            validSize_(rFpPyCross, tNN*tSizeAll);
            validSize_(rFpPzCross, tNN*tSizeAll);
        }
        
        // 现在直接计算基组偏导
        evalPartial0(tNN, rFp, rFpPx, rFpPy, rFpPz, rFpPxCross, rFpPyCross, rFpPzCross);
    }
    
    void buildNL(IDxyzTypeIterable aNL) {
        // 缓存情况需要先清空这些
        mNlDx.clear(); mNlDy.clear(); mNlDz.clear();
        mNlType.clear();
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            // 现在不再检测距离，因为需要处理合并情况下截断不一致的情况
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            // 简单缓存近邻列表
            mNlDx.add(dx); mNlDy.add(dy); mNlDz.add(dz);
            mNlType.add(type);
        });
    }
    
    void eval0(int aNN, IDataShell<double[]> rFp) {
        int tShiftFp = rFp.internalDataShift();
        eval1(lengthCheck(mNlDx, aNN), lengthCheck(mNlDy, aNN), lengthCheck(mNlDz, aNN), lengthCheckI(mNlType, aNN), aNN,
              lengthCheck(mRn, mNMax+1), lengthCheck(rFp, mSize, tShiftFp), tShiftFp,
              mTypeNum, mRCut, mNMax, mWType);
    }
    private static native void eval1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                     double[] rRn, double[] rFp, int aShiftFp,
                                     int aTypeNum, double aRCut, int aNMax, int aWType);
    
    void evalPartial0(int aNN, IDataShell<double[]> rFp, IDataShell<double[]> rFpPx, IDataShell<double[]> rFpPy, IDataShell<double[]> rFpPz,
                      @Nullable IDataShell<double[]> rFpPxCross, @Nullable IDataShell<double[]> rFpPyCross, @Nullable IDataShell<double[]> rFpPzCross) {
        int tShiftFp = rFp.internalDataShift();
        int tSizeFp = rFp.internalDataSize();
        evalPartial1(lengthCheck(mNlDx, aNN), lengthCheck(mNlDy, aNN), lengthCheck(mNlDz, aNN), lengthCheckI(mNlType, aNN), aNN,
                     lengthCheck(mNlRn, aNN*(mNMax+1)), lengthCheck(mRnPx, mNMax+1), lengthCheck(mRnPy, mNMax+1), lengthCheck(mRnPz, mNMax+1), lengthCheck(mCheby2, mNMax),
                     lengthCheck(rFp, mSize, tShiftFp), lengthCheck(rFpPx, mSize, tShiftFp), lengthCheck(rFpPy, mSize, tShiftFp), lengthCheck(rFpPz, mSize, tShiftFp), tSizeFp, tShiftFp,
                     rFpPxCross!=null?lengthCheck(rFpPxCross, aNN*(tSizeFp+tShiftFp)):null,
                     rFpPyCross!=null?lengthCheck(rFpPyCross, aNN*(tSizeFp+tShiftFp)):null,
                     rFpPzCross!=null?lengthCheck(rFpPzCross, aNN*(tSizeFp+tShiftFp)):null,
                     mTypeNum, mRCut, mNMax, mWType);
    }
    private static native void evalPartial1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                            double[] rNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, double[] rCheby2,
                                            double[] rFp, double[] rFpPx, double[] rFpPy, double[] rFpPz, int aSizeFp, int aShiftFp,
                                            double @Nullable[] rFpPxCross, double @Nullable[] rFpPyCross, double @Nullable[] rFpPzCross,
                                            int aTypeNum, double aRCut, int aNMax, int aWType);
}
