package code.apc

import jse.atom.APC
import jse.code.IO
import jse.lmp.Data

import static jse.code.UT.Plot.*


int N = 200
int N_G = 500
double rMax = 10.0

// 导入 data 文件
def data = Data.read('lmp/data/data-glass')

// 根据 data 创建参数计算器 apc 并计算 gr
def apc = APC.of(data)
def gr = apc.calRDF(N, rMax)

// 获取 r 值和 g 值
println('r = ' + gr.x())
println('g = ' + gr.f())
// 获取峰值的位置
println('maxR = ' + gr.op().maxX())
// 保存到 csv
IO.data2csv(gr, '.temp/example/apc/rdf.csv')


// 计算单个种类的 gr
def grCu = apc.calRDF_AB(1,1, N, rMax)
def grZr = apc.calRDF_AB(2,2, N, rMax)
def grCuZr = apc.calRDF_AB(1,2, N, rMax)
// 计算更加光滑的结果
def grCu_G = apc.calRDF_AB_G(1,1, N_G, rMax)
def grZr_G = apc.calRDF_AB_G(2,2, N_G, rMax)
def grCuZr_G = apc.calRDF_AB_G(1,2, N_G, rMax)

// 获取峰值的位置
println('maxR_CuCu = ' + grCu_G.op().maxX())
println('maxR_ZrZr = ' + grZr_G.op().maxX())
println('maxR_CuZr = ' + grCuZr_G.op().maxX())

apc.shutdown() // optional

// 绘制
plot(gr, 'RDF-All')
plot(grCu, 'RDF-CuCu')
plot(grZr, 'RDF-ZrZr')
plot(grCuZr, 'RDF-CuZr')
plot(grCu_G, null).color('gray').type('--')
plot(grZr_G, null).color('gray').type('--')
plot(grCuZr_G, null).color('gray').type('--')


//OUTPUT:
// r = 200-length Vector:
//    0.000   0.05000   0.1000   0.1500  ...  9.800   9.850   9.900   9.950
// g = 200-length Vector:
//    0.000   0.000   0.000   0.000  ...  1.038   1.065   1.055   1.055
// maxR = 2.75
// maxR_CuCu = 2.54
// maxR_ZrZr = 3.2
// maxR_CuZr = 2.8000000000000003

