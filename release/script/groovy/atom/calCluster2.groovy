package atom

import jtool.code.UT
import jtool.lmp.Dump
import jtoolex.rareevent.atom.ABOOPSolidChecker
import jtoolex.rareevent.atom.MultiTypeClusterSizeCalculator

import static jtool.code.UT.Par.parfor


final def dump = Dump.read('lmp/.ffs-in/dump-NiAl');
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

dump.write('lmp/.ffs-out/filter2-dump-NiAl');

