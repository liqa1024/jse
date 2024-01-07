package test.lpc

import jtool.atom.Structures
import jtool.lmp.LmpIn
import jtool.lmp.Lmpdat
import jtool.lmp.NativeLmp
import jtool.parallel.MPI
import jtool.vasp.POSCAR

import static jtool.code.CS.MASS

/**
 * 测试本地原生的 lammps 运行具体的 in 文件，
 * 考虑 mpi 的情况
 */

// CuZr 需要开启 MANYBODY 包
NativeLmp.Conf.CMAKE_SETTING.PKG_MANYBODY = 'ON';
NativeLmp.Conf.REBUILD = false; // 如果没有这个包需要开启此选项重新构建

MPI.init();
int me = MPI.Comm.WORLD.rank();

Lmpdat.fromAtomData(Structures.from(POSCAR.read('vasp/data/MgCu2.poscar').setBoxScale(1.030).opt().mapType({(int)3-it.type()}), 4),
                    [MASS.Cu, MASS.Zr]).write('lmp/.temp/data-laves1');
def inMelt = LmpIn.DATA2DATA_MELT_NPT_Cu();
inMelt.vInDataPath = 'lmp/.temp/data-laves1';
inMelt.vOutDataPath = 'lmp/.temp/data-laves1-out';
inMelt.vT = 1400;
inMelt.pair_style = 'eam/fs';
inMelt.pair_coeff = '* * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr';

try (def lammps = new NativeLmp()) {
    lammps.file(inMelt);
    lammps.lmpdat().write("lmp/.temp/data-laves1-out-$me");
}
// Total wall time: 0:04:11 (mpi np 1)
// Total wall time: 0:01:10 (mpi np 4)

MPI.shutdown();

