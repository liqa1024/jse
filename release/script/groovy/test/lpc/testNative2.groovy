package test.lpc

import jse.atom.Structures
import jse.lmp.Lmpdat
import jse.lmp.NativeLmp
import jse.parallel.MPI
import jse.vasp.POSCAR

import static jse.code.CS.MASS

/**
 * 测试本地原生的 lammps 不输入 in 的情况下运行，
 * 考虑 mpi 的情况
 */

// CuZr 需要开启 MANYBODY 包
NativeLmp.Conf.CMAKE_SETTING.PKG_MANYBODY = 'ON';
NativeLmp.Conf.REBUILD = false; // 如果没有这个包需要开启此选项重新构建

MPI.init();
int me = MPI.Comm.WORLD.rank();

double T = 1400;
double P = 0.0;
int seed = 4587281;

def data = Lmpdat.fromAtomData(Structures.from(POSCAR.read('vasp/data/MgCu2.poscar').setBoxScale(1.030).opt().mapType({(int)3-it.type()}), 4), [MASS.Cu, MASS.Zr]);

try (def lammps = new NativeLmp()) {
    lammps.command('units           metal');
    lammps.command('boundary        p p p');
    lammps.command('timestep        0.002');
    lammps.loadData(data);
    lammps.command('pair_style      eam/fs');
    lammps.command('pair_coeff      * * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr');
    lammps.command("velocity        all create $T $seed dist gaussian mom yes rot yes");
    lammps.command('thermo          1000');
    lammps.command('thermo_style    custom step temp press vol pe ke etotal');
    lammps.command('thermo_modify   flush yes');
    lammps.command('fix             1 all nve');
    lammps.command("fix             2 all temp/berendsen $T $T 0.2");
    lammps.command("fix             3 all press/berendsen iso $P $P 2000.0");
    lammps.command('run             50000');
    lammps.command('write_data      lmp/.temp/data-laves1-out');
    lammps.data().write("lmp/.temp/data-laves1-out-$me");
}
// Total wall time: 0:04:11 (mpi np 1)
// Total wall time: 0:01:06 (mpi np 4)
// 0:02:41

MPI.shutdown();

