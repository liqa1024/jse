package com.jtool.math.table;

import com.jtool.code.UT;
import com.jtool.math.matrix.IMatrixFull;
import com.jtool.math.vector.IVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author liqa
 * <p> 获取列表的类，默认获取 {@link Table} </p>
 */
public class Tables {
    private Tables() {}
    
    
    public static Table from(IMatrixFull<?, ?> aMatrix, String... aHeads) {
        List<double[]> rData = new ArrayList<>(aMatrix.rowNumber());
        for (IVector tRow : aMatrix.rows()) rData.add(tRow.vec());
        return (aHeads!=null && aHeads.length>0) ? new Table(aHeads, rData) : new Table(rData);
    }
    
    public static Table from(int aSize, Iterable<? extends Iterable<? extends Number>> aRows, String... aHeads) {return fromRows(aSize, aRows, aHeads);}
    public static Table from(int aRowNum, int aColNum, Iterable<? extends Iterable<? extends Number>> aRows, String... aHeads) {return fromRows(aRowNum, aColNum, aRows, aHeads);}
    public static Table from(Collection<? extends Collection<? extends Number>> aRows, String... aHeads) {return fromRows(aRows, aHeads);}
    
    public static Table fromRows(int aSize, Iterable<? extends Iterable<? extends Number>> aRows, String... aHeads) {return fromRows(aSize, aSize, aRows, aHeads);}
    public static Table fromRows(int aRowNum, int aColNum, Iterable<? extends Iterable<? extends Number>> aRows, String... aHeads) {
        List<double[]> rData = new ArrayList<>(aRowNum);
        for (Iterable<? extends Number> tRow : aRows) rData.add(UT.Code.toData(aColNum, tRow));
        return (aHeads!=null && aHeads.length>0) ? new Table(aHeads, rData) : new Table(rData);
    }
    public static Table fromRows(Collection<? extends Collection<? extends Number>> aRows, String... aHeads) {
        List<double[]> rData = new ArrayList<>(aRows.size());
        for (Collection<? extends Number> tRow : aRows) rData.add(UT.Code.toData(tRow));
        return (aHeads!=null && aHeads.length>0) ? new Table(aHeads, rData) : new Table(rData);
    }
    
    public static Table fromCols(int aSize, Iterable<? extends Iterable<? extends Number>> aCols, String... aHeads) {return fromCols(aSize, aSize, aCols, aHeads);}
    public static Table fromCols(int aRowNum, int aColNum, Iterable<? extends Iterable<? extends Number>> aCols, String... aHeads) {
        List<double[]> rData = new ArrayList<>(aRowNum);
        for (int row = 0; row < aRowNum; ++row) rData.add(new double[aColNum]);
        int col = 0;
        for (Iterable<? extends Number> tCol : aCols) {
            int row = 0;
            for (Number tValue : tCol) {
                rData.get(row)[col] = tValue.doubleValue();
                ++row;
            }
            ++col;
        }
        return (aHeads!=null && aHeads.length>0) ? new Table(aHeads, rData) : new Table(rData);
    }
    public static Table fromCols(Collection<? extends Collection<? extends Number>> aCols, String... aHeads) {
        return fromCols(aCols.iterator().next().size(), aCols.size(), aCols, aHeads);
    }
}
