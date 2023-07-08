package test

import com.jtool.code.SP
import com.jtool.code.UT
import com.jtool.iofile.InFiles
import com.jtool.lmp.Dump
import com.jtool.lmp.LmpIn
import com.jtool.lmp.Lmpdat
import com.jtool.math.MathEX
import com.jtool.plot.Plotters
import com.jtool.system.SSH
import com.jtool.vasp.POSCAR

// 测试脚本调用中参数
SP.Groovy.run('script/groovy/run/runScript.groovy', '123', '456');


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
