package com.jtool.atom;

import com.jtool.parallel.AbstractHasThreadPool;
import com.jtool.parallel.ParforThreadPool;

import static com.jtool.code.CS.BOX_ONE;
import static com.jtool.code.CS.BOX_ZERO;
import static com.jtool.math.MathEX.*;

/**
 * @author liqa
 * <p> 单原子的相关的参数的计算器 </p>
 * <p> 存储 atomDataXYZ，并且暂存对应的 NeighborListGetter 来加速计算 </p>
 * <p> 认为所有边界都是周期边界条件 </p>
 * <p> 会存在一些重复的代码，为了可读性不进一步消去 </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 */
public class MonatomicParameterCalculator extends AbstractHasThreadPool<ParforThreadPool> {
    private double[][] mAtomDataXYZ; // reference of mAtomDataXYZ in mNL
    private final double[] mBox;
    private final double[] mBoxLo; // 用来记录数据是否经过了 shift
    
    private final int mAtomNum;
    private final double mUnitLen; // 平均单个原子的距离
    
    private NeighborListGetter mNL;
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {shutdown_(); System.gc();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    
    public void shutdown_() {mDead = true; mAtomDataXYZ = null; super.shutdown(); mNL.shutdown_();}
    
    public MonatomicParameterCalculator(IOrthogonalXYZData aHasOrthogonalXYZ) {this(aHasOrthogonalXYZ, 1);}
    public MonatomicParameterCalculator(IOrthogonalXYZData aHasOrthogonalXYZ, int aThreadNum) {this(aHasOrthogonalXYZ, aThreadNum, 2.0);}
    public MonatomicParameterCalculator(IOrthogonalXYZData aHasOrthogonalXYZ, int aThreadNum, double aCellStep) {this(aHasOrthogonalXYZ.orthogonalXYZ(), aHasOrthogonalXYZ.boxLo(), aHasOrthogonalXYZ.boxHi(), aThreadNum, aCellStep);}
    public MonatomicParameterCalculator(double[][] aAtomDataXYZ                                                  ) {this(aAtomDataXYZ, BOX_ONE);}
    public MonatomicParameterCalculator(double[][] aAtomDataXYZ, double[] aBox                                   ) {this(aAtomDataXYZ, BOX_ZERO, aBox);}
    public MonatomicParameterCalculator(double[][] aAtomDataXYZ, double[] aBoxLo, double[] aBoxHi                ) {this(aAtomDataXYZ, aBoxLo, aBoxHi, 1);}
    public MonatomicParameterCalculator(double[][] aAtomDataXYZ, double[] aBoxLo, double[] aBoxHi, int aThreadNum) {this(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum, 2.0);}
    /**
     * 根据输入数据直接创建 MPC
     * @param aAtomDataXYZ 粒子的 XYZ 坐标组成的矩阵
     * @param aBoxLo 模拟盒的下界，会据此将输入 aAtomDataXYZ 平移
     * @param aBoxHi 模拟盒的上界
     * @param aThreadNum MPC 进行计算会使用的线程数
     * @param aCellStep 内部用于加速近邻搜索的 LinkedCell 不同 Cell 大小的步长
     */
    MonatomicParameterCalculator(double[][] aAtomDataXYZ, double[] aBoxLo, double[] aBoxHi, int aThreadNum, double aCellStep) {
        super(new ParforThreadPool(aThreadNum));
        // 由于需要对输入进行处理，进行一次值拷贝（即使传入的 aAtomDataXYZ 可以是将亡值）
        aAtomDataXYZ = Mat.copy(aAtomDataXYZ);
        // 如果有 aBoxLo 则需要将数据 shift
        if (aBoxLo != BOX_ZERO) XYZ.shiftAtomDataXYZ(aAtomDataXYZ, aBoxLo);
        // 获取模拟盒数据
        mBox = aBoxLo==BOX_ZERO ? aBoxHi : new double[] {aBoxHi[0]-aBoxLo[0], aBoxHi[1]-aBoxLo[1], aBoxHi[2]-aBoxLo[2]};
        // 由于 lammps 精度的问题，需要将超出边界的进行平移
        XYZ.wrapOnceAtomDataXYZ(aAtomDataXYZ, mBox);
        // 保存结果
        mBoxLo = aBoxLo;
        mAtomDataXYZ = aAtomDataXYZ;
        mAtomNum = mAtomDataXYZ.length;
        mUnitLen = Math.cbrt(Vec.product(mBox)/mAtomNum);
        
        mNL = new NeighborListGetter(mAtomDataXYZ, mBox, aCellStep);
    }
    
    /// 参数设置
    /**
     * 修改线程数，如果相同则不会进行任何操作
     * @param aThreadNum 线程数目
     * @return 返回自身用于链式调用
     */
    public MonatomicParameterCalculator setThreadNum(int aThreadNum)  {if (aThreadNum!=nThreads()) setPool(new ParforThreadPool(aThreadNum)); return this;}
    /**
     * 修改 LinkedCell 的步长，如果相同则不会进行任何操作
     * @param sCellStep 步长，默认为 2.0，需要大于 1.1，理论上越小速度会越快，同时会消耗更多的内存
     * @return 返回自身用于链式调用
     */
    public MonatomicParameterCalculator setCellStep(double sCellStep) {if (sCellStep != mNL.getCellStep()) {mNL.shutdown(); mNL = new NeighborListGetter(mAtomDataXYZ, mBox, sCellStep);} return this;}
    
    
    /// 计算方法
    public double[][] calRDF(                          ) {return calRDF(100);}
    public double[][] calRDF(int aN                    ) {return calRDF(aN, Vec.min(mBox));}
    /**
     * 计算 RDF (radial distribution function，即 g(r))，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aN 指定分划的份数（默认为 100）
     * @param aRMax 指定计算的最大半径（默认为最短的 Box 长度）
     * @return gr 以及对应横坐标 r 构成的矩阵，排成两列，gr 在前 r 在后
     */
    public double[][] calRDF(int aN, final double aRMax) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        final double[] r = Func.sequence(0.0, dr, aN);
        final double[][] tdn = new double[nThreads()][aN]; // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        
        // 使用 mNL 的通用获取近邻的方法
        pool().parfor_(mAtomNum, (i, threadID) -> {
            for (double[] tXYZ_Dis : mNL.get_IDX(i, aRMax - dr*0.5, true)) {
                int tIdx = (int) Math.ceil((tXYZ_Dis[3] - dr*0.5) / dr);
                if (tIdx > 0 && tIdx < aN) ++tdn[threadID][tIdx];
            }
        });
        
        // 获取结果
        double[] dn = Mat.sum2Dest(tdn); // 使用 sum2Dest 避免重新创建对象，自动跳过串行时的情况
        double rou = dr * mAtomNum*0.5 * mAtomNum/Vec.product(mBox); // mAtomNum*0.5 为对所有原子求和需要进行的平均
        double[] tDiv = Vec.mapMultiply2Dest(Vec.ebeMultiply(r, r), 4.0*PI*rou);
        double[] gr = Vec.ebeLDivide2Dest(tDiv, dn); // 保证所有的 r，dn 不会变，而 LDivide 可以利用后续不再需要的 tDiv
        
        // 修复截断数据
        r[0] = 0.0;
        gr[0] = 0.0;
        // 输出
        return Mat.transpose(new double[][]{gr, r});
    }
    public double[][] calRDF_AB(double[][]                   aAtomDataXYZ                            ) {return calRDF_AB(aAtomDataXYZ, 100);}
    public double[][] calRDF_AB(double[][]                   aAtomDataXYZ, int aN                    ) {return calRDF_AB(aAtomDataXYZ, aN, Vec.min(mBox));}
    public double[][] calRDF_AB(double[][]                   aAtomDataXYZ, int aN, final double aRMax) {return calRDF_AB(aAtomDataXYZ, aN, aRMax, mBoxLo==null);} // 如果 mBoxLo==null 则创建 MPC 的数据是已经经过平移的，则认为输入的 aAtomDataXYZ 也是已经经过平移的
    public double[][] calRDF_AB(MonatomicParameterCalculator aMPC                                    ) {return calRDF_AB(aMPC, 100);}
    public double[][] calRDF_AB(MonatomicParameterCalculator aMPC        , int aN                    ) {return calRDF_AB(aMPC, aN, Vec.min(mBox));}
    public double[][] calRDF_AB(MonatomicParameterCalculator aMPC        , int aN, final double aRMax) {return calRDF_AB(aMPC.mAtomDataXYZ, aN, aRMax, true);} // aMPC 的 mAtomDataXYZ 都已经经过了平移
    /**
     * 计算自身与输入的 aAtomDataXYZ 之间的 RDF，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aAtomDataXYZ 另一个元素的 xyz 坐标组成的矩阵，或者输入 aMPC 即计算两个 MPC 之间的 RDF，如果初始化使用的 Box 则认为 aAtomDataXYZ 未经过平移，否则认为 aAtomDataXYZ 已经经过了平移。对于 MPC 没有这个问题
     * @param aN 指定分划的份数（默认为 100）
     * @param aRMax 指定计算的最大半径（默认为最短的 Box 长度）
     * @param aShifted 手动指定输入 aAtomDataXYZ 是否经过了平移
     * @return gr 以及对应横坐标 r 构成的矩阵，排成两列，gr 在前 r 在后
     */
    public double[][] calRDF_AB(double[][]                   aAtomDataXYZ, int aN, final double aRMax, boolean aShifted) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 处理数据平移的问题，如果未经过平移，则认为输入的数据需要经过同样的平移
        if (!aShifted) {
            aAtomDataXYZ = Mat.copy(aAtomDataXYZ);
            XYZ.shiftAtomDataXYZ(aAtomDataXYZ, mBoxLo);
        }
        final double[][] fAtomDataXYZ = aAtomDataXYZ;
        
