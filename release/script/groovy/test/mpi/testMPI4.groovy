package test.mpi

import jtool.code.UT
import jtool.parallel.MPI


/**
 * 测试 MPI 分配任务的情况
 */

MPI.initThread(args, MPI.Thread.MULTIPLE); // 需要指定 MPI.Thread.MULTIPLE 来保证线程安全

final int me = MPI.Comm.WORLD.rank();
final int np = MPI.Comm.WORLD.size();

if (me == 0) {
    def jobs = UT.Math.rand(12);
    println("job list: $jobs");
    
    UT.Timer.tic()
    UT.Par.parfor(jobs.size(), np) {int i, int rank ->
        double[] out = [0.0];
        if (rank == 0) {
            out[0] = procJob(jobs[i]);
        } else {
            MPI.Comm.WORLD.send([jobs[i]] as double[], 1, rank);
            MPI.Comm.WORLD.recv(out, 1, rank);
        }
        println("processed job[$i] by ${rank}: ${jobs[i]} -> ${out[0]}");
    }
    // 运行完成发送关闭信息
    for (rank in 1..<np) {
        MPI.Comm.WORLD.send([-1] as double[], 1, rank);
    }
    UT.Timer.toc()
    println("sum(jobs)/np: ${jobs.sum()/np}");
} else {
    while (true) {
        double[] job = [0.0];
        MPI.Comm.WORLD.recv(job, 1, 0);
        if (job[0] < 0) break;
        job[0] = procJob(job[0]);
        MPI.Comm.WORLD.send(job, 1, 0);
    }
}

MPI.shutdown();


static double procJob(double job) {
    sleep(Math.round(job*1000));
    return job * 2;
}

