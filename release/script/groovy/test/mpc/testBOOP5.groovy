package test.mpc

import jse.atom.Structures
import jse.code.UT
import jse.lmp.Lmpdat
import jse.math.table.Tables
import jse.math.vector.Vectors
import jse.plot.Plotters
import jse.vasp.POSCAR


/** 测试计算 BOOP，测试固液判断的阈值选择 */

// 设置线程数
final int nThreads = 4;

final int l = 6;
final double cutoffMul = 1.5;
final int nnn = -1;
final double connectThreshold = 0.84;
final int maxConnect = 16;
final int solidThreshold = 7;

final boolean onlyCu = false;
final boolean onlyZr = true;

final double perturbMul = 1.0;


// 首先导入 Lmpdat
def dataG       = Lmpdat.read('lmp/.CuZr/data-nolaves-800');
if (onlyCu) dataG = dataG.opt().filterType(1);
if (onlyZr) dataG = dataG.opt().filterType(2);
def dataFCC     = Structures.FCC(4.0, 8).opt().perturbXYZ(0.25*perturbMul);
def dataBCC     = Structures.BCC(4.0, 12).opt().perturbXYZ(0.32*perturbMul);
def dataHCP     = Structures.HCP(4.0, 8).opt().perturbXYZ(0.35*perturbMul);

def dataMgCu2   = Structures.from(POSCAR.read('vasp/data/MgCu2.poscar'), 5).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataMgCu2 = dataMgCu2.opt().filterType(2);
if (onlyZr) dataMgCu2 = dataMgCu2.opt().filterType(1);
def dataZr3Cu8  = Structures.from(POSCAR.read('vasp/data/Zr3Cu8.poscar'), 4).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataZr3Cu8 = dataZr3Cu8.opt().filterType(2);
if (onlyZr) dataZr3Cu8 = dataZr3Cu8.opt().filterType(1);
def dataZr7Cu10 = Structures.from(POSCAR.read('vasp/data/Zr7Cu10.poscar'), 4).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataZr7Cu10 = dataZr7Cu10.opt().filterType(2);
if (onlyZr) dataZr7Cu10 = dataZr7Cu10.opt().filterType(1);
def dataZrCu2   = Structures.from(POSCAR.read('vasp/data/ZrCu2.poscar'), 6).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataZrCu2 = dataZrCu2.opt().filterType(2);
if (onlyZr) dataZrCu2 = dataZrCu2.opt().filterType(1);
def dataZr14Cu51= Structures.from(POSCAR.read('vasp/data/re_Zr14Cu51.poscar'), 3).opt().perturbXYZ(0.25*perturbMul);
if (onlyCu) dataZr14Cu51 = dataZr14Cu51.opt().filterType(2);
if (onlyZr) dataZr14Cu51 = dataZr14Cu51.opt().filterType(1);

// 计算连接数向量
def connectCountG       = dataG       .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountFCC     = dataFCC     .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountBCC     = dataBCC     .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountHCP     = dataHCP     .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountMgCu2   = dataMgCu2   .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountZr3Cu8  = dataZr3Cu8  .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountZr7Cu10 = dataZr7Cu10 .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountZrCu2   = dataZrCu2   .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountZr14Cu51= dataZr14Cu51.getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountG2      = dataG       .getMPC(nThreads).withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}

// 统计结果
def distributionG       = Vectors.zeros(maxConnect+1);
def distributionFCC     = Vectors.zeros(maxConnect+1);
def distributionBCC     = Vectors.zeros(maxConnect+1);
def distributionHCP     = Vectors.zeros(maxConnect+1);
def distributionMgCu2   = Vectors.zeros(maxConnect+1);
def distributionZr3Cu8  = Vectors.zeros(maxConnect+1);
def distributionZr7Cu10 = Vectors.zeros(maxConnect+1);
def distributionZrCu2   = Vectors.zeros(maxConnect+1);
def distributionZr14Cu51= Vectors.zeros(maxConnect+1);
def distributionG2      = Vectors.zeros(maxConnect+1);

