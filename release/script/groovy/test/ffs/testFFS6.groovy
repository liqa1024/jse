package test.ffs

import jse.code.UT
import jse.jobs.StepJobManager
import jse.math.table.Tables
import jse.math.vector.Vectors
import jse.plot.Plotters
import jsex.rareevent.BufferedFullPathGenerator
import jsex.rareevent.ForwardFluxSampling
import rareevent.NoiseClusterGrowth


/**
 * 用来测试只有快过程时的 Gap FFS 准确性
 */

int N0 = 100;
def biCal = new NoiseClusterGrowth.ParameterCalculator();
int threadNum = 12;

double plusProb = 0.100;
double minusProb = 0.110;
def lambda = (20..200).step(10);


new StepJobManager('testFFS6', 1)
.init {println("0. 绘制 lambda 随时间变化曲线");}
.doJob {
    def biPathGen = new NoiseClusterGrowth.PathGenerator(2, plusProb, minusProb, 0.60, -0.10, 1);
    
    def fullPath = new BufferedFullPathGenerator<>(biPathGen, biCal);
    def pi = fullPath.fullPathInit();
    
    def sizes = Vectors.zeros(20000);
    def sizesReal = Vectors.zeros(20000);
    for (i in 0..<sizes.size()) {
        def p = pi.next();
        sizes[i] = pi.lambda();
        sizesReal[i] = p.value;
    }
    
    def step = Vectors.linsequence(0, biPathGen.skipNum, sizes.size());
    
    def plt = Plotters.get();
    plt.plot(step, sizes    , 'lambda').color(0);
    plt.plot(step, sizesReal, 'real'  ).color(1).width(5.0);
    plt.xRange(0, step.last()+1);
    plt.xlabel('step').ylabel('value');
    plt.show();
    
    step      = step     [(0..<step     .size()).step(1000)];
    sizes     = sizes    [(0..<sizes    .size()).step(1000)];
    sizesReal = sizesReal[(0..<sizesReal.size()).step(1000)];
    
    def plt2 = Plotters.get();
    plt2.plot(step, sizes    , 'lambda').color(0);
    plt2.plot(step, sizesReal, 'real'  ).color(1).width(5.0);
    plt2.xRange(0, step.last()+1);
    plt2.xlabel('step').ylabel('value');
    plt2.show();
}
.then {println("1. FFS 并绘制不同间距的结果");}
.doJob {
    def k1 = Vectors.zeros(lambda.size());
    def k100 = Vectors.zeros(lambda.size());
    def k1000 = Vectors.zeros(lambda.size());
    def k10000 = Vectors.zeros(lambda.size());
    def kRef = Vectors.zeros(lambda.size());
    
    def biPathGen = new NoiseClusterGrowth.PathGenerator(100, plusProb, minusProb, 0.60, -0.10);
    def FFS = new ForwardFluxSampling<>(biPathGen, biCal, threadNum, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*10000);
    UT.Timer.tic();
    FFS.run();
    k1[0] = FFS.getK0();
    println("k0 = ${k1[0]}");
    int i = 0;
    while (!FFS.finished()) {
        FFS.run();
        k1[i+1] = FFS.getProb(i);
        println("prob = ${k1[i]}");
        ++i;
    }
    UT.Timer.toc("1, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, plusProb, minusProb, 0.60, -0.10, 100);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, threadNum, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*10000);
    UT.Timer.tic();
    FFS.run();
    k100[0] = FFS.getK0();
    println("k0 = ${k100[0]}");
    i = 0;
    while (!FFS.finished()) {
        FFS.run();
        k100[i+1] = FFS.getProb(i);
        println("prob = ${k100[i]}");
        ++i;
    }
    UT.Timer.toc("100, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, plusProb, minusProb, 0.60, -0.10, 1000);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, threadNum, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*10000);
    UT.Timer.tic();
    FFS.run();
    k1000[0] = FFS.getK0();
    println("k0 = ${k1000[0]}");
    i = 0;
    while (!FFS.finished()) {
        FFS.run();
        k1000[i+1] = FFS.getProb(i);
        println("prob = ${k1000[i]}");
        ++i;
    }
    UT.Timer.toc("1000, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, plusProb, minusProb, 0.60, -0.10, 10000);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, threadNum, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*10000);
    UT.Timer.tic();
    FFS.run();
    k10000[0] = FFS.getK0();
    println("k0 = ${k10000[0]}");
    i = 0;
    while (!FFS.finished()) {
        FFS.run();
        k10000[i+1] = FFS.getProb(i);
        println("prob = ${k10000[i]}");
        ++i;
    }
    UT.Timer.toc("10000, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");
    FFS.shutdown();
    
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(100, plusProb, minusProb, 0.10, -0.10);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, threadNum, 10, lambda, N0).setMaxPathNum(N0*1000);
    UT.Timer.tic();
    FFS.run();
    kRef[0] = FFS.getK0();
    println("k0 = ${kRef[0]}");
    i = 0;
    while (!FFS.finished()) {
        FFS.run();
        kRef[i+1] = FFS.getProb(i);
        println("prob = ${kRef[i]}");
        ++i;
    }
    UT.Timer.toc("ref, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");
    FFS.shutdown();
    
    
    // 保存数据并绘制
    k1 = k1.opt().cumprod();
    k100 = k100.opt().cumprod();
    k1000 = k1000.opt().cumprod();
    k10000 = k10000.opt().cumprod();
    kRef = kRef.opt().cumprod();
    UT.IO.table2csv(Tables.fromCols([lambda, k1, k100, k1000, k10000, kRef], 'lambda', 'all', 'gap=100', 'gap=1000', 'gap=10000', 'kRef'), '.temp/FFS6-gap.csv');
    
    def plt = Plotters.get();
    plt.semilogy(lambda, k1    , 'all'      ).marker('o');
    plt.semilogy(lambda, k100  , 'gap=100'  ).marker('s');
    plt.semilogy(lambda, k1000 , 'gap=1000' ).marker('d');
    plt.semilogy(lambda, k10000, 'gap=10000').marker('^');
    plt.semilogy(lambda, kRef  , 'reference (low noise)').marker('*');
    plt.show();
}
.finish {}

