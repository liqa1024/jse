package com.jtool.rareevent.atom;

import com.jtool.atom.IHasAtomData;
import com.jtool.code.UT;
import com.jtool.parallel.MPI;
import com.jtool.rareevent.IParameterCalculator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;


/**
 * MPI 版本的参数计算机，计算体系中的最大的固体团簇的尺寸；
 * 为了使用上的统一，本身不包含线程池，依旧需要使用一个线程池来并行
 * <p>
 * 现在进程内并行计算这个的效率足够高，不再需要 MPI
 * @author liqa
 */
@Deprecated
public class ClusterSizeCalculatorMPI implements IParameterCalculator<IHasAtomData> {
    /** 所有的 Worker，第二个值记录是否正在工作 */
    private final Map<MPI.Worker, Boolean> mWorkers = new HashMap<>();
    /** 构造函数，指定进程数 */
    public ClusterSizeCalculatorMPI(int aProcessNum) {
        try {
            for (int i = 0; i < aProcessNum; ++i) mWorkers.put(MPI.getWorkerOf(ClusterSizeCalculatorMPI.class, "workerInit"), false);
        } catch (Exception e) {
            this.shutdown();
            throw new RuntimeException(e);
        }
    }
    
    
    /** 内部使用的向任务分配工作器的方法 */
    private synchronized @Nullable MPI.Worker assignWorker() {
        for (Map.Entry<MPI.Worker, Boolean> tEntry : mWorkers.entrySet()) {
            if (!tEntry.getValue()) {
                tEntry.setValue(true);
                return tEntry.getKey();
            }
        }
        // 所有工作器都分配了工作，输出 null
        return null;
    }
    /** 内部使用的任务完成归还工作器的方法 */
    private synchronized void returnWorker(MPI.Worker aWorker) {
        mWorkers.put(aWorker, false);
    }
    
    @Override public double lambdaOf(IHasAtomData aPoint) {
        // 先尝试获取工作器
        MPI.Worker tWorker = assignWorker();
        if (tWorker == null) {
            System.err.println("WARNING: Can NOT to assign worker for this MPI calculator temporarily, this calculation blocks until there are any free worker.");
            System.err.println("It may be caused by too large number of parallels.");
        }
        while (tWorker == null) {
            try {Thread.sleep(20);} catch (InterruptedException e) {throw new RuntimeException(e);}
            tWorker = assignWorker();
        }
        // 获取结果
        double tLambda = UT.Serial.bytes2double(tWorker.doWork(UT.Serial.atomDataXYZ2bytes(aPoint)));
        // 任务完成后需要归还工作器
        returnWorker(tWorker);
        return tLambda;
    }
    
    /** 直接遍历关闭 mWorkers，认为调用此方法时所有的计算已经完成 */
    @Override public void shutdown() {
        for (MPI.Worker tWorker : mWorkers.keySet()) tWorker.shutdown();
    }
    
    /** 内部方法，子进程工作器实际计算参数时使用 */
    @ApiStatus.Internal public static void workerInit(String aAddress) {
        try (final ClusterSizeCalculator tCal = new ClusterSizeCalculator()) {
            MPI.abstractWorkerInit(aAddress, input -> UT.Serial.double2bytes(tCal.lambdaOf(UT.Serial.bytes2atomDataXYZ(input))));
        }
    }
}
