package com.jtool.parallel;

import com.jtool.code.UT;
import com.jtool.code.operator.IOperator1;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jtool.code.CS.Exec.JAR_PATH;
import static com.jtool.code.CS.*;
import static com.jtool.code.CS.Slurm.IS_SLURM;
import static com.jtool.code.CS.Slurm.RESOURCES_MANAGER;


/**
 * @author liqa
 * <p> 跨程序通讯的通用接口，目前基于 ZeroMQ 实现 </p>
 */
public class MPI {
    
    /**
     * 抽象的创建子程序工作器，会连接到输入的 aAddress 并持续监听输入，
     * 执行 aOpt 来获取对应输出并返回给指定地址；
     * 通过 lambda 表达式重写 aOpt 来实现各种自定义的功能
     * @author liqa
     * @param aAddress 主程序为此工作器分配的地址
     * @param aOpt 此工作器需要执行的操作
     */
    @ApiStatus.Internal public static void abstractWorkerInit(String aAddress, IOperator1<byte[], byte[]> aOpt) {
        // 子程序禁止标准输出和标准错误输出，防止因为任何意外导致的流死锁
        System.setErr(NUL_PRINT_STREAM);
        System.setOut(NUL_PRINT_STREAM);
        // 根据输入的地址创建一个回复消息的子服务器
        ZMQ.Socket tSocket = CONTEXT.createSocket(SocketType.REP);
        tSocket.setSendTimeOut(SEND_TIMEOUT);
        // 连接到分配的地址
        tSocket.connect(aAddress);
        // 挂起等待输入
        while (true) {
            byte[] tReceived = tSocket.recv();
            // 约定，接受到的数据为 null 或为空则关闭子程序（关闭信号）
            if (tReceived==null || tReceived.length==0) break;
            // 将计算结果返回，返回失败（主进程失效）则直接结束即可
            boolean tSuc = tSocket.send(aOpt.cal(tReceived));
            if (!tSuc) break;
        }
    }
    
    /**
     * 子程序工作器抽象后的对象，
     * 用于方便主程序管理和关闭子程序
     */
    private abstract static class AbstractWorker implements IAutoShutdown {
        /** 关闭此工作器 */
        @Override public final void shutdown() {
            shutdown_();
            ALL_WORKER.remove(this);
        }
        protected abstract void shutdown_();
    }
    public static final class Worker extends AbstractWorker {
        private final ZMQ.Socket mSocket;
        private boolean mDead = false;
        private Worker(ZMQ.Socket aSocket) {mSocket = aSocket;}
        
        /** 执行工作 */
        public byte[] doWork(byte @NotNull[] aInput) {
            if (aInput.length == 0) throw new RuntimeException("Zero Length input is invalid due to it means shutdown");
            boolean tSuc = mSocket.send(aInput);
            byte[] tBytes = null;
            if (tSuc) tBytes = mSocket.recv();
            return tBytes;
        }
        /** 关闭此工作器，发送终止信号后注销 mSocket */
        @Override protected void shutdown_() {
            if (mDead) return;
            mSocket.send(ZL_BYTE);
            mSocket.close();
            mDead = true;
        }
    }
    
    /** 获取一个特定工作的工作器 */
    public static Worker getWorkerOf(Class<?> aClazz, String aWorkerInitMethodName) throws Exception {
        // 先创建连接此工作器的连接
        ZMQ.Socket tSocket = CONTEXT.createSocket(SocketType.REQ);
        tSocket.setSendTimeOut(SEND_TIMEOUT);
        // 为了兼容性统一采用 tcp 连接
        int tPort = tSocket.bindToRandomPort("tcp://*");
        // 检测方法调用是否合法，为了效率不会检测后续子程序的输出
        String aAddress = "tcp://*:"+tPort;
        if (UT.Hack.findMethod_(aClazz, aWorkerInitMethodName, aAddress) == null) {
            tSocket.close();
            throw new NoSuchMethodException("No such method: " + aWorkerInitMethodName);
        }
        // 构建指令，对于 SLURM 中的情况特殊处理，需要专门分配资源
        String tCommand = String.format("java -jar %s -invoke %s.%s %s", JAR_PATH, aClazz.getName(), aWorkerInitMethodName, aAddress);
        if (IS_SLURM) {
            // 申请资源
            Slurm.Resource tResource = RESOURCES_MANAGER.assignResource(1);
            // 分配失败直接抛出错误
            if (tResource == null) {
                tSocket.close();
                throw new Exception("Not enough resource in SLURM to assign");
            }
            List<String> rCommand = new ArrayList<>();
            rCommand.add("srun");
            rCommand.add("--nodelist");         rCommand.add(String.join(",", tResource.nodelist));
            rCommand.add("--nodes");            rCommand.add(String.valueOf(tResource.nodes));
            rCommand.add("--ntasks");           rCommand.add(String.valueOf(tResource.ntasks));
            rCommand.add("--ntasks-per-node");  rCommand.add(String.valueOf(tResource.ntasksPerNode));
            rCommand.add(tCommand);
            tCommand = String.join(" ", rCommand);
        }
        // 通过执行指令创建子进程，指定连接到此地址；直接使用 Runtime 创建后台程序，减少资源占用，可以开启更多的进程；为了避免创建过多线程这里不去捕获输入输出流
        try {Runtime.getRuntime().exec(tCommand);}
        catch (Exception e) {e.printStackTrace();}
        // 返回 worker
        Worker tWorker = new Worker(tSocket);
        ALL_WORKER.add(tWorker);
        return tWorker;
    }
    
    
    private static void close() {
        for (AbstractWorker tWorker : ALL_WORKER) tWorker.shutdown_();
        ALL_WORKER.clear();
        CONTEXT.close();
    }
    
    private final static int SEND_TIMEOUT = 30000; // ms
    private final static ZContext CONTEXT;
    private final static Set<AbstractWorker> ALL_WORKER = new HashSet<>();
    static {
        // 先手动加载 UT，会自动重新设置工作目录，保证路径的正确性
        UT.IO.init();
        // 认为创建时不会出现异常
        CONTEXT = new ZContext();
        // 在 JVM 关闭时关闭 CONTEXT 以及所有的 Worker 避免遗留程序
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {close();} catch (Exception ignored) {}
        }));
    }
}
