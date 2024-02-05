package test.mathex

import jse.code.collection.ArrayLists
import jsex.mcmc.Ising2D


/** 测试蒙特卡洛处理 Ising 模型固定种子的结果 */
long seed = 123456789;
int threadNum = 12;
double J = 1.0, H = 0.3;

// 需要模拟的体系大小，系统数目，统计步数，以及温度
int L  = 40;
int Ns = 24;
int N  = 10000000;
double T = 2.20;

def stat;
// 创建 Ising2D 进行模拟
try (def ISING = new Ising2D(J, H, threadNum, seed)) {
    // 初始化系统，直接初始化
    def allSpins = ArrayLists.from(Ns, {ISING.initSpins(L)});
    // 先在 T[0] 处维持 N 步保证平衡
    ISING.startMonteCarlo(allSpins, N, T, false);
    // 再进行统计
    stat = ISING.startMonteCarlo(allSpins, N, T);
}

println("E: ${stat.E}, M: ${stat.M}, E2: ${stat.E2}, M2: ${stat.M2}");
// E: -3265.4918320296233, M: 1483.1253324333334, E2: 1.0667833167332923E7, M2: 2200150.6202675

