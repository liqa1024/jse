package example.lmp

import jse.lmp.Data

// 读取现有的 lammps data 文件
def data = Data.read('lmp/data/CuFCC108.lmpdat')

// 获取属性
println('atom number: ' + data.natoms())
println('masses: ' + data.masses())
println('atom at 10: ' + data.atom(10))

// 写入文本
data.write('.temp/example/lmp/dataFCC')


//OUTPUT:
// atom number: 108
// masses: 2-length Vector:
//    63.55   91.22
// atom at 10: {id: 11, type: 1, xyz: (9.025, 0.000, 1.805)}

