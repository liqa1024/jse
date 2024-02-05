package test.ml

import jse.math.vector.LogicalVector
import jse.math.vector.Vectors
import jsex.ml.DecisionTree

import static jse.code.UT.Math.*
import static jse.code.UT.Plot.*
import static jse.code.UT.Code.*
import static jse.code.CS.*

/**
 * 测试简单的异或数据集
 */
def dataInput = [
    Vectors.from(0.0, 0.0),
    Vectors.from(1.0, 0.0),
    Vectors.from(0.0, 1.0),
    Vectors.from(1.0, 1.0)
];
def dataOutput = LogicalVector.zeros(4);
dataOutput[0] = false;
dataOutput[1] = true;
dataOutput[2] = true;
dataOutput[3] = false;


def tree = DecisionTree.builder(dataInput, dataOutput).build();


def x = linspace(-1.0, 2.0, 20);
def y = linspace(-1.0, 2.0, 20);
def xx = zeros(20, 20);
def yy = zeros(20, 20);
for (i in range(20)) {
    xx[i][ALL] = x[i];
    yy[ALL][i] = y[i];
}
xx = xx.asVecCol();
yy = yy.asVecCol();
def out = LogicalVector.zeros(20*20);
for (i in range(20*20)) {
    out[i] = tree.makeDecision(Vectors.from(xx[i], yy[i]));
}

plot(xx[ out], yy[ out], 'true' ).lineType('none').marker('o').filled();
plot(xx[~out], yy[~out], 'false').lineType('none').marker('o').filled();

