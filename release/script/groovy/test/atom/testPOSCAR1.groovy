package test.atom

import jtool.atom.Structures
import jtool.lmp.Lmpdat
import jtool.vasp.POSCAR


data = Structures.FCC(4.0, 8, 2, 1).opt().mapType {(it.index().intdiv(8)+1)};

poscar = POSCAR.fromAtomData(data);
poscar.write('lmp/.temp/0.poscar');
Lmpdat.fromAtomData(poscar).write('lmp/.temp/0.lmpdat');

println('10 -> '+poscar.pickAtom(10).setType(5).index());
poscar.write('lmp/.temp/1.poscar');
Lmpdat.fromAtomData(poscar).write('lmp/.temp/1.lmpdat');


println('50 -> '+poscar.pickAtom(50).setType(2).index());
poscar.write('lmp/.temp/2.poscar');
Lmpdat.fromAtomData(poscar).write('lmp/.temp/2.lmpdat');

