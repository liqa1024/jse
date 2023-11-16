package test.mpc

import jtool.code.UT
import jtool.code.collection.ArrayLists
import jtool.math.vector.Vectors
import jtool.plot.Plotters
import jtool.vasp.XDATCAR

import static jtool.code.UT.Par.*
import static jtool.code.UT.Code.*

def dump = XDATCAR.read('lmp/.lll-in/XDATCAR');
final int atomNum = dump.atomNum();

//dump = dump[range(0, dump.size(), 20)];
// 此数据需要每帧都进行一次 wrap
for (data in dump) data.opt().wrap2this();

// 统计原子体积和配位数的变化趋势
def allAtomicVolume = ArrayLists.from(dump.size(), {Vectors.zeros(atomNum)});
def allCoordination = ArrayLists.from(dump.size(), {Vectors.zeros(atomNum)});

UT.Timer.tic();
parfor(dump.size()) {int i ->
    try (def mpc = dump[i].getMPC()) {
        def voronoi = mpc.calVoronoi();
        def subAllAtomicVolume = allAtomicVolume[i];
        def subAllCoordination = allCoordination[i];
        subAllAtomicVolume.fill({int j -> voronoi[j].atomicVolume()});
        subAllCoordination.fill({int j -> voronoi[j].coordination()});
    }
}
UT.Timer.toc('voronoi');

// 直接对时间进行平均
def meanAtomicVolume = Vectors.zeros(atomNum);
def meanCoordination = Vectors.zeros(atomNum);
for (i in range(dump.size())) {
    meanAtomicVolume.plus2this(allAtomicVolume[i]);
    meanCoordination.plus2this(allCoordination[i]);
}
meanAtomicVolume.div2this(dump.size());
meanCoordination.div2this(dump.size());


// 绘图
def plt = Plotters.get();
def type = Vectors.from((dump[0].asList()*.type()));
plt.plot(meanCoordination[type.equal(2)], meanAtomicVolume[type.equal(2)], 'type-2').color(2).lineType('none').marker('o');
plt.plot(meanCoordination[type.equal(1)], meanAtomicVolume[type.equal(1)], 'type-1').color(1).lineType('none').marker('s');
plt.xlabel('coordination').ylabel('atomicVolume');
plt.show();

meanAtomicVolume1 = meanAtomicVolume[type.equal(1)];
meanCoordination1 = meanCoordination[type.equal(1)];
for (i in 0..<meanAtomicVolume1.size()) if (meanAtomicVolume1[i] < 28.5 && meanCoordination1[i] > 14.5) {
    println("i = $i, meanAtomicVolume = ${meanAtomicVolume1[i]}, meanCoordination = ${meanCoordination1[i]}");
}

