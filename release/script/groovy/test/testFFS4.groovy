package test

import com.jtool.code.UT
import com.jtool.math.vector.Vectors
import com.jtool.plot.Plotters
import com.jtool.rareevent.BufferedFullPathGenerator
import com.jtool.rareevent.ForwardFluxSampling
import rareevent.ClusterGrowth


/**
 * 用来测试 FFS 保存和加载
 */

int N0 = 10000;


def biPathGen = new ClusterGrowth.PathGenerator(10, 0.2, 0.1);
def biCal = new ClusterGrowth.ParameterCalculator();



def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (10..100).step(2), N0).setMinProb(0.0001);

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i++)}");
}
UT.Timer.toc("all, k = ${FFS.getK()}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();


FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (10..50).step(2), N0).setMinProb(0.0001);

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i++)}");
}
double k = FFS.getK();
UT.Timer.toc("restart 1, k = ${k}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

UT.IO.data2csv(UT.Code.map(FFS.pointsOnLambda(), p -> p.value), '.temp/FFS-points');
UT.IO.map2json(FFS.restData(), '.temp/FFS-restdata');

FFS.shutdown();

def pointsValue = UT.IO.csv2data('.temp/FFS-points');
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (50..100).step(2), N0).setMinProb(0.0001).setStep(0, UT.Code.map(pointsValue.col(0).asList(), value -> new ClusterGrowth.Point(value as int, 0)), UT.IO.json2map('.temp/FFS-restdata'));

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    double prob = FFS.getProb(i++);
    k *= prob;
    println("prob = ${prob}");
}
UT.Timer.toc("restart 2, k = ${k}, step1PointNum = ${FFS.step1PointNum()}, totPointNum = ${FFS.totalPointNum()},");

FFS.shutdown();
