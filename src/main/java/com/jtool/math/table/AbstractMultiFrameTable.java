package com.jtool.math.table;

import com.jtool.math.matrix.IMatrix;
import com.jtool.math.vector.IVector;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

/**
 * @author liqa
 * <p> 抽象的拥有多个帧的列表的类，方便子类实现接口 </p>
 * <p> 注意这里每一帧的 Table 都是完全独立的，继承 ITable 只是为了方便只有一帧的情况直接使用 </p>
 */
public abstract class AbstractMultiFrameTable<T extends ITable> extends AbstractList<T> implements ITable {
    /** ITable stuffs */
    @Override public double[][] data() {return defaultFrame().data();}
    @Override public boolean noHead() {return defaultFrame().noHead();}
    @Override public Collection<String> heads() {return defaultFrame().heads();}
    @Override public String getHead(int aCol) {return defaultFrame().getHead(aCol);}
    @Override public int getColumn(String aHead) {return defaultFrame().getColumn(aHead);}
    
    @Override public IVector get(String aHead) {return defaultFrame().get(aHead);}
    @Override public double get(int aRow, String aHead) {return defaultFrame().get(aRow, aHead);}
    @Override public boolean containsHead(String aHead) {return defaultFrame().containsHead(aHead);}
    @Override public boolean setHead(String aOldHead, String aNewHead) {return defaultFrame().setHead(aOldHead, aNewHead);}
    
    @Override public IMatrix matrix() {return defaultFrame().matrix();}
    @Override public List<IVector> rows() {return defaultFrame().rows();}
    @Override public IVector row(int aRow) {return defaultFrame().row(aRow);}
    @Override public List<IVector> cols() {return defaultFrame().cols();}
    @Override public IVector col(int aCol) {return defaultFrame().col(aCol);}
    @Override public int rowNumber() {return defaultFrame().rowNumber();}
    @Override public int columnNumber() {return defaultFrame().columnNumber();}
    
    @Override public ITableSlicer slicer() {return defaultFrame().slicer();}
    @Override public ITableSlicer refSlicer() {return defaultFrame().refSlicer();}
    
    @Override public ITable copy() {return defaultFrame().copy();} // TODO 需要改成返回 AbstractMultiFrameTable
    
    /** stuff to override */
    public T defaultFrame() {return get(0);}
    
    /** AbstractList stuffs */
    public abstract T get(int index);
    public abstract int size();
}
