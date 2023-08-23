package test

import com.jtool.atom.Structures
import com.jtool.code.UT
import com.jtool.lmp.Lmpdat
import com.jtool.plot.Plotters
import com.jtool.vasp.POSCAR


/** 测试计算 AOOP */

final double cutoffMul = 2.0;

// 先计算玻璃态
data_G = Lmpdat.read('lmp/data/data-glass');
mpc_G = data_G.getMPC();
println("glass, u: ${mpc_G.unitLen()}");
UT.Timer.tic();
q6_G = mpc_G.calAOOP(6, mpc_G.unitLen()*cutoffMul, 12);
UT.Timer.toc("glass, q6");
UT.Timer.tic();
q4_G = mpc_G.calAOOP(4, mpc_G.unitLen()*cutoffMul, 12);
UT.Timer.toc("glass, q4");
mpc_G.shutdown();


// 再计算生成的结果
data_FCC = Structures.FCC(4.0, 8).opt().perturbG(0.25);
mpc_FCC = data_FCC.getMPC();
println("FCC, u: ${mpc_FCC.unitLen()}");
UT.Timer.tic();
q6_FCC = mpc_FCC.calAOOP(6, mpc_FCC.unitLen()*cutoffMul, 12);
UT.Timer.toc("FCC, q6");
UT.Timer.tic();
q4_FCC = mpc_FCC.calAOOP(4, mpc_FCC.unitLen()*cutoffMul, 12);
UT.Timer.toc("FCC, q4");
mpc_FCC.shutdown();

data_BCC = Structures.BCC(4.0, 12).opt().perturbG(0.32);
mpc_BCC = data_BCC.getMPC();
println("BCC, u: ${mpc_BCC.unitLen()}");
UT.Timer.tic();
q6_BCC = mpc_BCC.calAOOP(6, mpc_BCC.unitLen()*cutoffMul, 12);
UT.Timer.toc("BCC, q6");
UT.Timer.tic();
q4_BCC = mpc_BCC.calAOOP(4, mpc_BCC.unitLen()*cutoffMul, 12);
UT.Timer.toc("BCC, q4");
mpc_BCC.shutdown();

data_HCP = Structures.HCP(4.0, 8).opt().perturbG(0.35);
mpc_HCP = data_HCP.getMPC();
println("HCP, u: ${mpc_HCP.unitLen()}");
UT.Timer.tic();
q6_HCP = mpc_HCP.calAOOP(6, mpc_HCP.unitLen()*cutoffMul, 12);
UT.Timer.toc("HCP, q6");
UT.Timer.tic();
q4_HCP = mpc_HCP.calAOOP(4, mpc_HCP.unitLen()*cutoffMul, 12);
UT.Timer.toc("HCP, q4");
mpc_HCP.shutdown();


data_MgCu2 = Structures.from(POSCAR.read('lmp/data/MgCu2.poscar'), 4).opt().perturbG(0.25);
mpc_MgCu2 = data_MgCu2.getMPC();
println("MgCu2, u: ${mpc_MgCu2.unitLen()}");
UT.Timer.tic();
q6_MgCu2 = mpc_MgCu2.calAOOP(6, mpc_MgCu2.unitLen()*cutoffMul, 12);
UT.Timer.toc("MgCu2, q6");
UT.Timer.tic();
q4_MgCu2 = mpc_MgCu2.calAOOP(4, mpc_MgCu2.unitLen()*cutoffMul, 12);
UT.Timer.toc("MgCu2, q4");
mpc_MgCu2.shutdown();

data_Zr3Cu8 = Structures.from(POSCAR.read('lmp/data/Zr3Cu8.poscar'), 3).opt().perturbG(0.25);
mpc_Zr3Cu8 = data_Zr3Cu8.getMPC();
println("Zr3Cu8, u: ${mpc_Zr3Cu8.unitLen()}");
UT.Timer.tic();
q6_Zr3Cu8 = mpc_Zr3Cu8.calAOOP(6, mpc_Zr3Cu8.unitLen()*cutoffMul, 12);
UT.Timer.toc("Zr3Cu8, q6");
UT.Timer.tic();
q4_Zr3Cu8 = mpc_Zr3Cu8.calAOOP(4, mpc_Zr3Cu8.unitLen()*cutoffMul, 12);
UT.Timer.toc("Zr3Cu8, q4");
mpc_Zr3Cu8.shutdown();

data_Zr7Cu10 = Structures.from(POSCAR.read('lmp/data/Zr7Cu10.poscar'), 3).opt().perturbG(0.25);
mpc_Zr7Cu10 = data_Zr7Cu10.getMPC();
println("Zr7Cu10, u: ${mpc_Zr7Cu10.unitLen()}");
UT.Timer.tic();
q6_Zr7Cu10 = mpc_Zr7Cu10.calAOOP(6, mpc_Zr7Cu10.unitLen()*cutoffMul, 12);
UT.Timer.toc("Zr7Cu10, q6");
UT.Timer.tic();
q4_Zr7Cu10 = mpc_Zr7Cu10.calAOOP(4, mpc_Zr7Cu10.unitLen()*cutoffMul, 12);
UT.Timer.toc("Zr7Cu10, q4");
mpc_Zr7Cu10.shutdown();

data_ZrCu2 = Structures.from(POSCAR.read('lmp/data/ZrCu2.poscar'), 5).opt().perturbG(0.25);
mpc_ZrCu2 = data_ZrCu2.getMPC();
println("ZrCu2, u: ${mpc_ZrCu2.unitLen()}");
UT.Timer.tic();
q6_ZrCu2 = mpc_ZrCu2.calAOOP(6, mpc_ZrCu2.unitLen()*cutoffMul, 12);
UT.Timer.toc("ZrCu2, q6");
UT.Timer.tic();
q4_ZrCu2 = mpc_ZrCu2.calAOOP(4, mpc_ZrCu2.unitLen()*cutoffMul, 12);
UT.Timer.toc("ZrCu2, q4");
mpc_ZrCu2.shutdown();



// 使用 Plotter 绘图
plt = Plotters.get();

plt.plot(q4_G      , q6_G      , 'glass'  ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_FCC    , q6_FCC    , 'FCC'    ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_BCC    , q6_BCC    , 'BCC'    ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_HCP    , q6_HCP    , 'HCP'    ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_MgCu2  , q6_MgCu2  , 'MgCu2'  ).lineType('none').markerType('s').markerSize(4);
plt.plot(q4_Zr3Cu8 , q6_Zr3Cu8 , 'Zr3Cu8' ).lineType('none').markerType('^').markerSize(4);
plt.plot(q4_Zr7Cu10, q6_Zr7Cu10, 'Zr7Cu10').lineType('none').markerType('^').markerSize(4);
plt.plot(q4_ZrCu2  , q6_ZrCu2  , 'ZrCu2'  ).lineType('none').markerType('^').markerSize(4);

plt.xlabel('q4').ylabel('q6');
plt.show();

