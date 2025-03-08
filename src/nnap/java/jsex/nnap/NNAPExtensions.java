package jsex.nnap;

import jse.atom.AtomicParameterCalculator;
import jse.code.collection.NewCollections;
import jse.math.vector.Vector;
import jsex.nnap.basis.IBasis;
import jsex.nnap.basis.SphericalChebyshev;

import java.util.List;

/**
 * 采用这种方式将 nnap 方法注入到 APC 中，
 * 这种做法可以降低代码的耦合，并且保证 jse 不会依赖 jsex
 * @author liqa
 */
public class NNAPExtensions {
    /**
     * 通过 {@link SphericalChebyshev} 实现的基组计算
     * @author Su Rui, liqa
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCutOff 截断半径
     * @return 原子描述符向量组成的列表；如果存在超过一个种类则输出长度翻倍
     */
    public static List<Vector> calBasisNNAP(final AtomicParameterCalculator self, final int aNMax, final int aLMax, final double aRCutOff) {
        if (self.isShutdown()) throw new RuntimeException("This Calculator is dead");
        final int tThreadNum = self.threadNumber();
        IBasis[] tBasis = new IBasis[tThreadNum];
        for (int i = 0; i < tThreadNum; ++i) {
            //noinspection resource
            tBasis[i] = new SphericalChebyshev(self.atomTypeNumber(), aNMax, aLMax, aRCutOff);
        }
        try {
            final List<Vector> rFingerPrints = NewCollections.nulls(self.atomNumber());
            // 理论上只需要遍历一半从而加速这个过程，但由于实现较麻烦且占用过多内存（所有近邻的 Ylm, Rn, fc 都要存，会随着截断半径增加爆炸增涨），这里不考虑
            self.pool_().parfor(self.atomNumber(), (i, threadID) -> {
                rFingerPrints.set(i, tBasis[threadID].eval(self, i));
            });
            return rFingerPrints;
        } finally {
            for (int i = 0; i < tThreadNum; ++i) tBasis[i].shutdown();
        }
    }
    
    /**
     * 通过 {@link SphericalChebyshev} 实现的基组结果对于 {@code xyz}
     * 偏微分的计算结果，主要用于力的计算
     * @author Su Rui, liqa
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCutOff 截断半径
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    public static List<List<Vector>> calBasisPartialNNAP(final AtomicParameterCalculator self, final int aNMax, final int aLMax, final double aRCutOff, boolean aCalBasis, boolean aCalCross) {
        if (self.isShutdown()) throw new RuntimeException("This Calculator is dead");
        final int tThreadNum = self.threadNumber();
        IBasis[] tBasis = new IBasis[tThreadNum];
        for (int i = 0; i < tThreadNum; ++i) {
            //noinspection resource
            tBasis[i] = new SphericalChebyshev(self.atomTypeNumber(), aNMax, aLMax, aRCutOff);
        }
        try {
            final List<List<Vector>> rOut = NewCollections.nulls(self.atomNumber());
            // 理论上只需要遍历一半从而加速这个过程，但由于实现较麻烦且占用过多内存（所有近邻的 Ylm, Rn, fc 都要存，会随着截断半径增加爆炸增涨），这里不考虑
            self.pool_().parfor(self.atomNumber(), (i, threadID) -> {
                rOut.set(i, tBasis[threadID].evalPartial(aCalBasis, aCalCross, self, i));
            });
            
            return rOut;
        } finally {
            for (int i = 0; i < tThreadNum; ++i) tBasis[i].shutdown();
        }
    }
    public static List<List<Vector>> calBasisPartialNNAP(final AtomicParameterCalculator self, final int aNMax, final int aLMax, final double aRCutOff, boolean aCalBasis) {return calBasisPartialNNAP(self, aNMax, aLMax, aRCutOff, aCalBasis, false);}
    public static List<List<Vector>> calBasisPartialNNAP(final AtomicParameterCalculator self, final int aNMax, final int aLMax, final double aRCutOff) {return calBasisPartialNNAP(self, aNMax, aLMax, aRCutOff, true);}
}
