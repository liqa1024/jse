package jsex.nnap;

import jse.cache.*;
import jse.clib.DoubleCPointer;
import jse.clib.IntCPointer;
import jse.clib.NestedDoubleCPointer;
import jse.clib.NestedIntCPointer;
import jse.code.collection.IntList;
import jse.lmp.LmpPlugin;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;

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
    
    protected PairNNAP(long aPairPtr) {
        super(aPairPtr);
    }
    
    @Override public void settings(String... aArgs) throws Exception {
        super.settings(aArgs);
        // nnap 支持 centroidstress
        setCentroidstressflag(CENTROID_AVAIL);
        // 限制只能调用一次 pair_coeff
        setOneCoeff(true);
        // 此时需要禁用 VirialFdotrCompute
        noVirialFdotrCompute();
    }
    
    @Override public void initStyle() {
        // nnap 需要完整的近邻列表
        neighborRequestFull();
    }
    
    /**
     * 这里为了和 lammps 接口保持一致，并不完全按照 java 中的代码风格编写
     */
    @Override public void compute() throws Exception {
        boolean eflag = eflagEither();
        boolean vflag = vflagEither();
        boolean eflagAtom = eflagAtom();
        boolean vflagAtom = vflagAtom();
        boolean cvflagAtom = cvflagAtom();
        DoubleCPointer engVdwl = engVdwl();
        DoubleCPointer eatom = eatom();
        DoubleCPointer virial = virial();
        NestedDoubleCPointer vatom = vatom();
        NestedDoubleCPointer cvatom = cvatom();
        
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
        final RowMatrix xMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        final RowMatrix fMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        final IntVector typeVec = IntVectorCache.getVec(nlocal+nghost);
        x.parse2dest(xMat);
        f.parse2dest(fMat);
        type.parse2dest(typeVec);
        
        final double[] engBuf = {0.0};
        final double[] virialBuf = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        final Vector eatomVec = eflagAtom ? VectorCache.getVec(nlocal) : null;
        final RowMatrix vatomMat = vflagAtom ? MatrixCache.getMatRow(nlocal+nghost, 6) : null;
        final RowMatrix cvatomMat = cvflagAtom ? MatrixCache.getMatRow(nlocal+nghost, 9) : null;
        if (eflagAtom) {
            eatom.parse2dest(eatomVec);
        }
        if (vflagAtom) {
            vatom.parse2dest(vatomMat);
        }
        if (cvflagAtom) {
            cvatom.parse2dest(cvatomMat);
        }
        
        // 将原子序号改为按照种类排序，这样可以利用缓存机制
        for (int ti = 0; ti < mTypeNum; ++ti) {
            mTypeIlist[ti].clear();
        }
        for (int ii = 0; ii < inum; ++ii) {
            int i = ilist.getAt(ii);
            final int typei = typeVec.get(i);
            mTypeIlist[typei-1].add(i);
        }
        // loop over neighbors of atoms
        mNNAP.calEnergyForceVirial(nlocal, (initDo, finalDo, neighborListDo) -> {
            if (initDo != null) initDo.run(0);
            for (IntList tSubIlist : mTypeIlist) {
                final int tSubInum = tSubIlist.size();
                for (int ii = 0; ii < tSubInum; ++ii) {
                    int i = tSubIlist.get(ii);
                    final double xtmp = xMat.get(i, 0);
                    final double ytmp = xMat.get(i, 1);
                    final double ztmp = xMat.get(i, 2);
                    final int typei = typeVec.get(i);
                    IntCPointer jlist = firstneigh.getAt(i);
                    final int jnum = numneigh.getAt(i);
                    final IntVector jlistVec = IntVectorCache.getVec(jnum);
                    jlist.parse2dest(jlistVec);
                    // 遍历近邻
                    neighborListDo.run(0, i, mLmpType2NNAPType[typei], (aRMax, aDxyzTypeIdxDo) -> {
                        for (int jj = 0; jj < jnum; ++jj) {
                            int j = jlistVec.get(jj);
                            j &= LmpPlugin.NEIGHMASK;
                            // 注意 jse 中的 dxyz 和 lammps 定义的相反
                            double delx = xMat.get(j, 0) - xtmp;
                            double dely = xMat.get(j, 1) - ytmp;
                            double delz = xMat.get(j, 2) - ztmp;
                            double rsq = delx*delx + dely*dely + delz*delz;
                            if (rsq < mCutsq[typei]) {
                                aDxyzTypeIdxDo.run(delx, dely, delz, mLmpType2NNAPType[typeVec.get(j)], j);
                            }
                        }
                    });
                    IntVectorCache.returnVec(jlistVec);
                }
            }
            if (finalDo != null) finalDo.run(0);
        }, !eflag ? null : (threadID, cIdx, idx, eng) -> {
            if (idx >= 0) throw new IllegalStateException();
            engBuf[0] += eng;
            if (eflagAtom) {
                eatomVec.add(cIdx, eng);
            }
        }, (threadID, cIdx, idx, fx, fy, fz) -> {
            fMat.update(cIdx, 0, v -> v - fx);
            fMat.update(cIdx, 1, v -> v - fy);
            fMat.update(cIdx, 2, v -> v - fz);
            fMat.update(idx, 0, v -> v + fx);
            fMat.update(idx, 1, v -> v + fy);
            fMat.update(idx, 2, v -> v + fz);
        }, !vflag ? null : (threadID, cIdx, idx, fx, fy, fz, dx, dy, dz) -> {
            if (cIdx >= 0) throw new IllegalStateException();
            virialBuf[0] += dx*fx;
            virialBuf[1] += dy*fy;
            virialBuf[2] += dz*fz;
            virialBuf[3] += dx*fy;
            virialBuf[4] += dx*fz;
            virialBuf[5] += dy*fz;
            if (vflagAtom) {
                vatomMat.update(idx, 0, v -> v + dx*fx);
                vatomMat.update(idx, 1, v -> v + dy*fy);
                vatomMat.update(idx, 2, v -> v + dz*fz);
                vatomMat.update(idx, 3, v -> v + dx*fy);
                vatomMat.update(idx, 4, v -> v + dx*fz);
                vatomMat.update(idx, 5, v -> v + dy*fz);
            }
            if (cvflagAtom) {
                cvatomMat.update(idx, 0, v -> v + dx*fx);
                cvatomMat.update(idx, 1, v -> v + dy*fy);
                cvatomMat.update(idx, 2, v -> v + dz*fz);
                cvatomMat.update(idx, 3, v -> v + dx*fy);
                cvatomMat.update(idx, 4, v -> v + dx*fz);
                cvatomMat.update(idx, 5, v -> v + dy*fz);
                cvatomMat.update(idx, 6, v -> v + dy*fx);
                cvatomMat.update(idx, 7, v -> v + dz*fx);
                cvatomMat.update(idx, 8, v -> v + dz*fy);
            }
        });
        
        if (eflag) {
            engVdwl.set(engVdwl.get() + engBuf[0]);
        }
        if (vflag) {
            for (int i = 0; i < 6; ++i) {
                virial.putAt(i, virial.getAt(i) + virialBuf[i]);
            }
        }
        if (eflagAtom) {
            eatom.fill(eatomVec);
            VectorCache.returnVec(eatomVec);
        }
        if (vflagAtom) {
            vatom.fill(vatomMat);
            MatrixCache.returnMat(vatomMat);
        }
        if (cvflagAtom) {
            cvatom.fill(cvatomMat);
            MatrixCache.returnMat(cvatomMat);
        }
        f.fill(fMat);
        IntVectorCache.returnVec(typeVec);
        MatrixCache.returnMat(fMat);
        MatrixCache.returnMat(xMat);
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
        mTypeNum = atomNtypes();
        int tArgLen = aArgs.length-2;
        if (tArgLen-1 != mTypeNum) throw new IllegalArgumentException("Elements number in pair_coeff not match ntypes ("+mTypeNum+").");
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
        mTypeIlist = new IntList[mTypeNum];
        for (int ti = 0; ti < mTypeNum; ++ti) {
            mTypeIlist[ti] = new IntList(16);
        }
    }
    protected NNAP mNNAP = null;
    protected int[] mLmpType2NNAPType = null;
    protected double[] mCutoff = null;
    protected double[] mCutsq = null;
    protected int mTypeNum = -1;
    private IntList[] mTypeIlist = null;
    
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
