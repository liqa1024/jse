package test.ml

import jtool.code.UT
import jtool.math.table.Tables
import jtool.math.vector.Vectors
import jtoolex.ml.RandomForest

import static jtool.code.CS.ALL
import static jtool.code.UT.Math.*
import static jtool.code.UT.Plot.*

/**
 * 测试课程中给出的例子
 */

// 读取文本数据
def lines = UT.IO.csv2str('.temp/heart_2020_cleaned.csv');
def heads = lines.first();
// 手动保存到 table
def table = Tables.zeros(lines.size()-1, heads);
for (i in 1..<lines.size()) {
    def row = table.row(i-1);
    def tokens = lines[i];
    row[0 ] = tokens[0 ]=='Yes' ? 1.0 : 0.0;
    row[2 ] = tokens[2 ]=='Yes' ? 1.0 : 0.0;
    row[3 ] = tokens[3 ]=='Yes' ? 1.0 : 0.0;
    row[4 ] = tokens[4 ]=='Yes' ? 1.0 : 0.0;
    row[7 ] = tokens[7 ]=='Yes' ? 1.0 : 0.0;
    row[8 ] = tokens[8 ]=='Male' ? 1.0 : 0.0;
    row[12] = tokens[12]=='Yes' ? 1.0 : 0.0;
    row[15] = tokens[15]=='Yes' ? 1.0 : 0.0;
    row[16] = tokens[16]=='Yes' ? 1.0 : 0.0;
    row[17] = tokens[17]=='Yes' ? 1.0 : 0.0;
    row[9] = switch (tokens[9]) {
    case '18-24'       -> 0.0 ;
    case '25-29'       -> 1.0 ;
    case '30-34'       -> 2.0 ;
    case '35-39'       -> 3.0 ;
    case '40-44'       -> 4.0 ;
    case '45-49'       -> 5.0 ;
    case '50-54'       -> 6.0 ;
    case '55-59'       -> 7.0 ;
    case '60-64'       -> 8.0 ;
    case '65-69'       -> 9.0 ;
    case '70-74'       -> 10.0;
    case '75-79'       -> 11.0;
    case '80 or older' -> 12.0;
    default -> throw new RuntimeException();
    }
    row[10] = switch (tokens[10]) {
    case 'White'                          -> 0.0;
    case 'Black'                          -> 1.0;
    case 'Asian'                          -> 2.0;
    case 'American Indian/Alaskan Native' -> 3.0;
    case 'Hispanic'                       -> 4.0;
    case 'Other'                          -> 5.0;
    default -> throw new RuntimeException();
    }
    row[11] = switch (tokens[11]) {
    case 'Yes'                     -> 0.0;
    case 'No'                      -> 1.0;
    case 'No, borderline diabetes' -> 2.0;
    case 'Yes (during pregnancy)'  -> 3.0;
    default -> throw new RuntimeException();
    }
    row[13] = switch (tokens[13]) {
    case 'Poor'      -> 0.0;
    case 'Fair'      -> 1.0;
    case 'Good'      -> 2.0;
    case 'Very good' -> 3.0;
    case 'Excellent' -> 4.0;
    default -> throw new RuntimeException();
    }
    row[1 ] = tokens[1 ] as double;
    row[5 ] = tokens[5 ] as double;
    row[6 ] = tokens[6 ] as double;
    row[14] = tokens[14] as double;
}

// 前 5000 个用于测试，后面全部用于训练
def tableTest = table.refSlicer().get(0..<5000, ALL);
def tableTrain = table.refSlicer().get(5000..<table.rowNumber(), ALL);

// 调整正负样本比例一致
def isHeartDisease = tableTrain['HeartDisease'].equal(1.0);
def positiveIndex = isHeartDisease.where();
positiveIndex = Vectors.merge(positiveIndex, positiveIndex); // 正样本也扩容一倍
def negativeIndex = (~isHeartDisease).where();
negativeIndex.shuffle();
negativeIndex = negativeIndex.subVec(0, positiveIndex.size());
def totalIndex = Vectors.merge(positiveIndex, negativeIndex);
totalIndex.shuffle();
tableTrain = tableTrain.slicer().get(totalIndex, ALL);

// 第 0 列为输出，其余为输入
def tree = new RandomForest(tableTrain.refSlicer().get(ALL, h-> h!= 'HeartDisease').rows(), tableTrain['HeartDisease'].equal(1.0));

// 统计预测结果表格
def recall = zeros(20);
def Ne = zeros(20);
def ratio = linspace(0.0, 1.0, 20);
UT.Timer.pbar('RandomForest Pred', 20);
for (i in 0..<20) {
    UT.Timer.pbar();
    if (ratio[i] == (double)0.0) {
        recall[i] = 1.0;
        Ne[i] = 1.0;
        continue;
    }
    if (ratio[i] == (double)1.0) {
        recall[i] = 0.0;
        Ne[i] = 0.0;
        continue;
    }
    int Npp = 0, Nnn = 0, Npn = 0, Nnp = 0;
    for (row in tableTest.rows()) {
        boolean real = row[0] == (double)1.0;
        boolean pred = tree.makeDecision(row[1..<row.size()], ratio[i] as double);
        if (real) {
            if (pred) ++Npp;
            else ++Npn;
        } else {
            if (pred) ++Nnp;
            else ++Nnn;
        }
    }
    recall[i] = Npp / (Npp + Npn);
    Ne[i] = Nnp / (Nnn + Nnp);
}

//println("Npp = $Npp, Npn = $Npn");
//println("Nnp = $Nnp, Nnn = $Nnn");

plot([0, 1], [0, 1], null).lineType('--').width(1.0);
plot(Ne, recall, 'roc').marker('o');
axis(0, 1, 0, 1);

tree.shutdown();
