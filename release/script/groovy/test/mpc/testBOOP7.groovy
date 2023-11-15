package test.mpc


import jtool.code.UT
import jtool.lmp.Dump
import jtool.math.table.Tables
import jtool.math.vector.Vectors
import jtool.plot.Plotters
import jtool.rareevent.atom.ABOOPSolidChecker
import jtool.rareevent.atom.ClusterSizeCalculator
import jtool.rareevent.atom.MultiTypeClusterSizeCalculator

import static jtool.code.UT.Par.parfor


/** 绘制参数随时间的演化 */

final double timestep = 0.002; // ps

// 读取 dump
def dump  = Dump.read('lmp/.stableglass-in/dump-fs1');

// 结果保存成 Vector
def crystalSizeOld = Vectors.zeros(dump.size());
def crystalSizeNew = Vectors.zeros(dump.size());

// 用于计算合金的团簇计算器

UT.Timer.tic();
def cal = new ClusterSizeCalculator();
parfor(dump.size()) {int i ->
    // 统计结晶的数目
    crystalSizeOld[i] = cal.lambdaOf(dump[i]);
}
UT.Timer.toc('old λ');

UT.Timer.tic();
cal = new MultiTypeClusterSizeCalculator(
    new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.89).setSolidThreshold(7),
    [new ABOOPSolidChecker().setRNearestMul(1.8).setConnectThreshold(0.84).setSolidThreshold(13), new ABOOPSolidChecker().setRNearestMul(1.5).setConnectThreshold(0.84).setSolidThreshold(7)]
);
parfor(dump.size()) {int i ->
    // 统计结晶的数目
    crystalSizeNew[i] = cal.lambdaOf(dump[i]);
}
UT.Timer.toc('new λ');
def time = Vectors.linsequence(0, (dump[1].timeStep()-dump[0].timeStep())*timestep*0.001, dump.size());


// 正式图片，现在需要存一下 csv
def data = Tables.zeros(dump.size());
data['time'] = time;
data['crystalSizeOld'] = crystalSizeOld;
data['crystalSizeNew'] = crystalSizeNew;
UT.IO.table2csv(data, '.temp/CuZr-fs1-800.csv');

// 绘制
def plt = Plotters.get();
plt.plot(time, crystalSizeOld, 'λ-old');
plt.plot(time, crystalSizeNew, 'λ-new');
plt.xlabel('t [ns]').ylabel('cluster size');
plt.axis(0, time.last(), 0, 200);
plt.show();

