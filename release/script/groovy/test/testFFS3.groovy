package test

import com.jtool.code.UT
import com.jtool.math.vector.Vectors
import com.jtool.plot.Plotters
import com.jtool.rareevent.BufferedFullPathGenerator
import com.jtool.rareevent.ForwardFluxSampling
import rareevent.ClusterGrowth


/**
 * 用来测试 FFS 准确性
 */

int N0 = 10000;


def biPathGen = new ClusterGrowth.PathGenerator(10, 0.2, 0.1);
def biCal = new ClusterGrowth.ParameterCalculator();

//def fullPath = new BufferedFullPathGenerator<>(biPathGen, biCal);
//def it = fullPath.fullPathInit();
//def sizes = Vectors.zeros(1000);
//for (i in 0..<1000) {
//    it.next();
//    sizes[i] = it.lambda();
//}
//
//plt = Plotters.get();
//plt.plot(sizes);
//plt.show();

def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (10..100).step(10), N0).setMinProb(0.0001);

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
int i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i++)}");
}
UT.Timer.toc("1, k = ${FFS.getK()}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");

FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (10..100).step(5), N0).setMinProb(0.0001);

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i++)}");
}
UT.Timer.toc("2, k = ${FFS.getK()}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");

FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (10..100).step(2), N0).setMinProb(0.0001);

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i++)}");
}
UT.Timer.toc("2, k = ${FFS.getK()}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");

FFS.shutdown();
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, 10..100, N0).setMinProb(0.0001);

UT.Timer.tic();
FFS.run();
println("k0 = ${FFS.getK0()}");
i = 0;
while (!FFS.finished()) {
    FFS.run();
    println("prob = ${FFS.getProb(i++)}");
}
UT.Timer.toc("3, k = ${FFS.getK()}, totPointNum = ${FFS.totalPointNum()}, totPathNum = ${FFS.totalPathNum()},");

FFS.shutdown();
