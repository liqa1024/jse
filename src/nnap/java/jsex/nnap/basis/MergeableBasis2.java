package jsex.nnap.basis;

import jse.code.io.ISavable;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class MergeableBasis2 implements ISavable {
    /** 随机初始化内部可能存在的可拟合参数 */
    public void initParameters() {/**/}
    
    /** @return 内部超参数组成的向量 */
    public IVector hyperParameters() {
        return new RefVector() {
            @Override public double get(int aIdx) {
                if (aIdx==0) return rcut();
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {return 1;}
        };
    }
    /** @return 内部超参数的长度 */
    public int hyperParameterSize() {
        return 1;
    }
    
    /** @return 内部可能存在的可拟合参数组成的向量 */
    public @Nullable IVector parameters() {return null;}
    /** @return 内部可能存在的可拟合参数的长度 */
    public int parameterSize() {return 0;}
    /** @return 是否确实存在可拟合的参数 */
    public boolean hasParameters() {return false;}
    
    /** @return 基组需要的近邻截断半径 */
    public abstract double rcut();
    /** @return 基组的长度 */
    public abstract int size();
    
    /** 更新内部 code gen 的 map，将参数编码进 jit */
    public abstract void updateGenMap(Map<String, Object> rGenMap, int aGenIdxType, int aGenIdxMerge);
    /** 本基组是否和输入的基组有着相同的 code gen map，相同时则会简单合并简化最终的 jit 代码 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean hasSameGenMap(MergeableBasis2 aBasis);
    
    /** @return 前向传播中需要的缓存大小 */
    public abstract int forwardCacheSize(int aNN, boolean aFullCache);
    /** @return 反向传播中需要的缓存大小 */
    public abstract int backwardCacheSize(int aNN, boolean aFullCache);
}
