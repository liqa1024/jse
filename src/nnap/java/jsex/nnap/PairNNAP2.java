package jsex.nnap;

import jse.cptr.DoubleCPointer;
import jse.cptr.GrowableIntCPointer;
import jse.cptr.IntCPointer;
import jse.cptr.NestedCPointer;
import jse.jit.SimpleJIT;
import jse.lmp.LmpPlugin;

/**
 * {@link LmpPlugin.Pair} 的 NNAP 版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nnap.PairNNAP2
 * pair_coeff   * * path/to/nnpot.json Cu Zr
 * } </pre>
 * 来使用
 */
public class PairNNAP2 extends LmpPlugin.Pair {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(_INIT_FLAG);
        }
    }
    private final static boolean _INIT_FLAG;
    static {
        InitHelper.INITIALIZED = true;
        // 现在只需要 jit 初始化即可
        SimpleJIT.InitHelper.init();
        _INIT_FLAG = false;
    }
    
    protected PairNNAP2(long aPairPtr) {
        super(aPairPtr);
    }
    
    @Override public void settings(String... aArgs) throws Exception {
        super.settings(aArgs);
        // nnap 支持 centroidstress
        setCentroidstressflag(CENTROID_AVAIL);
        // 限制只能调用一次 pair_coeff
        setOneCoeff(true);
        // 此时需要禁用 VirialFdotrCompute
        noVirialFdotrCompute();
    }
    
    @Override public void initStyle() {
        if (!forceNewtonPair()) {
            throw new IllegalArgumentException("Pair style NNAP requires newton pair on");
        }
        // nnap 需要完整的近邻列表
        neighborRequestFull();
    }
    
    /**
     * 这里为了和 lammps 接口保持一致，并不完全按照 java 中的代码风格编写
     */
    @Override public void compute() throws Exception {
        mNNAP.computeLammps(this);
    }
    
    @Override public void coeff(String... aArgs) throws Exception {
        // 这样错位初始化，保证 me == 0 的优先单独初始化，初始化库或者任何报错都可以单独输出
        if (commMe()==0) {
            coeff_(aArgs);
            commBarrier();
        } else {
            commBarrier();
            coeff_(aArgs);
        }
    }
    private void coeff_(String... aArgs) throws Exception {
        if (aArgs==null || aArgs.length<4) throw new IllegalArgumentException("Not enough arguments, pair_coeff MUST be like `* * path/to/nnpot elem1 ...`");
        if (!aArgs[0].equals("*") || !aArgs[1].equals("*")) throw new IllegalArgumentException("pair_coeff MUST start with `* *`");
        mNNAP = new NNAP2(aArgs[2], 1, precision());
        String tNNAPUnits = mNNAP.units();
        if (tNNAPUnits != null) {
            String tLmpUnits = unitStyle();
            if (tLmpUnits!=null && !tLmpUnits.equals(tNNAPUnits)) throw new IllegalArgumentException("Invalid units ("+tLmpUnits+") for this model ("+tNNAPUnits+")");
        }
        mTypeNum = atomNtypes();
        int tArgLen = aArgs.length-2;
        if (tArgLen-1 != mTypeNum) throw new IllegalArgumentException("Elements number in pair_coeff not match ntypes ("+mTypeNum+").");
        mLmpType2NNAPType = IntCPointer.calloc(tArgLen);
        mCutoff = new double[tArgLen];
        mCutsq = DoubleCPointer.calloc(tArgLen);
        for (int type = 1; type < tArgLen; ++type) {
            String tElem = aArgs[2+type];
            int tNNAPType = mNNAP.typeOf(tElem);
            if (tNNAPType <= 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in pair_coeff");
            mLmpType2NNAPType.putAt(type, tNNAPType);
            mCutoff[type] = mNNAP.rcut(tNNAPType);
            mCutsq.putAt(type, mCutoff[type]*mCutoff[type]);
        }
        mTypeInum = IntCPointer.calloc(tArgLen);
        mTypeIlist = NestedCPointer.calloc(tArgLen);
        mTypeIlistBuf = new GrowableIntCPointer[tArgLen];
        for (int type = 1; type < tArgLen; ++type) {
            mTypeIlistBuf[type] = new GrowableIntCPointer(128);
        }
    }
    NNAP2 mNNAP = null;
    IntCPointer mLmpType2NNAPType = null;
    double[] mCutoff = null;
    DoubleCPointer mCutsq = null;
    int mTypeNum = -1;
    GrowableIntCPointer[] mTypeIlistBuf = null;
    NestedCPointer mTypeIlist = null;
    IntCPointer mTypeInum = null;
    
    @Override public double initOne(int i, int j) {
        return mCutoff[i];
    }
    protected String precision() {
        return null;
    }
    
    @Override public void shutdown() {
        if (mLmpType2NNAPType != null) {
            mLmpType2NNAPType.free();
            mLmpType2NNAPType = null;
        }
        if (mCutsq != null) {
            mCutsq.free();
            mCutsq = null;
        }
        if (mTypeIlistBuf != null) {
            for (int type = 1; type < mTypeNum; ++type) mTypeIlistBuf[type].free();
            mTypeIlistBuf = null;
        }
        if (mTypeIlist != null) {
            mTypeIlist.free();
            mTypeIlist = null;
        }
        if (mTypeInum != null) {
            mTypeInum.free();
            mTypeInum = null;
        }
        if (mNNAP != null) {
            mNNAP.shutdown();
            mNNAP = null;
        }
    }
}
