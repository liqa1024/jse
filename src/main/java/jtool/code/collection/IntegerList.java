package jtool.code.collection;

import jtool.code.functional.IIntegerConsumer1;
import jtool.math.IDataShell;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

import static jtool.code.CS.ZL_INT;

/**
 * 通用的使用 {@code int[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class IntegerList implements IDataShell<int[]> {
    protected int[] mData;
    protected int mSize = 0;
    private IntegerList(int aSize, int[] aData) {mSize = aSize; mData = aData;}
    public IntegerList() {mData = ZL_INT; mSize = 0;}
    public IntegerList(int aInitSize) {mData = new int[aInitSize];}
    
    public int get(int aIdx) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return mData[aIdx];
    }
    public void set(int aIdx, int aValue) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public int last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntegerList");
        return mData[mSize-1];
    }
    public int first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntegerList");
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
    public void forEach(IIntegerConsumer1 aCon) {
        for (int i = 0; i < mSize; ++i) aCon.run(mData[i]);
    }
    
    public List<Integer> asList() {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return IntegerList.this.get(index);}
            @Override public Integer set(int index, Integer element) {int oValue = IntegerList.this.get(index); IntegerList.this.set(index, element); return oValue;}
            @Override public int size() {return IntegerList.this.size();}
            @Override public boolean add(Integer element) {IntegerList.this.add(element); return true;}
        };
    }
    
    
    /** IDataShell stuffs */
    @Override  public int internalDataSize() {return size();}
    @Override public void setInternalData(int[] aData) {mData = aData;}
    @Override public IntegerList newShell() {return new IntegerList(mSize, null);}
    @Override public int[] internalData() {return mData;}
    @ApiStatus.Internal @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntegerList) return ((IntegerList)aObj).mData;
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
}
