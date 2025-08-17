package jse.atom.pot;

import jse.atom.IPairPotential;
import jse.cache.VectorCache;
import jse.code.IO;
import jse.math.MathEX;
import jse.math.function.ConstBoundFunc1;
import jse.math.function.IFunc1;
import jse.math.function.ZeroBoundFunc1;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import static jse.code.CS.ATOMIC_NUMBER_TO_SYMBOL;
import static jse.code.CS.UNITS;

/**
 * EAM 势（Embedded-Atom Method）的 jse 实现，具体形式为:
 * {@code E = F(Σρ(r)) + 0.5 ΣΦ(r)}；这里通过读取 DYNAMO
 * 格式的文件获取函数 {@code F, ρ, Φ} 的数值形式。
 * <p>
 * 具体类似
 * <a href="https://wiki.fysik.dtu.dk/ase/ase/calculators/eam.html">ase 的 EAM 计算器</a>，
 * 同样会自动识别势函数文件类型来选择 {@code eam}, {@code eam/alloy}, {@code eam/fs} 或
 * {@code adp}；但修复了 ase 实现中的许多问题，现在应该兼容更多 lammps 能读取的势函数文件。
 * <p>
 * 这里不去专门优化单粒子移动、翻转、种类交换时的能量差的计算，因为这需要缓存电荷密度值
 * <p>
 * 这里采用简单的线性插值的方法获取数值函数值，而不是样条，因此某些情况下可能会有精度问题。
 *
 * @author liqa
 */
public class EAM implements IPairPotential {
    public final static class Conf {
        /** 是否使用 lammps 中采用的低精度 hartree 和 bohr，从而让结果和 lammps 一致 */
        public static boolean USE_LAMMPS_PRECISION = true;
    }
    private final static double EAM_MUL;
    static {
        if (Conf.USE_LAMMPS_PRECISION) {
            EAM_MUL = 27.2*0.529;
        } else {
            EAM_MUL = UNITS.get("Hartree")*UNITS.get("Bohr");
        }
    }
    
    private final double mCut, mCutsq;
    private final String mHeader;
    private final IFunc1[] mFRho, mFRhoGrad;
    private final IFunc1[][] mRhoR, mPhiR, mRhoRGrad, mPhiRGrad;
    private final IFunc1 @Nullable[][] mUR, mWR, mURGrad, mWRGrad;
    private final int mTypeNum;
    private final String[] mSymbols, mLatticeTypes;
    private final int[] mAtomicNumbers;
    private final double[] mMasses, mLatticeConsts;
    
