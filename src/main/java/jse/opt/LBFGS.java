package jse.opt;

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
 *   r <- (s_(k-m+1) · y_(k-m+1)) / (y_(k-m+1) · y_(k-m+1)) q
 *   for i in (k-m)<..k:
 *     b_i <- rho_i (y_i · r)
 *     r <- r + s_i (a_i - b_i)
 * } </pre>
 *
 * @see IOptimizer
 * @author liqa
 */
public class LBFGS extends AbstractOptimizer {
    public final static double FIRST_ETA = 0.01, UPDATE_EPS = 1e-10;
    
    protected Vector mLastPara = null, mLastGrad = null;
    protected Vector[] mMemS, mMemY;
    protected double[] mMemRho;
    protected double[] mAlpha;
    protected int mMemorySize, mUsedMemorySize = 0;
    protected boolean mIsFirst = true;
    
    /**
     * 创建一个 LBFGS 优化器
     * @param aM 使用的缓存长度，默认为 {@code 100}
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
    public LBFGS() {this(100);}
    
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
     * {@inheritDoc}
     */
    @Override public void reset() {
        mUsedMemorySize = 0;
        mIsFirst = true;
    }
    
    /**
     * {@inheritDoc}
     * @param aStep {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override protected double calStep(int aStep) {
        double tLoss = eval(true);
        IVector tGrad = grad();
        if (mIsFirst) {
            mIsFirst = false;
            tGrad.operation().multiply2dest(-FIRST_ETA, mParameterStep); // 实际第一步会减小步长来确保收敛
            mLastPara.fill(mParameter);
            mLastGrad.fill(tGrad);
            return tLoss;
        }
        // 优先更新步长
        mLastPara.operation().lminus2this(mParameter);
        mLastGrad.operation().lminus2this(tGrad);
        double tDot = mLastPara.operation().dot(mLastGrad);
        // 判断一次大小用于确保数值稳定
        if (tDot > UPDATE_EPS) {
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
            mMemRho[mUsedMemorySize] = 1.0 / tDot;
            mMemS[mUsedMemorySize].fill(mLastPara);
            mMemY[mUsedMemorySize].fill(mLastGrad);
            ++mUsedMemorySize;
            mLastPara.fill(mParameter);
            mLastGrad.fill(tGrad);
        } else {
            // 否则需要回滚缓存
            mLastPara.operation().lminus2this(mParameter);
            mLastGrad.operation().lminus2this(tGrad);
        }
        // 特殊处理没有缓存的情况
        if (mUsedMemorySize == 0) {
            tGrad.operation().multiply2dest(-FIRST_ETA, mParameterStep); // 实际第一步会减小步长来确保收敛
            return tLoss;
        }
        // 开始两轮循环的 LBFGS 过程
        mParameterStep.fill(tGrad);
        for (int m = mUsedMemorySize-1; m >= 0; --m) {
            mAlpha[m] = mMemRho[m] * mMemS[m].operation().dot(mParameterStep);
            mParameterStep.operation().mplus2this(mMemY[m], -mAlpha[m]);
        }
        mParameterStep.multiply2this(mMemS[0].operation().dot(mMemY[0]) / mMemY[0].operation().dot(mMemY[0]));
        for (int m = 0; m < mUsedMemorySize; ++m) {
            double tBeta = mMemRho[m] * mMemY[m].operation().dot(mParameterStep);
            mParameterStep.operation().mplus2this(mMemS[m], mAlpha[m] - tBeta);
        }
        mParameterStep.negative2this();
        return tLoss;
    }
}
