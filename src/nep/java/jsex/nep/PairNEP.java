package jsex.nep;

import jse.cache.*;
import jse.cptr.DoubleCPointer;
import jse.cptr.IntCPointer;
import jse.cptr.NestedDoubleCPointer;
import jse.cptr.NestedIntCPointer;
import jse.lmp.LmpPlugin;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;

/**
 * {@link LmpPlugin.Pair} 的 NEP 版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nep.PairNEP
 * pair_coeff   * * path/to/neppot.txt Cu Zr
 * } </pre>
 * 来使用
 *
 * @see NEP
 * @author Junjie Wang，liqa
 */
public class PairNEP extends LmpPlugin.Pair {
    protected PairNEP(long aPairPtr) {super(aPairPtr);}
    
    @Override public void settings(String... aArgs) throws Exception {
        super.settings(aArgs);
        // nep 支持 centroidstress
        setCentroidstressflag(CENTROID_AVAIL);
        // 限制只能调用一次 pair_coeff
        setOneCoeff(true);
        // 此时需要禁用 VirialFdotrCompute
        noVirialFdotrCompute();
    }
    
    @Override public void initStyle() {
        if (!forceNewtonPair()) {
            throw new IllegalArgumentException("Pair style NEP requires newton pair on");
        }
        // nep 需要完整的近邻列表
        neighborRequestFull();
    }
    
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
        
        int nlocal = atomNlocal();
        int nghost = atomNghost();
        final IntVector typeVec = IntVectorCache.getVec(nlocal+nghost);
        final RowMatrix xMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        final RowMatrix fMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        type.parse2dest(typeVec);
        x.parse2dest(xMat);
        f.parse2dest(fMat);
        
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
        
        mNEP.calEnergyForceVirial(nlocal, (initDo, finalDo, neighborListDo) -> {
            if (initDo != null) initDo.run(0);
            for (int ii = 0; ii < inum; ++ii) {
                int i = ilist.getAt(ii);
                final double xtmp = xMat.get(i, 0);
                final double ytmp = xMat.get(i, 1);
                final double ztmp = xMat.get(i, 2);
                final int typei = mTypeMap[typeVec.get(i)];
                IntCPointer jlist = firstneigh.getAt(i);
                final int jnum = numneigh.getAt(i);
                final IntVector jlistVec = IntVectorCache.getVec(jnum);
                jlist.parse2dest(jlistVec);
                // 遍历近邻
                neighborListDo.run(0, i, typei, (rmax, dxyzTypeDo) -> {
                    for (int jj = 0; jj < jnum; ++jj) {
                        int j = jlistVec.get(jj);
                        j &= LmpPlugin.NEIGHMASK;
                        // 注意 jse 中的 dxyz 和 lammps 定义的相反
                        double delx = xMat.get(j, 0) - xtmp;
                        double dely = xMat.get(j, 1) - ytmp;
                        double delz = xMat.get(j, 2) - ztmp;
                        dxyzTypeDo.run(delx, dely, delz, mTypeMap[typeVec.get(j)], j);
                    }
                });
                IntVectorCache.returnVec(jlistVec);
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
        
        int tTypeNum = atomNtypes();
        int tArgLen = aArgs.length-2;
        if (tArgLen-1 != tTypeNum) throw new IllegalArgumentException("Elements number in pair_coeff not match ntypes ("+tTypeNum+").");
        mTypeMap = new int[tArgLen];
        mNEP.init_from_file(aArgs[2]);
        for (int type = 1; type < tArgLen; ++type) {
            String tElem = aArgs[2+type];
            int tNEPType = mNEP.typeOf(tElem);
            if (tNEPType <= 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in pair_coeff");
            mTypeMap[type] = tNEPType;
        }
        // 一些检测，要求 NEP 是全近邻列表，且需要在内部检测截断半径
        if (mNEP.neighborListHalf()) throw new IllegalStateException();
        if (!mNEP.neighborListChecked()) throw new IllegalStateException();
        
        // get cutoff from NEP model
        mCutoff = Math.max(mNEP.paramb.rc_radial, mNEP.paramb.rc_angular);
        mCutoffsq = mCutoff * mCutoff;
    }
    protected double mCutoff = Double.NaN;
    protected double mCutoffsq = Double.NaN;
    protected NEP mNEP = new NEP();
    protected int[] mTypeMap = null;
    
    @Override public double initOne(int i, int j) {
        return mCutoff;
    }
}
