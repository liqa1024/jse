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
    
    public static Table from(int aRowNum, IMatrixGetter aDataGetter, String... aHeads) {
        Table rTable = zeros(aRowNum, aHeads);
        rTable.asMatrix().fill(aDataGetter);
        return rTable;
    }
    public static Table from(IMatrix aData, String... aHeads) {
        Table rTable = zeros(aData.rowNumber(), aHeads);
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
    public static Table from(int aRowNum, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask, String... aHeads) {return from(aRowNum, (i, j) -> aGroovyTask.call(i, j).doubleValue(), aHeads);}
    
    public static Table from(Collection<?> aRows, String... aHeads) {return fromRows(aRows, aHeads);}
    public static Table fromRows(Collection<?> aRows, String... aHeads) {
        Table rTable = zeros(aRows.size(), aHeads);
        rTable.asMatrix().fillWithRows(aRows);
        return rTable;
    }
    public static Table fromCols(Collection<?> aCols, String... aHeads) {
        int tRowNum;
        Object tFirst = UT.Code.first(aCols);
        if (tFirst instanceof Collection) {
            tRowNum = ((Collection<?>)tFirst).size();
        } else if (tFirst instanceof IVector) {
            tRowNum = ((IVector)tFirst).size();
        } else {
            throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number> or IVector");
        }
        Table rTable = zeros(tRowNum, aHeads);
        rTable.asMatrix().fillWithCols(aCols);
        return rTable;
    }
}
