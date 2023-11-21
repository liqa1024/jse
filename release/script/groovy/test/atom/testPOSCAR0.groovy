package test.atom

import jtool.lmp.Lmpdat
import jtool.vasp.POSCAR


data = Lmpdat.read('lmp/data/data-glass');

atom = data.pickAtom(10);
println(atom);

poscar = POSCAR.fromAtomData(data);
atom = poscar.pickAtom(10);
println(atom);

data = Lmpdat.fromAtomData(poscar);
atom = data.pickAtom(10);
println(atom);

