package jse.optim;

import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;

/**
 * 限制内存的拟牛顿优化器
 * <a href="https://en.wikipedia.org/wiki/Limited-memory_BFGS">LBFGS</a>
 * 实现：
 * <pre> {@code
 * x_0
 * g_0 <- df/dx_0
 * r <- g_0
 * for-each k:
 *   x_(k+1) <- x_k - r
 *   g_(k+1) <- df/dx_(k+1)
 *   s_k <- x_(k+1) - x_k
 *   y_k <- g_(k+1) - g_k
 *   rho_k <- 1 / (y_k · s_k)
 *   q <- g_(k+1)
 *   for i in k..>(k-m):
 *     a_i <- rho_i (s_i · q)
 *     q <- q - a_i y_i
 *   r <- (s_k · y_k) / (y_k · y_k) q
 *   for i in (k-m)<..k:
 *     b_i <- rho_i (y_i · r)
 *     r <- r + s_i (a_i - b_i)
 * } </pre>
 * 这里实际使用了 Nocedal & Wright 的 Numerical Optimization
 * 中的 7.4 的实现
 *
 * @see IOptimizer
 * @author liqa
 */
public class LBFGS extends AbstractOptimizer {
    public final static double UPDATE_EPS = 1e-10;
    
    protected Vector mLastPara = null, mLastGrad = null;
    protected Vector[] mMemS, mMemY;
    protected double[] mMemRho;
    protected double[] mAlpha;
    protected int mMemorySize, mUsedMemorySize = 0;
    protected boolean mIsFirst = true;
    protected double mEta = 0.01, mEtaIncrement = 0.01;
    
    /**
     * 创建一个 LBFGS 优化器
     * @param aM 使用的缓存长度，默认为 {@code 20}
     */
    public LBFGS(int aM) {
        mMemorySize = aM;
        mMemS = new Vector[aM];
        mMemY = new Vector[aM];
        mMemRho = new double[aM];
        mAlpha = new double[aM];
    }
    /**
     * 创建一个 LBFGS 优化器
     */
    public LBFGS() {this(20);}
    
    /**
     * {@inheritDoc}
     * @param aLearningRate {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public LBFGS setLearningRate(double aLearningRate) {
        mEta = mEtaIncrement = aLearningRate;
        return this;
    }
    
    /**
     * {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public LBFGS setParameter(IVector aParameter) {
        super.setParameter(aParameter);
        if (aParameter != null) {
            mLastPara = Vectors.zeros(aParameter.size());
            mLastGrad = Vectors.zeros(aParameter.size());
            for (int m = 0; m < mMemorySize; ++m) {
                mMemS[m] = Vectors.zeros(aParameter.size());
                mMemY[m] = Vectors.zeros(aParameter.size());
            }
        }
        return this;
    }
    /**
     * 对于 LBFGS，线搜索的 wolfe 条件默认设为 {@code 0.9}
     */
    @Override public LBFGS setLineSearch() {
        return (LBFGS)setLineSearchStrongWolfe(0.0001, 0.9, 10);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override public void reset() {
        mUsedMemorySize = 0;
        mIsFirst = true;
    }
    
    /**
     * {@inheritDoc}
     * @param aStep {@inheritDoc}
     * @param aParameter {@inheritDoc}
     * @param rParameterStep {@inheritDoc}
     */
    @Override protected void calStep(int aStep, IVector aParameter, Vector rParameterStep) {
        IVector tGrad = grad();
        if (mIsFirst) {
            mIsFirst = false;
            // 只在无线搜索时第一步会减小步长来确保收敛
            tGrad.operation().multiply2dest(mLineSearch ? -1.0 : -mEta, rParameterStep);
            mLastPara.fill(aParameter);
            mLastGrad.fill(tGrad);
            return;
        }
        // 优先更新步长
        mLastPara.operation().lminus2this(aParameter);
        mLastGrad.operation().lminus2this(tGrad);
        double tDotSY = mLastPara.operation().dot(mLastGrad);
        double tDotYY = mLastGrad.operation().dot(mLastGrad);
        // 判断一次大小用于确保数值稳定
        if (tDotSY > UPDATE_EPS) {
            // 容量满了简单移除最开头的
            if (mUsedMemorySize == mMemorySize) {
                --mUsedMemorySize;
                // 这些引用值需要保留
                Vector tS0 = mMemS[0];
                Vector tY0 = mMemY[0];
                for (int m = 0; m < mUsedMemorySize; ++m) {
                    mMemRho[m] = mMemRho[m+1];
                    mMemS[m] = mMemS[m+1];
                    mMemY[m] = mMemY[m+1];
                }
                // 这些引用值需要保留
                mMemS[mUsedMemorySize] = tS0;
                mMemY[mUsedMemorySize] = tY0;
            }
            mMemRho[mUsedMemorySize] = 1.0 / tDotSY;
            mMemS[mUsedMemorySize].fill(mLastPara);
            mMemY[mUsedMemorySize].fill(mLastGrad);
            ++mUsedMemorySize;
            mLastPara.fill(aParameter);
            mLastGrad.fill(tGrad);
        } else {
            // 否则需要回滚缓存
            mLastPara.operation().lminus2this(aParameter);
            mLastGrad.operation().lminus2this(tGrad);
            // dot 值后续还需要使用
            if (mUsedMemorySize > 0) {
                tDotSY = mMemS[mUsedMemorySize-1].operation().dot(mMemY[mUsedMemorySize-1]);
                tDotYY = mMemY[mUsedMemorySize-1].operation().dot(mMemY[mUsedMemorySize-1]);
            }
        }
        // 特殊处理没有缓存的情况
        if (mUsedMemorySize == 0) {
            // 只在无线搜索时第一步会减小步长来确保收敛
            tGrad.operation().multiply2dest(mLineSearch ? -1.0 : -mEta, rParameterStep);
            return;
        }
        // 开始两轮循环的 LBFGS 过程
        rParameterStep.fill(tGrad);
        for (int m = mUsedMemorySize-1; m >= 0; --m) {
            mAlpha[m] = mMemRho[m] * mMemS[m].operation().dot(rParameterStep);
            rParameterStep.operation().mplus2this(mMemY[m], -mAlpha[m]);
        }
        rParameterStep.multiply2this(tDotSY / tDotYY);
        for (int m = 0; m < mUsedMemorySize; ++m) {
            double tBeta = mMemRho[m] * mMemY[m].operation().dot(rParameterStep);
            rParameterStep.operation().mplus2this(mMemS[m], mAlpha[m] - tBeta);
        }
        if (mLineSearch) {
            rParameterStep.negative2this();
        } else {
            // 无线搜索情况下现在会增加学习率限制来确保稳定性
            double tEta = mEta + mUsedMemorySize*mUsedMemorySize*mEtaIncrement;
            if (tEta > 1.0) tEta = 1.0;
            rParameterStep.multiply2this(-tEta);
        }
    }
}
