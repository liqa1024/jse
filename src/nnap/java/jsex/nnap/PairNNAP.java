package jsex.nnap;

import jse.cache.*;
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
 * pair_style   jse lmpex.nnap.PairNNAP
 * pair_coeff   path/to/nnpot.json Cu Zr
 * } </pre>
 * 来使用
 */
public class PairNNAP extends LmpPlugin.Pair {
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
        double evdwl = 0.0;
        boolean evflag = evflag();
        boolean eflag = eflagEither();
        
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
        IntVector typeVec = IntVectorCache.getVec(nlocal+nghost);
        x.parse2dest(xMat.internalData(), xMat.internalDataShift(), xMat.rowNumber(), xMat.columnNumber());
        f.parse2dest(fMat.internalData(), fMat.internalDataShift(), fMat.rowNumber(), fMat.columnNumber());
        type.parse2dest(typeVec.internalData(), typeVec.internalDataShift(), typeVec.internalDataSize());
        
        // loop over neighbors of atoms
        for (int ii = 0; ii < inum; ++ii) {
            int i = ilist.getAt(ii);
            final double xtmp = xMat.get(i, 0);
            final double ytmp = xMat.get(i, 1);
            final double ztmp = xMat.get(i, 2);
            final int typetmp = mLmpType2NNAPType[typeVec.getAt(i)];
            IntCPointer jlist = firstneigh.getAt(i);
            final int jnum = numneigh.getAt(i);
            final IntVector jlistVec = IntVectorCache.getVec(jnum);
            LogicalVector jlistMask = LogicalVectorCache.getZeros(jnum);
            jlist.parse2dest(jlistVec.internalData(), jlistVec.internalDataShift(), jlistVec.internalDataSize());
            
            // 这里需要计算力，先计算基组偏导
            final List<@NotNull RowMatrix> tOut = mNNAP.mBasis.evalPartial(true, true, dxyzTypeDo -> {
                // 在这里遍历近邻列表
                for (int jj = 0; jj < jnum; ++jj) {
                    int j = jlistVec.get(jj);
                    j &= LmpPlugin.NEIGHMASK;
                    double delx = xtmp - xMat.get(j, 0);
                    double dely = ytmp - xMat.get(j, 1);
                    double delz = ztmp - xMat.get(j, 2);
                    double rsq = delx*delx + dely*dely + delz*delz;
                    if (rsq < mCutsq) {
                        jlistMask.set(jj, true);
                        dxyzTypeDo.run(delx, dely, delz, mLmpType2NNAPType[typeVec.get(j)]);
                    }
                }
            });
            // 反向传播
            RowMatrix tBasis = tOut.get(0); tBasis.asVecRow().div2this(mNNAP.mNormVec);
            final Vector tPredPartial = VectorCache.getVec(tBasis.rowNumber()*tBasis.columnNumber());
            double tPred = mNNAP.backward(tBasis.internalData(), tBasis.internalDataShift(), tPredPartial.internalData(), tPredPartial.internalDataShift(), tBasis.internalDataSize());
            tPredPartial.div2this(mNNAP.mNormVec);
            // 更新能量
            if (eflag) {
                evdwl = tPred + mNNAP.mRefEngs.get(typetmp-1);
            }
            // 更新自身的力
            fMat.update(i, 0, v -> v - tPredPartial.opt().dot(tOut.get(1).asVecRow()));
            fMat.update(i, 1, v -> v - tPredPartial.opt().dot(tOut.get(2).asVecRow()));
            fMat.update(i, 2, v -> v - tPredPartial.opt().dot(tOut.get(3).asVecRow()));
            // 然后再遍历一次，传播力和位力到近邻
            final int tNN = (tOut.size()-4)/3;
            for (int jj = 0; jj < jnum; ++jj) if (jlistMask.get(jj)) {
                int j = jlistVec.get(jj);
                j &= LmpPlugin.NEIGHMASK;
                
                final double fx = -tPredPartial.opt().dot(tOut.get(4+jj).asVecRow());
                final double fy = -tPredPartial.opt().dot(tOut.get(4+tNN+jj).asVecRow());
                final double fz = -tPredPartial.opt().dot(tOut.get(4+tNN+tNN+jj).asVecRow());
                fMat.update(j, 0, v -> v + fx);
                fMat.update(j, 1, v -> v + fy);
                fMat.update(j, 2, v -> v + fz);
                
                // ev stuffs
                if (evflag) {
                    double delx = xtmp - xMat.get(j, 0);
                    double dely = ytmp - xMat.get(j, 1);
                    double delz = ztmp - xMat.get(j, 2);
                    evTallyXYZFull(i, evdwl, 0.0d, fx+fx, fy+fy, fz+fz, delx, dely, delz);
                }
            }
            LogicalVectorCache.returnVec(jlistMask);
            IntVectorCache.returnVec(jlistVec);
        }
        
        f.fill(fMat.internalData(), fMat.nrows(), fMat.ncols());
        IntVectorCache.returnVec(typeVec);
        MatrixCache.returnMat(fMat);
        MatrixCache.returnMat(xMat);
        
        // ev stuffs
        if (vflagFdotr()) virialFdotrCompute();
    }
    
    
    @Override public void coeff(String... aArgs) throws Exception {
        mNNAP = new NNAP(aArgs[0]);
        int tArgLen = aArgs.length;
        mLmpType2NNAPType = new int[tArgLen];
        for (int type = 1; type < tArgLen; ++type) {
            String tElem = aArgs[type];
            int idx = mNNAP.mElems.indexOf(tElem);
            if (idx < 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in pair_coeff");
            mLmpType2NNAPType[type] = idx+1;
        }
        mCutoff = mNNAP.mBasis.rcut();
        mCutsq = mCutoff*mCutoff;
    }
    protected NNAP mNNAP = null;
    protected int[] mLmpType2NNAPType = null;
    protected double mCutoff = 0.0d;
    protected double mCutsq = Double.NaN;
    
    @Override public double initOne(int i, int j) {
        return mCutoff;
    }
}
