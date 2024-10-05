package jsex.nnap;

import com.google.common.collect.Lists;
import jse.atom.IAtomData;
import jse.atom.IXYZ;
import jse.atom.MonatomicParameterCalculator;
import jse.atom.XYZ;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.clib.DoubleCPointer;
import jse.clib.TorchException;
import jse.code.UT;
import jse.code.collection.ISlice;
import jse.code.collection.IntList;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.parallel.IAutoShutdown;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
    
    protected final long[] mModelPtrs;
    private boolean mDead = false;
    private final int mThreadNumber;
    
    protected final @Unmodifiable List<String> mElems;
    protected final IVector mRefEngs;
    protected final IVector mNormVec;
    protected final Basis.IBasis mBasis;
    
    @SuppressWarnings("unchecked")
    public NNAP(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException {
        mThreadNumber = aThreadNumber;
        Number tVersion = (Number)aModelInfo.get("version");
        if (tVersion != null) {
            int tVersionValue = tVersion.intValue();
            if (tVersionValue != 1) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        List<String> tElems = (List<String>)aModelInfo.get("elems");
        if (tElems == null) throw new IllegalArgumentException("No elems in ModelInfo");
        mElems = tElems;
        List<? extends Number> tRefEngs = (List<? extends Number>)aModelInfo.get("ref_engs");
        if (tRefEngs == null) throw new IllegalArgumentException("No ref_engs in ModelInfo");
        mRefEngs = Vectors.from(tRefEngs);
        List<? extends Number> tNormVec = (List<? extends Number>)aModelInfo.get("norm_vec");
        if (tNormVec == null) throw new IllegalArgumentException("No norm_vec in ModelInfo");
        mNormVec = Vectors.from(tNormVec);
        
        Map<String, ?> tBasis = (Map<String, ?>)aModelInfo.get("basis");
        if (tBasis == null) {
            tBasis = Maps.of(
                "type", "spherical_chebyshev",
                "nmax", Basis.SphericalChebyshev.DEFAULT_NMAX,
                "lmax", Basis.SphericalChebyshev.DEFAULT_LMAX,
                "rcut", Basis.SphericalChebyshev.DEFAULT_RCUT
            );
        }
        Object tBasisType = tBasis.get("type");
        if (tBasisType == null) {
            tBasisType = "spherical_chebyshev";
        }
        if (!tBasisType.equals("spherical_chebyshev")) throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
        mBasis = new Basis.SphericalChebyshev(
            ((Number)UT.Code.getWithDefault(tBasis, Basis.SphericalChebyshev.DEFAULT_NMAX, "nmax")).intValue(),
            ((Number)UT.Code.getWithDefault(tBasis, Basis.SphericalChebyshev.DEFAULT_LMAX, "lmax")).intValue(),
            ((Number)UT.Code.getWithDefault(tBasis, Basis.SphericalChebyshev.DEFAULT_RCUT, "rcut")).doubleValue()
        );
        Object tModel = aModelInfo.get("model");
        if (tModel == null) throw new IllegalArgumentException("No model data in ModelInfo");
        byte[] tModelBytes = Base64.getDecoder().decode(tModel.toString());
        mModelPtrs = new long[mThreadNumber];
        for (int i = 0; i < mThreadNumber; ++i) {
            long tModelPtr = load1(tModelBytes, tModelBytes.length);
            if (tModelPtr==0 || tModelPtr==-1) throw new TorchException("Failed to load Torch Model");
            mModelPtrs[i] = tModelPtr;
        }
    }
    public NNAP(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException, IOException {
        this(aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? UT.IO.yaml2map(aModelPath) : UT.IO.json2map(aModelPath), aThreadNumber);
    }
    public NNAP(Map<?, ?> aModelInfo) throws TorchException {this(aModelInfo, 1);}
    public NNAP(String aModelPath) throws TorchException, IOException {this(aModelPath, 1);}
    private static native long load0(String aModelPath) throws TorchException;
    private static native long load1(byte[] aModelBytes, int aSize) throws TorchException;
    
    @Override public void shutdown() {
        if (!mDead) {
            mDead = true;
            for (int i = 0; i < mThreadNumber; ++i) shutdown0(mModelPtrs[i]);
        }
    }
    private static native void shutdown0(long aModelPtr);
    public boolean isShutdown() {return mDead;}
    public int threadNumber() {return mThreadNumber;}
    @VisibleForTesting public int nthreads() {return threadNumber();}
    
    
    protected IAtomData reorderSymbols(IAtomData aAtomData) {
        if (mElems.size() != aAtomData.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of AtomData: " + aAtomData.atomTypeNumber() + ", model: " + mElems.size());
        List<String> tAtomDataSymbols = aAtomData.symbols();
        if (tAtomDataSymbols==null || tAtomDataSymbols.equals(mElems)) return aAtomData;
        int[] tAtomDataType2newType = new int[tAtomDataSymbols.size()+1];
        for (int i = 0; i < tAtomDataSymbols.size(); ++i) {
            String tElem = tAtomDataSymbols.get(i);
            int idx = mElems.indexOf(tElem);
            if (idx < 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in AtomData");
            tAtomDataType2newType[i+1] = idx+1;
        }
        return aAtomData.opt().mapType(atom -> tAtomDataType2newType[atom.type()]);
    }
    
    /**
     * 使用 nnap 计算给定原子结构每个原子的能量值
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 每个原子能量组成的向量
     * @author liqa
     */
    public Vector calEnergies(final MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mElems.size() != aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + mElems.size());
        // 统一存储常量
        final int tAtomNumber = aMPC.atomNumber();
        Vector rEngs = VectorCache.getVec(tAtomNumber);
        // 获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = aMPC.getNLWhichNeedBuffer_(mBasis.rcut(), -1, false);
        try {
            aMPC.pool_().parforWithException(tAtomNumber, null, null, (i, threadID) -> {
                RowMatrix tBasis = mBasis.eval(aMPC.atomTypeNumber(), dxyzTypeDo -> {
                    aMPC.nl_().forEachNeighbor(i, mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, aMPC.atomType_().get(idx));
                        // 还是需要顺便统计近邻进行缓存
                        if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
                    });
                });
                tBasis.asVecRow().div2this(mNormVec);
                double tPred = forward(threadID, tBasis.internalData(), tBasis.internalDataShift(), tBasis.internalDataSize());
                MatrixCache.returnMat(tBasis);
                tPred += mRefEngs.get(aMPC.atomType_().get(i)-1);
                rEngs.set(i, tPred);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergies(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergies(tMPC);}
    }
    /**
     * 使用 nnap 计算给定原子结构指定原子的能量
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @return 此原子的能量组成的向量
     * @author liqa
     */
    public Vector calEnergiesAt(final MonatomicParameterCalculator aMPC, ISlice aIndices) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mElems.size() != aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + mElems.size());
        // 统一存储常量
        final int tSize = aIndices.size();
        Vector rEngs = VectorCache.getVec(tSize);
        try {
            aMPC.pool_().parforWithException(tSize, null, null, (i, threadID) -> {
                final int cIdx = aIndices.get(i);
                RowMatrix tBasis = mBasis.eval(aMPC.atomTypeNumber(), dxyzTypeDo -> {
                    aMPC.nl_().forEachNeighbor(cIdx, mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, aMPC.atomType_().get(idx));
                    });
                });
                tBasis.asVecRow().div2this(mNormVec);
                double tPred = forward(threadID, tBasis.internalData(), tBasis.internalDataShift(), tBasis.internalDataSize());
                MatrixCache.returnMat(tBasis);
                tPred += mRefEngs.get(aMPC.atomType_().get(cIdx)-1);
                rEngs.set(i, tPred);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergiesAt(IAtomData aAtomData, ISlice aIndices) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergiesAt(tMPC, aIndices);}
    }
    /**
     * 使用 nnap 计算给定原子结构的总能量
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 总能量
     * @author liqa
     */
    public double calEnergy(MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector tEngs = calEnergies(aMPC);
        double tTotEng = tEngs.sum();
        VectorCache.returnVec(tEngs);
        return tTotEng;
    }
    public double calEnergy(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergy(tMPC);}
    }
    
    /**
     * 计算移动前后的能量差，只计算移动影响的近邻列表中的原子，可以加快计算速度
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aRestoreMPC 计算完成后是否还原 MPC 的状态，默认为 {@code true}
     * @return 移动后能量 - 移动前能量
     */
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mElems.size() != aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + mElems.size());
        XYZ oXYZ = new XYZ(aMPC.atomDataXYZ_().row(aI));
        IIntVector oNL = aMPC.getNeighborList(oXYZ, mBasis.rcut());
        XYZ nXYZ = oXYZ.plus(aDx, aDy, aDz);
        IIntVector nNL = aMPC.getNeighborList(nXYZ, mBasis.rcut());
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(oNL.size());
        oNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        nNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aMPC, tNL);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aMPC.setAtomXYZ(aI, nXYZ), tNL);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreMPC) aMPC.setAtomXYZ(aI, oXYZ);
        return nEng - oEng;
    }
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDx, aDy, aDz, true);}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, double aDx, double aDy, double aDz) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffMove(tMPC, aI, aDx, aDy, aDz, false);}
    }
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    /**
     * 计算交换种类前后的能量差，只计算交换影响的近邻列表中的原子，可以加快计算速度
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aRestoreMPC 计算完成后是否还原 MPC 的状态，默认为 {@code true}
     * @return 交换后能量 - 交换前能量
     */
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ, boolean aRestoreMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mElems.size() != aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + mElems.size());
        int oTypeI = aMPC.atomType_().get(aI);
        int oTypeJ = aMPC.atomType_().get(aJ);
        if (oTypeI == oTypeJ) return 0.0;
        IIntVector iNL = aMPC.getNeighborList(aI, mBasis.rcut());
        IIntVector jNL = aMPC.getNeighborList(aJ, mBasis.rcut());
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(iNL.size()+1);
        tNL.add(aI);
        iNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        if (!tNL.contains(aJ)) tNL.add(aJ);
        jNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aMPC, tNL);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aMPC.setAtomType(aI, oTypeJ).setAtomType(aJ, oTypeI), tNL);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreMPC) aMPC.setAtomType(aI, oTypeI).setAtomType(aJ, oTypeJ);
        return nEng - oEng;
    }
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ) throws TorchException {return calEnergyDiffSwap(aMPC, aI, aJ, true);}
    public double calEnergyDiffSwap(IAtomData aAtomData, int aI, int aJ) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffSwap(tMPC, aI, aJ, false);}
    }
    
    
    /**
     * 使用 nnap 计算给定原子结构每个原子和受力
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 每个原子力组成的矩阵，按行排列
     * @author liqa
     */
    public RowMatrix calForces(MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        RowMatrix rForces = MatrixCache.getMatRow(aMPC.atomNumber(), 3);
        calEnergyForces_(aMPC, null, rForces.col(0), rForces.col(1), rForces.col(2));
        return rForces;
    }
    public RowMatrix calForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calForces(tMPC);}
    }
    
    /**
     * 使用 nnap 同时计算给定原子结构每个原子的能量和受力
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 按照 {@code [eng, fx, fy, fz]} 的顺序返回结果
     * @author liqa
     */
    public List<Vector> calEnergyForces(MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector rEngs = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesX = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesY = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesZ = VectorCache.getVec(aMPC.atomNumber());
        calEnergyForces_(aMPC, rEngs, rForcesX, rForcesY, rForcesZ);
        return Lists.newArrayList(rEngs, rForcesX, rForcesY, rForcesZ);
    }
    public List<Vector> calEnergyForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyForces(tMPC);}
    }
    protected void calEnergyForces_(final MonatomicParameterCalculator aMPC, final @Nullable IVector rEnergies, @NotNull IVector rForcesX, @NotNull IVector rForcesY, @NotNull IVector rForcesZ) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mElems.size() != aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + mElems.size());
        // 统一存储常量
        final int tAtomNumber = aMPC.atomNumber();
        final int tThreadNumber = aMPC.threadNumber();
        // 需要手动管理一下临时的近邻列表
        final IntList[] tNLPar = new IntList[tThreadNumber];
        for (int i = 0; i < tThreadNumber; i++) tNLPar[i] = new IntList();
        // 力需要累加统计，所以统一设置为 0
        rForcesX.fill(0.0);
        rForcesY.fill(0.0);
        rForcesZ.fill(0.0);
        // 并行情况下存在并行写入的问题，因此需要这样操作
        IVector[] rForcesXPar = new IVector[tThreadNumber]; rForcesXPar[0] = rForcesX; for (int i = 1; i < tThreadNumber; ++i) {rForcesXPar[i] = VectorCache.getZeros(tAtomNumber);}
        IVector[] rForcesYPar = new IVector[tThreadNumber]; rForcesYPar[0] = rForcesY; for (int i = 1; i < tThreadNumber; ++i) {rForcesYPar[i] = VectorCache.getZeros(tAtomNumber);}
        IVector[] rForcesZPar = new IVector[tThreadNumber]; rForcesZPar[0] = rForcesZ; for (int i = 1; i < tThreadNumber; ++i) {rForcesZPar[i] = VectorCache.getZeros(tAtomNumber);}
        // 获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = aMPC.getNLWhichNeedBuffer_(mBasis.rcut(), -1, false);
        try {
            aMPC.pool_().parforWithException(tAtomNumber, null, null, (i, threadID) -> {
                final IntList tNL = tNLPar[threadID]; tNL.clear();
                final IVector tForcesX = rForcesXPar[threadID];
                final IVector tForcesY = rForcesYPar[threadID];
                final IVector tForcesZ = rForcesZPar[threadID];
                List<@NotNull RowMatrix> tOut = mBasis.evalPartial(aMPC.atomTypeNumber(), true, true, dxyzTypeDo -> {
                    aMPC.nl_().forEachNeighbor(i, mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, aMPC.atomType_().get(idx));
                        // 手动统计近邻列表，这样可以保证顺序一致
                        tNL.add(idx);
                        // 还是需要顺便统计近邻进行缓存
                        if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
                    });
                });
                RowMatrix tBasis = tOut.get(0); tBasis.asVecRow().div2this(mNormVec);
                
                Vector tPredPartial = VectorCache.getVec(tBasis.rowNumber()*tBasis.columnNumber());
                double tPred = backward(threadID, tBasis.internalData(), tBasis.internalDataShift(), tPredPartial.internalData(), tPredPartial.internalDataShift(), tBasis.internalDataSize());
                tPredPartial.div2this(mNormVec);
                if (rEnergies != null) {
                    tPred += mRefEngs.get(aMPC.atomType_().get(i)-1);
                    rEnergies.set(i, tPred);
                }
                tForcesX.add(i, -tPredPartial.opt().dot(tOut.get(1).asVecRow()));
                tForcesY.add(i, -tPredPartial.opt().dot(tOut.get(2).asVecRow()));
                tForcesZ.add(i, -tPredPartial.opt().dot(tOut.get(3).asVecRow()));
                // 累加交叉项到近邻
                final int tNN = tNL.size();
                for (int j = 0; j < tNN; ++j) {
                    int tIdx = tNL.get(j);
                    tForcesX.add(tIdx, -tPredPartial.opt().dot(tOut.get(4+j).asVecRow()));
                    tForcesY.add(tIdx, -tPredPartial.opt().dot(tOut.get(4+tNN+j).asVecRow()));
                    tForcesZ.add(tIdx, -tPredPartial.opt().dot(tOut.get(4+tNN+tNN+j).asVecRow()));
                }
                
                VectorCache.returnVec(tPredPartial);
                MatrixCache.returnMat(tOut);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 累加其余线程的数据然后归还临时变量
        for (int i = 1; i < tThreadNumber; ++i) {rForcesZ.plus2this(rForcesZPar[i]); VectorCache.returnVec(rForcesZPar[i]);}
        for (int i = 1; i < tThreadNumber; ++i) {rForcesY.plus2this(rForcesYPar[i]); VectorCache.returnVec(rForcesYPar[i]);}
        for (int i = 1; i < tThreadNumber; ++i) {rForcesX.plus2this(rForcesXPar[i]); VectorCache.returnVec(rForcesXPar[i]);}
    }
    
    
    protected double forward(int aThreadID, double[] aX, int aStart, int aCount) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        rangeCheck(aX.length, aStart+aCount);
        return forward0(mModelPtrs[aThreadID], aX, aStart, aCount);
    }
    protected double forward(double[] aX, int aStart, int aCount) throws TorchException {return forward(0, aX, aStart, aCount);}
    protected double forward(double[] aX, int aCount) throws TorchException {return forward(aX, 0, aCount);}
    protected double forward(double[] aX) throws TorchException {return forward(aX, aX.length);}
    protected double forward(int aThreadID, DoubleCPointer aX, int aCount) throws TorchException {return forward1(mModelPtrs[aThreadID], aX.ptr_(), aCount);}
    protected double forward(DoubleCPointer aX, int aCount) throws TorchException {return forward(0, aX, aCount);}
    private static native double forward0(long aModelPtr, double[] aX, int aStart, int aCount) throws TorchException;
    private static native double forward1(long aModelPtr, long aXPtr, int aCount) throws TorchException;
    
    protected double backward(int aThreadID, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        rangeCheck(aX.length, aStart+aCount);
        rangeCheck(rGradX.length, rStart+aCount);
        return backward0(mModelPtrs[aThreadID], aX, aStart, rGradX, rStart, aCount);
    }
    protected double backward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {return backward(0, aX, aStart, rGradX, rStart, aCount);}
    protected double backward(double[] aX, double[] rGradX, int aCount) throws TorchException {return backward(aX, 0, rGradX, 0, aCount);}
    protected double backward(double[] aX, double[] rGradX) throws TorchException {return backward(aX, rGradX, aX.length);}
    protected double backward(int aThreadID, DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward1(mModelPtrs[aThreadID], aX.ptr_(), rGradX.ptr_(), aCount);}
    protected double backward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward(0, aX, rGradX, aCount);}
    private static native double backward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException;
    private static native double backward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount) throws TorchException;
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
