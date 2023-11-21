package test.ffs

import jtool.code.UT
import jtool.math.vector.Vectors
import jtool.plot.Plotters
import jtoolex.rareevent.ForwardFluxSampling
import rareevent.RandomWalk


/**
 * 用来测试 FFS 准确性
 */

int N0 = 10000;


def biPathGen = new RandomWalk.PathGenerator(10);
def biCal = new RandomWalk.ParameterCalculator();

def lambda = 1..40;
def k1 = Vectors.zeros(lambda.size());
def k2 = Vectors.zeros(lambda.size());

def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, lambda, N0).setPruningProb(0.5).setPruningThreshold(3);
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
UT.Timer.toc("1, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, step1PathNum = ${FFS.step1PathNum()}, totPointNum = ${FFS.totalPointNum()},");
FFS.shutdown();


FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, lambda, N0);
UT.Timer.tic();
FFS.run();
k2[0] = FFS.getK0();
println("k0 = ${k2[0]}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    k2[i+1] = FFS.getProb(i);
    println("prob = ${k2[i]}");
    ++i;
}
UT.Timer.toc("2, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, step1PathNum = ${FFS.step1PathNum()}, totPointNum = ${FFS.totalPointNum()},");
FFS.shutdown();


// 绘制
k1 = k1.opt().cumprod();
k2 = k2.opt().cumprod();

def plt = Plotters.get();
plt.semilogy(lambda, k1, 'RandomWalk').marker('s');
plt.semilogy(lambda, k2, 'RandomWalk-No-Pruning').marker('o');
plt.show();
