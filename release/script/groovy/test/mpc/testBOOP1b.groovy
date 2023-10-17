package test.mpc

import jtool.atom.Structures
import jtool.code.UT
import jtool.lmp.Dump
import jtool.lmp.Lmpdat
import jtool.plot.Plotters
import jtool.vasp.POSCAR


/** 测试计算 BOOP */

// 设置线程数
final int nThreads = 4;

final double cutoffMul = 2.2;
final int nnn = -1;
final double perturbMul = 1.0;

final boolean onlyCu = true;
final boolean onlyZr = false;


dataMgCu2 = Structures.from(POSCAR.read('lmp/data/MgCu2.poscar'), 4).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataMgCu2 = dataMgCu2.opt().filterType(2);
if (onlyZr) dataMgCu2 = dataMgCu2.opt().filterType(1);
mpcMgCu2 = dataMgCu2.getMPC(nThreads);
println("MgCu2, u: ${mpcMgCu2.unitLen()}");
UT.Timer.tic();
q4MgCu2 = mpcMgCu2.calABOOP(4, mpcMgCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgCu2, q4");
UT.Timer.tic();
q6MgCu2 = mpcMgCu2.calABOOP(6, mpcMgCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgCu2, q6");
mpcMgCu2.shutdown();

dataZrCu2 = Structures.from(POSCAR.read('lmp/data/ZrCu2.poscar'), 5).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataZrCu2 = dataZrCu2.opt().filterType(2);
if (onlyZr) dataZrCu2 = dataZrCu2.opt().filterType(1);
mpcZrCu2 = dataZrCu2.getMPC(nThreads);
println("ZrCu2, u: ${mpcZrCu2.unitLen()}");
UT.Timer.tic();
q4ZrCu2 = mpcZrCu2.calABOOP(4, mpcZrCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("ZrCu2, q4");
UT.Timer.tic();
q6ZrCu2 = mpcZrCu2.calABOOP(6, mpcZrCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("ZrCu2, q6");
mpcZrCu2.shutdown();

dataB2 = Structures.from(POSCAR.read('lmp/data/ZrCu-B2.poscar'), 8).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataB2 = dataB2.opt().filterType(2);
if (onlyZr) dataB2 = dataB2.opt().filterType(1);
mpcB2 = dataB2.getMPC(nThreads);
println("B2, u: ${mpcB2.unitLen()}");
UT.Timer.tic();
q4B2 = mpcB2.calABOOP(4, mpcB2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("B2, q4");
UT.Timer.tic();
q6B2 = mpcB2.calABOOP(6, mpcB2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("B2, q6");
mpcB2.shutdown();


// 再计算生成的结果
//def dataFFS1   = Dump.read('lmp/.ffs-in/dump-fs1-new').last();
//if (onlyCu) dataFFS1 = dataFFS1.opt().filterType(1);
//if (onlyZr) dataFFS1 = dataFFS1.opt().filterType(2);
//mpcFFS1 = dataFFS1.getMPC(nThreads);
//println("FFS1, u: ${mpcFFS1.unitLen()}");
//UT.Timer.tic();
//q4FFS1 = mpcFFS1.calABOOP(4, mpcFFS1.unitLen()*cutoffMul, nnn);
//UT.Timer.toc("FFS1, q4");
//UT.Timer.tic();
//q6FFS1 = mpcFFS1.calABOOP(6, mpcFFS1.unitLen()*cutoffMul, nnn);
//UT.Timer.toc("FFS1, q6");
//mpcFFS1.shutdown();
//
//def dataFFS2   = Dump.read('lmp/.ffs-in/dump-fs2-new').last();
//if (onlyCu) dataFFS2 = dataFFS2.opt().filterType(1);
//if (onlyZr) dataFFS2 = dataFFS2.opt().filterType(2);
//mpcFFS2 = dataFFS2.getMPC(nThreads);
//println("FFS2, u: ${mpcFFS2.unitLen()}");
//UT.Timer.tic();
//q4FFS2 = mpcFFS2.calABOOP(4, mpcFFS2.unitLen()*cutoffMul, nnn);
//UT.Timer.toc("FFS2, q4");
//UT.Timer.tic();
//q6FFS2 = mpcFFS2.calABOOP(6, mpcFFS2.unitLen()*cutoffMul, nnn);
//UT.Timer.toc("FFS2, q6");
//mpcFFS2.shutdown();

def dataMelt   = Lmpdat.read('lmp/.stableglass-in/data-last');
if (onlyCu) dataMelt = dataMelt.opt().filterType(1);
if (onlyZr) dataMelt = dataMelt.opt().filterType(2);
mpcMelt = dataMelt.getMPC(nThreads);
println("melt, u: ${mpcMelt.unitLen()}");
UT.Timer.tic();
q4Melt = mpcMelt.calABOOP(4, mpcMelt.unitLen()*cutoffMul, nnn);
UT.Timer.toc("melt, q4");
UT.Timer.tic();
q6Melt = mpcMelt.calABOOP(6, mpcMelt.unitLen()*cutoffMul, nnn);
UT.Timer.toc("melt, q6");
mpcMelt.shutdown();

//def dataMelt2  = Dump.read('lmp/.stableglass-in/dump-fs1-melt-fast').getMFPC(0.002).getMeanAtomData(1000, 50);
//if (onlyCu) dataMelt2 = dataMelt2.opt().filterType(1);
//if (onlyZr) dataMelt2 = dataMelt2.opt().filterType(2);
//mpcMelt2 = dataMelt2.getMPC(nThreads);
//println("Melt2, u: ${mpcMelt2.unitLen()}");
//UT.Timer.tic();
//q4Melt2 = mpcMelt2.calABOOP(4, mpcMelt2.unitLen()*cutoffMul, nnn);
//UT.Timer.toc("Melt2, q4");
//UT.Timer.tic();
//q6Melt2 = mpcMelt2.calABOOP(6, mpcMelt2.unitLen()*cutoffMul, nnn);
//UT.Timer.toc("Melt2, q6");
//mpcMelt2.shutdown();


// 使用 Plotter 绘图
//plt1 = Plotters.get();
//plt1.plot(q4MgCu2   , q6MgCu2   , 'laves-MgCu2').color(1).lineType('none').markerType('o').markerSize(4);
//plt1.plot(q4ZrCu2   , q6ZrCu2   , 'ZrCu2'      ).color(2).lineType('none').markerType('o').markerSize(4);
//plt1.plot(q4B2      , q6B2      , 'B2'         ).color(3).lineType('none').markerType('o').markerSize(4);
//plt1.plot(q4FFS1    , q6FFS1    , 'FFS1'       ).color(0).lineType('none').markerType('^').markerSize(5);
//plt1.xlabel('q4').ylabel('q6');
//plt1.show();
//
//plt2 = Plotters.get();
//plt2.plot(q4MgCu2   , q6MgCu2   , 'laves-MgCu2').color(1).lineType('none').markerType('o').markerSize(4);
//plt2.plot(q4ZrCu2   , q6ZrCu2   , 'ZrCu2'      ).color(2).lineType('none').markerType('o').markerSize(4);
//plt2.plot(q4B2      , q6B2      , 'B2'         ).color(3).lineType('none').markerType('o').markerSize(4);
//plt2.plot(q4FFS2    , q6FFS2    , 'FFS2'       ).color(0).lineType('none').markerType('^').markerSize(5);
//plt2.xlabel('q4').ylabel('q6');
//plt2.show();

plt3 = Plotters.get();
plt3.plot(q4MgCu2   , q6MgCu2   , 'laves-MgCu2').color(1).lineType('none').markerType('o').markerSize(4);
plt3.plot(q4ZrCu2   , q6ZrCu2   , 'ZrCu2'      ).color(2).lineType('none').markerType('o').markerSize(4);
plt3.plot(q4B2      , q6B2      , 'B2'         ).color(3).lineType('none').markerType('o').markerSize(4);
plt3.plot(q4Melt    , q6Melt    , 'melt'       ).color(0).lineType('none').markerType('^').markerSize(5);
plt3.xlabel('q4').ylabel('q6');
plt3.show();

//plt4 = Plotters.get();
//plt4.plot(q4MgCu2   , q6MgCu2   , 'laves-MgCu2').color(1).lineType('none').markerType('o').markerSize(4);
//plt4.plot(q4ZrCu2   , q6ZrCu2   , 'ZrCu2'      ).color(2).lineType('none').markerType('o').markerSize(4);
//plt4.plot(q4B2      , q6B2      , 'B2'         ).color(3).lineType('none').markerType('o').markerSize(4);
//plt4.plot(q4Melt2   , q6Melt2   , 'melt2'      ).color(0).lineType('none').markerType('^').markerSize(5);
//plt4.xlabel('q4').ylabel('q6');
//plt4.show();
