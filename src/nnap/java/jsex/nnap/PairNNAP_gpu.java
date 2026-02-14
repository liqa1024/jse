package jsex.nnap;

/**
 * {@link PairNNAP2} 的 GPU 版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nnap.PairNNAP_gpu
 * pair_coeff   * * path/to/nnpot.json Cu Zr
 * } </pre>
 * 来使用
 */
public class PairNNAP_gpu extends PairNNAP2 {
    protected PairNNAP_gpu(long aPairPtr) {
        super(aPairPtr);
    }
    @Override protected NNAP2 initNNAP(String aPath) throws Exception {
        return new NNAP_cuda(aPath);
    }
}