    /**
     * 通过势函数文件创建一个 EAM 势函数
     * @param aFilePath EAM 势函数路径，要求 DYNAMO 格式的 lammps 支持的文件
     * @param aFormat 可选的 EAM 势函数文件格式，可选 {@code "eam", "alloy", "fs", "adp"}，默认根据后缀名自动检测
     */
    public EAM(String aFilePath, @Nullable String aFormat) throws IOException {
        if (aFormat == null) {
            if (aFilePath.endsWith(".eam")) {
                aFormat = "eam";
            } else
            if (aFilePath.endsWith(".alloy")) {
                aFormat = "alloy";
            } else
            if (aFilePath.endsWith(".fs")) {
                aFormat = "fs";
            } else
            if (aFilePath.endsWith(".adp")) {
                aFormat = "adp";
            } else {
                throw new IllegalArgumentException("Unsupported EAM format: " + aFilePath);
            }
        }
        try (BufferedReader tReader = IO.toReader(aFilePath)) {
            switch(aFormat) {
            case "eam": {
                mHeader = tReader.readLine();
                String[] tTokens = IO.Text.splitBlank(tReader.readLine());
                mTypeNum = 1;
                mAtomicNumbers = new int[] {Integer.parseInt(tTokens[0])};
                mSymbols = new String[] {ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers[0])};
                mMasses = new double[] {Double.parseDouble(tTokens[1])};
                mLatticeConsts = new double[] {Double.parseDouble(tTokens[2])};
                mLatticeTypes = new String[] {tTokens[3]};
                tTokens = IO.Text.splitBlank(tReader.readLine());
                int tNRho = Integer.parseInt(tTokens[0]);
                double tDRho = Double.parseDouble(tTokens[1]);
                int tNR = Integer.parseInt(tTokens[2]);
                double tDR = Double.parseDouble(tTokens[3]);
                double tRCut = Double.parseDouble(tTokens[4]);
                mCutsq = tRCut*tRCut;
                mCut = tRCut;
                mFRho = new IFunc1[] {ConstBoundFunc1.zeros(0, tDRho, tNRho)};
                mRhoR = new IFunc1[][] {{ConstBoundFunc1.zeros(0, tDR, tNR)}};
                mPhiR = new IFunc1[][] {{ConstBoundFunc1.zeros(0, tDR, tNR)}};
                IVector tData = readData_(tReader, tNRho+tNR+tNR);
                mFRho[0].fill(tData.subVec(0, tNRho));
                mPhiR[0][0].fill(tData.subVec(tNRho, tNRho+tNR));
                mRhoR[0][0].fill(tData.subVec(tNRho+tNR, tNRho+tNR+tNR));
                // Z(r) -> phi(r)
                mPhiR[0][0].operation().mapFull2this((z, r) -> EAM_MUL * z*z / r);
                mFRhoGrad = new IFunc1[] {ZeroBoundFunc1.zeros(tDRho*0.5, tDRho, tNRho-1)};
                mRhoRGrad = new IFunc1[][] {{ZeroBoundFunc1.zeros(tDR*0.5, tDR, tNR-1)}};
                mPhiRGrad = new IFunc1[][] {{ZeroBoundFunc1.zeros(tDR*0.5, tDR, tNR-1)}};
                for (int i = 0; i < tNRho-1; ++i) {
                    mFRhoGrad[0].set(i, (mFRho[0].get(i+1) - mFRho[0].get(i)) / tDRho);
                }
                for (int i = 0; i < tNR-1; ++i) {
                    mRhoRGrad[0][0].set(i, (mRhoR[0][0].get(i+1) - mRhoR[0][0].get(i)) / tDR);
                    mPhiRGrad[0][0].set(i, (mPhiR[0][0].get(i+1) - mPhiR[0][0].get(i)) / tDR);
                }
                mUR = null; mWR = null; mURGrad = null; mWRGrad = null;
                break;
            }
            case "alloy": case "fs": case "adp": {
                boolean tIsFs = aFormat.equals("fs");
                boolean tIsAdp = aFormat.equals("adp");
                mHeader = tReader.readLine() + "\n" +
                          tReader.readLine() + "\n" +
                          tReader.readLine();
                String[] tTokens = IO.Text.splitBlank(tReader.readLine());
                mTypeNum = Integer.parseInt(tTokens[0]);
                mSymbols = new String[mTypeNum];
                System.arraycopy(tTokens, 1, mSymbols, 0, mTypeNum);
                mAtomicNumbers = new int[mTypeNum];
                mMasses = new double[mTypeNum];
                mLatticeConsts = new double[mTypeNum];
                mLatticeTypes = new String[mTypeNum];
                tTokens = IO.Text.splitBlank(tReader.readLine());
                int tNRho = Integer.parseInt(tTokens[0]);
                double tDRho = Double.parseDouble(tTokens[1]);
                int tNR = Integer.parseInt(tTokens[2]);
                double tDR = Double.parseDouble(tTokens[3]);
                double tRCut = Double.parseDouble(tTokens[4]);
                mCutsq = tRCut*tRCut;
                mCut = tRCut;
                mFRho = new IFunc1[mTypeNum];
                mRhoR = new IFunc1[tIsFs?mTypeNum:1][mTypeNum];
                mPhiR = new IFunc1[mTypeNum][mTypeNum];
                mFRhoGrad = new IFunc1[mTypeNum];
                mRhoRGrad = new IFunc1[tIsFs?mTypeNum:1][mTypeNum];
                mPhiRGrad = new IFunc1[mTypeNum][mTypeNum];
                for (int i = 0; i < mTypeNum; ++i) {
                    tTokens = IO.Text.splitBlank(tReader.readLine());
                    mAtomicNumbers[i] = Integer.parseInt(tTokens[0]);
                    mMasses[i] = Double.parseDouble(tTokens[1]);
                    mLatticeConsts[i] = Double.parseDouble(tTokens[2]);
                    mLatticeTypes[i] = tTokens[3];
                    IVector tData = readData_(tReader, tNRho + (tIsFs?(mTypeNum*tNR):tNR));
                    mFRho[i] = ConstBoundFunc1.zeros(0, tDRho, tNRho);
                    mFRho[i].fill(tData.subVec(0, tNRho));
                    mFRhoGrad[i] = ZeroBoundFunc1.zeros(tDRho*0.5, tDRho, tNRho-1);
                    for (int k = 0; k < tNRho-1; ++k) {
                        mFRhoGrad[i].set(k, (mFRho[i].get(k+1) - mFRho[i].get(k)) / tDRho);
                    }
                    if (tIsFs) {
                        int tShift = tNRho;
                        for (int j = 0; j < mTypeNum; ++j) {
                            mRhoR[j][i] = ConstBoundFunc1.zeros(0, tDR, tNR);
                            mRhoR[j][i].fill(tData.subVec(tShift, tShift+tNR));
                            mRhoRGrad[j][i] = ZeroBoundFunc1.zeros(tDR*0.5, tDR, tNR-1);
                            for (int k = 0; k < tNRho-1; ++k) {
                                mRhoRGrad[j][i].set(k, (mRhoR[j][i].get(k+1) - mRhoR[j][i].get(k)) / tDR);
                            }
                            tShift += tNR;
                        }
                    } else {
                        mRhoR[0][i] = ConstBoundFunc1.zeros(0, tDR, tNR);
                        mRhoR[0][i].fill(tData.subVec(tNRho, tNRho+tNR));
                        mRhoRGrad[0][i] = ZeroBoundFunc1.zeros(tDR*0.5, tDR, tNR-1);
                        for (int k = 0; k < tNR-1; ++k) {
                            mRhoRGrad[0][i].set(k, (mRhoR[0][i].get(k+1) - mRhoR[0][i].get(k)) / tDR);
                        }
                    }
                }
                IVector tData = readData_(tReader, (1+mTypeNum)*mTypeNum/2 * tNR);
                int tShift = 0;
                for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                    mPhiR[i][j] = ConstBoundFunc1.zeros(0, tDR, tNR);
                    mPhiR[i][j].fill(tData.subVec(tShift, tShift+tNR));
                    // r * phi(r) -> phi(r)
                    mPhiR[i][j].f().div2this(mPhiR[i][j].x());
                    mPhiRGrad[i][j] = ZeroBoundFunc1.zeros(tDR*0.5, tDR, tNR-1);
                    for (int k = 0; k < tNR-1; ++k) {
                        mPhiRGrad[i][j].set(k, (mPhiR[i][j].get(k+1) - mPhiR[i][j].get(k)) / tDR);
                    }
                    if (j != i) {
                        mPhiR[j][i] = mPhiR[i][j];
                        mPhiRGrad[j][i] = mPhiRGrad[i][j];
                    }
                    tShift += tNR;
                }
                if (!tIsAdp) {
                    mUR = null; mWR = null; mURGrad = null; mWRGrad = null;
                    break;
                }
                mUR = new IFunc1[mTypeNum][mTypeNum];
                mWR = new IFunc1[mTypeNum][mTypeNum];
                mURGrad = new IFunc1[mTypeNum][mTypeNum];
                mWRGrad = new IFunc1[mTypeNum][mTypeNum];
                tData = readData_(tReader, (1+mTypeNum)*mTypeNum/2 * tNR);
                tShift = 0;
                for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                    mUR[i][j] = ConstBoundFunc1.zeros(0, tDR, tNR);
                    mUR[i][j].fill(tData.subVec(tShift, tShift+tNR));
                    mURGrad[i][j] = ZeroBoundFunc1.zeros(tDR*0.5, tDR, tNR-1);
                    for (int k = 0; k < tNR-1; ++k) {
                        mURGrad[i][j].set(k, (mUR[i][j].get(k+1) - mUR[i][j].get(k)) / tDR);
                    }
                    if (j != i) {
                        mUR[j][i] = mUR[i][j];
                        mURGrad[j][i] = mURGrad[i][j];
                    }
                    tShift += tNR;
                }
                tData = readData_(tReader, (1+mTypeNum)*mTypeNum/2 * tNR);
                tShift = 0;
                for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                    mWR[i][j] = ConstBoundFunc1.zeros(0, tDR, tNR);
                    mWR[i][j].fill(tData.subVec(tShift, tShift+tNR));
                    mWRGrad[i][j] = ZeroBoundFunc1.zeros(tDR*0.5, tDR, tNR-1);
                    for (int k = 0; k < tNR-1; ++k) {
                        mWRGrad[i][j].set(k, (mWR[i][j].get(k+1) - mWR[i][j].get(k)) / tDR);
                    }
                    if (j != i) {
                        mWR[j][i] = mWR[i][j];
                        mWRGrad[j][i] = mWRGrad[i][j];
                    }
                    tShift += tNR;
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid EAM format: " + aFormat);
            }}
            
        }
    }
    /**
     * 通过势函数文件创建一个 EAM 势函数
     * @param aFilePath EAM 势函数路径，要求 DYNAMO 格式的 lammps 支持的文件
     */
    public EAM(String aFilePath) throws IOException {
        this(aFilePath, null);
    }
    
    private static IVector readData_(BufferedReader aReader, int aSize) throws IOException {
        Vector rOut = Vectors.NaN(aSize);
        for (int k = 0; k < aSize;) {
            String[] tTokens = IO.Text.splitBlank(aReader.readLine());
            for (String tToken : tTokens) {
                rOut.set(k, Double.parseDouble(tToken));
                ++k;
            }
        }
        return rOut;
    }
    
    
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return mTypeNum;}
    /** @return {@inheritDoc} */
    @Override public boolean hasSymbol() {return true;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public @NotNull String symbol(int aType) {return mSymbols[aType-1];}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public double rcutMax() {return mCut;}
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
    public EAM setThreadNum(int aThreadNum) {mThreadNum = aThreadNum; return this;}
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public void calEnergyPart(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyPartAccumulator rEnergyAccumulator) {
        int tThreadNum = threadNumber();
        final List<Vector> tRhoPar = VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuXPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuYPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuZPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXXPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaYYPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaZZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXYPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaYZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final Vector tRho = tRhoPar.get(threadID);
            final Vector tMuX = mUR==null ? null : tMuXPar.get(threadID);
            final Vector tMuY = mUR==null ? null : tMuYPar.get(threadID);
            final Vector tMuZ = mUR==null ? null : tMuZPar.get(threadID);
            final Vector tLambdaXX = mWR==null ? null : tLambdaXXPar.get(threadID);
            final Vector tLambdaYY = mWR==null ? null : tLambdaYYPar.get(threadID);
            final Vector tLambdaZZ = mWR==null ? null : tLambdaZZPar.get(threadID);
            final Vector tLambdaXY = mWR==null ? null : tLambdaXYPar.get(threadID);
            final Vector tLambdaXZ = mWR==null ? null : tLambdaXZPar.get(threadID);
            final Vector tLambdaYZ = mWR==null ? null : tLambdaYZPar.get(threadID);
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                double deng = 0.5 * mPhiR[cType-1][type-1].subs(r);
                rEnergyAccumulator.add(threadID, cIdx, deng);
                tRho.add(cIdx, mRhoR[mRhoR.length==1?0:(cType-1)][type-1].subs(r));
                if (mUR != null) {
                    double u = mUR[cType-1][type-1].subs(r);
                    tMuX.add(cIdx, u*dx);
                    tMuY.add(cIdx, u*dy);
                    tMuZ.add(cIdx, u*dz);
                }
                if (mWR != null) {
                    double w = mWR[cType-1][type-1].subs(r);
                    tLambdaXX.add(cIdx, w*dx*dx);
                    tLambdaYY.add(cIdx, w*dy*dy);
                    tLambdaZZ.add(cIdx, w*dz*dz);
                    tLambdaXY.add(cIdx, w*dx*dy);
                    tLambdaYZ.add(cIdx, w*dy*dz);
                    tLambdaXZ.add(cIdx, w*dx*dz);
                }
            });
        });
        Vector tRho = tRhoPar.get(0);
        for (int i = 1; i < tThreadNum; ++i) tRho.plus2this(tRhoPar.get(i));
        Vector tMuX = mUR==null ? null : tMuXPar.get(0);
        Vector tMuY = mUR==null ? null : tMuYPar.get(0);
        Vector tMuZ = mUR==null ? null : tMuZPar.get(0);
        if (mUR != null) for (int i = 1; i < tThreadNum; ++i) {
            tMuX.plus2this(tMuXPar.get(i));
            tMuY.plus2this(tMuYPar.get(i));
            tMuZ.plus2this(tMuZPar.get(i));
        }
        Vector tLambdaXX = mWR==null ? null : tLambdaXXPar.get(0);
        Vector tLambdaYY = mWR==null ? null : tLambdaYYPar.get(0);
        Vector tLambdaZZ = mWR==null ? null : tLambdaZZPar.get(0);
        Vector tLambdaXY = mWR==null ? null : tLambdaXYPar.get(0);
        Vector tLambdaYZ = mWR==null ? null : tLambdaYZPar.get(0);
        Vector tLambdaXZ = mWR==null ? null : tLambdaXZPar.get(0);
        if (mWR != null) for (int i = 1; i < tThreadNum; ++i) {
            tLambdaXX.plus2this(tLambdaXXPar.get(i));
            tLambdaYY.plus2this(tLambdaYYPar.get(i));
            tLambdaZZ.plus2this(tLambdaZZPar.get(i));
            tLambdaXY.plus2this(tLambdaXYPar.get(i));
            tLambdaYZ.plus2this(tLambdaYZPar.get(i));
            tLambdaXZ.plus2this(tLambdaXZPar.get(i));
        }
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            double deng = mFRho[cType-1].subs(tRho.get(cIdx));
            if (mUR != null) {
                double mx = tMuX.get(cIdx);
                double my = tMuY.get(cIdx);
                double mz = tMuZ.get(cIdx);
                deng += 0.5 * (mx*mx + my*my + mz*mz);
            }
            if (mWR != null) {
                double lxx = tLambdaXX.get(cIdx);
                double lyy = tLambdaYY.get(cIdx);
                double lzz = tLambdaZZ.get(cIdx);
                double lxy = tLambdaXY.get(cIdx);
                double lxz = tLambdaXZ.get(cIdx);
                double lyz = tLambdaYZ.get(cIdx);
                deng += 0.5 * (lxx*lxx + lyy*lyy + lzz*lzz);
                deng += (lxy*lxy + lxz*lxz + lyz*lyz);
                double nu = lxx + lyy + lzz;
                deng -= (1.0/6.0) * nu*nu;
            }
            rEnergyAccumulator.add(threadID, cIdx, deng);
        });
        if (mWR != null) {
            VectorCache.returnVec(tLambdaXXPar);
            VectorCache.returnVec(tLambdaYYPar);
            VectorCache.returnVec(tLambdaZZPar);
            VectorCache.returnVec(tLambdaXYPar);
            VectorCache.returnVec(tLambdaXZPar);
            VectorCache.returnVec(tLambdaYZPar);
        }
        if (mUR != null) {
            VectorCache.returnVec(tMuXPar);
            VectorCache.returnVec(tMuYPar);
            VectorCache.returnVec(tMuZPar);
        }
        VectorCache.returnVec(tRhoPar);
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) {
        int tThreadNum = threadNumber();
        final List<Vector> tRhoPar = VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuXPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuYPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuZPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXXPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaYYPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaZZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXYPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaYZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final Vector tRho = tRhoPar.get(threadID);
            final Vector tMuX = mUR==null ? null : tMuXPar.get(threadID);
            final Vector tMuY = mUR==null ? null : tMuYPar.get(threadID);
            final Vector tMuZ = mUR==null ? null : tMuZPar.get(threadID);
            final Vector tLambdaXX = mWR==null ? null : tLambdaXXPar.get(threadID);
            final Vector tLambdaYY = mWR==null ? null : tLambdaYYPar.get(threadID);
            final Vector tLambdaZZ = mWR==null ? null : tLambdaZZPar.get(threadID);
            final Vector tLambdaXY = mWR==null ? null : tLambdaXYPar.get(threadID);
            final Vector tLambdaXZ = mWR==null ? null : tLambdaXZPar.get(threadID);
            final Vector tLambdaYZ = mWR==null ? null : tLambdaYZPar.get(threadID);
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                double deng = mPhiR[cType-1][type-1].subs(r);
                rEnergyAccumulator.add(threadID, cIdx, idx, deng);
                tRho.add(cIdx, mRhoR[mRhoR.length==1?0:(cType-1)][type-1].subs(r));
                tRho.add(idx, mRhoR[mRhoR.length==1?0:(type-1)][cType-1].subs(r));
                if (mUR != null) {
                    double u = mUR[cType-1][type-1].subs(r);
                    tMuX.add(cIdx, u*dx);
                    tMuY.add(cIdx, u*dy);
                    tMuZ.add(cIdx, u*dz);
                    u = mUR[type-1][cType-1].subs(r);
                    tMuX.add(idx, -u*dx);
                    tMuY.add(idx, -u*dy);
                    tMuZ.add(idx, -u*dz);
                }
                if (mWR != null) {
                    double w = mWR[cType-1][type-1].subs(r);
                    tLambdaXX.add(cIdx, w*dx*dx);
                    tLambdaYY.add(cIdx, w*dy*dy);
                    tLambdaZZ.add(cIdx, w*dz*dz);
                    tLambdaXY.add(cIdx, w*dx*dy);
                    tLambdaYZ.add(cIdx, w*dy*dz);
                    tLambdaXZ.add(cIdx, w*dx*dz);
                    w = mWR[type-1][cType-1].subs(r);
                    tLambdaXX.add(idx, w*dx*dx);
                    tLambdaYY.add(idx, w*dy*dy);
                    tLambdaZZ.add(idx, w*dz*dz);
                    tLambdaXY.add(idx, w*dx*dy);
                    tLambdaYZ.add(idx, w*dy*dz);
                    tLambdaXZ.add(idx, w*dx*dz);
                }
            });
        });
        Vector tRho = tRhoPar.get(0);
        for (int i = 1; i < tThreadNum; ++i) tRho.plus2this(tRhoPar.get(i));
        Vector tMuX = mUR==null ? null : tMuXPar.get(0);
        Vector tMuY = mUR==null ? null : tMuYPar.get(0);
        Vector tMuZ = mUR==null ? null : tMuZPar.get(0);
        if (mUR != null) for (int i = 1; i < tThreadNum; ++i) {
            tMuX.plus2this(tMuXPar.get(i));
            tMuY.plus2this(tMuYPar.get(i));
            tMuZ.plus2this(tMuZPar.get(i));
        }
        Vector tLambdaXX = mWR==null ? null : tLambdaXXPar.get(0);
        Vector tLambdaYY = mWR==null ? null : tLambdaYYPar.get(0);
        Vector tLambdaZZ = mWR==null ? null : tLambdaZZPar.get(0);
        Vector tLambdaXY = mWR==null ? null : tLambdaXYPar.get(0);
        Vector tLambdaYZ = mWR==null ? null : tLambdaYZPar.get(0);
        Vector tLambdaXZ = mWR==null ? null : tLambdaXZPar.get(0);
        if (mWR != null) for (int i = 1; i < tThreadNum; ++i) {
            tLambdaXX.plus2this(tLambdaXXPar.get(i));
            tLambdaYY.plus2this(tLambdaYYPar.get(i));
            tLambdaZZ.plus2this(tLambdaZZPar.get(i));
            tLambdaXY.plus2this(tLambdaXYPar.get(i));
            tLambdaYZ.plus2this(tLambdaYZPar.get(i));
            tLambdaXZ.plus2this(tLambdaXZPar.get(i));
        }
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            double deng = mFRho[cType-1].subs(tRho.get(cIdx));
            if (mUR != null) {
                double mx = tMuX.get(cIdx);
                double my = tMuY.get(cIdx);
                double mz = tMuZ.get(cIdx);
                deng += 0.5 * (mx*mx + my*my + mz*mz);
            }
            if (mWR != null) {
                double lxx = tLambdaXX.get(cIdx);
                double lyy = tLambdaYY.get(cIdx);
                double lzz = tLambdaZZ.get(cIdx);
                double lxy = tLambdaXY.get(cIdx);
                double lxz = tLambdaXZ.get(cIdx);
                double lyz = tLambdaYZ.get(cIdx);
                deng += 0.5 * (lxx*lxx + lyy*lyy + lzz*lzz);
                deng += (lxy*lxy + lxz*lxz + lyz*lyz);
                double nu = lxx + lyy + lzz;
                deng -= (1.0/6.0) * nu*nu;
            }
            rEnergyAccumulator.add(threadID, cIdx, -1, deng);
        });
        if (mWR != null) {
            VectorCache.returnVec(tLambdaXXPar);
            VectorCache.returnVec(tLambdaYYPar);
            VectorCache.returnVec(tLambdaZZPar);
            VectorCache.returnVec(tLambdaXYPar);
            VectorCache.returnVec(tLambdaXZPar);
            VectorCache.returnVec(tLambdaYZPar);
        }
        if (mUR != null) {
            VectorCache.returnVec(tMuXPar);
            VectorCache.returnVec(tMuYPar);
            VectorCache.returnVec(tMuZPar);
        }
        VectorCache.returnVec(tRhoPar);
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
        int tThreadNum = threadNumber();
        final List<Vector> tRhoPar = VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuXPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuYPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tMuZPar = mUR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXXPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaYYPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaZZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXYPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaXZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        final List<Vector> tLambdaYZPar = mWR==null ? null : VectorCache.getZeros(aAtomNumber, tThreadNum);
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final Vector tRho = tRhoPar.get(threadID);
            final Vector tMuX = mUR==null ? null : tMuXPar.get(threadID);
            final Vector tMuY = mUR==null ? null : tMuYPar.get(threadID);
            final Vector tMuZ = mUR==null ? null : tMuZPar.get(threadID);
            final Vector tLambdaXX = mWR==null ? null : tLambdaXXPar.get(threadID);
            final Vector tLambdaYY = mWR==null ? null : tLambdaYYPar.get(threadID);
            final Vector tLambdaZZ = mWR==null ? null : tLambdaZZPar.get(threadID);
            final Vector tLambdaXY = mWR==null ? null : tLambdaXYPar.get(threadID);
            final Vector tLambdaXZ = mWR==null ? null : tLambdaXZPar.get(threadID);
            final Vector tLambdaYZ = mWR==null ? null : tLambdaYZPar.get(threadID);
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                tRho.add(cIdx, mRhoR[mRhoR.length==1?0:(cType-1)][type-1].subs(r));
                tRho.add(idx, mRhoR[mRhoR.length==1?0:(type-1)][cType-1].subs(r));
                if (mUR != null) {
                    double u = mUR[cType-1][type-1].subs(r);
                    tMuX.add(cIdx, u*dx);
                    tMuY.add(cIdx, u*dy);
                    tMuZ.add(cIdx, u*dz);
                    tMuX.add(idx, -u*dx);
                    tMuY.add(idx, -u*dy);
                    tMuZ.add(idx, -u*dz);
                }
                if (mWR != null) {
                    double w = mWR[cType-1][type-1].subs(r);
                    tLambdaXX.add(cIdx, w*dx*dx);
                    tLambdaYY.add(cIdx, w*dy*dy);
                    tLambdaZZ.add(cIdx, w*dz*dz);
                    tLambdaXY.add(cIdx, w*dx*dy);
                    tLambdaYZ.add(cIdx, w*dy*dz);
                    tLambdaXZ.add(cIdx, w*dx*dz);
                    tLambdaXX.add(idx, w*dx*dx);
                    tLambdaYY.add(idx, w*dy*dy);
                    tLambdaZZ.add(idx, w*dz*dz);
                    tLambdaXY.add(idx, w*dx*dy);
                    tLambdaYZ.add(idx, w*dy*dz);
                    tLambdaXZ.add(idx, w*dx*dz);
                }
                if (rEnergyAccumulator != null) {
                    double deng = mPhiR[cType-1][type-1].subs(r);
                    rEnergyAccumulator.add(threadID, cIdx, idx, deng);
                }
            });
        });
        final Vector tRho = tRhoPar.get(0);
        for (int i = 1; i < tThreadNum; ++i) tRho.plus2this(tRhoPar.get(i));
        final Vector tMuX = mUR==null ? null : tMuXPar.get(0);
        final Vector tMuY = mUR==null ? null : tMuYPar.get(0);
        final Vector tMuZ = mUR==null ? null : tMuZPar.get(0);
        if (mUR != null) for (int i = 1; i < tThreadNum; ++i) {
            tMuX.plus2this(tMuXPar.get(i));
            tMuY.plus2this(tMuYPar.get(i));
            tMuZ.plus2this(tMuZPar.get(i));
        }
        final Vector tLambdaXX = mWR==null ? null : tLambdaXXPar.get(0);
        final Vector tLambdaYY = mWR==null ? null : tLambdaYYPar.get(0);
        final Vector tLambdaZZ = mWR==null ? null : tLambdaZZPar.get(0);
        final Vector tLambdaXY = mWR==null ? null : tLambdaXYPar.get(0);
        final Vector tLambdaYZ = mWR==null ? null : tLambdaYZPar.get(0);
        final Vector tLambdaXZ = mWR==null ? null : tLambdaXZPar.get(0);
        if (mWR != null) for (int i = 1; i < tThreadNum; ++i) {
            tLambdaXX.plus2this(tLambdaXXPar.get(i));
            tLambdaYY.plus2this(tLambdaYYPar.get(i));
            tLambdaZZ.plus2this(tLambdaZZPar.get(i));
            tLambdaXY.plus2this(tLambdaXYPar.get(i));
            tLambdaYZ.plus2this(tLambdaYZPar.get(i));
            tLambdaXZ.plus2this(tLambdaXZPar.get(i));
        }
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final double fpi = mFRhoGrad[cType-1].subs(tRho.get(cIdx));
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                double recip = 1.0/r;
                double phip = mPhiRGrad[cType-1][type-1].subs(r);
                double fpj = mFRhoGrad[type-1].subs(tRho.get(idx));
                double rhojp = mRhoRGrad[mRhoR.length==1?0:(cType-1)][type-1].subs(r);
                double rhoip = mRhoRGrad[mRhoR.length==1?0:(type-1)][cType-1].subs(r);
                double fpair = -(fpi*rhojp + fpj*rhoip + phip) * recip;
                double fx = dx*fpair, fy = dy*fpair, fz = dz*fpair;
                if (mUR != null) {
                    assert mURGrad != null;
                    double u = mUR[cType-1][type-1].subs(r);
                    double up = mURGrad[cType-1][type-1].subs(r);
                    double dmx = tMuX.get(cIdx) - tMuX.get(idx);
                    double dmy = tMuY.get(cIdx) - tMuY.get(idx);
                    double dmz = tMuZ.get(cIdx) - tMuZ.get(idx);
                    double dm3 = dmx*dx + dmy*dy + dmz*dz;
                    fx -= (dmx*u + dm3*up*dx*recip);
                    fy -= (dmy*u + dm3*up*dy*recip);
                    fz -= (dmz*u + dm3*up*dz*recip);
                }
                if (mWR != null) {
                    assert mWRGrad != null;
                    double w = mWR[cType-1][type-1].subs(r);
                    double wp = mWRGrad[cType-1][type-1].subs(r);
                    double slxx = tLambdaXX.get(cIdx) + tLambdaXX.get(idx);
                    double slyy = tLambdaYY.get(cIdx) + tLambdaYY.get(idx);
                    double slzz = tLambdaZZ.get(cIdx) + tLambdaZZ.get(idx);
                    double slxy = tLambdaXY.get(cIdx) + tLambdaXY.get(idx);
                    double slyz = tLambdaYZ.get(cIdx) + tLambdaYZ.get(idx);
                    double slxz = tLambdaXZ.get(cIdx) + tLambdaXZ.get(idx);
                    double sl3 = slxx*dx*dx + slyy*dy*dy + slzz*dz*dz
                        + 2.0 * (slxy*dx*dy + slyz*dy*dz + slxz*dx*dz);
                    double snu = slxx + slyy + slzz;
                    fx -= (2.0*w*(slxx*dx + slxy*dy + slxz*dz) + wp*dx*recip*sl3 - (1.0/3.0)*snu*(wp*r + 2.0*w)*dx);
                    fy -= (2.0*w*(slxy*dx + slyy*dy + slyz*dz) + wp*dy*recip*sl3 - (1.0/3.0)*snu*(wp*r + 2.0*w)*dy);
                    fz -= (2.0*w*(slxz*dx + slyz*dy + slzz*dz) + wp*dz*recip*sl3 - (1.0/3.0)*snu*(wp*r + 2.0*w)*dz);
                }
                if (rForceAccumulator != null) {
                    rForceAccumulator.add(threadID, cIdx, idx, fx, fy, fz);
                }
                if (rVirialAccumulator != null) {
                    rVirialAccumulator.add(threadID, cIdx, idx, dx*fx, dy*fy, dz*fz, dx*fy, dx*fz, dy*fz);
                }
            });
            if (rEnergyAccumulator != null) {
                double deng = mFRho[cType-1].subs(tRho.get(cIdx));
                if (mUR != null) {
                    double mx = tMuX.get(cIdx);
                    double my = tMuY.get(cIdx);
                    double mz = tMuZ.get(cIdx);
                    deng += 0.5 * (mx*mx + my*my + mz*mz);
                }
                if (mWR != null) {
                    double lxx = tLambdaXX.get(cIdx);
                    double lyy = tLambdaYY.get(cIdx);
                    double lzz = tLambdaZZ.get(cIdx);
                    double lxy = tLambdaXY.get(cIdx);
                    double lxz = tLambdaXZ.get(cIdx);
                    double lyz = tLambdaYZ.get(cIdx);
                    deng += 0.5 * (lxx*lxx + lyy*lyy + lzz*lzz);
                    deng += (lxy*lxy + lxz*lxz + lyz*lyz);
                    double nu = lxx + lyy + lzz;
                    deng -= (1.0/6.0) * nu*nu;
                }
                rEnergyAccumulator.add(threadID, cIdx, -1, deng);
            }
        });
        if (mWR != null) {
            VectorCache.returnVec(tLambdaXXPar);
            VectorCache.returnVec(tLambdaYYPar);
            VectorCache.returnVec(tLambdaZZPar);
            VectorCache.returnVec(tLambdaXYPar);
            VectorCache.returnVec(tLambdaXZPar);
            VectorCache.returnVec(tLambdaYZPar);
        }
        if (mUR != null) {
            VectorCache.returnVec(tMuXPar);
            VectorCache.returnVec(tMuYPar);
            VectorCache.returnVec(tMuZPar);
        }
        VectorCache.returnVec(tRhoPar);
    }
}
