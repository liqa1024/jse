package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.ShiftVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    private final ShiftVector[] mFpShell, mFpGradShell;
    
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
        mFpGradShell = new ShiftVector[mMergeBasis.length];
        int tShiftFp = 0;
        for (int i = 0; i < mMergeBasis.length; ++i) {
            int tSizeFp = mMergeBasis[i].size();
            mFpShell[i] = new ShiftVector(tSizeFp, tShiftFp, null);
            mFpGradShell[i] = new ShiftVector(tSizeFp, tShiftFp, null);
            tShiftFp += tSizeFp;
        }
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
    protected void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, boolean aBufferNl) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        if (Conf.OPERATION_CHECK) {
            if (mSize != rFp.size()) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (mSize > rFp.size()) throw new IllegalArgumentException("data size mismatch");
        }
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            tFp.setInternalData(rFp.internalData());
            mMergeBasis[i].eval_(aNlDx, aNlDy, aNlDz, aNlType, tFp, aBufferNl);
        }
    }
    @Override
    protected void evalPartial_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            int tShiftFp = tFp.internalDataShift();
            mMergeBasis[i].evalPartialWithShift_(aNlDx, aNlDy, aNlDz, aNlType, tShiftFp, mSize-tShiftFp-tFp.internalDataSize(), rFpPx, rFpPy, rFpPz);
        }
    }
    @Override
    protected void evalPartialAndForceDot_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aFpGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 这里需要手动清空旧值
        mMergeBasis[0].clearForce_(rFx, rFy, rFz);
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFpGrad = mFpGradShell[i];
            tFpGrad.setInternalData(aFpGrad.internalData());
            mMergeBasis[i].evalPartialAndForceDotAccumulate_(aNlDx, aNlDy, aNlDz, aNlType, tFpGrad, rFx, rFy, rFz);
        }
    }
}
