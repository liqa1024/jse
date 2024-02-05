package test.lpc

import jse.atom.Structures
import jse.code.CS
import jse.code.UT
import jse.lmp.Lmpdat
import jse.lmp.NativeLmp
import jse.parallel.MPI

import static jse.code.CS.MASS

/**
 * 测试传输 lammps 的 data 的效率
 */
// 选项测试在开关 mimalloc 的情况下的区别
MPI.Conf.USE_MIMALLOC = true;
NativeLmp.Conf.USE_MIMALLOC = true;

MPI.init(args);
final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

println("Mem of ${CS.Slurm.PROCID}/$me: ${Runtime.runtime.maxMemory()}");

def data = Lmpdat.fromAtomData(Structures.FCC(3.910, 10).opt().mapTypeRandom(new Random(UT.Code.randSeed(MPI.Comm.WORLD, 0)), 60, 40), [MASS.Cu, MASS.Zr]);

if (me == 0) UT.Timer.tic();
try (def lmp = new NativeLmp('-log', 'none', '-screen', 'none')) {
    for (_ in 1..10000) {
        lmp.command('units           metal');
        lmp.command('boundary        p p p');
        lmp.command('timestep        0.002');
        lmp.loadData(data);
        lmp.data();
        lmp.clear();
    }
}
if (me == 0) UT.Timer.toc("np = $np");
// np = 1 time: 00 hour 00 min 3.33 sec
// np = 4 time: 00 hour 00 min 10.35 sec

MPI.shutdown();