        final double dr = aRMax/aN;
        final double[] r = Func.sequence(0.0, dr, aN);
        final double[][] tdn = new double[nThreads()][aN]; // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        
        // 使用 mNL 的通用获取近邻的方法
        pool().parfor_(fAtomDataXYZ.length, (i, threadID) -> {
            for (double[] tXYZ_Dis : mNL.get_XYZ(fAtomDataXYZ[i], aRMax - dr*0.5)) {
                int tIdx = (int) Math.ceil((tXYZ_Dis[3] - dr*0.5) / dr);
                if (tIdx > 0 && tIdx < aN) ++tdn[threadID][tIdx];
            }
        });
        
        // 获取结果
        double[] dn = Mat.sum2Dest(tdn); // 使用 sum2Dest 避免重新创建对象，自动跳过串行时的情况
        double rou = dr * fAtomDataXYZ.length * mAtomNum/Vec.product(mBox); // aAtomDataXYZ.length 为对所有原子求和需要进行的平均
        double[] tDiv = Vec.mapMultiply2Dest(Vec.ebeMultiply(r, r), 4.0*PI*rou);
        double[] gr = Vec.ebeLDivide2Dest(tDiv, dn); // 保证所有的 r，dn 不会变，而 LDivide 可以利用后续不再需要的 tDiv
        
