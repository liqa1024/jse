package test.mpi

import jse.lmp.NativeLmp
import jse.parallel.MPI

import static jse.code.CS.Exec.*
import static jse.code.UT.Exec.*

system("javah -cp ${JAR_PATH} -d ../src/main/resources/assets/mpi/src ${MPI.Native.name}");

