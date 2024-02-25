package example.vasp

import jse.vasp.XDATCAR

// 读取现有的 vasp XDATCAR 文件
def xdatcar = XDATCAR.read('vasp/xdatcar/CuFCC108.xdatcar')

// 获取帧数
println('frame number: ' + xdatcar.size())
// 直接获取某一帧，会获取到 POSCAR
def frame = xdatcar[4]
// 获取属性
println('atom number: ' + frame.natoms())
println('type names: ' + frame.typeNames())
println('atom at 10: ' + frame.pickAtom(10))

// 写入文本
xdatcar.write('.temp/example/vasp/xdatcarFCC')


//OUTPUT:
// frame number: 21
// atom number: 108
// type names: [Cu, Zr]
// atom at 10: {id: 11, type: 1, xyz: (5.211, 8.424, 1.354)}

