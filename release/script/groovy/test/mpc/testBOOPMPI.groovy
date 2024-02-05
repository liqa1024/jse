package test.mpc

import jse.atom.MPC
import jse.code.UT
import jse.lmp.Lmpdat
import jse.parallel.MPI


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
for (_ in 0..<100) mpc.calBOOP(6, mpc.unitLen()*1.5);
def Q6 = mpc.calBOOP(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of Q6 jse: ${Q6.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of Q6 jse: 0.37245757782954264
// Total time: 00 hour 00 min 5.87 sec / 2.91 sec

if (me == 0) UT.Timer.tic();
for (_ in 0..<100) mpc.calABOOP(6, mpc.unitLen()*1.5);
def q6 = mpc.calABOOP(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of q6 jse: ${q6.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of q6 jse: 0.11684061284708915
// Total time: 00 hour 00 min 7.92 sec / 2.79 sec

if (me == 0) UT.Timer.tic();
for (_ in 0..<100) mpc.calConnectCountABOOP(6, 0.83, mpc.unitLen()*1.5);
def c6 = mpc.calConnectCountABOOP(6, 0.83, mpc.unitLen()*1.5);
if (me == 0) println("Mean of c6 jse: ${c6.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of c6 jse: 0.8780740740740741
// Total time: 00 hour 00 min 2.91 sec


// 使用 MPI 方式计算
if (me == 0) UT.Timer.tic();
for (_ in 0..<100) mpc.calBOOP_MPI(6, mpc.unitLen()*1.5);
def Q6_MPI = mpc.calBOOP_MPI(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of Q6 jse (MPI): ${Q6_MPI.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of Q6 jse: 0.37245757782954264
// Total time: 00 hour 00 min 6.43 sec / 2.81 sec / 3.40 sec (np = 1)
// Total time: 00 hour 00 min 2.56 sec / 1.28 sec / 2.20 sec (np = 4)
// Total time: 00 hour 00 min 1.95 sec / 1.01 sec (np = 8)

if (me == 0) UT.Timer.tic();
for (_ in 0..<100) mpc.calABOOP_MPI(6, mpc.unitLen()*1.5);
def q6_MPI = mpc.calABOOP_MPI(6, mpc.unitLen()*1.5);
if (me == 0) println("Mean of q6 jse (MPI): ${q6_MPI.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of q6 jse (MPI): 0.11684061284708915
// Total time: 00 hour 00 min 9.91 sec / 3.27 sec / 4.45 sec (np = 1)
// Total time: 00 hour 00 min 3.29 sec / 1.39 sec / 2.56 sec (np = 4)
// Total time: 00 hour 00 min 2.64 sec / 1.54 sec (np = 8)

if (me == 0) UT.Timer.tic();
for (_ in 0..<100) mpc.calConnectCountABOOP_MPI(6, 0.83, mpc.unitLen()*1.5);
def c6_MPI = mpc.calConnectCountABOOP_MPI(6, 0.83, mpc.unitLen()*1.5);
if (me == 0) println("Mean of c6 jse (MPI): ${c6_MPI.mean()}");
if (me == 0) UT.Timer.toc();
// Mean of c6 jse (MPI): 0.8780740740740741
// Total time: 00 hour 00 min 3.52 sec / 4.47 sec (np = 1)
// Total time: 00 hour 00 min 1.73 sec / 3.18 sec (np = 4)
// Total time: 00 hour 00 min 2.22 sec (np = 8)

mpc.shutdown();


MPI.shutdown();

