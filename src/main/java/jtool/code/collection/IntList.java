package jtool.code.collection;

import jtool.math.IDataShell;
import jtool.math.vector.IIntVector;
import jtool.math.vector.IntVector;
import jtool.math.vector.RefIntVector;
import jtool.math.vector.ShiftIntVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

import static jtool.code.CS.ZL_INT;
import static jtool.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code int[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class IntList implements ISlice, IDataShell<int[]> {
    protected int[] mData;
    protected int mSize = 0;
    private IntList(int aSize, int[] aData) {mSize = aSize; mData = aData;}
    public IntList() {mData = ZL_INT;}
    public IntList(int aInitSize) {mData = new int[aInitSize];}
    
    public int get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, int aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public int last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntList");
        return mData[mSize-1];
    }
    public int first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntList");
        return mData[0];
    }
    
    public void add(int aValue) {
        if (mData.length == 0) {
            mData = new int[1];
        } else
        if (mData.length <= mSize) {
            int[] oData = mData;
            mData = new int[oData.length * 2];
            System.arraycopy(oData, 0, mData, 0, oData.length);
        }
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            int[] oData = mData;
            mData = new int[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(IntConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Integer> asList() {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return IntList.this.get(index);}
            @Override public Integer set(int index, Integer element) {int oValue = IntList.this.get(index); IntList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Integer element) {IntList.this.add(element); return true;}
        };
    }
    @ApiStatus.Experimental
    public @Unmodifiable List<Integer> asConstList() {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return IntList.this.get(index);}
            @Override public int size() {return mSize;}
        };
    }
    public IIntVector asVec() {
        return new RefIntVector() {
            @Override public int get(int aIdx) {return IntList.this.get(aIdx);}
            @Override public void set(int aIdx, int aValue) {IntList.this.set(aIdx, aValue);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public IIntVector asConstVec() {
        return new RefIntVector() {
            @Override public int get(int aIdx) {return IntList.this.get(aIdx);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public IntVector copy2vec() {
        IntVector rVector = IntVector.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    
    
    /** IDataShell stuffs */
    @Override  public int internalDataSize() {return size();}
    @Override public void setInternalData(int[] aData) {mData = aData;}
    @Override public IntList newShell() {return new IntList(mSize, null);}
    @Override public int[] internalData() {return mData;}
    @ApiStatus.Internal @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntList) return ((IntList)aObj).mData;
        if (aObj instanceof IntVector) return ((IntVector)aObj).internalData();
        if (aObj instanceof ShiftIntVector) return ((ShiftIntVector)aObj).internalData();
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
}
