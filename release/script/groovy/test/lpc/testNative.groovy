package test.lpc

import jse.lmp.NativeLmp

/**
 * 测试本地原生的 lammps 运行
 */

try (def lammps = new NativeLmp()) {
    println(lammps.version());
}

