package test

import com.jtool.atom.Structures
import com.jtool.lmp.Lmpdat
import com.jtool.vasp.POSCAR


/** 测试创建的结构是否正确 */

Lmpdat.fromAtomData(Structures.FCC(4.0, 4, 5, 6)).write('.temp/fcc');
Lmpdat.fromAtomData(Structures.BCC(4.0, 4, 5, 6)).write('.temp/bcc');
Lmpdat.fromAtomData(Structures.HCP(4.0, 4, 5, 6)).write('.temp/hcp');

Lmpdat.fromAtomData(Structures.FCC(4.0, 6).opt().randomUpdateTypeByWeight(3, 7)).write('.temp/alloy');

Lmpdat.fromAtomData(Structures.FCC(4.0, 6).opt().perturbG(0.2)).write('.temp/fccP');
Lmpdat.fromAtomData(Structures.BCC(4.0, 6).opt().perturbG(0.3)).write('.temp/bccP');
Lmpdat.fromAtomData(Structures.HCP(4.0, 6).opt().perturbG(0.3)).write('.temp/hcpP');

Lmpdat.fromAtomData(Structures.from(Lmpdat.read('lmp/data/data-glass'    ), 1, 2, 3)).write('.temp/glass-rep');
Lmpdat.fromAtomData(Structures.from(POSCAR.read('lmp/data/ZrCu2.poscar'), 10, 5, 10).opt().perturbG(0.3)).write('.temp/ZrCu2');

