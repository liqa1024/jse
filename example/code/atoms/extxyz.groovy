package code.atoms

import jse.atom.data.DumpXYZ

// 读取现有的多帧 xyz 文件
def dump = DumpXYZ.read('data/xyz/CuFCC108.xyz')

// 获取帧数
println('frame number: ' + dump.size())
// 直接获取某一帧
def frame = dump[4]
// 获取属性
println('atom number: ' + frame.natoms())
println('atom parameters: ' + frame.parameters())
println('atom at 10: ' + frame.atom(10))

// 写入文本
dump.write('.temp/example/extxyz/dumpFCC.xyz')


//OUTPUT:
// frame number: 51
// atom number: 108
// atom parameters: [pbc:T T T]
// atom at 10: {type: 1, xyz: (10.65, 2.333, 0.9837)}

