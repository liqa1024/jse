package test.lpc

import jse.atom.Structures
import jse.lmp.Lmpdat
import jse.lmp.NativeLmp
import jse.parallel.MPI

import static jse.code.CS.MASS

/**
 * 测试丢原子的情况
 */

// CuZr 需要开启 MANYBODY 包
NativeLmp.Conf.CMAKE_SETTING.PKG_MANYBODY = 'ON';
NativeLmp.Conf.REBUILD = false; // 如果没有这个包需要开启此选项重新构建

MPI.init();
int me = MPI.Comm.WORLD.rank();

double T = 3500;
double P = 0.0;
int seed = 4587284;

def data = Lmpdat.fromAtomData(Structures.FCC(4.100, 10).opt().mapTypeRandom(new Random(seed), 60, 40), [MASS.Cu, MASS.Zr]);

try (def lammps = new NativeLmp()) {
    lammps.command('units           metal');
    lammps.command('boundary        p p p');
    lammps.command('timestep        0.009');
    lammps.loadData(data);
    lammps.command('pair_style      eam/fs');
    lammps.command('pair_coeff      * * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr');
    lammps.command("velocity        all create $T $seed dist gaussian mom yes rot yes");
    lammps.command('thermo          1000');
    lammps.command('thermo_style    custom step temp press vol pe ke etotal');
    lammps.command('thermo_modify   flush yes');
    lammps.command("fix             1 all npt temp $T $T 4.0 iso $P $P 20.0");
    lammps.command('run             200000');
    lammps.command('write_data      lmp/.temp/data-fcc-out');
    lammps.data().write("lmp/.temp/data-fcc-out-$me");
}


MPI.shutdown();

