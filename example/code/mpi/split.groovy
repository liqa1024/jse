package code.mpi

import jse.parallel.MPI


MPI.init()

int me = MPI.Comm.WORLD.rank()
int np = MPI.Comm.WORLD.size()

int color = me.intdiv(3)

try (def subComm = MPI.Comm.WORLD.split(color)) {
    int subme = subComm.rank()
    int subnp = subComm.size()
    println("WORLD RANK/SIZE: $me/$np \t ROW RANK/SIZE: $subme/$subnp \t COLOR: $color")
}

MPI.shutdown()


//OUTPUT (jse code/mpi/split):
// WORLD RANK/SIZE: 0/1 	 ROW RANK/SIZE: 0/1 	 COLOR: 0

//OUTPUT (mpiexec -np 4 jse code/mpi/split):
// WORLD RANK/SIZE: 1/4     ROW RANK/SIZE: 1/3      COLOR: 0
// WORLD RANK/SIZE: 3/4     ROW RANK/SIZE: 0/1      COLOR: 1
// WORLD RANK/SIZE: 0/4     ROW RANK/SIZE: 0/3      COLOR: 0
// WORLD RANK/SIZE: 2/4     ROW RANK/SIZE: 2/3      COLOR: 0

//OUTPUT (mpiexec -np 8 jse code/mpi/split):
// WORLD RANK/SIZE: 6/8     ROW RANK/SIZE: 0/2      COLOR: 2
// WORLD RANK/SIZE: 2/8     ROW RANK/SIZE: 2/3      COLOR: 0
// WORLD RANK/SIZE: 0/8     ROW RANK/SIZE: 0/3      COLOR: 0
// WORLD RANK/SIZE: 4/8     ROW RANK/SIZE: 1/3      COLOR: 1
// WORLD RANK/SIZE: 5/8     ROW RANK/SIZE: 2/3      COLOR: 1
// WORLD RANK/SIZE: 1/8     ROW RANK/SIZE: 1/3      COLOR: 0
// WORLD RANK/SIZE: 3/8     ROW RANK/SIZE: 0/3      COLOR: 1
// WORLD RANK/SIZE: 7/8     ROW RANK/SIZE: 1/2      COLOR: 2

