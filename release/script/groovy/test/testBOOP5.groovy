package test

import com.jtool.atom.Structures
import com.jtool.lmp.Lmpdat
import com.jtool.math.vector.Vectors
import com.jtool.plot.Plotters
import com.jtool.vasp.POSCAR


/** 测试计算 BOOP，测试固液判断的阈值选择 */

final int l = 6;
final double cutoffMul = 2.2;
final int nnn = -1;
final double connectThreshold = 0.88;
final int maxConnect = 64;
final int solidThreshold = 13;

final boolean onlyCu = false;
final boolean onlyZr = false;


// 首先导入 Lmpdat
def dataG       = Lmpdat.read('lmp/data/data-glass');
if (onlyCu) dataG = dataG.opt().filterType(1);
if (onlyZr) dataG = dataG.opt().filterType(2);
def dataFCC     = Structures.FCC(4.0, 8).opt().perturbXYZ(0.25);
def dataBCC     = Structures.BCC(4.0, 12).opt().perturbXYZ(0.32);
def dataHCP     = Structures.HCP(4.0, 8).opt().perturbXYZ(0.35);

def dataMgCu2   = Structures.from(POSCAR.read('lmp/data/MgCu2.poscar'), 5).opt().perturbXYZ(0.25);
if (onlyCu) dataMgCu2 = dataMgCu2.opt().filterType(2);
if (onlyZr) dataMgCu2 = dataMgCu2.opt().filterType(1);
def dataZr3Cu8  = Structures.from(POSCAR.read('lmp/data/Zr3Cu8.poscar'), 4).opt().perturbXYZ(0.25);
if (onlyCu) dataZr3Cu8 = dataZr3Cu8.opt().filterType(2);
if (onlyZr) dataZr3Cu8 = dataZr3Cu8.opt().filterType(1);
def dataZr7Cu10 = Structures.from(POSCAR.read('lmp/data/Zr7Cu10.poscar'), 4).opt().perturbXYZ(0.25);
if (onlyCu) dataZr7Cu10 = dataZr7Cu10.opt().filterType(2);
if (onlyZr) dataZr7Cu10 = dataZr7Cu10.opt().filterType(1);
def dataZrCu2   = Structures.from(POSCAR.read('lmp/data/ZrCu2.poscar'), 6).opt().perturbXYZ(0.25);
if (onlyCu) dataZrCu2 = dataZrCu2.opt().filterType(2);
if (onlyZr) dataZrCu2 = dataZrCu2.opt().filterType(1);

// 计算连接数向量
def connectCountG       = dataG      .getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountFCC     = dataFCC    .getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountBCC     = dataBCC    .getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountHCP     = dataHCP    .getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountMgCu2   = dataMgCu2  .getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountZr3Cu8  = dataZr3Cu8 .getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountZr7Cu10 = dataZr7Cu10.getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}
def connectCountZrCu2   = dataZrCu2  .getMPC().withCloseable {def mpc -> mpc.calConnectCountABOOP(l, connectThreshold, mpc.unitLen()*cutoffMul, nnn)}

// 统计结果
def distributionG       = Vectors.zeros(maxConnect+1);
def distributionFCC     = Vectors.zeros(maxConnect+1);
def distributionBCC     = Vectors.zeros(maxConnect+1);
def distributionHCP     = Vectors.zeros(maxConnect+1);
def distributionMgCu2   = Vectors.zeros(maxConnect+1);
def distributionZr3Cu8  = Vectors.zeros(maxConnect+1);
def distributionZr7Cu10 = Vectors.zeros(maxConnect+1);
def distributionZrCu2   = Vectors.zeros(maxConnect+1);

connectCountG       .forEach {distributionG         .increment(Math.min(maxConnect, (int)it));}
connectCountFCC     .forEach {distributionFCC       .increment(Math.min(maxConnect, (int)it));}
connectCountBCC     .forEach {distributionBCC       .increment(Math.min(maxConnect, (int)it));}
connectCountHCP     .forEach {distributionHCP       .increment(Math.min(maxConnect, (int)it));}
connectCountMgCu2   .forEach {distributionMgCu2     .increment(Math.min(maxConnect, (int)it));}
connectCountZr3Cu8  .forEach {distributionZr3Cu8    .increment(Math.min(maxConnect, (int)it));}
connectCountZr7Cu10 .forEach {distributionZr7Cu10   .increment(Math.min(maxConnect, (int)it));}
connectCountZrCu2   .forEach {distributionZrCu2     .increment(Math.min(maxConnect, (int)it));}
distributionG       /= dataG        .atomNum();
distributionFCC     /= dataFCC      .atomNum();
distributionBCC     /= dataBCC      .atomNum();
distributionHCP     /= dataHCP      .atomNum();
distributionMgCu2   /= dataMgCu2    .atomNum();
distributionZr3Cu8  /= dataZr3Cu8   .atomNum();
distributionZr7Cu10 /= dataZr7Cu10  .atomNum();
distributionZrCu2   /= dataZrCu2    .atomNum();
// 计算玻璃中判断为固体的百分比（保证在一个较小的不为零的值，如 0.5%）
println("solid prob in glass: ${distributionG[solidThreshold..maxConnect].sum()}");
// 输出不能分辨的
println('Can NOT Classify:');
if (distributionFCC     [solidThreshold..maxConnect].sum() < 0.9) println('fcc'     );
if (distributionBCC     [solidThreshold..maxConnect].sum() < 0.9) println('bcc'     );
if (distributionHCP     [solidThreshold..maxConnect].sum() < 0.9) println('hcp'     );
if (distributionMgCu2   [solidThreshold..maxConnect].sum() < 0.9) println('MgCu2'   );
if (distributionZr3Cu8  [solidThreshold..maxConnect].sum() < 0.9) println('Zr3Cu8'  );
if (distributionZr7Cu10 [solidThreshold..maxConnect].sum() < 0.9) println('Zr7Cu10' );
if (distributionZrCu2   [solidThreshold..maxConnect].sum() < 0.9) println('ZrCu2'   );

// 绘制结果
def plt = Plotters.get();

plt.plot(distributionG      , 'glass'   );
plt.plot(distributionFCC    , 'fcc'     );
plt.plot(distributionBCC    , 'bcc'     );
plt.plot(distributionHCP    , 'hcp'     );
plt.plot(distributionMgCu2  , 'MgCu2'   );
plt.plot(distributionZr3Cu8 , 'Zr3Cu8'  );
plt.plot(distributionZr7Cu10, 'Zr7Cu10' );
plt.plot(distributionZrCu2  , 'ZrCu2'   );

plt.xlabel('connect count, n').ylabel('p(n)');
plt.axis(0, maxConnect, 0.0, 0.301);
plt.xTick(4).yTick(0.05);
plt.show();

