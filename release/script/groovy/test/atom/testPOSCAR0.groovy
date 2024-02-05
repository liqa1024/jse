package test.atom

import jse.lmp.Lmpdat
import jse.vasp.POSCAR


data = Lmpdat.read('lmp/data/data-glass');

atom = data.pickAtom(10);
println(atom);

poscar = POSCAR.fromAtomData(data);
atom = poscar.pickAtom(10);
println(atom);

data = Lmpdat.fromAtomData(poscar);
atom = data.pickAtom(10);
println(atom);

