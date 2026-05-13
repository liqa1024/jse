package jse.gpu;

import jse.code.ReferenceChecker;
import jse.cptr.PointerManager;
import org.jetbrains.annotations.ApiStatus;

/**
 * 用来自动回收 cuda 指针，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
@ApiStatus.Internal
public class AutoCudaPointerHandle extends ReferenceChecker {
    public final long mPtr, mCount;
    public AutoCudaPointerHandle(PointerManager aMng, long aCount) throws CudaException {
        super(aMng);
        mPtr = CudaCore.cudaMalloc(aCount);
        CudaCore.cudaMemset(mPtr, 0, aCount); // 保证和 calloc 行为一致，我受够了这个 UB 导致的 debug 灾难
        mCount = aCount;
    }
    
    @Override protected void dispose_() throws CudaException {
        CudaCore.cudaFree(mPtr);
    }
}
