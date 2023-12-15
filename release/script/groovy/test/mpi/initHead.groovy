package test.mpi

import jtool.parallel.MPI

import static jtool.code.CS.Exec.*
import static jtool.code.UT.Exec.*

system("javah -cp ${JAR_PATH} -d ${JAR_DIR}.mpisrc ${MPI.name}");

