package test.mpc

import jse.lmp.Dump
import jse.lmp.Lmpdat
import jse.math.vector.Vectors
import jse.plot.Plotters


/** 测试计算 BOOP，测试固液判断的阈值选择 */

// 设置线程数
final int nThreads = 4;

final double cutoffMul = 1.8;
final int nnn = -1;
final double connectThreshold = 0.84;
final int maxConnect = 32;
final int solidThreshold = 13;

final boolean onlyCu = true;
final boolean onlyZr = false;


// 首先导入 Lmpdat
def dataG   = Lmpdat.read('lmp/data/data-glass');
if (onlyCu) dataG = dataG.opt().filterType(1);
if (onlyZr) dataG = dataG.opt().filterType(2);
def dataFFS = Dump.read('lmp/.ffs-in/dump-fs1-new').last();
if (onlyCu) dataFFS = dataFFS.opt().filterType(1);
if (onlyZr) dataFFS = dataFFS.opt().filterType(2);
def dataMelt   = Lmpdat.read('lmp/.stableglass-in/data-last');
if (onlyCu) dataMelt = dataMelt.opt().filterType(1);
if (onlyZr) dataMelt = dataMelt.opt().filterType(2);
def dataMelt2  = Dump.read('lmp/.stableglass-in/dump-fs1-melt-fast').getMFPC(0.002).getMeanAtomData(1000, 50);
if (onlyCu) dataMelt2 = dataMelt2.opt().filterType(1);
if (onlyZr) dataMelt2 = dataMelt2.opt().filterType(2);

// 计算连接数向量
def connectCountG       = dataG      .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(6, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountFFS     = dataFFS    .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(6, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountMelt    = dataMelt   .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(6, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountMelt2   = dataMelt2  .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(6, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}

// 统计结果
def distributionG       = Vectors.zeros(maxConnect+1);
def distributionFFS     = Vectors.zeros(maxConnect+1);
def distributionMelt    = Vectors.zeros(maxConnect+1);
def distributionMelt2   = Vectors.zeros(maxConnect+1);

connectCountG       .forEach {distributionG         .increment(Math.min(maxConnect, (int)it));}
connectCountFFS     .forEach {distributionFFS       .increment(Math.min(maxConnect, (int)it));}
connectCountMelt    .forEach {distributionMelt      .increment(Math.min(maxConnect, (int)it));}
connectCountMelt2   .forEach {distributionMelt2     .increment(Math.min(maxConnect, (int)it));}
println("FFS solid number: ${distributionFFS[solidThreshold..maxConnect].sum()}");
println("Melt solid number: ${distributionMelt[solidThreshold..maxConnect].sum()}");
distributionG       /= dataG        .atomNum();
distributionFFS     /= dataFFS      .atomNum();
distributionMelt    /= dataMelt     .atomNum();
distributionMelt2   /= dataMelt2    .atomNum();

// 绘制结果
def plt = Plotters.get();

plt.plot(distributionG      , 'glass');
plt.plot(distributionFFS    , 'ffs');
plt.plot(distributionMelt   , 'melt');
plt.plot(distributionMelt2  , 'melt2');

plt.xlabel('connect count, n').ylabel('p(n)');
plt.axis(0, maxConnect, 0.0, 0.010);
plt.show();

