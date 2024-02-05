package jse.math.table;

import jse.code.UT;
import jse.math.matrix.IMatrix;
import jse.math.matrix.IMatrixGetter;
import jse.math.vector.IVector;

import java.util.Collection;

/**
 * @author liqa
 * <p> 获取列表的类，默认获取 {@link Table} </p>
 */
public class Tables {
    private Tables() {}
    
    public static ITable zeros(int aRowNum, String... aHeads) {return Table.zeros(aRowNum, aHeads);}
    public static ITable zeros(int aRowNum, int aColNum) {return Table.zeros(aRowNum, aColNum);}
    
    public static ITable from(int aRowNum, IMatrixGetter aDataGetter, String... aHeads) {
        ITable rTable = zeros(aRowNum, aHeads);
        rTable.asMatrix().fill(aDataGetter);
        return rTable;
    }
    public static ITable from(IMatrix aData, String... aHeads) {
        ITable rTable = zeros(aData.rowNumber(), aHeads);
        rTable.asMatrix().fill(aData);
        return rTable;
    }
    
    public static ITable from(Collection<?> aRows, String... aHeads) {return fromRows(aRows, aHeads);}
    public static ITable fromRows(Collection<?> aRows, String... aHeads) {
        ITable rTable = zeros(aRows.size(), aHeads);
        rTable.asMatrix().fillWithRows(aRows);
        return rTable;
    }
    public static ITable fromCols(Collection<?> aCols, String... aHeads) {
        int tRowNum;
        Object tFirst = UT.Code.first(aCols);
        if (tFirst instanceof Collection) {
            tRowNum = ((Collection<?>)tFirst).size();
        } else if (tFirst instanceof IVector) {
            tRowNum = ((IVector)tFirst).size();
        } else {
            throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number> or IVector");
        }
        ITable rTable = zeros(tRowNum, aHeads);
        rTable.asMatrix().fillWithCols(aCols);
        return rTable;
    }
}
