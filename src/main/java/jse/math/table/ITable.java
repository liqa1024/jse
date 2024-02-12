package jse.math.table;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.matrix.IMatrix;
import jse.math.vector.IVector;
import jse.math.vector.IVectorGetter;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * 通用的列表类，不提供 get(int...) 以及 size() 之类的接口方便 {@link AbstractMultiFrameTable} 的使用，
 * 不去涉及复杂的返回类型的情况，因此不使用泛型
 * @author liqa
 */
public interface ITable {
    /** 转为兼容性更好的 double[][] */
    double[][] data();
    
    String getHead(int aCol);
    int getColumn(String aHead);
    default List<String> heads() {
        // 这样防止被外部修改
        return new AbstractRandomAccessList<String>() {
            @Override public String get(int index) {return getHead(index);}
            @Override public int size() {return columnNumber();}
        };
    }
    
    /** Map like stuffs */
    IVector get(String aHead);
    void put(String aHead, double aValue);
    void put(String aHead, IVector aVector);
    void put(String aHead, IVectorGetter aVectorGetter);
    void put(String aHead, double[] aData);
    void put(String aHead, Iterable<? extends Number> aList);
    boolean containsHead(String aHead);
    boolean setHead(String aOldHead, String aNewHead);
    
    /** Matrix like stuffs */
    IMatrix asMatrix();
    double get(int aRow, String aHead);
    void set(int aRow, String aHead, double aValue);
    List<? extends IVector> rows();
    IVector row(int aRow);
    List<? extends IVector> cols();
    IVector col(String aHead);
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
    @VisibleForTesting default void putAt(String aHead, double aValue) {put(aHead, aValue);}
    @VisibleForTesting default void putAt(String aHead, IVector aVector) {put(aHead, aVector);}
    @VisibleForTesting default void putAt(String aHead, double[] aData) {put(aHead, aData);}
    @VisibleForTesting default void putAt(String aHead, Iterable<? extends Number> aList) {put(aHead, aList);}
}
