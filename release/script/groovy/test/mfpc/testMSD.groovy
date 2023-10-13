package test.mfpc

import com.jtool.atom.Structures
import com.jtool.code.UT
import com.jtool.lmp.LPC
import com.jtool.math.MathEX
import com.jtool.math.table.Tables
import com.jtool.plot.Plotters
import com.jtool.system.WSL

import static com.jtool.code.CS.*

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
final boolean calMSD        = false;


if (genData) {
    String lmpExe   = '~/.local/bin/lmp';
    int lmpCores    = 12;
    
    UT.Timer.tic();
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr')) {
        lpc.runMelt(Structures.FCC(cellSize, 10).opt().mapTypeRandom(Cu, Zr), [MASS.Cu, MASS.Zr], FS1dataPath, meltTemp, 0.002, 500000);
        lpc.runMelt(FS1dataPath, FS1dataPath, 1000, 0.002, 500000 );
        lpc.runMelt(FS1dataPath, FS1dataPath, 800 , 0.002, 5000000);
    }
    try (def lpc = new LPC(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}", 'eam/fs', '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr')) {
        lpc.runMelt(Structures.FCC(cellSize, 10).opt().mapTypeRandom(Cu, Zr), [MASS.Cu, MASS.Zr], FS2dataPath, meltTemp, 0.002, 500000);
        lpc.runMelt(FS2dataPath, FS2dataPath, 1000, 0.002, 500000 );
        lpc.runMelt(FS2dataPath, FS2dataPath, 850 , 0.002, 5000000);
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
        
        def table = Tables.zeros(msd800Cu.first().size());
        table['t'] = msd800Cu.second();
        table['800-Cu'] = msd800Cu.first();
        table['800-Zr'] = msd800Zr.first();
        
        subCal = lpc.calMSD(FS1dataPath, 800).setDenseNormalized();
        msd800Cu = subCal.calType(1);
        msd800Zr = subCal.calType(2);
        table['800-Cu-2'] = msd800Cu.first();
        table['800-Zr-2'] = msd800Zr.first();
        
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
        
        def table = Tables.zeros(msd800Cu.first().size());
        table['t'] = msd800Cu.second();
        table['800-Cu'] = msd800Cu.first();
        table['800-Zr'] = msd800Zr.first();
        table['850-Cu'] = msd850Cu.first();
        table['850-Zr'] = msd850Zr.first();
        table['900-Cu'] = msd900Cu.first();
        table['900-Zr'] = msd900Zr.first();
        
        subCal = lpc.calMSD(FS2dataPath, 800).setDenseNormalized();
        msd800Cu = subCal.calType(1);
        msd800Zr = subCal.calType(2);
        subCal = lpc.calMSD(FS2dataPath, 850).setDenseNormalized();
        msd850Cu = subCal.calType(1);
        msd850Zr = subCal.calType(2);
        subCal = lpc.calMSD(FS2dataPath, 900).setDenseNormalized();
        msd900Cu = subCal.calType(1);
        msd900Zr = subCal.calType(2);
        
        table['800-Cu-2'] = msd800Cu.first();
        table['800-Zr-2'] = msd800Zr.first();
        table['850-Cu-2'] = msd850Cu.first();
        table['850-Zr-2'] = msd850Zr.first();
        table['900-Cu-2'] = msd900Cu.first();
        table['900-Zr-2'] = msd900Zr.first();
        
        UT.IO.table2csv(table, FS2MSDPath);
    }
    UT.Timer.toc('calMSD');
}


// 读取数据
def msdFS1 = UT.IO.csv2table(FS1MSDPath);
def msdFS2 = UT.IO.csv2table(FS2MSDPath);

// 绘制
def plt1 = Plotters.get();
plt1.loglog(msdFS1['t'], msdFS1['800-Cu']  , 'FS1-Cu, 800K'  ).color(0).marker('s');
plt1.loglog(msdFS2['t'], msdFS2['800-Cu']  , 'FS2-Cu, 800K'  ).color(1).marker('o');
plt1.loglog(msdFS2['t'], msdFS2['850-Cu']  , 'FS2-Cu, 850K'  ).color(2).marker('^');
plt1.loglog(msdFS2['t'], msdFS2['900-Cu']  , 'FS2-Cu, 900K'  ).color(3).marker('d');
plt1.loglog(msdFS1['t'], msdFS1['800-Cu-2'], 'FS1-Cu-2, 800K').color(0).lineType('--');
plt1.loglog(msdFS2['t'], msdFS2['800-Cu-2'], 'FS2-Cu-2, 800K').color(1).lineType('--');
plt1.loglog(msdFS2['t'], msdFS2['850-Cu-2'], 'FS2-Cu-2, 850K').color(2).lineType('--');
plt1.loglog(msdFS2['t'], msdFS2['900-Cu-2'], 'FS2-Cu-2, 900K').color(3).lineType('--');
plt1.xLabel('time[ps]').yLabel('msd');
plt1.show();

def plt2 = Plotters.get();
plt2.loglog(msdFS1['t'], msdFS1['800-Zr']  , 'FS1-Zr, 800K'  ).color(0).marker('s');
plt2.loglog(msdFS2['t'], msdFS2['800-Zr']  , 'FS2-Zr, 800K'  ).color(1).marker('o');
plt2.loglog(msdFS2['t'], msdFS2['850-Zr']  , 'FS2-Zr, 850K'  ).color(2).marker('^');
plt2.loglog(msdFS2['t'], msdFS2['900-Zr']  , 'FS2-Zr, 900K'  ).color(3).marker('d');
plt2.loglog(msdFS1['t'], msdFS1['800-Zr-2'], 'FS1-Zr-2, 800K').color(0).lineType('--');
plt2.loglog(msdFS2['t'], msdFS2['800-Zr-2'], 'FS2-Zr-2, 800K').color(1).lineType('--');
plt2.loglog(msdFS2['t'], msdFS2['850-Zr-2'], 'FS2-Zr-2, 850K').color(2).lineType('--');
plt2.loglog(msdFS2['t'], msdFS2['900-Zr-2'], 'FS2-Zr-2, 900K').color(3).lineType('--');
plt2.xLabel('time[ps]').yLabel('msd');
plt2.show();
