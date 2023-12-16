package test.mpi

import jtool.parallel.MPI;

import static jtool.code.UT.Math.*;

/**
 * 测试 MPI
 */

MPI.init(args);

int me = MPI.Comm.WORLD.rank();
double[] r = [rand()];
println("rand of <$me>: $r");
if (me == 0) {
    double[] sum = new double[1];
    double[] max = new double[1];
    MPI.Comm.WORLD.reduce(r, sum, 1, MPI.Op.SUM, 0);
    MPI.Comm.WORLD.reduce(r, max, 1, MPI.Op.MAX, 0);
    println("sum of <$me>: $sum");
    println("max of <$me>: $max");
} else {
    MPI.Comm.WORLD.reduce(r, null, 1, MPI.Op.SUM, 0);
    MPI.Comm.WORLD.reduce(r, null, 1, MPI.Op.MAX, 0);
}

MPI.shutdown();

