package jsex.nnap.basis;

import jse.code.io.ISavable;
import jse.math.vector.IVector;

import java.util.Map;

/**
 * 基于其他元素基组的共享基组，和 {@link MirrorBasis}
 * 的区别主要是不进行种类映射，并且依旧保持神经网络的独立性。
 * 和单纯拷贝基组的主要区别是可拟合参数共享。
 * <p>
 * 现在是常用的其他种类默认基组
 * @author liqa
 */
public class SharedBasis2 implements ISavable {
    
    private final int mThisType;
    private final int mSharedType;
    public SharedBasis2(int aThisType, int aSharedType) {
        mThisType = aThisType;
        mSharedType = aSharedType;
    }
    /** @return {@inheritDoc} */
    public int thisType() {return mThisType;}
    public int sharedType() {return mSharedType;}
    
    public void initParameters() {}
    public IVector parameters() {return null;}
    public boolean hasParameters() {return false;}
    public int parameterSize() {return 0;}
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "share");
        rSaveTo.put("share", mSharedType);
    }
}
