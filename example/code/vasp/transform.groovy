package example.vasp

import jse.vasp.XDATCAR
import jse.lmp.Dump

// 读取现有的 vasp XDATCAR 文件
def xdatcar = XDATCAR.read('vasp/xdatcar/CuFCC108.xdatcar')
// 通过 `of` 方法将其转为 lammps dump
def dump = Dump.of(xdatcar)
// 写入文本
dump.write('.temp/example/vasp/xdatcar2dump')


// 读取现有的 lammps dump 文件
dump = Dump.read('lmp/dump/CuFCC108.lammpstrj')
// 通过 `of` 方法将其转为 XDATCAR
xdatcar = XDATCAR.of(dump, 'Cu', 'Zr') // dump 数据中不包含原子种类的名称，转为 XDATCAR/POSCAR 时可以这样来指定种类名称
// 写入文本
xdatcar.write('.temp/example/vasp/dump2xdatcar')

