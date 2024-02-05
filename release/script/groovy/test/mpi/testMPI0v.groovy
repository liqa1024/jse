package test.mpi

import jse.math.vector.Vector
import jse.parallel.MPI

import static jse.code.UT.Math.*;
import static jse.code.UT.Code.range

/**
 * 测试 MPI
 */

MPI.init(args);

println(MPI.libraryVersion());

final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

int size = 10;

int[] counts = new int[np];
int part = size.intdiv(np);
int rest = size % np;
for (i in range(np)) counts[i] = part;
counts[-1] += rest;

int[] displs = new int[np];
for (i in range(np-1)) displs[i+1] = displs[i] + counts[i];

Vector r;
if (me == 0) {
    r = zeros(size);
    for (i in range(displs[me], counts[me]+displs[me])) r[i] = rand();
} else {
    r = zeros(counts[me]);
    for (i in range(counts[me])) r[i] = rand();
}
println("rand of <$me>: $r");

MPI.Comm.WORLD.barrier();
if (me == 0) println("==========gatherv IN_PLACE==========");
MPI.Comm.WORLD.barrier();
MPI.Comm.WORLD.gatherv(r.internalData(), counts, displs, 0);
if (me == 0) {
    println("gathered rand of <$me>: $r");
    // 排序
    Arrays.sort(r.internalData());
    println("sorted rand of <$me>: $r");
}
MPI.Comm.WORLD.barrier();
if (me == 0) println("==========scatterv IN_PLACE==========");
MPI.Comm.WORLD.barrier();
println("rand of <$me>: $r");
MPI.Comm.WORLD.scatterv(r.internalData(), counts, displs, 0);
println("scattered rand of <$me>: $r");

sleep(100);

MPI.shutdown();

