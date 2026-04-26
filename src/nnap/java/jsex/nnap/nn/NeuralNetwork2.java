package jsex.nnap.nn;

import jse.code.io.ISavable;
import jse.cptr.IDoubleOrFloatCPointer;
import jse.math.vector.Vector;
import jsex.nnap.basis.Basis2;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

/**
 * 通用的 nnap 神经网络部分实现
 * <p>
 * 现在这个类只用来进行通用的参数初始化操作，不用于外部使用
 *
 * @author liqa
 */
@ApiStatus.Experimental @ApiStatus.Internal
public abstract class NeuralNetwork2 implements ISavable {
    /** 提供直接加载完整神经网络的通用接口 */
    @SuppressWarnings("rawtypes")
    public static NeuralNetwork2[] load(Basis2[] aBasis, List aData) {
        final int tNumTypes = aData.size();
        if (aBasis.length!=tNumTypes) throw new IllegalArgumentException("Input size of basis and nn mismatch");
        NeuralNetwork2[] rNN = new NeuralNetwork2[tNumTypes];
        for (int i = 0; i < tNumTypes; ++i) {
            Basis2 tBasis = aBasis[i];
//            // mirror 情况延迟初始化
//            if (tBasis instanceof MirrorBasis) continue;
            Map tModelMap = (Map)aData.get(i);
            Object tModelType = tModelMap.get("type");
            if (tModelType == null) {
                tModelType = "feed_forward";
            }
            switch(tModelType.toString()) {
            case "shared_feed_forward": {
                // share 情况延迟初始化
                break;
            }
            case "feed_forward": {
                rNN[i] = FeedForward2.load(tBasis.size(), tModelMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported nn type: " + tModelType);
            }}
        }
        for (int i = 0; i < tNumTypes; ++i) {
            Basis2 tBasis = aBasis[i];
            Map tModelMap = (Map)aData.get(i);
//            // mirror 情况不得有 nn
//            if (tBasis instanceof MirrorBasis) {
//                if (tModelMap != null) throw new IllegalArgumentException("nn data in mirror ModelInfo MUST be empty");
//                rNN[i] = rNN[((MirrorBasis)tBasis).mirrorType()-1];
//                continue;
//            }
            Object tModelType = tModelMap.get("type");
            if (tModelType.equals("shared_feed_forward")) {
                rNN[i] = SharedFeedForward2.load(tBasis.size(), rNN, tModelMap);
            }
        }
        return rNN;
    }
    
    /**
     * 挂载内部的参量到一个指针，从而自动同步修改
     * <p>
     * 输入指针包装是临时的，因此需要内部拷贝或等价形式
     */
    public abstract void mountCptrParameter(IDoubleOrFloatCPointer aPtr);
    /** @return 内部参数的长度 */
    public abstract int cptrParameterSize();
    
    /**
     * 挂载内部的可拟合参数到一个数组，从而自动同步修改
     */
    public abstract void mountParameter(Vector aVec);
    /** @return 内部可拟合参数的长度 */
    public abstract int parameterSize();
    
    /** 随机初始化内部存在的可拟合参数 */
    public abstract void initParameters();
    /** 更新同步内部参数 */
    public abstract void updateParameters();
    
    /** 告知此网络需要缓存参数梯度 */
    public abstract void requireGrad(int aNumThreads);
    /**
     * 挂载内部的参量的梯度到一个指针，从而自动同步修改
     * <p>
     * 输入指针包装是临时的，因此需要内部拷贝或等价形式
     */
    public abstract void mountGradCptrParameter(int aThreadID, IDoubleOrFloatCPointer aPtr);
    /**
     * 挂载内部的可拟合参数的梯度到一个数组，从而自动同步修改
     */
    public abstract void mountGradParameter(Vector aVec);
    /**
     * 反向传播参数梯度到可拟合参数
     * <p>
     * 注意输入指针包装是临时的，因此可能需要内部拷贝或等价形式
     */
    public abstract void backwardParameter();
    
    /** @return 前向传播中需要的缓存大小 */
    public abstract int forwardCacheSize();
    
    /** 更新内部 code gen 的 map，将参数编码进 jit */
    public abstract void updateGenMap(Map<String, Object> rGenMap, int aGenIdx);
    /** 本基组是否和输入的基组有着相同的 code gen map，相同时则会简单合并简化最终的 jit 代码 */
    public abstract boolean hasSameGenMap(NeuralNetwork2 aBasis);
}
