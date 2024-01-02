package test.mpi

import jtool.math.vector.DoubleArrayVector
import jtool.parallel.MPI

import static jtool.code.UT.Math.*;

/**
 * 测试 MPI
 */

MPI.init(args);

final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

def r = rand(10);
println("rand of <$me>: $r");
MPI.Comm.WORLD.allreduce(((DoubleArrayVector)r).getData(), r.size(), MPI.Op.SUM);
println("sum of <$me>: $r");

MPI.shutdown();

