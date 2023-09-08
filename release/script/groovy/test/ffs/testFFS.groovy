package test.ffs

import com.jtool.code.UT
import com.jtool.rareevent.ForwardFluxSampling
import rareevent.RandomWalk


/**
 * 用来测试 FFS 准确性
 */

int N0 = 10000;


def biPathGen = new RandomWalk.PathGenerator(10);
def biCal = new RandomWalk.ParameterCalculator();

//def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, [10], N0).setMinProb(0.0001);
//
//UT.Timer.tic();
//while (!FFS.finished()) FFS.run();
//UT.Timer.toc("0, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");
//
//FFS.shutdown();
//FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, [5, 10], N0).setMinProb(0.0001);
//
//UT.Timer.tic();
//while (!FFS.finished()) FFS.run();
//UT.Timer.toc("1, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");
//
//FFS.shutdown();
//FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, [2, 4, 6, 8, 10], N0).setMinProb(0.0001);
//
//UT.Timer.tic();
//while (!FFS.finished()) FFS.run();
//UT.Timer.toc("3, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");
//
//FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, 1..10, N0).setPruningProb(0.3).setPruningThreshold(2);

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
int i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i++)}");
}
UT.Timer.toc("4, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, step1PathNum = ${FFS.step1PathNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();
