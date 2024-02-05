package test.mpc

import jse.lmp.Dump
import jse.lmp.LmpExecutor
import jse.lmp.LmpIn
import jse.lmp.Lmpdat
import jse.system.WSL

/**
 * 测试 BOOP 结果是否和 lammps 的一致
 */


final def dataPath = 'lmp/data/data-crystal';
final def dumpPath = 'lmp/.temp/out-cal';

// 读取需要计算的结构
def mpc = Lmpdat.read(dataPath).getMPC(4);

// 先使用 lammps 计算
final def lmpExe = '~/.local/bin/lmp';
final int lmpCores = 1;
try (def lmp = new LmpExecutor(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}")) {
    def calQ6In = LmpIn.of('lmp/in/cal-BOOP')
    .i('vInDataPath', 'lmp/data/CuFCC108.lmpdat')
    .o('vDumpPath', 'lmp/.temp/Cu108.lammpstrj')
    ;
    calQ6In.vCutoff = mpc.unitLen()*1.5;
    calQ6In.vL = 6;
    
    calQ6In.vInDataPath = dataPath;
    calQ6In.vDumpPath = dumpPath;
    lmp.run(calQ6In);
}
// 读取计算结果
def dump = Dump.read(dumpPath);
println("Mean of Q6 lmp: ${dump.first().asTable()['c_Ql[1]'].mean()}");
// Mean of Q6 lmp: 0.4266242822499992


// 直接计算结果
println("Mean of Q6 jse: ${mpc.calBOOP(6, mpc.unitLen()*1.5).mean()}");
// Mean of Q6 jse: 0.4266242779918842

// 重复计算检测缓存的正确性
println("Mean of Q6 jse: ${mpc.calBOOP(6, mpc.unitLen()*1.5).mean()}");
// Mean of Q6 jse: 0.4266242779918842
println("Mean of Q4 jse: ${mpc.calBOOP(4, mpc.unitLen()*1.5).mean()}");
// Mean of Q4 jse: 0.07240307373895653

mpc.shutdown();
