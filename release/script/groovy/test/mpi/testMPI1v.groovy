package test.mpi

import jse.math.vector.Vector
import jse.parallel.MPI

import static jse.code.UT.Code.range
import static jse.code.UT.Code.range
import static jse.code.UT.Code.range
import static jse.code.UT.Code.range
import static jse.code.UT.Math.*;

/**
 * 测试 MPI
 */

MPI.init(args);

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

def r = zeros(size);
for (i in range(displs[me], counts[me]+displs[me])) r[i] = rand();
println("rand of <$me>: $r");

// 测试 allgatherv IN_PLACE
MPI.Comm.WORLD.allgatherv(r.internalData(), counts, displs);
println("allgathered rand of <$me>: $r");

MPI.shutdown();

