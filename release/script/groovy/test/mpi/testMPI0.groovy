package test.mpi

import jtool.parallel.MPI

import static jtool.code.UT.Math.*;

/**
 * 测试 MPI
 */

MPI.init(args);

println(MPI.libraryVersion());

final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

double[] r1 = [rand()];
// 测试 reduce IN_PLACE
println("rand of <$me>: $r1");
MPI.Comm.WORLD.barrier();
MPI.Comm.WORLD.reduce(r1, 1, MPI.Op.MAX, 0);
println("reduced of <$me>: $r1");
MPI.Comm.WORLD.barrier();

double[] r;
if (me == 0) {
    r = new double[np];
    r[me] = rand();
} else {
    r = [rand()];
}
println("rand of <$me>: $r");
// 测试 gather IN_PLACE
MPI.Comm.WORLD.gather(r, 1, 0);
if (me == 0) {
    println("gathered rand of <$me>: $r");
    // 排序
    Arrays.sort(r);
    println("sorted rand of <$me>: $r");
}
// 测试 scatter IN_PLACE
println("rand of <$me>: $r");
MPI.Comm.WORLD.scatter(r, 1, 0);
println("scattered of <$me>: $r");

MPI.shutdown();

