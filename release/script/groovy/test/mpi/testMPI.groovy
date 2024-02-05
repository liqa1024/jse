package test.mpi

import jse.parallel.MPI
import static jse.code.UT.Math.*;

MPI.init();
int me = MPI.Comm.WORLD.rank();

byte[] r = [0, 0, 0];
if (me == 0) {
    r[0] = (byte)randi(127);
    r[1] = (byte)randi(127);
    r[2] = (byte)randi(127);
}
println("rand of <$me>: $r");

MPI.Comm.WORLD.bcast(r, r.size(), 0);
println("bcast rand of <$me>: $r");

MPI.shutdown();

