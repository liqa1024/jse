package test.mpc

import jtool.atom.Structures
import jtool.code.UT
import jtool.lmp.Lmpdat
import jtool.plot.Plotters
import jtool.vasp.POSCAR


/** 测试计算 BOOP */

// 设置线程数
final int nThreads = 4;

final double cutoffMul = 1.5;
final int nnn = 12;
final double perturbMul = 1.0;

final boolean onlyCu = false;
final boolean onlyZr = true;

// 先计算玻璃态
data_G = Lmpdat.read('lmp/data/data-glass');
if (onlyCu) data_G = data_G.opt().filterType(1);
if (onlyZr) data_G = data_G.opt().filterType(2);
mpc_G = data_G.getMPC(nThreads);
println("glass, u: ${mpc_G.unitLen()}");
UT.Timer.tic();
q4_G = mpc_G.calABOOP(4, mpc_G.unitLen()*cutoffMul, nnn);
UT.Timer.toc("glass, q4");
UT.Timer.tic();
q6_G = mpc_G.calABOOP(6, mpc_G.unitLen()*cutoffMul, nnn);
UT.Timer.toc("glass, q6");
mpc_G.shutdown();


// 再计算生成的结果
data_FCC = Structures.FCC(4.0, 7).opt().perturbXYZ(0.25*perturbMul);
mpc_FCC = data_FCC.getMPC(nThreads);
println("FCC, u: ${mpc_FCC.unitLen()}");
UT.Timer.tic();
q4_FCC = mpc_FCC.calABOOP(4, mpc_FCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("FCC, q4");
UT.Timer.tic();
q6_FCC = mpc_FCC.calABOOP(6, mpc_FCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("FCC, q6");
mpc_FCC.shutdown();

data_BCC = Structures.BCC(4.0, 10).opt().perturbXYZ(0.32*perturbMul);
mpc_BCC = data_BCC.getMPC(nThreads);
println("BCC, u: ${mpc_BCC.unitLen()}");
UT.Timer.tic();
q4_BCC = mpc_BCC.calABOOP(4, mpc_BCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("BCC, q4");
UT.Timer.tic();
q6_BCC = mpc_BCC.calABOOP(6, mpc_BCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("BCC, q6");
mpc_BCC.shutdown();

data_HCP = Structures.HCP(4.0, 7).opt().perturbXYZ(0.35*perturbMul);
mpc_HCP = data_HCP.getMPC(nThreads);
println("HCP, u: ${mpc_HCP.unitLen()}");
UT.Timer.tic();
q4_HCP = mpc_HCP.calABOOP(4, mpc_HCP.unitLen()*cutoffMul, nnn);
UT.Timer.toc("HCP, q4");
UT.Timer.tic();
q6_HCP = mpc_HCP.calABOOP(6, mpc_HCP.unitLen()*cutoffMul, nnn);
UT.Timer.toc("HCP, q6");
mpc_HCP.shutdown();

data_MgCu2 = Structures.from(POSCAR.read('lmp/data/MgCu2.poscar'), 4).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) data_MgCu2 = data_MgCu2.opt().filterType(2);
if (onlyZr) data_MgCu2 = data_MgCu2.opt().filterType(1);
mpc_MgCu2 = data_MgCu2.getMPC(nThreads);
println("MgCu2, u: ${mpc_MgCu2.unitLen()}");
UT.Timer.tic();
q4_MgCu2 = mpc_MgCu2.calABOOP(4, mpc_MgCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgCu2, q4");
UT.Timer.tic();
q6_MgCu2 = mpc_MgCu2.calABOOP(6, mpc_MgCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgCu2, q6");
mpc_MgCu2.shutdown();

data_MgNi2 = Structures.from(POSCAR.read('lmp/data/re_MgNi2.poscar'), 3).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) data_MgNi2 = data_MgNi2.opt().filterType(2);
if (onlyZr) data_MgNi2 = data_MgNi2.opt().filterType(1);
mpc_MgNi2 = data_MgNi2.getMPC(nThreads);
println("MgNi2, u: ${mpc_MgNi2.unitLen()}");
UT.Timer.tic();
q4_MgNi2 = mpc_MgNi2.calABOOP(4, mpc_MgNi2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgNi2, q4");
UT.Timer.tic();
q6_MgNi2 = mpc_MgNi2.calABOOP(6, mpc_MgNi2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgNi2, q6");
mpc_MgNi2.shutdown();

data_MgZn2 = Structures.from(POSCAR.read('lmp/data/re_MgZn2.poscar'), 4).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) data_MgZn2 = data_MgZn2.opt().filterType(2);
if (onlyZr) data_MgZn2 = data_MgZn2.opt().filterType(1);
mpc_MgZn2 = data_MgZn2.getMPC(nThreads);
println("MgZn2, u: ${mpc_MgZn2.unitLen()}");
UT.Timer.tic();
q4_MgZn2 = mpc_MgZn2.calABOOP(4, mpc_MgZn2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgZn2, q4");
UT.Timer.tic();
q6_MgZn2 = mpc_MgZn2.calABOOP(6, mpc_MgZn2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgZn2, q6");
mpc_MgZn2.shutdown();


// 使用 Plotter 绘图
plt = Plotters.get();

plt.plot(q4_G       , q6_G       , 'glass'   ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_FCC     , q6_FCC     , 'FCC'     ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_BCC     , q6_BCC     , 'BCC'     ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_HCP     , q6_HCP     , 'HCP'     ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_MgCu2   , q6_MgCu2   , 'MgCu2'   ).lineType('none').markerType('s').markerSize(4);
plt.plot(q4_MgNi2   , q6_MgNi2   , 'MgNi2'   ).lineType('none').markerType('d').markerSize(4);
plt.plot(q4_MgZn2   , q6_MgZn2   , 'MgZn2'   ).lineType('none').markerType('^').markerSize(4);

plt.xlabel('q4').ylabel('q6');
plt.xTick(0.02).yTick(0.05);
plt.show();

