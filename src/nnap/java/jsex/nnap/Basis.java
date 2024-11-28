package jsex.nnap;

import com.google.common.collect.Lists;
import jse.cache.ComplexMatrixCache;
import jse.cache.ComplexVectorCache;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.math.MathEX;
import jse.math.matrix.*;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static jse.math.MathEX.PI;
import static jse.math.MathEX.SH_LARGEST_L;

/**
 * 所有 nnap 的基组/描述符实现暂时都会放在这里
 * @author liqa
 */
public class Basis {
    
    @ApiStatus.Experimental
    public interface IBasis {
        double rcut();
        default int nrows() {return rowNumber();}
        default int ncols() {return columnNumber();}
        int rowNumber();
        int columnNumber();
        RowMatrix eval(IDxyzTypeIterable aNL);
        List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, boolean aCalCross, IDxyzTypeIterable aNL);
        default List<@NotNull RowMatrix> evalPartial(IDxyzTypeIterable aNL) {return evalPartial(true, false, aNL);}
    }
    
    public static class SphericalChebyshev implements IBasis {
        public final static int DEFAULT_NMAX = 5;
        public final static int DEFAULT_LMAX = 6;
        public final static double DEFAULT_RCUT = 6.2;
        
        private final int mTypeNum;
        private final int mNMax, mLMax;
        private final double mRCut;
        public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
            mTypeNum = aTypeNum;
            mNMax = aNMax; mLMax = aLMax;
            mRCut = aRCut;
        }
        
        @Override public double rcut() {
            return mRCut;
        }
        @Override public int rowNumber() {
            return mTypeNum>1 ? mNMax+mNMax+2 : mNMax+1;
        }
        @Override public int columnNumber() {
            return mLMax+1;
        }
        @Override public RowMatrix eval(IDxyzTypeIterable aNL) {
            return sphericalChebyshev(mTypeNum, mNMax, mLMax, mRCut, aNL);
        }
        @Override public List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, boolean aCalCross, IDxyzTypeIterable aNL) {
            return sphericalChebyshevPartial(mTypeNum, mNMax, mLMax, mRCut, aCalBasis, aCalCross, aNL);
        }
    }
    
    @FunctionalInterface public interface IDxyzTypeIterable {void forEachDxyzType(IDxyzTypeDo aDxyzTypeDo);}
    @FunctionalInterface public interface IDxyzTypeDo {void run(double aDx, double aDy, double aDz, int aType);}
    
    /** 需要的固定系数存储 */
    private final static IVector SH_SQRT_LPM_LMM1; // sqrt((l+m)(l-m+1))
    private final static IVector SH_SQRT_LPM1_LMM; // sqrt((l+m+1)(l-m))
    static {
        final int tSize = (SH_LARGEST_L+1)*(SH_LARGEST_L+1);
        SH_SQRT_LPM_LMM1 = Vectors.NaN(tSize);
        SH_SQRT_LPM1_LMM = Vectors.NaN(tSize);
        int tStart = 0;
        for (int tL = 0; tL <= SH_LARGEST_L; ++tL) {
            for (int tM = -tL; tM <= tL; ++tM) {
                SH_SQRT_LPM_LMM1.set(tStart+tL+tM, MathEX.Fast.sqrt((tL+tM) * (tL-tM+1)));
                SH_SQRT_LPM1_LMM.set(tStart+tL+tM, MathEX.Fast.sqrt((tL+tM+1) * (tL-tM)));
            }
            tStart += tL+tL+1;
        }
    }
    
    /**
     * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
     * 主要用于作为机器学习的输入向量；这是 NNAP 中默认使用的原子基组。
     * <p>
     * References:
     * <a href="https://arxiv.org/abs/2211.03350v3">
     * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
     * @author Su Rui, liqa
     * @param aTypeNum 原子种类数目，默认为 {@code 1}
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCutOff 截断半径
     * @param aNL 近邻列表遍历器
     * @return 原子描述符矩阵组成的数组，n 为行，l 为列，因此 asVecRow 即为原本定义的基；如果存在超过一个种类则输出行数翻倍
     */
    public static RowMatrix sphericalChebyshev(final int aTypeNum, final int aNMax, final int aLMax, final double aRCutOff, IDxyzTypeIterable aNL) {
        if (aNMax < 0) throw new IllegalArgumentException("Input n_max MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input l_max MUST be Non-Negative, input: "+aLMax);
        
        final int tSizeN = aTypeNum>1 ? aNMax+aNMax+2 : aNMax+1;
        final RowMatrix rFingerPrint = MatrixCache.getMatRow(tSizeN, aLMax+1);
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final ColumnComplexMatrix cnlm = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
        // 缓存 Rn 数组
        final IVector tRn = VectorCache.getVec(aNMax+1);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final IComplexVector tY = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        
        // 遍历近邻计算 Ylm, Rn, fc
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= aRCutOff) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > aTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+aTypeNum+")");
            
            // 计算种类的权重
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc
            double fc = MathEX.Fast.powFast(1.0 - MathEX.Fast.pow2(dis/aRCutOff), 4);
            // 统一遍历一次计算 Rn
            final double tX = 1.0 - 2.0*dis/aRCutOff;
            tRn.fill(n -> MathEX.Func.chebyshev(n, tX));
            
            // 遍历求 n，l 的情况
            MathEX.Func.sphericalHarmonicsFull2DestXYZDis_(aLMax, dx, dy, dz, dis, tY);
            for (int tN = 0; tN <= aNMax; ++tN) {
                // 现在提供了 mplus2this 支持将数乘到 tY 中后再加到 cijm，可以不用中间变量；
                // 虽然看起来和使用 operate2this 效率基本一致，即使后者理论上应该还会创建一些 DoubleComplex；
                // 总之至少没有反向优化，并且这样包装后更加不吃编译器的优化，也不存在一大坨 lambda 表达式，以及传入的 DoubleComplex 一定不是引用等这种约定
                double tMul = fc * tRn.get(tN);
                cnlm.col(tN).operation().mplus2this(tY, tMul);
                if (aTypeNum > 1) cnlm.col(tN+aNMax+1).operation().mplus2this(tY, wt*tMul);
            }
        });
        // 做标量积消去 m 项，得到此原子的 FP
        for (int tN = 0; tN < tSizeN; ++tN) for (int tL = 0; tL <= aLMax; ++tL) {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            int tStart = tL*tL;
            int tLen = tL+tL+1;
            rFingerPrint.set(tN, tL, (4.0*PI/(double)tLen) * cnlm.col(tN).subVec(tStart, tStart+tLen).operation().dot());
        }
        
        // 归还临时变量
        ComplexVectorCache.returnVec(tY);
        VectorCache.returnVec(tRn);
        ComplexMatrixCache.returnMat(cnlm);
        
        return rFingerPrint;
    }
    public static RowMatrix sphericalChebyshev(int aNMax, int aLMax, double aRCutOff, IDxyzTypeIterable aNL) {return sphericalChebyshev(1, aNMax, aLMax, aRCutOff, aNL);}
    
    /**
     * {@link #sphericalChebyshev} 对于 {@code xyz} 偏微分的计算结果，主要用于力的计算
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     * @author liqa
     */
    public static List<RowMatrix> sphericalChebyshevPartial(final int aTypeNum, final int aNMax, final int aLMax, final double aRCutOff, final boolean aCalBasis, final boolean aCalCross, IDxyzTypeIterable aNL) {
        if (aNMax < 0) throw new IllegalArgumentException("Input n_max MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input l_max MUST be Non-Negative, input: "+aLMax);
        
        final int tSizeN = aTypeNum>1 ? aNMax+aNMax+2 : aNMax+1;
        @Nullable RowMatrix rFingerPrint = aCalBasis ? MatrixCache.getMatRow(tSizeN, aLMax+1) : null;
        RowMatrix rFingerPrintPx = MatrixCache.getMatRow(tSizeN, aLMax+1);
        RowMatrix rFingerPrintPy = MatrixCache.getMatRow(tSizeN, aLMax+1);
        RowMatrix rFingerPrintPz = MatrixCache.getMatRow(tSizeN, aLMax+1);
        @Nullable List<RowMatrix> rFingerPrintPxCross = aCalCross ? new ArrayList<>() : null;
        @Nullable List<RowMatrix> rFingerPrintPyCross = aCalCross ? new ArrayList<>() : null;
        @Nullable List<RowMatrix> rFingerPrintPzCross = aCalCross ? new ArrayList<>() : null;
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final ColumnComplexMatrix cnlm = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
        final ColumnComplexMatrix cnlmPx = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
        final ColumnComplexMatrix cnlmPy = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
        final ColumnComplexMatrix cnlmPz = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
        final @Nullable List<ColumnComplexMatrix> cnlmPxAll = aCalCross ? new ArrayList<>() : null;
        final @Nullable List<ColumnComplexMatrix> cnlmPyAll = aCalCross ? new ArrayList<>() : null;
        final @Nullable List<ColumnComplexMatrix> cnlmPzAll = aCalCross ? new ArrayList<>() : null;
        // 缓存 Rn 数组
        final IVector tRn = VectorCache.getVec(aNMax+1);
        final IVector tRnPx = VectorCache.getVec(aNMax+1);
        final IVector tRnPy = VectorCache.getVec(aNMax+1);
        final IVector tRnPz = VectorCache.getVec(aNMax+1);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final IComplexVector tY = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        final IComplexVector tYPtheta = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        final IComplexVector tYPphi = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        final IComplexVector tYPx = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        final IComplexVector tYPy = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        final IComplexVector tYPz = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        
        // 遍历近邻计算 Ylm, Rn, fc
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= aRCutOff) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > aTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+aTypeNum+")");
            
            // 计算种类的权重
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc 以及偏导数
            double fcMul = 1.0 - MathEX.Fast.pow2(dis/aRCutOff);
            double fcMul3 = MathEX.Fast.pow3(fcMul);
            double fc = fcMul3 * fcMul;
            double fcPMul = 8.0 * fcMul3 / (aRCutOff*aRCutOff);
            double fcPx = dx * fcPMul;
            double fcPy = dy * fcPMul;
            double fcPz = dz * fcPMul;
            // 统一遍历一次计算 Rn 以及偏导数
            final double tX = 1.0 - 2.0*dis/aRCutOff;
            tRn.fill(n -> MathEX.Func.chebyshev(n, tX));
            final double tRnPMul = 2.0 / (dis*aRCutOff);
            tRnPx.fill(n -> n==0 ? 0.0 : (n*MathEX.Func.chebyshev2(n-1, tX)*tRnPMul));
            tRnPy.fill(tRnPx);
            tRnPz.fill(tRnPx);
            tRnPx.multiply2this(dx);
            tRnPy.multiply2this(dy);
            tRnPz.multiply2this(dz);
            // 统一遍历一次计算 Ylm 以及偏导数；这里需要使用事先计算好角度的版本
            double dxy = MathEX.Fast.hypot(dx, dy);
            final double cosTheta = dz / dis;
            final double sinTheta = dxy / dis;
            final double cosPhi;
            final double sinPhi;
            final boolean dxyCloseZero = MathEX.Code.numericEqual(dxy, 0.0);
            if (dxyCloseZero) {
                cosPhi = 1.0;
                sinPhi = 0.0;
            } else {
                cosPhi = dx / dxy;
                sinPhi = dy / dxy;
            }
            MathEX.Func.sphericalHarmonicsFull2Dest4_(aLMax, cosTheta, sinTheta, cosPhi, sinPhi, tY);
            if (dxyCloseZero) tYPphi.fill(0.0); // 这样来修复顶点的情况，此时另一边 tYPtheta 会恰好弥补使得全局连续
            for (int tL = 0; tL <= aLMax; ++tL) {
                // 这里简单处理，使用这种遍历的方式来获取对应的 l 和 m
                final int fL = tL;
                final int tStart = fL*fL;
                final int tLen = fL+fL+1;
                final int tEnd = tStart + tLen;
                IComplexVector subY = tY.subVec(tStart, tEnd);
                if (!dxyCloseZero) {
                    IComplexVector subYPphi = tYPphi.subVec(tStart, tEnd);
                    subYPphi.real().fill(i -> -(i-fL)*subY.getImag(i));
                    subYPphi.imag().fill(i ->  (i-fL)*subY.getReal(i));
                }
                // 这里实际运算比较复杂，因此直接分实部虚部运算
                IComplexVector subYPtheta = tYPtheta.subVec(tStart, tEnd);
                subYPtheta.real().fill(i -> {
                    double out = 0.0;
                    if (i > 0) {
                        out -= 0.5*SH_SQRT_LPM_LMM1.get(tStart+i)*cosPhi * subY.getReal(i-1);
                        out += 0.5*SH_SQRT_LPM_LMM1.get(tStart+i)*sinPhi * subY.getImag(i-1);
                    }
                    if (i < tLen-1) {
                        out += 0.5*SH_SQRT_LPM1_LMM.get(tStart+i)*cosPhi * subY.getReal(i+1);
                        out += 0.5*SH_SQRT_LPM1_LMM.get(tStart+i)*sinPhi * subY.getImag(i+1);
                    }
                    return out;
                });
                subYPtheta.imag().fill(i -> {
                    double out = 0.0;
                    if (i > 0) {
                        out -= 0.5*SH_SQRT_LPM_LMM1.get(tStart+i)*cosPhi * subY.getImag(i-1);
                        out -= 0.5*SH_SQRT_LPM_LMM1.get(tStart+i)*sinPhi * subY.getReal(i-1);
                    }
                    if (i < tLen-1) {
                        out += 0.5*SH_SQRT_LPM1_LMM.get(tStart+i)*cosPhi * subY.getImag(i+1);
                        out -= 0.5*SH_SQRT_LPM1_LMM.get(tStart+i)*sinPhi * subY.getReal(i+1);
                    }
                    return out;
                });
            }
            // 最后转换为 xyz 的偏微分
            final double thetaPx = -cosTheta * cosPhi / dis;
            final double thetaPy = -cosTheta * sinPhi / dis;
            final double thetaPz =  sinTheta / dis;
            final double phiPx = dxyCloseZero ? 0.0 :  sinPhi / dxy;
            final double phiPy = dxyCloseZero ? 0.0 : -cosPhi / dxy;
            tYPx.fill(0.0); tYPx.operation().mplus2this(tYPtheta, thetaPx); if (!dxyCloseZero) tYPx.operation().mplus2this(tYPphi, phiPx);
            tYPy.fill(0.0); tYPy.operation().mplus2this(tYPtheta, thetaPy); if (!dxyCloseZero) tYPy.operation().mplus2this(tYPphi, phiPy);
            tYPz.fill(0.0); tYPz.operation().mplus2this(tYPtheta, thetaPz);
            
            // 遍历求 n，l 的情况
            final ColumnComplexMatrix cnlmPxUpdate, cnlmPyUpdate, cnlmPzUpdate;
            if (aCalCross) {
                cnlmPxUpdate = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
                cnlmPyUpdate = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
                cnlmPzUpdate = ComplexMatrixCache.getZerosCol((aLMax+1)*(aLMax+1), tSizeN);
                cnlmPxAll.add(cnlmPxUpdate);
                cnlmPyAll.add(cnlmPyUpdate);
                cnlmPzAll.add(cnlmPzUpdate);
            } else {
                cnlmPxUpdate = cnlmPx;
                cnlmPyUpdate = cnlmPy;
                cnlmPzUpdate = cnlmPz;
            }
            
            for (int tN = 0; tN <= aNMax; ++tN) {
                // cnlm 部分
                double tMul = fc * tRn.get(tN);
                cnlm.col(tN).operation().mplus2this(tY, tMul);
                if (aTypeNum > 1) cnlm.col(tN+aNMax+1).operation().mplus2this(tY, wt*tMul);
                // 微分部分
                double tMulL = fc * tRnPx.get(tN);
                double tMulR = fcPx * tRn.get(tN);
                IComplexVectorOperation tOpt = cnlmPxUpdate.col(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPx, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (aTypeNum > 1) {
                    tOpt = cnlmPxUpdate.col(tN+aNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPx, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
                tMulL = fc * tRnPy.get(tN);
                tMulR = fcPy * tRn.get(tN);
                tOpt = cnlmPyUpdate.col(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPy, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (aTypeNum > 1) {
                    tOpt = cnlmPyUpdate.col(tN+aNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPy, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
                tMulL = fc * tRnPz.get(tN);
                tMulR = fcPz * tRn.get(tN);
                tOpt = cnlmPzUpdate.col(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPz, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (aTypeNum > 1) {
                    tOpt = cnlmPzUpdate.col(tN+aNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPz, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
            }
        });
        if (aCalCross) {
            // 如果计算了 cross 的，则需要在这里手动累加一下 cnlm
            for (ColumnComplexMatrix cnlmPxSub : cnlmPxAll) cnlmPx.plus2this(cnlmPxSub);
            for (ColumnComplexMatrix cnlmPySub : cnlmPyAll) cnlmPy.plus2this(cnlmPySub);
            for (ColumnComplexMatrix cnlmPzSub : cnlmPzAll) cnlmPz.plus2this(cnlmPzSub);
            // 在这里初始化 cross 的 FingerPrint 偏导
            final int tNN = cnlmPxAll.size();
            for (int i = 0; i < tNN; ++i) {
                rFingerPrintPxCross.add(MatrixCache.getMatRow(tSizeN, aLMax+1));
                rFingerPrintPyCross.add(MatrixCache.getMatRow(tSizeN, aLMax+1));
                rFingerPrintPzCross.add(MatrixCache.getMatRow(tSizeN, aLMax+1));
            }
            // 基组对于近邻原子坐标的偏导值和这里直接计算结果差一个负号；
            // 由于实际计算力时需要近邻的原本基组值来反向传播，因此这里结果实际会传递给近邻用于累加，所以需要的就是 基组对于近邻原子坐标的偏导值
            for (ColumnComplexMatrix cnlmPxSub : cnlmPxAll) cnlmPxSub.negative2this();
            for (ColumnComplexMatrix cnlmPySub : cnlmPyAll) cnlmPySub.negative2this();
            for (ColumnComplexMatrix cnlmPzSub : cnlmPzAll) cnlmPzSub.negative2this();
        }
        // 做标量积消去 m 项，得到此原子的 FP
        ShiftVector subCilmReal = new ShiftVector(cnlm.internalDataSize(), cnlm.internalDataShift(), cnlm.internalData()[0]), subCilmImag = new ShiftVector(cnlm.internalDataSize(), cnlm.internalDataShift(), cnlm.internalData()[1]);
        ShiftVector subCilmPxReal = new ShiftVector(cnlmPx.internalDataSize(), cnlmPx.internalDataShift(), cnlmPx.internalData()[0]), subCilmPxImag = new ShiftVector(cnlmPx.internalDataSize(), cnlmPx.internalDataShift(), cnlmPx.internalData()[1]);
        ShiftVector subCilmPyReal = new ShiftVector(cnlmPy.internalDataSize(), cnlmPy.internalDataShift(), cnlmPy.internalData()[0]), subCilmPyImag = new ShiftVector(cnlmPy.internalDataSize(), cnlmPy.internalDataShift(), cnlmPy.internalData()[1]);
        ShiftVector subCilmPzReal = new ShiftVector(cnlmPz.internalDataSize(), cnlmPz.internalDataShift(), cnlmPz.internalData()[0]), subCilmPzImag = new ShiftVector(cnlmPz.internalDataSize(), cnlmPz.internalDataShift(), cnlmPz.internalData()[1]);
        IVectorOperation subCilmRealOpt = subCilmReal.operation(), subCilmImagOpt = subCilmImag.operation();
        for (int tN = 0; tN < tSizeN; ++tN) {
            int tShift = tN * cnlm.rowNumber();
            for (int tL = 0; tL <= aLMax; ++tL) {
                // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
                int tStart = tL*tL;
                int tLen = tL+tL+1;
                double tMul = 4.0*PI/(double)tLen;
                double tMul2 = tMul+tMul;
                subCilmReal.setSize(tLen).setShift(tShift+tStart); subCilmImag.setSize(tLen).setShift(tShift+tStart);
                subCilmPxReal.setSize(tLen).setShift(tShift+tStart); subCilmPxImag.setSize(tLen).setShift(tShift+tStart);
                subCilmPyReal.setSize(tLen).setShift(tShift+tStart); subCilmPyImag.setSize(tLen).setShift(tShift+tStart);
                subCilmPzReal.setSize(tLen).setShift(tShift+tStart); subCilmPzImag.setSize(tLen).setShift(tShift+tStart);
                if (aCalBasis) rFingerPrint.set(tN, tL, tMul * (subCilmRealOpt.dot() + subCilmImagOpt.dot()));
                rFingerPrintPx.set(tN, tL, tMul2 * (subCilmRealOpt.dot(subCilmPxReal) + subCilmImagOpt.dot(subCilmPxImag)));
                rFingerPrintPy.set(tN, tL, tMul2 * (subCilmRealOpt.dot(subCilmPyReal) + subCilmImagOpt.dot(subCilmPyImag)));
                rFingerPrintPz.set(tN, tL, tMul2 * (subCilmRealOpt.dot(subCilmPzReal) + subCilmImagOpt.dot(subCilmPzImag)));
            }
        }
        // 如果计算 cross，则需要这样设置 cross 的 FingerPrint 偏导
        if (aCalCross) {
            final int tNN = cnlmPxAll.size();
            for (int i = 0; i < tNN; ++i) {
                ColumnComplexMatrix cnlmPxAllI = cnlmPxAll.get(i), cnlmPyAllI = cnlmPyAll.get(i), cnlmPzAllI = cnlmPzAll.get(i);
                ShiftVector subCilmPxAllIReal = new ShiftVector(cnlmPxAllI.internalDataSize(), cnlmPxAllI.internalDataShift(), cnlmPxAllI.internalData()[0]), subCilmPxAllIImag = new ShiftVector(cnlmPxAllI.internalDataSize(), cnlmPxAllI.internalDataShift(), cnlmPxAllI.internalData()[1]);
                ShiftVector subCilmPyAllIReal = new ShiftVector(cnlmPyAllI.internalDataSize(), cnlmPyAllI.internalDataShift(), cnlmPyAllI.internalData()[0]), subCilmPyAllIImag = new ShiftVector(cnlmPyAllI.internalDataSize(), cnlmPyAllI.internalDataShift(), cnlmPyAllI.internalData()[1]);
                ShiftVector subCilmPzAllIReal = new ShiftVector(cnlmPzAllI.internalDataSize(), cnlmPzAllI.internalDataShift(), cnlmPzAllI.internalData()[0]), subCilmPzAllIImag = new ShiftVector(cnlmPzAllI.internalDataSize(), cnlmPzAllI.internalDataShift(), cnlmPzAllI.internalData()[1]);
                RowMatrix tFingerPrintPxCrossI = rFingerPrintPxCross.get(i);
                RowMatrix tFingerPrintPyCrossI = rFingerPrintPyCross.get(i);
                RowMatrix tFingerPrintPzCrossI = rFingerPrintPzCross.get(i);
                for (int tN = 0; tN < tSizeN; ++tN) {
                    int tShift = tN * cnlm.rowNumber();
                    for (int tL = 0; tL <= aLMax; ++tL) {
                        int tStart = tL*tL;
                        int tLen = tL+tL+1;
                        double tMul = 4.0*PI/(double)tLen;
                        double tMul2 = tMul+tMul;
                        subCilmReal.setSize(tLen).setShift(tShift+tStart); subCilmImag.setSize(tLen).setShift(tShift+tStart);
                        subCilmPxAllIReal.setSize(tLen).setShift(tShift+tStart); subCilmPxAllIImag.setSize(tLen).setShift(tShift+tStart);
                        subCilmPyAllIReal.setSize(tLen).setShift(tShift+tStart); subCilmPyAllIImag.setSize(tLen).setShift(tShift+tStart);
                        subCilmPzAllIReal.setSize(tLen).setShift(tShift+tStart); subCilmPzAllIImag.setSize(tLen).setShift(tShift+tStart);
                        tFingerPrintPxCrossI.set(tN, tL, tMul2 * (subCilmRealOpt.dot(subCilmPxAllIReal) + subCilmImagOpt.dot(subCilmPxAllIImag)));
                        tFingerPrintPyCrossI.set(tN, tL, tMul2 * (subCilmRealOpt.dot(subCilmPyAllIReal) + subCilmImagOpt.dot(subCilmPyAllIImag)));
                        tFingerPrintPzCrossI.set(tN, tL, tMul2 * (subCilmRealOpt.dot(subCilmPzAllIReal) + subCilmImagOpt.dot(subCilmPzAllIImag)));
                    }
                }
            }
        }
        
        // 归还临时变量
        if (aCalCross) {
            for (ColumnComplexMatrix cnlmPzSub : cnlmPzAll) ComplexMatrixCache.returnMat(cnlmPzSub);
            for (ColumnComplexMatrix cnlmPySub : cnlmPyAll) ComplexMatrixCache.returnMat(cnlmPySub);
            for (ColumnComplexMatrix cnlmPxSub : cnlmPxAll) ComplexMatrixCache.returnMat(cnlmPxSub);
        }
        ComplexVectorCache.returnVec(tYPz);
        ComplexVectorCache.returnVec(tYPy);
        ComplexVectorCache.returnVec(tYPx);
        ComplexVectorCache.returnVec(tYPphi);
        ComplexVectorCache.returnVec(tYPtheta);
        ComplexVectorCache.returnVec(tY);
        VectorCache.returnVec(tRnPz);
        VectorCache.returnVec(tRnPy);
        VectorCache.returnVec(tRnPx);
        VectorCache.returnVec(tRn);
        ComplexMatrixCache.returnMat(cnlmPz);
        ComplexMatrixCache.returnMat(cnlmPy);
        ComplexMatrixCache.returnMat(cnlmPx);
        ComplexMatrixCache.returnMat(cnlm);
        
        List<RowMatrix> rOut = Lists.newArrayList(rFingerPrint, rFingerPrintPx, rFingerPrintPy, rFingerPrintPz);
        if (aCalCross) {
            rOut.addAll(rFingerPrintPxCross);
            rOut.addAll(rFingerPrintPyCross);
            rOut.addAll(rFingerPrintPzCross);
        }
        return rOut;
    }
    public static List<RowMatrix> sphericalChebyshevPartial(int aTypeNum, int aNMax, int aLMax, double aRCutOff, IDxyzTypeIterable aNL) {return sphericalChebyshevPartial(aTypeNum, aNMax, aLMax, aRCutOff, true, false, aNL);}
    public static List<RowMatrix> sphericalChebyshevPartial(int aNMax, int aLMax, double aRCutOff, IDxyzTypeIterable aNL) {return sphericalChebyshevPartial(1, aNMax, aLMax, aRCutOff, aNL);}
}
