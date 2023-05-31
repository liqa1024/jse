package com.jtool.math.table;


import com.jtool.math.matrix.AbstractMatrix;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vector;

import java.util.List;

/**
 * 方便直接使用 csv 读取结果的数据格式
 * @author liqa
 */
public final class Table extends AbstractTable {
    private final IMatrix mMatrix;
    
    public Table(String[] aHeads, List<double[]> aData) {
        super(aHeads);
        mMatrix = genMatrix_(aHeads.length, aData);
    }
    public Table(int aColNum, List<double[]> aData) {
        super(aColNum);
        mMatrix = genMatrix_(aColNum, aData);
    }
    public Table(List<double[]> aData) {
        this(aData.isEmpty() ? 0 : aData.get(0).length, aData);
    }
    
    private static IMatrix genMatrix_(final int aColNum, final List<double[]> aData) {
        return new AbstractMatrix() {
            @Override public double get_(int aRow, int aCol) {return aData.get(aRow)[aCol];}
            @Override public void set_(int aRow, int aCol, double aValue) {aData.get(aRow)[aCol] = aValue;}
            @Override public double getAndSet_(int aRow, int aCol, double aValue) {
                double oValue = aData.get(aRow)[aCol];
                aData.get(aRow)[aCol] = aValue;
                return oValue;
            }
            @Override public int rowNumber() {return aData.size();}
            @Override public int columnNumber() {return aColNum;}
            
            /** Optimize stuffs，重写这个提高行向的索引速度 */
            @Override public IVector row(final int aRow) {
                if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
                return new Vector(columnNumber(), aData.get(aRow));
            }
        };
    }
    
    /** AbstractTable stuffs */
    @Override public IMatrix matrix() {return mMatrix;}
}
