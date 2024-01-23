package jtool.code.collection;

import jtool.math.vector.IIntVector;
import jtool.math.vector.IntVector;
import jtool.math.vector.RefIntVector;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

import static jtool.code.CS.ZL_INT;
import static jtool.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code int[]} 存储内部元素的 deque，
 * 支持两端增长和删除；
 * 并且支持随机访问
 */
public class IntDeque {
    protected int[] mData;
    protected int mStart = 0;
    protected int mEnd = 0;
    private IntDeque(int aStart, int aEnd, int[] aData) {mStart = aStart; mEnd = aEnd; mData = aData;}
    public IntDeque() {mData = ZL_INT;}
    public IntDeque(int aInitSize) {mData = new int[aInitSize];}
    
    public int get(int aIdx) {
        rangeCheck(aIdx, size());
        aIdx += mStart;
        if (mEnd<mStart && aIdx>=mData.length) aIdx -= mData.length;
        assert aIdx < mEnd;
        return mData[aIdx];
    }
    public void set(int aIdx, int aValue) {
        rangeCheck(aIdx, size());
        aIdx += mStart;
        if (mEnd<mStart && aIdx>=mData.length) aIdx -= mData.length;
        assert aIdx < mEnd;
        mData[aIdx] = aValue;
    }
    public int size() {
        int tSize = mEnd-mStart;
        if (tSize < 0) tSize += mData.length;
        return tSize;
    }
    /** 用于方便访问 */
    public boolean isEmpty() {return mEnd==mStart;}
    public int last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntDeque");
        return mData[mEnd-1];
    }
    public int first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntDeque");
        return mData[mStart];
    }
    
    private void growthIfNeed_() {
        if (mData.length == 0) {
            mData = new int[16+1];
        } else {
            boolean tCross;
            int tSize = mEnd-mStart;
            if (tSize < 0) {
                tSize += mData.length;
                tCross = true;
            } else {
                tCross = false;
            }
            if (mData.length == tSize+1) {
                int[] oData = mData;
                mData = new int[mData.length + tSize];
                if (tCross) {
                    int oStart = mStart;
                    mStart += tSize;
                    System.arraycopy(oData, 0, mData, 0, mEnd);
                    System.arraycopy(oData, oStart, mData, mStart, tSize-mEnd);
                } else {
                    System.arraycopy(oData, mStart, mData, mStart, tSize);
                }
            }
        }
    }
    
    public void addFirst(int aValue) {
        growthIfNeed_();
        --mStart;
        if (mStart < 0) mStart += mData.length;
        mData[mStart] = aValue;
    }
    public void addLast(int aValue) {
        growthIfNeed_();
        if (mEnd == mData.length) mEnd = 0;
        mData[mEnd] = aValue;
        ++mEnd;
    }
    
    public int removeFirst() {
        if (isEmpty()) throw new NoSuchElementException("Cannot removeFirst() from an empty IntDeque");
        int tValue = mData[mStart];
        ++mStart;
        if (mStart == mData.length) mStart = 0;
        return tValue;
    }
    public int removeLast() {
        if (isEmpty()) throw new NoSuchElementException("Cannot removeLast() from an empty IntDeque");
        --mEnd;
        if (mEnd < 0) mEnd += mData.length;
        return mData[mStart];
    }
    public int getFirst() {return first();}
    public int getLast() {return last();}
    
    
    // *** Queue methods ***
    public void add(int aValue) {addLast(aValue);}
    public int remove() {return removeFirst();}
    public int element() {return getFirst();}
    // *** Stack methods ***
    public void push(int aValue) {addFirst(aValue);}
    public int pop() {return removeFirst();}
    
    
    public void trimToSize() {
        boolean tCross;
        int tSize = mEnd-mStart;
        if (tSize < 0) {
            tSize += mData.length;
            tCross = true;
        } else {
            tCross = false;
        }
        if (mData.length != tSize) {
            int oStart = mStart;
            int oEnd = mEnd;
            int[] oData = mData;
            mStart = 0;
            mEnd = tSize;
            mData = new int[tSize];
            if (tCross) {
                System.arraycopy(oData, 0, mData, tSize-oEnd, oEnd);
                System.arraycopy(oData, oStart, mData, 0, tSize-oEnd);
            } else {
                System.arraycopy(oData, oStart, mData, 0, tSize);
            }
        }
    }
    public void clear() {
        mStart = 0;
        mEnd = 0;
    }
    public void forEach(IntConsumer aCon) {
        if (mEnd == mStart) return;
        if (mEnd > mStart) {
            for (int i = mStart; i < mEnd; ++i) aCon.accept(mData[i]);
        } else {
            for (int i = mStart; i < mData.length; ++i) aCon.accept(mData[i]);
            for (int i = 0; i < mEnd; ++i) aCon.accept(mData[i]);
        }
    }
    
    public List<Integer> asList() {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return IntDeque.this.get(index);}
            @Override public Integer set(int index, Integer element) {int oValue = IntDeque.this.get(index); IntDeque.this.set(index, element); return oValue;}
            @Override public int size() {return IntDeque.this.size();}
            @Override public boolean add(Integer element) {IntDeque.this.add(element); return true;}
        };
    }
    public IIntVector asVec() {
        return new RefIntVector() {
            @Override public int get(int aIdx) {return IntDeque.this.get(aIdx);}
            @Override public void set(int aIdx, int aValue) {IntDeque.this.set(aIdx, aValue);}
            @Override public int size() {return IntDeque.this.size();}
        };
    }
    @ApiStatus.Experimental
    public IntVector copy2vec() {
        boolean tCross;
        int tSize = mEnd-mStart;
        if (tSize < 0) {
            tSize += mData.length;
            tCross = true;
        } else {
            tCross = false;
        }
        IntVector rVector = IntVector.zeros(tSize);
        if (tCross) {
            final int[] rData = rVector.internalData();
            final int tShift = rVector.internalDataShift();
            System.arraycopy(mData, 0, rData, tShift+tSize-mEnd, mEnd);
            System.arraycopy(mData, mStart, rData, tShift, tSize-mEnd);
        } else {
            System.arraycopy(mData, mStart, rVector.internalData(), rVector.internalDataShift(), tSize);
        }
        return rVector;
    }
}
