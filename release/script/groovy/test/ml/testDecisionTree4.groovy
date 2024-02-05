package test.ml

import jse.code.UT
import jse.code.collection.ArrayLists
import jse.math.vector.IVector
import jse.math.vector.LogicalVector
import jsex.ml.DecisionTree
import jsex.ml.RandomForest

import static jse.code.UT.Plot.axis
import static jse.code.UT.Plot.plot

/**
 * 测试决策树 save/load
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


// 创建并保存读取
def tree = DecisionTree.builder(inTrain, outTrain).build();
UT.IO.map2yaml(tree.asMap(), '.temp/decisionTree.yaml');
def treeLoad = DecisionTree.load(UT.IO.yaml2map('.temp/decisionTree.yaml'));

axis(0, 1, 0, 1);
plot([0, 1], [0, 1], null).lineType('--').width(1.0).color(0);
plotROC(tree    , inTrain, outTrain, 'tree-train'    , 1, '-.');
plotROC(tree    , inTest , outTest , 'tree-test'     , 1);
plotROC(treeLoad, inTrain, outTrain, 'treeLoad-train', 3, '-.');
plotROC(treeLoad, inTest , outTest , 'treeLoad-test' , 3);


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



