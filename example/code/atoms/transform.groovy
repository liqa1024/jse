package code.atoms

import jse.atom.Structures
import jse.lmp.Data
import jse.lmp.Dump
import jse.vasp.POSCAR

// 创建一个 FCC 的通用数据结构
def data = Structures.fcc(4.0, 5)

// 转为 lammps 的 data
def lmpdat = Data.of(data)
// 写入文件
lmpdat.write('.temp/example/atoms/lmpdat')

// 转为 lammps 的 dump
def dump = Dump.of(data)
// 写入文件
dump.write('.temp/example/atoms/dump')

// 转为 POSCAR
def poscar = POSCAR.of(data)
// 写入文件
poscar.write('.temp/example/atoms/poscar')

