package atom

import jtool.code.SP
import jtool.code.UT
import jtool.math.matrix.RowMatrix
import jtool.math.vector.ILogicalVector
import jtool.math.vector.IVector
import jtool.math.vector.LogicalVector
import jtoolex.ml.DecisionTree
import jtoolex.ml.RandomForest

import static jtool.code.UT.Math.linspace
import static jtool.code.UT.Math.zeros
import static jtool.code.UT.Plot.axis
import static jtool.code.UT.Plot.plot

/**
 * 涉及的功能较多，
 * 这里使用一个类来存储
 */
class ClassifyCe {
    
    final static def read_vasp_xdatcar;
    final static def basisCalculator;
    
    /** 计算对应的基，这里暂时使用现有的 python 脚本计算 */
    static def calBasis(String path, int index=-1) {
        // 使用 ase 的读取 xdatcar 方法，读取成 ase 的实例
        def data = read_vasp_xdatcar(path, [index: index]);
        // 使用 basisCalculator 来计算基，输出为 numpy 的数组，注意需要调用一次 unwrap 来获取到 JEP 可以识别到的 python 对象
        def basis = basisCalculator.evaluate(data.unwrap());
        // 获取的是 jep.NDArray，这样获取内部数据，直接转为 IMatrix，这里获取到的是按行排列的
        double[] basisData = basis.data;
        int[] basisDim = basis.dimensions;
        return new RowMatrix(basisDim[0], basisDim[1], basisData);
    }
    
    static {
        // 导入需要的 python 包
        SP.Python.runText('from ase.io.vasp import read_vasp_xdatcar');
        SP.Python.runText('from libenv.spherical_chebyshev import SphericalChebyshev');
        
        // 获取需要的类/方法名，存为静态变量
        read_vasp_xdatcar = SP.Python.getClass('read_vasp_xdatcar');
        basisCalculator = SP.Python.newInstance('SphericalChebyshev', ['Ce'], 5, 6, (double)6.5);
    }
    
