package test

import com.jtool.code.UT
import com.jtool.iofile.IOFiles
import com.jtool.system.SSH
import obj.GUT;


/** 测试 SLURM 在 node 中的任务再次创建任务的可行性 */

// 创建 ssh
def ssh = new SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'));

// 由于需要使用 jTool 本身来计算，如果没有初始化需要首先使用这个指令初始化一下 ssh 上的 jTool 环境
GUT.initJToolEnv(ssh);

def ioFiles = (new IOFiles())
    .i('<self>', 'script/groovy/run/inNodeJob.groovy');
// 提交指令
//ssh.system("srun -N 1 -n 1 -o out -c 6 ./jTool ${ioFiles.i('<self>')}", ioFiles); // 通过 key 来获取注册的文件名
ssh.system("echo -e '#!/bin/bash\\nsrun -n 1 -c 6 ./jTool ${ioFiles.i('<self>')}' | sbatch -N 1 -p debug", ioFiles); // 通过 key 来获取注册的文件名

// 关闭 ssh
ssh.shutdown();

