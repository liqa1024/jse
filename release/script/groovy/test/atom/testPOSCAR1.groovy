package test.atom

import jse.atom.Structures
import jse.lmp.Lmpdat
import jse.vasp.POSCAR


data = Structures.FCC(4.0, 8, 2, 1).opt().mapType {(it.index().intdiv(8)+1)};

poscar = POSCAR.fromAtomData(data);
poscar.write('lmp/.temp/0.poscar');
Lmpdat.fromAtomData(poscar).write('lmp/.temp/0.lmpdat');

atom = poscar.pickAtom(10);
println("10 -> ${atom.setType(5).index()}; atom: ${atom}");
poscar.write('lmp/.temp/1.poscar');
Lmpdat.fromAtomData(poscar).write('lmp/.temp/1.lmpdat');


atom = poscar.pickAtom(50);
println("50 -> ${atom.setType(2).index()}; atom: ${atom}");
poscar.write('lmp/.temp/2.poscar');
Lmpdat.fromAtomData(poscar).write('lmp/.temp/2.lmpdat');

