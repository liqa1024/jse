package jse.atom.pot;

import jse.atom.IPairPotential;
import jse.math.MathEX;
import org.jetbrains.annotations.Nullable;

/**
 * soft 势的 jse 实现，采用公式：{@code E = A[1 + cos(πr/rc)]}；
 * 主要用于将过近的原子推开
 * @author liqa
 */
public class Soft implements IPairPotential {
    private final double mCutMax;
    private final double[][] mPrefactor, mCut, mCutsq;
    private final int mTypeNum;
    private final String @Nullable[] mSymbols;
    
    /**
     * 创建一个 soft 势函数，{@code E = A[1 + cos(πr/rc)]}，
     * 不考虑原子种类都使用相同的参数
     * @param aPrefactor 公式中 {@code A} 值
     * @param aRCut 需要的截断半径值
     */
    public Soft(double aPrefactor, double aRCut) {
        mTypeNum = -1;
        mSymbols = null;
        mPrefactor = new double[][]{{aPrefactor}};
        mCut = new double[][]{{aRCut}};
        mCutsq = new double[][]{{aRCut*aRCut}};
        mCutMax = aRCut;
    }
    /**
     * 创建一个 soft 势函数，{@code E = A[1 + cos(πr/rc)]}，
     * 不同原子种类使用不同的参数
     * @param aPrefactor 公式中的 {@code A} 值，{@code aPrefactor[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aRCut 需要的截断半径值，{@code aRCut[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aSymbols 可选的元素符号信息，如果输入则会根据此元素符号自动映射输入的原子数据，默认为 {@code null}
     */
    public Soft(double[][] aPrefactor, double[][] aRCut, String @Nullable[] aSymbols) {
        mTypeNum = aRCut.length;
        if (aPrefactor.length != mTypeNum) throw new IllegalArgumentException("Input A size MUST be the same size of RCut");
        if (aSymbols!=null && aSymbols.length!=mTypeNum) throw new IllegalArgumentException("Input Symbols size MUST be the same size of RCut");
        mSymbols = aSymbols;
        mPrefactor = new double[mTypeNum+1][mTypeNum+1];
        mCut = new double[mTypeNum+1][mTypeNum+1];
        mCutsq = new double[mTypeNum+1][mTypeNum+1];
        double tCutMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
            double tA = aPrefactor[i][j];
            mPrefactor[i+1][j+1] = tA;
            double tRCut = aRCut[i][j];
            mCut[i+1][j+1] = tRCut;
            mCutsq[i+1][j+1] = tRCut*tRCut;
            if (tRCut > tCutMax) tCutMax = tRCut;
        }
        mCutMax = tCutMax;
        for (int j = 2; j <= mTypeNum; ++j) for (int i = 1; i < j; ++i) {
            mPrefactor[i][j] = mPrefactor[j][i];
            mCut[i][j] = mCut[j][i];
            mCutsq[i][j] = mCutsq[j][i];
        }
    }
    /**
     * 创建一个 soft 势函数，{@code E = A[1 + cos(πr/rc)]}，
     * 不同原子种类使用不同的参数
     * @param aPrefactor 公式中的 {@code A} 值，{@code aPrefactor[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aRCut 需要的截断半径值，{@code aRCut[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     */
    public Soft(double[][] aPrefactor, double[][] aRCut) {
        this(aPrefactor, aRCut, null);
    }
    
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
    public Soft setThreadNum(int aThreadNum) {mThreadNum = aThreadNum; return this;}
    
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
                double deng = mPrefactor[cType][type] * (1.0 + MathEX.Fast.cos(MathEX.PI * MathEX.Fast.sqrt(rsq) / mCut[cType][type]));
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
                double r = MathEX.Fast.sqrt(rsq);
                double arg = MathEX.PI * r / mCut[cType][type];
                double fpair = r<=0.0 ? 0.0 : (mPrefactor[cType][type] * MathEX.Fast.sin(arg) * MathEX.PI/mCut[cType][type]/r);
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
                    double deng = mPrefactor[cType][type] * (1.0 + MathEX.Fast.cos(arg));
                    rEnergyAccumulator.add(threadID, cIdx, idx, deng);
                }
            });
        });
    }
}
