package test.mpc

import jse.code.UT
import jse.code.collection.ArrayLists
import jse.math.vector.LogicalVector
import jse.math.vector.Vectors
import jse.plot.Plotters
import jse.vasp.XDATCAR

import static jse.code.UT.Par.*
import static jse.code.UT.Code.*

def dump = XDATCAR.read('vasp/.lll-in/XDATCAR');
final int atomNum = dump.atomNum();
final int atomTypeNum = dump.atomTypeNum();

//dump = dump[range(0, dump.size(), 20)];
// 此数据需要每帧都进行一次 wrap
for (data in dump) data.opt().wrap2this();

// 统计原子体积和配位数的变化趋势
def allAtomicVolume = ArrayLists.from(dump.size(), {Vectors.zeros(atomNum)});
def allCoordination = ArrayLists.from(dump.size(), {Vectors.zeros(atomNum)});
def neighborAtomicVolume = ArrayLists.from(dump.size(), {Vectors.zeros(atomNum)});
def neighborCoordination = ArrayLists.from(dump.size(), {Vectors.zeros(atomNum)});

UT.Timer.tic();
parfor(dump.size()) {int i ->
    try (def mpc = dump[i].getMPC()) {
        def voronoi = mpc.calVoronoi();
        def subAllAtomicVolume = allAtomicVolume[i];
        def subAllCoordination = allCoordination[i];
        subAllAtomicVolume.fill({int j -> voronoi[j].atomicVolume()});
        subAllCoordination.fill({int j -> voronoi[j].coordination()});
        // 获取近邻平均的结果
        def subNeighborAtomicVolume = neighborAtomicVolume[i];
        def subNeighborCoordination = neighborCoordination[i];
        for (int j in range(atomNum)) {
            def nl = mpc.getNeighborList(j);
            subNeighborAtomicVolume[j] =  subAllAtomicVolume[nl].mean();
            subNeighborCoordination[j] =  subAllCoordination[nl].mean();
        }
    }
}
UT.Timer.toc('voronoi');
// voronoi time: 00 hour 00 min 4.49 sec (old version)
// voronoi time: 00 hour 00 min 50.75 sec (no cache)
// voronoi time: 00 hour 00 min 4.42 sec (cached)


// 绘图
def plt1 = Plotters.get();
def plt2 = Plotters.get();
def plt3 = Plotters.get();

def data = dump[0];
def typeLegend = LogicalVector.zeros(atomTypeNum);
for (j in atomNum<..0) {
    def type = data.pickAtom(j).type();
    plt1.plot(1..dump.size(), (allAtomicVolume*.get(j)), typeLegend[type-1] ? null : "type-$type").color(type).width(type==2 ? 2.5 : 1.7);
    plt2.plot(1..dump.size(), (allCoordination*.get(j)), typeLegend[type-1] ? null : "type-$type").color(type).width(type==2 ? 2.5 : 1.7);
    plt3.plot((allCoordination*.get(j)), (allAtomicVolume*.get(j)), typeLegend[type-1] ? null : "type-$type").lineType('none').color(type).marker(type==1 ? 'o' : 's').size(type==2 ? 12 : 8);
    typeLegend[type-1] = true;
}

plt1.xrange(0, dump.size()+1);
plt2.xrange(0, dump.size()+1);
plt1.xlabel('frame').ylabel('atomicVolume');
plt2.xlabel('frame').ylabel('coordination');
plt3.xlabel('coordination').ylabel('atomicVolume');
plt1.show();
plt2.show();
plt3.show();

