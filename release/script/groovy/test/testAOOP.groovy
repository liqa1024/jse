package test

import com.jtool.code.UT
import com.jtool.lmp.Lmpdat
import com.jtool.plot.Plotters


/** 测试计算 AOOP */

// 设置线程数
nThreads = 1;


// 首先导入 Lmpdat
data_G = Lmpdat.read('lmp/data/data-glass');
// 获取 MPC 计算单原子数据
mpc_G = data_G.getMPC(nThreads);

// 计算 q6
UT.Timer.tic();
q6_G = mpc_G.calAOOP(6, mpc_G.unitLen()*2.0, 12);
UT.Timer.toc("${nThreads} threads, q6");

// 计算 q4
UT.Timer.tic();
q4_G = mpc_G.calAOOP(4, mpc_G.unitLen()*2.0, 12);
UT.Timer.toc("${nThreads} threads, q4");

// 计算完毕关闭 MPC
mpc_G.shutdown();


// 再计算一个结晶的结果
data_C = Lmpdat.read('lmp/data/data-crystal');
// 获取 MPC 计算单原子数据
mpc_C = data_C.getMPC(nThreads);

// 计算 q6
UT.Timer.tic();
q6_C = mpc_C.calAOOP(6, mpc_G.unitLen()*2.0, 12);
UT.Timer.toc("${nThreads} threads, q6");

// 计算 q4
UT.Timer.tic();
q4_C = mpc_C.calAOOP(4, mpc_G.unitLen()*2.0, 12);
UT.Timer.toc("${nThreads} threads, q4");

// 计算完毕关闭 MPC
mpc_C.shutdown();

//
//// 输出为 csv 文件
//UT.IO.data2csv(q6_G, 'lmp/.temp/q6_G.csv');
//UT.IO.data2csv(q4_G, 'lmp/.temp/q4_G.csv');


// 使用 Plotter 绘图
plt = Plotters.get();

plt.plot(q4_G, q6_G).lineType('none').markerType('o').markerSize(4);
plt.plot(q4_C, q6_C).lineType('none').markerType('o').markerSize(4);

plt.xlabel('q4').ylabel('q6');

plt.show();

