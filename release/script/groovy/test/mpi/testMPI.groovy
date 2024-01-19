package test.mpi

import jtool.parallel.MPI
import static jtool.code.UT.Math.*;

MPI.init();
int me = MPI.Comm.WORLD.rank();

double[] r = [0.0];
if (me == 0) r[0] = rand();
println("rand of <$me>: $r");

MPI.Comm.WORLD.bcast(r, 1, 0);
println("bcast rand of <$me>: $r");

MPI.shutdown();

