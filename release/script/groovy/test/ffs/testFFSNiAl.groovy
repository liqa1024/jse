package test.ffs

import groovy.transform.Field
import jtool.atom.Structures
import jtool.code.CS.Slurm
import jtool.code.UT
import jtool.io.RefreshableFilePrintStream
import jtool.lmp.Dump
import jtool.lmp.Lmpdat
import jtool.lmp.NativeLmp
import jtool.math.MathEX
import jtool.parallel.MPI
import jtoolex.rareevent.ForwardFluxSampling
import jtoolex.rareevent.atom.ABOOPSolidChecker_MPI
import jtoolex.rareevent.atom.MultiTypeClusterSizeCalculator
import jtoolex.rareevent.lmp.MultipleNativeLmpFullPathGenerator

import static jtool.code.CS.MASS
import static jtool.code.CS.VERSION
import static jtool.code.UT.Code.*
import static jtool.code.UT.Math.rng

/**
 * lammps 跑 FFS 的实例；
 * 此脚本需要作为输入文件在 SLURM 上运行
 */


/** 体系参数 */
final def UNIQUE_NAME = args ? args[0] : 'NiAl-FFS';

final int Ni = 50;
final int Al = 100-Ni;
final int replicate = 16;
@Field final static def pairStyle = 'eam/alloy';
@Field final static def pairCoeff = '* * lmp/.potential/Mishin-Ni-Al-2009.eam.alloy Ni Al';

final int atomNum = replicate*replicate*replicate*4;


/** MPI 初始化 */
@Field static int me; // 需要先声明后手动赋值，因为 Field 的执行顺序不是看起来这样
@Field static int np;
MPI.initThread(args, MPI.Thread.MULTIPLE); // 需要指定 MPI.Thread.MULTIPLE 来保证线程安全
me = MPI.Comm.WORLD.rank();
np = MPI.Comm.WORLD.size();
// 手动重定向标准输出而不使用 slurm 的，保证进度条功能稳定
if (me == 0 && Slurm.IS_SLURM) {
    System.setOut(new RefreshableFilePrintStream("slurm-${Slurm.JOB_ID}.jout"));
}
if (me == 0) println("JTOOL_VERSION: ${VERSION}");
if (me == 0) println("MPI_VERSION: ${MPI.libraryVersion()}");


/** 设置一下随机流，保证每个进程的随机流确实独立；这里直接这样简单实现而不涉及通讯 */
for (_ in range(me)) rng().nextInt();
rng(rng().nextLong());


/** NativeLmp 参数 */
NativeLmp.Conf.CMAKE_SETTING.PKG_MANYBODY = 'ON';
NativeLmp.Conf.REBUILD = false; // 如果没有这个包需要开启此选项重新构建
if (me == 0) println("LMP_HOME: ${NativeLmp.Conf.LMP_HOME}");


/** 进度条显示 */
@Field final static boolean pbar = true;
if (pbar) {
    UT.Timer.USE_ASCII = true; // 避免乱码
}


/** 模拟参数 */
final boolean genInitPoints = false;
final boolean genPostInitPoints = false;

final double initTimestep   = 0.002; // ps
final double cellSize       = 3.971;
final int meltTemp          = 2000;
final int SCTemp            = 1600;
final int FFSTemp           = 1500; // 现在需要统一都使用 1500K
final int initRunStep       = 500000;
final int SCRunStep         = 500000;
final int FFSRunStep        = 5000000; // FFS 温度维持的步数，认为已经达到"平衡"
final int FFSContinueStep   = 200000; // 认为这些步数后结构不再有关联

final def workingDir   = "lmp/.temp/${UNIQUE_NAME}/Ni${Ni}Al${Al}/";

final def initDir           = workingDir+'init/';
final def initInDataPath    = initDir+'data-in';
final def initOutDataPath   = initDir+'data-out';

final def SCDir             = workingDir+'supercooling/';
final def SCInDataPath      = SCDir+'data-in';
final def SCOutDataPath     = SCDir+'data-out';

