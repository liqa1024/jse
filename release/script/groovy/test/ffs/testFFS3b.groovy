package test.ffs

import jtool.code.UT
import jtool.math.vector.Vectors
import jtool.plot.Plotters
import jtoolex.rareevent.BufferedFullPathGenerator
import jtoolex.rareevent.ForwardFluxSampling
import rareevent.BiClusterGrowth


/**
 * 用来测试 FFS 准确性
 */

int N0 = 5000;


def biPathGen = new BiClusterGrowth.PathGenerator(10, 0.4, 0.5, 0.10);
def biCal = new BiClusterGrowth.ParameterCalculator();

//def fullPath = new BufferedFullPathGenerator<>(biPathGen, biCal);
//def pi = fullPath.fullPathInit();
//def sizes = Vectors.zeros(100000);
//def sizesPre = Vectors.zeros(100000);
//for (i in 0..<sizes.size()) {
//    def p = pi.next();
//    sizes[i] = p.value;
//    sizesPre[i] = p.valuePre;
//}
//
//plt = Plotters.get();
//plt.plot(sizesPre, 'invisible').type('..').color('r').size(0.5);
//plt.plot(sizes, 'lambda').color('k');
//plt.axis(0, sizes.size(), 0, 100);
//plt.show();

def FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (5..105).step(5), N0).setPruningProb(0.5);

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
FFS = new ForwardFluxSampling<>(biPathGen, biCal, 0, (5..105).step(5), N0*10).setPruningProb(0.5);

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

