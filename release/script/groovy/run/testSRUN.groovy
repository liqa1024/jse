package run

import jse.system.SRUN


// 测试 srun 不能及时释放资源的问题

try (def srun = new SRUN(10)) {
    srun.system('echo 1');
    srun.system('sleep 2s');
    srun.system('echo 2');
}

try (def srun = new SRUN(10)) {
    srun.system('echo 11');
    srun.system('sleep 2s');
    srun.system('echo 22');
}