final def FFSDir            = workingDir+'ffs/';
final def FFSInDataPath     = FFSDir+'data-in';
final def FFSOutDataPath    = FFSDir+'data-out';

/** FFS 参数 */
final boolean dumpAllPath   = false;
final boolean printPathEff  = true;

final double timestep       = 0.001; // ps

final int lmpCores          = 2; // lammps 核越少效率越高，并且 MPI 参数计算器效率也会更高
final int initParallelNum   = MathEX.Code.divup(np, lmpCores);

final int color             = me.intdiv(lmpCores);
final def subComm           = MPI.Comm.WORLD.split(color)
final def subRoots          = range(0, np, lmpCores);

final int dumpStep          = 10; // 测试发现 NiAl 应该间距过大了

final int N0                = 2000;
final int step1Mul          = 2;
final double surfaceA       = 5;

// 间距还是需要稍微增大一些避免 pruning 的影响
final def surfaces = [
    10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 70, 80, 90, 100,
    110, 120, 130, 140, 150, 160, 180, 200, 220, 240, 260, 280, 300,
    320, 340, 360, 380, 400, 420, 440, 460, 480, 500, 520, 540, 560,
    580, 600, 620, 640, 660, 680, 700, 720, 740, 760, 780, 800
];

final double pruningProb    = 0.5;
final int pruningThreshold  = 6;

final def FFSDumpPath       = workingDir+'dump-0';
final def FFSRestartPathDu  = workingDir+'restart-dump';
final def FFSRestartPathRe  = workingDir+'restart-rest';
final def FFSAllDumpPath    = workingDir+'dump-all';



/** 开始模拟，初始结构生成，现在会使用多个节点共同生成 */
if (genInitPoints) try (def lmp = new NativeLmp('-log', 'none', '-screen', 'none')) {
    if (me == 0) {
    println("=====GENERATOR MELT DATA OF Ni${Ni}Al${Al}=====");
    println("PAIR_STYLE: ${pairStyle}");
    println("PAIR_COEFF: ${pairCoeff}");
    println("ATOM_NUM: ${atomNum}");
    println("TEMPERATURE: ${meltTemp}K");
    println("TIMESTEP: ${initTimestep} ps");
    println("STEP_NUM: ${initRunStep}");
    println("INIT_CORE_NUM: ${np}");
    }
    // 执行并获取输出，由于 MPI 情况下还需要注意随机数流的问题，这里不使用
    if (me == 0) UT.Timer.tic();
    def inDataInit = Lmpdat.fromAtomData(Structures.FCC(cellSize, replicate).opt().mapTypeRandom(new Random(seed()), Ni, Al), [MASS.Ni, MASS.Al]);
    MPI.Comm.WORLD.barrier();
    runMelt(lmp, inDataInit, initOutDataPath, meltTemp, initRunStep, initTimestep);
    MPI.Comm.WORLD.barrier();
    if (me == 0) UT.Timer.toc('Init melt data');
    
    // 然后冷却到 SCTemp 维持一段时间，设置冷却需要的参数
    if (me == 0) {
    println("=====COOLDOWN MELT DATA AND GET THE SUPERCOOLING DATA=====");
    println("PAIR_STYLE: ${pairStyle}");
    println("PAIR_COEFF: ${pairCoeff}");
    println("ATOM_NUM: ${atomNum}");
    println("TEMPERATURE: ${SCTemp}K");
    println("TIMESTEP: ${initTimestep} ps");
    println("STEP_NUM: ${SCRunStep}");
    println("INIT_CORE_NUM: ${np}");
    }
    if (me == 0) UT.Timer.tic();
    if (me == 0) UT.IO.copy(initOutDataPath, SCInDataPath);
    MPI.Comm.WORLD.barrier();
    runMelt(lmp, SCInDataPath, SCOutDataPath, SCTemp, SCRunStep, initTimestep);
    MPI.Comm.WORLD.barrier();
    if (me == 0) UT.Timer.toc('Gen supercooling data');
    
    // 然后冷却到需要进行FFS的温度并维持一段时间直到平衡
    if (me == 0) {
    println("=====COOLDOWN SUPERCOOLING DATA AND GET THE FFS DATA=====");
    println("PAIR_STYLE: ${pairStyle}");
    println("PAIR_COEFF: ${pairCoeff}");
    println("ATOM_NUM: ${atomNum}");
    println("TEMPERATURE: ${FFSTemp}K");
    println("TIMESTEP: ${initTimestep} ps");
    println("STEP_NUM: ${FFSRunStep}");
    println("INIT_CORE_NUM: ${np}");
    }
    if (me == 0) UT.Timer.tic();
    if (me == 0) UT.IO.copy(SCOutDataPath, FFSInDataPath);
    MPI.Comm.WORLD.barrier();
    runMelt(lmp, FFSInDataPath, FFSOutDataPath, FFSTemp, FFSRunStep, initTimestep);
    MPI.Comm.WORLD.barrier();
    if (me == 0) UT.Timer.toc('Gen FFS data');
}

