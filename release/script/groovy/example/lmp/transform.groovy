package example.lmp

import jse.lmp.Data
import jse.lmp.Dump

import static jse.code.CS.MASS

// 读取现有的 lammps dump 文件
def dump = Dump.read('lmp/dump/CuFCC108.lammpstrj')

// 通过 `of` 方法将其中某一帧转为 data 文件
def data = Data.of(dump[4], MASS.Cu, MASS.Zr) // dump 数据中不包含原子质量，因此转为 data 时可以这样来指定每个原子种类的质量
// 获取属性
println('atom number: ' + data.natoms())
println('masses: ' + data.masses())

// 写入文本
data.write('.temp/example/lmp/dump2data')


// 同样通过 `of` 方法来将单个或多个 data 转为 dump
def dump1 = Dump.of(data)
println('frame number of dump1: ' + dump1.size())
def dump4 = Dump.of([data, dump[3], dump[0], dump.last()])
println('frame number of dump4: ' + dump4.size())

// 写入文本
dump4.write('.temp/example/lmp/data2dump')


//OUTPUT:
// atom number: 108
// masses: 2-length Vector:
//    63.55   91.22
// frame number of dump1: 1
// frame number of dump4: 4

