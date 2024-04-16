package jse.atom;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jse.code.UT;
import jse.code.collection.NewCollections;
import jse.math.function.Func1;
import jse.math.function.IFunc1;
import jse.math.matrix.IMatrix;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vectors;
import jse.parallel.AbstractThreadPool;
import jse.cache.MatrixCache;
import jse.parallel.ParforThreadPool;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * 多帧的单原子参数计算器，自动根据 ID 来进行排序，
 * 这里会顺便记录原子种类来进行统计输出原子数据；
 * 主要用于计算原子随时间运动的性质
 * <p>
 * 认为所有边界都是周期边界条件
 * <p>
 * 认为时间间隔是均匀的，并且粒子数和种类都不会发生改变
 * @author liqa
 */
@SuppressWarnings("FieldCanBeLocal")
@ApiStatus.Experimental
public class MultiFrameParameterCalculator extends AbstractThreadPool<ParforThreadPool> {
    private List<? extends IMatrix> mAllAtomDataXYZ; // 现在改为 Matrix 存储，每行为一个原子的 xyz 数据
    private final double mTimestep;
    private final List<IBox> mBoxList;
    
    private final int mFrameNum;
    private final int mAtomNum;
    private final IIntVector mTypeVec; // 统计所有的原子种类
    private final int mAtomTypeNum; // 统计所有的原子种类数目
    private final @Unmodifiable BiMap<Integer, Integer> mId2Index; // 原子的 id 转为存储在 AtomDataXYZ 的指标 index
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {
        mDead = true; super.shutdown();
        // 此时 MFPC 关闭，归还 mAllAtomDataXYZ，这种写法保证永远能获取到 mAllAtomDataXYZ 时都是合法的
        if (mAllAtomDataXYZ != null) {
            List<? extends IMatrix> oAllAtomDataXYZ = mAllAtomDataXYZ;
            mAllAtomDataXYZ = null;
            MatrixCache.returnMat(oAllAtomDataXYZ);
        }
    }
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    /** ParforThreadPool close 时不需要 awaitTermination */
    @ApiStatus.Internal @Override public void close() {shutdown();}
    
    
    /** @deprecated use {@link #of} */ @SuppressWarnings("DeprecatedIsStillUsed")
    MultiFrameParameterCalculator(Collection<? extends IAtomData> aAtomDataList, double aTimestep, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {
        super(new ParforThreadPool(aThreadNum));
        
        // 获取模拟盒等数据
        mBoxList = NewCollections.map(aAtomDataList, data -> data.box().copy());
        mTimestep = aTimestep;
        int tAtomTypeNum = UT.Code.first(aAtomDataList).atomTypeNumber();
        
        // 获取帧数并进行非法输入判断
        if (aAtomDataList.isEmpty()) throw new IllegalArgumentException("aAtomDataList MUST be Non-Empty");
        mFrameNum = aAtomDataList.size();
        
        List<? extends IMatrix> tXYZArray = null;
        try {
            Iterator<? extends IAtomData> it = aAtomDataList.iterator();
            IAtomData tFirst = it.next();
            // 第一帧需要统计这些数据
            mAtomNum = tFirst.atomNumber();
            mId2Index = HashBiMap.create(mAtomNum);
            mTypeVec = IntVector.zeros(mAtomNum);
            // 使用第一帧的数据初始化所有的 xyz 矩阵
            tXYZArray = MatrixCache.getMat(mAtomNum, 3, mFrameNum);
            // 获取第一帧粒子的 xyz 数据，顺便统计 mId2Index, mTypeArray 和 mAtomTypeNum
            IMatrix subXYZArray = tXYZArray.get(0);
            IBox tBox = mBoxList.get(0);
            XYZ tBuf = new XYZ();
            for (int i = 1; i < mAtomNum; ++i) {
                IAtom tAtom = tFirst.atom(i);
                MonatomicParameterCalculator.setValidXYZ_(tBox, subXYZArray, tAtom, i, tBuf);
                mId2Index.put(tAtom.id(), i);
                int tType = tAtom.type();
                mTypeVec.set(i, tType);
                if (tType > tAtomTypeNum) tAtomTypeNum = tType;
            }
            mAtomTypeNum = tAtomTypeNum;
            // 获取其余帧粒子的 xyz 数据
            for (int frame = 1; it.hasNext(); ++frame) {
                IAtomData tAtomData = it.next();
                subXYZArray = tXYZArray.get(frame);
                tBox = mBoxList.get(frame);
                for (int i = 1; i < mAtomNum; ++i) {
                    IAtom tAtom = tAtomData.atom(i);
                    int idx = mId2Index.get(tAtom.id());
                    MonatomicParameterCalculator.setValidXYZ_(tBox, subXYZArray, tAtom, idx, tBuf);
                }
            }
        } catch (Throwable t) {
            // 可能会有非法输入的错误，这里不去仔细处理，注意归还缓存即可
            if (tXYZArray != null) MatrixCache.returnMat(tXYZArray);
            throw t;
        }
        mAllAtomDataXYZ = tXYZArray;
    }
    /** @deprecated use {@link #of} */ @SuppressWarnings("DeprecatedIsStillUsed")
    MultiFrameParameterCalculator(Collection<? extends IAtomData> aAtomDataList, double aTimestep) {this(aAtomDataList, aTimestep, 1);}
    
    /**
     * 根据输入数据直接创建 MFPC
     * @param aAtomDataList 原子数据列表
     * @param aTimestep 每帧原子数据的时间步长，这里认为是等间距的
     * @param aThreadNum MFPC 进行计算会使用的线程数
     */
    public static MultiFrameParameterCalculator of(Collection<? extends IAtomData> aAtomDataList, double aTimestep, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return new MultiFrameParameterCalculator(aAtomDataList, aTimestep, aThreadNum);}
    public static MultiFrameParameterCalculator of(Collection<? extends IAtomData> aAtomDataList, double aTimestep) {return new MultiFrameParameterCalculator(aAtomDataList, aTimestep);}
    
    
    /// 参数设置
    /**
     * 修改线程数，如果相同则不会进行任何操作
     * @param aThreadNum 线程数目
     * @return 返回自身用于链式调用
     */
    public MultiFrameParameterCalculator setThreadNumber(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {if (aThreadNum != threadNumber()) setPool(new ParforThreadPool(aThreadNum)); return this;}
    
    
    /// 获取信息
    public int atomNumber() {return mAtomNum;}
    public int frameNumber() {return mFrameNum;}
    public double timestep() {return mTimestep;}
    /** @deprecated use {@link #atomNumber} */
    @Deprecated public final int atomNum() {return atomNumber();}
    
    
    /// 计算方法
    /**
     * 计算 MSD (Mean Square Displacement)
     * <p>
     * @author liqa
     * @param aN 需要计算的时间点数目，默认为 20
     * @param aTimeGap 进行平均的时间间隔，认为这个时间间隔后的系统不再相关，默认为 10*mTimestep
     * @param aMaxTime 希望的最高时间，默认为 mTimestep*mFrameNum*0.8，且不会超过此值
     * @return 对应时间下 MSD 的函数（msd 在前，t 在后）
     */
    public IFunc1 calMSD(int aN, double aTimeGap, double aMaxTime) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (mFrameNum <= 3) throw new RuntimeException("FrameNum MUST be Greater than 3 for MSD calculation, current: "+mFrameNum);
        
        // 初始化需要计算的时间序列
        final IVector tFrames = Vectors.logspace(mTimestep*2.0, Math.min(mTimestep*mFrameNum*0.8, aMaxTime), aN);
        // 通过时间点计算帧数值（不去排除重复值，如果存在）
        tFrames.operation().map2this(t -> Math.round(t / mTimestep));
        // x 坐标为通过帧数计算得到的时间值
        final IFunc1 rMSD = Func1.zeros(aN, i -> (tFrames.get(i) * mTimestep));
        
        // 获取需要间隔的帧数值
        final int tFrameGap = Math.max(1, (int)Math.round(aTimeGap / mTimestep));
        
        // 根据帧数值来计算 MSD
        pool().parfor(aN, i -> {
            double rStatNum = 0;
            double rMSDi = 0.0;
            for (int start = 0, end = (int)tFrames.get(i); end < mFrameNum; start+=tFrameGap, end+=tFrameGap) {
                IMatrix tStartFrame = mAllAtomDataXYZ.get(start);
                IMatrix tEndFrame = mAllAtomDataXYZ.get(end);
                for (int j = 0; j < mAtomNum; ++j) {
                    rMSDi += distance2(tStartFrame.get(j, 0), tStartFrame.get(j, 1), tStartFrame.get(j, 2), tEndFrame.get(j, 0), tEndFrame.get(j, 1), tEndFrame.get(j, 2));
                }
                rStatNum += mAtomNum;
            }
            // 在这里统一设置可以避免频繁访问数组
            rMSD.set(i, rMSDi/rStatNum);
        });
        
        // 输出结果
        return rMSD;
    }
    public IFunc1 calMSD(int aN, double aTimeGap) {return calMSD(aN, aTimeGap, mTimestep*mFrameNum*0.8);}
    public IFunc1 calMSD(int aN) {return calMSD(aN, 10*mTimestep);}
    public IFunc1 calMSD() {return calMSD(20);}
    
    
    
    private static double distance2(double aX1, double aY1, double aZ1, double aX2, double aY2, double aZ2) {
        aX1 -= aX2;
        aY1 -= aY2;
        aZ1 -= aZ2;
        return aX1*aX1 + aY1*aY1 + aZ1*aZ1;
    }
}
