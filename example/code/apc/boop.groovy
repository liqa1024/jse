package code.apc

import jse.atom.APC
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
def dataB = Structures.bcc(2.0, 10).op().perturbXYZ(0.1)
def dataF = Structures.fcc(3.0,  8).op().perturbXYZ(0.1)

// 计算 Q4，Q6，W4
def apcG = APC.of(dataG)
def Q4G = apcG.calBOOP(4)
def Q6G = apcG.calBOOP(6)
def W4G = apcG.calBOOP3(4)
apcG.shutdown() // optional

def apcC = APC.of(dataC)
def Q4C = apcC.calBOOP(4)
def Q6C = apcC.calBOOP(6)
def W4C = apcC.calBOOP3(4)
apcC.shutdown() // optional

def apcB = APC.of(dataB)
def Q4B = apcB.calBOOP(4)
def Q6B = apcB.calBOOP(6)
def W4B = apcB.calBOOP3(4)
apcB.shutdown() // optional

def apcF = APC.of(dataF)
def Q4F = apcF.calBOOP(4)
def Q6F = apcF.calBOOP(6)
def W4F = apcF.calBOOP3(4)
apcF.shutdown() // optional

// 输出平均值
println("Mean of Q4 of glass:   ${Q4G.mean()}")
println("Mean of Q4 of crystal: ${Q4C.mean()}")
println("Mean of Q4 of BCC:     ${Q4B.mean()}")
println("Mean of Q4 of FCC:     ${Q4F.mean()}")
println()
println("Mean of Q6 of glass:   ${Q6G.mean()}")
println("Mean of Q6 of crystal: ${Q6C.mean()}")
println("Mean of Q6 of BCC:     ${Q6B.mean()}")
println("Mean of Q6 of FCC:     ${Q6F.mean()}")
println()
println("Mean of W4 of glass:   ${W4G.mean()}")
println("Mean of W4 of crystal: ${W4C.mean()}")
println("Mean of W4 of BCC:     ${W4B.mean()}")
println("Mean of W4 of FCC:     ${W4F.mean()}")


// 统计分布，这里使用 Func1 提供的方法来直接获取分布，使用 _G 的版本可以让结果光滑
def distQ4G = Func1.distFrom_G(Q4G, 0.0, 0.25, 500)
def distQ4C = Func1.distFrom_G(Q4C, 0.0, 0.25, 500)
def distQ4B = Func1.distFrom_G(Q4B, 0.0, 0.25, 500)
def distQ4F = Func1.distFrom_G(Q4F, 0.0, 0.25, 500)

def distQ6G = Func1.distFrom_G(Q6G, 0.0, 0.6, 500)
def distQ6C = Func1.distFrom_G(Q6C, 0.0, 0.6, 500)
def distQ6B = Func1.distFrom_G(Q6B, 0.0, 0.6, 500)
def distQ6F = Func1.distFrom_G(Q6F, 0.0, 0.6, 500)

def distW4G = Func1.distFrom_G(W4G, -0.2, 0.2, 500)
def distW4C = Func1.distFrom_G(W4C, -0.2, 0.2, 500)
def distW4B = Func1.distFrom_G(W4B, -0.2, 0.2, 500)
def distW4F = Func1.distFrom_G(W4F, -0.2, 0.2, 500)


// 绘制统计分布，多张图这样绘制
figure().name('distribution of Q4')
plot(distQ4G, 'glass'  )
plot(distQ4C, 'crystal')
plot(distQ4B, 'BCC'    )
plot(distQ4F, 'FCC'    )
xlabel('Q4')
xrange(distQ4G.x().first(), distQ4G.x().last())

figure().name('distribution of Q6')
plot(distQ6G, 'glass'  )
plot(distQ6C, 'crystal')
plot(distQ6B, 'BCC'    )
plot(distQ6F, 'FCC'    )
xlabel('Q6')
xrange(distQ6G.x().first(), distQ6G.x().last())

figure().name('distribution of W4')
plot(distW4G, 'glass'  )
plot(distW4C, 'crystal')
plot(distW4B, 'BCC'    )
plot(distW4F, 'FCC'    )
xlabel('W4')
xrange(distW4G.x().first(), distW4G.x().last())


//OUTPUT:
// Mean of Q4 of glass:   0.08728394247381817
// Mean of Q4 of crystal: 0.07240307373895653
// Mean of Q4 of BCC:     0.07485692305181178
// Mean of Q4 of FCC:     0.15600087055713363
//
// Mean of Q6 of glass:   0.3661153576744139
// Mean of Q6 of crystal: 0.42662427799188424
// Mean of Q6 of BCC:     0.4589953178875175
// Mean of Q6 of FCC:     0.49133802632044926
//
// Mean of W4 of glass:   -0.025397134886252924
// Mean of W4 of crystal: -0.010124805423113597
// Mean of W4 of BCC:     0.021556528136214895
// Mean of W4 of FCC:     -0.1037359751956782

