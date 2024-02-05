package test.mpc

import jse.code.UT
import jse.lmp.Lmpdat
import jse.plot.Plotters

/** 测试计算 RDF */

// 设置线程数
nThreads = 1;

// 首先导入 Lmpdat
data = Lmpdat.read('lmp/data/data-glass');
// 获取 MPC 计算单原子数据
mpc = data.getTypeMPC(1, nThreads);

// 计算 RDF
UT.Timer.tic();
gr = mpc.calRDF();
UT.Timer.toc("${nThreads} threads, RDF");
// calRDF(1000, 100.0); 1 threads, RDF time: 00 hour 00 min 2.91 sec
// calRDF(1000, 100.0); 4 threads, RDF time: 00 hour 00 min 0.84 sec

// 计算 SF
UT.Timer.tic();
Sq = mpc.calSF();
UT.Timer.toc("${nThreads} threads, SF");

// 使用 FT 来计算
UT.Timer.tic();
grFT = mpc.SF2RDF(Sq);
SqFT = mpc.RDF2SF(gr);
UT.Timer.toc("FT");

// 计算完毕关闭 MPC
mpc.shutdown();

// 输出为 csv 文件
UT.IO.data2csv(gr, 'lmp/.temp/gr.csv');
UT.IO.data2csv(Sq, 'lmp/.temp/Sq.csv');
UT.IO.data2csv(grFT, 'lmp/.temp/grFT.csv');
UT.IO.data2csv(SqFT, 'lmp/.temp/SqFT.csv');

// 使用 Plotter 绘图
plt = Plotters.get();

plt.plot(gr, 'g(r)').color('k').lineType('-');
plt.plot(grFT, 'g(r)-FT').color('k').lineType('--');

plt.plot(Sq, 'S(q)').color('r').lineType('-');
plt.plot(SqFT, 'S(q)-FT').color('r').lineType('--');

plt.show();

