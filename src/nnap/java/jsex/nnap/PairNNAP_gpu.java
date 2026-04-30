package jsex.nnap;

import jse.gpu.CudaJIT;
import org.jetbrains.annotations.ApiStatus;

/**
 * {@link PairNNAP} 的 GPU 版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nnap.PairNNAP_gpu
 * pair_coeff   * * path/to/nnpot.json Cu Zr
 * } </pre>
 * 来使用
 */
@ApiStatus.Obsolete
public class PairNNAP_gpu extends PairNNAP {
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
        PairNNAP_gpu.InitHelper.INITIALIZED = true;
        // 现在只需要 jit 初始化即可
        CudaJIT.InitHelper.init();
        _INIT_FLAG = false;
    }
    
    protected PairNNAP_gpu(long aPairPtr) {
        super(aPairPtr);
    }
    @Override protected NNAP initNNAP(String aPath) throws Exception {
        return new NNAP_cuda(aPath);
    }
}
