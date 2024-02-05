package test.system

import jse.code.UT
import jse.system.SLURM
import jse.system.SSH


/** 测试 SLURM 打包功能 */
// 创建输出目录
UT.IO.mkdir('.temp');

// 创建 ssh，用来监控
SSH_INFO = UT.IO.json2map('.SECRET/SSH_INFO.json');
ssh = new SSH(SSH_INFO);

// 创建 slurm
slurm = new SLURM('test', 4, 20, SSH_INFO); // 此设定每节点可以有 4 个任务一起运行（需要保留一个线程给 srun 本身）


// 打包提交任务
for (int i = 0; i < 20; ++i) slurm.putSubmit("echo $i");
task = slurm.getSubmit();

sleep(1000);
// 直接根据 slurm 的指令查看
ssh.system("squeue -u ${SSH_INFO.username}");
sleep(1000);
ssh.system("squeue -u ${SSH_INFO.username}");

// 等待执行完成
exitValue = task.get();
println("exit value: ${exitValue}");


//System.in.read();

// 关闭 slurm
slurm.shutdown();
// 关闭 ssh
ssh.shutdown();
