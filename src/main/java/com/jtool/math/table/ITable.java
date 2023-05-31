package com.jtool.math.table;

import com.jtool.math.matrix.IMatrix;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;

/**
 * 通用的列表类，不提供 get(int...) 以及 size() 之类的接口方便 {@link AbstractMultiFrameTable} 的使用，
 * 不去涉及复杂的返回类型的情况，因此不使用泛型
 * @author liqa
 */
public interface ITable {
    /** 转为兼容性更好的 double[][] */
    double[][] data();
    
    boolean noHead();
    Collection<String> heads();
    String getHead(int aCol);
    int getColumn(String aHead);
    
    /** Map like stuffs */
    IVector get(String aHead);
    double get(int aRow, String aHead);
    boolean containsHead(String aHead);
    @SuppressWarnings("UnusedReturnValue")
    boolean setHead(String aOldHead, String aNewHead);
    
    /** Matrix like stuffs */
    IMatrix matrix();
    List<IVector> rows();
    IVector row(int aRow);
    List<IVector> cols();
    IVector col(int aCol);
    int rowNumber();
    int columnNumber();
    default @VisibleForTesting int nrows() {return rowNumber();}
    default @VisibleForTesting int ncols() {return columnNumber();}
    
    /** 切片操作，默认返回新的列表，refSlicer 则会返回引用的切片结果 */
    ITableSlicer slicer();
    ITableSlicer refSlicer();
    
    /** 对于表格，不提供复杂的生成器，但是保留 copy 方法 */
    ITable copy();
    
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting default IVector getAt(String aHead) {return get(aHead);}
    @VisibleForTesting default void putAt(String aOldHead, String aNewHead) {setHead(aOldHead, aNewHead);}
}
