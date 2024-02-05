package test.mpi

import jse.parallel.MPI

import static jse.code.UT.Math.*;

/**
 * 测试 MPI
 */

MPI.init(args);

final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

def r = rand(10);
println("rand of <$me>: $r");
// 测试 allreduce IN_PLACE
MPI.Comm.WORLD.allreduce(r.internalData(), r.size(), MPI.Op.SUM);
println("sum of <$me>: $r");
double[] r1 = new double[np];
r1[me] = rand();
println("rand1 of <$me>: $r1");
// 测试 allgather IN_PLACE
MPI.Comm.WORLD.allgather(r1, 1);
println("rand1 of <$me>: $r1");

MPI.shutdown();

