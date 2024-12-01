package jsex.nnap.basis;

import jse.io.ISavable;
import jse.math.matrix.RowMatrix;
import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 通用的 nnap 基组/描述符实现
 * @author liqa
 */
@ApiStatus.Experimental
public interface IBasis extends ISavable, IAutoShutdown {
    /** @return 基组需要的近邻截断半径 */
    double rcut();
    /** @see #rowNumber() */
    default int nrows() {return rowNumber();}
    /** @see #columnNumber() */
    default int ncols() {return columnNumber();}
    /** @return 基组矩阵的行数目 */
    int rowNumber();
    /** @return 基组矩阵的列数目 */
    int columnNumber();
    
    @FunctionalInterface interface IDxyzTypeIterable {void forEachDxyzType(IDxyzTypeDo aDxyzTypeDo);}
    @FunctionalInterface interface IDxyzTypeDo {void run(double aDx, double aDy, double aDz, int aType);}
    
    /**
     * 根据给定近邻获取基组具体值，通过定义一个近邻列表遍历器来传入通用的近邻列表，对于
     * {@link jse.atom.MonatomicParameterCalculator} 来获取近邻列表的情况，则可以通过类似：
     * <pre> {@code
     * import jse.atom.MPC
     *
     * def mpc = MPC.of(data)
     * int i = 10 // index of atom that want to eval basis
     * def fp = basis.eval {dxyzTypeDo ->
     *     mpc.nl_().forEachNeighbor(i, basis.rcut(), false) {x, y, z, idx, dx, dy, dz ->
     *         dxyzTypeDo.run(dx, dy, dz, mpc.atomType_()[idx])
     *     }
     * }
     * mpc.shutdown()
     * } </pre>
     * 这样代码来得到特定原子的基组
     * @param aNL 近邻列表遍历器
     * @return 原子描述符行矩阵，可以通过 asVecRow 来转为向量形式方便作为神经网络的输入
     */
    RowMatrix eval(IDxyzTypeIterable aNL);
    
    /**
     * 基组结果对于 {@code xyz} 偏微分的计算结果，主要用于力的计算
     * @param aCalBasis 控制是否同时计算基组本来的值，默认为 {@code true}
     * @param aCalCross 控制是否同时计算基组对于近邻原子坐标的偏导值，默认为 {@code false}
     * @param aNL 近邻列表遍历器
     * @return {@code [fp, fpPx, fpPy, fpPz]}，如果关闭 aCalBasis 则第一项
     * {@code fp} 为 null，如果开启 aCalBasis 则在后续追加近邻的偏导
     */
    List<@NotNull RowMatrix> evalPartial(boolean aCalBasis, boolean aCalCross, IDxyzTypeIterable aNL);
    default List<@NotNull RowMatrix> evalPartial(IDxyzTypeIterable aNL) {return evalPartial(true, false, aNL);}
    
    @Override default void shutdown() {/**/}
}
