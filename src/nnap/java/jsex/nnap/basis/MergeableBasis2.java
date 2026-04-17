package jsex.nnap.basis;

import jse.code.io.ISavable;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.IDataShell;

import java.util.Map;

public abstract class MergeableBasis2 implements ISavable {
    /** 随机初始化内部可能存在的可拟合参数 */
    public void initParameters() {/**/}
    
    /**
     * 挂载内部的参量到一个指针，从而自动同步修改
     * <p>
     * 输入指针包装是临时的，因此需要内部拷贝或等价形式
     */
    public void mountParameter(IDoubleOrFloatCPointer aPtr) {}
    /** @return 内部参数的长度 */
    public int parameterSize() {return 0;}
    
    /**
     * 挂载内部超参数到一个指针，这里认为超参数不会改变，因此实现中会直接设置数值
     * <p>
     * 输入指针包装是临时的，因此需要内部拷贝或等价形式
     */
    public void mountHyperParameter(IDoubleOrFloatCPointer aPtr) {
        aPtr.setD(rcut());
    }
    /** @return 内部超参数的长度 */
    public int hyperParameterSize() {
        return 1;
    }
    
    /**
     * 挂载内部的可拟合参数到一个数组，从而自动同步修改
     */
    public void mountFittableParameter(IDataShell<double[]> aData) {}
    /** @return 内部可能存在的可拟合参数的长度 */
    public int fittableParameterSize() {return 0;}
    
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
    public abstract int forwardCacheSize(int aNumNei);
    /** @return 反向传播中需要的缓存大小 */
    public abstract int backwardCacheSize(int aNumNei);
}
