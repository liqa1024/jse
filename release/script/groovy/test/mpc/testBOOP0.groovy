package test.mpc

import jtool.atom.Structures
import jtool.code.UT
import jtool.lmp.Lmpdat
import jtool.plot.Plotters
import jtool.vasp.POSCAR


/** 测试计算 BOOP */

final double cutoffMul = 1.5;
final int nnn = 12;
final double perturbMul = 1.0;

final boolean onlyCu = false;
final boolean onlyZr = false;

// 先计算玻璃态
data_G = Lmpdat.read('lmp/data/data-glass');
if (onlyCu) data_G = data_G.opt().filterType(1);
if (onlyZr) data_G = data_G.opt().filterType(2);
mpc_G = data_G.getMPC();
println("glass, u: ${mpc_G.unitLen()}");
UT.Timer.tic();
w4_G = mpc_G.calABOOP3(4, mpc_G.unitLen()*cutoffMul, nnn);
UT.Timer.toc("glass, w4");
UT.Timer.tic();
q4_G = mpc_G.calABOOP(4, mpc_G.unitLen()*cutoffMul, nnn);
UT.Timer.toc("glass, q4");
mpc_G.shutdown();


// 再计算生成的结果
data_FCC = Structures.FCC(4.0, 8).opt().perturbXYZ(0.25*perturbMul);
mpc_FCC = data_FCC.getMPC();
println("FCC, u: ${mpc_FCC.unitLen()}");
UT.Timer.tic();
q4_FCC = mpc_FCC.calABOOP(4, mpc_FCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("FCC, q4");
UT.Timer.tic();
w4_FCC = mpc_FCC.calABOOP3(4, mpc_FCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("FCC, w4");
mpc_FCC.shutdown();

data_BCC = Structures.BCC(4.0, 12).opt().perturbXYZ(0.32*perturbMul);
mpc_BCC = data_BCC.getMPC();
println("BCC, u: ${mpc_BCC.unitLen()}");
UT.Timer.tic();
q4_BCC = mpc_BCC.calABOOP(4, mpc_BCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("BCC, q4");
UT.Timer.tic();
w4_BCC = mpc_BCC.calABOOP3(4, mpc_BCC.unitLen()*cutoffMul, nnn);
UT.Timer.toc("BCC, w4");
mpc_BCC.shutdown();

data_HCP = Structures.HCP(4.0, 8).opt().perturbXYZ(0.35*perturbMul);
mpc_HCP = data_HCP.getMPC();
println("HCP, u: ${mpc_HCP.unitLen()}");
UT.Timer.tic();
q4_HCP = mpc_HCP.calABOOP(4, mpc_HCP.unitLen()*cutoffMul, nnn);
UT.Timer.toc("HCP, q4");
UT.Timer.tic();
w4_HCP = mpc_HCP.calABOOP3(4, mpc_HCP.unitLen()*cutoffMul, nnn);
UT.Timer.toc("HCP, w4");
mpc_HCP.shutdown();


data_MgCu2 = Structures.from(POSCAR.read('vasp/data/MgCu2.poscar'), 4).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) data_MgCu2 = data_MgCu2.opt().filterType(2);
if (onlyZr) data_MgCu2 = data_MgCu2.opt().filterType(1);
mpc_MgCu2 = data_MgCu2.getMPC();
println("MgCu2, u: ${mpc_MgCu2.unitLen()}");
UT.Timer.tic();
q4_MgCu2 = mpc_MgCu2.calABOOP(4, mpc_MgCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgCu2, q4");
UT.Timer.tic();
w4_MgCu2 = mpc_MgCu2.calABOOP3(4, mpc_MgCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("MgCu2, w4");
mpc_MgCu2.shutdown();

data_Zr3Cu8 = Structures.from(POSCAR.read('vasp/data/Zr3Cu8.poscar'), 3).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) data_Zr3Cu8 = data_Zr3Cu8.opt().filterType(2);
if (onlyZr) data_Zr3Cu8 = data_Zr3Cu8.opt().filterType(1);
mpc_Zr3Cu8 = data_Zr3Cu8.getMPC();
println("Zr3Cu8, u: ${mpc_Zr3Cu8.unitLen()}");
UT.Timer.tic();
q4_Zr3Cu8 = mpc_Zr3Cu8.calABOOP(4, mpc_Zr3Cu8.unitLen()*cutoffMul, nnn);
UT.Timer.toc("Zr3Cu8, q4");
UT.Timer.tic();
w4_Zr3Cu8 = mpc_Zr3Cu8.calABOOP3(4, mpc_Zr3Cu8.unitLen()*cutoffMul, nnn);
UT.Timer.toc("Zr3Cu8, w4");
mpc_Zr3Cu8.shutdown();

data_Zr7Cu10 = Structures.from(POSCAR.read('vasp/data/Zr7Cu10.poscar'), 3).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) data_Zr7Cu10 = data_Zr7Cu10.opt().filterType(2);
if (onlyZr) data_Zr7Cu10 = data_Zr7Cu10.opt().filterType(1);
mpc_Zr7Cu10 = data_Zr7Cu10.getMPC();
println("Zr7Cu10, u: ${mpc_Zr7Cu10.unitLen()}");
UT.Timer.tic();
q4_Zr7Cu10 = mpc_Zr7Cu10.calABOOP(4, mpc_Zr7Cu10.unitLen()*cutoffMul, nnn);
UT.Timer.toc("Zr7Cu10, q4");
UT.Timer.tic();
w4_Zr7Cu10 = mpc_Zr7Cu10.calABOOP3(4, mpc_Zr7Cu10.unitLen()*cutoffMul, nnn);
UT.Timer.toc("Zr7Cu10, w4");
mpc_Zr7Cu10.shutdown();

data_ZrCu2 = Structures.from(POSCAR.read('vasp/data/ZrCu2.poscar'), 5).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) data_ZrCu2 = data_ZrCu2.opt().filterType(2);
if (onlyZr) data_ZrCu2 = data_ZrCu2.opt().filterType(1);
mpc_ZrCu2 = data_ZrCu2.getMPC();
println("ZrCu2, u: ${mpc_ZrCu2.unitLen()}");
UT.Timer.tic();
q4_ZrCu2 = mpc_ZrCu2.calABOOP(4, mpc_ZrCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("ZrCu2, q4");
UT.Timer.tic();
w4_ZrCu2 = mpc_ZrCu2.calABOOP3(4, mpc_ZrCu2.unitLen()*cutoffMul, nnn);
UT.Timer.toc("ZrCu2, w4");
mpc_ZrCu2.shutdown();



// 使用 Plotter 绘图
plt = Plotters.get();

plt.plot(q4_G      , w4_G      , 'glass'  ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_FCC    , w4_FCC    , 'FCC'    ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_BCC    , w4_BCC    , 'BCC'    ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_HCP    , w4_HCP    , 'HCP'    ).lineType('none').markerType('o').markerSize(4);
//plt.plot(q4_MgCu2  , w4_MgCu2  , 'MgCu2'  ).lineType('none').markerType('s').markerSize(4);
//plt.plot(q4_Zr3Cu8 , w4_Zr3Cu8 , 'Zr3Cu8' ).lineType('none').markerType('^').markerSize(4);
//plt.plot(q4_Zr7Cu10, w4_Zr7Cu10, 'Zr7Cu10').lineType('none').markerType('^').markerSize(4);
//plt.plot(q4_ZrCu2  , w4_ZrCu2  , 'ZrCu2'  ).lineType('none').markerType('^').markerSize(4);

plt.xlabel('q4').ylabel('w4');
plt.xTick(0.02).yTick(0.05);
plt.show();

