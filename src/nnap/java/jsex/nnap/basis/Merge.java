package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IntArrayVector;
import jse.math.vector.ShiftIntVector;
import jse.math.vector.ShiftVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jse.code.CS.RANDOM;
import static jse.code.CS.ZL_STR;

/**
 * 使用多个基组的合并基组，用于实现自定义的高效基组
 * @author liqa
 */
public class Merge extends Basis {
    
    private final MergeableBasis[] mMergeBasis;
    private final double mRCut;
    private final int mSize, mTypeNum;
    private final String @Nullable[] mSymbols;
    private final ShiftVector[] mFpShell, mNNGradShell;
    private final ShiftIntVector[] mFpNlSizeShell, mFpGradNlIndexShell, mFpGradFpIndexShell;
    private final ShiftVector[] mFpPxShell, mFpPyShell, mFpPzShell;
    
    public Merge(MergeableBasis... aMergeBasis) {
        if (aMergeBasis==null || aMergeBasis.length==0) throw new IllegalArgumentException("Merge basis can not be null or empty");
        double tRCut = Double.NEGATIVE_INFINITY;
        int tSize = 0;
        int tTypeNum = -1;
        @Nullable List<String> tSymbols = null;
        Boolean tHasSymbols = null;
        for (Basis tBasis : aMergeBasis) {
            if (!(tBasis instanceof SphericalChebyshev) && !(tBasis instanceof Chebyshev)) {
                throw new IllegalArgumentException("MergeBasis should be SphericalChebyshev or Chebyshev");
            }
            tRCut = Math.max(tBasis.rcut(), tRCut);
            tSize += tBasis.size();
            if (tTypeNum < 0) {
                tTypeNum = tBasis.atomTypeNumber();
            } else {
                if (tTypeNum != tBasis.atomTypeNumber()) throw new IllegalArgumentException("atom type number mismatch");
            }
            if (tHasSymbols == null) {
                tHasSymbols = tBasis.hasSymbol();
                if (tHasSymbols) {
                    tSymbols = tBasis.symbols();
                    if (tSymbols == null) throw new NullPointerException();
                }
            } else {
                if (tHasSymbols != tBasis.hasSymbol()) throw new IllegalArgumentException("symbols mismatch");
                List<String> tSymbols_ = tBasis.symbols();
                if (tHasSymbols) {
                    if (!tSymbols.equals(tSymbols_)) throw new IllegalArgumentException("symbols mismatch");
                } else {
                    if (tSymbols_ != null) throw new IllegalArgumentException("symbols mismatch");
                }
            }
            tTypeNum = Math.min(tBasis.atomTypeNumber(), tTypeNum);
        }
        mMergeBasis = aMergeBasis;
        mRCut = tRCut;
        mSize = tSize;
        mTypeNum = tTypeNum;
        mSymbols = tSymbols==null ? null : tSymbols.toArray(ZL_STR);
        // init fp shell
        mFpShell = new ShiftVector[mMergeBasis.length];
        mNNGradShell = new ShiftVector[mMergeBasis.length];
        mFpNlSizeShell = new ShiftIntVector[mMergeBasis.length];
        mFpGradNlIndexShell = new ShiftIntVector[mMergeBasis.length];
        mFpGradFpIndexShell = new ShiftIntVector[mMergeBasis.length];
        mFpPxShell = new ShiftVector[mMergeBasis.length];
        mFpPyShell = new ShiftVector[mMergeBasis.length];
        mFpPzShell = new ShiftVector[mMergeBasis.length];
        int tShiftFp = 0;
        for (int i = 0; i < mMergeBasis.length; ++i) {
            int tSizeFp = mMergeBasis[i].size();
            mFpShell[i] = new ShiftVector(tSizeFp, tShiftFp, null);
            mNNGradShell[i] = new ShiftVector(tSizeFp, tShiftFp, null);
            mFpNlSizeShell[i] = new ShiftIntVector(tSizeFp, tShiftFp, null);
            mFpGradNlIndexShell[i]  = new ShiftIntVector(0, 0, null);
            mFpGradFpIndexShell[i]  = new ShiftIntVector(0, 0, null);
            mFpPxShell[i] = new ShiftVector(0, 0, null);
            mFpPyShell[i] = new ShiftVector(0, 0, null);
            mFpPzShell[i] = new ShiftVector(0, 0, null);
            tShiftFp += tSizeFp;
        }
    }
    @Override public Merge threadSafeRef() {
        MergeableBasis[] rBasis = new MergeableBasis[mMergeBasis.length];
        for (int i = 0; i < mMergeBasis.length; ++i) {
            rBasis[i] = mMergeBasis[i].threadSafeRef();
        }
        return new Merge(rBasis);
    }
    @Override public void initParameters() {
        for (Basis tBasis : mMergeBasis) tBasis.initParameters();
    }
    
