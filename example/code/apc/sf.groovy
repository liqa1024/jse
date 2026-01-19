package code.apc

import jse.atom.APC
import jse.code.IO
import jse.lmp.Data

import static jse.code.UT.Plot.*


int N = 200
double qMax = 10.0

// 导入 data 文件
def data = Data.read('lmp/data/data-glass')

// 根据 data 创建参数计算器 apc 并计算 Sq
def apc = APC.of(data)
def Sq = apc.calSF(N, qMax)


// 获取 q 值和 S 值
println('q = ' + Sq.x())
println('S = ' + Sq.f())
// 获取峰值的位置
println('maxQ = ' + Sq.op().maxX())
// 保存到 csv
IO.data2csv(Sq, '.temp/example/apc/sf.csv')


// 计算单个种类的 Sq
def SqCu = apc.calSF_AB(1, 1, N, qMax)
def SqZr = apc.calSF_AB(2, 2, N, qMax)
def SqCuZr = apc.calSF_AB(1, 2, N, qMax)

// 获取峰值的位置
println('maxQ_CuCu = ' + SqCu.op().maxX())
println('maxQ_ZrZr = ' + SqZr.op().maxX())
println('maxQ_CuZr = ' + SqCuZr.op().maxX())

apc.shutdown() // optional

// 绘制
plot(Sq, 'SF-All')
plot(SqCu, 'SF-CuCu')
plot(SqZr, 'SF-ZrZr')
plot(SqCuZr, 'SF-CuZr')
axis(0.0, qMax, 0.0, Sq.f().max()*1.1)


//OUTPUT:
// q = 201-length Vector:
//    1.446   1.488   1.531   1.574   1.617  ...  9.872   9.914   9.957   10.00
// S = 201-length Vector:
//    0.000   0.9926   1.037   0.9130   0.6716  ...  1.050   1.045   1.039   1.033
// maxQ = 2.7286752741732254
// maxQ_CuCu = 2.829308436722008
// maxQ_ZrZr = 2.5637272677117124
// maxQ_CuZr = 2.6965178522168602

