package jsex.nnap;

import jse.atom.MonatomicParameterCalculator;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.math.matrix.RowMatrix;
import jsex.nnap.basis.IBasis;
import jsex.nnap.basis.SphericalChebyshev;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 采用这种方式将 nnap 方法注入到 MPC 中，
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
     * @return 原子指纹矩阵组成的数组，n 为行，l 为列，因此 asVecRow 即为原本定义的基；如果存在超过一个种类则输出行数翻倍
     */
    public static List<RowMatrix> calBasisNNAP(final MonatomicParameterCalculator self, final int aNMax, final int aLMax, final double aRCutOff) {
        if (self.isShutdown()) throw new RuntimeException("This Calculator is dead");
        try (IBasis tBasis = new SphericalChebyshev(self.atomTypeNumber(), aNMax, aLMax, aRCutOff)) {
            final List<RowMatrix> rFingerPrints = NewCollections.nulls(self.atomNumber());
            
            // 获取需要缓存的近邻列表
            final IntList @Nullable[] tNLToBuffer = self.getNLWhichNeedBuffer_(aRCutOff, -1, false);
            
            // 理论上只需要遍历一半从而加速这个过程，但由于实现较麻烦且占用过多内存（所有近邻的 Ylm, Rn, fc 都要存，会随着截断半径增加爆炸增涨），这里不考虑
            self.pool_().parfor(self.atomNumber(), i -> {
                rFingerPrints.set(i, tBasis.eval(dxyzTypeDo -> {
                    self.nl_().forEachNeighbor(i, aRCutOff, false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, self.atomType_().get(idx));
                        // 还是需要顺便统计近邻进行缓存
                        if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
                    });
                }));
            });
            
            return rFingerPrints;
        }
    }
}
