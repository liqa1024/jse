package test.ffs

import jse.code.UT
import jsex.rareevent.ForwardFluxSampling
import rareevent.AsymmetryWalk


/**
 * 用来测试 FFS 准确性
 */

int N0 = 1000;


def biPathGen = new AsymmetryWalk.PathGenerator(10);
def biCal = new AsymmetryWalk.ParameterCalculator();

def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, [8], N0).setMinProb(0.0001);

UT.Timer.tic();
while (!FFS.finished()) FFS.run();
UT.Timer.toc("0, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, [2, 4, 8], N0).setMinProb(0.0001);

UT.Timer.tic();
while (!FFS.finished()) FFS.run();
UT.Timer.toc("1, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, [2, 4, 6, 8], N0).setMinProb(0.0001);

UT.Timer.tic();
while (!FFS.finished()) FFS.run();
UT.Timer.toc("3, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, [3, 5, 6, 7, 8], N0).setMinProb(0.0001);

UT.Timer.tic();
while (!FFS.finished()) FFS.run();
UT.Timer.toc("4, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, 1..8, N0).setMinProb(0.0001);

UT.Timer.tic();
while (!FFS.finished()) FFS.run();
UT.Timer.toc("5, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();
