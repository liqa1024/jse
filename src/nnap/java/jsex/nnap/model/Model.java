package jsex.nnap.model;

import jse.math.vector.DoubleArrayVector;
import jse.parallel.IAutoShutdown;

/**
 * 通用的 nnap 推理模型实现
 * <p>
 * 由于内部会缓存中间结果，因此此类一般来说相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
public abstract class Model implements IAutoShutdown {
    public abstract double forward(DoubleArrayVector aX) throws Exception;
    public abstract double backward(DoubleArrayVector aX, DoubleArrayVector rGradX) throws Exception;
    
    private boolean mDead = false;
    /** @return 此模型是否已经关闭 */
    public final boolean isShutdown() {return mDead;}
    @Override public final void shutdown() {
        if (mDead) return;
        mDead = true;
        shutdown_();
    }
    protected void shutdown_() {/**/}
}
