package jse.atom;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.NewCollections;
import jse.math.function.Func1;
import jse.math.function.IFunc1;
import jse.math.matrix.IMatrix;
import jse.math.matrix.Matrices;
import jse.math.vector.IVector;
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

import static jse.code.CS.*;
import static jse.code.UT.Code.newBox;

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
@ApiStatus.Experimental
public class MultiFrameParameterCalculator extends AbstractThreadPool<ParforThreadPool> {
    private List<? extends IMatrix> mAllAtomDataXYZ; // 现在改为 Matrix 存储，每行为一个原子的 xyz 数据
    private final double mTimestep;
    private final List<IXYZ> mBoxList;
    
    private final int mFrameNum;
    private final int mAtomNum;
    private final int[] mTypeArray; // 统计所有的原子种类，用于获取 AtomData 时使用
    private final int mAtomTypeNum; // 统计所有的原子种类数目，用于获取 AtomData 时使用
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
    
    
    /**
     * 根据输入数据直接创建 MFPC
     * @param aAtomDataList 原子数据列表
     * @param aBoxList 模拟盒大小；现在也统一认为所有输入的原子坐标都经过了 shift
     * @param aTimestep 每帧原子数据的时间步长，这里认为是等间距的
     * @param aThreadNum MFPC 进行计算会使用的线程数
     * @param aMinAtomTypeNum 期望的最少原子种类数目
     */
    public MultiFrameParameterCalculator(Collection<? extends Collection<? extends IAtom>> aAtomDataList, Collection<? extends IXYZ> aBoxList, double aTimestep, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, int aMinAtomTypeNum) {
        super(new ParforThreadPool(aThreadNum));
        
        // 获取模拟盒等数据
        mBoxList = NewCollections.map(aBoxList, UT.Code::newBox);
        mTimestep = aTimestep;
        int tAtomTypeNum = aMinAtomTypeNum;
        
        // 获取帧数并进行非法输入判断
        if (aAtomDataList.isEmpty()) throw new IllegalArgumentException("aAtomDataList MUST be Non-Empty");
        mFrameNum = aAtomDataList.size();
        
        List<? extends IMatrix> tXYZArray = null;
        try {
            Iterator<? extends Collection<? extends IAtom>> it = aAtomDataList.iterator();
            Collection<? extends IAtom> tFirst = it.next();
            // 第一帧需要统计这些数据
            mAtomNum = tFirst.size();
            mId2Index = HashBiMap.create(mAtomNum);
            mTypeArray = new int[mAtomNum];
            // 使用第一帧的数据初始化所有的 xyz 矩阵
            tXYZArray = MatrixCache.getMat(mAtomNum, 3, mFrameNum);
            // 获取第一帧粒子的 xyz 数据，顺便统计 mId2Index, mTypeArray 和 mAtomTypeNum
            IMatrix subXYZArray = tXYZArray.get(0);
            XYZ tBox = XYZ.toXYZ(mBoxList.get(0));
            int tIdx = 0;
            for (IAtom tAtom : tFirst) {
                setValidXYZ_(tBox, subXYZArray, tAtom, tIdx);
                mId2Index.put(tAtom.id(), tIdx);
                int tType = tAtom.type();
                mTypeArray[tIdx] = tType;
                if (tType > tAtomTypeNum) tAtomTypeNum = tType;
                ++tIdx;
            }
            mAtomTypeNum = tAtomTypeNum;
            // 获取其余帧粒子的 xyz 数据
            for (int frame = 1; it.hasNext(); ++frame) {
                Collection<? extends IAtom> tAtomData = it.next();
                subXYZArray = tXYZArray.get(frame);
                tBox = XYZ.toXYZ(mBoxList.get(frame));
                for (IAtom tAtom : tAtomData) {
                    tIdx = mId2Index.get(tAtom.id());
                    setValidXYZ_(tBox, subXYZArray, tAtom, tIdx);
                }
            }
        } catch (Throwable t) {
            // 可能会有非法输入的错误，这里不去仔细处理，注意归还缓存即可
            if (tXYZArray != null) MatrixCache.returnMat(tXYZArray);
            throw t;
        }
        mAllAtomDataXYZ = tXYZArray;
    }
    public MultiFrameParameterCalculator(Collection<? extends Collection<? extends IAtom>> aAtomDataList, Collection<? extends IXYZ> aBoxList, double aTimestep) {this(aAtomDataList, aBoxList, aTimestep, 1);}
    public MultiFrameParameterCalculator(Collection<? extends Collection<? extends IAtom>> aAtomDataList, Collection<? extends IXYZ> aBoxList, double aTimestep, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {this(aAtomDataList, aBoxList, aTimestep, aThreadNum, 1);}
    
    public MultiFrameParameterCalculator(Collection<? extends IAtomData> aAtomDataList, double aTimestep) {this(aAtomDataList, aTimestep, 1);}
    public MultiFrameParameterCalculator(Collection<? extends IAtomData> aAtomDataList, double aTimestep, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {this(AbstractCollections.map(aAtomDataList, IAtomData::atoms), AbstractCollections.map(aAtomDataList, IAtomData::box), aTimestep, aThreadNum, UT.Code.first(aAtomDataList).atomTypeNumber());}
    
    
    /** 内部使用方法，处理精度问题造成的超出边界问题 */
    private static void setValidXYZ_(XYZ aBox, IMatrix rXYZMat, IXYZ aXYZ, int aIdx) {
        // 由于 lammps 精度的问题，需要将超出边界的进行平移
        double tX = aXYZ.x();
        if      (tX <  0.0    ) tX += aBox.mX;
        else if (tX >= aBox.mX) tX -= aBox.mX;
        rXYZMat.set(aIdx, 0, tX);
        
        double tY = aXYZ.y();
        if      (tY <  0.0    ) tY += aBox.mY;
        else if (tY >= aBox.mY) tY -= aBox.mY;
        rXYZMat.set(aIdx, 1, tY);
        
        double tZ = aXYZ.z();
        if      (tZ <  0.0    ) tZ += aBox.mZ;
        else if (tZ >= aBox.mZ) tZ -= aBox.mZ;
        rXYZMat.set(aIdx, 2, tZ);
    }
    
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
    /** 保留旧名称兼容，当时起名太随意了，居然这么久都没发现 */
    @Deprecated public final int atomNum() {return atomNumber();}
    
    
    /// 计算方法
    
    /** 通过给定矩阵获取可以进行修改的原子数据 */
    private ISettableAtomData getAtomData_(final IMatrix aData, IXYZ aBox) {
        return new SettableAtomData(new AbstractRandomAccessList<ISettableAtom>() {
            private final @Unmodifiable BiMap<Integer, Integer> mIndex2Id = mId2Index.inverse();
            @Override public ISettableAtom get(final int index) {
                return new AbstractSettableAtom() {
                    @Override public double x() {return aData.get(index, TYPE_XYZ_X_COL);}
                    @Override public double y() {return aData.get(index, TYPE_XYZ_Y_COL);}
                    @Override public double z() {return aData.get(index, TYPE_XYZ_Z_COL);}
                    @Override public int id() {return mIndex2Id.get(index);}
                    @Override public int type() {return (int)aData.get(index, TYPE_XYZ_TYPE_COL);}
                    @Override public int index() {return index;}
                    
                    @Override public ISettableAtom setX(double aX) {aData.set(index, TYPE_XYZ_X_COL, aX); return this;}
                    @Override public ISettableAtom setY(double aY) {aData.set(index, TYPE_XYZ_Y_COL, aY); return this;}
                    @Override public ISettableAtom setZ(double aZ) {aData.set(index, TYPE_XYZ_Z_COL, aZ); return this;}
                    @Override public ISettableAtom setID(int aID) {throw new UnsupportedOperationException("setID");}
                    @Override public ISettableAtom setType(int aType) {aData.set(index, TYPE_XYZ_TYPE_COL, aType); return this;}
                };
            }
            @Override public int size() {return mAtomNum;}
        }, mAtomTypeNum, newBox(aBox));
    }
    
    /**
     * 直接获取指定帧的原子数据，
     * 为了数据独立并且避免 shutdown 后数据失效这里会进行一次值拷贝
     * @author liqa
     * @param aFrame 指定帧的索引
     * @return 可以进行修改的原子数据，抹除了速度信息（如果有的话）
     */
    public ISettableAtomData getAtomData(final int aFrame) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aFrame < 0 || aFrame>=mFrameNum) throw new IllegalArgumentException("Input aFrame MUST be in range [0, "+mFrameNum+"), input: "+aFrame);
        // 采用专门矩阵存储来节省空间
        IMatrix rData = Matrices.zeros(mAtomNum, ATOM_DATA_KEYS_TYPE_XYZ.length);
        // 先统一设置好粒子种类和坐标
        IMatrix subXYZArray = mAllAtomDataXYZ.get(aFrame);
        rData.col(TYPE_XYZ_X_COL).fill(subXYZArray.col(0));
        rData.col(TYPE_XYZ_Y_COL).fill(subXYZArray.col(1));
        rData.col(TYPE_XYZ_Z_COL).fill(subXYZArray.col(2));
        for (int row = 0; row < mAtomNum; ++row) {
            rData.set(row, TYPE_XYZ_TYPE_COL, mTypeArray[row]);
        }
        // 返回结果
        return getAtomData_(rData, mBoxList.get(aFrame));
    }
    /**
     * 为了数据独立并且避免 shutdown 后数据失效这里会进行一次值拷贝
     * @author liqa
     * @return 可以进行修改的原子数据组成的抽象数组
     */
    public List<IAtomData> allAtomData() {return NewCollections.from(mFrameNum, this::getAtomData);}
    
