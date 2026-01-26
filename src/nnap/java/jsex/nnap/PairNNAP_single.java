package jsex.nnap;

/**
 * {@link PairNNAP2} 的单精度版本，在 lammps
 * in 文件中添加：
 * <pre> {@code
 * pair_style   jse jsex.nnap.PairNNAP_single
 * pair_coeff   * * path/to/nnpot.json Cu Zr
 * } </pre>
 * 来使用
 * <p>
 * 在绝大多数情况下精度足够，而速度快于传统的双精度版本（cpu 下不明显）
 */
public class PairNNAP_single extends PairNNAP2 {
    protected PairNNAP_single(long aPairPtr) {
        super(aPairPtr);
    }
    @Override protected String precision() {
        return "single";
    }
}
