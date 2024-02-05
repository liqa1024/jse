package test.lpc

import jse.atom.Structures
import jse.code.CS
import jse.lmp.Lmpdat
import jse.lmp.NativeLmp
import jse.parallel.MPI

import static jse.code.CS.MASS

/**
 * 暴力测试 NativeLmp 是否存在内存泄漏
 */
// 选项测试在开关 mimalloc 的情况下的内存占用
MPI.Conf.USE_MIMALLOC = true;
NativeLmp.Conf.USE_MIMALLOC = true;

MPI.init(args);
final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

println("Mem of ${CS.Slurm.PROCID}/$me: ${Runtime.runtime.maxMemory()}");

def data = Lmpdat.fromAtomData(Structures.FCC(3.910, 10).opt().mapTypeRandom(60, 40), [MASS.Cu, MASS.Zr]);

try (def lmp = new NativeLmp('-log', 'none', '-screen', 'none')) {
    while (true) {
        lmp.command('units           metal');
        lmp.command('boundary        p p p');
        lmp.command('timestep        0.002');
        lmp.loadData(data);
        lmp.data();
        lmp.clear();
    }
}

MPI.shutdown();
