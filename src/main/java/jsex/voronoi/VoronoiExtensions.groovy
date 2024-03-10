package jsex.voronoi

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import jse.atom.MonatomicParameterCalculator
import jse.code.collection.AbstractRandomAccessList 


/**
 * 采用这种方式将 voronoi 方法注入到 MPC 中，
 * 这种做法可以降低代码的耦合，并且保证 jse 不会依赖 jsex
 * @author liqa
 */
@CompileStatic
class VoronoiExtensions {
    interface ICalculator extends List<VoronoiBuilder.IVertex>, RandomAccess {
        ICalculator setNoWarning(boolean aNoWarning)
        ICalculator setNoWarning()
        ICalculator setAreaThreshold(double aAreaThreshold)
        ICalculator setLengthThreshold(double aLengthThreshold)
        ICalculator setAreaThresholdAbs(double aAreaThresholdAbs)
        ICalculator setLengthThresholdAbs(double aLengthThresholdAbs)
        ICalculator setIndexLength(int aIndexLength)
    }
    private static abstract class AbstractCalculator extends AbstractRandomAccessList<VoronoiBuilder.IVertex> implements ICalculator {
        final VoronoiBuilder mBuilder
        AbstractCalculator(VoronoiBuilder aBuilder) {mBuilder = aBuilder}
        @Override final AbstractCalculator setNoWarning(boolean aNoWarning) {mBuilder.setNoWarning(aNoWarning); return this}
        @Override final AbstractCalculator setNoWarning() {mBuilder.setNoWarning(); return this}
        @Override final AbstractCalculator setAreaThreshold(double aAreaThreshold) {mBuilder.setAreaThreshold(aAreaThreshold); return this}
        @Override final AbstractCalculator setLengthThreshold(double aLengthThreshold) {mBuilder.setLengthThreshold(aLengthThreshold); return this}
        @Override final AbstractCalculator setAreaThresholdAbs(double aAreaThresholdAbs) {mBuilder.setAreaThresholdAbs(aAreaThresholdAbs); return this}
        @Override final AbstractCalculator setLengthThresholdAbs(double aLengthThresholdAbs) {mBuilder.setLengthThresholdAbs(aLengthThresholdAbs); return this}
        @Override final AbstractCalculator setIndexLength(int aIndexLength) {mBuilder.setIndexLength(aIndexLength); return this}
    }
    
    
    /**
     * 计算 Voronoi 图并获取各种参数，
     * 由于内部实现是串行的，因此此方法不受线程数影响
     * <p>
     * 简单使用额外的镜像原子的方式处理周期边界条件，
     * 因此可能会出现不准确的情况，此时需要增加 aRCutOff
     * <p>
     * References:
     * <a href="https://ieeexplore.ieee.org/document/4276112">
     * Computing the 3D Voronoi Diagram Robustly: An Easy Explanation </a>
     * and
     * <a href="https://github.com/Hellblazer/Voronoi-3D">
     * Hellblazer/Voronoi-3D </a>
     * @author liqa
     * @param aRCutOff 外围周期边界增加的镜像粒子的半径，默认为 3 倍单位长度
     * @param aNoWarning 是否关闭错误警告，默认为 false
     * @param aIndexLength voronoi 参数的存储长度，默认为 9
     * @param aAreaThreshold 过小面积的阈值（相对值），默认为 0.0（不处理）
     * @param aLengthThreshold 过小长度的阈值（相对值），默认为 0.0（不处理）
     * @return Voronoi 分析的参数
     */
    @CompileDynamic @SuppressWarnings('GroovyAccessibility')
    static ICalculator calVoronoi(MonatomicParameterCalculator self, double aRCutOff, boolean aNoWarning, int aIndexLength, double aAreaThreshold, double aLengthThreshold) {
        final VoronoiBuilder rBuilder = new VoronoiBuilder().setNoWarning(aNoWarning).setIndexLength(aIndexLength).setAreaThreshold(aAreaThreshold).setLengthThreshold(aLengthThreshold)
        // 先增加内部原本的粒子，根据 cell 的顺序添加可以加速 voronoi 的构造
        final int[] idx2voronoi = new int[self.atomNumber()]
        self.mNL.forEachCell(aRCutOff) {idx ->
            idx2voronoi[idx] = rBuilder.sizeVertex()
            // 原则上 VoronoiBuilder.insert 内部也会进行一次拷贝避免坐标被意外修改，但是旧版本没有，这样写可以兼顾效率和旧版兼容
            rBuilder.insert(self.mAtomDataXYZ.get(idx, 0), self.mAtomDataXYZ.get(idx, 1), self.mAtomDataXYZ.get(idx, 2))
        }
        // 然后增加一些镜像粒子保证 PBC 下的准确性
        self.mNL.forEachMirrorCell(aRCutOff) {x, y, z, idx ->
            rBuilder.insert(x, y, z)
        }
        // 注意需要进行一次重新排序保证顺序和原子的顺序相同
        return new AbstractCalculator(rBuilder) {
            @Override int size() {return self.atomNumber()}
            @Override VoronoiBuilder.IVertex get(int aIdx) {return mBuilder.getVertex(idx2voronoi[aIdx])}
        }
    }
    static ICalculator calVoronoi(MonatomicParameterCalculator self, double aRCutOff, boolean aNoWarning, int aIndexLength, double aAreaThreshold) {return calVoronoi(self, aRCutOff, aNoWarning, aIndexLength, aAreaThreshold, (double)0.0)}
    static ICalculator calVoronoi(MonatomicParameterCalculator self, double aRCutOff, boolean aNoWarning, int aIndexLength) {return calVoronoi(self, aRCutOff, aNoWarning, aIndexLength, (double)0.0)}
    static ICalculator calVoronoi(MonatomicParameterCalculator self, double aRCutOff, boolean aNoWarning) {return calVoronoi(self, aRCutOff, aNoWarning, 9)}
    static ICalculator calVoronoi(MonatomicParameterCalculator self, double aRCutOff) {return calVoronoi(self, aRCutOff, false)}
    static ICalculator calVoronoi(MonatomicParameterCalculator self) {return calVoronoi(self, (double)(self.unitLen()*3.0))}
}
