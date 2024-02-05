package test.lpc

import jse.atom.Structures
import jse.code.UT
import jse.lmp.LPC
import jse.math.MathEX
import jse.math.table.Tables
import jse.plot.Plotters
import jse.system.WSL

import static jse.code.CS.*

/** 测试计算 MSD */

// 需要先运行一下 lammps 来创建 dump
final int Cu = 15, Zr = 85;
final int initTemp          = Math.round(1600 + MathEX.Code.units(800, Cu+Zr, Zr, false));
final double cellSize       = 3.971 + Zr/(Cu+Zr) * 1.006;

final int meltTemp          = 1200;

final String dataPath    = "lmp/.temp/data-fs2-${meltTemp}K-Cu${Cu}Zr${Zr}";
final String MSDPath     = "lmp/.temp/msd-fs2-${meltTemp}K-Cu${Cu}Zr${Zr}.csv";

final boolean genData       = true;
final boolean calMSD        = true;


if (genData) {
    String lmpExe   = '~/.local/bin/lmp';
    int lmpCores    = 12;
    
    UT.Timer.tic();
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr')) {
        lpc.runMelt(Structures.FCC(cellSize, 10).opt().mapTypeRandom(Cu, Zr), [MASS.Cu, MASS.Zr], dataPath, initTemp, 500000);
        lpc.runMelt(dataPath, dataPath, meltTemp, 500000);
    }
    UT.Timer.toc('genData');
}


if (calMSD) {
    String lmpExe   = '~/.local/bin/lmp';
    int lmpCores    = 12;
    
    UT.Timer.tic();
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr')) {
        def subCal = lpc.calMSD(dataPath, meltTemp).setDenseNormalized();
        def msdCu = subCal.calType(1);
        def msdZr = subCal.calType(2);
        
        def table = Tables.zeros(msdCu.Nx());
        table['t'] = msdCu.x();
        table['Cu'] = msdCu.f();
        table['Zr'] = msdZr.f();
        UT.IO.table2csv(table, MSDPath);
    }
    UT.Timer.toc('calMSD');
}


// 读取数据
def msd = UT.IO.csv2table(MSDPath);

// 绘制
def plt = Plotters.get();
plt.loglog(msd['t'], msd['Cu'], "Cu, ${meltTemp}K").marker('s');
plt.loglog(msd['t'], msd['Zr'], "Zr, ${meltTemp}K").marker('o');
plt.xLabel('time[ps]').yLabel('msd');
plt.show();

