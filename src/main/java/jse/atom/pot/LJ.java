package jse.atom.pot;

import jse.atom.IPairPotential;
import jse.math.MathEX;
import org.jetbrains.annotations.Nullable;

/**
 * LJ 势（Lennard-Jones）的 jse 实现，采用公式：{@code E = 4ε[(σ/r)^12 - (σ/r)^6]}；
 * 默认情况下不对截断处进行能量的 shift。
 * @author liqa
 */
public class LJ implements IPairPotential {
    private final double mCutMax;
    private final double[][] mCutsq;
    private final double[][] mLJ1, mLJ2, mLJ3, mLJ4;
    private final double[][] mOffset;
    private final int mTypeNum;
    private final String @Nullable[] mSymbols;
    
    /**
     * 创建一个 LJ 势函数，{@code E = 4ε[(σ/r)^12 - (σ/r)^6]}，
     * 不考虑原子种类都使用相同的参数
     * @param aEpsilon 公式中的 {@code ε} 值
     * @param aSigma 公式中的 {@code σ} 值
     * @param aRCut 需要的截断半径值
     */
    public LJ(double aEpsilon, double aSigma, double aRCut) {
        mTypeNum = -1;
        mSymbols = null;
        mLJ1 = new double[][]{{48.0 * aEpsilon * MathEX.Fast.powFast(aSigma, 12)}};
        mLJ2 = new double[][]{{24.0 * aEpsilon * MathEX.Fast.powFast(aSigma,  6)}};
        mLJ3 = new double[][]{{ 4.0 * aEpsilon * MathEX.Fast.powFast(aSigma, 12)}};
        mLJ4 = new double[][]{{ 4.0 * aEpsilon * MathEX.Fast.powFast(aSigma,  6)}};
        mCutsq = new double[][]{{aRCut*aRCut}};
        double tRatio = aSigma / aRCut;
        mOffset = new double[][]{{4.0 * aEpsilon * (MathEX.Fast.powFast(tRatio, 12) - MathEX.Fast.powFast(tRatio, 6))}};
        mCutMax = aRCut;
    }
    /**
     * 创建一个 LJ 势函数，{@code E = 4ε[(σ/r)^12 - (σ/r)^6]}，
     * 不同原子种类使用不同的参数
     * @param aEpsilon 公式中的 {@code ε} 值，{@code aEpsilon[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aSigma 公式中的 {@code σ} 值，{@code aSigma[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aRCut 需要的截断半径值，{@code aRCut[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aSymbols 可选的元素符号信息，如果输入则会根据此元素符号自动映射输入的原子数据，默认为 {@code null}
     */
    public LJ(double[][] aEpsilon, double[][] aSigma, double[][] aRCut, String @Nullable[] aSymbols) {
        mTypeNum = aRCut.length;
        if (aSigma.length != mTypeNum) throw new IllegalArgumentException("Input Sigma size MUST be the same size of RCut");
        if (aEpsilon.length != mTypeNum) throw new IllegalArgumentException("Input Epsilon size MUST be the same size of RCut");
        if (aSymbols!=null && aSymbols.length!=mTypeNum) throw new IllegalArgumentException("Input Symbols size MUST be the same size of RCut");
        mSymbols = aSymbols;
        mLJ1 = new double[mTypeNum+1][mTypeNum+1];
        mLJ2 = new double[mTypeNum+1][mTypeNum+1];
        mLJ3 = new double[mTypeNum+1][mTypeNum+1];
        mLJ4 = new double[mTypeNum+1][mTypeNum+1];
        mCutsq = new double[mTypeNum+1][mTypeNum+1];
        mOffset = new double[mTypeNum+1][mTypeNum+1];
        double tCutMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
            double tEpsilon = aEpsilon[i][j];
            double tSigma = aSigma[i][j];
            mLJ1[i+1][j+1] = 48.0 * tEpsilon * MathEX.Fast.powFast(tSigma, 12);
            mLJ2[i+1][j+1] = 24.0 * tEpsilon * MathEX.Fast.powFast(tSigma,  6);
            mLJ3[i+1][j+1] =  4.0 * tEpsilon * MathEX.Fast.powFast(tSigma, 12);
            mLJ4[i+1][j+1] =  4.0 * tEpsilon * MathEX.Fast.powFast(tSigma,  6);
            double tRCut = aRCut[i][j];
            mCutsq[i+1][j+1] =  tRCut*tRCut;
            double tRatio = tSigma / tRCut;
            mOffset[i+1][j+1] = 4.0 * tEpsilon * (MathEX.Fast.powFast(tRatio, 12) - MathEX.Fast.powFast(tRatio, 6));
            if (tRCut > tCutMax) tCutMax = tRCut;
        }
        mCutMax = tCutMax;
        for (int j = 2; j <= mTypeNum; ++j) for (int i = 1; i < j; ++i) {
            mLJ1[i][j] = mLJ1[j][i];
            mLJ2[i][j] = mLJ2[j][i];
            mLJ3[i][j] = mLJ3[j][i];
            mLJ4[i][j] = mLJ4[j][i];
            mCutsq[i][j] = mCutsq[j][i];
            mOffset[i][j] = mOffset[j][i];
        }
    }
    /**
     * 创建一个 LJ 势函数，{@code E = 4ε[(σ/r)^12 - (σ/r)^6]}，
     * 不同原子种类使用不同的参数
     * @param aEpsilon 公式中的 {@code ε} 值，{@code aEpsilon[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aSigma 公式中的 {@code σ} 值，{@code aSigma[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aRCut 需要的截断半径值，{@code aRCut[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     */
    public LJ(double[][] aEpsilon, double[][] aSigma, double[][] aRCut) {
        this(aEpsilon, aSigma, aRCut, null);
    }
    
    private boolean mShift = false;
    /** @return 此 LJ 势是否势进行了能量平移，保证截断处能量为 {@code 0}，默认为 {@code false} */
    public boolean shift() {return mShift;}
    /**
     * 设置此 LJ 势进行能量平移，保证截断处能量为 {@code 0}
     * @return 自身方便链式调用
     */
    public LJ setShift() {return setShift(true);}
    /**
     * 设置此 LJ 势是否进行能量平移，保证截断处能量为 {@code 0}
     * @param aShift 是否进行能量平移，默认为 {@code false}
     * @return 自身方便链式调用
     */
    public LJ setShift(boolean aShift) {mShift = aShift; return this;}
    
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return mTypeNum;}
    /** @return {@inheritDoc} */
    @Override public boolean hasSymbol() {return mSymbols!=null;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public @Nullable String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public double rcutMax() {return mCutMax;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean manybody() {return false;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean neighborListChecked() {return true;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean neighborListHalf() {return true;}

    
    private int mThreadNum = 1;
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public int threadNumber() {return mThreadNum;}
    /**
     * 设置传入原子数据后计算使用的默认线程数
     * @param aThreadNum 需要设置的线程数，默认为 {@code 1}
     * @return 自身方便链式调用
     */
    public LJ setThreadNum(int aThreadNum) {mThreadNum = aThreadNum; return this;}
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) {
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            nl.forEachDxyzTypeIdx(mCutMax, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq[cType][type]) return;
                double r2inv = 1.0 / rsq;
                double r6inv = r2inv*r2inv*r2inv;
                double deng = r6inv*(mLJ3[cType][type]*r6inv - mLJ4[cType][type]);
                if (mShift) deng -= mOffset[cType][type];
                rEnergyAccumulator.add(threadID, cIdx, idx, deng);
            });
        });
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     * @param rForceAccumulator {@inheritDoc}
     * @param rVirialAccumulator {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public void calEnergyForceVirial(int aAtomNumber, INeighborListGetter aNeighborListGetter, @Nullable IEnergyAccumulator rEnergyAccumulator, @Nullable IForceAccumulator rForceAccumulator, @Nullable IVirialAccumulator rVirialAccumulator) throws Exception {
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            nl.forEachDxyzTypeIdx(mCutMax, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq[cType][type]) return;
                double r2inv = 1.0 / rsq;
                double r6inv = r2inv*r2inv*r2inv;
                double fpair = r2inv*r6inv*(mLJ1[cType][type]*r6inv - mLJ2[cType][type]);
                double fx = dx*fpair;
                double fy = dy*fpair;
                double fz = dz*fpair;
                if (rForceAccumulator != null) {
                    rForceAccumulator.add(threadID, cIdx, idx, fx, fy, fz);
                }
                if (rVirialAccumulator != null) {
                    rVirialAccumulator.add(threadID, cIdx, idx, fx, fy, fz, dx, dy, dz);
                }
                if (rEnergyAccumulator != null) {
                    double deng = r6inv*(mLJ3[cType][type]*r6inv - mLJ4[cType][type]);
                    if (mShift) deng -= mOffset[cType][type];
                    rEnergyAccumulator.add(threadID, cIdx, idx, deng);
                }
            });
        });
    }
}