        // 修复截断数据
        r[0] = 0.0;
        gr[0] = 0.0;
        // 输出
        return Mat.transpose(new double[][]{gr, r});
    }
    
    private final static int G_RANG = 6;
    public double[][] calRDF_G(                                         ) {return calRDF_G(1000);}
    public double[][] calRDF_G(int aN                                   ) {return calRDF_G(aN, Vec.min(mBox));}
    public double[][] calRDF_G(int aN, final double aRMax               ) {return calRDF_G(aN, aRMax, 4);}
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来计算 RDF
     * @author liqa
     * @param aN 指定分划的份数，这里需要更多的份数来得到合适的结果，默认为 1000
     * @param aRMax 指定计算的最大半径（默认为最短的 Box 长度）
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return gr 以及对应横坐标 r 构成的矩阵，排成两列，gr 在前 r 在后
     */
    public double[][] calRDF_G(int aN, final double aRMax, int aSigmaMul) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final int tRangN = aSigmaMul*G_RANG;
        final int tN = aN-tRangN;
        final double dr = aRMax/aN;
        final double[] r = Func.sequence(0.0, dr, tN);
        final double[][] tdn = new double[nThreads()][tN]; // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        
        final double[][] tArrayTemp = new double[nThreads()][2*tRangN+1]; // 事先创建的每个线程的临时数组变量，避免重复创建数组
        
        // 使用 mNL 的通用获取近邻的方法
        pool().parfor_(mAtomNum, (i, threadID) -> {
            for (double[] tXYZ_Dis : mNL.get_IDX(i, aRMax - dr*0.5, true)) {
                int tStartIdx = (int) Math.ceil((tXYZ_Dis[3] - dr*0.5) / dr) - tRangN;
                double[] tDeltaG = Func.deltaG2Dest(dr*aSigmaMul, tXYZ_Dis[3], Func.sequence2Dest(tStartIdx*dr, dr, tArrayTemp[threadID]));
                int aDestPos = tStartIdx;
                int aDataPos = 0;
                int aLength = tDeltaG.length;
                if (aDestPos < 0) {
                    aDataPos -= aDestPos;
                    aLength += aDestPos;
                    aDestPos = 0;
                }
                if (aDestPos + aLength > tN) aLength = tN-aDestPos;
                Vec.ebeAdd2Dest(tdn[threadID], tDeltaG, aDestPos, aDataPos, aLength);
            }
        });
        
        // 获取结果
        double[] dn = Mat.sum2Dest(tdn); // 使用 sum2Dest 避免重新创建对象，自动跳过串行时的情况
        double rou = mAtomNum*0.5 * mAtomNum/Vec.product(mBox); // mAtomNum*0.5 为对所有原子求和需要进行的平均
        double[] tDiv = Vec.mapMultiply2Dest(Vec.ebeMultiply(r, r), 4.0*PI*rou);
        double[] gr = Vec.ebeLDivide2Dest(tDiv, dn); // 保证所有的 r，dn 不会变，而 LDivide 可以利用后续不再需要的 tDiv
        
        // 修复截断数据
        r[0] = 0.0;
        gr[0] = 0.0;
        // 输出
        return Mat.transpose(new double[][]{gr, r});
    }
    public double[][] calRDF_AB_G(MonatomicParameterCalculator aMPC                                                   ) {return calRDF_AB_G(aMPC, 1000);}
    public double[][] calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN                                   ) {return calRDF_AB_G(aMPC, aN, Vec.min(mBox));}
    public double[][] calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN, final double aRMax               ) {return calRDF_AB_G(aMPC, aN, aRMax, 4);} // aMPC 的 mAtomDataXYZ 都已经经过了平移
    public double[][] calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN, final double aRMax, int aSigmaMul) {return calRDF_AB_G(aMPC.mAtomDataXYZ, aN, aRMax, aSigmaMul, true);} // aMPC 的 mAtomDataXYZ 都已经经过了平移
    public double[][] calRDF_AB_G(double[][]                   aAtomDataXYZ                                           ) {return calRDF_AB_G(aAtomDataXYZ, 1000);}
    public double[][] calRDF_AB_G(double[][]                   aAtomDataXYZ, int aN                                   ) {return calRDF_AB_G(aAtomDataXYZ, aN, Vec.min(mBox));}
    public double[][] calRDF_AB_G(double[][]                   aAtomDataXYZ, int aN, final double aRMax               ) {return calRDF_AB_G(aAtomDataXYZ, aN, aRMax, 4);}
    public double[][] calRDF_AB_G(double[][]                   aAtomDataXYZ, int aN, final double aRMax, int aSigmaMul) {return calRDF_AB_G(aAtomDataXYZ, aN, aRMax, aSigmaMul, mBoxLo==null);} // 如果 mBoxLo==null 则创建 MPC 的数据是已经经过平移的，则认为输入的 aAtomDataXYZ 也是已经经过平移的
    public double[][] calRDF_AB_G(double[][]                   aAtomDataXYZ, int aN, final double aRMax, int aSigmaMul, boolean aShifted) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 处理数据平移的问题，如果未经过平移，则认为输入的数据需要经过同样的平移
        if (!aShifted) {
            aAtomDataXYZ = Mat.copy(aAtomDataXYZ);
            XYZ.shiftAtomDataXYZ(aAtomDataXYZ, mBoxLo);
        }
        final double[][] fAtomDataXYZ = aAtomDataXYZ;
        
        final int tRangN = aSigmaMul*G_RANG;
        final int tN = aN-tRangN;
        final double dr = aRMax/aN;
        final double[] r = Func.sequence(0.0, dr, tN);
        final double[][] tdn = new double[nThreads()][tN]; // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        
        final double[][] tArrayTemp = new double[nThreads()][2*tRangN+1]; // 事先创建的每个线程的临时数组变量，避免重复创建数组
        
        // 使用 mNL 的通用获取近邻的方法
        pool().parfor_(fAtomDataXYZ.length, (i, threadID) -> {
            for (double[] tXYZ_Dis : mNL.get_XYZ(fAtomDataXYZ[i], aRMax - dr*0.5)) {
                int tStartIdx = (int) Math.ceil((tXYZ_Dis[3] - dr*0.5) / dr) - tRangN;
                double[] tDeltaG = Func.deltaG2Dest(dr*aSigmaMul, tXYZ_Dis[3], Func.sequence2Dest(tStartIdx*dr, dr, tArrayTemp[threadID]));
                int aDestPos = tStartIdx;
                int aDataPos = 0;
                int aLength = tDeltaG.length;
                if (aDestPos < 0) {
                    aDataPos -= aDestPos;
                    aLength += aDestPos;
                    aDestPos = 0;
                }
                if (aDestPos + aLength > tN) aLength = tN-aDestPos;
                Vec.ebeAdd2Dest(tdn[threadID], tDeltaG, aDestPos, aDataPos, aLength);
            }
        });
        
        // 获取结果
        double[] dn = Mat.sum2Dest(tdn); // 使用 sum2Dest 避免重新创建对象，自动跳过串行时的情况
        double rou = fAtomDataXYZ.length * mAtomNum/Vec.product(mBox); // aAtomDataXYZ.length 为对所有原子求和需要进行的平均
        double[] tDiv = Vec.mapMultiply2Dest(Vec.ebeMultiply(r, r), 4.0*PI*rou);
        double[] gr = Vec.ebeLDivide2Dest(tDiv, dn); // 保证所有的 r，dn 不会变，而 LDivide 可以利用后续不再需要的 tDiv
        
        // 修复截断数据
        r[0] = 0.0;
        gr[0] = 0.0;
        // 输出
        return Mat.transpose(new double[][]{gr, r});
    }
    
    
    
    public double[][] calSF(double aQMax                            ) {return calSF(aQMax, 100);}
    public double[][] calSF(double aQMax, int aN                    ) {return calSF(aQMax, aN, Vec.min(mBox));}
    public double[][] calSF(double aQMax, int aN, final double aRMax) {return calSF(aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    /**
     * 计算 SF（structural factor，即 S(q)），结构参数，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aQMax 额外指定最大计算的 q 的位置
     * @param aN 指定分划的份数（默认为 100）
     * @param aRMax 指定计算的最大半径（默认为最短的 Box 长度）
     * @param aQMin 可以手动指定最小的截断的 q（由于 pbc 的原因，过小的结果发散）
     * @return Sq 以及对应横坐标 q 构成的矩阵，排成两列，Sq 在前 q 在后
     */
    public double[][] calSF(double aQMax, int aN, final double aRMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dq = (aQMax-aQMin)/aN;
        final double[] q = Func.sequence(aQMin+dq, dq, aN);
        final double[][] tHq = new double[nThreads()][aN]; // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        
        // 使用 mNL 的通用获取近邻的方法
        pool().parfor_(mAtomNum, (i, threadID) -> {
            double[] cXYZ = mAtomDataXYZ[i];
            for (double[] tXYZ : mNL.getMHT_IDX(i, aRMax * 0.9999, true)) {
                final double tDis = XYZ.distance(tXYZ, cXYZ);
                Vec.ebeDo2Dest(tHq[threadID], q, (lhs, rhs) -> lhs + Fast.sin(rhs*tDis) / (rhs*tDis));
            }
        });
        
        // 获取结果
        double[] Hq = Mat.sum2Dest(tHq); // 使用 sum2Dest 避免重新创建对象，自动跳过串行时的情况
        double effAtomNum = mAtomNum*0.5;
        double[] Sq = Vec.mapMultiply2Dest(Hq, 1.0/effAtomNum); Hq = null; // 2Dest 后 Hq 已经失效
        Vec.mapAdd2Dest(Sq, 1);
        
        // 补充截断数据
        return Mat.transpose(new double[][] {Vec.merge(0.0, Sq), Vec.merge(aQMin, q)});
    }
    public double[][] calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax                                          ) {return calSF_AB(aMPC, aQMax, 100);}
    public double[][] calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN                                  ) {return calSF_AB(aMPC, aQMax, aN, Vec.min(mBox));}
    public double[][] calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN, final double aRMax              ) {return calSF_AB(aMPC, aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    public double[][] calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN, final double aRMax, double aQMin) {return calSF_AB(aMPC.mAtomDataXYZ, aQMax, aN, aRMax, aQMin, true);}
    public double[][] calSF_AB(double[][]                   aAtomDataXYZ, double aQMax                                          ) {return calSF_AB(aAtomDataXYZ, aQMax, 100);}
    public double[][] calSF_AB(double[][]                   aAtomDataXYZ, double aQMax, int aN                                  ) {return calSF_AB(aAtomDataXYZ, aQMax, aN, Vec.min(mBox));}
    public double[][] calSF_AB(double[][]                   aAtomDataXYZ, double aQMax, int aN, final double aRMax              ) {return calSF_AB(aAtomDataXYZ, aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    public double[][] calSF_AB(double[][]                   aAtomDataXYZ, double aQMax, int aN, final double aRMax, double aQMin) {return calSF_AB(aAtomDataXYZ, aQMax, aN, aRMax, aQMin, mBoxLo==null);}
    /**
     * 计算自身与输入的 aAtomDataXYZ 之间的 SF，只计算一个固定结构的值，因此不包含温度信息
     * @param aAtomDataXYZ 另一个元素的 xyz 坐标组成的矩阵，或者输入 aMPC 即计算两个 MPC 之间的 RDF，如果初始化使用的 Box 则认为 aAtomDataXYZ 未经过平移，否则认为 aAtomDataXYZ 已经经过了平移。对于 MPC 没有这个问题
     * @param aQMax 额外指定最大计算的 q 的位置
     * @param aN 指定分划的份数（默认为 100）
     * @param aRMax 指定计算的最大半径（默认为最短的 Box 长度）
     * @param aQMin 手动指定最小的截断的 q
     * @param aShifted 手动指定输入 aAtomDataXYZ 是否经过了平移
     * @return Sq 以及对应横坐标 q 构成的矩阵，排成两列，Sq 在前 q 在后
     */
    public double[][] calSF_AB(double[][]                   aAtomDataXYZ, double aQMax, int aN, final double aRMax, double aQMin, boolean aShifted) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 处理数据平移的问题，如果未经过平移，则认为输入的数据需要经过同样的平移
        if (!aShifted) {
            aAtomDataXYZ = Mat.copy(aAtomDataXYZ);
            XYZ.shiftAtomDataXYZ(aAtomDataXYZ, mBoxLo);
        }
        final double[][] fAtomDataXYZ = aAtomDataXYZ;
        
        final double dq = (aQMax-aQMin)/aN;
        final double[] q = Func.sequence(aQMin+dq, dq, aN);
        final double[][] tHq = new double[nThreads()][aN]; // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        
        // 使用 mNL 的通用获取近邻的方法
        pool().parfor_(fAtomDataXYZ.length, (i, threadID) -> {
            double[] cXYZ = fAtomDataXYZ[i];
            for (double[] tXYZ : mNL.getMHT_XYZ(cXYZ, aRMax * 0.9999)) {
                final double tDis = XYZ.distance(tXYZ, cXYZ);
                Vec.ebeDo2Dest(tHq[threadID], q, (lhs, rhs) -> (lhs + Fast.sin(rhs * tDis) / (rhs * tDis)));
            }
        });
        
        // 获取结果
        double[] Hq = Mat.sum2Dest(tHq); // 使用 sum2Dest 避免重新创建对象，自动跳过串行时的情况
        double effAtomNum = Math.sqrt(mAtomNum*fAtomDataXYZ.length);
        double[] Sq = Vec.mapMultiply2Dest(Hq, 1.0/effAtomNum); Hq = null; // 2Dest 后 Hq 已经失效
        Vec.mapAdd2Dest(Sq, 1);
        
        // 补充截断数据
        return Mat.transpose(new double[][] {Vec.merge(0.0, Sq), Vec.merge(aQMin, q)});
    }
    
}
