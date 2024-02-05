package test.system

import jse.code.UT
import jse.lmp.Dump
import jse.math.vector.Vectors
import jse.plot.Plotters
import jse.rareevent.atom.ClusterSizeCalculator
import jse.rareevent.atom.ClusterSizeCalculatorMPI

import static jse.code.UT.Code.*

/** 测试基于 ZeroMQ 的跨进程通讯 */

// 读取测试数据，1000 帧的 dump
UT.Timer.tic();
def dump = Dump.read('lmp/.temp/dump-1000');
UT.Timer.toc('read dump');


// 在子进程上计算
UT.Timer.tic();

// 设置并行数
int processNum = 4;
// 计算结果
def crystalSize = Vectors.zeros(dump.size());


// 获取计算器，然后同样直接 parfor 并行
try (def cal = new ClusterSizeCalculator()) {
    // 并行提交任务并获取结果
    parfor(dump.size(), processNum) {int i ->
        crystalSize[i] = cal.lambdaOf(dump[i]);
    }
}

UT.Timer.toc("$processNum process cal");


// 保存结果
//UT.IO.data2csv(crystalSize, 'lmp/.temp/crystal-size.csv');

// 绘制结果
def plt = Plotters.get();
plt.plot(crystalSize, 'crystal size');
plt.xlabel('frame').ylabel('value');
plt.show();

