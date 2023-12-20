package test.lpc

import jtool.lmp.NativeLmp

/**
 * 测试本地原生的 lammps 运行，
 * 测试 data 的正确性
 */

try (def lammps = new NativeLmp()) {
    println(lammps.version());
    lammps.command('units           metal');
    lammps.command('boundary        p p p');
    lammps.command('lattice         fcc 3.61');
    lammps.command('region          box prism 0 7 0 6 0 4 2 1 0');
    lammps.command('create_box      1 box');
    lammps.command('create_atoms    1 box');
    lammps.command('mass            1 63.546');
    lammps.command('write_data      lmp/.temp/test.lmpdat');
    println(lammps.box());
    // {boxlo: (0.000, 0.000, 0.000), boxhi: (25.27, 21.66, 14.44), xy: 7.220, xz: 3.610, yz: 0.000}
    println(lammps.atomDataOf('x'));
    // 672 x 3 Matrix:
    //    0.000   0.000   0.000
    //    1.805   1.805   0.000
    //    1.805   0.000   1.805
    //    3.610   0.000   0.000
    //    5.415   1.805   0.000
    //    5.415   0.000   1.805
    //    3.610   1.805   1.805
    //    7.220   0.000   0.000
    //    9.025   1.805   0.000
    //    9.025   0.000   1.805
    //    7.220   1.805   1.805
    //    ...
    lammps.lmpdat().write('lmp/.temp/test2.lmpdat');
}