    /**
     * 获取这些多帧数据的平均原子坐标对应的原子数据
     * <p>
     * TODO: 需要考虑 box 的变化并对其求平均
     * @author liqa
     * @param aStart 开始的帧，包含
     * @param aFrameNum 需要进行平均的帧的数目
     * @return 可以进行修改的平均后的原子数据，抹除了速度信息（如果有的话）
     */
    public ISettableAtomData getMeanAtomData(int aStart, int aFrameNum) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aStart < 0 || aStart>=mFrameNum) throw new IllegalArgumentException("Input aStart MUST be in range [0, "+mFrameNum+"), input: "+aStart);
        if (aFrameNum<=0 || aFrameNum>(mFrameNum-aStart)) throw new IllegalArgumentException("Input aFrameNum MUST be in range (0, "+(mFrameNum-aStart)+"], input: "+aFrameNum);
        
        // 采用专门矩阵存储来节省空间并加速一些计算
        final IMatrix rData = Matrices.zeros(mAtomNum, ATOM_DATA_KEYS_TYPE_XYZ.length);
        // 先统一设置好粒子种类
        for (int row = 0; row < mAtomNum; ++row) {
            rData.set(row, TYPE_XYZ_TYPE_COL, mTypeArray[row]);
        }
        // 再遍历统计坐标，这样的遍历顺序应该更加内存友好
        int tEnd = aStart + aFrameNum;
        for (int frame = aStart; frame < tEnd; ++frame) {
            IMatrix subXYZArray = mAllAtomDataXYZ.get(frame);
            rData.col(TYPE_XYZ_X_COL).plus2this(subXYZArray.col(0));
            rData.col(TYPE_XYZ_Y_COL).plus2this(subXYZArray.col(1));
            rData.col(TYPE_XYZ_Z_COL).plus2this(subXYZArray.col(2));
        }
        // 这样求平均应该会更快，因为这里的矩阵是按列存储的
        rData.col(TYPE_XYZ_X_COL).div2this(aFrameNum);
        rData.col(TYPE_XYZ_Y_COL).div2this(aFrameNum);
        rData.col(TYPE_XYZ_Z_COL).div2this(aFrameNum);
        
        // 返回结果
        return getAtomData_(rData, mBoxList.get(aStart));
    }
    public ISettableAtomData getMeanAtomData() {return getMeanAtomData(0, mFrameNum);}
    
    
    /**
     * 获取指定帧的原子坐标对应的 MPC，MPC 特有的参数会直接保持同步；
     * 返回得到的 MPC 同样需要关闭
     * @author liqa
     * @param aFrame 指定帧的索引
     * @return 平均原子坐标对应的 MPC
     */
    public MonatomicParameterCalculator getMPC(final int aFrame) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aFrame < 0 || aFrame>=mFrameNum) throw new IllegalArgumentException("Input aFrame MUST be in range [0, "+mFrameNum+"), input: "+aFrame);
        
        // 使用这种方式创建 MPC
        return new MonatomicParameterCalculator(mAtomNum, mBoxList.get(aFrame), threadNumber(), xyzMat -> {
            xyzMat.fill(mAllAtomDataXYZ.get(aFrame));
            return xyzMat;
        });
    }
    /**
     * 直接获取这些多帧数据的平均原子坐标对应的 MPC，MPC 特有的参数会直接保持同步；
     * 为了数据独立并且避免 shutdown 后数据失效这里会进行一次值拷贝；
     * 返回得到的 MPC 同样需要关闭
     * @author liqa
     * @return 平均原子坐标对应的 MPC
     */
    public @Unmodifiable List<MonatomicParameterCalculator> allMPC() {
        return NewCollections.from(mFrameNum, this::getMPC);
    }
    
    /**
     * 直接获取这些多帧数据的平均原子坐标对应的 MPC，MPC 特有的参数会直接保持同步；
     * 返回得到的 MPC 同样需要关闭
     * <p>
     * TODO: 需要考虑 box 的变化并对其求平均
     * @author liqa
     * @param aStart 开始的帧，包含
     * @param aFrameNum 需要进行平均的帧的数目
     * @return 平均原子坐标对应的 MPC
     */
    public MonatomicParameterCalculator getMeanMPC(final int aStart, final int aFrameNum) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aStart < 0 || aStart>=mFrameNum) throw new IllegalArgumentException("Input aStart MUST be in range [0, "+mFrameNum+"), input: "+aStart);
        if (aFrameNum<=0 || aFrameNum>(mFrameNum-aStart)) throw new IllegalArgumentException("Input aFrameNum MUST be in range (0, "+(mFrameNum-aStart)+"], input: "+aFrameNum);
        
        // 使用这种方式创建 MPC
        return new MonatomicParameterCalculator(mAtomNum, mBoxList.get(aStart), threadNumber(), xyzMat -> {
            xyzMat.fill(0.0);
            final int tEnd = aStart + aFrameNum;
            for (int frame = aStart; frame < tEnd; ++frame) {
                xyzMat.plus2this(mAllAtomDataXYZ.get(frame));
            }
            xyzMat.div2this(aFrameNum);
            return xyzMat;
        });
    }
    public MonatomicParameterCalculator getMeanMPC() {return getMeanMPC(0, mFrameNum);}
    
    
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
        aZ2 -= aZ2;
        return aX1*aX1 + aY1*aY1 + aZ2*aZ2;
    }
}
