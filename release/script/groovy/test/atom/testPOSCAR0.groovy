package test.atom

import jtool.atom.XYZ
import jtool.lmp.Lmpdat
import jtool.vasp.POSCAR


data = Lmpdat.read('lmp/data/data-glass');

atom = data.pickAtom(10);
println("id: ${atom.id()}, type: ${atom.type()}, xyz: ${new XYZ(atom)}");

poscar = POSCAR.fromAtomData(data);
atom = poscar.pickAtom(10);
println("id: ${atom.id()}, type: ${atom.type()}, xyz: ${new XYZ(atom)}");

data = Lmpdat.fromAtomData(poscar);
atom = data.pickAtom(10);
println("id: ${atom.id()}, type: ${atom.type()}, xyz: ${new XYZ(atom)}");

