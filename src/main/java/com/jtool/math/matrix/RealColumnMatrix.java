package com.jtool.math.matrix;

import com.jtool.math.IDataGenerator2;
import com.jtool.math.operator.IOperator2Full;

import java.util.*;
import java.util.concurrent.Callable;


/**
 * @author liqa
 * <p> 矩阵一般实现，按照列排序 </p>
 */
public class RealColumnMatrix extends AbstractMatrix<Double, RealColumnMatrix> {
    /** 提供默认的创建 */
    public static RealColumnMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RealColumnMatrix ones(int aRowNum, int aColNum) {
        double[] tData = new double[aRowNum*aColNum];
        Arrays.fill(tData, 1.0);
        return new RealColumnMatrix(aRowNum, tData);
    }
    public static RealColumnMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RealColumnMatrix zeros(int aRowNum, int aColNum) {return new RealColumnMatrix(aRowNum, new double[aRowNum*aColNum]);}
    
    
    private final double[] mData;
    private final int mRowNum;
    
    public RealColumnMatrix(int aRowNum, double[] aData) {
        mRowNum = aRowNum;
        mData = aData;
    }
    public RealColumnMatrix(double[][] aMatrix) {
        mRowNum = aMatrix.length;
        int tColNum = aMatrix[0].length;
        mData = new double[mRowNum*tColNum];
        for (int row = 0; row < mRowNum; ++row) {
            double[] tRow = aMatrix[row];
            for (int col = 0; col < tColNum; ++col) {
                mData[row + col*mRowNum] = tRow[col];
            }
        }
    }
    public RealColumnMatrix(Collection<? extends Collection<? extends Number>> aMatrix) {
        mRowNum = aMatrix.size();
        Iterator<? extends Collection<? extends Number>> tIt = aMatrix.iterator();
        Collection<? extends Number> tRow = tIt.next();
        mData = new double[mRowNum*tRow.size()];
        int row = 0;
        while (true) {
            int col = 0;
            for (Number tValue : tRow) {
                mData[row + col*mRowNum] = tValue.doubleValue();
                ++col;
            }
            if (!tIt.hasNext()) break;
            tRow = tIt.next();
            ++row;
        }
    }
    
    
    /** IMatrix stuffs */
    @Override public Double get_(int aRow, int aCol) {return mData[aRow + aCol*mRowNum];}
    @Override public void set_(int aRow, int aCol, Number aValue) {mData[aRow + aCol*mRowNum] = aValue.doubleValue();}
    @Override public Double getAndSet_(int aRow, int aCol, Number aValue) {
        int tIdx = aRow + aCol*mRowNum;
        Double oValue = mData[tIdx];
        mData[tIdx] = aValue.doubleValue();
        return oValue;
    }
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mData.length/mRowNum;}
    
    /** 重写这个提高列向的索引速度 */
    @Override public List<Double> col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new AbstractList<Double>() {
            private final int mShift = aCol*mRowNum;
            @Override public int size() {return mRowNum;}
            @Override public Double get(int aRow) {
                if (aRow<0 || aRow>=mRowNum) throw new IndexOutOfBoundsException("Row: "+aRow);
                return mData[aRow + mShift];
            }
            @Override public Double set(int aRow, Double aValue) {
                if (aRow<0 || aRow>=mRowNum) throw new IndexOutOfBoundsException("Row: "+aRow);
                int tIdx = aRow + mShift;
                Double oValue = mData[tIdx];
                mData[tIdx] = aValue;
                return oValue;
            }
        };
    }
    
    
    /** IGenerator stuffs */
    @Override public IDataGenerator2<RealColumnMatrix> generator() {
        return new AbstractGenerator() {
            @Override public RealColumnMatrix same() {
                double[] rData = new double[mData.length];
                System.arraycopy(mData, 0, rData, 0, mData.length);
                return new RealColumnMatrix(mRowNum, rData);
            }
            @Override public RealColumnMatrix ones(int aRowNum, int aColNum) {return RealColumnMatrix.ones(aRowNum, aColNum);}
            @Override public RealColumnMatrix zeros(int aRowNum, int aColNum) {return RealColumnMatrix.zeros(aRowNum, aColNum);}
            @Override public RealColumnMatrix from(int aRowNum, int aColNum, Callable<? extends Number> aCall) {
                double[] rData = new double[aRowNum*aColNum];
                for (int i = 0; i < rData.length; ++i) {
                    try {rData[i] = aCall.call().doubleValue();}
                    catch (Exception e) {throw new RuntimeException(e);}
                }
                return new RealColumnMatrix(aRowNum, rData);
            }
            @Override public RealColumnMatrix from(int aRowNum, int aColNum, IOperator2Full<? extends Number, Integer, Integer> aOpt) {
                double[] rData = new double[aRowNum*aColNum];
                int i = 0;
                for (int col = 0; col < aColNum; ++col) for (int row = 0; row < aRowNum; ++row) {
                    rData[i] = aOpt.cal(row, col).doubleValue();
                    ++i;
                }
                return new RealColumnMatrix(aRowNum, rData);
            }
        };
    }
}
