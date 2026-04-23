package code.mpi

import jse.parallel.MPI
import static jse.code.UT.Math.*


// 固定随机流
rng(123456789)

MPI.init()
int me = MPI.Comm.WORLD.rank()

def r = me==0 ? rand(3) : zeros(3)
println("rand of <$me>: $r")

MPI.Comm.WORLD.bcast(r, 0)
println("bcast rand of <$me>: $r")

MPI.close()


//OUTPUT (jse code/mpi/bcast):
// rand of <0>: 3-length Vector:
//    0.6640   0.4570   0.3905
// bcast rand of <0>: 3-length Vector:
//    0.6640   0.4570   0.3905

//OUTPUT (mpiexec -np 4 jse code/mpi/bcast):
// rand of <2>: 3-length Vector:
//    0.000   0.000   0.000
// rand of <3>: 3-length Vector:
//    0.000   0.000   0.000
// rand of <1>: 3-length Vector:
//    0.000   0.000   0.000
// rand of <0>: 3-length Vector:
//    0.6640   0.4570   0.3905
// bcast rand of <0>: 3-length Vector:
//    0.6640   0.4570   0.3905
// bcast rand of <1>: 3-length Vector:
//    0.6640   0.4570   0.3905
// bcast rand of <2>: 3-length Vector:
//    0.6640   0.4570   0.3905
// bcast rand of <3>: 3-length Vector:
//    0.6640   0.4570   0.3905

