package test

import com.jtool.atom.Structures
import com.jtool.jobs.StepJobManager
import com.jtool.lmp.Lmpdat
import com.jtool.plot.Plotters
import com.jtool.vasp.POSCAR


/** 测试计算 BOOP，使用 ABOOP 判断结晶并绘制结果 */


// 首先导入 Lmpdat
def data = Lmpdat.read('lmp/data/data-glass');
//def data = Structures.from(POSCAR.read('lmp/data/MgCu2.poscar'), 4).opt().perturbXYZ(0.15);


new StepJobManager('testBOOP4', 1)
.init {println("0. 使用 q6 判断");}
.doJob {
    // 计算固体判断
    def isSolid;
    try (def mpc = data.getMPC()) {
        isSolid = mpc.calABOOP(6).greater(0.30);
    }
    println("solid number: ${isSolid.count()}");
    
    // 绘制晶体结构
    // 直接获取 xyz 数据
    def dataSTD = data.dataSTD();
    def type = dataSTD['type'];
    // 绘制
    def plt = Plotters.get();
    plt.plot(dataSTD['x'][~isSolid & type.equal(1)], dataSTD['y'][~isSolid & type.equal(1)], 'glass-Cu'  ).color(0.8, 0.6, 0.0).lineType('none').markerType('o').markerSize(3);
    plt.plot(dataSTD['x'][ isSolid & type.equal(1)], dataSTD['y'][ isSolid & type.equal(1)], 'crystal-Cu').color(0.5, 0.3, 0.0).lineType('none').markerType('o').markerSize(10);
    plt.plot(dataSTD['x'][~isSolid & type.equal(2)], dataSTD['y'][~isSolid & type.equal(2)], 'glass-Zr'  ).color(0.2, 0.6, 0.0).lineType('none').markerType('o').markerSize(4);
    plt.plot(dataSTD['x'][ isSolid & type.equal(2)], dataSTD['y'][ isSolid & type.equal(2)], 'crystal-Zr').color(0.1, 0.3, 0.0).lineType('none').markerType('o').markerSize(12);
    plt.show();
}
.then {println("1. 使用专门的内置方法判断");}
.doJob {
    // 计算固体判断
    def isSolid;
    try (def mpc = data.getMPC()) {
        isSolid = mpc.checkSolidQ6();
    }
    println("solid number: ${isSolid.count()}");
    
    // 绘制晶体结构
    // 直接获取 xyz 数据
    def dataSTD = data.dataSTD();
    def type = dataSTD['type'];
    // 绘制
    def plt = Plotters.get();
    plt.plot(dataSTD['x'][~isSolid & type.equal(1)], dataSTD['y'][~isSolid & type.equal(1)], 'glass-Cu'  ).color(0.8, 0.6, 0.0).lineType('none').markerType('o').markerSize(3);
    plt.plot(dataSTD['x'][ isSolid & type.equal(1)], dataSTD['y'][ isSolid & type.equal(1)], 'crystal-Cu').color(0.5, 0.3, 0.0).lineType('none').markerType('o').markerSize(10);
    plt.plot(dataSTD['x'][~isSolid & type.equal(2)], dataSTD['y'][~isSolid & type.equal(2)], 'glass-Zr'  ).color(0.2, 0.6, 0.0).lineType('none').markerType('o').markerSize(4);
    plt.plot(dataSTD['x'][ isSolid & type.equal(2)], dataSTD['y'][ isSolid & type.equal(2)], 'crystal-Zr').color(0.1, 0.3, 0.0).lineType('none').markerType('o').markerSize(12);
    plt.show();
}
.finish {println("Finished");}
;
