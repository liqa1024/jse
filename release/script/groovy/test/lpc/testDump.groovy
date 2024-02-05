package test.lpc

import jse.atom.Structures
import jse.code.UT
import jse.jobs.StepJobManager
import jse.lmp.Dump
import jse.lmp.LPC
import jse.math.MathEX
import jse.math.vector.Vectors
import jse.plot.Plotters
import jsex.rareevent.atom.ABOOPSolidChecker
import jsex.rareevent.atom.MultiTypeClusterSizeCalculator
import jse.system.WSL

import static jse.code.CS.*
import static jse.code.UT.Par.parfor

/** 测试熔融一个结构并输出 dump */

final int Cu = 15, Zr = 85;
final int initTemp          = Math.round(1600 + MathEX.Code.units(800, Cu+Zr, Zr, false));
final double cellSize       = 3.971 + Zr/(Cu+Zr) * 1.006;

final int meltTemp          = 1200;

final String initDataPath   = "lmp/.temp/data-init-Cu${Cu}Zr${Zr}";
final String meltDumpPath   = "lmp/.temp/dump-melt-${meltTemp}K-Cu${Cu}Zr${Zr}";

final String lmpExe         = '~/.local/bin/lmp';
final int lmpCores          = 12;



new StepJobManager('testFFS3c', 0)
.init {println("0. 初始化 ${initTemp}K 熔融 Cu${Cu}Zr${Zr} 结构");}
.doJob {
    UT.Timer.tic()
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr')) {
        lpc.runMelt(Structures.FCC(cellSize, 10).opt().mapTypeRandom(Cu, Zr), [MASS.Cu, MASS.Zr], initDataPath, initTemp, 500000);
    }
    UT.Timer.toc('initData');
}
.then {println("1. 维持 ${meltTemp}K 并持续输出 Cu${Cu}Zr${Zr} 结构");}
.doJob {
    UT.Timer.tic()
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr')) {
        lpc.runMeltDump(initDataPath, meltDumpPath, meltTemp, 1000000);
    }
    UT.Timer.toc('meltData');
}
.then {println("2. 计算 ${meltTemp}K 下 Cu${Cu}Zr${Zr} 结构的结晶情况");}
.doJob {
    // 读取 dump
    def dump = Dump.read(meltDumpPath);
    
    // 结果保存成 Vector
    def crystalSize = Vectors.zeros(dump.size());
    
    // 并行计算，新参数下的多种类团簇判断
    UT.Timer.tic();
    def calculator = new MultiTypeClusterSizeCalculator(
        new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
        [new ABOOPSolidChecker().setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
    );
    parfor(dump.size()) {int i ->
        // 统计结晶的数目
        crystalSize[i] = calculator.lambdaOf(dump[i]);
    }
    UT.Timer.toc('λ');
    
    def time = Vectors.linsequence(0, (dump[1].timeStep()-dump[0].timeStep())*0.002*0.001, dump.size());
    
    // 绘制
    def plt = Plotters.get();
    plt.plot(time, crystalSize      , 'λ');
    plt.xlabel('t [ns]').ylabel('cluster size');
    plt.xRange(0, time.last());
    plt.show();
}
.finish {}
;
