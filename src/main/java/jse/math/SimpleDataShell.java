package jse.math;

import org.jetbrains.annotations.Nullable;

class SimpleDataShell<D> implements IDataShell<D> {
    private final int mSize;
    private D mData;
    SimpleDataShell(int aSize, D aData) {mData = aData; mSize = aSize;}
    
    @Override public void setInternalData(D aData) {mData = aData;}
    @Override public SimpleDataShell<D> newShell() {return new SimpleDataShell<>(mSize, null);}
    @Override public D internalData() {return mData;}
    @Override public int internalDataSize() {return mSize;}
    @Override public @Nullable D getIfHasSameOrderData(Object aObj) {return null;}
}
