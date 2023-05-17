package test

import com.guan.code.UT
import com.guan.system.SLURM
import com.guan.system.SSH


/** 测试新的更加易用的 SLURM */
// 创建输出目录
UT.IO.mkdir('.temp');

// 创建 ssh，用来监控
SSH_INFO = UT.IO.json2map('.SECRET/SSH_INFO.json');
ssh = new SSH(SSH_INFO);

// 创建 slurm
slurm = new SLURM('test', SSH_INFO); // 新的可以直接通过 Map 来构建，和 SSH 可以兼容同样的 map

// 提交任务
task = slurm.submitSystem('sleep 10s; echo 1');
// 获取任务状态和 id
println("state: ${task.state().name()}, jobID: ${task.jobID()}");
sleep(1000);
println("state: ${task.state().name()}, jobID: ${task.jobID()}");

// 直接根据 slurm 的指令查看
ssh.system("squeue -u ${SSH_INFO.username}");

// 等待执行完成
exitValue = task.get();
println("state: ${task.state().name()}, jobID: ${task.jobID()}");
println("exit value: ${exitValue}");


// 打包提交任务
for (int i = 0; i < 20; ++i) slurm.putSubmit("echo $i");
task = slurm.getSubmit();
// 注意 getSubmit 只能获得纯 Future，无法得到其他信息
sleep(1000);
// 直接根据 slurm 的指令查看
ssh.system("squeue -u ${SSH_INFO.username}");

// 等待执行完成
exitValue = task.get();
println("exit value: ${exitValue}");


//System.in.read();

// 关闭 slurm
slurm.shutdown();
// 关闭 ssh
ssh.shutdown();
