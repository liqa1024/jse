package test

import com.jtool.code.UT
import com.jtool.rareevent.ForwardFluxSampling
import rareevent.AsymmetryWalk
import rareevent.RandomWalk


/**
 * 用来测试 FFS 准确性
 */

int N0 = 1000;


def biPathGen = new AsymmetryWalk.PathGenerator(10);
def biCal = new AsymmetryWalk.ParameterCalculator();

def FFS = new ForwardFluxSampling(biPathGen, biCal, 0, [8], N0);

UT.Timer.tic();
FFS.run();
UT.Timer.toc("0, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS = new ForwardFluxSampling(biPathGen, biCal, 0, [4, 8], N0);

UT.Timer.tic();
FFS.run();
UT.Timer.toc("1, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");


FFS = new ForwardFluxSampling(biPathGen, biCal, 0, [2, 4, 6, 8], N0);

UT.Timer.tic();
FFS.run();
UT.Timer.toc("3, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");


FFS = new ForwardFluxSampling(biPathGen, biCal, 0, [3, 5, 6, 7, 8], N0);

UT.Timer.tic();
FFS.run();
UT.Timer.toc("4, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

