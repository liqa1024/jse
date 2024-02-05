package test.mpi

import jse.parallel.MPI

import static jse.code.UT.Code.range;
import static jse.code.UT.Math.*;

/**
 * 测试 MPI 的 Comm 拆分功能
 */

MPI.init(args);

final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

final int color = me.intdiv(4);

try (def subComm = MPI.Comm.WORLD.split(color)) {
    final int subme = subComm.rank();
    final int subnp = subComm.size();
    println("WORLD RANK/SIZE: $me/$np \t ROW RANK/SIZE: $subme/$subnp \t COLOR: $color");
}

MPI.shutdown();

