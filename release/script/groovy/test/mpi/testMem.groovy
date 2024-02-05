package test.mpi

import jse.code.CS
import jse.parallel.MPI

import static jse.code.UT.Math.*;

/**
 * 暴力测试 MPI 是否存在内存泄漏
 */

// 选项测试在开关 mimalloc 的情况下的内存占用
MPI.Conf.USE_MIMALLOC = true;

MPI.init(args);
final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

println("Mem of ${CS.Slurm.PROCID}/$me: ${Runtime.runtime.maxMemory()}");

while (true) {
    def r = rand(1024);
    if (np >= 2) {
        if (me == 0) MPI.Comm.WORLD.send(r.internalData(), r.size(), 1);
        if (me == 1) MPI.Comm.WORLD.recv(r.internalData(), r.size(), 0);
    }
    MPI.Comm.WORLD.bcast(r.internalData(), r.size(), 0);
}

MPI.shutdown();
