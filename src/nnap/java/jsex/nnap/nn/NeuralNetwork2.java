package jsex.nnap.nn;

import jse.code.io.ISavable;
import jse.math.vector.IVector;
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
        final int tTypeNum = aData.size();
        if (aBasis.length!=tTypeNum) throw new IllegalArgumentException("Input size of basis and nn mismatch");
        NeuralNetwork2[] rNN = new NeuralNetwork2[tTypeNum];
        for (int i = 0; i < tTypeNum; ++i) {
//            Basis2 tBasis = aBasis[i];
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
                rNN[i] = FeedForward2.load(tModelMap);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported nn type: " + tModelType);
            }}
        }
        for (int i = 0; i < tTypeNum; ++i) {
//            Basis2 tBasis = aBasis[i];
            Map tModelMap = (Map)aData.get(i);
//            // mirror 情况不得有 nn
//            if (tBasis instanceof MirrorBasis) {
//                if (tModelMap != null) throw new IllegalArgumentException("nn data in mirror ModelInfo MUST be empty");
//                rNN[i] = rNN[((MirrorBasis)tBasis).mirrorType()-1];
//                continue;
//            }
            Object tModelType = tModelMap.get("type");
            if (tModelType.equals("shared_feed_forward")) {
                rNN[i] = SharedFeedForward2.load_(rNN, tModelMap);
            }
        }
        return rNN;
    }
    
    /** 随机初始化内部可能存在的可拟合参数 */
    public void initParameters() {/**/}
    
    /** @return 内部可拟合参数组成的向量 */
    public abstract IVector parameters();
    /** @return 内部可拟合参数的长度 */
    public abstract int parameterSize();
    
    
    /** 更新内部 code gen 的 map，将参数编码进 jit */
    public abstract void updateGenMap(Map<String, Object> rGenMap, int aGenIdx);
    /** 本基组是否和输入的基组有着相同的 code gen map，相同时则会简单合并简化最终的 jit 代码 */
    public abstract boolean hasSameGenMap(NeuralNetwork2 aBasis);
    
    /** @return 前向传播中需要的缓存大小 */
    public abstract int forwardCacheSize();
    /** @return 反向传播中需要的缓存大小 */
    public abstract int backwardCacheSize();
}
