package com.jtool.math.matrix;

import com.jtool.math.vector.IVectorGenerator;
import com.jtool.math.vector.AbstractVector;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.RealVector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


/**
 * @author liqa
 * <p> 矩阵一般实现，按照列排序 </p>
 */
public class RealColumnMatrix extends AbstractMatrixFull<Double, RealColumnMatrix, RealVector> {
    /** 提供默认的创建 */
    public static RealColumnMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RealColumnMatrix ones(int aRowNum, int aColNum) {
        double[] tData = new double[aRowNum*aColNum];
        Arrays.fill(tData, 1.0);
        return new RealColumnMatrix(aRowNum, aColNum, tData);
    }
    public static RealColumnMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RealColumnMatrix zeros(int aRowNum, int aColNum) {return new RealColumnMatrix(aRowNum, aColNum, new double[aRowNum*aColNum]);}
    
    
    private final double[] mData;
    private final int mRowNum;
    private final int mColNum;
    
    public RealColumnMatrix(int aRowNum, int aColNum, double[] aData) {
        mRowNum = aRowNum;
        mData = aData;
        mColNum = aColNum;
    }
    
    public RealColumnMatrix(int aRowNum, double[] aData) {this(aRowNum, aData.length/aRowNum, aData);}
    public RealColumnMatrix(double[][] aMat) {
        mRowNum = aMat.length;
        mColNum = aMat[0].length;
        mData = new double[mRowNum*mColNum];
        for (int row = 0; row < mRowNum; ++row) {
            double[] tRow = aMat[row];
            for (int col = 0; col < mColNum; ++col) {
                mData[row + col*mRowNum] = tRow[col];
            }
        }
    }
    public RealColumnMatrix(Collection<? extends Collection<? extends Number>> aRows) {
        mRowNum = aRows.size();
        Iterator<? extends Collection<? extends Number>> tIt = aRows.iterator();
        Collection<? extends Number> tRow = tIt.next();
        mColNum = tRow.size();
        mData = new double[mRowNum*mColNum];
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
    @Override public int columnNumber() {return mColNum;}
    
    /** 重写这个提高列向的索引速度 */
    @Override public IVector<Double> col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new AbstractVector<Double>() {
            private final int mShift = aCol*mRowNum;
            @Override public Double get_(int aIdx) {return mData[aIdx + mShift];}
            @Override public void set_(int aIdx, Number aValue) {mData[aIdx + mShift] = aValue.doubleValue();}
            @Override public Double getAndSet_(int aIdx, Number aValue) {
                int tIdx = aIdx + mShift;
                Double oValue = mData[tIdx];
                mData[tIdx] = aValue.doubleValue();
                return oValue;
            }
            @Override public int size() {return mRowNum;}
        };
    }
    
    
    /** 重写这些接口来加速批量填充过程 */
    @Override public void fill(Number aValue) {Arrays.fill(mData, aValue.doubleValue());}
    @Override public void fillWith(IMatrixGetter<? extends Number> aMatrixGetter) {
        int tRowNum = rowNumber();
        int tColNum = columnNumber();
        int i = 0;
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) {
            mData[i] = aMatrixGetter.get(row, col).doubleValue();
            ++i;
        }
    }
    
    
    /** IGenerator stuffs，重写 same 接口专门优化 */
    @Override protected RealColumnMatrix newZeros(int aRowNum, int aColNum) {return RealColumnMatrix.zeros(aRowNum, aColNum);}
    @Override protected RealVector newZeros(int aSize) {return RealVector.zeros(aSize);}
    @Override public IMatrixGenerator<RealColumnMatrix> generatorMat() {
        return new MatrixGenerator() {
            @Override public RealColumnMatrix same() {
                double[] rData = new double[mData.length];
                System.arraycopy(mData, 0, rData, 0, mData.length);
                return new RealColumnMatrix(mRowNum, mColNum, rData);
            }
        };
    }
    @Override public IVectorGenerator<RealVector> generatorVec() {
        return new VectorGenerator() {
            @Override public RealVector same() {
                double[] rData = new double[mData.length];
                System.arraycopy(mData, 0, rData, 0, mData.length);
                return new RealVector(rData);
            }
        };
    }
}
