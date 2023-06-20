package run

import com.jtool.code.UT
import com.jtool.system.InternalSLURM


/** SLURM 在 node 中的任务再次创建任务的实例 */
def exe = new InternalSLURM(4);

// 高压测试，直接跑大量的 sleepEcho
UT.IO.write('sleepEchoIn.sh', '#!/bin/bash\nsleep 5s\necho "${1}"');
exe.system("chmod 777 sleepEchoIn.sh");

for (i in 0..<100) {
    exe.submitSystem("./sleepEchoIn.sh $i");
}

exe.shutdown();
exe.awaitTermination();
