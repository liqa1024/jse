package jsex.nep;

import jse.gpu.CudaJIT;

/**
 * {@link PairNEP} 的 GPU 版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nep.PairNEP_gpu
 * pair_coeff   * * path/to/neppot.txt Cu Zr
 * } </pre>
 * 来使用
 */
public class PairNEP_gpu extends PairNEP {
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
        PairNEP_gpu.InitHelper.INITIALIZED = true;
        // 现在只需要 jit 初始化即可
        CudaJIT.InitHelper.init();
        _INIT_FLAG = false;
    }
    
    protected PairNEP_gpu(long aPairPtr) {
        super(aPairPtr);
    }
    
    @Override public void compute() throws Exception {
        mNEP.computeLammpsCuda(this);
    }
    @Override protected void initNEP(String aPath) throws Exception {
        mNEP.init_from_file(aPath, "cuda");
    }
}