    @Override public double rcut() {return mRCut;}
    @Override public int size() {return mSize;}
    @Override public int atomTypeNumber() {return mTypeNum;}
    @Override public boolean hasSymbol() {return mSymbols != null;}
    @Override public String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    
    @Override protected void shutdown_() {
        for (Basis tBasis : mMergeBasis) {
            tBasis.shutdown();
        }
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "merge");
        List<Map> tMergeBasis = NewCollections.from(mMergeBasis.length, i -> new LinkedHashMap<>());
        for (int i = 0; i < mMergeBasis.length; ++i) {
            mMergeBasis[i].save(tMergeBasis.get(i));
        }
        rSaveTo.put("basis", tMergeBasis);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Merge load(String @NotNull[] aSymbols, Map aMap) {
        Object tObj = aMap.get("basis");
        if (tObj == null) throw new IllegalArgumentException("Key `basis` required for merge load");
        List<Map> tList = (List<Map>)tObj;
        MergeableBasis[] tMergeBasis = new MergeableBasis[tList.size()];
        for (int i = 0; i < tMergeBasis.length; ++i) {
            Map tMap = tList.get(i);
            Object tType = tMap.get("type");
            if (tType == null) {
                tType = "spherical_chebyshev";
            }
            switch(tType.toString()) {
            case "spherical_chebyshev": {
                tMergeBasis[i] = SphericalChebyshev.load(aSymbols, tMap);
                break;
            }
            case "chebyshev": {
                tMergeBasis[i] = Chebyshev.load(aSymbols, tMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tType);
            }}
        }
        return new Merge(tMergeBasis);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Merge load(int aTypeNum, Map aMap) {
        Object tObj = aMap.get("basis");
        if (tObj == null) throw new IllegalArgumentException("Key `basis` required for merge load");
        List<Map> tList = (List<Map>)tObj;
        MergeableBasis[] tMergeBasis = new MergeableBasis[tList.size()];
        for (int i = 0; i < tMergeBasis.length; ++i) {
            Map tMap = tList.get(i);
            Object tType = tMap.get("type");
            if (tType == null) {
                tType = "spherical_chebyshev";
            }
            switch(tType.toString()) {
            case "spherical_chebyshev": {
                tMergeBasis[i] = SphericalChebyshev.load(aTypeNum, tMap);
                break;
            }
            case "chebyshev": {
                tMergeBasis[i] = Chebyshev.load(aTypeNum, tMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tType);
            }}
        }
        return new Merge(tMergeBasis);
    }
    
    @Override
    protected void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, @Nullable IntArrayVector rFpGradNlSize, boolean aBufferNl) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        if (Conf.OPERATION_CHECK) {
            if (mSize != rFp.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (mSize > rFp.size()) throw new IllegalArgumentException("data size mismatch");
        }
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            tFp.setInternalData(rFp.internalData());
            ShiftIntVector tFpNlSize = rFpGradNlSize ==null ? null : mFpNlSizeShell[i];
            if (tFpNlSize!=null) tFpNlSize.setInternalData(rFpGradNlSize.internalData());
            mMergeBasis[i].eval_(aNlDx, aNlDy, aNlDz, aNlType, tFp, tFpNlSize, aBufferNl);
        }
    }
    @Override
    protected void evalGrad_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, IntArrayVector aFpGradNlSize, IntArrayVector rFpGradNlIndex, IntArrayVector rFpGradFpIndex, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        int tNlShift = 0;
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftIntVector tFpNlSize = mFpNlSizeShell[i];
            tFpNlSize.setInternalData(aFpGradNlSize.internalData());
            int tSubSizeAll = tFpNlSize.sum();
            ShiftIntVector tFpGradNlIndex = mFpGradNlIndexShell[i];
            ShiftIntVector tFpGradFpIndex = mFpGradFpIndexShell[i];
            tFpGradNlIndex.setInternalData(rFpGradNlIndex.internalData()); tFpGradNlIndex.setInternalDataShift(tNlShift); tFpGradNlIndex.setInternalDataSize(tSubSizeAll);
            tFpGradFpIndex.setInternalData(rFpGradFpIndex.internalData()); tFpGradFpIndex.setInternalDataShift(tNlShift); tFpGradFpIndex.setInternalDataSize(tSubSizeAll);
            ShiftVector tFpPx = mFpPxShell[i];
            ShiftVector tFpPy = mFpPyShell[i];
            ShiftVector tFpPz = mFpPzShell[i];
            tFpPx.setInternalData(rFpPx.internalData()); tFpPx.setInternalDataShift(tNlShift); tFpPx.setInternalDataSize(tSubSizeAll);
            tFpPy.setInternalData(rFpPy.internalData()); tFpPy.setInternalDataShift(tNlShift); tFpPy.setInternalDataSize(tSubSizeAll);
            tFpPz.setInternalData(rFpPz.internalData()); tFpPz.setInternalDataShift(tNlShift); tFpPz.setInternalDataSize(tSubSizeAll);
            mMergeBasis[i].evalGrad_(aNlDx, aNlDy, aNlDz, aNlType, tFpNlSize, tFpGradNlIndex, tFpGradFpIndex, tFpPx, tFpPy, tFpPz);
            tFpGradFpIndex.plus2this(tFpNlSize.internalDataShift());
            tNlShift += tSubSizeAll;
        }
    }
    @Override
    protected void evalForce_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 这里需要手动清空旧值
        MergeableBasis.clearForce_(rFx, rFy, rFz);
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tNNGrad = mNNGradShell[i];
            tNNGrad.setInternalData(aNNGrad.internalData());
            mMergeBasis[i].evalForceAccumulate_(aNlDx, aNlDy, aNlDz, aNlType, tNNGrad, rFx, rFy, rFz);
        }
    }
    @Override @Deprecated
    protected void evalGrad_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            int tShiftFp = tFp.internalDataShift();
            mMergeBasis[i].evalGradWithShift_(aNlDx, aNlDy, aNlDz, aNlType, tShiftFp, mSize-tShiftFp-tFp.internalDataSize(), rFpPx, rFpPy, rFpPz);
        }
    }
}
