package jse.math.table;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import jse.code.UT;
import jse.math.matrix.IMatrix;
import jse.math.matrix.IMatrixGetter;
import jse.math.vector.IVector;

import java.util.Collection;

import static jse.code.CS.ZL_STR;

/**
 * @author liqa
 * <p> 获取列表的类，默认获取 {@link Table} </p>
 */
public class Tables {
    private Tables() {}
    
    public static Table zeros(int aRowNum) {return Table.zeros(aRowNum);}
    public static Table zeros(int aRowNum, String... aHeads) {return Table.zeros(aRowNum, aHeads);}
    public static Table zeros(int aRowNum, int aColNum) {return Table.zeros(aRowNum, aColNum);}
    
    public static Table from(int aRowNum, String[] aHeads, IMatrixGetter aDataGetter) {
        Table rTable = zeros(aRowNum, aHeads);
        rTable.asMatrix().fill(aDataGetter);
        return rTable;
    }
    public static Table from(int aRowNum, Collection<? extends CharSequence> aHeads, IMatrixGetter aDataGetter) {
        return from(aRowNum, UT.Text.toArray(aHeads), aDataGetter);
    }
    public static Table from(int aRowNum, int aColNum, IMatrixGetter aDataGetter) {
        Table rTable = zeros(aRowNum, aColNum);
        rTable.asMatrix().fill(aDataGetter);
        return rTable;
    }
    public static Table from(IMatrix aData, String... aHeads) {
        if (aHeads==null || aHeads.length==0) return from(aData);
        Table rTable = zeros(aData.rowNumber(), aHeads);
        rTable.asMatrix().fill(aData);
        return rTable;
    }
    public static Table from(IMatrix aData) {
        Table rTable = zeros(aData.rowNumber(), aData.columnNumber());
        rTable.asMatrix().fill(aData);
        return rTable;
    }
    public static Table from(ITable aTable) {
        if (aTable instanceof Table) return ((Table)aTable).copy();
        Table rTable = zeros(aTable.rowNumber(), aTable.heads().toArray(ZL_STR));
        rTable.asMatrix().fill(aTable.asMatrix());
        return rTable;
    }
    /** Groovy stuff */
    public static Table from(int aRowNum, String[] aHeads, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {return from(aRowNum, aHeads, (i, j) -> aGroovyTask.call(i, j).doubleValue());}
    public static Table from(int aRowNum, Collection<? extends CharSequence> aHeads, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {return from(aRowNum, aHeads, (i, j) -> aGroovyTask.call(i, j).doubleValue());}
    public static Table from(int aRowNum, int aColNum, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {return from(aRowNum, aColNum, (i, j) -> aGroovyTask.call(i, j).doubleValue());}
    
    public static Table from(Collection<?> aRows, String... aHeads) {return fromRows(aRows, aHeads);}
    public static Table from(Collection<?> aRows) {return fromRows(aRows);}
    public static Table fromRows(Collection<?> aRows, String... aHeads) {
        if (aHeads==null || aHeads.length==0) return fromRows(aRows);
        Table rTable = zeros(aRows.size(), aHeads);
        rTable.asMatrix().fillWithRows(aRows);
        return rTable;
    }
    public static Table fromRows(Collection<?> aRows) {
        int tColNum;
        Object tFirst = UT.Code.first(aRows);
        if (tFirst instanceof Collection) {
            tColNum = ((Collection<?>)tFirst).size();
        } else
        if (tFirst instanceof IVector) {
            tColNum = ((IVector)tFirst).size();
        } else
        if (tFirst instanceof double[]) {
            tColNum = ((double[])tFirst).length;
        } else {
            throw new IllegalArgumentException("Type of Row Must be Collection<? extends Number>, IVector or double[]");
        }
        Table rTable = zeros(aRows.size(), tColNum);
        rTable.asMatrix().fillWithRows(aRows);
        return rTable;
    }
    public static Table fromCols(Collection<?> aCols, String... aHeads) {
        if (aHeads==null || aHeads.length==0) return fromCols(aCols);
        int tRowNum;
        Object tFirst = UT.Code.first(aCols);
        if (tFirst instanceof Collection) {
            tRowNum = ((Collection<?>)tFirst).size();
        } else
        if (tFirst instanceof IVector) {
            tRowNum = ((IVector)tFirst).size();
        } else
        if (tFirst instanceof double[]) {
            tRowNum = ((double[])tFirst).length;
        } else {
            throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number>, IVector or double[]");
        }
        Table rTable = zeros(tRowNum, aHeads);
        rTable.asMatrix().fillWithCols(aCols);
        return rTable;
    }
    public static Table fromCols(Collection<?> aCols) {
        int tRowNum;
        Object tFirst = UT.Code.first(aCols);
        if (tFirst instanceof Collection) {
            tRowNum = ((Collection<?>)tFirst).size();
        } else
        if (tFirst instanceof IVector) {
            tRowNum = ((IVector)tFirst).size();
        } else
        if (tFirst instanceof double[]) {
            tRowNum = ((double[])tFirst).length;
        } else {
            throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number>, IVector or double[]");
        }
        Table rTable = zeros(tRowNum, aCols.size());
        rTable.asMatrix().fillWithCols(aCols);
        return rTable;
    }
}
