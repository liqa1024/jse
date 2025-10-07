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
import org.jetbrains.annotations.ApiStatus;
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
 * 这里默认采用 lammps 使用的样条算法进行插值，考虑到样条插值并不总是收敛，有时需要主动关闭这个。
 *
 * @author liqa
 */
public class EAM implements IPairPotential {
    public final static class Conf {
        /** 是否使用 lammps 中采用的低精度 hartree 和 bohr，从而让结果和 lammps 一致 */
        public static boolean USE_LAMMPS_PRECISION = true;
        /** 是否使用 lammps 中使用的样条插值，从而让结果和 lammps 一致；样条插值并不总是收敛，因此有时需要手动关闭 */
        public static boolean USE_SPLINE = true;
    }
    private final static double EAM_MUL;
    static {
        if (Conf.USE_LAMMPS_PRECISION) {
            EAM_MUL = 27.2*0.529;
        } else {
            EAM_MUL = UNITS.get("Hartree")*UNITS.get("Bohr");
        }
    }
    
    interface ISpline {
        double subs(double aX);
        double subsGrad(double aX);
    }
    
    /** 简单的线性样条插值 */
    static class LinearSpline implements ISpline {
        private final IFunc1 mFunc, mFuncGrad;
        LinearSpline(double aDx, IVector aData) {
            int tN = aData.size();
            mFunc = ConstBoundFunc1.zeros(0, aDx, tN);
            mFunc.fill(aData);
            mFuncGrad = ZeroBoundFunc1.zeros(aDx*0.5, aDx, tN-1);
            for (int i = 0; i < tN-1; ++i) {
                mFuncGrad.set(i, (aData.get(i+1) - aData.get(i)) / aDx);
            }
        }
        @Override public double subs(double aX) {
            return mFunc.subs(aX);
        }
        @Override public double subsGrad(double aX) {
            return mFuncGrad.subs(aX);
        }
    }
    
    /** lammps 中使用的快速样条 */
    static class FastSpline implements ISpline {
        private final double mDx;
        private final int mNx;
        private final double[][] mSplines;
        FastSpline(double aDx, IVector aData) {
            mDx = aDx;
            mNx = aData.size();
            mSplines = new double[mNx][7];
            
            // 样条参数计算，参考 lammps 相关代码
            for (int m = 0; m < mNx; ++m) {
                mSplines[m][6] = aData.get(m);
            }
            mSplines[0][5] = mSplines[1][6] - mSplines[0][6];
            mSplines[1][5] = 0.5 * (mSplines[2][6] - mSplines[0][6]);
            mSplines[mNx-2][5] = 0.5 * (mSplines[mNx-1][6] - mSplines[mNx-3][6]);
            mSplines[mNx-1][5] = mSplines[mNx-1][6] - mSplines[mNx-2][6];
            
            for (int m = 2; m < mNx-2; ++m) {
                mSplines[m][5] = ((mSplines[m-2][6] - mSplines[m+2][6]) + 8.0*(mSplines[m+1][6] - mSplines[m-1][6])) / 12.0;
            }
            for (int m = 0; m < mNx-1; ++m) {
                mSplines[m][4] = 3.0*(mSplines[m+1][6] - mSplines[m][6]) - 2.0*mSplines[m][5] - mSplines[m+1][5];
                mSplines[m][3] = mSplines[m][5] + mSplines[m+1][5] - 2.0*(mSplines[m+1][6] - mSplines[m][6]);
            }
            mSplines[mNx-1][4] = 0.0;
            mSplines[mNx-1][3] = 0.0;
            
            for (int m = 0; m < mNx; ++m) {
                mSplines[m][2] = mSplines[m][5]/mDx;
                mSplines[m][1] = 2.0*mSplines[m][4]/mDx;
                mSplines[m][0] = 3.0*mSplines[m][3]/mDx;
            }
        }
        
