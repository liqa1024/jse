package test.mfpc

import com.jtool.code.UT
import com.jtool.lmp.Dump
import com.jtool.lmp.LmpExecutor
import com.jtool.lmp.LmpIn
import com.jtool.lmp.Lmpdat
import com.jtool.plot.Plotters
import com.jtool.system.WSL

import static com.jtool.code.CS.*
import static com.jtool.code.UT.Code.randSeed

/** 测试计算 MSD */

// 需要先运行一下 lammps 来创建 dump
final String lmpExe         = '~/.local/bin/lmp';
final int lmpCores          = 12;
final double timestep       = 0.001;

final String FS1inDataPath  = 'lmp/.ffs-in/data-fs1-init';
final String FS1dumpPath    = 'lmp/.temp/msd-fs1';

final String FS2inDataPath  = 'lmp/.ffs-in/data-fs2-init';
final String FS2dumpPath800 = 'lmp/.temp/msd-fs2-800';
final String FS2dumpPath850 = 'lmp/.temp/msd-fs2-850';
final String FS2dumpPath900 = 'lmp/.temp/msd-fs2-900';

final boolean runLmp        = false;

if (runLmp) try (def lmp = new LmpExecutor(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}")) {
    // 设置参数
    def lmpIn = LmpIn.DUMP_MELT_NPT_Cu();
    lmpIn.vT = 800;
    lmpIn.vTimestep = timestep;
    lmpIn.vRunStep = 100000;
    lmpIn.vDumpStep = 100;
    lmpIn.vThermoStep = 1000;
    lmpIn.pair_style = 'eam/fs';
    lmpIn.pair_coeff = '* * lmp/.potential/Cu-Zr_2.eam.fs Cu Zr';
    // 如果 data 有速率的话才移除 velocity 设定保留速率
    lmpIn.velocity = Lmpdat.read(FS1inDataPath).hasVelocities() ? REMOVE : KEEP;
    // 执行并获取输出
    lmpIn.vSeed = randSeed();
    lmpIn.vInDataPath = FS1inDataPath;
    lmpIn.vDumpPath = FS1dumpPath;
    lmp.run(lmpIn);
    
    // 设置参数
    lmpIn.vT = 800;
    lmpIn.pair_style = 'eam/fs';
    lmpIn.pair_coeff = '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr';
    // 如果 data 有速率的话才移除 velocity 设定保留速率
    lmpIn.velocity = Lmpdat.read(FS2inDataPath).hasVelocities() ? REMOVE : KEEP;
    // 执行并获取输出
    lmpIn.vSeed = randSeed();
    lmpIn.vInDataPath = FS2inDataPath;
    lmpIn.vDumpPath = FS2dumpPath800;
    lmp.run(lmpIn);

    // 设置参数
    lmpIn.vT = 850;
    lmpIn.pair_style = 'eam/fs';
    lmpIn.pair_coeff = '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr';
    // 由于初始速率对应温度不同，这里直接抹除
    lmpIn.velocity = REMOVE;
    // 执行并获取输出
    lmpIn.vSeed = randSeed();
    lmpIn.vInDataPath = FS2inDataPath;
    lmpIn.vDumpPath = FS2dumpPath850;
    lmp.run(lmpIn);
    
    // 设置参数
    lmpIn.vT = 900;
    lmpIn.pair_style = 'eam/fs';
    lmpIn.pair_coeff = '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr';
    // 由于初始速率对应温度不同，这里直接抹除
    lmpIn.velocity = REMOVE;
    // 执行并获取输出
    lmpIn.vSeed = randSeed();
    lmpIn.vInDataPath = FS2inDataPath;
    lmpIn.vDumpPath = FS2dumpPath900;
    lmp.run(lmpIn);
}


// 设置线程数
final int nThreads = 4;

// 运行完成后绘制，这里统一将密度归一化
def dumpFS1 = Dump.read(FS1dumpPath).setDenseNormalized();
UT.Timer.tic();
def msdFS1 = dumpFS1.getMFPC(timestep, nThreads).withCloseable {it.calMSD()}
UT.Timer.toc("${nThreads} threads, FS1 MSD");

def dumpFS2800 = Dump.read(FS2dumpPath800).setDenseNormalized();
UT.Timer.tic();
def msdFS2800 = dumpFS2800.getMFPC(timestep, nThreads).withCloseable {it.calMSD()}
UT.Timer.toc("${nThreads} threads, FS2 MSD 800");

def dumpFS2850 = Dump.read(FS2dumpPath850).cutFront(100).setDenseNormalized();
UT.Timer.tic();
def msdFS2850 = dumpFS2850.getMFPC(timestep, nThreads).withCloseable {it.calMSD()}
UT.Timer.toc("${nThreads} threads, FS2 MSD 850");

def dumpFS2900 = Dump.read(FS2dumpPath900).cutFront(100).setDenseNormalized();
UT.Timer.tic();
def msdFS2900 = dumpFS2900.getMFPC(timestep, nThreads).withCloseable {it.calMSD()}
UT.Timer.toc("${nThreads} threads, FS2 MSD 900");

def plt = Plotters.get();
plt.loglog(msdFS1   .first(), msdFS1   .second(), 'FS1, 800K').marker('s');
plt.loglog(msdFS2800.first(), msdFS2800.second(), 'FS2, 800K').marker('o');
plt.loglog(msdFS2850.first(), msdFS2850.second(), 'FS2, 850K').marker('^');
plt.loglog(msdFS2900.first(), msdFS2900.second(), 'FS2, 900K').marker('d');
plt.xLabel('time[ps]').yLabel('msd');
plt.show();
