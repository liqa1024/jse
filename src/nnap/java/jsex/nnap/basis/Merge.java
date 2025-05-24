package jsex.nnap.basis;

import jse.code.collection.DoubleList;
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
public class Merge implements IBasis {
    
    private final IBasis[] mMergeBasis;
    private final double mRCut;
    private final int mSize, mTypeNum;
    private final String @Nullable[] mSymbols;
    private final ShiftVector[] mFpShell, mFpPxShell, mFpPyShell, mFpPzShell;
    
    public Merge(IBasis... aMergeBasis) {
        if (aMergeBasis==null || aMergeBasis.length==0) throw new IllegalArgumentException("Merge basis can not be null or empty");
        double tRCut = Double.NEGATIVE_INFINITY;
        int tSize = 0;
        int tTypeNum = -1;
        @Nullable List<String> tSymbols = null;
        Boolean tHasSymbols = null;
        for (IBasis tBasis : aMergeBasis) {
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
        mFpPxShell = new ShiftVector[mMergeBasis.length];
        mFpPyShell = new ShiftVector[mMergeBasis.length];
        mFpPzShell = new ShiftVector[mMergeBasis.length];
        int tShift = 0;
        for (int i = 0; i < mMergeBasis.length; ++i) {
            int tSizeFp = mSize - tShift;
            mFpShell[i] = new ShiftVector(tSizeFp, tShift, null);
            mFpPxShell[i] = new ShiftVector(tSizeFp, tShift, null);
            mFpPyShell[i] = new ShiftVector(tSizeFp, tShift, null);
            mFpPzShell[i] = new ShiftVector(tSizeFp, tShift, null);
            tShift += mMergeBasis[i].size();
        }
    }
    @Override public double rcut() {return mRCut;}
    @Override public int size() {return mSize;}
    @Override public int atomTypeNumber() {return mTypeNum;}
    @Override public boolean hasSymbol() {return mSymbols != null;}
    @Override public String symbol(int aType) {return mSymbols==null ? null : mSymbols[aType-1];}
    
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
        IBasis[] tMergeBasis = new IBasis[tList.size()];
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
        IBasis[] tMergeBasis = new IBasis[tList.size()];
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
    
    @Override public void eval(IDxyzTypeIterable aNL, DoubleArrayVector rFp) {
        int tSizeFp = rFp.size();
        if (mSize > tSizeFp) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFp);
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            tFp.setInternalData(rFp.internalData());
            mMergeBasis[i].eval(aNL, tFp);
        }
    }
    @Override public void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz) {
        int tSizeFp = rFp.size();
        int tSizeFpPx = rFpPx.size();
        int tSizeFpPy = rFpPy.size();
        int tSizeFpPz = rFpPz.size();
        if (mSize > tSizeFp) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFp);
        if (mSize > tSizeFpPx) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPx);
        if (mSize > tSizeFpPy) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPy);
        if (mSize > tSizeFpPz) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPz);
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            ShiftVector tFpPx = mFpPxShell[i];
            ShiftVector tFpPy = mFpPyShell[i];
            ShiftVector tFpPz = mFpPzShell[i];
            tFp.setInternalData(rFp.internalData());
            tFpPx.setInternalData(rFpPx.internalData());
            tFpPy.setInternalData(rFpPy.internalData());
            tFpPz.setInternalData(rFpPz.internalData());
            mMergeBasis[i].evalPartial(aNL, tFp, tFpPx, tFpPy, tFpPz);
        }
    }
    @Override public void evalPartial(IDxyzTypeIterable aNL, DoubleArrayVector rFp, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz, DoubleList rFpPxCross, DoubleList rFpPyCross, DoubleList rFpPzCross) {
        int tSizeFp = rFp.size();
        int tSizeFpPx = rFpPx.size();
        int tSizeFpPy = rFpPy.size();
        int tSizeFpPz = rFpPz.size();
        if (mSize > tSizeFp) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFp);
        if (mSize > tSizeFpPx) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPx);
        if (mSize > tSizeFpPy) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPy);
        if (mSize > tSizeFpPz) throw new IndexOutOfBoundsException(mSize+" > "+tSizeFpPz);
        for (int i = 0; i < mMergeBasis.length; ++i) {
            ShiftVector tFp = mFpShell[i];
            ShiftVector tFpPx = mFpPxShell[i];
            ShiftVector tFpPy = mFpPyShell[i];
            ShiftVector tFpPz = mFpPzShell[i];
            tFp.setInternalData(rFp.internalData());
            tFpPx.setInternalData(rFpPx.internalData());
            tFpPy.setInternalData(rFpPy.internalData());
            tFpPz.setInternalData(rFpPz.internalData());
            mMergeBasis[i].evalPartial(aNL, tFp, tFpPx, tFpPy, tFpPz, rFpPxCross, rFpPyCross, rFpPzCross);
        }
    }
}
