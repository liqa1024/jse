package test.mpc

import jtool.code.UT
import jtool.code.collection.ArrayLists
import jtool.math.vector.LogicalVector
import jtool.math.vector.Vectors
import jtool.vasp.XDATCAR

import static jtool.code.UT.Par.*
import static jtool.code.UT.Code.*
import static jtool.code.UT.Plot.*

def dump = XDATCAR.read('lmp/.lll-in/XDATCAR');
// 此数据需要每帧都进行一次 wrap
for (data in dump) data.opt().wrap2this();

// 统计原子体积变化趋势
def allAtomicVolume = ArrayLists.from(dump.size(), {Vectors.zeros(dump.atomNum())})

// 借助一下 MFPC 将原子按照相同的 id 排序；虽然对于 XDATCAR 是不必要的
UT.Timer.tic();
try (def mfpc = dump.getMFPC(0.001)) {
    parfor(dump.size()) {int i ->
        try (def mpc = mfpc.getMPC(i)) {
            def voronoi = mpc.calVoronoi();
            allAtomicVolume[i].fill({int j -> voronoi[j].atomicVolume()});
        }
    }
}
UT.Timer.toc('voronoi');


// 绘图
def data = dump[0];
def typeLegend = LogicalVector.zeros(data.atomTypeNum());
for (j in range(data.atomNum())) {
    def type = data.pickAtom(j).type();
    plot(1..dump.size(), (allAtomicVolume*.get(j)), typeLegend[type-1] ? null : "type-$type").color(type);
    typeLegend[type-1] = true;
}

xrange(0, dump.size()+1);
