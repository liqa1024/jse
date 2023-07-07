package test

import com.jtool.code.UT
import com.jtool.lmp.Dump
import com.jtool.math.vector.Vectors
import com.jtool.parallel.ParforThreadPool
import com.jtool.plot.Plotters
import com.jtool.rareevent.atom.ClusterSizeCalculator
import com.jtool.rareevent.atom.ClusterSizeCalculatorMPI

import static com.jtool.code.UT.Code.*

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
try (def cal = new ClusterSizeCalculator(); def pool = new ParforThreadPool(processNum)) {
    // 并行提交任务并获取结果
    pool.parfor(dump.size()) {int i ->
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

