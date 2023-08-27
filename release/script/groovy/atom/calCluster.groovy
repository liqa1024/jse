package atom

import com.jtool.atom.Atom
import com.jtool.atom.IAtom
import com.jtool.code.UT
import com.jtool.lmp.Lmpdat
import com.jtool.rareevent.atom.MultiTypeClusterSizeCalculator

final def dataDir       = 'lmp/.lhr-in/';
final def filterDir     = 'lmp/.lhr-out/';
final def cal = new MultiTypeClusterSizeCalculator().setQ6CutoffMul(2.20).setConnectThreshold(0.88).setSolidThreshold(13);

/** 读取 data 并过滤 */
for (fileName in UT.IO.list(dataDir)) {
    def data = Lmpdat.read(dataDir+fileName);
    def isSolid = data.getMPC().withCloseable {cal.getIsSolid_(it, data)}
    
    println("solid atom number of ${fileName}: ${isSolid.count()}");
    int i = 0;
    Lmpdat.fromAtomData(data.opt().collect {IAtom atom -> isSolid[i++] ? new Atom(atom).setType(atom.type()+ 3) : atom}).write(filterDir+ 'filter-'+ fileName);
}

