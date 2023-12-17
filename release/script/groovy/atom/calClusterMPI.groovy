package atom

import jtool.atom.IAtomData
import jtool.code.UT
import jtool.lmp.Dump
import jtool.math.vector.LogicalVector
import jtool.parallel.MPI
import jtoolex.rareevent.atom.ABOOPSolidChecker
import jtoolex.rareevent.atom.MultiTypeClusterSizeCalculator

import static jtool.code.UT.Code.range

/**
 * 使用 MPI 并行计算
 */

MPI.init(args);

final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

Dump dump = null;
final int[] size = [-1];
if (me == 0) {
    // 只需要主线程读取
    dump = Dump.read('lmp/.stableglass-in/dump-fs2');
    size[0] = dump.size();
}
// 将数目传给其他进程
MPI.Comm.WORLD.bcast(size, 1, 0);

final def calculator = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker().setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);

if (me == 0) UT.Timer.tic();
if (me == 0) UT.Timer.pbar(size[0]);
for (i in range(me, size[0], np)) {
    IAtomData atomData;
    if (me == 0) {
        atomData = dump[i];
        for (others in range(1, np)) if (i+others < size[0]) {
            def bytes = UT.Serial.atomDataXYZType2bytes(dump[i+others]);
            MPI.Comm.WORLD.send([bytes.size()] as int[], 1, others);
            MPI.Comm.WORLD.send(bytes, bytes.size(), others);
        }
    } else {
        int[] bytesSize = [-1];
        MPI.Comm.WORLD.recv(bytesSize, 1, 0);
        def bytes = new byte[bytesSize[0]];
        MPI.Comm.WORLD.recv(bytes, bytes.size(), 0);
        atomData = UT.Serial.bytes2atomDataXYZType(bytes);
    }
    def isSolid = atomData.getMPC().withCloseable {calculator.getIsSolid_(it, atomData)}
    if (me == 0) {
        // 现在可以利用 asTable 来直接增加一列
        dump[i].asTable()['issolid'] = isSolid.asVec();
        UT.Timer.pbar();
        def isSolidOthers = new boolean[isSolid.size()];
        for (others in range(1, np)) if (i+others < size[0]) {
            MPI.Comm.WORLD.recv(isSolidOthers, isSolidOthers.size(), others);
            dump[i+others].asTable()['issolid'] = new LogicalVector(isSolidOthers).asVec();
            UT.Timer.pbar();
        }
    } else {
        MPI.Comm.WORLD.send(isSolid.data(), isSolid.size(), 0);
    }
}
if (me == 0) UT.Timer.toc("MPI np $np");
// MPI np 12 time: 00 hour 03 min 29.75 sec
// MPI np 24 time: 00 hour 02 min 30.49 sec

if (me == 0) dump.write('lmp/.stableglass-out/filter2-dump-fs2');

MPI.shutdown();
