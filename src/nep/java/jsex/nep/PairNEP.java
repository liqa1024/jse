package jsex.nep;

import jse.cptr.DoubleCPointer;
import jse.cptr.IntCPointer;
import jse.jit.SimpleJIT;
import jse.lmp.LmpPlugin;

/**
 * {@link LmpPlugin.Pair} 的 NEP 版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nep.PairNEP
 * pair_coeff   * * path/to/neppot.txt Cu Zr
 * } </pre>
 * 来使用
 *
 * @see NEP
 * @author Junjie Wang，liqa
 */
public class PairNEP extends LmpPlugin.Pair {
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
    
    protected PairNEP(long aPairPtr) {
        super(aPairPtr);
    }
    
    @Override public void settings(String... aArgs) throws Exception {
        super.settings(aArgs);
        // nep 支持 centroidstress
        setCentroidstressflag(CENTROID_AVAIL);
        // 限制只能调用一次 pair_coeff
        setOneCoeff(true);
        // 此时需要禁用 VirialFdotrCompute
        noVirialFdotrCompute();
    }
    
    @Override public void initStyle() {
        if (!forceNewtonPair()) {
            throw new IllegalArgumentException("Pair style NEP requires newton pair on");
        }
        // nep 需要完整的近邻列表
        neighborRequestFull();
    }
    
    /**
     * 这里为了和 lammps 接口保持一致，并不完全按照 java 中的代码风格编写
     */
    @Override public void compute() throws Exception {
        mNEP.computeLammps(this);
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
        
        int tTypeNum = atomNtypes();
        int tArgLen = aArgs.length-2;
        if (tArgLen-1 != tTypeNum) throw new IllegalArgumentException("Elements number in pair_coeff not match ntypes ("+tTypeNum+").");
        mTypeMap = IntCPointer.calloc(tArgLen);
        mNEP.init_from_file(aArgs[2]);
        for (int type = 1; type < tArgLen; ++type) {
            String tElem = aArgs[2+type];
            int tNEPType = mNEP.typeOf(tElem)-1;
            if (tNEPType < 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in pair_coeff");
            mTypeMap.putAt(type, tNEPType);
        }
        // get cutoff from NEP model
        mCutoff = mNEP.rcutMax();
        mCutoffsq = DoubleCPointer.malloc(1);
        mCutoffsq.set(mCutoff * mCutoff);
    }
    protected double mCutoff = Double.NaN;
    protected DoubleCPointer mCutoffsq = null;
    protected NEP mNEP = new NEP();
    protected IntCPointer mTypeMap = null;
    
    @Override public double initOne(int i, int j) {
        return mCutoff;
    }
    
    @Override public void shutdown() {
        if (mTypeMap != null) {
            mTypeMap.free();
            mTypeMap = null;
        }
        if (mCutoffsq != null) {
            mCutoffsq.free();
            mCutoffsq = null;
        }
        if (mNEP != null) {
            mNEP.shutdown();
            mNEP = null;
        }
    }
}
