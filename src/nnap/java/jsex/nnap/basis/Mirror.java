package jsex.nnap.basis;

import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 基于其他元素基组的一个镜像基组，其对于自身元素和对应的镜像元素种类会进行交换
 * <p>
 * 目前主要用于实现 ising 模型
 * @author liqa
 */
public class Mirror implements IBasis {
    
    private final IBasis mMirrorBasis;
    private final int mMirrorType, mThisType;
    public Mirror(IBasis aMirrorBasis, int aMirrorType, int aThisType) {
        if (aMirrorBasis instanceof Mirror) throw new IllegalArgumentException("MirrorBasis MUST NOT be Mirror");
        mMirrorBasis = aMirrorBasis;
        mMirrorType = aMirrorType;
        mThisType = aThisType;
    }
    public IBasis mirrorBasis() {return mMirrorBasis;}
    public int mirrorType() {return mMirrorType;}
    public int thisType() {return mThisType;}
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "mirror");
        rSaveTo.put("mirror", mMirrorType);
    }
    @SuppressWarnings("rawtypes")
    public static Mirror load(IBasis aMirrorBasis, int aThisType, Map aMap) {
        Object tMirror = aMap.get("mirror");
        if (tMirror == null) throw new IllegalArgumentException("Key `mirror` required for mirror load");
        int tMirrorType = ((Number)tMirror).intValue();
        return new Mirror(aMirrorBasis, tMirrorType, aThisType);
    }
    
    @Override public double rcut() {return mMirrorBasis.rcut();}
    @Override public int size() {return mMirrorBasis.size();}
    @Override public @Nullable List<String> symbols() {return mMirrorBasis.symbols();}
    
    @Override public Vector eval(IDxyzTypeIterable aNL) {
        return mMirrorBasis.eval(dxyzTypeDo -> aNL.forEachDxyzType((dx, dy, dz, type) -> {
            if (type == mThisType) type = mMirrorType;
            else if (type == mMirrorType) type = mThisType;
            dxyzTypeDo.run(dx, dy, dz, type);
        }));
    }
    @Override public List<@NotNull Vector> evalPartial(boolean aCalBasis, boolean aCalCross, IDxyzTypeIterable aNL) {
        return mMirrorBasis.evalPartial(aCalBasis, aCalCross, dxyzTypeDo -> aNL.forEachDxyzType((dx, dy, dz, type) -> {
            if (type == mThisType) type = mMirrorType;
            else if (type == mMirrorType) type = mThisType;
            dxyzTypeDo.run(dx, dy, dz, type);
        }));
    }
}
