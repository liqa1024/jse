package jse.gpu;

import jse.Main;
import org.jetbrains.annotations.ApiStatus;

/**
 * 针对 cuda gpu 的通用支持，主要是创建 gpu 数组，调用自定义的核函数，获取结果等
 * <p>
 * 此类提供核心的 jni 接口转发，其他类进行包装后提供 OOP 式使用
 *
 * @author liqa
 */
@ApiStatus.Experimental
public class CudaCore {
    
    static {
        // 在 JVM 关闭时总是调用 cudaDeviceSynchronize，避免错误被吞掉
        Main.addGlobalAutoCloseable(CudaCore::cudaDeviceSynchronize);
    }
    
    public static native void cudaDeviceSynchronize() throws CudaException;
    
    static native long cudaMalloc(int aCount) throws CudaException;
    static native void cudaFree(long aPtr) throws CudaException;
    static native void cudaMemcpyH2D(long aSrc, long rDest, int aCount) throws CudaException;
    static native void cudaMemcpyD2H(long aSrc, long rDest, int aCount) throws CudaException;
    static native void cudaMemcpyD2D(long aSrc, long rDest, int aCount) throws CudaException;
}
