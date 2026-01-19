package code.atoms

import jse.atom.Structures

// 晶格常数为 4.0，xyz 方向都重复 5 次，FCC 结构
def data1 = Structures.fcc(4.0, 5)

println('atom number: ' + data1.natoms())
println('atom at 10: ' + data1.atom(10))
println('box: ' + data1.box())

//OUTPUT:
// atom number: 500
// atom at 10: {id: 11, type: 1, xyz: (10.00, 0.000, 2.000)}
// box: (20.00, 20.00, 20.00)


println('==================')
// 晶格常数为 3.0，x 方向重复 2 次，y 3 次，z 4 次，BCC 结构
def data2 = Structures.bcc(3.0, 2, 3, 4)

println('atom number: ' + data2.natoms())
println('atom at 10: ' + data2.atom(10))
println('box: ' + data2.box())

//OUTPUT:
// atom number: 48
// atom at 10: {id: 11, type: 1, xyz: (3.000, 6.000, 0.000)}
// box: (6.000, 9.000, 12.00)


println('==================')
// 晶格常数为 3.0（晶胞底边六边形的边长），xyz 方向都重复 4 次，z 4 次，HCP 结构
def data3 = Structures.hcp(3.0, 4)

println('atom number: ' + data3.natoms())
println('atom at 10: ' + data3.atom(10))
println('box: ' + data3.box())

//OUTPUT:
// atom number: 256
// atom at 10: {id: 11, type: 1, xyz: (6.000, 2.000, 2.449)}
// box: (12.00, 20.78, 19.60)
