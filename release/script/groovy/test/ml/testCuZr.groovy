package test.ml

import jse.code.UT
import jse.code.collection.ArrayLists
import jse.lmp.Lmpdat
import jse.math.matrix.IMatrix
import jse.math.vector.IVector
import jse.math.vector.LogicalVector
import jse.parallel.MatrixCache
import jsex.ml.RandomForest
import atom.ClassifyCe;

import static jse.code.UT.Plot.axis
import static jse.code.UT.Plot.plot

/**
 * 测试使用基组 + 随机森林来区分 CuZr 中的晶相，
 * 这里只区分 laves 相，可以方便对比
 */



final int nmax = 1;
final int lmax = 6;
final double cutoff = 2.0;

final def lavesPaths = [
    'lmp/.CuZr/data-laves1-600', 'lmp/.CuZr/data-laves1-800', 'lmp/.CuZr/data-laves1-1400', 'lmp/.CuZr/data-laves1-1600',
    'lmp/.CuZr/data-laves2-600', 'lmp/.CuZr/data-laves2-800', 'lmp/.CuZr/data-laves2-1400', 'lmp/.CuZr/data-laves2-1600',
    'lmp/.CuZr/data-laves3-600', 'lmp/.CuZr/data-laves3-800', 'lmp/.CuZr/data-laves3-1400', 'lmp/.CuZr/data-laves3-1600'
];
final def nolavesPaths = [
    'lmp/.CuZr/data-nolaves-600', 'lmp/.CuZr/data-nolaves-800', 'lmp/.CuZr/data-nolaves-1600'
];

// 构造样本
def dataIn = new ArrayList<IVector>();
def dataOut = LogicalVector.builder();
// 玻璃样本
for (nolavesPath in nolavesPaths) try (def mpc = Lmpdat.read(nolavesPath).getMPC()) {
    println("AtomNum of $nolavesPath: ${mpc.atomNum()}");
    def basis = getBasisMean(mpc.calFPSuRui(nmax, lmax, mpc.unitLen()*cutoff), mpc, cutoff);
    for (fp in basis) {
        dataIn.add(fp.asVecRow());
        dataOut.add(false);
    }
}
// 三种 laves 相样本
for (lavesPath in lavesPaths) try (def mpc = Lmpdat.read(lavesPath).getMPC()) {
    println("AtomNum of $lavesPath: ${mpc.atomNum()}");
    def basis = getBasisMean(mpc.calFPSuRui(nmax, lmax, mpc.unitLen()*cutoff), mpc, cutoff);
    for (fp in basis) {
        dataIn.add(fp.asVecRow());
        dataOut.add(true);
    }
}
dataOut = dataOut.build();


// 抽取部分作为测试集
int end = dataOut.size();
int mid = (int)Math.round(end * 0.8);
def randIndex = ArrayLists.from(end, {i->i}); randIndex.shuffle();
def trainIn = dataIn[randIndex[0..<mid]];
def trainOut = dataOut[randIndex[0..<mid]];
def testIn = dataIn[randIndex[mid..<end]];
def testOut = dataOut[randIndex[mid..<end]];

// 训练随机森林
def rf = new RandomForest(trainIn, trainOut, 500, 0.02);

// 保存训练结果
UT.IO.map2json(rf.asMap(), 'lmp/.CuZr/rf.json');
println("训练模型已保存至 'lmp/.CuZr/rf.json'");

// 绘制 ROC
axis(0, 1, 0, 1);
plot([0, 1], [0, 1], null).lineType('--').width(1.0).color(0);
UT.Timer.tic();
ClassifyCe.plotROC(rf, trainIn, trainOut, 'rf-train', 3, '-.');
ClassifyCe.plotROC(rf, testIn , testOut , 'rf-test' , 3);
UT.Timer.toc();

rf.shutdown();



// 再对近邻做一次平均来弱化温度的影响
static List<IMatrix> getBasisMean(List<IMatrix> basis, def mpc, double cutoff) {
    def basisMean = MatrixCache.getMat(basis.first().nrows(), basis.first().ncols(), basis.size());
    for (int i : 0..<basis.size()) {
        basisMean[i].fill(basis[i]);
        def nl = mpc.getNeighborList(i, mpc.unitLen()*cutoff);
        nl.forEach {int j -> basisMean[i].plus2this(basis[j]);}
        basisMean[i].div2this(nl.size()+1);
    }
    MatrixCache.returnMat(basis);
    return basisMean;
}



