package jsex.nnap.basis;

import jse.code.io.ISavable;
import jse.math.vector.IVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static jse.code.CS.ZL_VEC;

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
            case "spherical_chebyshev": {
                rBasis[i] = SphericalChebyshev2.load(i+1, tTypeNum, tBasisMap);
                break;
            }
            case "chebyshev": {
                rBasis[i] = Chebyshev2.load(i+1, tTypeNum, tBasisMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
            }}
        }
        return rBasis;
    }
    
    /** 随机初始化内部可能存在的可拟合参数 */
    public void initParameters() {/**/}
    
    /** @return 内部超参数组成的向量 */
    public IVector hyperParameters() {return ZL_VEC;}
    /** @return 内部超参数的长度 */
    public int hyperParameterSize() {return 0;}
    
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
    
    /** @return 基组的种类数目 */
    public int atomTypeNumber() {return 1;}
    /** @return 本基组的种类编号 */
    public int thisType() {return 1;}
    
    /** 更新内部 code gen 的 map，将参数编码进 jit */
    public abstract void updateGenMap(Map<String, Object> rGenMap);
    /** 本基组是否和输入的基组有着相同的 code gen map，相同时则会简单合并简化最终的 jit 代码 */
    public abstract boolean hasSameGenMap(Object aBasis);
    
    /** @return 前向传播中需要的缓存大小 */
    public abstract int forwardCacheSize(int aNN, boolean aFullCache);
    /** @return 反向传播中需要的缓存大小 */
    public abstract int backwardCacheSize(int aNN, boolean aFullCache);
}
