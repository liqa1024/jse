package jtool.code.collection;

import jtool.math.IDataShell;
import jtool.math.vector.ILongVector;
import jtool.math.vector.LongVector;
import jtool.math.vector.RefLongVector;
import jtool.math.vector.ShiftLongVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import static jtool.code.CS.ZL_LONG;
import static jtool.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code long[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class LongList implements IDataShell<long[]> {
    protected long[] mData;
    protected int mSize = 0;
    private LongList(int aSize, long[] aData) {mSize = aSize; mData = aData;}
    public LongList() {mData = ZL_LONG;}
    public LongList(int aInitSize) {mData = new long[aInitSize];}
    
    public long get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, long aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public long last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LongList");
        return mData[mSize-1];
    }
    public long first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LongList");
        return mData[0];
    }
    
    public void add(long aValue) {
        if (mData.length == 0) {
            mData = new long[1];
        } else
        if (mData.length <= mSize) {
            long[] oData = mData;
            mData = new long[oData.length + Math.max(1, oData.length>>1)];
            System.arraycopy(oData, 0, mData, 0, oData.length);
        }
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            long[] oData = mData;
            mData = new long[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(LongConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Long> asList() {
        return new AbstractRandomAccessList<Long>() {
            @Override public Long get(int index) {return LongList.this.get(index);}
            @Override public Long set(int index, Long element) {long oValue = LongList.this.get(index); LongList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Long element) {LongList.this.add(element); return true;}
        };
    }
    @ApiStatus.Experimental
    public @Unmodifiable List<Long> asConstList() {
        return new AbstractRandomAccessList<Long>() {
            @Override public Long get(int index) {return LongList.this.get(index);}
            @Override public int size() {return mSize;}
        };
    }
    public ILongVector asVec() {
        return new RefLongVector() {
            @Override public long get(int aIdx) {return LongList.this.get(aIdx);}
            @Override public void set(int aIdx, long aValue) {LongList.this.set(aIdx, aValue);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public ILongVector asConstVec() {
        return new RefLongVector() {
            @Override public long get(int aIdx) {return LongList.this.get(aIdx);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public LongVector copy2vec() {
        LongVector rVector = LongVector.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
    @Override public void setInternalData(long[] aData) {mData = aData;}
    @Override public LongList newShell() {return new LongList(mSize, null);}
    @Override public long[] internalData() {return mData;}
    @ApiStatus.Internal @Override public long @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LongList) return ((LongList)aObj).mData;
        if (aObj instanceof LongVector) return ((LongVector)aObj).internalData();
        if (aObj instanceof ShiftLongVector) return ((ShiftLongVector)aObj).internalData();
        if (aObj instanceof long[]) return (long[])aObj;
        return null;
    }
}
