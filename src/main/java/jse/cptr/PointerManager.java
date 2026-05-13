package jse.cptr;

import jse.gpu.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动回收各种指针的管理器，通过创建管理器对象后进行分配指针，
 * 通过存储这个管理器的引用来自动借助 java 垃圾回收来自动释放其分配的指针
 * <p>
 * 这套写法应该和原本逻辑是兼容的，并且可以保留最大的灵活性
 * @author liqa
 */
public class PointerManager implements AutoCloseable {
    private final Map<Long, AutoCPointerHandle> mCPointers = new HashMap<>();
    private final Map<Long, AutoCudaPointerHandle> mCudaPointers = new HashMap<>();
    
    private boolean mDead = false;
    public final boolean isClosed() {
        return mDead;
    }
    @Override public final void close() {
        if (mDead) return;
        mDead = true;
        for (AutoCPointerHandle tHandle : mCPointers.values()) {
            try {tHandle.dispose();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        for (AutoCudaPointerHandle tHandle : mCudaPointers.values()) {
            try {tHandle.dispose();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        mCPointers.clear();
        mCudaPointers.clear();
    }
    
    public long getCount(ICPointer rPtr) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        long tPtr = rPtr.ptr_();
        if (tPtr==0) return 0;
        AutoCPointerHandle tHandle = mCPointers.get(tPtr);
        if (tHandle==null) throw new IllegalArgumentException("Input pointer is not created by this PointerManager");
        if (tHandle.mSize!=rPtr.typeSize()) throw new IllegalArgumentException("Type size mismatch: "+tHandle.mSize+" vs "+rPtr.typeSize());
        return tHandle.mCount;
    }
    public long getCount(ICudaPointer rPtr) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        long tPtr = rPtr.ptr_();
        if (tPtr==0) return 0;
        AutoCudaPointerHandle tHandle = mCudaPointers.get(tPtr);
        if (tHandle==null) throw new IllegalArgumentException("Input pointer is not created by this PointerManager");
        return tHandle.mCount/rPtr.typeSize();
    }
    public void free(ICPointer rPtr) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        long tPtr = rPtr.ptr_();
        if (tPtr==0) throw new IllegalStateException("Cannot free a NULL pointer");
        AutoCPointerHandle tHandle = mCPointers.get(tPtr);
        if (tHandle==null) throw new IllegalArgumentException("Input pointer is not created by this PointerManager");
        try {tHandle.dispose();}
        catch (Exception e) {throw new RuntimeException(e);}
        mCPointers.remove(tPtr);
        rPtr.setPtr_(0);
    }
    public void free(ICudaPointer rPtr) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        long tPtr = rPtr.ptr_();
        if (tPtr==0) throw new IllegalStateException("Cannot free a NULL pointer");
        AutoCudaPointerHandle tHandle = mCudaPointers.get(tPtr);
        if (tHandle==null) throw new IllegalArgumentException("Input pointer is not created by this PointerManager");
        try {tHandle.dispose();}
        catch (Exception e) {throw new RuntimeException(e);}
        mCudaPointers.remove(tPtr);
        rPtr.setPtr_(0);
    }
    public void ensureCapacity(ICPointer rPtr, long aMinCount) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        final long oCount, tSize;
        final AutoCPointerHandle oHandle;
        long tPtr = rPtr.ptr_();
        if (tPtr==0) {
            oCount = 0;
            oHandle = null;
            tSize = rPtr.typeSize();
        } else {
            oHandle = mCPointers.get(tPtr);
            if (oHandle==null) throw new IllegalArgumentException("Input pointer is not created by this PointerManager");
            oCount = oHandle.mCount;
            tSize = oHandle.mSize;
            if (tSize!=rPtr.typeSize()) throw new IllegalArgumentException("Type size mismatch: "+tSize+" vs "+rPtr.typeSize());
        }
        if (aMinCount > oCount) {
            if (oHandle!=null) {
                try {oHandle.dispose();}
                catch (Exception e) {throw new RuntimeException(e);}
                mCPointers.remove(tPtr);
            }
            long tCount = Math.max(aMinCount, oCount + (oCount>>1));
            AutoCPointerHandle tHandle = new AutoCPointerHandle(this, tCount, tSize);
            mCPointers.put(tHandle.mPtr, tHandle);
            rPtr.setPtr_(tHandle.mPtr);
        }
    }
    public void ensureCapacity(ICudaPointer rPtr, long aMinCount) throws CudaException {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        final long oCount;
        final AutoCudaPointerHandle oHandle;
        long tPtr = rPtr.ptr_();
        if (tPtr==0) {
            oCount = 0;
            oHandle = null;
        } else {
            oHandle = mCudaPointers.get(tPtr);
            if (oHandle==null) throw new IllegalArgumentException("Input pointer is not created by this PointerManager");
            oCount = oHandle.mCount;
        }
        final long tMinCount = aMinCount*rPtr.typeSize();
        if (tMinCount > oCount) {
            if (oHandle!=null) {
                try {oHandle.dispose();}
                catch (Exception e) {throw new RuntimeException(e);}
                mCudaPointers.remove(tPtr);
            }
            long tCount = Math.max(tMinCount, oCount + (oCount>>1));
            AutoCudaPointerHandle tHandle = new AutoCudaPointerHandle(this, tCount);
            mCudaPointers.put(tHandle.mPtr, tHandle);
            rPtr.setPtr_(tHandle.mPtr);
        }
    }
    
    public CPointer newCPointer(long aCount) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new CPointer(0);
        AutoCPointerHandle tHandle = new AutoCPointerHandle(this, aCount, CPointer.TYPE_SIZE);
        mCPointers.put(tHandle.mPtr, tHandle);
        return new CPointer(tHandle.mPtr);
    }
    public CudaPointer newCudaPointer(long aCount) throws CudaException {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new CudaPointer(0);
        AutoCudaPointerHandle tHandle = new AutoCudaPointerHandle(this, aCount);
        mCudaPointers.put(tHandle.mPtr, tHandle);
        return new CudaPointer(tHandle.mPtr);
    }
    public IDoubleOrFloatCPointer newDoubleOrFloatCPointer(boolean aSingle, long aCount) {
        return aSingle ? newFloatCPointer(aCount) : newDoubleCPointer(aCount);
    }
    public IDoubleOrFloatCPointer newDoubleOrFloatCPointer(boolean aSingle) {
        return newDoubleOrFloatCPointer(aSingle, 0);
    }
    public IDoubleOrFloatCudaPointer newDoubleOrFloatCudaPointer(boolean aSingle, long aCount) throws CudaException {
        return aSingle ? newFloatCudaPointer(aCount) : newDoubleCudaPointer(aCount);
    }
    public IDoubleOrFloatCudaPointer newDoubleOrFloatCudaPointer(boolean aSingle) throws CudaException {
        return newDoubleOrFloatCudaPointer(aSingle, 0);
    }
    public DoubleCPointer newDoubleCPointer(long aCount) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new DoubleCPointer(0);
        AutoCPointerHandle tHandle = new AutoCPointerHandle(this, aCount, DoubleCPointer.TYPE_SIZE);
        mCPointers.put(tHandle.mPtr, tHandle);
        return new DoubleCPointer(tHandle.mPtr);
    }
    public DoubleCPointer newDoubleCPointer() {
        return newDoubleCPointer(0);
    }
    public DoubleCudaPointer newDoubleCudaPointer(long aCount) throws CudaException {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new DoubleCudaPointer(0);
        AutoCudaPointerHandle tHandle = new AutoCudaPointerHandle(this, aCount*DoubleCudaPointer.TYPE_SIZE);
        mCudaPointers.put(tHandle.mPtr, tHandle);
        return new DoubleCudaPointer(tHandle.mPtr);
    }
    public DoubleCudaPointer newDoubleCudaPointer() throws CudaException {
        return newDoubleCudaPointer(0);
    }
    public FloatCPointer newFloatCPointer(long aCount) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new FloatCPointer(0);
        AutoCPointerHandle tHandle = new AutoCPointerHandle(this, aCount, FloatCPointer.TYPE_SIZE);
        mCPointers.put(tHandle.mPtr, tHandle);
        return new FloatCPointer(tHandle.mPtr);
    }
    public FloatCPointer newFloatCPointer() {
        return newFloatCPointer(0);
    }
    public FloatCudaPointer newFloatCudaPointer(long aCount) throws CudaException {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new FloatCudaPointer(0);
        AutoCudaPointerHandle tHandle = new AutoCudaPointerHandle(this, aCount*FloatCudaPointer.TYPE_SIZE);
        mCudaPointers.put(tHandle.mPtr, tHandle);
        return new FloatCudaPointer(tHandle.mPtr);
    }
    public FloatCudaPointer newFloatCudaPointer() throws CudaException {
        return newFloatCudaPointer(0);
    }
    public IntCPointer newIntCPointer(long aCount) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new IntCPointer(0);
        AutoCPointerHandle tHandle = new AutoCPointerHandle(this, aCount, IntCPointer.TYPE_SIZE);
        mCPointers.put(tHandle.mPtr, tHandle);
        return new IntCPointer(tHandle.mPtr);
    }
    public IntCPointer newIntCPointer() {
        return newIntCPointer(0);
    }
    public IntCudaPointer newIntCudaPointer(long aCount) throws CudaException {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new IntCudaPointer(0);
        AutoCudaPointerHandle tHandle = new AutoCudaPointerHandle(this, aCount*IntCudaPointer.TYPE_SIZE);
        mCudaPointers.put(tHandle.mPtr, tHandle);
        return new IntCudaPointer(tHandle.mPtr);
    }
    public IntCudaPointer newIntCudaPointer() throws CudaException {
        return newIntCudaPointer(0);
    }
    public Int64CPointer newInt64CPointer(long aCount) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new Int64CPointer(0);
        AutoCPointerHandle tHandle = new AutoCPointerHandle(this, aCount, Int64CPointer.TYPE_SIZE);
        mCPointers.put(tHandle.mPtr, tHandle);
        return new Int64CPointer(tHandle.mPtr);
    }
    public Int64CPointer newInt64CPointer() {
        return newInt64CPointer(0);
    }
    public Int64CudaPointer newInt64CudaPointer(long aCount) throws CudaException {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new Int64CudaPointer(0);
        AutoCudaPointerHandle tHandle = new AutoCudaPointerHandle(this, aCount*Int64CudaPointer.TYPE_SIZE);
        mCudaPointers.put(tHandle.mPtr, tHandle);
        return new Int64CudaPointer(tHandle.mPtr);
    }
    public Int64CudaPointer newInt64CudaPointer() throws CudaException {
        return newInt64CudaPointer(0);
    }
    public AnyCPointer newAnyCPointer(long aCount) {
        if (mDead) throw new IllegalStateException("This PointerManager is dead");
        if (aCount<0) throw new IllegalArgumentException("Input count must be non-negative");
        if (aCount==0) return new AnyCPointer(0);
        AutoCPointerHandle tHandle = new AutoCPointerHandle(this, aCount, AnyCPointer.TYPE_SIZE);
        mCPointers.put(tHandle.mPtr, tHandle);
        return new AnyCPointer(tHandle.mPtr);
    }
    public AnyCPointer newAnyCPointer() {
        return newAnyCPointer(0);
    }
}