        @Override public double subs(double aX) {
            double p = aX/mDx;
            int m = MathEX.Code.floor2int(p);
            if (m >= mNx) m = mNx-1;
            p -= m;
            if (p > 1.0) p = 1.0;
            double[] coeff = mSplines[m];
            return ((coeff[3]*p + coeff[4])*p + coeff[5])*p + coeff[6];
        }
        @Override public double subsGrad(double aX) {
            double p = aX/mDx;
            int m = MathEX.Code.floor2int(p);
            if (m >= mNx) m = mNx-1;
            p -= m;
            if (p > 1.0) p = 1.0;
            double[] coeff = mSplines[m];
            return (coeff[0]*p + coeff[1])*p + coeff[2];
        }
    }
    
    private final double mCut, mCutsq;
    private final double mDRho, mDR;
    private final int mNRho, mNR;
    private final String mHeader;
    private final IVector[] mFRho;
    private final IVector[][] mRhoR, mRPhiR;
    private final IVector @Nullable[][] mUR, mWR;
    private final ISpline[] mFRhoSpline;
    private final ISpline[][] mRhoRSpline, mRPhiRSpline;
    private final ISpline @Nullable[][] mURSpline, mWRSpline;
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
                mNRho = Integer.parseInt(tTokens[0]);
                mDRho = Double.parseDouble(tTokens[1]);
                mNR = Integer.parseInt(tTokens[2]);
                mDR = Double.parseDouble(tTokens[3]);
                double tRCut = Double.parseDouble(tTokens[4]);
                mCutsq = tRCut*tRCut;
                mCut = tRCut;
                mFRho = new IVector[1];
                mRhoR = new IVector[1][1];
                mRPhiR = new IVector[1][1];
                IVector tData = readData_(tReader, mNRho+mNR+mNR);
                mFRho[0] = tData.subVec(0, mNRho);
                mRPhiR[0][0] = tData.subVec(mNRho, mNRho+mNR);
                mRhoR[0][0] = tData.subVec(mNRho+mNR, mNRho+mNR+mNR);
                // Z(r) -> r*phi(r)
                mRPhiR[0][0].operation().map2this(z -> EAM_MUL * z*z);
                mFRhoSpline = new ISpline[] {Conf.USE_SPLINE ? new FastSpline(mDRho, mFRho[0]) : new LinearSpline(mDRho, mFRho[0])};
                mRhoRSpline = new ISpline[][] {{Conf.USE_SPLINE ? new FastSpline(mDR, mRhoR[0][0]) : new LinearSpline(mDR, mRhoR[0][0])}};
                mRPhiRSpline = new ISpline[][] {{Conf.USE_SPLINE ? new FastSpline(mDR, mRPhiR[0][0]) : new LinearSpline(mDR, mRPhiR[0][0])}};
                mUR = null; mWR = null; mURSpline = null; mWRSpline = null;
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
                mNRho = Integer.parseInt(tTokens[0]);
                mDRho = Double.parseDouble(tTokens[1]);
                mNR = Integer.parseInt(tTokens[2]);
                mDR = Double.parseDouble(tTokens[3]);
                double tRCut = Double.parseDouble(tTokens[4]);
                mCutsq = tRCut*tRCut;
                mCut = tRCut;
                mFRho = new IVector[mTypeNum];
                mRhoR = new IVector[tIsFs?mTypeNum:1][mTypeNum];
                mRPhiR = new IVector[mTypeNum][mTypeNum];
                mFRhoSpline = new ISpline[mTypeNum];
                mRhoRSpline = new ISpline[tIsFs?mTypeNum:1][mTypeNum];
                mRPhiRSpline = new ISpline[mTypeNum][mTypeNum];
                for (int i = 0; i < mTypeNum; ++i) {
                    tTokens = IO.Text.splitBlank(tReader.readLine());
                    mAtomicNumbers[i] = Integer.parseInt(tTokens[0]);
                    mMasses[i] = Double.parseDouble(tTokens[1]);
                    mLatticeConsts[i] = Double.parseDouble(tTokens[2]);
                    mLatticeTypes[i] = tTokens[3];
                    IVector tData = readData_(tReader, mNRho + (tIsFs?(mTypeNum*mNR):mNR));
                    mFRho[i] = tData.subVec(0, mNRho);
                    mFRhoSpline[i] = Conf.USE_SPLINE ? new FastSpline(mDRho, mFRho[i]) : new LinearSpline(mDRho, mFRho[i]);
                    if (tIsFs) {
                        int tShift = mNRho;
                        for (int j = 0; j < mTypeNum; ++j) {
                            mRhoR[j][i] = tData.subVec(tShift, tShift+mNR);
                            mRhoRSpline[j][i] = Conf.USE_SPLINE ? new FastSpline(mDR, mRhoR[j][i]) : new LinearSpline(mDR, mRhoR[j][i]);
                            tShift += mNR;
                        }
                    } else {
                        mRhoR[0][i] = tData.subVec(mNRho, mNRho+mNR);
                        mRhoRSpline[0][i] = Conf.USE_SPLINE ? new FastSpline(mDR, mRhoR[0][i]) : new LinearSpline(mDR, mRhoR[0][i]);
                    }
                }
                IVector tData = readData_(tReader, (1+mTypeNum)*mTypeNum/2 * mNR);
                int tShift = 0;
                for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                    mRPhiR[i][j] = tData.subVec(tShift, tShift+mNR);
                    mRPhiRSpline[i][j] = Conf.USE_SPLINE ? new FastSpline(mDR, mRPhiR[i][j]) : new LinearSpline(mDR, mRPhiR[i][j]);
                    if (j != i) {
                        mRPhiR[j][i] = mRPhiR[i][j];
                        mRPhiRSpline[j][i] = mRPhiRSpline[i][j];
                    }
                    tShift += mNR;
                }
                if (!tIsAdp) {
                    mUR = null; mWR = null; mURSpline = null; mWRSpline = null;
                    break;
                }
                mUR = new IVector[mTypeNum][mTypeNum];
                mWR = new IVector[mTypeNum][mTypeNum];
                mURSpline = new ISpline[mTypeNum][mTypeNum];
                mWRSpline = new ISpline[mTypeNum][mTypeNum];
                tData = readData_(tReader, (1+mTypeNum)*mTypeNum/2 * mNR);
                tShift = 0;
                for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                    mUR[i][j] = tData.subVec(tShift, tShift+mNR);
                    mURSpline[i][j] = Conf.USE_SPLINE ? new FastSpline(mDR, mUR[i][j]) : new LinearSpline(mDR, mUR[i][j]);
                    if (j != i) {
                        mUR[j][i] = mUR[i][j];
                        mURSpline[j][i] = mURSpline[i][j];
                    }
                    tShift += mNR;
                }
                tData = readData_(tReader, (1+mTypeNum)*mTypeNum/2 * mNR);
                tShift = 0;
                for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                    mWR[i][j] = tData.subVec(tShift, tShift+mNR);
                    mWRSpline[i][j] = Conf.USE_SPLINE ? new FastSpline(mDR, mWR[i][j]) : new LinearSpline(mDR, mWR[i][j]);
                    if (j != i) {
                        mWR[j][i] = mWR[i][j];
                        mWRSpline[j][i] = mWRSpline[i][j];
                    }
                    tShift += mNR;
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
    
    @ApiStatus.Experimental
    public IFunc1 frho(int aType) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, mDRho, mNRho);
        tOut.fill(mFRho[aType-1]);
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 frhoSpline(int aType, int aN) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDRho*mNRho)/aN, aN);
        tOut.fill(rho -> mFRhoSpline[aType-1].subs(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 frhoGradSpline(int aType, int aN) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDRho*mNRho)/aN, aN);
        tOut.fill(rho -> mFRhoSpline[aType-1].subsGrad(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 rhor(int aType1, int aType2) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, mDR, mNR);
        tOut.fill(mRhoR[mRhoR.length==1?0:(aType1-1)][aType2-1]);
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 rhorSpline(int aType1, int aType2, int aN) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mRhoRSpline[mRhoR.length==1?0:(aType1-1)][aType2-1].subs(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 rhorGradSpline(int aType1, int aType2, int aN) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mRhoRSpline[mRhoR.length==1?0:(aType1-1)][aType2-1].subsGrad(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 rphir(int aType1, int aType2) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, mDR, mNR);
        tOut.fill(mRPhiR[aType1-1][aType2-1]);
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 rphirSpline(int aType1, int aType2, int aN) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mRPhiRSpline[aType1-1][aType2-1].subs(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public IFunc1 rphirGradSpline(int aType1, int aType2, int aN) {
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mRPhiRSpline[aType1-1][aType2-1].subsGrad(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public @Nullable IFunc1 ur(int aType1, int aType2) {
        if (mUR == null) return null;
        IFunc1 tOut = ConstBoundFunc1.zeros(0, mDR, mNR);
        tOut.fill(mUR[aType1-1][aType2-1]);
        return tOut;
    }
    @ApiStatus.Experimental
    public @Nullable IFunc1 urSpline(int aType1, int aType2, int aN) {
        if (mURSpline == null) return null;
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mURSpline[aType1-1][aType2-1].subs(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public @Nullable IFunc1 urGradSpline(int aType1, int aType2, int aN) {
        if (mURSpline == null) return null;
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mURSpline[aType1-1][aType2-1].subsGrad(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public @Nullable IFunc1 wr(int aType1, int aType2) {
        if (mWR == null) return null;
        IFunc1 tOut = ConstBoundFunc1.zeros(0, mDR, mNR);
        tOut.fill(mWR[aType1-1][aType2-1]);
        return tOut;
    }
    @ApiStatus.Experimental
    public @Nullable IFunc1 wrSpline(int aType1, int aType2, int aN) {
        if (mWRSpline == null) return null;
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mWRSpline[aType1-1][aType2-1].subs(rho));
        return tOut;
    }
    @ApiStatus.Experimental
    public @Nullable IFunc1 wrGradSpline(int aType1, int aType2, int aN) {
        if (mWRSpline == null) return null;
        IFunc1 tOut = ConstBoundFunc1.zeros(0, (mDR*mNR)/aN, aN);
        tOut.fill(rho -> mWRSpline[aType1-1][aType2-1].subsGrad(rho));
        return tOut;
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
    
    /**
     * 输出成 lammps 支持的 eam 势函数文件
     * @param aFilePath 需要输出的路径
     * @param aFormat 可选的 EAM 势函数文件格式，可选 {@code "eam", "alloy", "fs", "adp"}，默认根据后缀名自动检测
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath, @Nullable String aFormat) throws IOException {
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
        try (IO.IWriteln tWriteln = IO.toWriteln(aFilePath)) {write_(tWriteln, aFormat);}
    }
    /**
     * 输出成 lammps 支持的 eam 势函数文件
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {
        write(aFilePath, null);
    }
    /** 提供 {@link IO.IWriteln} 的接口来实现边写入边处理，此方法不会自动关闭流 */
    void write_(IO.IWriteln aWriteln, String aFormat) throws IOException {
        String[] tHeaders = mHeader.split("\n");
        String tDNCut = mNRho+" "+mDRho+" "+mNR+" "+mDR+" "+mCut;
        switch(aFormat) {
        case "eam": {
            if (mTypeNum != 1) throw new IllegalStateException();
            aWriteln.writeln(tHeaders.length<1 ? "" : tHeaders[0]);
            aWriteln.writeln(mAtomicNumbers[0]+" "+mMasses[0]+" "+mLatticeConsts[0]+" "+mLatticeTypes[0]);
            aWriteln.writeln(tDNCut);
            for (int i = 0; i < mNRho; ++i) {
            aWriteln.writeln(String.valueOf(mFRho[0].get(i)));
            }
            for (int i = 0; i < mNR; ++i) {
            aWriteln.writeln(String.valueOf(MathEX.Fast.sqrt(mRPhiR[0][0].get(i)/EAM_MUL)));
            }
            for (int i = 0; i < mNR; ++i) {
            aWriteln.writeln(String.valueOf(mRhoR[0][0].get(i)));
            }
            break;
        }
        case "alloy": case "fs": case "adp": {
            boolean tIsFs = aFormat.equals("fs");
            boolean tIsAdp = aFormat.equals("adp");
            if (!tIsFs && mRhoR.length!=1) throw new IllegalStateException();
            if (!tIsAdp && (mUR!=null || mWR!=null)) throw new IllegalStateException();
            aWriteln.writeln(tHeaders.length<1 ? "" : tHeaders[0]);
            aWriteln.writeln(tHeaders.length<2 ? "" : tHeaders[1]);
            aWriteln.writeln(tHeaders.length<3 ? "" : tHeaders[2]);
            aWriteln.writeln(mTypeNum+" "+String.join(" ", mSymbols));
            aWriteln.writeln(tDNCut);
            for (int i = 0; i < mTypeNum; ++i) {
                aWriteln.writeln(mAtomicNumbers[i]+" "+mMasses[i]+" "+mLatticeConsts[i]+" "+mLatticeTypes[i]);
                for (int k = 0; k < mNRho; ++k) {
                aWriteln.writeln(String.valueOf(mFRho[i].get(k)));
                }
                if (tIsFs) {
                    for (int j = 0; j < mTypeNum; ++j) {
                        for (int k = 0; k < mNR; ++k) {
                        aWriteln.writeln(String.valueOf(mRhoR[mRhoR.length==1?0:j][i].get(k)));
                        }
                    }
                } else {
                    for (int k = 0; k < mNR; ++k) {
                    aWriteln.writeln(String.valueOf(mRhoR[0][i].get(k)));
                    }
                }
            }
            for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                for (int k = 0; k < mNR; ++k) {
                aWriteln.writeln(String.valueOf(mRPhiR[i][j].get(k)));
                }
            }
            if (!tIsAdp) break;
            for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                for (int k = 0; k < mNR; ++k) {
                aWriteln.writeln(mUR==null ? "0.0" : String.valueOf(mUR[i][j].get(k)));
                }
            }
            for (int i = 0; i < mTypeNum; ++i) for (int j = 0; j <= i; ++j) {
                for (int k = 0; k < mNR; ++k) {
                aWriteln.writeln(mWR==null ? "0.0" : String.valueOf(mWR[i][j].get(k)));
                }
            }
            break;
        }
        default: {
            throw new IllegalArgumentException("Invalid EAM format: " + aFormat);
        }}
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
                double deng = 0.5 * mRPhiRSpline[cType-1][type-1].subs(r) / r;
                rEnergyAccumulator.add(threadID, cIdx, deng);
                tRho.add(cIdx, mRhoRSpline[mRhoR.length==1?0:(cType-1)][type-1].subs(r));
                if (mUR != null) {
                    assert mURSpline != null;
                    double u = mURSpline[cType-1][type-1].subs(r);
                    tMuX.add(cIdx, u*dx);
                    tMuY.add(cIdx, u*dy);
                    tMuZ.add(cIdx, u*dz);
                }
                if (mWR != null) {
                    assert mWRSpline != null;
                    double w = mWRSpline[cType-1][type-1].subs(r);
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
            double deng = mFRhoSpline[cType-1].subs(tRho.get(cIdx));
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
                double deng = mRPhiRSpline[cType-1][type-1].subs(r) / r;
                rEnergyAccumulator.add(threadID, cIdx, idx, deng);
                tRho.add(cIdx, mRhoRSpline[mRhoR.length==1?0:(cType-1)][type-1].subs(r));
                tRho.add(idx, mRhoRSpline[mRhoR.length==1?0:(type-1)][cType-1].subs(r));
                if (mUR != null) {
                    assert mURSpline != null;
                    double u = mURSpline[cType-1][type-1].subs(r);
                    tMuX.add(cIdx, u*dx);
                    tMuY.add(cIdx, u*dy);
                    tMuZ.add(cIdx, u*dz);
                    tMuX.add(idx, -u*dx);
                    tMuY.add(idx, -u*dy);
                    tMuZ.add(idx, -u*dz);
                }
                if (mWR != null) {
                    assert mWRSpline != null;
                    double w = mWRSpline[cType-1][type-1].subs(r);
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
            double deng = mFRhoSpline[cType-1].subs(tRho.get(cIdx));
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
                tRho.add(cIdx, mRhoRSpline[mRhoR.length==1?0:(cType-1)][type-1].subs(r));
                tRho.add(idx, mRhoRSpline[mRhoR.length==1?0:(type-1)][cType-1].subs(r));
                if (mUR != null) {
                    assert mURSpline != null;
                    double u = mURSpline[cType-1][type-1].subs(r);
                    tMuX.add(cIdx, u*dx);
                    tMuY.add(cIdx, u*dy);
                    tMuZ.add(cIdx, u*dz);
                    tMuX.add(idx, -u*dx);
                    tMuY.add(idx, -u*dy);
                    tMuZ.add(idx, -u*dz);
                }
                if (mWR != null) {
                    assert mWRSpline != null;
                    double w = mWRSpline[cType-1][type-1].subs(r);
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
            final double fpi = mFRhoSpline[cType-1].subsGrad(tRho.get(cIdx));
            nl.forEachDxyzTypeIdx(mCut, (dx, dy, dz, type, idx) -> {
                double rsq = dx*dx + dy*dy + dz*dz;
                if (rsq >= mCutsq) return;
                double r = MathEX.Fast.sqrt(rsq);
                double recip = 1.0/r;
                double rphi = mRPhiRSpline[cType-1][type-1].subs(r);
                double rphip = mRPhiRSpline[cType-1][type-1].subsGrad(r);
                double fpj = mFRhoSpline[type-1].subsGrad(tRho.get(idx));
                double rhojp = mRhoRSpline[mRhoR.length==1?0:(cType-1)][type-1].subsGrad(r);
                double rhoip = mRhoRSpline[mRhoR.length==1?0:(type-1)][cType-1].subsGrad(r);
                double phi = rphi*recip;
                double phip = rphip*recip - phi*recip;
                double fpair = -(fpi*rhojp + fpj*rhoip + phip) * recip;
                double fx = dx*fpair, fy = dy*fpair, fz = dz*fpair;
                if (mUR != null) {
                    assert mURSpline != null;
                    double u = mURSpline[cType-1][type-1].subs(r);
                    double up = mURSpline[cType-1][type-1].subsGrad(r);
                    double dmx = tMuX.get(cIdx) - tMuX.get(idx);
                    double dmy = tMuY.get(cIdx) - tMuY.get(idx);
                    double dmz = tMuZ.get(cIdx) - tMuZ.get(idx);
                    double dm3 = dmx*dx + dmy*dy + dmz*dz;
                    fx -= (dmx*u + dm3*up*dx*recip);
                    fy -= (dmy*u + dm3*up*dy*recip);
                    fz -= (dmz*u + dm3*up*dz*recip);
                }
                if (mWR != null) {
                    assert mWRSpline != null;
                    double w = mWRSpline[cType-1][type-1].subs(r);
                    double wp = mWRSpline[cType-1][type-1].subsGrad(r);
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
                    rVirialAccumulator.add(threadID, cIdx, idx, fx, fy, fz, dx, dy, dz);
                }
                if (rEnergyAccumulator != null) {
                    rEnergyAccumulator.add(threadID, cIdx, idx, phi);
                }
            });
            if (rEnergyAccumulator != null) {
                double deng = mFRhoSpline[cType-1].subs(tRho.get(cIdx));
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
