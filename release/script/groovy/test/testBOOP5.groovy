package test

import com.jtool.atom.Structures
import com.jtool.lmp.Lmpdat
import com.jtool.math.vector.Vectors
import com.jtool.plot.Plotters
import com.jtool.vasp.POSCAR


/** 测试计算 BOOP，测试固液判断的阈值选择 */


// 首先导入 Lmpdat
def dataG = Lmpdat.read('lmp/data/data-glass');
def dataC = Lmpdat.read('lmp/data/data-crystal');

// 计算连接数向量
def connectCountG, connectCountC;
try (def mpc = dataG.getMPC()) {
    connectCountG = mpc.calConnectCountABOOP(6, mpc.unitLen()*2.0, 12, 0.83);
}
try (def mpc = dataC.getMPC()) {
    connectCountC = mpc.calConnectCountABOOP(6, mpc.unitLen()*2.0, 12, 0.83);
}

// 统计结果
def distributionG = Vectors.zeros(13);
def distributionC = Vectors.zeros(13);
connectCountG.forEach {double count ->
    distributionG.increment(count as int);
}
connectCountC.forEach {double count ->
    distributionC.increment(count as int);
}

// 计算玻璃中判断为固体的百分比（保证在一个较小的不为零的值，如 0.5%）
println("solid prob in glass: ${distributionG[7..12].sum() / dataG.atomNum()}")

// 绘制结果
def plt = Plotters.get();

plt.plot(distributionG, 'glass');
plt.plot(distributionC, 'crystal');

plt.show();

