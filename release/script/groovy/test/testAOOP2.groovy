package test

import com.jtool.lmp.Lmpdat
import com.jtool.plot.Plotters


/** 测试计算不同近邻半径选择下的 AOOP 区别 */

// 设置线程数
nThreads = 1;


// 首先导入 Lmpdat
data = Lmpdat.read('lmp/data/data-crystal');
// 获取 MPC 计算单原子数据
mpc = data.getMPC(nThreads);

// 使用 Plotter 绘图
plt = Plotters.get();

q6 = mpc.calAOOP(6, mpc.unitLen()*1.2);
q4 = mpc.calAOOP(4, mpc.unitLen()*1.2);
plt.plot(q4, q6, '1.2 U').lineType('none').markerType('o').markerSize(4);

q6 = mpc.calAOOP(6, mpc.unitLen()*1.4);
q4 = mpc.calAOOP(4, mpc.unitLen()*1.4);
plt.plot(q4, q6, '1.4 U').lineType('none').markerType('o').markerSize(4);

q6 = mpc.calAOOP(6, mpc.unitLen()*1.6);
q4 = mpc.calAOOP(4, mpc.unitLen()*1.6);
plt.plot(q4, q6, '1.6 U').lineType('none').markerType('o').markerSize(4);

q6 = mpc.calAOOP(6, mpc.unitLen()*2.0);
q4 = mpc.calAOOP(4, mpc.unitLen()*2.0);
plt.plot(q4, q6, '2.0 U').lineType('none').markerType('o').markerSize(4);

// 计算完毕关闭 MPC
mpc.shutdown();

plt.xlabel('q4').ylabel('q6');
plt.show();