if (genPostInitPoints) try (def lmp = new NativeLmp(['-log', 'none', '-screen', 'none'] as String[], subComm)) {
    // 然后批量继续过冷的体系，得到不相关的多个初始结构
    if (me == 0) {
    println("=====CONTINUE FFS DATA AND GET ${initParallelNum} FFS DATA=====");
    println("PAIR_STYLE: ${pairStyle}");
    println("PAIR_COEFF: ${pairCoeff}");
    println("ATOM_NUM: ${atomNum}");
    println("TEMPERATURE: ${FFSTemp}K");
    println("TIMESTEP: ${initTimestep} ps");
    println("STEP_NUM: ${FFSContinueStep}");
    println("PARALLEL_NUM: ${initParallelNum}");
    println("LMP_CORE_NUM: ${lmpCores}");
    }
    if (me == 0) UT.Timer.tic();
    if (subComm.rank() == 0) UT.IO.copy(FFSOutDataPath, "${FFSInDataPath}-${color}");
    MPI.Comm.WORLD.barrier();
    runMelt(lmp, "${FFSInDataPath}-${color}", "${FFSOutDataPath}-${color}", FFSTemp, FFSContinueStep, initTimestep);
    MPI.Comm.WORLD.barrier();
    if (me == 0) UT.Timer.toc('Continue FFS data');
}


// 读取结构
def initPoints = range(initParallelNum).collect {Lmpdat.read("${FFSOutDataPath}-${it}")};

def calComm = subComm.copy(); // 参数计算的也需要拷贝一次，避免相互干扰
def dumpCal = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker_MPI().setComm(calComm).setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker_MPI().setComm(calComm).setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker_MPI().setComm(calComm).setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);

