package jsex.nnap.basis;

import jse.code.io.ISavable;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.IDataShell;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

/**
 * 通用的 nnap 基组/描述符实现
 * <p>
 * 现在这个类只用来进行通用的参数初始化操作，不用于外部使用
 *
 * @author liqa
 */
@ApiStatus.Experimental @ApiStatus.Internal
public abstract class Basis2 implements ISavable {
    /** 提供直接加载完整基组的通用接口 */
    @SuppressWarnings("rawtypes")
    public static Basis2[] load(List aData) {
        final int tTypeNum = aData.size();
        Basis2[] rBasis = new Basis2[tTypeNum];
        for (int i = 0; i < tTypeNum; ++i) {
            Map tBasisMap = (Map)aData.get(i);
            Object tBasisType = tBasisMap.get("type");
            if (tBasisType == null) {
                tBasisType = "spherical_chebyshev";
            }
            switch(tBasisType.toString()) {
            case "share": case "shared_basis": {
                break; // share 情况延迟初始化
            }
            case "spherical_chebyshev": {
                rBasis[i] = new MergedBasis2(SphericalChebyshev2.load(tTypeNum, tBasisMap));
                break;
            }
            case "chebyshev": {
                rBasis[i] = new MergedBasis2(Chebyshev2.load(tTypeNum, tBasisMap));
                break;
            }
            case "merge": {
                rBasis[i] = MergedBasis2.load(tTypeNum, tBasisMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
            }}
        }
        for (int i = 0; i < tTypeNum; ++i) {
            Map tBasisMap = (Map)aData.get(i);
            Object tBasisType = tBasisMap.get("type");
            switch(tBasisType.toString()) {
            case "share": case "shared_basis": {
                rBasis[i] = SharedBasis2.load(rBasis, tBasisMap);
                break;
            }
            default: {
                continue;
            }}
        }
        return rBasis;
    }
    
    
    /**
     * 挂载内部的参量到一个指针，从而自动同步修改
     * <p>
     * 输入指针包装是临时的，因此需要内部拷贝或等价形式
     */
    public void mountCptrParameter(IDoubleOrFloatCPointer aPtr) {}
    /**
     * 挂载内部的参量的梯度到一个指针，从而自动同步修改
     * <p>
     * 输入指针包装是临时的，因此需要内部拷贝或等价形式
     */
    public void mountGradCptrParameter(IDoubleOrFloatCPointer aPtr) {}
    /** @return 内部参数的长度 */
    public int cptrParameterSize() {return 0;}
    
    /**
     * 挂载内部超参数到一个指针，这里认为超参数不会改变，因此实现中会直接设置数值
     * <p>
     * 输入指针包装是临时的，因此需要内部拷贝或等价形式
     */
    public void mountCptrHyperParameter(IDoubleOrFloatCPointer aPtr) {}
    /** @return 内部超参数的长度 */
    public int cptrHyperParameterSize() {return 0;}
    
    /** 随机初始化可能存在的可拟合参数 */
    public void initParameters() {/**/}
    
    /**
     * 挂载内部的可拟合参数到一个数组，从而自动同步修改
     */
    public void mountParameter(IDataShell<double[]> aData) {}
    /**
     * 挂载内部的可拟合参数的梯度到一个数组，从而自动同步修改
     */
    public void mountGradParameter(IDataShell<double[]> aData) {}
    /** @return 内部可能存在的可拟合参数的长度 */
    public int parameterSize() {return 0;}
    
    /** 更新同步内部参数 */
    public void updateParameters() {}
    /**
     * 反向传播参数梯度到可拟合参数
     * <p>
     * 注意输入指针包装是临时的，因此可能需要内部拷贝或等价形式
     */
    public void backwardParameter() {}
    
    /** @return 基组需要的近邻截断半径 */
    public abstract double rcut();
    /** @return 基组的长度 */
    public abstract int size();
    
    /** 更新内部 code gen 的 map，将参数编码进 jit */
    public abstract void updateGenMap(Map<String, Object> rGenMap, int aGenIdx);
    /** 本基组是否和输入的基组有着相同的 code gen map，相同时则会简单合并简化最终的 jit 代码 */
    public abstract boolean hasSameGenMap(Basis2 aBasis);
    
    /** @return 前向传播中需要的缓存大小 */
    public abstract int forwardCacheSize(int aNumNei);
    /** @return 反向传播中需要的缓存大小 */
    public abstract int backwardCacheSize(int aNumNei);
}
