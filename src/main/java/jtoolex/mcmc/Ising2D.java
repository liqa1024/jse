package jtoolex.mcmc;

import jtool.code.CS;
import jtool.math.MathEX;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.Matrices;
import jtool.parallel.LocalRandom;
import jtool.math.vector.IVector;
import jtool.math.vector.Vectors;
import jtool.parallel.AbstractThreadPool;
import jtool.parallel.ParforThreadPool;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static jtool.code.CS.RANDOM;


/**
 * 简单的蒙特卡洛模拟二维 Ising 模型，
 * 支持并行
 * @author liqa
 */
public class Ising2D extends AbstractThreadPool<ParforThreadPool> {
    private final double mJ, mH;
    /** 可定义的主随机数生成器，默认为 {@link CS#RANDOM} */
    private final Random mRNG;
    
    /**
     * 按照给定参数构造一个模拟二维 Ising 模型的模拟器，有：
     * {@code Ham = -JΣᵢσᵢσᵢ₊₁ + HΣᵢσᵢ}
     * @author liqa
     * @param aJ 哈密顿量中的参数 J
     * @param aH 哈密顿量中的参数 H
     * @param aThreadNum 计算使用的线程数，默认为 1
     * @param aRNG 可自定义的随机数生成器
     * @param aNoCompetitive 内部 parfor 是否是竞争的，默认为 false，在固定种子后为 true 来保证结果一致
     */
    Ising2D(double aJ, double aH, int aThreadNum, Random aRNG, boolean aNoCompetitive) {
        super(new ParforThreadPool(aThreadNum, aNoCompetitive));
        mJ = aJ; mH = aH;
        mRNG = aRNG;
    }
    public Ising2D(double aJ, double aH, int aThreadNum, long aSeed) {this(aJ, aH, aThreadNum, new Random(aSeed), true);}
    public Ising2D(double aJ, double aH, int aThreadNum) {this(aJ, aH, aThreadNum, RANDOM, false);}
    public Ising2D(double aJ, double aH) {this(aJ, aH, 1);}
    
    private long[] genSeeds_(int aSize) {
        long[] rSeeds = new long[aSize];
        for (int i = 0; i < aSize; ++i) rSeeds[i] = mRNG.nextLong();
        return rSeeds;
    }
    
    /** 获取一个随机的初始结构 */
    public IMatrix initSpins(int aRowNum, int aColNum) {
        IMatrix rSpins = Matrices.zeros(aRowNum, aColNum);
        rSpins.assignCol(() -> mRNG.nextBoolean() ? 1.0 : -1.0);
        return rSpins;
    }
    public IMatrix initSpins(int aL) {return initSpins(aL, aL);}
    
    
    /** 统计能量，注意自旋相互作用只需要考虑一半 */
    public double statE(IMatrix aSpins) {
        int tRowNum = aSpins.rowNumber();
        int tColNum = aSpins.columnNumber();
        double rE = 0.0;
        for (int i = 0; i < tRowNum; ++i) for (int j = 0; j < tColNum; ++j)  {
            // 先考虑周期边界条件
            int ipp = i + 1; if (ipp >= tRowNum) ipp -= tRowNum;
            int jpp = j + 1; if (jpp >= tColNum) jpp -= tColNum;
            // 获取周围自旋值
            double tSpinC = aSpins.get(i  , j  );
            double tSpinR = aSpins.get(ipp, j  );
            double tSpinU = aSpins.get(i  , jpp);
            // 计算能量
            rE += tSpinC * mH;
            rE -= tSpinC*tSpinR * mJ;
            rE -= tSpinC*tSpinU * mJ;
        }
        return rE;
    }
    
    /** 用于返回的统计物理量 */
    public static final class Data {
        public final double E, M, E2, M2;
        Data(double E, double M, double E2, double M2) {
            this.E = E; this.M = M; this.E2 = E2; this.M2 = M2;
        }
    }
    
