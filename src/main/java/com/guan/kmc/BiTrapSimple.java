package com.guan.kmc;


import com.guan.code.UT;
import com.guan.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import static java.nio.file.StandardOpenOption.*;

/**
 * @author liqa
 * <p> 简单的双势阱模型 KMC 模拟，为了快速得到结果暂时 </p>
 * <p> 采用单位（能量：eV，时间：ps，坐标：Å，温度：K） </p>
 * <p> 此类线程不安全，但不同实例间线程安全 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public class BiTrapSimple {
    // 模拟过程的量
    private double mTemp;
    private int mNlo, mNhi;
    private double mTime;
    // 属性
    private final double mEa, mEb;
    private double mCoolRate;
    
    // 越过能垒的速率，这里只有两个过程
    private double mRateHi2Lo, mRateLo2Hi;
    private final static double BASE_RATE = 1.0; // ps^-1，速率的前系数，选取原子震动的特征频率
    private final static double K_B = 0.0000861733262; // eV/K，玻尔兹曼常数
    private final static double TEMP_MIN = 0.1; // 设置模拟最低的温度
    
    // 独立的随机数生成器
    private final Random mRNG;
    
    
    public BiTrapSimple(int aNlo, int aNhi, double aEa, double aEb, double aTempStart                               ) {this(aNlo, aNhi, aEa, aEb, aTempStart, 0.0);}
    public BiTrapSimple(int aN  ,           double aEa, double aEb, double aTempStart                               ) {this(aN, 0, aEa, aEb, aTempStart);}
    public BiTrapSimple(int aNlo, int aNhi, double aEa, double aEb, double aTempStart, double aCoolRate             ) {this(aNlo, aNhi, aEa, aEb, aTempStart, aCoolRate, new Random());}
    public BiTrapSimple(int aNlo, int aNhi, double aEa, double aEb, double aTempStart, double aCoolRate, long  aSeed) {this(aNlo, aNhi, aEa, aEb, aTempStart, aCoolRate, new Random(aSeed));}
    /**
     * 创建一个模拟器，可以指定 aSeed 来手动设定种子
     * @param aNlo 初始状态处于低能的粒子数
     * @param aNhi 初始状态处于高能的粒子数
     * @param aEa 低能和高能的能量差, eV
     * @param aEb 高能到达低能需要翻越的能垒, eV
     * @param aTempStart 开始时的温度, K
     * @param aCoolRate 冷却速率, K/ps
     * @param aRNG 可以指定 aSeed 来控制内部的随机数生成器
     */
    BiTrapSimple(int aNlo, int aNhi, double aEa, double aEb, double aTempStart, double aCoolRate, Random aRNG) {
        mNlo = aNlo; mNhi = aNhi;
        mEa = aEa; mEb = aEb;
        mTemp = aTempStart;
        mCoolRate = aCoolRate;
        mRNG = aRNG;
        updateRate();
    }
    /// 参数设置
    /**
     * 修改冷却速率
     * @param aCoolRate 冷却速率，K/ps
     * @return 返回自身来方便链式调用
     */
    public BiTrapSimple setCoolRate(double aCoolRate) {mCoolRate = aCoolRate; return this;}
    
    // 更新反应速率
    private void updateRate() {
        double tBeta = 1.0 / (mTemp * K_B);
        mRateHi2Lo = BASE_RATE * MathEX.Fast.exp(-tBeta* mEb);
        mRateLo2Hi = BASE_RATE * MathEX.Fast.exp(-tBeta*(mEb+mEa));
    }
    
    
    /// 获取属性
    public double temp() {return mTemp;}
    public double time() {return mTime;}
    public double energyTotal() {return mNhi*mEa;}
    public double energyParticle() {return energyTotal()/(double)numberTotal();}
    public int numberLow() {return mNlo;}
    public int numberHigh() {return mNhi;}
    public int numberTotal() {return mNlo+mNhi;}
    public double concentrationLow() {return mNlo/(double)numberTotal();}
    public double concentrationHigh() {return mNhi/(double)numberTotal();}
    
    @Deprecated public double Etot() {return energyTotal();}
    @Deprecated public double Epar() {return energyParticle();}
    @Deprecated public double clo() {return concentrationLow();}
    @Deprecated public double chi() {return concentrationHigh();}
    @Deprecated public int Nlo() {return numberLow();}
    @Deprecated public int Nhi() {return numberHigh();}
    @Deprecated public int Ntot() {return numberTotal();}
    
    /// 进行模拟
    private boolean mEnd = false;
    private boolean mIsWaitingStep = true;
    private boolean mReactIsHi2Lo;
    /** 模拟一步，为了体现停留时间这里实际一步需要两次执行才会完成 */
    public void nextStep() {
        // 等待的步，计算反应需要的等待时间并进行等待
        if (mIsWaitingStep) {
            // 统计累加的速率 R
            double tRmid = mRateHi2Lo * mNhi;
            double tRtot = tRmid + mRateLo2Hi * mNlo;
            // 获取应该执行的反应
            mReactIsHi2Lo = mRNG.nextDouble() * tRtot < tRmid;
            // 计算等待时间
            double tWaitTime = -MathEX.Fast.log(mRNG.nextDouble()) / tRtot;
            // 更新时间和温度
            // 如果冷速不为零则需要修改温度和反应速率（认为反应时间很短，期间温度对速率的影响可以忽略）
            if (mCoolRate != 0.0) {
                double tTemp = mTemp - tWaitTime*mCoolRate;
                // 如果温度低于设定值则模拟结束
                if (tTemp < TEMP_MIN) {
                    mTime += (mTemp-TEMP_MIN) / mCoolRate; // 设置时间到温度降低到最低值的时刻
                    mTemp = TEMP_MIN;
                    mEnd = true;
                } else {
                    mTemp = tTemp;
                    mTime += tWaitTime;
                }
                updateRate();
            } else {
                // 如果没有冷速则可以只更新时间
                mTime += tWaitTime;
            }
            // 进入反应步
            mIsWaitingStep = false;
        }
        // 反应的步，执行反应
        else {
            // 根据选择的反应进行执行反应
            if (mReactIsHi2Lo) {--mNhi; ++mNlo;}
            else {++mNhi; --mNlo;}
            // 进入等待步
            mIsWaitingStep = true;
        }
    }
    
    
    public void run(int aStep                                                 ) {run(aStep, false);}
    public void run(int aStep,                                 boolean aAppend) {run(aStep, 1, aAppend);}
    public void run(int aStep, int aOutStep                                   ) {run(aStep, aOutStep, System.out);}
    public void run(int aStep, int aOutStep,                   boolean aAppend) {run(aStep, aOutStep, System.out, aAppend);}
    public void run(int aStep, int aOutStep, String aFilePath                 ) throws IOException {run(aStep, aOutStep, aFilePath, false);}
    public void run(int aStep, int aOutStep, String aFilePath, boolean aAppend) throws IOException {
        PrintStream tFilePS = UT.IO.toPrintStream(aFilePath, CREATE, aAppend ? APPEND : TRUNCATE_EXISTING);
        run(aStep, aOutStep, tFilePS, aAppend);
        tFilePS.close();
    }
    public void run(int aStep, int aOutStep, @Nullable PrintStream aOut                 ) {run(aStep, aOutStep, aOut, false);}
    public void run(int aStep, int aOutStep, @Nullable PrintStream aOut, boolean aAppend) {
        run(aStep, aOutStep, aOut, aAppend,
            String.format("%12s, %12s, %12s, %12s, %12s, %12s, %12s, %12s", "time", "temp", "Nlo", "Nhi", "clo", "chi", "Etot", "Epar"),
            () -> String.format("%12.6g, %12.6g, %12d, %12d, %12.6g, %12.6g, %12.6g, %12.6g", mTime, mTemp, mNlo, mNhi, concentrationLow(), concentrationHigh(), energyTotal(), energyParticle())
           );
    }
    /**
     * 目前暂定的模拟指定步骤数目的接口
     * @param aStep 需要运行的步骤数目，实际运行次数是 aStep * aOutStep，指定 -1 则会关闭输出并运行直到结束
     * @param aOutStep 实际输出的间隔运行数目，指定 -1 则不会输出
     * @param aOut 指定输出对象，默认为 System.out，可以指定路径则会输出文件，csv格式
     * @param aAppend 是否是附加的格式，如果是附加的则不会增加标题，将结果附加到后面，默认为 false
     * @param aHead 输出的标题字符串
     * @param aBody 输出的内容属性，重写 IStringSupplier 来指定具体内容
     */
    public void run(int aStep, int aOutStep, @Nullable PrintStream aOut, boolean aAppend, @Nullable String aHead, IStringSupplier aBody) {
        // 特殊输入处理
        if (aOutStep > 0) aStep = aStep*aOutStep;
        if (aStep < 0) {aOutStep = -1; aStep = Integer.MAX_VALUE;}
        // 标题
        if (aOutStep > 0 && aOut != null && !aAppend && aHead != null) aOut.println(aHead);
        // 初始情况
        if (aOutStep > 0 && aOut != null && !aAppend) aOut.println(aBody.getAsString());
        // 开始模拟
        for (int i = 0; i < aStep; ++i) if (!mEnd) {
            nextStep();
            if (aOut != null && aOutStep > 0 && (mEnd || aOutStep == 1 || (i != 0 && i%aOutStep == 0))) aOut.println(aBody.getAsString());
        }
    }
    @FunctionalInterface public interface IStringSupplier {String getAsString();}
}
