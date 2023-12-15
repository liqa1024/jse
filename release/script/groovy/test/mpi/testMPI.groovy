package test.mpi

import static jtool.parallel.MPI.*;
import static jtool.code.UT.Math.*;

/**
 * 测试 MPI
 */

MPI_Init(args);

int me = MPI_Comm_rank(MPI_COMM_WORLD);
int size = MPI_Comm_size(MPI_COMM_WORLD);
double r = rand();
println("rand of <$me>: $r");
double [] recv = new double[size];
MPI_Allgather([r] as double[], 1, recv, 1, MPI_COMM_WORLD);
println("recv of <$me>: $recv");

MPI_Finalize();

