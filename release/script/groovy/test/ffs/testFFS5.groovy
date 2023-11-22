package test.ffs

import jtool.code.UT
import jtool.jobs.StepJobManager
import jtool.math.table.Tables
import jtool.math.vector.Vectors
import jtool.plot.Plotters
import jtoolex.rareevent.BufferedFullPathGenerator
import jtoolex.rareevent.ForwardFluxSampling
import rareevent.NoiseClusterGrowth


/**
 * 用来测试 FFS 并行下的可重复性
 */

long seed = 123456789;
int N0 = 100;
def biCal = new NoiseClusterGrowth.ParameterCalculator();
int threadNum = 12;


def lambda = (20..200).step(5);

def biPathGen = new NoiseClusterGrowth.PathGenerator(100, 0.00045, 0.00050, 0.50, -0.10);
def FFS = new ForwardFluxSampling<>(biPathGen, biCal, threadNum, 10, lambda, N0).setMaxPathNum(N0*1000).setRNG(seed);
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

biPathGen = new NoiseClusterGrowth.PathGenerator(100, 0.00045, 0.00050, 0.10, -0.10);
FFS = new ForwardFluxSampling<>(biPathGen, biCal, threadNum, 10, lambda, N0).setMaxPathNum(N0*1000).setRNG(seed);
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

