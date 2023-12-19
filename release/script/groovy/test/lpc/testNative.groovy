package test.lpc

import jtool.lmp.NativeLmp

/**
 * 测试本地原生的 lammps 运行
 */

try (def lammps = new NativeLmp()) {
    println(lammps.version());
}

