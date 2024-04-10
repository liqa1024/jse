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

// 计算 ConnectCount
def countQ6G, countq6G
try (def mpcG = MPC.of(dataG)) {
    countQ6G = mpcG.calConnectCountBOOP(6, 0.5)
    countq6G = mpcG.calConnectCountABOOP(6, 0.9)
}
def countQ6C, countq6C
try (def mpcC = MPC.of(dataC)) {
    countQ6C = mpcC.calConnectCountBOOP(6, 0.5)
    countq6C = mpcC.calConnectCountABOOP(6, 0.9)
}
def countQ6B, countq6B
try (def mpcB = MPC.of(dataB)) {
    countQ6B = mpcB.calConnectCountBOOP(6, 0.5)
    countq6B = mpcB.calConnectCountABOOP(6, 0.9)
}
def countQ6F, countq6F
try (def mpcF = MPC.of(dataF)) {
    countQ6F = mpcF.calConnectCountBOOP(6, 0.5)
    countq6F = mpcF.calConnectCountABOOP(6, 0.9)
}

// 输出平均值
println("Mean of connect count Q6 of glass:   ${countQ6G.mean()}")
println("Mean of connect count Q6 of crystal: ${countQ6C.mean()}")
println("Mean of connect count Q6 of BCC:     ${countQ6B.mean()}")
println("Mean of connect count Q6 of FCC:     ${countQ6F.mean()}")
println()
println("Mean of connect count q6 of glass:   ${countq6G.mean()}")
println("Mean of connect count q6 of crystal: ${countq6C.mean()}")
println("Mean of connect count q6 of BCC:     ${countq6B.mean()}")
println("Mean of connect count q6 of FCC:     ${countq6F.mean()}")


// 统计分布，这里使用 Func1 提供的方法来直接获取分布，使用 _G 的版本可以让结果光滑
def distCountQ6G = Func1.distFrom(countQ6G, 0, 16, 17)
def distCountQ6C = Func1.distFrom(countQ6C, 0, 16, 17)
def distCountQ6B = Func1.distFrom(countQ6B, 0, 16, 17)
def distCountQ6F = Func1.distFrom(countQ6F, 0, 16, 17)

def distCountq6G = Func1.distFrom(countq6G, 0, 16, 17)
def distCountq6C = Func1.distFrom(countq6C, 0, 16, 17)
def distCountq6B = Func1.distFrom(countq6B, 0, 16, 17)
def distCountq6F = Func1.distFrom(countq6F, 0, 16, 17)


// 绘制统计分布，多张图这样绘制
figure().name('distribution of connect count Q6')
plot(distCountQ6G, 'glass'  )
plot(distCountQ6C, 'crystal')
plot(distCountQ6B, 'BCC'    )
plot(distCountQ6F, 'FCC'    )
xlabel('connect count Q6')
xrange(distCountQ6G.x().first(), distCountQ6G.x().last())

figure().name('distribution of connect count q6')
plot(distCountq6G, 'glass'  )
plot(distCountq6C, 'crystal')
plot(distCountq6B, 'BCC'    )
plot(distCountq6F, 'FCC'    )
xlabel('connect count q6')
xrange(distCountq6G.x().first(), distCountq6G.x().last())


//OUTPUT:
// Mean of connect count Q6 of glass:   1.617
// Mean of connect count Q6 of crystal: 12.438
// Mean of connect count Q6 of BCC:     13.98
// Mean of connect count Q6 of FCC:     12.6591796875
//
// Mean of connect count q6 of glass:   0.1755
// Mean of connect count q6 of crystal: 13.6825
// Mean of connect count q6 of BCC:     13.98
// Mean of connect count q6 of FCC:     12.6591796875

