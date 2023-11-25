package test.ml

import jtool.code.UT
import jtool.code.collection.ArrayLists
import jtool.math.matrix.Matrices
import jtool.math.vector.IVector
import jtool.math.vector.LogicalVector
import jtoolex.ml.DecisionTree
import jtoolex.ml.RandomForest

import static jtool.code.CS.ALL
import static jtool.code.UT.Plot.axis
import static jtool.code.UT.Plot.plot

/**
 * 测试区分基组
 */

static def readData(String path, List<IVector> dest) {
    def lines = UT.IO.readAllLines(path);
    int colNum = UT.Texts.splitBlank(lines.first()).size();
    // 手动保存到 dest
    for (i in 0..<lines.size()) {
        dest.add(UT.Texts.str2data(lines[i], colNum));
    }
}

// 读取文本数据
def inTrain = new ArrayList<IVector>();
def outTrain = LogicalVector.builder();
for (i in 100..300) {
    readData("lmp/.lll-out/.XDATCAR-out/${i}.dat", inTrain);
    // 已知前 20 个为 true
    for (j in 0..<20) outTrain.add(true);
    for (j in 20..<100) outTrain.add(false);
}
outTrain = outTrain.build();

// 调整正负样本比例一致
def positiveIndex =   outTrain .where();
def negativeIndex = (~outTrain).where();
negativeIndex.shuffle();
negativeIndex = negativeIndex[0..<positiveIndex.size()];
def totalIndex = ArrayLists.merge(positiveIndex, negativeIndex);
totalIndex.shuffle();
inTrain = inTrain[totalIndex];
outTrain = outTrain[totalIndex];


def inTest = new ArrayList<IVector>();
def outTest = LogicalVector.builder();
for (i in 350..390) {
    readData("lmp/.lll-out/.XDATCAR-out/${i}.dat", inTest);
    // 已知前 20 个为 true
    for (j in 0..<20) outTest.add(true);
    for (j in 20..<100) outTest.add(false);
}
outTest = outTest.build();


// 测试单个决策树和随机森林的效果差距
def tree = DecisionTree.builder(inTrain, outTrain).build();
plotROC(tree, inTrain, outTrain, 'DecisionTree-train', 1, '-.');
plotROC(tree, inTest, outTest, 'DecisionTree-test', 1);
def rf = new RandomForest(inTrain, outTrain, 500, 0.02);
plotROC(rf, inTrain, outTrain, 'RandomForest-train', 3, '-.');
plotROC(rf, inTest, outTest, 'RandomForest-test', 3);
rf.shutdown();

axis(0, 1, 0, 1);
plot([0, 1], [0, 1], null).lineType('--').width(1.0).color(0);

static def plotROC(tree, inTest, outTest, name, color, line='-') {
    // 统计预测结果表格
    int Npp = 0, Nnn = 0, Npn = 0, Nnp = 0;
    for (i in 0..<inTest.size()) {
        boolean real = outTest[i];
        boolean pred = tree.makeDecision(inTest[i]);
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
    
    println("Npp = $Npp, Npn = $Npn");
    println("Nnp = $Nnp, Nnn = $Nnn");
    
    plot([0, Ne, 1], [0, recall, 1], "roc of $name").marker('o').lineType(line).color(color);
}



