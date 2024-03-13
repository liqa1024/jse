package example.mpi

import jse.parallel.MPI
import static jse.code.UT.Math.*

// 固定随机流
rng(123456789)

MPI.init()
int me = MPI.Comm.WORLD.rank()

byte[] r = [0, 0, 0]
if (me == 0) {
    r[0] = (byte)randi(128)
    r[1] = (byte)randi(128)
    r[2] = (byte)randi(128)
}
println("rand of <$me>: $r")

MPI.Comm.WORLD.bcast(r, r.size(), 0)
println("bcast rand of <$me>: $r")

MPI.shutdown()


//OUTPUT (jse example/mpi/bcast):
// rand of <0>: [84, 97, 58]
// bcast rand of <0>: [84, 97, 58]

//OUTPUT (mpiexec -np 4 jse example/mpi/bcast):
// rand of <3>: [0, 0, 0]
// rand of <2>: [0, 0, 0]
// rand of <1>: [0, 0, 0]
// rand of <0>: [84, 97, 58]
// bcast rand of <0>: [84, 97, 58]
// bcast rand of <1>: [84, 97, 58]
// bcast rand of <2>: [84, 97, 58]
// bcast rand of <3>: [84, 97, 58]

