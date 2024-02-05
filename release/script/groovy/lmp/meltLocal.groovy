package lmp

import jse.atom.Structures
import jse.lmp.LPC
import jse.lmp.Lmpdat
import jse.system.WSL
import jse.vasp.POSCAR

import static jse.code.CS.MASS


/**
 * 直接本地跑熔融，快速得到此温度下的结构
 */

final int temp = 1400;

//final def dataIn = 'lmp/.ffs-in/data-fs1-init'
//final def dataOut = "lmp/.CuZr/data-nolaves-$temp";

final def dataIn = Lmpdat.fromAtomData(Structures.from(POSCAR.read('vasp/data/MgCu2.poscar').setBoxScale(1.030).opt().mapType({(int)3-it.type()}), 6), [MASS.Cu, MASS.Zr]);
final def dataOut = "lmp/.CuZr/data-laves1-$temp";

//final def dataIn = Lmpdat.fromAtomData(Structures.from(POSCAR.read('vasp/data/re_MgNi2.poscar').setBoxScale(1.065).opt().mapType({(int)3-it.type()}), 4), [MASS.Cu, MASS.Zr]);
//final def dataOut = "lmp/.CuZr/data-laves2-$temp";

//final def dataIn = Lmpdat.fromAtomData(Structures.from(POSCAR.read('vasp/data/re_MgZn2.poscar').setBoxScale(0.985).opt().mapType({(int)3-it.type()}), 6), [MASS.Cu, MASS.Zr]);
//final def dataOut = "lmp/.CuZr/data-laves3-$temp";


final def lmpExe = '~/.local/bin/lmp';
final int lmpCores = 12;

try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr')) {
    lpc.runMelt(dataIn, dataOut, temp);
}

