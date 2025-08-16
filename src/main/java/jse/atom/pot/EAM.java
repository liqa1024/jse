package jse.atom.pot;

import jse.atom.IPairPotential;
import jse.cache.VectorCache;
import jse.code.IO;
import jse.math.MathEX;
import jse.math.function.Func1;
import jse.math.function.IFunc1;
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
 * 这里采用和 lammps 类似的方法来实现数值函数样条的计算。
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
                mFRho = new IFunc1[] {Func1.zeros(0, tDRho, tNRho)};
                mRhoR = new IFunc1[][] {{Func1.zeros(0, tDR, tNR)}};
                mPhiR = new IFunc1[][] {{Func1.zeros(0, tDR, tNR)}};
                IVector tData = readData_(tReader, tNRho+tNR+tNR);
                mFRho[0].fill(tData.subVec(0, tNRho));
                mPhiR[0][0].fill(tData.subVec(tNRho, tNRho+tNR));
                mRhoR[0][0].fill(tData.subVec(tNRho+tNR, tNRho+tNR+tNR));
                // Z(r) -> phi(r)
                mPhiR[0][0].operation().mapFull2this((z, r) -> EAM_MUL * z*z / r);
                mFRhoGrad = new IFunc1[] {mFRho[0].operation().gradient(true)};
                mRhoRGrad = new IFunc1[][] {{mRhoR[0][0].operation().gradient(true)}};
                mPhiRGrad = new IFunc1[][] {{mPhiR[0][0].operation().gradient(true)}};
                break;
            }
            case "alloy": case "fs": case "adp": {
                boolean tIsFs = aFormat.equals("fs");
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
                    mFRho[i] = Func1.zeros(0, tDRho, tNRho);
                    mFRho[i].fill(tData.subVec(0, tNRho));
                    mFRhoGrad[i] = mFRho[i].operation().gradient(true);
                    if (tIsFs) {
                        int tShift = tNRho;
                        for (int j = 0; j < mTypeNum; ++j) {
                            mRhoR[j][i] = Func1.zeros(0, tDR, tNR);
                            mRhoR[j][i].fill(tData.subVec(tShift, tShift+tNR));
                            mRhoRGrad[j][i] = mRhoR[j][i].operation().gradient(true);
                            tShift += tNR;
                        }
                    } else {
                        mRhoR[0][i] = Func1.zeros(0, tDR, tNR);
                        mRhoR[0][i].fill(tData.subVec(tNRho, tNRho+tNR));
                        mRhoRGrad[0][i] = mRhoR[0][i].operation().gradient(true);
                    }
                }
                IVector tData = readData_(tReader, (1+mTypeNum)*mTypeNum/2 * tNR);
                int tShift = 0;
                for (int i = 0; i < mTypeNum; ++i) {
                    for (int j = 0; j <= i; ++j) {
                        mPhiR[i][j] = Func1.zeros(0, tDR, tNR);
                        mPhiR[i][j].fill(tData.subVec(tShift, tShift+tNR));
                        // r * phi(r) -> phi(r)
                        mPhiR[i][j].f().div2this(mPhiR[i][j].x());
                        mPhiRGrad[i][j] = mPhiR[i][j].operation().gradient(true);
                        if (j != i) {
                            mPhiR[j][i] = mPhiR[i][j];
                            mPhiRGrad[j][i] = mPhiRGrad[i][j];
                        }
                        tShift += tNR;
                    }
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
        List<Vector> tRhoPar = VectorCache.getZeros(aAtomNumber, tThreadNum);
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final Vector tRho = tRhoPar.get(threadID);
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                double deng = 0.5 * mPhiR[cType-1][type-1].subs(r);
                rEnergyAccumulator.add(threadID, cIdx, deng);
                if (mRhoR.length == 1) {
                    tRho.add(cIdx, mRhoR[0][type-1].subs(r));
                } else {
                    tRho.add(cIdx, mRhoR[cType-1][type-1].subs(r));
                }
            });
        });
        Vector tRho = tRhoPar.get(0);
        for (int i = 1; i < tThreadNum; ++i) tRho.plus2this(tRhoPar.get(i));
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            rEnergyAccumulator.add(threadID, cIdx, mFRho[cType-1].subs(tRho.get(cIdx)));
        });
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
        List<Vector> tRhoPar = VectorCache.getZeros(aAtomNumber, tThreadNum);
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final Vector tRho = tRhoPar.get(threadID);
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                double deng = mPhiR[cType-1][type-1].subs(r);
                rEnergyAccumulator.add(threadID, cIdx, idx, deng);
                if (mRhoR.length == 1) {
                    tRho.add(cIdx, mRhoR[0][type-1].subs(r));
                    tRho.add(idx, mRhoR[0][cType-1].subs(r));
                } else {
                    tRho.add(cIdx, mRhoR[cType-1][type-1].subs(r));
                    tRho.add(idx, mRhoR[type-1][cType-1].subs(r));
                }
            });
        });
        Vector tRho = tRhoPar.get(0);
        for (int i = 1; i < tThreadNum; ++i) tRho.plus2this(tRhoPar.get(i));
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            rEnergyAccumulator.add(threadID, cIdx, -1, mFRho[cType-1].subs(tRho.get(cIdx)));
        });
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
        List<Vector> tRhoPar = VectorCache.getZeros(aAtomNumber, tThreadNum);
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final Vector tRho = tRhoPar.get(threadID);
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                tRho.add(cIdx, mRhoR[mRhoR.length==1?0:(cType-1)][type-1].subs(r));
                tRho.add(idx, mRhoR[mRhoR.length==1?0:(type-1)][cType-1].subs(r));
                if (rEnergyAccumulator != null) {
                    double deng = mPhiR[cType-1][type-1].subs(r);
                    rEnergyAccumulator.add(threadID, cIdx, idx, deng);
                }
            });
        });
        Vector tRho = tRhoPar.get(0);
        for (int i = 1; i < tThreadNum; ++i) tRho.plus2this(tRhoPar.get(i));
        aNeighborListGetter.forEachNL((threadID, cIdx, cType, nl) -> {
            final double fpi = mFRhoGrad[cType-1].subs(tRho.get(cIdx));
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                double phip = mPhiRGrad[cType-1][type-1].subs(r);
                double fpj = mFRhoGrad[type-1].subs(tRho.get(idx));
                double rhojp = mRhoRGrad[mRhoR.length==1?0:(cType-1)][type-1].subs(r);
                double rhoip = mRhoRGrad[mRhoR.length==1?0:(type-1)][cType-1].subs(r);
                double fpair = -(fpi*rhojp + fpj*rhoip + phip) / r;
                if (rForceAccumulator != null) {
                    rForceAccumulator.add(threadID, cIdx, idx, dx*fpair, dy*fpair, dz*fpair);
                }
                if (rVirialAccumulator != null) {
                    rVirialAccumulator.add(threadID, cIdx, idx, dx*dx*fpair, dy*dy*fpair, dz*dz*fpair, dx*dy*fpair, dx*dz*fpair, dy*dz*fpair);
                }
            });
            if (rEnergyAccumulator != null) {
                rEnergyAccumulator.add(threadID, cIdx, -1, mFRho[cType-1].subs(tRho.get(cIdx)));
            }
        });
        VectorCache.returnVec(tRhoPar);
    }
}
