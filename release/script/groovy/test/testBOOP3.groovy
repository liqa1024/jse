package test

import com.jtool.code.UT
import com.jtool.lmp.Lmpdat


/** 测试计算 AOOP */

// 设置线程数
nThreads = 1;


// 首先导入 Lmpdat
data = Lmpdat.read('lmp/data/data-glass');
// 获取 MPC 计算单原子数据
mpc = data.getMPC(nThreads);

// 计算 q6
UT.Timer.tic();
Q6 = mpc.calBOOP(6);
UT.Timer.toc("${nThreads} threads, Q6");

// 计算 q4
UT.Timer.tic();
Q4 = mpc.calBOOP(4);
UT.Timer.toc("${nThreads} threads, Q4");

// 计算完毕关闭 MPC
mpc.shutdown();


// 直接输出平均值检验正确性
println(Q6.mean());
println(Q4.mean());

