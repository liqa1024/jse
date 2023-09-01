package atom

import com.jtool.lmp.Dump
import com.jtool.rareevent.atom.ABOOPSolidChecker
import com.jtool.rareevent.atom.MainTypeClusterSizeCalculator

import java.util.stream.Collectors



//final def dataDir       = 'lmp/.stableglass-in/';
//final def filterDir     = 'lmp/.stableglass-out/';
//final def checker = new ABOOPSolidChecker().setRNearestQMul(4.0).setConnectThreshold(0.950).setSolidThreshold(7);
//
///** 读取 data 并过滤 */
//for (fileName in UT.IO.list(dataDir)) {
//    def data = Lmpdat.read(dataDir+fileName);
//    def isSolid = data.getMPC().withCloseable {checker.checkSolid(it)}
//
//    println("solid atom number of ${fileName}: ${isSolid.count()}");
//    int i = 0;
//    Lmpdat.fromAtomData(data.opt().collect {IAtom atom -> isSolid[i++] ? new Atom(atom).setType(atom.type()+ 3) : atom}).write(filterDir+ 'filter-'+ fileName);
//}
final def filterDir = 'lmp/.stableglass-out/';

final def dump = Dump.read('lmp/.stableglass-in/dump');
final def calculator = new MainTypeClusterSizeCalculator(
    new ABOOPSolidChecker().setRNearestMul(2.2).setConnectThreshold(0.83).setSolidThreshold(25),
    new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7),
    2
);

def filterDump = dump.parallelStream().map {subDump ->
    def isSolid = subDump.getMPC().withCloseable {calculator.getIsSolid_(it, subDump)}
    int j = 0;
    subDump.opt().mapType {def atom -> isSolid[j++] ? atom.type()+2 : atom.type()};
}.collect(Collectors.toList());

Dump.fromAtomDataList(filterDump).write(filterDir+'filter-dump');

