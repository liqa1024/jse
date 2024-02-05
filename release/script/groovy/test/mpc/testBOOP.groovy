package test.mpc

import jse.atom.Structures
import jse.code.UT
import jse.lmp.Lmpdat
import jse.plot.Plotters
import jse.vasp.POSCAR


/** 测试计算 BOOP */


// 首先导入 Lmpdat
data_G = Lmpdat.read('lmp/data/data-glass');
// 获取 MPC 计算单原子数据
mpc_G = data_G.getMPC();
println("glass, u: ${mpc_G.unitLen()}");

// 计算 q4
UT.Timer.tic();
q4_G = mpc_G.calABOOP(4);
UT.Timer.toc("glass, q4");

// 计算 q6
UT.Timer.tic();
q6_G = mpc_G.calABOOP(6);
UT.Timer.toc("glass, q6");

// 计算完毕关闭 MPC
mpc_G.shutdown();


// 再计算一个结晶的结果
data_C = Lmpdat.read('lmp/data/data-crystal');
// 获取 MPC 计算单原子数据
mpc_C = data_C.getMPC();
println("crystal, u: ${mpc_C.unitLen()}");

// 计算 q4
UT.Timer.tic();
q4_C = mpc_C.calABOOP(4);
UT.Timer.toc("crystal, q4");

// 计算 q6
UT.Timer.tic();
q6_C = mpc_C.calABOOP(6);
UT.Timer.toc("crystal, q6");

// 计算完毕关闭 MPC
mpc_C.shutdown();

//
//// 输出为 csv 文件
//UT.IO.data2csv(q6_G, 'lmp/.temp/q6_G.csv');
//UT.IO.data2csv(q4_G, 'lmp/.temp/q4_G.csv');


// 再计算生成的结果
data_FCC = Structures.FCC(4.0, 10).opt().perturbXYZ(0.25);
mpc_FCC = data_FCC.getMPC();
println("FCC, u: ${mpc_FCC.unitLen()}");
UT.Timer.tic();
q4_FCC = mpc_FCC.calABOOP(4);
UT.Timer.toc("FCC, q4");
UT.Timer.tic();
q6_FCC = mpc_FCC.calABOOP(6);
UT.Timer.toc("FCC, q6");
mpc_FCC.shutdown();

data_BCC = Structures.BCC(4.0, 15).opt().perturbXYZ(0.32);
mpc_BCC = data_BCC.getMPC();
println("BCC, u: ${mpc_BCC.unitLen()}");
UT.Timer.tic();
q4_BCC = mpc_BCC.calABOOP(4);
UT.Timer.toc("BCC, q4");
UT.Timer.tic();
q6_BCC = mpc_BCC.calABOOP(6);
UT.Timer.toc("BCC, q6");
mpc_BCC.shutdown();


// 使用 Plotter 绘图
plt = Plotters.get();

plt.plot(q4_G      , q6_G      , 'glass'  ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_C      , q6_C      , 'crystal').lineType('none').markerType('o').markerSize(4);
plt.plot(q4_FCC    , q6_FCC    , 'FCC'    ).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_BCC    , q6_BCC    , 'BCC'    ).lineType('none').markerType('o').markerSize(4);

plt.xlabel('q4').ylabel('q6');

plt.show();

