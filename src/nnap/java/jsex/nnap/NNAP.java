package jsex.nnap;

import jse.atom.IAtomData;
import jse.atom.MonatomicParameterCalculator;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.clib.DoubleCPointer;
import jse.clib.TorchException;
import jse.code.UT;
import jse.code.collection.IntList;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import jse.parallel.IAutoShutdown;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

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
    
    private final long mModelPtr;
    private boolean mDead = false;
    
    private final @Unmodifiable List<String> mElems;
    private final IVector mRefEngs;
    private final IVector mNormVec;
    private final Basis.IBasis mBasis;
    
    @SuppressWarnings("unchecked")
    public NNAP(Map<?, ?> aModelInfo) throws TorchException {
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
        mModelPtr = load1(tModelBytes, tModelBytes.length);
        if (mModelPtr==0 || mModelPtr==-1) throw new TorchException("Failed to load Torch Model");
    }
    public NNAP(String aModelPath) throws TorchException, IOException {
        this(aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? UT.IO.yaml2map(aModelPath) : UT.IO.json2map(aModelPath));
    }
    private static native long load0(String aModelPath) throws TorchException;
    private static native long load1(byte[] aModelBytes, int aSize) throws TorchException;
    
    @Override public void shutdown() {
        if (!mDead) {
            mDead = true;
            shutdown0(mModelPtr);
        }
    }
    private static native void shutdown0(long aModelPtr);
    
    
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
    public IVector calEnergies(final MonatomicParameterCalculator aMPC) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mElems.size() != aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + mElems.size());
        IVector rEngs = VectorCache.getVec(aMPC.atomNumber());
        // 获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = aMPC.getNLWhichNeedBuffer_(mBasis.rcut(), -1, false);
        aMPC.pool_().parfor(rEngs.size(), i -> {
            RowMatrix tBasis = mBasis.eval(aMPC.atomTypeNumber(), dxyzTypeDo -> {
                aMPC.nl_().forEachNeighbor(i, mBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                    dxyzTypeDo.run(dx, dy, dz, aMPC.atomType_().get(idx));
                    // 还是需要顺便统计近邻进行缓存
                    if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
                });
            });
            tBasis.asVecRow().div2this(mNormVec);
            double tPred;
            try {tPred = forward(tBasis.internalData(), tBasis.internalDataShift(), tBasis.internalDataSize());}
            catch (TorchException e) {throw new RuntimeException(e);}
            MatrixCache.returnMat(tBasis);
            tPred += mRefEngs.get(aMPC.atomType_().get(i)-1);
            rEngs.set(i, tPred);
        });
        return rEngs;
    }
    public IVector calEnergies(IAtomData aAtomData) throws TorchException {
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData)) {return calEnergies(tMPC);}
    }
    /**
     * 使用 nnap 计算给定原子结构的总能量
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @return 总能量
     * @author liqa
     */
    public double calEnergy(MonatomicParameterCalculator aMPC) throws TorchException {
        IVector tEngs = calEnergies(aMPC);
        double tTotEng = tEngs.sum();
        VectorCache.returnVec(tEngs);
        return tTotEng;
    }
    public double calEnergy(IAtomData aAtomData) throws TorchException {
        aAtomData = reorderSymbols(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData)) {return calEnergy(tMPC);}
    }
    
    
    protected double forward(double[] aX, int aStart, int aCount) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        rangeCheck(aX.length, aStart+aCount);
        return forward0(mModelPtr, aX, aStart, aCount);
    }
    protected double forward(double[] aX, int aCount) throws TorchException {return forward(aX, 0, aCount);}
    protected double forward(double[] aX) throws TorchException {return forward(aX, aX.length);}
    protected double forward(DoubleCPointer aX, int aCount) throws TorchException {return forward1(mModelPtr, aX.ptr_(), aCount);}
    private static native double forward0(long aModelPtr, double[] aX, int aStart, int aCount) throws TorchException;
    private static native double forward1(long aModelPtr, long aXPtr, int aCount) throws TorchException;
    
    protected double backward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        rangeCheck(aX.length, aStart+aCount);
        rangeCheck(rGradX.length, rStart+aCount);
        return backward0(mModelPtr, aX, aStart, rGradX, rStart, aCount);
    }
    protected double backward(double[] aX, double[] rGradX, int aCount) throws TorchException {return backward(aX, 0, rGradX, 0, aCount);}
    protected double backward(double[] aX, double[] rGradX) throws TorchException {return backward(aX, rGradX, aX.length);}
    protected double backward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward1(mModelPtr, aX.ptr_(), rGradX.ptr_(), aCount);}
    private static native double backward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException;
    private static native double backward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount) throws TorchException;
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
