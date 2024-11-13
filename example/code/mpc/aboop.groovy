package example.mpc

import jse.atom.MPC
import jse.atom.Structures
import jse.lmp.Data
import jse.math.function.Func1

import static jse.code.UT.Math.rng
import static jse.code.UT.Plot.*

// 这样设置种子来固定随机流
rng(123456789)

// 导入 data 文件
def dataG = Data.read('lmp/data/data-glass')
def dataC = Data.read('lmp/data/data-crystal')
// 创建固定结构
def dataB = Structures.BCC(2.0, 10).opt().perturbXYZ(0.1)
def dataF = Structures.FCC(3.0,  8).opt().perturbXYZ(0.1)

// 计算 q4，q6，w4
def q4G, q6G, w4G
try (def mpcG = MPC.of(dataG)) {
    q4G = mpcG.calABOOP(4)
    q6G = mpcG.calABOOP(6)
    w4G = mpcG.calABOOP3(4)
}
def q4C, q6C, w4C
try (def mpcC = MPC.of(dataC)) {
    q4C = mpcC.calABOOP(4)
    q6C = mpcC.calABOOP(6)
    w4C = mpcC.calABOOP3(4)
}
def q4B, q6B, w4B
try (def mpcB = MPC.of(dataB)) {
    q4B = mpcB.calABOOP(4)
    q6B = mpcB.calABOOP(6)
    w4B = mpcB.calABOOP3(4)
}
def q4F, q6F, w4F
try (def mpcF = MPC.of(dataF)) {
    q4F = mpcF.calABOOP(4)
    q6F = mpcF.calABOOP(6)
    w4F = mpcF.calABOOP3(4)
}

// 输出平均值
println("Mean of q4 of glass:   ${q4G.mean()}")
println("Mean of q4 of crystal: ${q4C.mean()}")
println("Mean of q4 of BCC:     ${q4B.mean()}")
println("Mean of q4 of FCC:     ${q4F.mean()}")
println()
println("Mean of q6 of glass:   ${q6G.mean()}")
println("Mean of q6 of crystal: ${q6C.mean()}")
println("Mean of q6 of BCC:     ${q6B.mean()}")
println("Mean of q6 of FCC:     ${q6F.mean()}")
println()
println("Mean of w4 of glass:   ${w4G.mean()}")
println("Mean of w4 of crystal: ${w4C.mean()}")
println("Mean of w4 of BCC:     ${w4B.mean()}")
println("Mean of w4 of FCC:     ${w4F.mean()}")


// 统计分布，这里使用 Func1 提供的方法来直接获取分布，使用 _G 的版本可以让结果光滑
def distq4G = Func1.distFrom_G(q4G, 0.0, 0.25, 500)
def distq4C = Func1.distFrom_G(q4C, 0.0, 0.25, 500)
def distq4B = Func1.distFrom_G(q4B, 0.0, 0.25, 500)
def distq4F = Func1.distFrom_G(q4F, 0.0, 0.25, 500)

def distq6G = Func1.distFrom_G(q6G, 0.0, 0.6, 500)
def distq6C = Func1.distFrom_G(q6C, 0.0, 0.6, 500)
def distq6B = Func1.distFrom_G(q6B, 0.0, 0.6, 500)
def distq6F = Func1.distFrom_G(q6F, 0.0, 0.6, 500)

def distw4G = Func1.distFrom_G(w4G, -0.2, 0.2, 500)
def distw4C = Func1.distFrom_G(w4C, -0.2, 0.2, 500)
def distw4B = Func1.distFrom_G(w4B, -0.2, 0.2, 500)
def distw4F = Func1.distFrom_G(w4F, -0.2, 0.2, 500)


// 绘制统计分布，多张图这样绘制
figure().name('distribution of q4')
plot(distq4G, 'glass'  )
plot(distq4C, 'crystal')
plot(distq4B, 'BCC'    )
plot(distq4F, 'FCC'    )
xlabel('q4')
xrange(distq4G.x().first(), distq4G.x().last())

figure().name('distribution of q6')
plot(distq6G, 'glass'  )
plot(distq6C, 'crystal')
plot(distq6B, 'BCC'    )
plot(distq6F, 'FCC'    )
xlabel('q6')
xrange(distq6G.x().first(), distq6G.x().last())

figure().name('distribution of w4')
plot(distw4G, 'glass'  )
plot(distw4C, 'crystal')
plot(distw4B, 'BCC'    )
plot(distw4F, 'FCC'    )
xlabel('w4')
xrange(distw4G.x().first(), distw4G.x().last())


//OUTPUT:
// Mean of q4 of glass:   0.026290468162618954
// Mean of q4 of crystal: 0.029580139168237367
// Mean of q4 of BCC:     0.040159882262763305
// Mean of q4 of FCC:     0.13594808870000363
//
// Mean of q6 of glass:   0.12213441015428728
// Mean of q6 of crystal: 0.3796104738656118
// Mean of q6 of BCC:     0.4499787412320044
// Mean of q6 of FCC:     0.4788786220797295
//
// Mean of w4 of glass:   -0.007354072706115116
// Mean of w4 of crystal: 0.03537355255281202
// Mean of w4 of BCC:     0.11134009567481694
// Mean of w4 of FCC:     -0.15448786572333384

