package example.mpc

import jse.atom.MPC
import jse.lmp.Data

import static jse.code.UT.Plot.*

// 导入 data 文件
def data = Data.read('lmp/data/data-glass')

// 根据 data 创建参数计算器 mpc 并计算 gr 和 Sq
def gr, Sq
def grFT, SqFT
try (def mpc = MPC.of(data)) {
    gr = mpc.calRDF()
    Sq = mpc.calSF()
    SqFT = mpc.RDF2SF(gr)
    grFT = mpc.SF2RDF(Sq)
}

// 绘制
plot(gr, 'RDF').color(0)
plot(Sq, 'SF').color(1)
plot(grFT, 'RDF-FT').color(0).type('--')
plot(SqFT, 'SF-FT').color(1).type('--')

