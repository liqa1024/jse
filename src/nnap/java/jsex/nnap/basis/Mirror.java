package jsex.nnap.basis;

import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IntArrayVector;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 基于其他元素基组的一个镜像基组，其对于自身元素和对应的镜像元素种类会进行交换
 * <p>
 * 目前主要用于实现 ising 模型
 * @author liqa
 */
public class Mirror extends Basis {
    
    private final Basis mMirrorBasis;
    private final int mMirrorType, mThisType;
    public Mirror(Basis aMirrorBasis, int aMirrorType, int aThisType) {
        if (aMirrorBasis instanceof Mirror) throw new IllegalArgumentException("MirrorBasis MUST NOT be Mirror");
        mMirrorBasis = aMirrorBasis;
        mMirrorType = aMirrorType;
        mThisType = aThisType;
    }
    public Basis mirrorBasis() {return mMirrorBasis;}
    public int mirrorType() {return mMirrorType;}
    public int thisType() {return mThisType;}
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "mirror");
        rSaveTo.put("mirror", mMirrorType);
    }
    @SuppressWarnings("rawtypes")
    public static Mirror load(Basis aMirrorBasis, int aThisType, Map aMap) {
        Object tMirror = aMap.get("mirror");
        if (tMirror == null) throw new IllegalArgumentException("Key `mirror` required for mirror load");
        int tMirrorType = ((Number)tMirror).intValue();
        return new Mirror(aMirrorBasis, tMirrorType, aThisType);
    }
    
    @Override public double rcut() {return mMirrorBasis.rcut();}
    @Override public int size() {return mMirrorBasis.size();}
    @Override public int atomTypeNumber() {return mMirrorBasis.atomTypeNumber();}
    @Override public boolean hasSymbol() {return mMirrorBasis.hasSymbol();}
    @Override public String symbol(int aType) {return mMirrorBasis.symbol(aType);}
    
    @Override protected void shutdown_() {
        mMirrorBasis.shutdown();
    }
    
    private boolean mMirrorNlTypeValid = false;
    private final IntList mMirrorNlType = new IntList(16);
    private void buildNlType_(IntList aNlType) {
        mMirrorNlType.clear();
        mMirrorNlType.ensureCapacity(aNlType.size());
        aNlType.forEach(type -> {
            if (type == mThisType) mMirrorNlType.add(mMirrorType);
            else if (type == mMirrorType) mMirrorNlType.add(mThisType);
            else mMirrorNlType.add(type);
        });
        mMirrorNlTypeValid = true;
    }
    
    @Override
    protected void eval_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, @Nullable IntArrayVector rFpGradNlSize, boolean aBufferNl) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        if (mMirrorNlTypeValid) throw new IllegalStateException();
        buildNlType_(aNlType);
        mMirrorBasis.eval_(aNlDx, aNlDy, aNlDz, mMirrorNlType, rFp, rFpGradNlSize, aBufferNl);
        if (!aBufferNl) mMirrorNlTypeValid = false;
    }
    @Override
    protected void evalGrad_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, IntArrayVector aFpGradNlSize, IntArrayVector rFpGradNlIndex, IntArrayVector rFpGradFpIndex, DoubleArrayVector rFpPx, DoubleArrayVector rFpPy, DoubleArrayVector rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 由于 evalGrad_ 总是在 eval_ 之后调用的，此时不需要重新构造 mMirrorNlType
        if (!mMirrorNlTypeValid) throw new IllegalStateException();
        mMirrorBasis.evalGrad_(aNlDx, aNlDy, aNlDz, mMirrorNlType, aFpGradNlSize, rFpGradNlIndex, rFpGradFpIndex, rFpPx, rFpPy, rFpPz);
        mMirrorNlTypeValid = false;
    }
    @Override
    protected void evalGradAndForceDot_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 由于 evalGrad_ 总是在 eval_ 之后调用的，此时不需要重新构造 mMirrorNlType
        if (!mMirrorNlTypeValid) throw new IllegalStateException();
        mMirrorBasis.evalGradAndForceDot_(aNlDx, aNlDy, aNlDz, mMirrorNlType, aNNGrad, rFx, rFy, rFz);
        mMirrorNlTypeValid = false;
    }
    
    @Override @Deprecated
    protected void evalGrad_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 由于 evalGrad_ 总是在 eval_ 之后调用的，此时不需要重新构造 mMirrorNlType
        if (!mMirrorNlTypeValid) throw new IllegalStateException();
        mMirrorBasis.evalGrad_(aNlDx, aNlDy, aNlDz, mMirrorNlType, rFpPx, rFpPy, rFpPz);
        mMirrorNlTypeValid = false;
    }
}
