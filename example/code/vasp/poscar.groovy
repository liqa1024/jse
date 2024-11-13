package example.vasp

import jse.vasp.POSCAR

// 读取现有的 vasp POSCAR 文件
def poscar = POSCAR.read('vasp/poscar/CuFCC108.poscar')

// 获取属性
println('atom number: ' + poscar.natoms())
println('type names: ' + poscar.typeNames())
println('atom at 10: ' + poscar.atom(10))

// 写入文本
poscar.write('.temp/example/vasp/poscarFCC')


//OUTPUT:
// atom number: 108
// type names: [Cu, Zr]
// atom at 10: {id: 11, type: 1, xyz: (9.025, 0.000, 1.805)}

