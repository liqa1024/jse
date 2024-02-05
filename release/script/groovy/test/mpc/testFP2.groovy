package test.mpc

import jse.code.UT
import jse.lmp.Lmpdat


/**
 * 测试基组效率
 */

nThreads = 1;
data = Lmpdat.read('lmp/data/data-glass');
println("AtomNum: ${data.atomNum()}");

UT.Timer.tic();
mpc = data.getMPC(nThreads);
for (_ in 1..20) mpc.calFPSuRui(5, 6, 6.5);
mpc.shutdown();
UT.Timer.toc("${nThreads} threads");
// 1 threads time: 00 hour 00 min 3.19 sec
// 4 threads time: 00 hour 00 min 1.80 sec

