package jsex.nnap;

import jse.atom.XYZ;
import jse.cache.*;
import jse.clib.DoubleCPointer;
import jse.clib.IntCPointer;
import jse.clib.NestedDoubleCPointer;
import jse.clib.NestedIntCPointer;
import jse.lmp.LmpPlugin;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IntVector;
import jse.math.vector.LogicalVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@link LmpPlugin.Pair} 的 NNAP 版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nnap.PairNNAP
 * pair_coeff   * * path/to/nnpot.json Cu Zr
 * } </pre>
 * 来使用
 */
public class PairNNAP extends LmpPlugin.Pair {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(_INIT_FLAG);
        }
    }
    private final static boolean _INIT_FLAG;
    static {
        InitHelper.INITIALIZED = true;
        // 确保 NNAP 已经确实初始化
        NNAP.InitHelper.init();
        _INIT_FLAG = false;
    }
    
    protected PairNNAP(long aPairPtr) {super(aPairPtr);}
    
    @Override public void initStyle() {
        // nnap 需要完整的近邻列表，此时需要禁用 VirialFdotrCompute
        neighborRequestFull();
        noVirialFdotrCompute();
    }
    
    /**
     * 这里为了和 lammps 接口保持一致，并不完全按照 java 中的代码风格编写
     * @author liqa
     */
    @Override public void compute() throws Exception {
        boolean evflag = evflag();
        boolean eflag = eflagEither();
        boolean eflagGlobal = eflagGlobal();
        boolean eflagAtom = eflagAtom();
        DoubleCPointer engVdwl = engVdwl();
        DoubleCPointer eatom = eatom();
        
        NestedDoubleCPointer x = atomX();
        NestedDoubleCPointer f = atomF();
        IntCPointer type = atomType();
        
        int inum = listInum();
        IntCPointer ilist = listIlist();
        IntCPointer numneigh = listNumneigh();
        NestedIntCPointer firstneigh = listFirstneigh();
        
        // 优化部分，将这些经常访问的数据全部缓存来加速遍历
        int nlocal = atomNlocal();
        int nghost = atomNghost();
        RowMatrix xMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        RowMatrix fMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        final IntVector typeVec = IntVectorCache.getVec(nlocal+nghost);
        x.parse2dest(xMat.internalData(), xMat.internalDataShift(), xMat.rowNumber(), xMat.columnNumber());
        f.parse2dest(fMat.internalData(), fMat.internalDataShift(), fMat.rowNumber(), fMat.columnNumber());
        type.parse2dest(typeVec.internalData(), typeVec.internalDataShift(), typeVec.internalDataSize());
        
        // loop over neighbors of atoms
        for (int ii = 0; ii < inum; ++ii) {
            int i = ilist.getAt(ii);
            final double xtmp = xMat.get(i, 0);
            final double ytmp = xMat.get(i, 1);
            final double ztmp = xMat.get(i, 2);
            final int typei = typeVec.get(i);
            IntCPointer jlist = firstneigh.getAt(i);
            final int jnum = numneigh.getAt(i);
            final IntVector jlistVec = IntVectorCache.getVec(jnum);
            final LogicalVector jlistMask = LogicalVectorCache.getZeros(jnum);
            jlist.parse2dest(jlistVec.internalData(), jlistVec.internalDataShift(), jlistVec.internalDataSize());
            
            final NNAP.SingleNNAP tNNAP = mNNAP.model(mLmpType2NNAPType[typei]);
            // 这里需要计算力，先计算基组偏导
            final List<@NotNull Vector> tOut = tNNAP.basis().evalPartial(true, true, dxyzTypeDo -> {
                // 在这里遍历近邻列表
                for (int jj = 0; jj < jnum; ++jj) {
                    int j = jlistVec.get(jj);
                    j &= LmpPlugin.NEIGHMASK;
                    // 注意 jse 中的 dxyz 和 lammps 定义的相反
                    double delx = xMat.get(j, 0) - xtmp;
                    double dely = xMat.get(j, 1) - ytmp;
                    double delz = xMat.get(j, 2) - ztmp;
                    double rsq = delx*delx + dely*dely + delz*delz;
                    if (rsq < mCutsq[typei]) {
                        jlistMask.set(jj, true);
                        dxyzTypeDo.run(delx, dely, delz, mLmpType2NNAPType[typeVec.get(j)]);
                    }
                }
            });
            // 反向传播，现在改为批处理方式，可以大大提高效率
            Vector tBasis = tOut.get(0); tNNAP.normBasis(tBasis);
            tNNAP.submitBatchBackward(tBasis, eflag ? pred -> {
                // 更新能量
                double eng = tNNAP.denormEng(pred) + tNNAP.refEng();
                // 由于不是瓶颈，并且不是频繁调用，因此这里不去专门优化
                if (eflagGlobal) engVdwl.set(engVdwl.get()+eng);
                if (eflagAtom) eatom.putAt(i, eatom.getAt(i)+eng);
            } : null, xGrad -> {
                tNNAP.normBasisPartial(xGrad);
                tNNAP.denormEngPartial(xGrad);
                final XYZ rBuf = new XYZ();
                // 更新自身的力
                NNAP.forceDot_(xGrad.internalData(), xGrad.internalDataShift(), tOut.get(1).internalData(), tOut.get(2).internalData(), tOut.get(3).internalData(), xGrad.internalDataSize(), rBuf);
                fMat.update(i, 0, v -> v - rBuf.mX);
                fMat.update(i, 1, v -> v - rBuf.mY);
                fMat.update(i, 2, v -> v - rBuf.mZ);
                // 然后再遍历一次，传播力和位力到近邻
                final int tNN = (tOut.size()-4)/3;
                int ji = 0;
                for (int jj = 0; jj < jnum; ++jj) if (jlistMask.get(jj)) {
                    int j = jlistVec.get(jj);
                    j &= LmpPlugin.NEIGHMASK;
                    
                    NNAP.forceDot_(xGrad.internalData(), xGrad.internalDataShift(), tOut.get(4+ji).internalData(), tOut.get(4+tNN+ji).internalData(), tOut.get(4+tNN+tNN+ji).internalData(), xGrad.internalDataSize(), rBuf);
                    final double fx = -rBuf.mX;
                    final double fy = -rBuf.mY;
                    final double fz = -rBuf.mZ;
                    fMat.update(j, 0, v -> v + fx);
                    fMat.update(j, 1, v -> v + fy);
                    fMat.update(j, 2, v -> v + fz);
                    ++ji;
                    
                    // ev stuffs
                    if (evflag) {
                        // 注意 jse 中的 dxyz 和 lammps 定义的相反
                        double delx = xMat.get(j, 0) - xtmp;
                        double dely = xMat.get(j, 1) - ytmp;
                        double delz = xMat.get(j, 2) - ztmp;
                        evTallyXYZFull(i, 0.0, 0.0, fx+fx, fy+fy, fz+fz, delx, dely, delz);
                    }
                }
                // 同样这个返回需要放在里面延迟归还
                VectorCache.returnVec(tOut);
                LogicalVectorCache.returnVec(jlistMask);
                IntVectorCache.returnVec(jlistVec);
            });
        }
        for (NNAP.SingleNNAP tNNAP : mNNAP.models()) {
            tNNAP.clearSubmittedBatchBackward();
        }
        
        f.fill(fMat.internalData(), fMat.internalDataShift(), fMat.rowNumber(), fMat.columnNumber());
        IntVectorCache.returnVec(typeVec);
        MatrixCache.returnMat(fMat);
        MatrixCache.returnMat(xMat);
        
        // ev stuffs
        if (vflagFdotr()) virialFdotrCompute();
    }
    
    
    @Override public void coeff(String... aArgs) throws Exception {
        if (aArgs==null || aArgs.length<4) throw new IllegalArgumentException("Not enough arguments, pair_coeff MUST be like `* * path/to/nnpot elem1 ...`");
        if (!aArgs[0].equals("*") || !aArgs[1].equals("*")) throw new IllegalArgumentException("pair_coeff MUST start with `* *`");
        mNNAP = new NNAP(aArgs[2]);
        String tNNAPUnits = mNNAP.units();
        if (tNNAPUnits != null) {
            String tLmpUnits = unitStyle();
            if (tLmpUnits!=null && !tLmpUnits.equals(tNNAPUnits)) throw new IllegalArgumentException("Invalid units ("+tLmpUnits+") for this model ("+tNNAPUnits+")");
        }
        int tArgLen = aArgs.length-2;
        mLmpType2NNAPType = new int[tArgLen];
        mCutoff = new double[tArgLen];
        mCutsq = new double[tArgLen];
        for (int type = 1; type < tArgLen; ++type) {
            String tElem = aArgs[2+type];
            int tNNAPType = mNNAP.typeOf(tElem);
            if (tNNAPType <= 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in pair_coeff");
            mLmpType2NNAPType[type] = tNNAPType;
            mCutoff[type] = mNNAP.model(tNNAPType).basis().rcut();
            mCutsq[type] = mCutoff[type]*mCutoff[type];
        }
    }
    protected NNAP mNNAP = null;
    protected int[] mLmpType2NNAPType = null;
    protected double[] mCutoff = null;
    protected double[] mCutsq = null;
    
    @Override public double initOne(int i, int j) {
        return mCutoff[i];
    }
    
    @Override public void shutdown() {
        if (mNNAP != null) {
            mNNAP.shutdown();
            mNNAP = null;
        }
    }
}
