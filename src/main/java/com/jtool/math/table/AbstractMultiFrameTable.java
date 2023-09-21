package com.jtool.math.table;

import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;

import java.util.Collection;
import java.util.List;

/**
 * @author liqa
 * <p> 抽象的拥有多个帧的列表的类，方便子类实现接口 </p>
 * <p> 注意这里每一帧的 Table 都是完全独立的，继承 ITable 只是为了方便只有一帧的情况直接使用 </p>
 */
public abstract class AbstractMultiFrameTable<T extends ITable> extends AbstractRandomAccessList<T> implements ITable {
    /** ITable stuffs */
    @Override public final double[][] data() {return defaultFrame().data();}
    @Override public final boolean noHead() {return defaultFrame().noHead();}
    @Override public final List<String> heads() {return defaultFrame().heads();}
    @Override public final String getHead(int aCol) {return defaultFrame().getHead(aCol);}
    @Override public final int getColumn(String aHead) {return defaultFrame().getColumn(aHead);}
    
    @Override public final IVector get(String aHead) {return defaultFrame().get(aHead);}
    @Override public final boolean containsHead(String aHead) {return defaultFrame().containsHead(aHead);}
    @Override public final boolean setHead(String aOldHead, String aNewHead) {return defaultFrame().setHead(aOldHead, aNewHead);}
    
    @Override public final double get(int aRow, String aHead) {return defaultFrame().get(aRow, aHead);}
    @Override public final void set(int aRow, String aHead, double aValue) {defaultFrame().set(aRow, aHead, aValue);}
    @Override public final void fill(String aHead, double aValue) {defaultFrame().fill(aHead, aValue);}
    @Override public final void fill(String aHead, IVector aVector) {defaultFrame().fill(aHead, aVector);}
    @Override public final void fill(String aHead, IVectorGetter aVectorGetter) {defaultFrame().fill(aHead, aVectorGetter);}
    @Override public final void fill(String aHead, double[] aData) {defaultFrame().fill(aHead, aData);}
    @Override public final void fill(String aHead, Iterable<? extends Number> aList) {defaultFrame().fill(aHead, aList);}
    @Override public final IMatrix matrix() {return defaultFrame().matrix();}
    @Override public final List<IVector> rows() {return defaultFrame().rows();}
    @Override public final IVector row(int aRow) {return defaultFrame().row(aRow);}
    @Override public final List<IVector> cols() {return defaultFrame().cols();}
    @Override public final IVector col(String aHead) {return defaultFrame().col(aHead);}
    @Override public final int rowNumber() {return defaultFrame().rowNumber();}
    @Override public final int columnNumber() {return defaultFrame().columnNumber();}
    
    @Override public final ITableSlicer slicer() {return defaultFrame().slicer();}
    @Override public final ITableSlicer refSlicer() {return defaultFrame().refSlicer();}
    
    /** stuff to override */
    public T defaultFrame() {return get(0);}
    public abstract AbstractMultiFrameTable<T> copy();
    
    /** AbstractList stuffs */
    public abstract T get(int index);
    public abstract int size();
}
