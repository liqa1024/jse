package jsex.nnap.basis;

import com.google.common.collect.Lists;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.UT;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jse.math.MathEX.*;

/**
 * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
 * 主要用于作为机器学习的输入向量；这是 NNAP 中默认使用的原子基组。
 * <p>
 * 为了中间变量缓存利用效率，此类相同实例线程不安全，而不同实例之间线程安全
 * <p>
 * References:
 * <a href="https://arxiv.org/abs/2211.03350v3">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * @author Su Rui, liqa
 */
@SuppressWarnings("SameParameterValue")
public class SphericalChebyshev implements IBasis {
    /** 需要的固定系数存储 */
    private final static Vector SQRT_LPM_LMM1; // sqrt((l+m)(l-m+1))
    private final static Vector SQRT_LPM1_LMM; // sqrt((l+m+1)(l-m))
    static {
        final int tSize = (SH_LARGEST_L+1)*(SH_LARGEST_L+1);
        SQRT_LPM_LMM1 = Vectors.NaN(tSize);
        SQRT_LPM1_LMM = Vectors.NaN(tSize);
        int tStart = 0;
        for (int tL = 0; tL <= SH_LARGEST_L; ++tL) {
            for (int tM = -tL; tM <= tL; ++tM) {
                SQRT_LPM_LMM1.set(tStart+tL+tM, MathEX.Fast.sqrt((tL+tM) * (tL-tM+1)));
                SQRT_LPM1_LMM.set(tStart+tL+tM, MathEX.Fast.sqrt((tL+tM+1) * (tL-tM)));
            }
            tStart += tL+tL+1;
        }
    }
    
    public final static int DEFAULT_NMAX = 5;
    public final static int DEFAULT_LMAX = 6;
    public final static double DEFAULT_RCUT = 6.2;
    
