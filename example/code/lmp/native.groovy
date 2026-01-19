package code.lmp

import jse.lmp.NativeLmp

def lmp = new NativeLmp()

println('VERSION: ' + lmp.version())
// 直接运行 in 文件
lmp.file('lmp/in/CuMelt')
// 获取原子数目
println('ATOM NUMBER: ' + lmp.natoms())
println('ATOM TYPE NUMBER: ' + lmp.ntypes())
println('LOCAL ATOM NUMBER: ' + lmp.nlocal())
// 获取其他信息
println('BOX: ' + lmp.box())
println('MASSES: ' + lmp.masses())
// 直接获取 data 到内存
def data = lmp.data()
// 写入文件
data.write('.temp/example/lmp/dataMelt')
// 清空数据重新加载
lmp.clear()
lmp.command('units           metal')
lmp.command('boundary        p p p')
lmp.command('timestep        0.002')
lmp.loadData(data)

lmp.shutdown() // optional
