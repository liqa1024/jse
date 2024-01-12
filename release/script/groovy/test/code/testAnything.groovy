package test.code

import jtool.atom.Structures
import jtool.atom.XYZ
import jtool.code.SP
import jtool.code.UT
import jtool.io.InFiles
import jtool.lmp.Dump
import jtool.lmp.LmpIn
import jtool.lmp.Lmpdat
import jtool.math.MathEX
import jtool.parallel.LocalRandom
import jtool.plot.Plotters
import jtool.system.SSH
import jtool.system.WSL
import jtool.vasp.POSCAR
import jtool.vasp.XDATCAR

import static jtool.code.UT.Math.*
import static jtool.code.UT.Plot.*

try (def wsl = new WSL()) {
    wsl.system('ls');
    wsl.system('echo 1');
}



//data = Structures.from(POSCAR.read('vasp/data/MgCu2.poscar'), 4).opt().perturbXYZ(0.25);
//plot(data);

//a = [0, 1, 2, 3, 4, 5];
//for (i in -1..-3) println(a[i]);

//println(rand());
//println(rand());
//rng(123456);
//println(rand());
//println(new Random(123456).nextDouble());
//println(new LocalRandom(123456).nextDouble());

//a = new XYZ(1, 0, 3);
//b = new XYZ(0, 4, 0);
//c = new XYZ(2, 0, 2);
//
//println(MathEX.Graph.area(a, b, c));

//// 测试脚本调用中参数
//SP.Groovy.run('script/groovy/run/runScript.groovy', '123', '456');


//// 尝试使用 lammps 计算
//def lmpIn = LmpIn.custom('lmp/in/cal-OOP');
//lmpIn
//    .i('vInDataPath', 'lmp/data/CuFCC108.lmpdat')
//    .o('vDumpPath', 'lmp/.temp/Cu108.lammpstrj');
//
//lmpIn.vInDataPath = 'lmp/data/data-glass';
//lmpIn.vDumpPath = 'lmp/.temp/out-cal';
//lmpIn.vCutoff = 3.45891;
//lmpIn.write('lmp/.temp/in-cal');
//
//def ssh = new SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'));
//
//ssh.system("srun -p debug -n 1 lmp_ann -in ${lmpIn.i('<self>')}", lmpIn);
//
//ssh.shutdown();
//
//// 读取 dump 计算平均值
//def dump = Dump.read(lmpIn.vDumpPath as String);
//println(dump.asTable()['c_Ql[1]'].opt().mean());
