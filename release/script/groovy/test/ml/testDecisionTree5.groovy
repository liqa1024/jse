package test.ml

import jse.code.UT
import jse.code.collection.ArrayLists
import jse.math.vector.IVector
import jse.math.vector.LogicalVector
import jsex.ml.DecisionTree
import jsex.ml.RandomForest

import static jse.code.UT.Math.linspace
import static jse.code.UT.Math.zeros
import static jse.code.UT.Math.zeros
import static jse.code.UT.Plot.axis
import static jse.code.UT.Plot.plot

/**
 * 测试随机森林 save/load
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
    readData("vasp/.lll-out/.XDATCAR-out/${i}.dat", inTrain);
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
    readData("vasp/.lll-out/.XDATCAR-out/${i}.dat", inTest);
    // 已知前 20 个为 true
    for (j in 0..<20) outTest.add(true);
    for (j in 20..<100) outTest.add(false);
}
outTest = outTest.build();


// 读取已有模型
def rf = RandomForest.load(UT.IO.json2map('vasp/.Ce/rf.json'));

axis(0, 1, 0, 1);
plot([0, 1], [0, 1], null).lineType('--').width(1.0).color(0);
plotROC(rf, inTrain, outTrain, 'rf-train', 1, '-.');
plotROC(rf, inTest , outTest , 'rf-test' , 1);

rf.shutdown();

static def plotROC(decider, dataInput, dataOutput, name, color, line='-') {
    // 统计预测结果表格
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
}



