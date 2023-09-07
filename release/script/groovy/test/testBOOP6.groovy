package test

import com.jtool.atom.Structures
import com.jtool.code.UT
import com.jtool.lmp.Dump
import com.jtool.lmp.Lmpdat
import com.jtool.plot.Plotters
import com.jtool.rareevent.atom.ABOOPSolidChecker
import com.jtool.rareevent.atom.ClusterSizeCalculator
import com.jtool.rareevent.atom.MultiTypeClusterSizeCalculator
import com.jtool.vasp.POSCAR


/** 测试计算 BOOP，测试团簇计算器的效果 */


// 首先导入 Lmpdat
def dataG = Lmpdat.read('lmp/data/data-glass');
def dataC = Lmpdat.read('lmp/data/data-crystal');
def dataFFS = Dump.read('lmp/.ffs-in/dump-fs1').last();
def dataMgCu2   = Structures.from(POSCAR.read('lmp/data/MgCu2.poscar'   ).opt().mapType {3-it.type()}, 5).opt().perturbXYZ(0.25);
def dataZr3Cu8  = Structures.from(POSCAR.read('lmp/data/Zr3Cu8.poscar'  ).opt().mapType {3-it.type()}, 3).opt().perturbXYZ(0.25);
def dataZr7Cu10 = Structures.from(POSCAR.read('lmp/data/Zr7Cu10.poscar' ).opt().mapType {3-it.type()}, 3).opt().perturbXYZ(0.25);
def dataZrCu2   = Structures.from(POSCAR.read('lmp/data/ZrCu2.poscar'   ).opt().mapType {3-it.type()}, 5).opt().perturbXYZ(0.25);

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
UT.Timer.toc();

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
UT.Timer.toc();



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

