package jsex.nnap;

import com.google.common.collect.Lists;
import jse.cache.ComplexVectorCache;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IComplexVector;
import jse.math.vector.IComplexVectorOperation;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static jse.math.MathEX.PI;
import static jse.math.MathEX.SH_LARGEST_L;

/**
 * 所有 nnap 的基组/描述符实现会放在这里
 * @author liqa
 */
public class Basis {
    
    public interface IBasis {
        double rcut();
        RowMatrix eval(int aTypeNum, IDxyzTypeIterable aNL);
        List<RowMatrix> evalPartial(int aTypeNum, boolean aCalBasis, IDxyzTypeIterable aNL);
    }
    
    public static class SphericalChebyshev implements IBasis {
        public final static int DEFAULT_NMAX = 5;
        public final static int DEFAULT_LMAX = 6;
        public final static double DEFAULT_RCUT = 6.2;
        
        private final int mNMax, mLMax;
        private final double mRCut;
        public SphericalChebyshev(int aNMax, int aLMax, double aRCut) {
            mNMax = aNMax; mLMax = aLMax; mRCut = aRCut;
        }
        
        @Override public double rcut() {
            return mRCut;
        }
        @Override public RowMatrix eval(int aTypeNum, IDxyzTypeIterable aNL) {
            return sphericalChebyshev(aTypeNum, mNMax, mLMax, mRCut, aNL);
        }
        @Override public List<RowMatrix> evalPartial(int aTypeNum, boolean aCalBasis, IDxyzTypeIterable aNL) {
            return sphericalChebyshevPartial(aTypeNum, mNMax, mLMax, mRCut, aCalBasis, aNL);
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
        final List<? extends IComplexVector> cnlm = ComplexVectorCache.getZeros((aLMax+1)*(aLMax+1), tSizeN);
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
                cnlm.get(tN).operation().mplus2this(tY, tMul);
                if (aTypeNum > 1) cnlm.get(tN+aNMax+1).operation().mplus2this(tY, wt*tMul);
            }
        });
        // 做标量积消去 m 项，得到此原子的 FP
        for (int tN = 0; tN < tSizeN; ++tN) for (int tL = 0; tL <= aLMax; ++tL) {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            int tStart = tL*tL;
            int tLen = tL+tL+1;
            rFingerPrint.set(tN, tL, (4.0*PI/(double)tLen) * cnlm.get(tN).subVec(tStart, tStart+tLen).operation().dot());
        }
        
        // 归还临时变量
        ComplexVectorCache.returnVec(tY);
        VectorCache.returnVec(tRn);
        ComplexVectorCache.returnVec(cnlm);
        
        return rFingerPrint;
    }
    public static RowMatrix sphericalChebyshev(int aNMax, int aLMax, double aRCutOff, IDxyzTypeIterable aNL) {return sphericalChebyshev(1, aNMax, aLMax, aRCutOff, aNL);}
    
    /**
     * {@link #sphericalChebyshev} 对于 {@code xyz} 偏微分的计算结果，主要用于力的计算
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @return 如果开启 {@code aCalBasis} 则输出基组分别对 {@code x, y, z} 偏微分的矩阵以及基组本身组成的 list
     * {@code [fpPx, fpPy, fpPz, fp]}；如果关闭则只输出 {@code [fpPx, fpPy, fpPz]}
     * @author liqa
     */
    public static List<RowMatrix> sphericalChebyshevPartial(final int aTypeNum, final int aNMax, final int aLMax, final double aRCutOff, final boolean aCalBasis, IDxyzTypeIterable aNL) {
        if (aNMax < 0) throw new IllegalArgumentException("Input n_max MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input l_max MUST be Non-Negative, input: "+aLMax);
        
        final int tSizeN = aTypeNum>1 ? aNMax+aNMax+2 : aNMax+1;
        @Nullable RowMatrix rFingerPrint = aCalBasis ? MatrixCache.getMatRow(tSizeN, aLMax+1) : null;
        RowMatrix rFingerPrintPx = MatrixCache.getMatRow(tSizeN, aLMax+1);
        RowMatrix rFingerPrintPy = MatrixCache.getMatRow(tSizeN, aLMax+1);
        RowMatrix rFingerPrintPz = MatrixCache.getMatRow(tSizeN, aLMax+1);
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final List<? extends IComplexVector> cnlm = ComplexVectorCache.getZeros((aLMax+1)*(aLMax+1), tSizeN);
        final List<? extends IComplexVector> cnlmPx = ComplexVectorCache.getZeros((aLMax+1)*(aLMax+1), tSizeN);
        final List<? extends IComplexVector> cnlmPy = ComplexVectorCache.getZeros((aLMax+1)*(aLMax+1), tSizeN);
        final List<? extends IComplexVector> cnlmPz = ComplexVectorCache.getZeros((aLMax+1)*(aLMax+1), tSizeN);
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
                final int tLen = tL+tL+1;
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
            for (int tN = 0; tN <= aNMax; ++tN) {
                // cnlm 部分
                double tMul = fc * tRn.get(tN);
                cnlm.get(tN).operation().mplus2this(tY, tMul);
                if (aTypeNum > 1) cnlm.get(tN+aNMax+1).operation().mplus2this(tY, wt*tMul);
                // 微分部分
                double tMulL = fc * tRnPx.get(tN);
                double tMulR = fcPx * tRn.get(tN);
                IComplexVectorOperation tOpt = cnlmPx.get(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPx, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (aTypeNum > 1) {
                    tOpt = cnlmPx.get(tN+aNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPx, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
                tMulL = fc * tRnPy.get(tN);
                tMulR = fcPy * tRn.get(tN);
                tOpt = cnlmPy.get(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPy, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (aTypeNum > 1) {
                    tOpt = cnlmPy.get(tN+aNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPy, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
                tMulL = fc * tRnPz.get(tN);
                tMulR = fcPz * tRn.get(tN);
                tOpt = cnlmPz.get(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPz, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (aTypeNum > 1) {
                    tOpt = cnlmPz.get(tN+aNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPz, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
            }
        });
        // 做标量积消去 m 项，得到此原子的 FP
        for (int tN = 0; tN < tSizeN; ++tN) for (int tL = 0; tL <= aLMax; ++tL) {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            int tStart = tL*tL;
            int tLen = tL+tL+1;
            double tMul = 4.0*PI/(double)tLen;
            double tMul2 = tMul+tMul;
            IComplexVector subCilm = cnlm.get(tN).subVec(tStart, tStart+tLen);
            IComplexVector subCilmPx = cnlmPx.get(tN).subVec(tStart, tStart+tLen);
            IComplexVector subCilmPy = cnlmPy.get(tN).subVec(tStart, tStart+tLen);
            IComplexVector subCilmPz = cnlmPz.get(tN).subVec(tStart, tStart+tLen);
            if (aCalBasis) rFingerPrint.set(tN, tL, tMul * subCilm.operation().dot());
            rFingerPrintPx.set(tN, tL, tMul2 * (subCilm.real().operation().dot(subCilmPx.real()) + subCilm.imag().operation().dot(subCilmPx.imag())));
            rFingerPrintPy.set(tN, tL, tMul2 * (subCilm.real().operation().dot(subCilmPy.real()) + subCilm.imag().operation().dot(subCilmPy.imag())));
            rFingerPrintPz.set(tN, tL, tMul2 * (subCilm.real().operation().dot(subCilmPz.real()) + subCilm.imag().operation().dot(subCilmPz.imag())));
        }
        
        // 归还临时变量
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
        ComplexVectorCache.returnVec(cnlmPz);
        ComplexVectorCache.returnVec(cnlmPy);
        ComplexVectorCache.returnVec(cnlmPx);
        ComplexVectorCache.returnVec(cnlm);
        
        return aCalBasis ?
            Lists.newArrayList(rFingerPrintPx, rFingerPrintPy, rFingerPrintPz, rFingerPrint) :
            Lists.newArrayList(rFingerPrintPx, rFingerPrintPy, rFingerPrintPz);
    }
    public static List<RowMatrix> sphericalChebyshevPartial(int aTypeNum, int aNMax, int aLMax, double aRCutOff, IDxyzTypeIterable aNL) {return sphericalChebyshevPartial(aTypeNum, aNMax, aLMax, aRCutOff, true, aNL);}
    public static List<RowMatrix> sphericalChebyshevPartial(int aNMax, int aLMax, double aRCutOff, IDxyzTypeIterable aNL) {return sphericalChebyshevPartial(1, aNMax, aLMax, aRCutOff, aNL);}
}
