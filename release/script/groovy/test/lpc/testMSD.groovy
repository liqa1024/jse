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
final int Cu = 60, Zr = 40;
final int meltTemp          = Math.round(1600 + MathEX.Code.units(800, Cu+Zr, Zr, false));
final double cellSize       = 3.971 + Zr/(Cu+Zr) * 1.006;

final String FS1dataPath    = 'lmp/.temp/data-fs1';
final String FS2dataPath    = 'lmp/.temp/data-fs2';

final String FS1MSDPath     = 'lmp/.temp/msd-fs1.csv';
final String FS2MSDPath     = 'lmp/.temp/msd-fs2.csv';

final boolean genData       = false;
final boolean calMSD        = true;


if (genData) {
    String lmpExe   = '~/.local/bin/lmp';
    int lmpCores    = 12;
    
    UT.Timer.tic();
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr')) {
        lpc.runMelt(Structures.FCC(cellSize, 10).opt().mapTypeRandom(Cu, Zr), [MASS.Cu, MASS.Zr], FS1dataPath, meltTemp, 500000);
        lpc.runMelt(FS1dataPath, FS1dataPath, 1000, 500000 );
        lpc.runMelt(FS1dataPath, FS1dataPath, 800 , 5000000);
    }
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr')) {
        lpc.runMelt(Structures.FCC(cellSize, 10).opt().mapTypeRandom(Cu, Zr), [MASS.Cu, MASS.Zr], FS2dataPath, meltTemp, 500000);
        lpc.runMelt(FS2dataPath, FS2dataPath, 1000, 500000 );
        lpc.runMelt(FS2dataPath, FS2dataPath, 850 , 5000000);
    }
    UT.Timer.toc('genData');
}


if (calMSD) {
    String lmpExe   = '~/.local/bin/lmp';
    int lmpCores    = 12;
    
    UT.Timer.tic();
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr')) {
        def subCal = lpc.calMSD(FS1dataPath, 800).setDenseNormalized();
        def msd800Cu = subCal.calType(1);
        def msd800Zr = subCal.calType(2);
        
        def table = Tables.zeros(msd800Cu.Nx());
        table['t'] = msd800Cu.x();
        table['800-Cu'] = msd800Cu.f();
        table['800-Zr'] = msd800Zr.f();
        
        UT.IO.table2csv(table, FS1MSDPath);
    }
    
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr')) {
        def subCal = lpc.calMSD(FS2dataPath, 800).setDenseNormalized();
        def msd800Cu = subCal.calType(1);
        def msd800Zr = subCal.calType(2);
        subCal = lpc.calMSD(FS2dataPath, 850).setDenseNormalized();
        def msd850Cu = subCal.calType(1);
        def msd850Zr = subCal.calType(2);
        subCal = lpc.calMSD(FS2dataPath, 900).setDenseNormalized();
        def msd900Cu = subCal.calType(1);
        def msd900Zr = subCal.calType(2);
        
        def table = Tables.zeros(msd800Cu.Nx());
        table['t'] = msd800Cu.x();
        table['800-Cu'] = msd800Cu.f();
        table['800-Zr'] = msd800Zr.f();
        table['850-Cu'] = msd850Cu.f();
        table['850-Zr'] = msd850Zr.f();
        table['900-Cu'] = msd900Cu.f();
        table['900-Zr'] = msd900Zr.f();
        
        UT.IO.table2csv(table, FS2MSDPath);
    }
    UT.Timer.toc('calMSD');
}


// 读取数据
def msdFS1 = UT.IO.csv2table(FS1MSDPath);
def msdFS2 = UT.IO.csv2table(FS2MSDPath);

// 绘制
def plt1 = Plotters.get();
plt1.loglog(msdFS1['t'], msdFS1['800-Cu'], 'FS1-Cu, 800K').marker('s');
plt1.loglog(msdFS2['t'], msdFS2['800-Cu'], 'FS2-Cu, 800K').marker('o');
plt1.loglog(msdFS2['t'], msdFS2['850-Cu'], 'FS2-Cu, 850K').marker('^');
plt1.loglog(msdFS2['t'], msdFS2['900-Cu'], 'FS2-Cu, 900K').marker('d');
plt1.xLabel('time[ps]').yLabel('msd');
plt1.show();

def plt2 = Plotters.get();
plt2.loglog(msdFS1['t'], msdFS1['800-Zr'], 'FS1-Zr, 800K').marker('s');
plt2.loglog(msdFS2['t'], msdFS2['800-Zr'], 'FS2-Zr, 800K').marker('o');
plt2.loglog(msdFS2['t'], msdFS2['850-Zr'], 'FS2-Zr, 850K').marker('^');
plt2.loglog(msdFS2['t'], msdFS2['900-Zr'], 'FS2-Zr, 900K').marker('d');
plt2.xLabel('time[ps]').yLabel('msd');
plt2.show();