MPI.Comm.WORLD.barrier();
MultipleNativeLmpFullPathGenerator.NO_LMP_IN_WORLD_ROOT = true; // slurm 上还是不需要主进程运行 lammps
MultipleNativeLmpFullPathGenerator.withOf(subComm, subRoots, dumpCal, initPoints, [MASS.Ni, MASS.Al], FFSTemp, pairStyle, pairCoeff, timestep, dumpStep) {fullPathGen ->
    int parallelNum = fullPathGen.parallelNum();
    
    /** 开始 FFS */
    println("=====BEGIN ${UNIQUE_NAME} OF Ni${Ni}Al${Al}=====");
    println("TIMESTEP: ${timestep} ps");
    println("PAIR_STYLE: ${pairStyle}");
    println("PAIR_COEFF: ${pairCoeff}");
    println("PARALLEL_NUM: ${parallelNum}");
    println("LMP_CORE_NUM: ${lmpCores}");
    println("TEMPERATURE: ${FFSTemp}K");
    println("N0: ${N0}");
    println("DUMP_STEP: ${dumpStep}");
    println("SURFACE_A: ${surfaceA}");
    println("SURFACES: ${surfaces}");
    println("PRUNING_PROB: ${pruningProb}");
    println("PRUNING_THRESHOLD: ${pruningThreshold}");
    
    // 我也不知道为啥 i = 0 就会达到路径总数阈值，可能第一步有很多大飞跃？总之先设高
    try (def FFS = new ForwardFluxSampling<>(fullPathGen, parallelNum, surfaceA, surfaces, N0).setStep1Mul(step1Mul).setPruningProb(pruningProb).setPruningThreshold(pruningThreshold).setMaxPathNum(N0 * 10000)) {
        if (pbar) FFS.setProgressBar();
        // 增加对 fullPathGen 的单元耗时统计
        if (printPathEff) fullPathGen.initTimer();
        // 第一步，每步都会输出结构
        UT.Timer.tic();
        FFS.run();
        UT.Timer.toc("i = -1, k0 = ${FFS.getK0()}, step1PointNum = ${FFS.step1PointNum()}, step1PathNum = ${FFS.step1PathNum()},");
        // 输出 fullPathGen 的耗时
        if (printPathEff) {
            def info = fullPathGen.getTimerInfo();
            println("PathGenEff: lmp = ${percent(info.lmp/info.total)}, lambda = ${percent(info.lambda/info.total)}, wait = ${percent(info.wait/info.total)}, else = ${percent((info.other)/info.total)}");
        }
        // 然后直接随便选一个输出路径到 dump 并保存 restart
        Dump.fromAtomDataList(FFS.pickPath()).write(FFSDumpPath);
        if (dumpAllPath) {
            // 遍历所有的路径输出到 FFSAllDumpPath/j
            for (j in range(FFS.pointsOnLambda().size())) Dump.fromAtomDataList(FFS.pickPath(j)).write(FFSAllDumpPath + '/' + j);
            // 然后压缩到 FFSAllDumpPath-{i+1}.zip 并删除缓存目录，这里简单实现不直接输出成 zip
            UT.IO.dir2zip(FFSAllDumpPath, "${FFSAllDumpPath}-0.zip");
            UT.IO.rmdir(FFSAllDumpPath);
        }
        if (FFS.stepFinished()) {
            Dump.fromAtomDataList(FFS.pointsOnLambda()).write(FFSRestartPathDu);
            UT.IO.map2json(FFS.restData(), FFSRestartPathRe);
        }
        
        // 后面的步骤，每步都会输出结构并保存 restart
        def i = 0;
        while (!FFS.finished()) {
            // 增加对 fullPathGen 的单元耗时统计
            if (printPathEff) fullPathGen.resetTimer();
            UT.Timer.tic();
            FFS.run();
            UT.Timer.toc("i = ${i}, prob = ${FFS.getProb(i)}, step2PointNum = ${FFS.step2PointNum(i)}, step2PathNum = ${FFS.step2PathNum(i)},");
            // 输出 fullPathGen 的耗时
            if (printPathEff) {
                def info = fullPathGen.getTimerInfo();
                println("PathGenEff: lmp = ${percent(info.lmp/info.total)}, lambda = ${percent(info.lambda/info.total)}, wait = ${percent(info.wait/info.total)}, else = ${percent(info.other/info.total)}");
            }
            // 然后直接随便选一个输出路径到 dump 并保存 restart
            Dump.fromAtomDataList(FFS.pickPath()).write(FFSDumpPath);
            if (dumpAllPath) {
                // 遍历所有的路径输出到 FFSAllDumpPath/j
                for (j in range(FFS.pointsOnLambda().size())) Dump.fromAtomDataList(FFS.pickPath(j)).write(FFSAllDumpPath + '/' + j);
                // 然后压缩到 FFSAllDumpPath-{i+1}.zip 并删除缓存目录，这里简单实现不直接输出成 zip
                UT.IO.dir2zip(FFSAllDumpPath, "${FFSAllDumpPath}-${i + 1}.zip");
                UT.IO.rmdir(FFSAllDumpPath);
            }
            if (FFS.stepFinished()) {
                Dump.fromAtomDataList(FFS.pointsOnLambda()).write(FFSRestartPathDu);
                UT.IO.map2json(FFS.restData(), FFSRestartPathRe);
            }
            ++i;
        }
        println("=====${UNIQUE_NAME} FINISHED=====");
        println("k = ${FFS.getK()}, totalPointNum = ${FFS.totalPointNum()}");
    }
}
MPI.Comm.WORLD.barrier();

