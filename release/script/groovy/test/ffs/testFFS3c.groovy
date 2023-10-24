package test.ffs

import jtool.code.UT
import jtool.jobs.StepJobManager
import jtool.math.table.Tables
import jtool.math.vector.Vectors
import jtool.plot.Plotters
import jtool.rareevent.BufferedFullPathGenerator
import jtool.rareevent.ForwardFluxSampling
import rareevent.NoiseClusterGrowth


/**
 * 用来测试 FFS 准确性
 */

int N0 = 100;
def biCal = new NoiseClusterGrowth.ParameterCalculator();

new StepJobManager('testFFS3c', 2)
.init {println("0. 绘制 lambda 随时间变化曲线");}
.doJob {
    def biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 500, 40);
    
    def fullPath = new BufferedFullPathGenerator<>(biPathGen, biCal);
    def pi = fullPath.fullPathInit();
    
    def sizes = Vectors.zeros(2000);
    def sizesReal = Vectors.zeros(2000);
    for (i in 0..<sizes.size()) {
        def p = pi.next();
        sizes[i] = pi.lambda();
        sizesReal[i] = p.value;
    }
    
    def step = Vectors.linsequence(0, biPathGen.skipNum, sizes.size())
    
    def plt = Plotters.get();
    plt.plot(step, sizes    , 'lambda').color(0);
    plt.plot(step, sizesReal, 'real'  ).color(1).width(5.0);
    plt.xRange(0, step.last()+1);
    plt.xlabel('step').ylabel('value');
    plt.show();
    
    step      = step     [(0..<step     .size()).step(10)];
    sizes     = sizes    [(0..<sizes    .size()).step(10)];
    sizesReal = sizesReal[(0..<sizesReal.size()).step(10)];
    
    def plt2 = Plotters.get();
    plt2.plot(step, sizes    , 'lambda').color(0);
    plt2.plot(step, sizesReal, 'real'  ).color(1).width(5.0);
    plt2.xRange(0, step.last()+1);
    plt2.xlabel('step').ylabel('value');
    plt2.show();
}
.then {println("1. 直接 FFS 并绘制不同噪音情况的结果");}
.doJob {
    def lambda = (20..200).step(5);
    def kNoise = Vectors.zeros(lambda.size());
    def kRef = Vectors.zeros(lambda.size());
    
    def biPathGen = new NoiseClusterGrowth.PathGenerator(10, 0.00050, 0.00050, 0.50, -0.10);
    def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setMaxPathNum(N0*1000);
    UT.Timer.tic();
    FFS.run();
    kNoise[0] = FFS.getK0();
    println("k0 = ${kNoise[0]}");
    def i = 0;
    while (!FFS.finished()) {
        FFS.run();
        kNoise[i+1] = FFS.getProb(i);
        println("prob = ${kNoise[i]}");
        ++i;
    }
    UT.Timer.toc("noise, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(10, 0.00050, 0.00050, 0.10, -0.10);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setMaxPathNum(N0*1000);
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
    UT.Timer.toc("ref, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    
    // 保存数据并绘制
    kNoise = kNoise.opt().cumprod();
    kRef = kRef.opt().cumprod();
    UT.IO.table2csv(Tables.fromCols([lambda, kNoise, kRef], 'lambda', 'kNoise', 'kRef'), '.temp/FFS3c-noise.csv');
    
    def plt = Plotters.get();
    plt.semilogy(lambda, kNoise, 'high noise');
    plt.semilogy(lambda, kRef  , 'low noise (reference)');
    plt.show();
}
.then {println("2. FFS 并绘制不同间距的结果");}
.doJob {
    def lambda = (20..200).step(5);
    def k1 = Vectors.zeros(lambda.size());
    def k100 = Vectors.zeros(lambda.size());
    def k1000 = Vectors.zeros(lambda.size());
    def k10000 = Vectors.zeros(lambda.size());
    def kRef = Vectors.zeros(lambda.size());
    
    def biPathGen = new NoiseClusterGrowth.PathGenerator(10, 0.00050, 0.00050, 0.50, -0.10);
    def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*100000);
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
    UT.Timer.toc("1, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 100);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*100000);
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
    UT.Timer.toc("100, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 1000);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*1000);
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
    UT.Timer.toc("1000, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 10000);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*1000);
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
    UT.Timer.toc("10000, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(10, 0.00050, 0.00050, 0.10, -0.10);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setMaxPathNum(N0*1000);
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
    UT.Timer.toc("ref, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    
    // 保存数据并绘制
    k1 = k1.opt().cumprod();
    k100 = k100.opt().cumprod();
    k1000 = k1000.opt().cumprod();
    k10000 = k10000.opt().cumprod();
    kRef = kRef.opt().cumprod();
    UT.IO.table2csv(Tables.fromCols([lambda, k1, k100, k1000, k10000, kRef], 'lambda', 'all', 'gap=100', 'gap=1000', 'gap=10000', 'kRef'), '.temp/FFS3c-gap.csv');
    
    def plt = Plotters.get();
    plt.semilogy(lambda, k1    , 'all'      ).marker('o');
    plt.semilogy(lambda, k100  , 'gap=100'  ).marker('s');
    plt.semilogy(lambda, k1000 , 'gap=1000' ).marker('d');
    plt.semilogy(lambda, k10000, 'gap=10000').marker('^');
    plt.semilogy(lambda, kRef  , 'reference (low noise)').marker('*');
    plt.show();
}
.then {println("3. FFS 并绘制不同剪枝之间的结果");}
.doJob {
//    def lambda = [20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 180, 200];
    def lambda = (20..200).step(5);
    def k3 = Vectors.zeros(lambda.size());
    def k5 = Vectors.zeros(lambda.size());
    def k8 = Vectors.zeros(lambda.size());
    def kNone = Vectors.zeros(lambda.size());
    
    def biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 5000);
    def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(3).setMaxPathNum(N0*1000);
    UT.Timer.tic();
    FFS.run();
    k3[0] = FFS.getK0();
    println("k0 = ${k3[0]}");
    def i = 0;
    while (!FFS.finished()) {
        FFS.run();
        k3[i+1] = FFS.getProb(i);
        println("prob = ${k3[i]}");
        ++i;
    }
    UT.Timer.toc("3, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 5000);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(5).setMaxPathNum(N0*1000);
    UT.Timer.tic();
    FFS.run();
    k5[0] = FFS.getK0();
    println("k0 = ${k5[0]}");
    i = 0;
    while (!FFS.finished()) {
        FFS.run();
        k5[i+1] = FFS.getProb(i);
        println("prob = ${k5[i]}");
        ++i;
    }
    UT.Timer.toc("5, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 5000);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setPruningProb(0.5).setPruningThreshold(8).setMaxPathNum(N0*1000);
    UT.Timer.tic();
    FFS.run();
    k8[0] = FFS.getK0();
    println("k0 = ${k8[0]}");
    i = 0;
    while (!FFS.finished()) {
        FFS.run();
        k8[i+1] = FFS.getProb(i);
        println("prob = ${k8[i]}");
        ++i;
    }
    UT.Timer.toc("8, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    biPathGen = new NoiseClusterGrowth.PathGenerator(2, 0.00050, 0.00050, 0.50, -0.10, 5000);
    FFS = new ForwardFluxSampling<>(biPathGen, biCal, 10, lambda, N0).setMaxPathNum(N0*1000);
    UT.Timer.tic();
    FFS.run();
    kNone[0] = FFS.getK0();
    println("k0 = ${kNone[0]}");
    i = 0;
    while (!FFS.finished()) {
        FFS.run();
        kNone[i+1] = FFS.getProb(i);
        println("prob = ${kNone[i]}");
        ++i;
    }
    UT.Timer.toc("none, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value},");
    FFS.shutdown();
    
    
    
    // 保存数据并绘制
    k3 = k3.opt().cumprod();
    k5 = k5.opt().cumprod();
    k8 = k8.opt().cumprod();
    kNone = kNone.opt().cumprod();
    UT.IO.table2csv(Tables.fromCols([lambda, k3, k5, k8, kNone], 'lambda', 'pruning=3', 'pruning=5', 'pruning=8', 'no-pruning'), '.temp/FFS3c-pruning.csv');
    
    def plt = Plotters.get();
    plt.semilogy(lambda, k3   , 'pruning=3').marker('o');
    plt.semilogy(lambda, k5   , 'pruning=5').marker('d');
    plt.semilogy(lambda, k8   , 'pruning=8').marker('s');
    plt.semilogy(lambda, kNone, 'no-pruning').marker('^');
    plt.show();
}
.finish {}

