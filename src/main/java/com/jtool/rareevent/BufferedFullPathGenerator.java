package com.jtool.rareevent;

import com.jtool.atom.IHasAtomData;
import com.jtool.parallel.AbstractHasThreadPool;
import com.jtool.parallel.IExecutorEX;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 将 {@link IPathGenerator} 使用内部 Buffer 的方法转换成 {@link IFullPathGenerator} 方便使用
 * <p>
 * 对于相同的实例线程安全，获取到的不同路径实例之间线程安全，获取的相同路径实例线程不安全
 * @author liqa
 * @param <T> 获取到点的类型，对于 lammps 模拟则是原子结构信息 {@link IHasAtomData}
 */
public class BufferedFullPathGenerator<T> implements IFullPathGenerator<T> {
    private final IPathGenerator<T> mPathGenerator;
    private final IParameterCalculator<? super T> mParameterCalculator;
    private final int mParameterThreadNum;
    private boolean mDoNotClose = false;
    
    public BufferedFullPathGenerator(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, int aParameterThreadNum) {
        mPathGenerator = aPathGenerator;
        mParameterCalculator = aParameterCalculator;
        mParameterThreadNum = aParameterThreadNum;
    }
    public BufferedFullPathGenerator(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator) {this(aPathGenerator, aParameterCalculator, -1);}
    
    /** 是否在关闭此实例时顺便关闭输入的生成器和计算器 */
    public BufferedFullPathGenerator<T> setDoNotClose(boolean aDoNotClose) {mDoNotClose = aDoNotClose; return this;}
    
    /** 这里还是保持一致，第一个值为 aStart（或等价于 aStart）*/
    @Override public ITimeAndParameterIterator<T> fullPathFrom(final T aStart) {return new BufferedIterator(aStart);}
    @Override public ITimeAndParameterIterator<T> fullPathInit() {return fullPathFrom(mPathGenerator.initPoint());}
    
    private class BufferedIterator extends AbstractHasThreadPool<IExecutorEX> implements ITimeAndParameterIterator<T> {
        /** 路径部分 */
        private Iterator<T> mPathIt = null;
        private T mNext;
        /** 时间部分 */
        private double mStartTime = Double.NaN;
        private double mTimeConsumed = 0.0;
        /** 参数部分 */
        private Iterator<T> mParameterIt = null;
        private final @Nullable LinkedList<Future<Double>> mBufferParameterGetter;
        private int mBufferSize = 0;
        
        
        /** 创建时进行初始化，初始化第一个 mBufferPathGetter */
        public BufferedIterator(T aStart) {
            super(mParameterThreadNum>0 ? newPool(mParameterThreadNum) : SERIAL_EXECUTOR);
            mBufferParameterGetter = mParameterThreadNum>0 ? new LinkedList<>() : null;
            if (mParameterThreadNum > 0) mBufferSize = 1; // bufferSize 从 1 开始
            mNext = aStart;
        }
        
        /** 内部使用，初始化 mBuffer，会同时初始化 mNext，mStartTime，并累加 mTimeConsumed */
        private void validNextBuffer_() {
            // 如果有设置初始时间则累加花费的时间，因为下一个路径的时间可能会不连续
            if (!Double.isNaN(mStartTime)) mTimeConsumed += mPathGenerator.timeOf(mNext) - mStartTime;
            List<T> tBufferPath;
            do {
                // 获取路径
                tBufferPath = mPathGenerator.pathFrom(mNext);
                // 更新路径迭代器
                mPathIt = tBufferPath.iterator();
                // 由于存在约定，一定有一个 next，跳过
                mNext = mPathIt.next();
                // 如果 mBuffer 只有这一个元素则是非法的，重新获取
            } while (!mPathIt.hasNext());
            // 更新一下新的开始时间
            mStartTime = mPathGenerator.timeOf(mNext);
            
            // 更新参数迭代器
            if (mBufferParameterGetter != null) {
                mParameterIt = tBufferPath.iterator();
                // 提交后台参数计算任务
                mBufferParameterGetter.clear();
                for (int i = 0; i < mBufferSize && mParameterIt.hasNext(); ++i) {
                    mBufferParameterGetter.addLast(pool().submit(() -> mParameterCalculator.lambdaOf(mParameterIt.next())));
                }
            }
        }
        
        @Override public T next() {
            // 第一次调用则合法化后直接返回，保留第一个值
            if (mPathIt == null) {
                validNextBuffer_();
                return mNext;
            }
            // 一般操作直接合法化后 next
            if (!mPathIt.hasNext()) {
                validNextBuffer_();
            }
            mNext = mPathIt.next();
            // 此时也需要更新参数迭代器并且持续提交任务
            if (mBufferParameterGetter != null) {
                mBufferParameterGetter.removeFirst();
                // 提交后台任务直到达到 mBufferSize
                while (mBufferParameterGetter.size()<mBufferSize && mParameterIt.hasNext()) {
                    mBufferParameterGetter.addLast(pool().submit(() -> mParameterCalculator.lambdaOf(mParameterIt.next())));
                }
                // 先提交后逐步增加 mBufferSize，保证前两步都是一次计算 1 个
                if (mBufferSize < mParameterThreadNum+1) ++mBufferSize;
            }
            return mNext;
        }
        
        /** 获取当前位置点从初始开始消耗的时间，如果没有调用过 next 则会抛出错误 */
        @Override public double timeConsumed() {
            if (Double.isNaN(mStartTime)) throw new IllegalStateException();
            return mTimeConsumed + (mPathGenerator.timeOf(mNext) - mStartTime);
        }
        
        /** 获取当前位置点的参数 λ */
        @Override public double lambda() {
            if (mPathIt == null) throw new IllegalStateException();
            if (mBufferParameterGetter != null) {
                try {return mBufferParameterGetter.getFirst().get();} catch (Exception e) {throw new RuntimeException(e);}
            } else {
                return mParameterCalculator.lambdaOf(mNext);
            }
        }
        
        /** 完整路径永远都有 next */
        @Override public boolean hasNext() {return true;}
    }
    
    
    
    /** 默认程序结束时会顺便关闭内部的 mPathGenerator, mParameterCalculator */
    @Override public void shutdown() {
        if (!mDoNotClose) {
            mPathGenerator.shutdown();
            mParameterCalculator.shutdown();
        }
    }
}
