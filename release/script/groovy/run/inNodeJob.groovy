package run

import com.jtool.system.SRUN

import static com.jtool.code.UT.Code.*

/** SLURM 在 node 中的任务再次创建任务的实例 */

/** 直接提交任务直到遇到达到极限 */
try (def exe = new SRUN(1, 10)) {
    parfor(50000, 10) {int i ->
        exe.system("echo $i");
    }
}

