package test.ffs

import jtool.code.UT
import jtool.lmp.Dump
import jtool.lmp.Lmpdat
import jtool.math.MathEX
import jtool.math.vector.Vectors
import jtool.parallel.MPI
import jtoolex.rareevent.ForwardFluxSampling
import jtoolex.rareevent.atom.ABOOPSolidChecker_MPI
import jtoolex.rareevent.atom.MultiTypeClusterSizeCalculator
import jtoolex.rareevent.lmp.MultipleNativeLmpFullPathGenerator

import static jtool.code.CS.MASS
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

/** FFS 参数 */
final int lmpCores          = 2;

final int dumpStep          = 5; // 0.01 ps

final int N0                = 100;
final int step1Mul          = 1;
final double surfaceA       = 30;
final def surfaces          = (50..500).step(20);

final double pruningProb    = 0.5;
final int pruningThreshold  = 5;

final def FFSDumpPath       = workingDir+'dump-0';
final def FFSRestartPathDu  = workingDir+'restart-dump';
final def FFSRestartPathRe  = workingDir+'restart-rest';


MPI.initThread(args, MPI.Thread.MULTIPLE); // 需要指定 MPI.Thread.MULTIPLE 来保证线程安全
int me = MPI.Comm.WORLD.rank();
int np = MPI.Comm.WORLD.size();

final int parallelNum       = MathEX.Code.divup(np, lmpCores);
final def subRoots = range(0, np, lmpCores);


// 读取结构
def initPoints = range(parallelNum).collect {Lmpdat.read("${SCOutDataPath}-${it}")};

int color = me.intdiv(lmpCores);
try (def subComm = MPI.Comm.WORLD.split(color)) {

def dumpCal = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker_MPI().setComm(subComm).setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker_MPI().setComm(subComm).setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker_MPI().setComm(subComm).setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);

def fullPathGen = new MultipleNativeLmpFullPathGenerator(MPI.Comm.WORLD, 0, subComm, subRoots, dumpCal, initPoints, Vectors.from([MASS.Cu, MASS.Zr]), SCTemp, pairStyle, pairCoeff, timestep, dumpStep);

/** 开始 FFS */
if (me == 0) {
println("=====BEGIN ${UNIQUE_NAME} OF Cu${Cu}Zr${Zr}=====");
println("TIMESTEP: ${timestep} ps");
println("PAIR_STYLE: ${pairStyle}");
println("PAIR_COEFF: ${pairCoeff}");
println("PARALLEL_NUM: ${parallelNum}");
println("LMP_CORE_NUM: ${lmpCores}");
println("TEMPERATURE: ${SCTemp}K");
println("N0: ${N0}");
println("DUMP_STEP: ${dumpStep}");
println("SURFACE_A: ${surfaceA}");
println("SURFACES: ${surfaces}");
println("PRUNING_PROB: ${pruningProb}");
println("PRUNING_THRESHOLD: ${pruningThreshold}");

try (def FFS = new ForwardFluxSampling<>(fullPathGen, parallelNum, surfaceA, surfaces, N0).setStep1Mul(step1Mul).setPruningProb(pruningProb).setPruningThreshold(pruningThreshold)) {
    // 第一步，每步都会输出结构
    UT.Timer.tic();
    FFS.run();
    UT.Timer.toc("i = -1, k0 = ${FFS.getK0()}, step1PointNum = ${FFS.step1PointNum()}, step1PathNum = ${FFS.step1PathNum()},");
    // 然后直接随便选一个输出路径到 dump 并保存 restart
    Dump.fromAtomDataList(FFS.pickPath()).write(FFSDumpPath);
    if (FFS.stepFinished()) {
        Dump.fromAtomDataList(FFS.pointsOnLambda()).write(FFSRestartPathDu);
        UT.IO.map2json(FFS.restData(), FFSRestartPathRe);
    }
    
    // 后面的步骤，每步都会输出结构并保存 restart
    def i = 0;
    while (!FFS.finished()) {
        UT.Timer.tic();
        FFS.run();
        UT.Timer.toc("i = ${i}, prob = ${FFS.getProb(i)}, step2PointNum = ${FFS.step2PointNum(i)}, step2PathNum = ${FFS.step2PathNum(i)},");
        // 然后直接随便选一个输出路径到 dump 并保存 restart
        Dump.fromAtomDataList(FFS.pickPath()).write(FFSDumpPath);
        if (FFS.stepFinished()) {
            Dump.fromAtomDataList(FFS.pointsOnLambda()).write(FFSRestartPathDu);
            UT.IO.map2json(FFS.restData(), FFSRestartPathRe);
        }
        ++i;
    }
    println("=====${UNIQUE_NAME} FINISHED=====");
    println("k = ${FFS.getK()}, totalPointNum = ${FFS.totalPointNum()}");
}}}


MPI.shutdown();

