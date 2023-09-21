package test.mpc

import com.jtool.atom.Structures
import com.jtool.code.UT
import com.jtool.plot.Plotters


/** 测试计算 RDF */

// 设置线程数
nThreads = 4;

// 直接计算 FCC
data1 = Structures.FCC(4.0, 1);
mpc1 = data1.getMPC(nThreads);
UT.Timer.tic();
gr1 = mpc1.calRDF(400);
UT.Timer.toc("FCC 1");
mpc1.shutdown();

// 计算扩胞后的 FCC
data211 = Structures.FCC(4.0, 2, 1, 1);
mpc211 = data211.getMPC(nThreads);
UT.Timer.tic();
gr211 = mpc211.calRDF(400);
UT.Timer.toc("FCC 211");
mpc211.shutdown();

data221 = Structures.FCC(4.0, 2, 2, 1);
mpc221 = data221.getMPC(nThreads);
UT.Timer.tic();
gr221 = mpc221.calRDF(400);
UT.Timer.toc("FCC 221");
mpc221.shutdown();

data122 = Structures.FCC(4.0, 1, 2, 2);
mpc122 = data122.getMPC(nThreads);
UT.Timer.tic();
gr122 = mpc122.calRDF(400);
UT.Timer.toc("FCC 122");
mpc122.shutdown();

data4 = Structures.FCC(4.0, 4);
mpc4 = data4.getMPC(nThreads);
UT.Timer.tic();
gr4 = mpc4.calRDF(400);
UT.Timer.toc("FCC 4");
mpc4.shutdown();

data7 = Structures.FCC(4.0, 7);
mpc7 = data7.getMPC(nThreads);
UT.Timer.tic();
gr7 = mpc7.calRDF(400);
UT.Timer.toc("FCC 7");
mpc7.shutdown();


// 使用 Plotter 绘图
plt = Plotters.get();
plt.plot(gr1  , 'g(r)-1'  );
plt.plot(gr211, 'g(r)-211');
plt.plot(gr221, 'g(r)-221');
plt.plot(gr122, 'g(r)-122');
plt.plot(gr4  , 'g(r)-4'  );
plt.plot(gr7  , 'g(r)-7'  ).lineType('--');
plt.show();

