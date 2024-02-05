package test.ml

import jse.code.UT
import jse.lmp.Dump
import jse.math.vector.Vectors
import jse.parallel.MatrixCache
import jse.plot.Plotters
import jsex.ml.RandomForest
import jsex.rareevent.atom.ABOOPSolidChecker
import jsex.rareevent.atom.CustomClusterSizeCalculator
import jsex.rareevent.atom.MultiTypeClusterSizeCalculator

import static jse.code.UT.Par.parfor
import static test.ml.testCuZr.getBasisMean

/**
 * 测试使用基组 + 随机森林来区分 CuZr 中的晶相，
 * 这里只区分 laves 相，可以方便对比
 */

final int nmax = 1;
final int lmax = 6;
final double cutoff = 2.0;

final double timestep = 0.002; // ps

// 读取 dump
def dump = Dump.read('lmp/.stableglass-in/dump-fs1');
//dump = dump[(0..<dump.size()).step(8)];

// 结果保存成 Vector
def crystalSizeNew = Vectors.zeros(dump.size());
def crystalSizeML  = Vectors.zeros(dump.size());

// 用于计算合金的团簇计算器
UT.Timer.tic();
cal = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker().setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);
UT.Timer.pbar('new λ', dump.size());
parfor(dump.size()) {int i ->
    // 统计结晶的数目
    crystalSizeNew[i] = cal.lambdaOf(dump[i]);
    UT.Timer.pbar();
}
UT.Timer.toc('new λ');
// new λ time: 00 hour 01 min 26.19 sec

// 使用 ML 来区分
UT.Timer.tic();
def rf = RandomForest.load(UT.IO.json2map('lmp/.CuZr/rf.json'), 1);
//cal = new CustomClusterSizeCalculator({mpc -> rf.makeDecision(getBasisMean(mpc.calFPSuRui(nmax, lmax, mpc.unitLen()*cutoff), mpc, cutoff).collect {it.asVecRow()})});
cal = new CustomClusterSizeCalculator({mpc ->
    def fp = mpc.calFPSuRui(nmax, lmax, mpc.unitLen()*cutoff);
    def isSolid = rf.makeDecision(getBasisMean(fp, mpc, cutoff).collect {it.asVecRow()});
    MatrixCache.returnMat(fp);
    return isSolid;
});
UT.Timer.pbar('ml λ', dump.size());
parfor(dump.size()) {int i ->
    // 统计结晶的数目
    crystalSizeML[i] = cal.lambdaOf(dump[i]);
    UT.Timer.pbar();
}
rf.shutdown();
UT.Timer.toc('ml λ');
// ml λ time: 00 hour 03 min 30.26 sec (no cache)
// ml λ time: 00 hour 03 min 23.56 sec (cached)

def time = Vectors.linsequence(0, (dump[1].timeStep()-dump[0].timeStep())*timestep*0.001, dump.size());


// 绘制
def plt = Plotters.get();
plt.plot(time, crystalSizeNew, 'λ-new');
plt.plot(time, crystalSizeML , 'λ-ml' );
plt.xlabel('t [ns]').ylabel('cluster size');
//plt.axis(0, time.last(), 0, 200);
plt.show();