    private final int mTypeNum;
    private final int mNMax, mLMax;
    private final double mRCut;
    /**
     * @param aTypeNum 原子种类数目
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCut 截断半径
     */
    public SphericalChebyshev(int aTypeNum, int aNMax, int aLMax, double aRCut) {
        if (aNMax < 0) throw new IllegalArgumentException("Input nmax MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input lmax MUST be Non-Negative, input: "+aLMax);
        mTypeNum = aTypeNum;
        mNMax = aNMax;
        mLMax = aLMax;
        mRCut = aRCut;
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "spherical_chebyshev");
        rSaveTo.put("nmax", mNMax);
        rSaveTo.put("lmax", mLMax);
        rSaveTo.put("rcut", mRCut);
    }
    @SuppressWarnings("rawtypes")
    public static SphericalChebyshev load(int aTypeNum, Map aMap) {
        return new SphericalChebyshev(
            aTypeNum,
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_NMAX, "nmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_LMAX, "lmax")).intValue(),
            ((Number) UT.Code.getWithDefault(aMap, DEFAULT_RCUT, "rcut")).doubleValue()
        );
    }
    
    /** @return {@inheritDoc} */
    @Override public double rcut() {
        return mRCut;
    }
    /**
     * @return {@inheritDoc}；如果只有一个种类则为
     * {@code nmax+1}，如果超过一个种类则为 {@code 2(nmax+1)}
     */
    @Override public int rowNumber() {
        return mTypeNum>1 ? mNMax+mNMax+2 : mNMax+1;
    }
    /** @return {@inheritDoc}；具体为 {@code lmax+1} */
    @Override public int columnNumber() {
        return mLMax+1;
    }
    
    /** 一些缓存的中间变量，现在统一作为对象存储，对于这种大规模的缓存情况可以进一步提高效率 */
    private @Nullable RowMatrix mCnlm = null, mCnlmPx = null, mCnlmPy = null, mCnlmPz = null;
    @NotNull RowMatrix bufCnlm(boolean aClear) {if (mCnlm==null) {mCnlm = MatrixCache.getMatRow(rowNumber(), (mLMax+1)*(mLMax+1));} if (aClear) {mCnlm.fill(0.0);} return mCnlm;}
    @NotNull RowMatrix bufCnlmPx(boolean aClear) {if (mCnlmPx==null) {mCnlmPx = MatrixCache.getMatRow(rowNumber(), (mLMax+1)*(mLMax+1));} if (aClear) {mCnlmPx.fill(0.0);} return mCnlmPx;}
    @NotNull RowMatrix bufCnlmPy(boolean aClear) {if (mCnlmPy==null) {mCnlmPy = MatrixCache.getMatRow(rowNumber(), (mLMax+1)*(mLMax+1));} if (aClear) {mCnlmPy.fill(0.0);} return mCnlmPy;}
    @NotNull RowMatrix bufCnlmPz(boolean aClear) {if (mCnlmPz==null) {mCnlmPz = MatrixCache.getMatRow(rowNumber(), (mLMax+1)*(mLMax+1));} if (aClear) {mCnlmPz.fill(0.0);} return mCnlmPz;}
    
    private final List<RowMatrix> mCnlmPxAll = new ArrayList<>(), mCnlmPyAll = new ArrayList<>(), mCnlmPzAll = new ArrayList<>();
    @NotNull RowMatrix bufCnlmPxAll(int i, boolean aClear) {while (mCnlmPxAll.size()<=i) {mCnlmPxAll.add(MatrixCache.getMatRow(rowNumber(), (mLMax+1)*(mLMax+1)));} RowMatrix tCnlmPx = mCnlmPxAll.get(i); if (aClear) {tCnlmPx.fill(0.0);} return tCnlmPx;}
    @NotNull RowMatrix bufCnlmPyAll(int i, boolean aClear) {while (mCnlmPyAll.size()<=i) {mCnlmPyAll.add(MatrixCache.getMatRow(rowNumber(), (mLMax+1)*(mLMax+1)));} RowMatrix tCnlmPy = mCnlmPyAll.get(i); if (aClear) {tCnlmPy.fill(0.0);} return tCnlmPy;}
    @NotNull RowMatrix bufCnlmPzAll(int i, boolean aClear) {while (mCnlmPzAll.size()<=i) {mCnlmPzAll.add(MatrixCache.getMatRow(rowNumber(), (mLMax+1)*(mLMax+1)));} RowMatrix tCnlmPz = mCnlmPzAll.get(i); if (aClear) {tCnlmPz.fill(0.0);} return tCnlmPz;}
    
    private @Nullable Vector mRn = null, mRnPx = null, mRnPy = null, mRnPz = null;
    @NotNull Vector bufRn(boolean aClear) {if (mRn==null) {mRn = VectorCache.getVec(mNMax+1);} if (aClear) {mRn.fill(0.0);} return mRn;}
    @NotNull Vector bufRnPx(boolean aClear) {if (mRnPx==null) {mRnPx = VectorCache.getVec(mNMax+1);} if (aClear) {mRnPx.fill(0.0);} return mRnPx;}
    @NotNull Vector bufRnPy(boolean aClear) {if (mRnPy==null) {mRnPy = VectorCache.getVec(mNMax+1);} if (aClear) {mRnPy.fill(0.0);} return mRnPy;}
    @NotNull Vector bufRnPz(boolean aClear) {if (mRnPz==null) {mRnPz = VectorCache.getVec(mNMax+1);} if (aClear) {mRnPz.fill(0.0);} return mRnPz;}
    
    private @Nullable Vector mY = null, mYPx = null, mYPy = null, mYPz = null, mYPphi = null, mYPtheta = null;
    @NotNull Vector bufY(boolean aClear) {if (mY==null) {mY = VectorCache.getVec((mLMax+1)*(mLMax+1));} if (aClear) {mY.fill(0.0);} return mY;}
    @NotNull Vector bufYPx(boolean aClear) {if (mYPx==null) {mYPx = VectorCache.getVec((mLMax+1)*(mLMax+1));} if (aClear) {mYPx.fill(0.0);} return mYPx;}
    @NotNull Vector bufYPy(boolean aClear) {if (mYPy==null) {mYPy = VectorCache.getVec((mLMax+1)*(mLMax+1));} if (aClear) {mYPy.fill(0.0);} return mYPy;}
    @NotNull Vector bufYPz(boolean aClear) {if (mYPz==null) {mYPz = VectorCache.getVec((mLMax+1)*(mLMax+1));} if (aClear) {mYPz.fill(0.0);} return mYPz;}
    @NotNull Vector bufYPphi(boolean aClear) {if (mYPphi==null) {mYPphi = VectorCache.getVec((mLMax+1)*(mLMax+1));} if (aClear) {mYPphi.fill(0.0);} return mYPphi;}
    @NotNull Vector bufYPtheta(boolean aClear) {if (mYPtheta==null) {mYPtheta = VectorCache.getVec((mLMax+1)*(mLMax+1));} if (aClear) {mYPtheta.fill(0.0);} return mYPtheta;}
    
    @Override public void shutdown() {
        if (mCnlm != null) MatrixCache.returnMat(mCnlm);
        if (mCnlmPx != null) MatrixCache.returnMat(mCnlmPx);
        if (mCnlmPy != null) MatrixCache.returnMat(mCnlmPy);
        if (mCnlmPz != null) MatrixCache.returnMat(mCnlmPz);
        for (RowMatrix cnlmPzSub : mCnlmPzAll) MatrixCache.returnMat(cnlmPzSub);
        for (RowMatrix cnlmPySub : mCnlmPyAll) MatrixCache.returnMat(cnlmPySub);
        for (RowMatrix cnlmPxSub : mCnlmPxAll) MatrixCache.returnMat(cnlmPxSub);
        if (mRn != null) VectorCache.returnVec(mRn);
        if (mRnPx != null) VectorCache.returnVec(mRnPx);
        if (mRnPy != null) VectorCache.returnVec(mRnPy);
        if (mRnPz != null) VectorCache.returnVec(mRnPz);
        if (mY != null) VectorCache.returnVec(mY);
        if (mYPx != null) VectorCache.returnVec(mYPx);
        if (mYPy != null) VectorCache.returnVec(mYPy);
        if (mYPz != null) VectorCache.returnVec(mYPz);
        if (mYPphi != null) VectorCache.returnVec(mYPphi);
        if (mYPtheta != null) VectorCache.returnVec(mYPtheta);
    }
    
    /**
     * {@inheritDoc}
     * @param aNL 近邻列表遍历器
     * @return {@inheritDoc}
     */
    @Override public RowMatrix eval(IDxyzTypeIterable aNL) {
        final int tSizeN = rowNumber();
        final RowMatrix rFingerPrint = MatrixCache.getMatRow(tSizeN, mLMax+1);
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final RowMatrix cnlm = bufCnlm(true);
        // 缓存 Rn 数组
        final IVector tRn = bufRn(false);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final IVector tY = bufY(false);
        
        // 遍历近邻计算 Ylm, Rn, fc
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= mRCut) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            
            // 计算种类的权重
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc
            double fc = MathEX.Fast.powFast(1.0 - MathEX.Fast.pow2(dis/mRCut), 4);
            // 统一遍历一次计算 Rn
            final double tX = 1.0 - 2.0*dis/mRCut;
            tRn.fill(n -> MathEX.Func.chebyshev(n, tX));
            
            // 遍历求 n，l 的情况；现在采用实球谐函数进行计算
            MathEX.Func.realSphericalHarmonicsFull2DestXYZDis_(mLMax, dx, dy, dz, dis, tY);
            for (int tN = 0; tN <= mNMax; ++tN) {
                // 现在统一使用 mplus2this 实现这个操作
                double tMul = fc * tRn.get(tN);
                cnlm.row(tN).operation().mplus2this(tY, tMul);
                if (mTypeNum > 1) cnlm.row(tN+mNMax+1).operation().mplus2this(tY, wt*tMul);
            }
        });
        // 做标量积消去 m 项，得到此原子的 FP
        for (int tN = 0; tN < tSizeN; ++tN) for (int tL = 0; tL <= mLMax; ++tL) {
            // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
            int tStart = tL*tL;
            int tLen = tL+tL+1;
            rFingerPrint.set(tN, tL, (4.0*PI/(double)tLen) * cnlm.row(tN).subVec(tStart, tStart+tLen).operation().dot());
        }
        
        return rFingerPrint;
    }
    
    /**
     * {@inheritDoc}
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aNL 近邻列表遍历器
     * @return {@inheritDoc}
     */
    @Override public List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, boolean aCalCross, IDxyzTypeIterable aNL) {
        final int tSizeN = rowNumber();
        @Nullable RowMatrix rFingerPrint = aCalBasis ? MatrixCache.getMatRow(tSizeN, mLMax+1) : null;
        RowMatrix rFingerPrintPx = MatrixCache.getMatRow(tSizeN, mLMax+1);
        RowMatrix rFingerPrintPy = MatrixCache.getMatRow(tSizeN, mLMax+1);
        RowMatrix rFingerPrintPz = MatrixCache.getMatRow(tSizeN, mLMax+1);
        @Nullable List<RowMatrix> rFingerPrintPxCross = aCalCross ? new ArrayList<>() : null;
        @Nullable List<RowMatrix> rFingerPrintPyCross = aCalCross ? new ArrayList<>() : null;
        @Nullable List<RowMatrix> rFingerPrintPzCross = aCalCross ? new ArrayList<>() : null;
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final RowMatrix cnlm = bufCnlm(true);
        final RowMatrix cnlmPx = bufCnlmPx(true);
        final RowMatrix cnlmPy = bufCnlmPy(true);
        final RowMatrix cnlmPz = bufCnlmPz(true);
        // 缓存 Rn 数组
        final IVector tRn = bufRn(false);
        final IVector tRnPx = bufRnPx(false);
        final IVector tRnPy = bufRnPy(false);
        final IVector tRnPz = bufRnPz(false);
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final IVector tY = bufY(false);
        final IVector tYPtheta = bufYPtheta(false);
        final IVector tYPphi = bufYPphi(false);
        final IVector tYPx = bufYPx(false);
        final IVector tYPy = bufYPy(false);
        final IVector tYPz = bufYPz(false);
        // 记录一下近邻数目（对于 cross 的情况）
        final int[] tNN = aCalCross ? new int[]{0} : null;
        
        // 遍历近邻计算 Ylm, Rn, fc
        aNL.forEachDxyzType((dx, dy, dz, type) -> {
            double dis = MathEX.Fast.hypot(dx, dy, dz);
            if (dis >= mRCut) return; // 理论不会触发，因为在上层遍历时就排除了
            if (type > mTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+mTypeNum+")");
            
            // 计算种类的权重
            double wt = ((type&1)==1) ? type : -type;
            // 计算截断函数 fc 以及偏导数
            double fcMul = 1.0 - MathEX.Fast.pow2(dis/mRCut);
            double fcMul3 = MathEX.Fast.pow3(fcMul);
            double fc = fcMul3 * fcMul;
            double fcPMul = 8.0 * fcMul3 / (mRCut*mRCut);
            double fcPx = dx * fcPMul;
            double fcPy = dy * fcPMul;
            double fcPz = dz * fcPMul;
            // 统一遍历一次计算 Rn 以及偏导数
            final double tX = 1.0 - 2.0*dis/mRCut;
            tRn.fill(n -> MathEX.Func.chebyshev(n, tX));
            final double tRnPMul = 2.0 / (dis*mRCut);
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
            // 现在采用实球谐函数进行计算
            MathEX.Func.realSphericalHarmonicsFull2Dest4_(mLMax, cosTheta, sinTheta, cosPhi, sinPhi, tY);
            if (dxyCloseZero) tYPphi.fill(0.0); // 这样来修复顶点的情况，此时另一边 tYPtheta 会恰好弥补使得全局连续
            for (int tL = 0; tL <= mLMax; ++tL) {
                // 这里简单处理，使用这种遍历的方式来获取对应的 l 和 m
                final int fL = tL;
                final int tStart = fL*fL;
                final int tLen = fL+fL+1;
                final int tEnd = tStart + tLen;
                IVector subY = tY.subVec(tStart, tEnd);
                if (!dxyCloseZero) {
                    IVector subYPphi = tYPphi.subVec(tStart, tEnd);
                    subYPphi.fill(i -> -(i-fL)*subY.get(fL+fL-i));
                }
                // 这里实际运算比较复杂，需要分多种情况考虑
                IVector subYPtheta = tYPtheta.subVec(tStart, tEnd);
                subYPtheta.fill(i -> {
                    int m = i-fL;
                    switch(m) {
                        case 0: {
                            if (fL == 0) return 0.0;
                            return SQRT_LPM_LMM1.get(tStart+i)*SQRT2_INV * (cosPhi*subY.get(i+1) + sinPhi*subY.get(i-1));
                        }
                        case 1: {
                            double out = -SQRT_LPM_LMM1.get(tStart+i)*SQRT2_INV * cosPhi*subY.get(i-1);
                            if (fL > 1) {
                                out += 0.5*SQRT_LPM1_LMM.get(tStart+i) * (cosPhi*subY.get(i+1) + sinPhi*subY.get(i-3));
                            }
                            return out;
                        }
                        case -1: {
                            double out = -SQRT_LPM1_LMM.get(tStart+i)*SQRT2_INV * sinPhi*subY.get(i+1);
                            if (fL > 1) {
                                out += 0.5*SQRT_LPM_LMM1.get(tStart+i) * (cosPhi*subY.get(i-1) - sinPhi*subY.get(i+3));
                            }
                            return out;
                        }
                        default: {
                            if (m > 0) {
                                double out = -0.5*SQRT_LPM_LMM1.get(tStart+i) * (cosPhi*subY.get(i-1) - sinPhi*subY.get(fL-m+1));
                                if (fL > m) {
                                    out += 0.5*SQRT_LPM1_LMM.get(tStart+i) * (cosPhi*subY.get(i+1) + sinPhi*subY.get(fL-m-1));
                                }
                                return out;
                            } else {
                                double out = -0.5*SQRT_LPM1_LMM.get(tStart+i) * (cosPhi*subY.get(i+1) + sinPhi*subY.get(fL-m-1));
                                if (fL > -m) {
                                    out += 0.5*SQRT_LPM_LMM1.get(tStart+i) * (cosPhi*subY.get(i-1) - sinPhi*subY.get(fL-m+1));
                                }
                                return out;
                            }
                        }
                    }
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
            final RowMatrix cnlmPxUpdate, cnlmPyUpdate, cnlmPzUpdate;
            if (aCalCross) {
                int j = tNN[0]; ++tNN[0];
                cnlmPxUpdate = bufCnlmPxAll(j, true);
                cnlmPyUpdate = bufCnlmPyAll(j, true);
                cnlmPzUpdate = bufCnlmPzAll(j, true);
            } else {
                cnlmPxUpdate = cnlmPx;
                cnlmPyUpdate = cnlmPy;
                cnlmPzUpdate = cnlmPz;
            }
            
            for (int tN = 0; tN <= mNMax; ++tN) {
                // cnlm 部分
                double tMul = fc * tRn.get(tN);
                cnlm.row(tN).operation().mplus2this(tY, tMul);
                if (mTypeNum > 1) cnlm.row(tN+mNMax+1).operation().mplus2this(tY, wt*tMul);
                // 微分部分
                double tMulL = fc * tRnPx.get(tN);
                double tMulR = fcPx * tRn.get(tN);
                IVectorOperation tOpt = cnlmPxUpdate.row(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPx, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (mTypeNum > 1) {
                    tOpt = cnlmPxUpdate.row(tN+mNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPx, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
                tMulL = fc * tRnPy.get(tN);
                tMulR = fcPy * tRn.get(tN);
                tOpt = cnlmPyUpdate.row(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPy, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (mTypeNum > 1) {
                    tOpt = cnlmPyUpdate.row(tN+mNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPy, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
                tMulL = fc * tRnPz.get(tN);
                tMulR = fcPz * tRn.get(tN);
                tOpt = cnlmPzUpdate.row(tN).operation();
                tOpt.mplus2this(tY, tMulL);
                tOpt.mplus2this(tYPz, tMul);
                tOpt.mplus2this(tY, tMulR);
                if (mTypeNum > 1) {
                    tOpt = cnlmPzUpdate.row(tN+mNMax+1).operation();
                    tOpt.mplus2this(tY, wt*tMulL);
                    tOpt.mplus2this(tYPz, wt*tMul);
                    tOpt.mplus2this(tY, wt*tMulR);
                }
            }
        });
        if (aCalCross) {
            final int tNN_ = tNN[0];
            // 如果计算了 cross 的，则需要在这里手动累加一下 cnlm
            for (int i = 0; i < tNN_; ++i) {
                cnlmPx.plus2this(bufCnlmPxAll(i, false));
                cnlmPy.plus2this(bufCnlmPyAll(i, false));
                cnlmPz.plus2this(bufCnlmPzAll(i, false));
            }
            // 在这里初始化 cross 的 FingerPrint 偏导
            for (int i = 0; i < tNN_; ++i) {
                rFingerPrintPxCross.add(MatrixCache.getMatRow(tSizeN, mLMax+1));
                rFingerPrintPyCross.add(MatrixCache.getMatRow(tSizeN, mLMax+1));
                rFingerPrintPzCross.add(MatrixCache.getMatRow(tSizeN, mLMax+1));
            }
            // 基组对于近邻原子坐标的偏导值和这里直接计算结果差一个负号；
            // 由于实际计算力时需要近邻的原本基组值来反向传播，因此这里结果实际会传递给近邻用于累加，所以需要的就是 基组对于近邻原子坐标的偏导值
            for (int i = 0; i < tNN_; ++i) {
                bufCnlmPxAll(i, false).negative2this();
                bufCnlmPyAll(i, false).negative2this();
                bufCnlmPzAll(i, false).negative2this();
            }
        }
        // 做标量积消去 m 项，得到此原子的 FP
        ShiftVector subCilm = new ShiftVector(cnlm.internalDataSize(), cnlm.internalDataShift(), cnlm.internalData());
        ShiftVector subCilmPx = new ShiftVector(cnlmPx.internalDataSize(), cnlmPx.internalDataShift(), cnlmPx.internalData());
        ShiftVector subCilmPy = new ShiftVector(cnlmPy.internalDataSize(), cnlmPy.internalDataShift(), cnlmPy.internalData());
        ShiftVector subCilmPz = new ShiftVector(cnlmPz.internalDataSize(), cnlmPz.internalDataShift(), cnlmPz.internalData());
        IVectorOperation subCilmOpt = subCilm.operation();
        for (int tN = 0; tN < tSizeN; ++tN) {
            int tShift = tN * cnlm.columnNumber();
            for (int tL = 0; tL <= mLMax; ++tL) {
                // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
                int tStart = tL*tL;
                int tLen = tL+tL+1;
                double tMul = 4.0*PI/(double)tLen;
                double tMul2 = tMul+tMul;
                subCilm.setSize(tLen).setShift(tShift+tStart);
                subCilmPx.setSize(tLen).setShift(tShift+tStart);
                subCilmPy.setSize(tLen).setShift(tShift+tStart);
                subCilmPz.setSize(tLen).setShift(tShift+tStart);
                if (aCalBasis) rFingerPrint.set(tN, tL, tMul * subCilmOpt.dot());
                rFingerPrintPx.set(tN, tL, tMul2 * subCilmOpt.dot(subCilmPx));
                rFingerPrintPy.set(tN, tL, tMul2 * subCilmOpt.dot(subCilmPy));
                rFingerPrintPz.set(tN, tL, tMul2 * subCilmOpt.dot(subCilmPz));
            }
        }
        // 如果计算 cross，则需要这样设置 cross 的 FingerPrint 偏导
        if (aCalCross) {
            final int tNN_ = tNN[0];
            for (int i = 0; i < tNN_; ++i) {
                RowMatrix cnlmPxAllI = bufCnlmPxAll(i, false), cnlmPyAllI = bufCnlmPyAll(i, false), cnlmPzAllI = bufCnlmPzAll(i, false);
                ShiftVector subCilmPxAllI = new ShiftVector(cnlmPxAllI.internalDataSize(), cnlmPxAllI.internalDataShift(), cnlmPxAllI.internalData());
                ShiftVector subCilmPyAllI = new ShiftVector(cnlmPyAllI.internalDataSize(), cnlmPyAllI.internalDataShift(), cnlmPyAllI.internalData());
                ShiftVector subCilmPzAllI = new ShiftVector(cnlmPzAllI.internalDataSize(), cnlmPzAllI.internalDataShift(), cnlmPzAllI.internalData());
                RowMatrix tFingerPrintPxCrossI = rFingerPrintPxCross.get(i);
                RowMatrix tFingerPrintPyCrossI = rFingerPrintPyCross.get(i);
                RowMatrix tFingerPrintPzCrossI = rFingerPrintPzCross.get(i);
                for (int tN = 0; tN < tSizeN; ++tN) {
                    int tShift = tN * cnlm.columnNumber();
                    for (int tL = 0; tL <= mLMax; ++tL) {
                        int tStart = tL*tL;
                        int tLen = tL+tL+1;
                        double tMul = 4.0*PI/(double)tLen;
                        double tMul2 = tMul+tMul;
                        subCilm.setSize(tLen).setShift(tShift+tStart);
                        subCilmPxAllI.setSize(tLen).setShift(tShift+tStart);
                        subCilmPyAllI.setSize(tLen).setShift(tShift+tStart);
                        subCilmPzAllI.setSize(tLen).setShift(tShift+tStart);
                        tFingerPrintPxCrossI.set(tN, tL, tMul2 * subCilmOpt.dot(subCilmPxAllI));
                        tFingerPrintPyCrossI.set(tN, tL, tMul2 * subCilmOpt.dot(subCilmPyAllI));
                        tFingerPrintPzCrossI.set(tN, tL, tMul2 * subCilmOpt.dot(subCilmPzAllI));
                    }
                }
            }
        }
        
        List<RowMatrix> rOut = Lists.newArrayList(rFingerPrint, rFingerPrintPx, rFingerPrintPy, rFingerPrintPz);
        if (aCalCross) {
            rOut.addAll(rFingerPrintPxCross);
            rOut.addAll(rFingerPrintPyCross);
            rOut.addAll(rFingerPrintPzCross);
        }
        return rOut;
    }
}
