package jse.math.table;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.matrix.IMatrix;
import jse.math.vector.IVector;
import jse.math.vector.IVectorGetter;

import java.util.List;

/**
 * @author liqa
 * <p> 抽象的拥有多个帧的列表的类，方便子类实现接口 </p>
 * <p> 注意这里每一帧的 Table 都是完全独立的，继承 ITable 只是为了方便只有一帧的情况直接使用 </p>
 */
public abstract class AbstractMultiFrameTable<T extends ITable> extends AbstractRandomAccessList<T> implements ITable {
    /** ITable stuffs */
    @Override public final double[][] data() {return defaultFrame().data();}
    @Override public final List<String> heads() {return defaultFrame().heads();}
    @Override public final String getHead(int aCol) {return defaultFrame().getHead(aCol);}
    @Override public final int getColumn(String aHead) {return defaultFrame().getColumn(aHead);}
    
    @Override public final IVector get(String aHead) {return defaultFrame().get(aHead);}
    @Override public final boolean containsHead(String aHead) {return defaultFrame().containsHead(aHead);}
    @Override public final boolean setHead(String aOldHead, String aNewHead) {return defaultFrame().setHead(aOldHead, aNewHead);}
    
    @Override public final double get(int aRow, String aHead) {return defaultFrame().get(aRow, aHead);}
    @Override public final void set(int aRow, String aHead, double aValue) {defaultFrame().set(aRow, aHead, aValue);}
    @Override public final void put(String aHead, double aValue) {defaultFrame().put(aHead, aValue);}
    @Override public final void put(String aHead, IVector aVector) {defaultFrame().put(aHead, aVector);}
    @Override public final void put(String aHead, IVectorGetter aVectorGetter) {defaultFrame().put(aHead, aVectorGetter);}
    @Override public final void put(String aHead, double[] aData) {defaultFrame().put(aHead, aData);}
    @Override public final void put(String aHead, Iterable<? extends Number> aList) {defaultFrame().put(aHead, aList);}
    @Override public final IMatrix asMatrix() {return defaultFrame().asMatrix();}
    @Override public final List<? extends IVector> rows() {return defaultFrame().rows();}
    @Override public final IVector row(int aRow) {return defaultFrame().row(aRow);}
    @Override public final List<? extends IVector> cols() {return defaultFrame().cols();}
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
