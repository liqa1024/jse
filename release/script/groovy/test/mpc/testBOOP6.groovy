package test.mpc

import jse.atom.Structures
import jse.code.UT
import jse.lmp.Dump
import jse.lmp.Lmpdat
import jse.plot.Plotters
import jsex.rareevent.atom.ABOOPSolidChecker
import jsex.rareevent.atom.ClusterSizeCalculator
import jsex.rareevent.atom.MultiTypeClusterSizeCalculator
import jse.vasp.POSCAR


/** 测试计算 BOOP，测试团簇计算器的效果 */

UT.Math.rng(123456789);

// 首先导入 Lmpdat
def dataG = Lmpdat.read('lmp/data/data-glass');
def dataC = Lmpdat.read('lmp/data/data-crystal');
def dataFFS = Dump.read('lmp/.ffs-in/dump-fs1').last();
def dataMgCu2   = Structures.from(POSCAR.read('vasp/data/MgCu2.poscar'      ).opt().mapType {3-it.type()}, 5).opt().perturbXYZ(0.25);
def dataZr3Cu8  = Structures.from(POSCAR.read('vasp/data/Zr3Cu8.poscar'     ).opt().mapType {3-it.type()}, 3).opt().perturbXYZ(0.25);
def dataZr7Cu10 = Structures.from(POSCAR.read('vasp/data/Zr7Cu10.poscar'    ).opt().mapType {3-it.type()}, 3).opt().perturbXYZ(0.25);
def dataZrCu2   = Structures.from(POSCAR.read('vasp/data/ZrCu2.poscar'      ).opt().mapType {3-it.type()}, 5).opt().perturbXYZ(0.25);
def dataZr14Cu51= Structures.from(POSCAR.read('vasp/data/re_Zr14Cu51.poscar').opt().mapType {3-it.type()}, 2).opt().perturbXYZ(0.25);

// 默认的团簇计算器
UT.Timer.tic();
def cal = new ClusterSizeCalculator();
println("default glass: ${cal.lambdaOf(dataG)}, total: ${dataG.atomNum()}");
println("default crystal: ${cal.lambdaOf(dataC)}, total: ${dataC.atomNum()}");
println("default ffs: ${cal.lambdaOf(dataFFS)}, total: ${dataFFS.atomNum()}");
println("default MgCu2: ${cal.lambdaOf(dataMgCu2)}, total: ${dataMgCu2.atomNum()}");
println("default Zr3Cu8: ${cal.lambdaOf(dataZr3Cu8)}, total: ${dataZr3Cu8.atomNum()}");
println("default Zr7Cu10: ${cal.lambdaOf(dataZr7Cu10)}, total: ${dataZr7Cu10.atomNum()}");
println("default ZrCu2: ${cal.lambdaOf(dataZrCu2)}, total: ${dataZrCu2.atomNum()}");
println("default Zr14Cu51: ${cal.lambdaOf(dataZr14Cu51)}, total: ${dataZr14Cu51.atomNum()}");
UT.Timer.toc();
// default glass: 4.0, total: 4000
// default crystal: 3780.0, total: 4000
// default ffs: 6.0, total: 13500
// default MgCu2: 0.0, total: 3000
// default Zr3Cu8: 0.0, total: 1188
// default Zr7Cu10: 19.0, total: 1836
// default ZrCu2: 1447.0, total: 1500
// default Zr14Cu51: 42.0, total: 4160
// Total time: 00 hour 00 min 0.46 sec

// 用于计算合金的团簇计算器
UT.Timer.tic();
def calMulti = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker().setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);
println("multi glass: ${calMulti.lambdaOf(dataG)}, total: ${dataG.atomNum()}");
println("multi crystal: ${calMulti.lambdaOf(dataC)}, total: ${dataC.atomNum()}");
println("multi ffs: ${calMulti.lambdaOf(dataFFS)}, total: ${dataFFS.atomNum()}");
println("multi MgCu2: ${calMulti.lambdaOf(dataMgCu2)}, total: ${dataMgCu2.atomNum()}");
println("multi Zr3Cu8: ${calMulti.lambdaOf(dataZr3Cu8)}, total: ${dataZr3Cu8.atomNum()}");
println("multi Zr7Cu10: ${calMulti.lambdaOf(dataZr7Cu10)}, total: ${dataZr7Cu10.atomNum()}");
println("multi ZrCu2: ${calMulti.lambdaOf(dataZrCu2)}, total: ${dataZrCu2.atomNum()}");
println("multi Zr14Cu51: ${calMulti.lambdaOf(dataZr14Cu51)}, total: ${dataZr14Cu51.atomNum()}");
UT.Timer.toc();
// multi glass: 1.0, total: 4000
// multi crystal: 4000.0, total: 4000
// multi ffs: 65.0, total: 13500
// multi MgCu2: 3000.0, total: 3000
// multi Zr3Cu8: 1188.0, total: 1188
// multi Zr7Cu10: 1830.0, total: 1836
// multi ZrCu2: 1500.0, total: 1500
// multi Zr14Cu51: 4160.0, total: 4160
// Total time: 00 hour 00 min 0.43 sec

UT.Timer.tic();
for (_ in 0..<100) calMulti.lambdaOf(dataFFS);
UT.Timer.toc();
// Total time: 00 hour 00 min 10.96 sec


//def isSolid = calMulti.getIsSolid_(dataFFS.last().getMPC(), dataFFS.last());
//// 绘制晶体结构
//// 直接获取 xyz 数据
//def dataSTD = dataFFS.last().dataSTD();
//def type = dataSTD['type'];
//// 绘制
//def plt = Plotters.get();
//plt.plot(dataSTD['x'][~isSolid & type.equal(1)], dataSTD['y'][~isSolid & type.equal(1)], 'glass-Cu'  ).color(0.8, 0.6, 0.0).lineType('none').markerType('o').markerSize(3);
//plt.plot(dataSTD['x'][ isSolid & type.equal(1)], dataSTD['y'][ isSolid & type.equal(1)], 'crystal-Cu').color(0.5, 0.3, 0.0).lineType('none').markerType('o').markerSize(10);
//plt.plot(dataSTD['x'][~isSolid & type.equal(2)], dataSTD['y'][~isSolid & type.equal(2)], 'glass-Zr'  ).color(0.2, 0.6, 0.0).lineType('none').markerType('o').markerSize(4);
//plt.plot(dataSTD['x'][ isSolid & type.equal(2)], dataSTD['y'][ isSolid & type.equal(2)], 'crystal-Zr').color(0.1, 0.3, 0.0).lineType('none').markerType('o').markerSize(12);
//plt.show();