connectCountG       .forEach {distributionG         .increment(Math.min(maxConnect, (int)it));}
connectCountFCC     .forEach {distributionFCC       .increment(Math.min(maxConnect, (int)it));}
connectCountBCC     .forEach {distributionBCC       .increment(Math.min(maxConnect, (int)it));}
connectCountHCP     .forEach {distributionHCP       .increment(Math.min(maxConnect, (int)it));}
connectCountMgCu2   .forEach {distributionMgCu2     .increment(Math.min(maxConnect, (int)it));}
connectCountZr3Cu8  .forEach {distributionZr3Cu8    .increment(Math.min(maxConnect, (int)it));}
connectCountZr7Cu10 .forEach {distributionZr7Cu10   .increment(Math.min(maxConnect, (int)it));}
connectCountZrCu2   .forEach {distributionZrCu2     .increment(Math.min(maxConnect, (int)it));}
connectCountZr14Cu51.forEach {distributionZr14Cu51  .increment(Math.min(maxConnect, (int)it));}
connectCountG2      .forEach {distributionG2        .increment(Math.min(maxConnect, (int)it));}
distributionG       /= dataG        .atomNum();
distributionFCC     /= dataFCC      .atomNum();
distributionBCC     /= dataBCC      .atomNum();
distributionHCP     /= dataHCP      .atomNum();
distributionMgCu2   /= dataMgCu2    .atomNum();
distributionZr3Cu8  /= dataZr3Cu8   .atomNum();
distributionZr7Cu10 /= dataZr7Cu10  .atomNum();
distributionZrCu2   /= dataZrCu2    .atomNum();
distributionZr14Cu51/= dataZr14Cu51 .atomNum();
distributionG2      /= dataG        .atomNum();

// 输出一些具体值用于 debug
println("prob 0 in glass: ${distributionG[0]}");
println("prob 0 in glass: ${distributionG2[0]}");
// prob 0 in glass: 0.6687037037037037
println("prob 1 in glass: ${distributionG[1]}");
println("prob 1 in glass: ${distributionG2[1]}");
// prob 1 in glass: 0.22074074074074074
println("prob 2 in glass: ${distributionG[2]}");
println("prob 2 in glass: ${distributionG2[2]}");
// prob 2 in glass: 0.06962962962962962

// 计算玻璃中判断为固体的百分比（保证在一个较小的不为零的值，如 0.5%）
println("solid prob in glass: ${distributionG[solidThreshold..maxConnect].sum()}");
println("solid prob in glass: ${distributionG2[solidThreshold..maxConnect].sum()}");
// solid prob in glass: 1.8518518518518518E-4

// 输出不能分辨的
println('Can NOT Classify:');
if (distributionFCC     [solidThreshold..maxConnect].sum() < 0.9) println('fcc'     );
if (distributionBCC     [solidThreshold..maxConnect].sum() < 0.9) println('bcc'     );
if (distributionHCP     [solidThreshold..maxConnect].sum() < 0.9) println('hcp'     );
if (distributionMgCu2   [solidThreshold..maxConnect].sum() < 0.9) println('MgCu2'   );
if (distributionZr3Cu8  [solidThreshold..maxConnect].sum() < 0.9) println('Zr3Cu8'  );
if (distributionZr7Cu10 [solidThreshold..maxConnect].sum() < 0.9) println('Zr7Cu10' );
if (distributionZrCu2   [solidThreshold..maxConnect].sum() < 0.9) println('ZrCu2'   );
if (distributionZr14Cu51[solidThreshold..maxConnect].sum() < 0.9) println('Zr14Cu51');

// 正式图片，现在需要存一下 csv
def data = Tables.zeros(distributionG.size());
data['n'] = 0..maxConnect;
data['glass'      ] = distributionG       ;
data['fcc'        ] = distributionFCC     ;
data['bcc'        ] = distributionBCC     ;
data['hcp'        ] = distributionHCP     ;
data['laves-MgCu2'] = distributionMgCu2   ;
data['Cu8Zr3'     ] = distributionZr3Cu8  ;
data['Cu10Zr7'    ] = distributionZr7Cu10 ;
data['Cu2Zr'      ] = distributionZrCu2   ;
data['Zr14Cu51'   ] = distributionZr14Cu51;
UT.IO.table2csv(data, '.temp/Sij-new-Zr.csv');

// 绘制结果
def plt = Plotters.get();

plt.plot(distributionG       , 'liquid'      ).marker('o').lineType('-' );
plt.plot(distributionFCC     , 'fcc'         ).marker('s').lineType('--').lineWidth(1.0).markerSize(10.0);
plt.plot(distributionBCC     , 'bcc'         ).marker('d').lineType('--').lineWidth(1.0).markerSize(10.0);
plt.plot(distributionHCP     , 'hcp'         ).marker('^').lineType('--').lineWidth(1.0).markerSize(10.0);
plt.plot(distributionMgCu2   , 'laves-MgCu2' ).marker('*').lineType('-' );
plt.plot(distributionZr3Cu8  , 'Cu8Zr3'      ).marker('s').lineType('-' );
plt.plot(distributionZr7Cu10 , 'Cu10Zr7'     ).marker('d').lineType('-' );
plt.plot(distributionZrCu2   , 'Cu2Zr'       ).marker('^').lineType('-' );
plt.plot(distributionZr14Cu51, 'Cu51Zr14'    ).marker('s').lineType('-' );
plt.plot([solidThreshold, solidThreshold], [0, 1], "n = ${solidThreshold}").color('r').lineType('-.');

plt.xlabel('connect count, n').ylabel('p(n)');
plt.axis(0, maxConnect, 0.0, 0.501);
plt.show();

