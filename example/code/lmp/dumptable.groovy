package example.lmp

import jse.lmp.Dump

import static jse.code.UT.Math.*

// 读取现有的 lammps dump 文件
def dump = Dump.read('lmp/dump/CuFCC108.lammpstrj')

// 获取某一帧并转为表格
def table = dump[4].asTable();
// 修改 x 列
println('origin x: ' + table['x'])
table['x'].plus2this(10)
println('new x: ' + table['x'])
// 增加新列
println('origin heads: ' + table.heads())
table['rand'] = rand(table.nrows())
println('new heads: ' + table.heads())

// 输出文件
dump.write('.temp/example/lmp/dumpSet')


//OUTPUT:
// origin x: 108-length Vector:
//    2.472   5.456   3.543   7.534  ...  1.653   2.865   1.121   8.152
// new x: 108-length Vector:
//    12.47   15.46   13.54   17.53   ...   11.65   12.87   11.12   18.15
// origin heads: [id, type, x, y, z, vx, vy, vz]
// new heads: [id, type, x, y, z, vx, vy, vz, rand]

