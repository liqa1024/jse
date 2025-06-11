package jsex.nnap.basis;

import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
public class Chebyshev extends NNAPWTypeBasis {
    public final static int DEFAULT_NMAX = 5;
    public final static double DEFAULT_RCUT = 6.0; // 现在默认值统一为 6
    
    final int mTypeNum;
    final String @Nullable[] mSymbols;
    final int mNMax;
    final double mRCut;
    final int mWType;
    
    final int mSize;
    
    /** 一些缓存的中间变量，现在统一作为对象存储，对于这种大规模的缓存情况可以进一步提高效率 */
    final IDataShell<double[]> mRn, mRnPx, mRnPy, mRnPz, mCheby2;
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
        
        mRn = Vectors.zeros(mNMax+1);
        mRnPx = Vectors.zeros(mNMax+1);
        mRnPy = Vectors.zeros(mNMax+1);
        mRnPz = Vectors.zeros(mNMax+1);
        mCheby2 = Vectors.zeros(mNMax);
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
    
    @Override
    public void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 现在直接计算基组
        eval0(aNlDx, aNlDy, aNlDz, aNlType, rFp);
    }
    
    @Override
    public void evalPartial_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                             DoubleArrayVector rFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        
        // 确保 Rn 的长度
        validSize_(mNlRn, aNlDx.size()*(mNMax+1));
        
        // 现在直接计算基组偏导
        evalPartial0(aNlDx, aNlDy, aNlDz, aNlType, rFp, rFpPx, rFpPy, rFpPz);
    }
    
    void eval0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType, IDataShell<double[]> rFp) {
        int tNN = aNlDx.internalDataSize();
        eval1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
              mRn.internalDataWithLengthCheck(mNMax+1, 0), rFp.internalDataWithLengthCheck(mSize), rFp.internalDataShift(),
              mTypeNum, mRCut, mNMax, mWType);
    }
    private static native void eval1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                     double[] rRn, double[] rFp, int aShiftFp,
                                     int aTypeNum, double aRCut, int aNMax, int aWType);
    
    void evalPartial0(IDataShell<double[]> aNlDx, IDataShell<double[]> aNlDy, IDataShell<double[]> aNlDz, IDataShell<int[]> aNlType,
                      IDataShell<double[]> rFp, IDataShell<double[]> rFpPx, IDataShell<double[]> rFpPy, IDataShell<double[]> rFpPz) {
        int tNN = aNlDx.internalDataSize();
        int tShiftFp = rFp.internalDataShift();
        int tSizeFp = rFp.internalDataSize();
        evalPartial1(aNlDx.internalDataWithLengthCheck(tNN, 0), aNlDy.internalDataWithLengthCheck(tNN, 0), aNlDz.internalDataWithLengthCheck(tNN, 0), aNlType.internalDataWithLengthCheck(tNN, 0), tNN,
                     mNlRn.internalDataWithLengthCheck(tNN*(mNMax+1), 0), mRnPx.internalDataWithLengthCheck(mNMax+1, 0), mRnPy.internalDataWithLengthCheck(mNMax+1, 0), mRnPz.internalDataWithLengthCheck(mNMax+1, 0), mCheby2.internalDataWithLengthCheck(mNMax, 0),
                     rFp.internalDataWithLengthCheck(mSize), tSizeFp, tShiftFp,
                     rFpPx.internalDataWithLengthCheck(tNN*(tSizeFp+tShiftFp), 0), rFpPy.internalDataWithLengthCheck(tNN*(tSizeFp+tShiftFp), 0), rFpPz.internalDataWithLengthCheck(tNN*(tSizeFp+tShiftFp), 0),
                     mTypeNum, mRCut, mNMax, mWType);
    }
    private static native void evalPartial1(double[] aNlDx, double[] aNlDy, double[] aNlDz, int[] aNlType, int aNN,
                                            double[] rNlRn, double[] rRnPx, double[] rRnPy, double[] rRnPz, double[] rCheby2,
                                            double[] rFp, int aSizeFp, int aShiftFp, double[] rFpPx, double[] rFpPy, double[] rFpPz,
                                            int aTypeNum, double aRCut, int aNMax, int aWType);
}
