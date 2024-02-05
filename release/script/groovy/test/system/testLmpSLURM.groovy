package test.system

import jse.code.UT
import jse.lmp.LmpIn
import jse.system.SLURM


/** 测试使用 SLURM 运行 lammps，以后会有 LmpRunner 直接执行 */
// 创建输出目录
UT.IO.mkdir('lmp/.temp');
// 各种目录
logPath = 'lmp/.temp/log-lmp';
inPath = 'lmp/.temp/in-lmp';

// 创建 slurm，可以这样覆盖设定的工作区间和使用核心数
SSH_INFO = UT.IO.json2map('.SECRET/SSH_INFO.json');
slurm = new SLURM('debug', 20, 20, SSH_INFO);


// 获取 LmpIn 文件
lmpIn = LmpIn.INIT_MELT_NPT_Cu();
// 修改输出目录，lammps 输出时不会自动创建目录，这个在 LmpRunner 中会自动解决（遍历一次输出文件获取要创建的目录）
lmpIn.vOutRestartPath = 'lmp/.temp/melt-Cu108-init';
// 设置势函数
lmpIn.pair_style = 'eam/alloy';
lmpIn.pair_coeff = '* * lmp/potential/ZrCu.lammps.eam Cu Zr';
// 附加势函数文件
lmpIn.i('eam', 'lmp/.potential/ZrCu.lammps.eam'); // 同样 key 可以随便起
// 写入输入文件
lmpIn.write(inPath);

// 提交任务，指定 log 输出路径，附加的输入输出文件路径
task = slurm.submitSystem("lmp_ann -in ${inPath}", logPath, lmpIn);

// 等待执行完成
task.get();

// 读取输出并输出到控制台
lines = UT.IO.readAllLines(logPath);
for (def line : lines) println(line);


// 关闭 slurm
slurm.shutdown();

