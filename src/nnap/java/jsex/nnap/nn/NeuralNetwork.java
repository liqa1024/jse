package jsex.nnap.nn;

import groovy.lang.DeprecationException;
import jse.code.io.ISavable;
import jse.math.vector.DoubleArrayVector;
import jse.parallel.IAutoShutdown;
import jsex.nnap.NNAP;
import jsex.nnap.basis.Basis;
import jsex.nnap.basis.MirrorBasis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用的 nnap 神经网络部分实现
 * <p>
 * 由于内部会缓存中间结果，因此此类一般来说相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
public abstract class NeuralNetwork implements IAutoShutdown, ISavable {
    static {
        // 依赖 nnap
        NNAP.InitHelper.init();
    }
    
    /** 提供直接加载完整神经网络的通用接口 */
    @SuppressWarnings({"rawtypes", "deprecation", "unchecked"})
    public static NeuralNetwork[] load(Basis[] aBasis, List aData, boolean aThrowDeprecate) throws Exception {
        final int tTypeNum = aData.size();
        if (aBasis.length!=tTypeNum) throw new IllegalArgumentException("Input size of basis and nn mismatch");
        NeuralNetwork[] rNN = new NeuralNetwork[tTypeNum];
        for (int i = 0; i < tTypeNum; ++i) {
            Basis tBasis = aBasis[i];
            // mirror 情况延迟初始化
            if (tBasis instanceof MirrorBasis) continue;
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
                rNN[i] = FeedForward.load(tModelMap);
                break;
            }
            case "torch": {
                if (aThrowDeprecate) throw new DeprecationException("torch nn is deprecated");
                //noinspection resource
                rNN[i] = new TorchModel(tBasis.size(), tModelMap.get("model").toString());
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported nn type: " + tModelType);
            }}
        }
        for (int i = 0; i < tTypeNum; ++i) {
            Basis tBasis = aBasis[i];
            Map tModelMap = (Map)aData.get(i);
            // mirror 情况不得有 nn
            if (tBasis instanceof MirrorBasis) {
                if (tModelMap != null) throw new IllegalArgumentException("nn data in mirror ModelInfo MUST be empty");
                rNN[i] = rNN[((MirrorBasis)tBasis).mirrorType()-1].threadSafeRef();
                continue;
            }
            Object tModelType = tModelMap.get("type");
            if (tModelType.equals("shared_feed_forward")) {
                Object tShare = tModelMap.get("share");
                if (tShare == null) throw new IllegalArgumentException("Key `share` required for shared_feed_forward");
                int tSharedType = ((Number)tShare).intValue();
                FeedForward tSharedNN = (FeedForward)rNN[tSharedType-1];
                // 旧版分层 share 情况简单兼容
                Object tSharedFlagsObj = tModelMap.get("shared_flags");
                if (tSharedFlagsObj != null) {
                    if (aThrowDeprecate) throw new DeprecationException("shared in layer nn is deprecated");
                    List tSharedFlags = (List)tSharedFlagsObj;
                    // 简单根据 share 结果组装参数，为了避免深度拷贝的问题，这里直接再次调用 save 简单处理
                    Map aModelMap = new HashMap();
                    tSharedNN.save(aModelMap);
                    int tIdx = 0;
                    for (int j = 0; j < tSharedNN.mHiddenNumber; ++j) {
                        if (!(Boolean)tSharedFlags.get(j)) {
                            ((List)aModelMap.get("hidden_weights")).set(j, ((List)tModelMap.get("hidden_weights")).get(tIdx));
                            ((List)aModelMap.get("hidden_biases")).set(j, ((List)tModelMap.get("hidden_biases")).get(tIdx));
                            ++tIdx;
                        }
                    }
                    if (tSharedFlags.size()==tSharedNN.mHiddenNumber || (!(Boolean)tSharedFlags.get(tSharedNN.mHiddenNumber))) {
                        aModelMap.put("output_weight", tModelMap.get("output_weight"));
                        aModelMap.put("output_bias", tModelMap.get("output_bias"));
                    }
                    rNN[i] = FeedForward.load(aModelMap);
                } else {
                    rNN[i] = SharedFeedForward.load_(tSharedNN.threadSafeRef(), tSharedType, tModelMap);
                }
            }
        }
        return rNN;
    }
    @SuppressWarnings("rawtypes")
    public static NeuralNetwork[] load(Basis[] aBasis, List aData) throws Exception {
        return load(aBasis, aData, false);
    }
    
    public abstract NeuralNetwork threadSafeRef() throws Exception;
    public abstract int inputSize();
    public abstract double eval(DoubleArrayVector aX) throws Exception;
    public abstract double evalGrad(DoubleArrayVector aX, DoubleArrayVector rGradX) throws Exception;
    
    @SuppressWarnings("rawtypes")
    @Override public void save(Map rSaveTo) {throw new UnsupportedOperationException();}
    
    private boolean mDead = false;
    /** @return 此模型是否已经关闭 */
    public final boolean isShutdown() {return mDead;}
    @Override public final void shutdown() {
        if (mDead) return;
        mDead = true;
        shutdown_();
    }
    protected void shutdown_() {/**/}
}
