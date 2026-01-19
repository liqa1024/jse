package code.apc

import jse.atom.APC
import jse.lmp.Data

import static jse.code.UT.Plot.*

// 导入 data 文件
def data = Data.read('lmp/data/data-glass')

// 根据 data 创建参数计算器 apc 并计算 gr 和 Sq
def apc = APC.of(data)
def gr = apc.calRDF_AB(1, 1)
def Sq = apc.calSF_AB(1, 1)
def SqFT = apc.RDF2SF(gr)
def grFT = apc.SF2RDF(Sq)
apc.shutdown() // optional

// 绘制
plot(gr, 'RDF').color(0)
plot(Sq, 'SF').color(1)
plot(grFT, 'RDF-FT').color(0).type('--')
plot(SqFT, 'SF-FT').color(1).type('--')

