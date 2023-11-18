package test.mathex

import groovy.transform.CompileStatic
import jtool.code.collection.ArrayLists
import jtool.math.matrix.IMatrix
import jtool.math.vector.IVector
import jtool.plot.Plotters

import static jtool.code.UT.Math.*
import static jtool.code.UT.Timer.*


/** 测试蒙特卡洛处理 Ising 模型，应该会比 python 实现快得多 */
@CompileStatic
class IsingMC {
    // 初始化参数
    static final double J = 1.0, H = 0.0;
    
    static void main(def args) {
        // 需要模拟的体系大小，统计步数，以及温度
        int[] L = [10, 20, 30];
        int[] N = [16000000, 16000000, 24000000];
        double[] T = [
            1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.10, 2.15, 2.20, 2.21,
            2.22, 2.23, 2.24, 2.25, 2.26, 2.27, 2.28, 2.29, 2.30, 2.31, 2.32, 2.33, 2.34,
            2.35, 2.36, 2.37, 2.38, 2.39, 2.40, 2.41, 2.42, 2.43, 2.44, 2.45, 2.46, 2.47,
            2.48, 2.49, 2.50, 2.55, 2.60, 2.7, 2.8, 2.9, 3.0, 3.1, 3.2, 3.3, 3.4, 3.5
        ];
        
        pbar(L.size()*T.size());
        // 开始蒙特卡洛模拟，这会花费一定的时间，我增加了一个进度条 :)
        List<IVector> E = ArrayLists.nulls(L.size());
        List<IVector> M = ArrayLists.nulls(L.size());
        List<IVector> C = ArrayLists.nulls(L.size());
        List<IVector> X = ArrayLists.nulls(L.size());
        // 好像 groovy 下并行会有性能问题，这里只能串行
        for (i in 0..<L.size()) {
            // 初始化系统
            def spins = zeros(L[i], L[i]);
            spins.assignCol {rand()<0.5 ? 1.0 : -1.0}
            // 先在 T[0] 处维持 N[i]*10 步保证平衡
            startMonteCarlo(spins, N[i]*10, T[0]);
            E[i] = zeros(T.size());
            M[i] = zeros(T.size());
            C[i] = zeros(T.size());
            X[i] = zeros(T.size());
            // 然后逐渐升温到 T[-1]，每次都统计 N[i] 步
            for (j in 0..<T.size()) {
                double[] out = startMonteCarlo(spins, N[i], T[j]);
                E[i][j] = out[0] / (double)(L[i]*L[i]);
                M[i][j] = out[1] / (double)(L[i]*L[i]);
                C[i][j] = (out[2] - out[0]*out[0]) / (T[j]*T[j]) / (double)(L[i]*L[i]);
                X[i][j] = (out[3] - out[1]*out[1]) / (T[j]) / (double)(L[i]*L[i]);
                pbar();
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
    }
    
    
    // 统计能量，注意自旋相互作用只需要考虑一半
    static double statE(IMatrix spins) {
        int L = spins.rowNumber();
        double E = 0.0;
        for (i in 0..<L) for (j in 0..<L) {
            // 先考虑周期边界条件
            int ipp = i + 1; if (ipp >= L) ipp -= L;
            int jpp = j + 1; if (jpp >= L) jpp -= L;
            // 获取周围自旋值
            double spinC = spins[i  ][j  ];
            double spinR = spins[ipp][j  ];
            double spinU = spins[i  ][jpp];
            // 计算能量
            E += spinC * H;
            E -= spinC*spinR * J;
            E -= spinC*spinU * J;
        }
        return E;
    }
    
    // 执行 N 步蒙特卡洛并统计 <E>, <|M|>, <E^2>, <|M|^2>
    static double[] startMonteCarlo(IMatrix spins, int N, double T) {
        int L = spins.rowNumber();
        // 开始之前先初始统计物理量，初始值不计入统计
        double E = statE(spins);
        double M = spins.opt().sum();
        // 需要返回的统计物理量
        double sumE = 0.0;
        double sumM = 0.0;
        double sumE2 = 0.0;
        double sumM2 = 0.0;
        // 开始蒙特卡洛模拟
        for (k in 0..<N) {
            int i = randi(L);
            int j = randi(L);
            // 先考虑周期边界条件
            int ipp = i + 1; if (ipp >= L) ipp -= L;
            int inn = i - 1; if (inn <  0) inn += L;
            int jpp = j + 1; if (jpp >= L) jpp -= L;
            int jnn = j - 1; if (jnn <  0) jnn += L;
            // 获取周围自旋值
            double spinC = spins[i  ][j  ];
            double spinL = spins[inn][j  ];
            double spinR = spins[ipp][j  ];
            double spinU = spins[i  ][jpp];
            double spinD = spins[i  ][jnn];
            // 计算翻转前后能量差
            double dE = -spinC * H
            dE += spinC*spinL * J
            dE += spinC*spinR * J
            dE += spinC*spinU * J
            dE += spinC*spinD * J
            dE *= 2.0
            // 如果能量差小于 0 则 100% 接受翻转，否则概率接受
            if (dE<=0 || rand()<exp(-dE/T)) {
                spins[i][j] = -spinC
                // 更新物理量
                E += dE
                M -= 2.0*spinC
            }
            // 累加统计结果
            sumE += E
            sumM += abs(M) // 磁矩需要取绝对值
            sumE2 += E*E
            sumM2 += M*M
        }
        // 返回结果
        return [sumE/N, sumM/N, sumE2/N, sumM2/N] as double[]
    }
}