    /**
     * 对输入的自旋结构执行 N 步蒙特卡洛模拟，并统计
     * {@code <E>, <|M|>, <E^2>, <|M|^2>}
     * @author liqa
     * @param aSpinsList 所有自旋结构组成的数组，会并行执行模拟
     * @param aN 需要的模拟步骤数目
     * @param aT 需要的模拟温度，认为 kB = 1.0
     * @param aStat 是否顺便统计物理量，默认为 true
     * @return 统计得到的物理量数据，会将输入所有结构的物理量进行平均，如果关闭了统计则输出 null
     */
    public Data startMonteCarlo(final List<? extends IMatrix> aSpinsList, final long aN, double aT, final boolean aStat) {
        // 需要返回的统计物理量，
        // 注意虽然 parfor 支持写入不同位置的数组时的线程安全，并且也是无锁的操作，
        // 但频繁写入依旧会严重影响性能，因此这里使用这样的写法
        // （虽然不同对象会比同一个 double[] 中不同位置性能更好，但依旧不如原生）
        int tThreadNum = nThreads();
        final IVector rSumE  = aStat ? Vectors.zeros(tThreadNum) : null;
        final IVector rSumM  = aStat ? Vectors.zeros(tThreadNum) : null;
        final IVector rSumE2 = aStat ? Vectors.zeros(tThreadNum) : null;
        final IVector rSumM2 = aStat ? Vectors.zeros(tThreadNum) : null;
        
        // 为了保证结果可重复，这里统一为每个线程生成一个种子，用于创建 LocalRandom
        final long[] tSeeds = genSeeds_(tThreadNum);
        
        pool().parfor(aSpinsList.size(), (l, threadID) -> {
            // 开始之前先初始统计物理量，初始值不计入统计
            double tE = Double.NaN;
            double tM = Double.NaN;
            double subSumE  = Double.NaN;
            double subSumM  = Double.NaN;
            double subSumE2 = Double.NaN;
            double subSumM2 = Double.NaN;
            // 从共享内存中获取数据
            IMatrix tSpins = aSpinsList.get(l);
            LocalRandom tRNG = new LocalRandom(tSeeds[threadID]);
            if (aStat) {
                tE = statE(tSpins);
                tM = tSpins.operation().sum();
                subSumE  = rSumE .get(threadID);
                subSumM  = rSumM .get(threadID);
                subSumE2 = rSumE2.get(threadID);
                subSumM2 = rSumM2.get(threadID);
            }
            int tRowNum = tSpins.rowNumber();
            int tColNum = tSpins.columnNumber();
            // 开始蒙特卡洛模拟
            for (long k = 0; k < aN; ++k) {
                int i = tRNG.nextInt(tRowNum);
                int j = tRNG.nextInt(tColNum);
                // 先考虑周期边界条件
                int ipp = i + 1; if (ipp >= tRowNum) ipp -= tRowNum;
                int imm = i - 1; if (imm <  0      ) imm += tRowNum;
                int jpp = j + 1; if (jpp >= tColNum) jpp -= tColNum;
                int jmm = j - 1; if (jmm <  0      ) jmm += tColNum;
                // 获取周围自旋值
                double tSpinC = tSpins.get(i  , j  );
                double tSpinL = tSpins.get(imm, j  );
                double tSpinR = tSpins.get(ipp, j  );
                double tSpinU = tSpins.get(i  , jpp);
                double tSpinD = tSpins.get(i  , jmm);
                // 计算翻转前后能量差
                double dE = -tSpinC * mH;
                dE += tSpinC*tSpinL * mJ;
                dE += tSpinC*tSpinR * mJ;
                dE += tSpinC*tSpinU * mJ;
                dE += tSpinC*tSpinD * mJ;
                dE *= 2.0;
                // 如果能量差小于 0 则 100% 接受翻转，否则概率接受
                // 由于使用了 Fast 库，因此计算 exp 的开销现在基本可以忽略
                if ((dE <= 0) || (tRNG.nextDouble() < MathEX.Fast.exp(-dE/aT))) {
                    tSpins.update(i, j, v->-v);
                    if (aStat) {
                        // 更新物理量
                        tE += dE;
                        tM -= 2.0*tSpinC;
                    }
                }
                if (aStat) {
                    // 累加统计结果
                    subSumE  += tE;
                    subSumM  += Math.abs(tM); // 磁矩需要取绝对值
                    subSumE2 += tE*tE;
                    subSumM2 += tM*tM;
                }
            }
            // 统一将修改后的数据存放回共享内存，这样效率更高
            if (aStat) {
                rSumE .set(threadID, subSumE );
                rSumM .set(threadID, subSumM );
                rSumE2.set(threadID, subSumE2);
                rSumM2.set(threadID, subSumM2);
            }
        });
        if (aStat) {
            double tDiv = aN*aSpinsList.size();
            return new Data(rSumE.sum()/tDiv, rSumM.sum()/tDiv, rSumE2.sum()/tDiv, rSumM2.sum()/tDiv);
        } else {
            return null;
        }
    }
    public Data startMonteCarlo(List<? extends IMatrix> aSpinsList, long aN, double aT) {return startMonteCarlo(aSpinsList, aN, aT, true);}
    public Data startMonteCarlo(IMatrix aSpins, long aN, double aT, boolean aStat) {return startMonteCarlo(Collections.singletonList(aSpins), aN, aT, aStat);}
    public Data startMonteCarlo(IMatrix aSpins, long aN, double aT) {return startMonteCarlo(aSpins, aN, aT, true);}
}