/** MPI 关闭 */
calComm.shutdown();
subComm.shutdown();
MPI.shutdown();


static void runMelt(NativeLmp lmp, Lmpdat inData, String outDataPath, double temperature, int runStep, double timestep) {
    // 注意到 lammps 本身输出时不能自动创建文件夹，因此需要手动先合法化输出文件夹
    if (lmp.comm().rank() == 0) UT.IO.validPath(outDataPath);
    if (pbar && me==0) UT.Timer.pbar('runMelt', MathEX.Code.divup(runStep, 5000));
    lmp.command('units           metal');
    lmp.command('boundary        p p p');
    lmp.command("timestep        $timestep");
    lmp.loadData(inData);
    lmp.command("pair_style      $pairStyle");
    lmp.command("pair_coeff      $pairCoeff");
    lmp.command("velocity        all create $temperature ${seedLmp(lmp.comm())} dist gaussian mom yes rot yes");
    lmp.command("fix             1 all npt temp $temperature $temperature 0.2 iso 0.0 0.0 2.0");
    if (pbar) {
        for (_ in range(runStep.intdiv(5000))) {
            lmp.command('run             5000');
            if (me==0) UT.Timer.pbar();
        }
        int rest = runStep % 5000;
        if (rest > 0) {
            lmp.command("run             $rest");
            if (me==0) UT.Timer.pbar();
        }
    } else {
        lmp.command('thermo          5000'); // debug usage
        lmp.command("run             $runStep");
    }
    lmp.command("write_data      $outDataPath");
    lmp.clear();
}
static void runMelt(NativeLmp lmp, String inDataPath, String outDataPath, double temperature, int runStep, double timestep) {
    // 注意到 lammps 本身输出时不能自动创建文件夹，因此需要手动先合法化输出文件夹
    if (lmp.comm().rank() == 0) UT.IO.validPath(outDataPath);
    if (pbar && me==0) UT.Timer.pbar('runMelt', MathEX.Code.divup(runStep, 5000));
    lmp.command('units           metal');
    lmp.command('boundary        p p p');
    lmp.command("timestep        $timestep");
    lmp.command("read_data       $inDataPath");
    lmp.command("pair_style      $pairStyle");
    lmp.command("pair_coeff      $pairCoeff");
    lmp.command("velocity        all create $temperature ${seedLmp(lmp.comm())} dist gaussian mom yes rot yes");
    lmp.command("fix             1 all npt temp $temperature $temperature 0.2 iso 0.0 0.0 2.0");
    if (pbar) {
        for (_ in range(runStep.intdiv(5000))) {
            lmp.command('run             5000');
            if (me==0) UT.Timer.pbar();
        }
        int rest = runStep % 5000;
        if (rest > 0) {
            lmp.command("run             $rest");
            if (me==0) UT.Timer.pbar();
        }
    } else {
        lmp.command('thermo          5000'); // debug usage
        lmp.command("run             $runStep");
    }
    lmp.command("write_data      $outDataPath");
    lmp.clear();
}

static int seedLmp(MPI.Comm comm) {
    // 进行种子同步保证所有进程种子一致
    int me = comm.rank();
    int[] seedBuf = [0];
    if (me == 0) seedBuf[0] = randSeed();
    comm.bcast(seedBuf, 1, 0);
    return seedBuf[0];
}
static long seed(MPI.Comm comm = MPI.Comm.WORLD) {
    // 进行种子同步保证所有进程种子一致
    int me = comm.rank();
    long[] seedBuf = [0];
    if (me == 0) seedBuf[0] = rng().nextLong();
    comm.bcast(seedBuf, 1, 0);
    return seedBuf[0];
}

