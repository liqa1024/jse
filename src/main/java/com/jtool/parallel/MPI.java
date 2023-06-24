package com.jtool.parallel;

import com.jtool.code.UT;
import com.jtool.code.operator.IOperator1;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.jtool.code.CS.EXE;
import static com.jtool.code.CS.ZL_BYTE;


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
    @ApiStatus.Internal
    public static void abstractWorkerInit(String aAddress, IOperator1<byte[], byte[]> aOpt) {
        // 根据输入的地址创建一个回复消息的子服务器
        ZMQ.Socket tSocket = CONTEXT.createSocket(SocketType.REP);
        // 连接到分配的地址
        tSocket.connect(aAddress);
        // 挂起等待输入
        while (true) {
            byte[] tReceived = tSocket.recv();
            // 约定，接受到的数据为 null 或为空则关闭子程序（关闭信号）
            if (tReceived==null || tReceived.length==0) break;
            // 将计算结果返回
            tSocket.send(aOpt.cal(tReceived));
        }
    }
    
    /**
     * 子程序工作器抽象后的对象，
     * 用于方便主程序管理和关闭子程序
     */
    public static final class Worker implements IAutoShutdown {
        private final ZMQ.Socket mSocket;
        
        private Worker(ZMQ.Socket aSocket) {mSocket = aSocket;}
        
        /** 执行工作 */
        public byte[] doWork(byte @NotNull[] aInput) {
            if (aInput.length == 0) throw new RuntimeException("Zero Length input is invalid due to it means shutdown");
            mSocket.send(aInput);
            return mSocket.recv();
        }
        
        /** 关闭此工作器，发送终止信号后注销 mSocket */
        @Override public void shutdown() {
            shutdown_();
            ALL_WORKER.remove(this);
        }
        private void shutdown_() {
            mSocket.send(ZL_BYTE);
            mSocket.close();
        }
    }
    
    /** 获取一个特定工作的工作器 */
    public static Worker getWorkerOf(String aWorkerInitMethodName) {
        // 先创建连接此工作器的连接
        ZMQ.Socket tSocket = CONTEXT.createSocket(SocketType.REQ);
        // 为了兼容性统一采用 tcp 连接
        int tPort = tSocket.bindToRandomPort("tcp://*");
        // 通过执行指令创建子进程，指定连接到此地址，使用 CompletableFuture 内部的线程池来方便管理
        CompletableFuture.runAsync(() -> EXE.system(String.format("./jTool -invoke %s tcp://*:%d", aWorkerInitMethodName, tPort)));
        // 返回 worker
        Worker tWorker = new Worker(tSocket);
        ALL_WORKER.add(tWorker);
        return tWorker;
    }
    
    private static void close() {
        for (Worker tWorker : ALL_WORKER) tWorker.shutdown_();
        ALL_WORKER.clear();
        CONTEXT.close();
    }
    
    private final static ZContext CONTEXT;
    private final static Set<Worker> ALL_WORKER = new HashSet<>();
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