    // 训练集和测试集定义
    final static def trainSet = [[
        path: 'vasp/.Ce/0UCe/4th5000/XDATCAR',
        index: ((1000-1)..<5000).step(500),
        ratio: [0, 100]
    ], [
        path: 'vasp/.Ce/0UCe/5th5000/XDATCAR',
        index: ((1000-1)..<5000).step(500),
        ratio: [0, 100]
    ], [
        path: 'vasp/.Ce/20U/1st400/XDATCAR',
        index: ((200-1)..<400).step(50),
        ratio: [20, 80]
    ], [
        path: 'vasp/.Ce/20U/2nd400/XDATCAR',
        index: ((200-1)..<400).step(50),
        ratio: [20, 80]
    ], [
        path: 'vasp/.Ce/40U/1st400/XDATCAR',
        index: ((200-1)..<400).step(50),
        ratio: [40, 60]
    ], [
        path: 'vasp/.Ce/60U/1st350/XDATCAR',
        index: ((200-1)..<350).step(50),
        ratio: [60, 40]
    ], [
        path: 'vasp/.Ce/60U/2nd350/XDATCAR',
        index: ((200-1)..<350).step(50),
        ratio: [60, 40]
    ], [
        path: 'vasp/.Ce/80U/1st250/XDATCAR',
        index: ((100-1)..<250).step(50),
        ratio: [80, 20]
    ], [
        path: 'vasp/.Ce/80U/2nd250/XDATCAR',
        index: ((100-1)..<250).step(50),
        ratio: [80, 20]
    ], [
        path: 'vasp/.Ce/100UCe/3rd700/XDATCAR',
        index: ((200-1)..<700).step(50),
        ratio: [100, 0]
    ], [
        path: 'vasp/.Ce/100UCe/7th700/XDATCAR',
        index: ((200-1)..<700).step(50),
        ratio: [100, 0]
    ]];
    final static def testSet = [[
        path: 'vasp/.Ce/20Urandom/1000-20/XDATCAR',
        index: ((200-1)..<500).step(50),
        ratio: [20, 80]
    ], [
        path: 'vasp/.Ce/20Urandom/2000-20/XDATCAR',
        index: ((200-1)..<500).step(50),
        ratio: [20, 80]
    ], [
        path: 'vasp/.Ce/40Urandom/1000-40/XDATCAR',
        index: ((200-1)..<500).step(50),
        ratio: [40, 60]
    ]];
    
    
    static def main(args) {
        // 从给定的训练集和测试集获取数据
        UT.Timer.pbar('Init DataSet', trainSet.size()+testSet.size());
        def trainInput = new ArrayList<IVector>();
        def trainOutput = LogicalVector.builder();
        for (subSet in trainSet) {
            for (j in subSet.index) {
                trainInput.addAll(calBasis(subSet.path, j).rows());
                // 加 U 为 true
                for (i in 0..<subSet.ratio[0]) trainOutput.add(true);
                for (i in 0..<subSet.ratio[1]) trainOutput.add(false);
            }
            UT.Timer.pbar();
        }
        trainOutput = trainOutput.build();
        def testInput = new ArrayList<IVector>();
        def testOutput= LogicalVector.builder();
        for (subSet in testSet) {
            for (j in subSet.index) {
                testInput.addAll(calBasis(subSet.path, j).rows());
                // 加 U 为 true
                for (i in 0..<subSet.ratio[0]) testOutput.add(true);
                for (i in 0..<subSet.ratio[1]) testOutput.add(false);
            }
            UT.Timer.pbar();
        }
        testOutput = testOutput.build();
        println("Train Data Size: ${trainOutput.size()}");
        println("Test Data Size: ${testOutput.size()}");
        
        // 训练随机森林和决策树
        def tree = DecisionTree.builder(trainInput, trainOutput).build();
        def rf = new RandomForest(trainInput, trainOutput, 500, 0.02);
        
        // 绘制 ROC
        axis(0, 1, 0, 1);
        plot([0, 1], [0, 1], null).lineType('--').width(1.0).color(0);
        plotROC(tree, trainInput, trainOutput, 'tree-train', 1, '-.');
        plotROC(tree, testInput , testOutput , 'tree-test' , 1);
        plotROC(rf  , trainInput, trainOutput, 'rf-train'  , 3, '-.');
        plotROC(rf  , testInput , testOutput , 'rf-test'   , 3);
        
        rf.shutdown();
    }
    
    
    static def plotROC(def decider, List<IVector> dataInput, ILogicalVector dataOutput, String name, def color, def line='-') {
        // 统计预测结果
        if (decider instanceof RandomForest) {
            def recall = zeros(21);
            def Ne = zeros(21);
            def ratio = linspace(0.0, 1.0, 21);
            for (j in 0..<20) {
                if (ratio[j] == (double)0.0) {recall[j] = 1.0; Ne[j] = 1.0; continue;}
                if (ratio[j] == (double)1.0) {recall[j] = 0.0; Ne[j] = 0.0; continue;}
                int Npp = 0, Nnn = 0, Npn = 0, Nnp = 0;
                for (i in 0..<dataInput.size()) {
                    boolean real = dataOutput[i];
                    boolean pred = decider.makeDecision(dataInput[i], ratio[j]);
                    if (real) {
                        if (pred) ++Npp;
                        else ++Npn;
                    } else {
                        if (pred) ++Nnp;
                        else ++Nnn;
                    }
                }
                recall[j] = Npp / (Npp + Npn);
                Ne[j] = Nnp / (Nnn + Nnp);
            }
            plot(Ne, recall, "ROC of $name").marker('o').lineType(line).color(color);
            plot([Ne[10]], [recall[10]], null).marker('x').lineType('none').color(color);
        } else {
            int Npp = 0, Nnn = 0, Npn = 0, Nnp = 0;
            for (i in 0..<dataInput.size()) {
                boolean real = dataOutput[i];
                boolean pred = decider.makeDecision(dataInput[i]);
                if (real) {
                    if (pred) ++Npp;
                    else ++Npn;
                } else {
                    if (pred) ++Nnp;
                    else ++Nnn;
                }
            }
            def recall = Npp / (Npp + Npn);
            def Ne = Nnp / (Nnn + Nnp);
            plot([0, Ne, 1], [0, recall, 1], "ROC of $name").marker('o').lineType(line).color(color);
        }
    }
}
