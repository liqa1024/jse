package test.mathex

import jse.code.collection.ArrayLists
import jse.math.vector.IVector
import jse.plot.Plotters
import jsex.mcmc.Ising2D

import static jse.code.UT.Math.*
import static jse.code.UT.Timer.*


/** 测试蒙特卡洛处理 Ising 模型，应该会比 python 实现快得多 */
int threadNum = 12;
double J = 1.0, H = 0.0;

// 需要模拟的体系大小，系统数目，统计步数，以及温度
int[] L  = [10, 20, 30];
int[] Ns = [24, 24, 24];
int[] N  = [400000, 800000, 1600000];
double[] T = [
    1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.10, 2.15, 2.20, 2.21,
    2.22, 2.23, 2.24, 2.25, 2.26, 2.27, 2.28, 2.29, 2.30, 2.31, 2.32, 2.33, 2.34,
    2.35, 2.36, 2.37, 2.38, 2.39, 2.40, 2.41, 2.42, 2.43, 2.44, 2.45, 2.46, 2.47,
    2.48, 2.49, 2.50, 2.55, 2.60, 2.7, 2.8, 2.9, 3.0, 3.1, 3.2, 3.3, 3.4, 3.5
];

List<IVector> E = ArrayLists.nulls(L.size());
List<IVector> M = ArrayLists.nulls(L.size());
List<IVector> C = ArrayLists.nulls(L.size());
List<IVector> X = ArrayLists.nulls(L.size());
// 创建 Ising2D 进行模拟
try (def ISING = new Ising2D(J, H, threadNum)) {
    // 开始蒙特卡洛模拟，这会花费一定的时间，我增加了一个进度条 :)
    pbar(L.size()*T.size());
    for (i in 0..<L.size()) {
        // 初始化系统，直接初始化
        def allSpins = ArrayLists.from(Ns[i], {ISING.initSpins(L[i])});
        // 先在 T[0] 处维持 N[i]*10 步保证平衡
        ISING.startMonteCarlo(allSpins, N[i]*10, T[0], false);
        E[i] = zeros(T.size());
        M[i] = zeros(T.size());
        C[i] = zeros(T.size());
        X[i] = zeros(T.size());
        // 然后逐渐升温到 T[-1]，每次都统计 N[i] 步，每次开始前都持续 N[i]*0.1 步保证平衡
        for (j in 0..<T.size()) {
            ISING.startMonteCarlo(allSpins, round(N[i]*0.1), T[0], false);
            def d = ISING.startMonteCarlo(allSpins, N[i], T[j]);
            E[i][j] = d.E / (double)(L[i]*L[i]);
            M[i][j] = d.M / (double)(L[i]*L[i]);
            C[i][j] = (d.E2 - d.E*d.E) / (T[j]*T[j]) / (double)(L[i]*L[i]);
            X[i][j] = (d.M2 - d.M*d.M) / (T[j]) / (double)(L[i]*L[i]);
            pbar();
        }
    }
}

def MARKER = ['o', '^', 's', 'd']
// 绘制物理量随温度的变化
def plt1 = Plotters.get();
for (i in 0..<L.size()) {
    plt1.plot(T, E[i], 'L='+L[i]).lineType('none').marker(MARKER[i]);
}
plt1.xlabel('T')
plt1.ylabel('<E>')
plt1.title('Average Energy')
plt1.show()

def plt2 = Plotters.get();
for (i in 0..<L.size()) {
    plt2.plot(T, M[i], 'L='+L[i]).lineType('none').marker(MARKER[i]);
}
plt2.xlabel('T')
plt2.ylabel('<M>')
plt2.title('Average Magnetization')
plt2.show()

def plt3 = Plotters.get();
for (i in 0..<L.size()) {
    plt3.plot(T, C[i], 'L='+L[i]).lineType('none').marker(MARKER[i]);
}
plt3.xlabel('T');
plt3.ylabel('C');
plt3.title('Specific Heat');
plt3.show();

def plt4 = Plotters.get();
for (i in 0..<L.size()) {
    plt4.plot(T, X[i], 'L='+L[i]).lineType('none').marker(MARKER[i]);
}
plt4.xlabel('T');
plt4.ylabel('X');
plt4.title('Magnetic Susceptibility');
plt4.show();

