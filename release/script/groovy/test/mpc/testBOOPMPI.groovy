package test.mpc

import jtool.code.UT
import jtool.lmp.Lmpdat
import jtool.parallel.MPI


/**
 * 测试 BOOP 结果是否一致
 */

MPI.init();
int me = MPI.Comm.WORLD.rank();

final def dataPath = 'lmp/.ffs-in/data-fs2-init';

// 读取需要计算的结构
def mpc = Lmpdat.read(dataPath).getMPC();

// 直接计算结果
if (me == 0) UT.Timer.tic();
def Q6 = mpc.calBOOP(6, mpc.unitLen()*1.5);
for (_ in 0..<10) mpc.calBOOP(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of Q6 jtool: ${Q6.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of Q6 jtool: 0.37245757782954275
// Total time: 00 hour 00 min 0.89 sec

if (me == 0) UT.Timer.tic();
def q6 = mpc.calABOOP(6, mpc.unitLen()*1.5);
for (_ in 0..<10) mpc.calABOOP(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of q6 jtool: ${q6.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of q6 jtool: 0.11684061284708915
// Total time: 00 hour 00 min 1.07 sec


// 使用 MPI 方式计算
if (me == 0) UT.Timer.tic();
def Q6_MPI = mpc.calBOOP_MPI(6, mpc.unitLen()*1.5);
for (_ in 0..<10) mpc.calBOOP_MPI(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of Q6 jtool (MPI): ${Q6_MPI.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of Q6 jtool: 0.37245757782954275
// Total time: 00 hour 00 min 0.90 sec (np = 1)
// Total time: 00 hour 00 min 0.33 sec (np = 4)

if (me == 0) UT.Timer.tic();
def q6_MPI = mpc.calABOOP_MPI(6, mpc.unitLen()*1.5);
for (_ in 0..<10) mpc.calABOOP_MPI(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of q6 jtool (MPI): ${q6_MPI.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of q6 jtool (MPI): 0.11684061284708915
// Total time: 00 hour 00 min 1.17 sec (np = 1)
// Total time: 00 hour 00 min 0.42 sec (np = 4)


mpc.shutdown();


MPI.shutdown();

