package test.ffs

import jtool.code.UT
import jtool.lmp.Dump
import jtool.lmp.Lmpdat
import jtool.math.vector.Vectors
import jtool.parallel.MPI
import jtoolex.rareevent.atom.ABOOPSolidChecker_MPI
import jtoolex.rareevent.atom.MultiTypeClusterSizeCalculator
import jtoolex.rareevent.lmp.MultipleNativeLmpFullPathGenerator

import static jtool.code.CS.MASS
import static jtool.code.UT.Code.percent
import static jtool.code.UT.Code.range

/**
 * lammps 跑 FFS 的实例；
 * 此脚本需要原生 lammps 环境
 */


/** 体系参数 */
final int Cu = 15;
final int Zr = 100-Cu;
final int replicate = 10; // 共 4x10x10x10 = 4000 原子
final def pairStyle = 'eam/fs';
final def pairCoeff = '* * lmp/.potential/Cu-Zr_4.eam.fs Cu Zr';

final int atomNum = replicate*replicate*replicate*4;

/** 模拟参数 */
final def UNIQUE_NAME = args ? args[0] : 'CuZr-FFS';

final double timestep   = 0.002;
final int SCTemp        = 1200;

final def workingDir   = "lmp/.temp/${UNIQUE_NAME}/Cu${Cu}Zr${Zr}/";

final def SCDir             = workingDir+'supercooling/';
final def SCOutDataPath     = SCDir+'data-out';

final def FFSDumpPath       = workingDir+'dump-0';

/** FFS 参数 */
final int parallelNum       = 8;
final int dumpStep          = 10; // 0.02 ps


// 读取结构
def initPoints = range(parallelNum).collect {Lmpdat.read("${SCOutDataPath}-${it}")};

def dumpCal = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker_MPI().setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker_MPI().setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker_MPI().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);
//def fullPathGen = new BufferedFullPathGenerator(new DumpPathGenerator(new LmpExecutor(new WSL().setNoSTDOutput(), "mpiexec -np 4 ~/.local/bin/lmp"), initPoints, [MASS.Cu, MASS.Zr], SCTemp, pairStyle, pairCoeff, timestep, dumpStep, 10), dumpCal);


MPI.initThread(args, MPI.Thread.MULTIPLE);
MPI.Comm.WORLD.barrier();
MultipleNativeLmpFullPathGenerator.withOf(MPI.Comm.WORLD, [0], dumpCal, initPoints, [MASS.Cu, MASS.Zr], SCTemp, pairStyle, pairCoeff, timestep, dumpStep) {fullPathGen ->
    // 增加对 fullPathGen 的单元耗时统计
    fullPathGen.initTimer();
    
    def dump = new Dump();
    try (def it = fullPathGen.fullPathInit(123456)) {
        UT.Timer.tic()
        for (_ in range(100)) {
            def data = it.next();
            def time = it.timeConsumed();
            def lambda = it.lambda();
            println("time: ${time}, lambda: ${lambda}");
            dump.append(data);
        }
        UT.Timer.toc()
    }
    dump.write(FFSDumpPath);
    
    // 输出 fullPathGen 的耗时
    def info = fullPathGen.getTimerInfo();
    println("FullPathGenTime: lmp = ${percent(info.lmp/info.total)}, lambda = ${percent(info.lambda/info.total)}, wait = ${percent(info.wait/info.total)}, else = ${percent((info.other)/info.total)}");
}
MPI.Comm.WORLD.barrier();
// Native (mpi np 1):
// time: 1.94, lambda: 9.0
// time: 1.96, lambda: 10.0
// time: 1.98, lambda: 11.0
// Total time: 00 hour 00 min 19.89 sec / 14.55 sec / 13.99 sec
// Native (mpi np 4):
// time: 1.94, lambda: 9.0
// time: 1.96, lambda: 10.0
// time: 1.98, lambda: 11.0
// Total time: 00 hour 00 min 8.05 sec / 6.35 sec / 6.07 sec

// WSL (mpi np 1):
// time: 1.94, lambda: 17.0
// time: 1.9599999999999997, lambda: 17.0
// time: 1.9799999999999998, lambda: 19.0
// Total time: 00 hour 00 min 41.90 sec
// WSL (mpi np 4):
// time: 1.94, lambda: 17.0
// time: 1.9599999999999997, lambda: 17.0
// time: 1.9799999999999998, lambda: 19.0
// Total time: 00 hour 00 min 22.86 sec

MPI.shutdown();

