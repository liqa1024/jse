package code.atoms

import jse.atom.Structures
import jse.lmp.Data
import jse.vasp.POSCAR

// 创建一个 FCC 的通用数据结构
def data = Structures.fcc(4.0, 10)

// 按照输入权重随机替换种类
data = data.op().mapTypeRandom(10, 20, 30, 40)

// 转为 lammps 的 data 并输出
Data.of(data).write('.temp/example/atoms/array.lmpdat')

// 转为 POSCAR 并输出
POSCAR.of(data).write('.temp/example/atoms/array.poscar')

