package jsex.nep;

import jse.cache.DoubleArrayCache;
import jse.cache.IntArrayCache;
import jse.cache.MatrixCache;
import jse.clib.DoubleCPointer;
import jse.clib.IntCPointer;
import jse.clib.NestedDoubleCPointer;
import jse.clib.NestedIntCPointer;
import jse.lmp.LmpPlugin;
import jse.math.matrix.RowMatrix;

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
        int[] typeBuf = IntArrayCache.getArray(nlocal+nghost);
        RowMatrix xMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        RowMatrix fMat = MatrixCache.getMatRow(nlocal+nghost, 3);
        type.parse2dest(typeBuf);
        x.parse2dest(xMat.internalData(), xMat.internalDataShift(), xMat.rowNumber(), xMat.columnNumber());
        f.parse2dest(fMat.internalData(), fMat.internalDataShift(), fMat.rowNumber(), fMat.columnNumber());
        
        double[] engBuf = {0.0};
        double[] virialBuf = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] eatomBuf = null;
        RowMatrix vatomMat = null;
        RowMatrix cvatomMat = null;
        if (eflagAtom) {
            eatomBuf = DoubleArrayCache.getArray(nlocal);
            eatom.parse2dest(eatomBuf);
        }
        if (vflagAtom) {
            vatomMat = MatrixCache.getMatRow(nlocal, 6);
            vatom.parse2dest(vatomMat.internalData(), vatomMat.internalDataShift(), vatomMat.rowNumber(), vatomMat.columnNumber());
        }
        if (cvflagAtom) {
            cvatomMat = MatrixCache.getMatRow(nlocal+nghost, 9);
            cvatom.parse2dest(cvatomMat.internalData(), cvatomMat.internalDataShift(), cvatomMat.rowNumber(), cvatomMat.columnNumber());
        }
        
        mNEP.compute_for_lammps(
            nlocal, inum, ilist, numneigh, firstneigh, typeBuf, mTypeMap,
            xMat, engBuf, virialBuf, eatomBuf, fMat, cvatomMat, vatomMat
        );
        
        if (eflag) {
            engVdwl.set(engVdwl.get() + engBuf[0]);
        }
        if (vflag) {
            for (int i = 0; i < 6; ++i) {
                virial.putAt(i, virial.getAt(i) + virialBuf[i]);
            }
        }
        if (eflagAtom) {
            eatom.fill(eatomBuf);
            DoubleArrayCache.returnArray(eatomBuf);
        }
        if (vflagAtom) {
            vatom.fill(vatomMat.internalData(), vatomMat.internalDataShift(), vatomMat.rowNumber(), vatomMat.columnNumber());
            MatrixCache.returnMat(vatomMat);
        }
        if (cvflagAtom) {
            cvatom.fill(cvatomMat.internalData(), cvatomMat.internalDataShift(), cvatomMat.rowNumber(), cvatomMat.columnNumber());
            MatrixCache.returnMat(cvatomMat);
        }
        f.fill(fMat.internalData(), fMat.internalDataShift(), fMat.rowNumber(), fMat.columnNumber());
        IntArrayCache.returnArray(typeBuf);
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
        mTypeMap[0] = -1;
        String[] tElements = new String[tArgLen];
        for (int type = 1; type < tArgLen; ++type) {
            tElements[type] = aArgs[2+type];
            mTypeMap[type] = type;
        }
        mNEP.init_from_file(aArgs[2]);
        mNEP.update_type_map(tTypeNum, mTypeMap, tElements);
        
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
