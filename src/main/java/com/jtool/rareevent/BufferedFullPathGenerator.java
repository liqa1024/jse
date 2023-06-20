package com.jtool.rareevent;

import com.jtool.atom.IHasAtomData;

import java.util.Iterator;

/**
 * 将 {@link IPathGenerator} 使用内部 Buffer 的方法转换成 {@link IFullPathGenerator} 方便使用
 * <p>
 * 对于相同的实例线程安全，获取到的不同路径实例之间线程安全，获取的相同路径实例线程不安全
 * @author liqa
 * @param <T> 获取到点的类型，对于 lammps 模拟则是原子结构信息 {@link IHasAtomData}
 */
public class BufferedFullPathGenerator<T> implements IFullPathGenerator<T> {
    private final IPathGenerator<T> mPathGenerator;
    public BufferedFullPathGenerator(IPathGenerator<T> aPathGenerator) {mPathGenerator = aPathGenerator;}
    
    
    @Override public ITimeIterator<T> fullPathInit() {return fullPathFrom(mPathGenerator.initPoint());}
    
    /** 这里还是保持一致，第一个值为 aStart（或等价于 aStart）*/
    @Override public ITimeIterator<T> fullPathFrom(final T aStart) {
        return new ITimeIterator<T>() {
            private Iterator<T> mBuffer = null;
            private T mNext = null;
            private double mStartTime = Double.NaN;
            private double mTimeConsumed = 0.0;
            
            /** 内部使用，初始化 mBuffer，会同时初始化 mNext，mStartTime，并累加 mTimeConsumed */
            private void validNextBuffer_(T aStart_) {
                // 如果有 Next 则累加花费的时间，因为下一个路径的时间可能会不连续
                if (mNext != null) mTimeConsumed += mPathGenerator.timeOf(mNext) - mStartTime;
                do {
                    mBuffer = mPathGenerator.pathFrom(aStart_).iterator();
                    // 由于存在约定，一定有一个 next，跳过
                    mNext = mBuffer.next();
                    // 如果 mBuffer 只有这一个元素则是非法的，重新获取
                } while (!mBuffer.hasNext());
                // 更新一下新的开始时间
                mStartTime = mPathGenerator.timeOf(mNext);
            }
            
            @Override public T next() {
                // 第一次调用则合法化后直接返回，保留第一个值
                if (mBuffer == null) {
                    validNextBuffer_(aStart);
                    return mNext;
                }
                // 一般操作直接合法化后 next
                if (!mBuffer.hasNext()) {
                    validNextBuffer_(mNext);
                }
                mNext = mBuffer.next();
                return mNext;
            }
            /** 获取当前位置点从初始开始消耗的时间，如果没有调用过 next 则会抛出错误 */
            @Override public double timeConsumed() {
                if (mNext == null) throw new IllegalStateException();
                return mTimeConsumed + (mPathGenerator.timeOf(mNext) - mStartTime);
            }
            
            /** 完整路径永远都有 next */
            @Override public boolean hasNext() {return true;}
        };
    }
}
