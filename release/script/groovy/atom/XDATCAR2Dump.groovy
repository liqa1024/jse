package atom

import jse.lmp.Dump
import jse.vasp.XDATCAR


def data = XDATCAR.read('lmp/.lll-in/XDATCAR');

println(data.atomNum('Ce'));

data.write('lmp/.lll-out/XDATCAR');
Dump.fromAtomDataList(data).write('lmp/.lll-out/dump');

