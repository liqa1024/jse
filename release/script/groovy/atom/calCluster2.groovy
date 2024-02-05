package atom

import jse.code.UT
import jse.lmp.Dump
import jsex.rareevent.atom.ABOOPSolidChecker
import jsex.rareevent.atom.MultiTypeClusterSizeCalculator

import static jse.code.UT.Par.parfor


final def dump = Dump.read('lmp/.stableglass-in/dump-fs2');
final def calculator = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker().setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);

UT.Timer.tic();
UT.Timer.pbar(dump.size());
parfor(dump.size()) {int i ->
    def subDump = dump[i];
    def isSolid = subDump.getMPC().withCloseable {calculator.getIsSolid_(it, subDump)}
    // 现在可以利用 asTable 来直接增加一列
    subDump.asTable()['issolid'] = isSolid.asVec();
    UT.Timer.pbar();
}
UT.Timer.toc();
// Total time: 00 hour 02 min 9.88 sec (jdk8, 6g, no cache)
// Total time: 00 hour 02 min 13.37 sec (jdk8, 6g, cached)
// Total time: 00 hour 02 min 51.97 sec (jdk17, 4g, no cache)
// Total time: 00 hour 02 min 37.83 sec (jdk17, 4g, cached)
// Total time: 00 hour 02 min 8.19 sec (jdk17, cached)

dump.write('lmp/.stableglass-out/filter2-dump-fs2');

