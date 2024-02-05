package test.lpc

import jse.lmp.NativeLmp

/**
 * 测试本地原生的 lammps 运行，
 * 测试 data 的正确性
 */

try (def lammps = new NativeLmp()) {
    println(lammps.version());
    lammps.command('units           metal');
    lammps.command('boundary        p p p');
    lammps.command('lattice         fcc 3.61');
    lammps.command('region          box prism 1 7 0 6 1 5 2 1 0');
    lammps.command('create_box      1 box');
    lammps.command('create_atoms    1 box');
    lammps.command('mass            1 63.546');
    lammps.command('write_data      lmp/.temp/test.lmpdat');
    println(lammps.box());
    // {boxlo: (3.610, 0.000, 3.610), boxhi: (25.27, 21.66, 18.05), xy: 7.220, xz: 3.610, yz: 0.000}
    println(lammps.atomDataOf('x'));
    // 576 x 3 Matrix:
    //    3.610   0.000   3.610
    //    5.415   1.805   3.610
    //    5.415   0.000   5.415
    //    7.220   0.000   3.610
    //    9.025   1.805   3.610
    //    9.025   0.000   5.415
    //    7.220   1.805   5.415
    //    10.83   0.000   3.610
    //    12.63   1.805   3.610
    //    12.64   0.000   5.415
    //    10.83   1.805   5.415
    //    ...
    println(lammps.atomDataOf('mass'));
    // 2 x 1 Matrix:
    //    0.000
    //    63.55
    println(lammps.masses());
    // 1-length Vector:
    //    63.55
    def data1 = lammps.data();
    data1.write('lmp/.temp/test1.lmpdat');
    lammps.clear();
    lammps.command('units           metal');
    lammps.command('boundary        p p p');
    lammps.loadData(data1);
    println(lammps.box());
    // {boxlo: (3.610, 0.000, 3.610), boxhi: (25.27, 21.66, 18.05), xy: 7.220, xz: 3.610, yz: 0.000}
    println(lammps.atomDataOf('x'));
    // 576 x 3 Matrix:
    //    3.610   0.000   3.610
    //    5.415   1.805   3.610
    //    5.415   0.000   5.415
    //    7.220   0.000   3.610
    //    9.025   1.805   3.610
    //    9.025   0.000   5.415
    //    7.220   1.805   5.415
    //    10.83   0.000   3.610
    //    12.63   1.805   3.610
    //    12.64   0.000   5.415
    //    10.83   1.805   5.415
    //    ...
    lammps.command('write_data      lmp/.temp/test2.lmpdat');
    lammps.data().write('lmp/.temp/test3.lmpdat');
}

