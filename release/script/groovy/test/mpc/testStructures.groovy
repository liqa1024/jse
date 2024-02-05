package test.mpc

import jse.atom.Structures
import jse.lmp.Dump
import jse.lmp.Lmpdat
import jse.vasp.POSCAR


/** 测试创建的结构是否正确 */

Dump.fromAtomData(Structures.FCC(4.0, 4, 5, 6)).write('.temp/fcc');
Dump.fromAtomData(Structures.BCC(4.0, 4, 5, 6)).write('.temp/bcc');
Dump.fromAtomData(Structures.HCP(4.0, 4, 5, 6)).write('.temp/hcp');

Dump.fromAtomData(Structures.FCC(4.0, 6).opt().mapTypeRandom(3, 7)).write('.temp/alloy');

Dump.fromAtomData(Structures.FCC(4.0, 6).opt().perturbXYZ(0.2)).write('.temp/fccP');
Dump.fromAtomData(Structures.BCC(4.0, 6).opt().perturbXYZ(0.3)).write('.temp/bccP');
Dump.fromAtomData(Structures.HCP(4.0, 6).opt().perturbXYZ(0.3)).write('.temp/hcpP');

Dump.fromAtomData(Structures.from(Lmpdat.read('lmp/data/data-glass'    ), 1, 2, 3)).write('.temp/glass-rep');
Dump.fromAtomData(Structures.from(POSCAR.read('lmp/data/ZrCu2.poscar'), 10, 5, 10).opt().perturbXYZ(0.3)).write('.temp/ZrCu2');

