package jsex.nnap;

import jse.clib.DoubleCPointer;
import jse.clib.TorchException;
import jse.parallel.IAutoShutdown;

/**
 * jse 实现的 nnap 计算器，所有
 * nnap 相关能量和力的计算都在此实现
 * <p>
 * 考虑到 Torch 本身的内存安全性，此类设计时确保不同对象之间线程安全，
 * 而不同线程访问相同的对象线程不安全
 * <p>
 * 由于需要并行来绕开 GIL，并且考虑到效率问题，这里需要使用原生的 pytorch
 * <p>
 * 和其他的 java 中调用 pytorch 库不同的是，这里不会做内存管理，主要考虑了以下原因：
 * <pre>
 *    1. 借助 gc 来回收非 java 内存很低效，实际往往还是需要手动关闭
 *    2. 和 {@link jse.lmp.NativeLmp} 保持一致，而 lammps 库内部依旧还是需要手动管理内存
 *    3. 方便外部扩展，而不需要担心破坏自动回收的部分
 * </pre>
 * @author liqa
 */
public class NNAP implements IAutoShutdown {
    static {
        try {setSingleThread0();}
        catch (TorchException ignored) {/* 可能已经设置过，这里就不考虑 */}
    }
    private static native void setSingleThread0() throws TorchException;
    
    private final long mModelPtr;
    private boolean mDead = false;
    public NNAP(String aModelPath) throws TorchException {
        mModelPtr = load0(aModelPath);
        if (mModelPtr==0 || mModelPtr==-1) throw new TorchException("Failed to load Torch Model");
    }
    private static native long load0(String aModelPath) throws TorchException;
    
    @Override public void shutdown() {
        if (!mDead) {
            mDead = true;
            shutdown0(mModelPtr);
        }
    }
    private static native void shutdown0(long aModelPtr);
    
    public double forward(double[] aX, int aStart, int aCount) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        rangeCheck(aX.length, aStart+aCount);
        return forward0(mModelPtr, aX, aStart, aCount);
    }
    public double forward(double[] aX, int aCount) throws TorchException {return forward(aX, 0, aCount);}
    public double forward(double[] aX) throws TorchException {return forward(aX, aX.length);}
    public double forward(DoubleCPointer aX, int aCount) throws TorchException {return forward1(mModelPtr, aX.ptr_(), aCount);}
    private static native double forward0(long aModelPtr, double[] aX, int aStart, int aCount) throws TorchException;
    private static native double forward1(long aModelPtr, long aXPtr, int aCount) throws TorchException;
    
    public double backward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        rangeCheck(aX.length, aStart+aCount);
        rangeCheck(rGradX.length, rStart+aCount);
        return backward0(mModelPtr, aX, aStart, rGradX, rStart, aCount);
    }
    public double backward(double[] aX, double[] rGradX, int aCount) throws TorchException {return backward(aX, 0, rGradX, 0, aCount);}
    public double backward(double[] aX, double[] rGradX) throws TorchException {return backward(aX, rGradX, aX.length);}
    public double backward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward1(mModelPtr, aX.ptr_(), rGradX.ptr_(), aCount);}
    private static native double backward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException;
    private static native double backward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount) throws TorchException;
    
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
