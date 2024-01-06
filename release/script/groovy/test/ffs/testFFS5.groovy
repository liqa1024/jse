package test.ffs

import jtool.code.UT
import jtoolex.rareevent.BufferedFullPathGenerator
import jtoolex.rareevent.ForwardFluxSampling
import rareevent.NoiseClusterGrowth
/**
 * 用来测试 FFS 并行下的可重复性
 */

long seed = 123456789;
int N0 = 120;
def biCal = new NoiseClusterGrowth.ParameterCalculator();
int threadNum = 24;


def lambda = (20..200).step(5);

def biPathGen = new NoiseClusterGrowth.PathGenerator(100, 0.00045, 0.00050, 0.50, -0.10);
def fullPath = new BufferedFullPathGenerator(biPathGen, biCal);
def FFS = new ForwardFluxSampling<>(fullPath, threadNum, 10, lambda, N0).setMaxPathNum(N0*1000).setRNG(seed);
UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
def i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i)}");
    ++i;
}
UT.Timer.toc("noise, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");
FFS.shutdown();
// noise, k = 2.0999886901039358E-39, realValue = 6, totPointNum = 27026267, totPathNum = 43282, time: 00 hour 00 min 0.42 sec
// noise, k = 2.2247259920630013E-63, realValue = 7, totPointNum = 77084792, totPathNum = 203337, time: 00 hour 00 min 0.78 sec

biPathGen = new NoiseClusterGrowth.PathGenerator(100, 0.00045, 0.00050, 0.10, -0.10);
fullPath = new BufferedFullPathGenerator(biPathGen, biCal);
FFS = new ForwardFluxSampling<>(fullPath, threadNum, 10, lambda, N0).setMaxPathNum(N0*1000).setRNG(seed);
UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i)}");
    ++i;
}
UT.Timer.toc("ref, k = ${FFS.getK()}, realValue = ${FFS.pickPath().last().value}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");
FFS.shutdown();
// ref, k = 8.373320012536551E-15, realValue = 197, totPointNum = 4919191240, totPathNum = 7561, time: 00 hour 00 min 27.55 sec
// ref, k = 2.878935390905855E-15, realValue = 197, totPointNum = 5347133628, totPathNum = 7651, time: 00 hour 00 min 30.78 sec
