package jtoolex.rareevent;

import jtool.atom.IAtomData;
import jtool.parallel.LocalRandom;
import jtool.parallel.AbstractHasAutoShutdown;
import jtool.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;
import java.util.List;

/**
 * 将 {@link IPathGenerator} 使用内部 Buffer 的方法转换成 {@link IFullPathGenerator} 方便使用
 * <p>
 * 对于相同的实例线程安全，获取到的不同路径实例之间线程安全，获取的相同路径实例线程不安全
 * @author liqa
 * @param <T> 获取到点的类型，对于 lammps 模拟则是原子结构信息 {@link IAtomData}
 */
@ApiStatus.Experimental
public class BufferedFullPathGenerator<T> extends AbstractHasAutoShutdown implements IFullPathGenerator<T> {
    private final IPathGenerator<T> mPathGenerator;
    private final IParameterCalculator<? super T> mParameterCalculator;
    
    public BufferedFullPathGenerator(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator) {
        mPathGenerator = aPathGenerator;
        mParameterCalculator = aParameterCalculator;
    }
    
    /** 是否在关闭此实例时顺便关闭输入的生成器和计算器 */
    @Override public BufferedFullPathGenerator<T> setDoNotShutdown(boolean aDoNotShutdown) {setDoNotShutdown_(aDoNotShutdown); return this;}
    
    /** 这里还是保持一致，第一个值为 aStart（或等价于 aStart）*/
    @Override public ITimeAndParameterIterator<T> fullPathFrom(T aStart, long aSeed) {return new BufferedIterator(aStart, aSeed);}
    @Override public ITimeAndParameterIterator<T> fullPathInit(long aSeed) {return new BufferedIterator(aSeed);}
    
    private class BufferedIterator implements ITimeAndParameterIterator<T> {
        /** 专门优化第一次调用，不去创建路径，因为可能直接满足条件 */
        private boolean mIsFirst = true;
        /** 路径部分 */
        private Iterator<? extends T> mPathIt = null;
        private T mNext;
        /** 时间部分 */
        private double mStartTime = Double.NaN;
        private double mTimeConsumed = 0.0;
        /** 此路径的局部随机数生成器，由于约定了相同实例线程不安全，并且考虑到可能存在的高并发需求，因此直接使用 {@link LocalRandom} */
        private final LocalRandom mRNG;
        
        /** 创建时进行初始化 */
        BufferedIterator(T aStart, long aSeed) {
            mRNG = new LocalRandom(aSeed);
            mNext = aStart;
        }
        BufferedIterator(long aSeed) {
            mRNG = new LocalRandom(aSeed);
            // 现在自动构造初始点，不直接使用传入的 aSeed 可以保证随机数生成器的独立性
            mNext = mPathGenerator.initPoint(mRNG.nextLong());
        }
        
        /** 内部使用，初始化 mBuffer，会同时初始化 mNext，mStartTime，并累加 mTimeConsumed */
        private void validNextBuffer_() {
            // 如果有设置初始时间则累加花费的时间，因为下一个路径的时间可能会不连续
            if (!Double.isNaN(mStartTime)) mTimeConsumed += mPathGenerator.timeOf(mNext) - mStartTime;
            List<? extends T> tBufferPath;
            do {
                // 获取路径，这里都使用原始的点
                tBufferPath = mPathGenerator.pathFrom(mNext, mRNG.nextLong());
                // 更新路径迭代器
                mPathIt = tBufferPath.iterator();
                // 由于存在约定，一定有一个 next，跳过
                mNext = mPathIt.next();
                // 如果 mBuffer 只有这一个元素则是非法的，重新获取
            } while (!mPathIt.hasNext());
            // 更新一下新的开始时间
            mStartTime = mPathGenerator.timeOf(mNext);
        }
        
        /** 这里获取到的点需要是精简的 */
        @Override public T next() {
            // 第一次调用特殊优化，直接返回
            if (mIsFirst) {
                mIsFirst = false;
                return mPathGenerator.reducedPoint(mNext);
            }
            // 一般操作直接合法化后 next
            if (mPathIt==null || !mPathIt.hasNext()) {
                validNextBuffer_();
            }
            mNext = mPathIt.next();
            return mPathGenerator.reducedPoint(mNext);
        }
        
        /** 获取当前位置点从初始开始消耗的时间，如果没有调用过 next 则会抛出错误 */
        @Override public double timeConsumed() {
            if (mIsFirst) throw new IllegalStateException();
            if (Double.isNaN(mStartTime)) return 0.0;
            return mTimeConsumed + (mPathGenerator.timeOf(mNext) - mStartTime);
        }
        
        /** 获取当前位置点的参数 λ */
        @Override public double lambda() {
            if (mIsFirst) throw new IllegalStateException();
            return mParameterCalculator.lambdaOf(mNext);
        }
        
        /** 完整路径永远都有 next */
        @Override public boolean hasNext() {return true;}
    }
    
    
    /** 默认程序结束时会顺便关闭内部的 mPathGenerator, mParameterCalculator */
    @Override protected void shutdownInternal_() {
        if (mPathGenerator instanceof IAutoShutdown) ((IAutoShutdown)mPathGenerator).shutdown();
        if (mParameterCalculator instanceof IAutoShutdown) ((IAutoShutdown)mParameterCalculator).shutdown();
    }
    @Override protected void closeInternal_() {
        if (mPathGenerator instanceof AutoCloseable) try {((AutoCloseable)mPathGenerator).close();} catch (Exception ignored) {}
        if (mParameterCalculator instanceof AutoCloseable) try {((AutoCloseable)mParameterCalculator).close();} catch (Exception ignored) {}
    }
}
