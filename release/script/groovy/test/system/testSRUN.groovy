package test.system

import jtool.code.UT
import jtool.iofile.IOFiles
import jtool.system.SSH


// 测试 srun 不能及时释放资源的问题
def ioFiles = IOFiles.create()
    .i('<self>', 'script/groovy/run/testSRUN.groovy');

try (def ssh = new SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'));) {
    ssh.system("srun -N 1 -n 1 -c 10 -p debug ./jtool ${ioFiles.i('<self>')}", ioFiles);
}
