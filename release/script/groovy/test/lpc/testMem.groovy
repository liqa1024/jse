package test.lpc

import jtool.atom.Structures
import jtool.code.CS
import jtool.lmp.Lmpdat
import jtool.lmp.NativeLmp
import jtool.parallel.MPI

import static jtool.code.CS.MASS

/**
 * 暴力测试 NativeLmp 是否存在内存泄漏
 */

MPI.init(args);
final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

println("Mem of ${CS.Slurm.PROCID}/$me: ${Runtime.runtime.maxMemory()}");

def data = Lmpdat.fromAtomData(Structures.FCC(3.910, 10).opt().mapTypeRandom(60, 40), [MASS.Cu, MASS.Zr]);

while (true) try (def lmp = new NativeLmp('-log', 'none', '-screen', 'none')) {
    lmp.command('units           metal');
    lmp.command('boundary        p p p');
    lmp.command('timestep        0.002');
    lmp.loadData(data);
    lmp.data();
}

MPI.shutdown();
