package test

import com.guan.code.UT
import com.guan.lmp.Lmpdat
import com.guan.math.RealMatrixColumn
import com.guan.plot.Plotters


/** 测试计算 RDF */

// 设置线程数
nThreads = 4;

// 首先导入 Lmpdat
data = Lmpdat.read('lmp/data/data-glass');
// 获取 MPC 计算单原子数据
mpc = data.getMPC(nThreads);
// 计算 RDF
UT.Timer.tic();
gr = mpc.calRDF();
UT.Timer.toc("${nThreads} threads, RDF");

// 计算完毕关闭 MPC
mpc.shutdown();
mpc.awaitTermination();

// 保存之前先创建文件夹
UT.IO.mkdir('lmp/.temp');
// 输出为 csv 文件
UT.IO.data2csv(gr, 'lmp/.temp/rdf.csv', 'gr', 'r');

//// 使用 Plotter 绘图
//plt = Plotters.get();
//plt.plot(new RealMatrixColumn(gr, 1), new RealMatrixColumn(gr, 0), 'RDF')
//plt.show();

