package test.mpc

import jtool.atom.Structures
import jtool.code.UT
import jtool.lmp.Lmpdat


/**
 * 测试基组效率
 */

nThreads = 1;
data = Structures.from(Lmpdat.read('lmp/data/data-glass'), 2);
println("AtomNum: ${data.atomNum()}");

UT.Timer.tic();
data.getMPC(nThreads).withCloseable {it.calFPSuRui(5, 6, 6.5)}
UT.Timer.toc("${nThreads} threads");
// 1 threads time: 00 hour 00 min 1.21 sec
// 4 threads time: 00 hour 00 min 0.75 sec

