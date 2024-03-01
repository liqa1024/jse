package example.mpc

import jse.atom.MPC
import jse.lmp.Data
import jse.math.function.Func1
import jse.plot.Plotters


// 导入 data 文件
def dataG = Data.read('lmp/data/data-glass')
def dataC = Data.read('lmp/data/data-crystal')

// 计算
def coordinationG, atomicVolumeG, cavityRadiusG
try (def mpcG = new MPC(dataG)) {
    def voronois = mpcG.calVoronoi()
    // 这样获取某个原子的 voronoi 参数
    println('coordination    of glass atom at 10: ' + voronois[10].coordination())
    println('atomic volume   of glass atom at 10: ' + voronois[10].atomicVolume())
    println('cavity radius   of glass atom at 10: ' + voronois[10].cavityRadius())
    println('voronoi indices of glass atom at 10: ' + voronois[10].index())
    // 可以通过 `*.` 直接获取所有原子的值
    coordinationG = voronois*.coordination()
    atomicVolumeG = voronois*.atomicVolume()
    cavityRadiusG = voronois*.cavityRadius()
}
def coordinationC, atomicVolumeC, cavityRadiusC
try (def mpcC = new MPC(dataC)) {
    def voronois = mpcC.calVoronoi()
    coordinationC = voronois*.coordination()
    atomicVolumeC = voronois*.atomicVolume()
    cavityRadiusC = voronois*.cavityRadius()
}

// 输出平均值，直接通过 `*.` 会得到 List 而不是 IVector，List 只有 sum() 方法
println("Mean of coordination of glass:    ${coordinationG.sum()/coordinationG.size()}")
println("Mean of coordination of crystal:  ${coordinationC.sum()/coordinationC.size()}")
println()
println("Mean of atomic volume of glass:   ${atomicVolumeG.sum()/atomicVolumeG.size()}")
println("Mean of atomic volume of crystal: ${atomicVolumeC.sum()/atomicVolumeC.size()}")
println()
println("Mean of cavity radius of glass:   ${cavityRadiusG.sum()/cavityRadiusG.size()}")
println("Mean of cavity radius of crystal: ${cavityRadiusC.sum()/cavityRadiusC.size()}")


// 统计分布，这里使用 Func1 提供的方法来直接获取分布，使用 _G 的版本可以让结果光滑
def distCoordinationG = Func1.distFrom(coordinationG, 0, 20, 21)
def distCoordinationC = Func1.distFrom(coordinationC, 0, 20, 21)

def distAtomicVolumeG = Func1.distFrom_G(atomicVolumeG, 10.0, 30.0, 400)
def distAtomicVolumeC = Func1.distFrom_G(atomicVolumeC, 10.0, 30.0, 400)

def distCavityRadiusG = Func1.distFrom_G(cavityRadiusG, 1.7, 2.5, 400)
def distCavityRadiusC = Func1.distFrom_G(cavityRadiusC, 1.7, 2.5, 400)


// 绘制统计分布，多张图这样绘制
def plt1 = Plotters.get()
plt1.plot(distCoordinationG, 'glass'  )
plt1.plot(distCoordinationC, 'crystal')
plt1.xlabel('coordination')
plt1.xrange(distCoordinationG.x().first(), distCoordinationG.x().last())
plt1.show('distribution of coordination')

def plt2 = Plotters.get()
plt2.plot(distAtomicVolumeG, 'glass'  )
plt2.plot(distAtomicVolumeC, 'crystal')
plt2.xlabel('atomic volume')
plt2.xrange(distAtomicVolumeG.x().first(), distAtomicVolumeG.x().last())
plt2.show('distribution of atomic volume')

def plt3 = Plotters.get()
plt3.plot(distCavityRadiusG, 'glass'  )
plt3.plot(distCavityRadiusC, 'crystal')
plt3.xlabel('cavity radius')
plt3.xrange(distCavityRadiusG.x().first(), distCavityRadiusG.x().last())
plt3.show('distribution of cavity radius')


//OUTPUT:
// coordination    of glass atom at 10: 11
// atomic volume   of glass atom at 10: 16.03691429956965
// cavity radius   of glass atom at 10: 2.0934715423237464
// voronoi indices of glass atom at 10: [0, 0, 0, 4, 4, 3, 0, 0, 0]
// Mean of coordination of glass:    13.9295
// Mean of coordination of crystal:  14.108
//
// Mean of atomic volume of glass:   17.73939390887716
// Mean of atomic volume of crystal: 22.16463977922477
//
// Mean of cavity radius of glass:   2.089348028123992
// Mean of cavity radius of crystal: 2.1605131099817743

