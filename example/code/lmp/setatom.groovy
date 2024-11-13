package example.lmp

import jse.lmp.Data

// 读取现有的 lammps data 文件
def data = Data.read('lmp/data/CuFCC108.lmpdat')

// 获取原子（获取到的是基于 data 的应用，进行修改时 data 会同时发生修改）
def atom10 = data.atom(10)
// 原始原子信息
println('origin atom10: ' + atom10)
// 修改种类和位置
atom10.type = 2
atom10.x = 3.14
// 新的原子信息
println('new atom at 10: ' + data.atom(10)) // 由于是引用，data 也会发生修改

// 遍历修改
for (atom in data.atoms()) atom.y += 10
// 新的原子信息
println('new atom10: ' + atom10) // 由于是引用，遍历的修改也会同时反应到 atom10 中

// 输出到文本
data.write('.temp/example/lmp/dataSet')


//OUTPUT:
// origin atom10: {id: 11, type: 1, xyz: (9.025, 0.000, 1.805)}
// new atom at 10: {id: 11, type: 2, xyz: (3.140, 0.000, 1.805)}
// new atom10: {id: 11, type: 2, xyz: (3.140, 10.00, 1.805)}

