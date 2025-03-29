package jse.atom.pot;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IPairPotential;
import jse.cache.VectorCache;
import jse.code.collection.ISlice;
import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntUnaryOperator;

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
     * 不考虑原子种类都使用相同的参数
     * @param aEpsilon 公式中的 {@code ε} 值，{@code aEpsilon[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aSigma 公式中的 {@code σ} 值，{@code aSigma[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aRCut 需要的截断半径值，{@code aRCut[i][j]} 记录元素种类
     * {@code i+1} 和 {@code j+1} 之间的值，只会读取 {@code j <= i} 的部分（下三角）
     * @param aSymbols 可选的元素符号信息，如果输入则会根据此元素符号自动映射输入的原子数据，默认为 {@code null}
     */
    public LJ(double[][] aEpsilon, double[][] aSigma, double[][] aRCut, String @Nullable[] aSymbols) {
        mTypeNum = aEpsilon.length;
        if (aSigma.length != mTypeNum) throw new IllegalArgumentException("Input Sigma size MUST be the same size of Epsilon");
        if (aRCut.length != mTypeNum) throw new IllegalArgumentException("Input RCut size MUST be the same size of Epsilon");
        if (aSymbols!=null && aSymbols.length!=mTypeNum) throw new IllegalArgumentException("Input Symbols size MUST be the same size of Epsilon");
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
     * 不考虑原子种类都使用相同的参数
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
    @Override public double rcut() {return mCutMax;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public boolean newton() {return true;}
    
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
     * @param aAPC {@inheritDoc}
     * @param aIndices {@inheritDoc}
     * @param aTypeMap {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public double calEnergyAt(final AtomicParameterCalculator aAPC, final ISlice aIndices, final IntUnaryOperator aTypeMap) {
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        final int tTypeNum = atomTypeNumber();
        Vector rEngPar = VectorCache.getZeros(aAPC.threadNumber());
        aAPC.pool_().parfor(aIndices.size(), (i, threadID) -> {
            final int cIdx = aIndices.get(i);
            final int cType = tTypeNum<=0 ? 0 : aTypeMap.applyAsInt(aAPC.atomType_().get(cIdx));
            // 计算部分原子能量不使用半数遍历优化
            aAPC.nl_().forEachNeighbor(cIdx, mCutMax, false, (x, y, z, idx, dx, dy, dz) -> {
                int tType = tTypeNum<=0 ? 0 : aTypeMap.applyAsInt(aAPC.atomType_().get(idx));
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq[cType][tType]) return;
                double r2inv = 1.0 / rsq;
                double r6inv = r2inv*r2inv*r2inv;
                double deng = r6inv*(mLJ3[cType][tType]*r6inv - mLJ4[cType][tType]);
                if (mShift) deng -= mOffset[cType][tType];
                rEngPar.add(threadID, deng*0.5);
            });
        });
        double rEng = rEngPar.sum();
        VectorCache.returnVec(rEngPar);
        return rEng;
    }
    /**
     * {@inheritDoc}
     * @param aAPC {@inheritDoc}
     * @param rEnergies {@inheritDoc}
     * @param rForcesX {@inheritDoc}
     * @param rForcesY {@inheritDoc}
     * @param rForcesZ {@inheritDoc}
     * @param rVirialsXX {@inheritDoc}
     * @param rVirialsYY {@inheritDoc}
     * @param rVirialsZZ {@inheritDoc}
     * @param rVirialsXY {@inheritDoc}
     * @param rVirialsXZ {@inheritDoc}
     * @param rVirialsYZ {@inheritDoc}
     * @param aTypeMap {@inheritDoc}
     */
    @Override public void calEnergyForceVirials(AtomicParameterCalculator aAPC, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ, IntUnaryOperator aTypeMap) {
        typeMapCheck(aAPC.atomTypeNumber(), aTypeMap);
        // 统一存储常量
        final int tTypeNum = atomTypeNumber();
        final int tAtomNumber = aAPC.atomNumber();
        final int tThreadNumber = aAPC.threadNumber();
        // 清空可能存在的旧值
        if (rEnergies != null) rEnergies.fill(0.0);
        // 并行情况下存在并行写入的问题，因此需要这样操作
        IVector @Nullable[] rEnergiesPar = rEnergies!=null ? new IVector[tThreadNumber] : null;
        if (rEnergies != null) {
            rEnergiesPar[0] = rEnergies;
            for (int i = 1; i < tThreadNumber; ++i) {
                rEnergiesPar[i] = VectorCache.getZeros(rEnergies.size());
            }
        }
        /// 特殊处理只需要计算能量的情况
        if (rForcesX==null && rForcesY==null && rForcesZ==null &&
            rVirialsXX==null && rVirialsYY==null && rVirialsZZ==null &&
            rVirialsXY==null && rVirialsXZ==null && rVirialsYZ==null) {
            if (rEnergies == null) return;
            aAPC.pool_().parfor(tAtomNumber, (i, threadID) -> {
                final IVector tEnergies = rEnergiesPar[threadID];
                final int cType = tTypeNum<=0 ? 0 : aTypeMap.applyAsInt(aAPC.atomType_().get(i));
                // 全遍历开启半数优化
                aAPC.nl_().forEachNeighbor(i, mCutMax, true, (x, y, z, idx, dx, dy, dz) -> {
                    int tType = tTypeNum<=0 ? 0 : aTypeMap.applyAsInt(aAPC.atomType_().get(idx));
                    double rsq = dx*dx + dy*dy + dz*dz;
                    if (rsq >= mCutsq[cType][tType]) return;
                    double r2inv = 1.0 / rsq;
                    double r6inv = r2inv*r2inv*r2inv;
                    double deng = r6inv*(mLJ3[cType][tType]*r6inv - mLJ4[cType][tType]);
                    if (mShift) deng -= mOffset[cType][tType];
                    if (tEnergies.size()==1) {tEnergies.add(0, deng);}
                    else {tEnergies.add(i, deng*0.5); tEnergies.add(idx, deng*0.5);}
                });
            });
            for (int i = 1; i < tThreadNumber; ++i) {
                rEnergies.plus2this(rEnergiesPar[i]);
                VectorCache.returnVec(rEnergiesPar[i]);
            }
            return;
        }
        /// 其余需要计算力或位力的情况
        // 清空可能存在的旧值
        if (rForcesX != null) rForcesX.fill(0.0);
        if (rForcesY != null) rForcesY.fill(0.0);
        if (rForcesZ != null) rForcesZ.fill(0.0);
        if (rVirialsXX != null) rVirialsXX.fill(0.0);
        if (rVirialsYY != null) rVirialsYY.fill(0.0);
        if (rVirialsZZ != null) rVirialsZZ.fill(0.0);
        if (rVirialsXY != null) rVirialsXY.fill(0.0);
        if (rVirialsXZ != null) rVirialsXZ.fill(0.0);
        if (rVirialsYZ != null) rVirialsYZ.fill(0.0);
        // 并行情况下存在并行写入的问题，因此需要这样操作
        IVector @Nullable[] rForcesXPar = rForcesX!=null ? new IVector[tThreadNumber] : null; if (rForcesX != null) {rForcesXPar[0] = rForcesX; for (int i = 1; i < tThreadNumber; ++i) {rForcesXPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rForcesYPar = rForcesY!=null ? new IVector[tThreadNumber] : null; if (rForcesY != null) {rForcesYPar[0] = rForcesY; for (int i = 1; i < tThreadNumber; ++i) {rForcesYPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rForcesZPar = rForcesZ!=null ? new IVector[tThreadNumber] : null; if (rForcesZ != null) {rForcesZPar[0] = rForcesZ; for (int i = 1; i < tThreadNumber; ++i) {rForcesZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsXXPar = rVirialsXX!=null ? new IVector[tThreadNumber] : null; if (rVirialsXX != null) {rVirialsXXPar[0] = rVirialsXX; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXXPar[i] = VectorCache.getZeros(rVirialsXX.size());}}
        IVector @Nullable[] rVirialsYYPar = rVirialsYY!=null ? new IVector[tThreadNumber] : null; if (rVirialsYY != null) {rVirialsYYPar[0] = rVirialsYY; for (int i = 1; i < tThreadNumber; ++i) {rVirialsYYPar[i] = VectorCache.getZeros(rVirialsYY.size());}}
        IVector @Nullable[] rVirialsZZPar = rVirialsZZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsZZ != null) {rVirialsZZPar[0] = rVirialsZZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsZZPar[i] = VectorCache.getZeros(rVirialsZZ.size());}}
        IVector @Nullable[] rVirialsXYPar = rVirialsXY!=null ? new IVector[tThreadNumber] : null; if (rVirialsXY != null) {rVirialsXYPar[0] = rVirialsXY; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXYPar[i] = VectorCache.getZeros(rVirialsXY.size());}}
        IVector @Nullable[] rVirialsXZPar = rVirialsXZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsXZ != null) {rVirialsXZPar[0] = rVirialsXZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXZPar[i] = VectorCache.getZeros(rVirialsXZ.size());}}
        IVector @Nullable[] rVirialsYZPar = rVirialsYZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsYZ != null) {rVirialsYZPar[0] = rVirialsYZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsYZPar[i] = VectorCache.getZeros(rVirialsYZ.size());}}
        // 遍历所有原子计算力
        aAPC.pool_().parfor(tAtomNumber, (i, threadID) -> {
            final @Nullable IVector tEnergies = rEnergies!=null ? rEnergiesPar[threadID] : null;
            final @Nullable IVector tForcesX = rForcesX!=null ? rForcesXPar[threadID] : null;
            final @Nullable IVector tForcesY = rForcesY!=null ? rForcesYPar[threadID] : null;
            final @Nullable IVector tForcesZ = rForcesZ!=null ? rForcesZPar[threadID] : null;
            final @Nullable IVector tVirialsXX = rVirialsXX!=null ? rVirialsXXPar[threadID] : null;
            final @Nullable IVector tVirialsYY = rVirialsYY!=null ? rVirialsYYPar[threadID] : null;
            final @Nullable IVector tVirialsZZ = rVirialsZZ!=null ? rVirialsZZPar[threadID] : null;
            final @Nullable IVector tVirialsXY = rVirialsXY!=null ? rVirialsXYPar[threadID] : null;
            final @Nullable IVector tVirialsXZ = rVirialsXZ!=null ? rVirialsXZPar[threadID] : null;
            final @Nullable IVector tVirialsYZ = rVirialsYZ!=null ? rVirialsYZPar[threadID] : null;
            final int cType = tTypeNum<=0 ? 0 : aTypeMap.applyAsInt(aAPC.atomType_().get(i));
            // 全遍历开启半数优化
            aAPC.nl_().forEachNeighbor(i, mCutMax, true, (x, y, z, idx, dx, dy, dz) -> {
                int tType = tTypeNum<=0 ? 0 : aTypeMap.applyAsInt(aAPC.atomType_().get(idx));
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq[cType][tType]) return;
                double r2inv = 1.0 / rsq;
                double r6inv = r2inv*r2inv*r2inv;
                double fpair = r2inv*r6inv*(mLJ1[cType][tType]*r6inv - mLJ2[cType][tType]);
                if (tForcesX != null) {tForcesX.add(i, -dx*fpair); tForcesX.add(idx, dx*fpair);}
                if (tForcesY != null) {tForcesY.add(i, -dy*fpair); tForcesY.add(idx, dy*fpair);}
                if (tForcesZ != null) {tForcesZ.add(i, -dz*fpair); tForcesZ.add(idx, dz*fpair);}
                if (tVirialsXX != null) {double tVxx = dx*dx*fpair; if (tVirialsXX.size()==1) {tVirialsXX.add(0, tVxx);} else {tVirialsXX.add(i, 0.5*tVxx); tVirialsXX.add(idx, 0.5*tVxx);}}
                if (tVirialsYY != null) {double tVyy = dy*dy*fpair; if (tVirialsYY.size()==1) {tVirialsYY.add(0, tVyy);} else {tVirialsYY.add(i, 0.5*tVyy); tVirialsYY.add(idx, 0.5*tVyy);}}
                if (tVirialsZZ != null) {double tVzz = dz*dz*fpair; if (tVirialsZZ.size()==1) {tVirialsZZ.add(0, tVzz);} else {tVirialsZZ.add(i, 0.5*tVzz); tVirialsZZ.add(idx, 0.5*tVzz);}}
                if (tVirialsXY != null) {double tVxy = dx*dy*fpair; if (tVirialsXY.size()==1) {tVirialsXY.add(0, tVxy);} else {tVirialsXY.add(i, 0.5*tVxy); tVirialsXY.add(idx, 0.5*tVxy);}}
                if (tVirialsXZ != null) {double tVxz = dx*dz*fpair; if (tVirialsXZ.size()==1) {tVirialsXZ.add(0, tVxz);} else {tVirialsXZ.add(i, 0.5*tVxz); tVirialsXZ.add(idx, 0.5*tVxz);}}
                if (tVirialsYZ != null) {double tVyz = dy*dz*fpair; if (tVirialsYZ.size()==1) {tVirialsYZ.add(0, tVyz);} else {tVirialsYZ.add(i, 0.5*tVyz); tVirialsYZ.add(idx, 0.5*tVyz);}}
                if (tEnergies != null) {
                    double deng = r6inv*(mLJ3[cType][tType]*r6inv - mLJ4[cType][tType]);
                    if (mShift) deng -= mOffset[cType][tType];
                    if (tEnergies.size()==1) {tEnergies.add(0, deng);}
                    else {tEnergies.add(i, deng*0.5); tEnergies.add(idx, deng*0.5);}
                }
            });
        });
        // 累加其余线程的数据然后归还临时变量
        if (rEnergies != null) {for (int i = 1; i < tThreadNumber; ++i) {rEnergies.plus2this(rEnergiesPar[i]); VectorCache.returnVec(rEnergiesPar[i]);}}
        if (rForcesZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesZ.plus2this(rForcesZPar[i]); VectorCache.returnVec(rForcesZPar[i]);}}
        if (rForcesY != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesY.plus2this(rForcesYPar[i]); VectorCache.returnVec(rForcesYPar[i]);}}
        if (rForcesX != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesX.plus2this(rForcesXPar[i]); VectorCache.returnVec(rForcesXPar[i]);}}
        if (rVirialsYZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsYZ.plus2this(rVirialsYZPar[i]); VectorCache.returnVec(rVirialsYZPar[i]);}}
        if (rVirialsXZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXZ.plus2this(rVirialsXZPar[i]); VectorCache.returnVec(rVirialsXZPar[i]);}}
        if (rVirialsXY != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXY.plus2this(rVirialsXYPar[i]); VectorCache.returnVec(rVirialsXYPar[i]);}}
        if (rVirialsZZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsZZ.plus2this(rVirialsZZPar[i]); VectorCache.returnVec(rVirialsZZPar[i]);}}
        if (rVirialsYY != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsYY.plus2this(rVirialsYYPar[i]); VectorCache.returnVec(rVirialsYYPar[i]);}}
        if (rVirialsXX != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXX.plus2this(rVirialsXXPar[i]); VectorCache.returnVec(rVirialsXXPar[i]);}}
    }
}
