package jse.atom;


import jse.code.collection.ISlice;
import jse.code.functional.IFilter;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.code.random.IRandom;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

import static jse.code.CS.RANDOM;

/**
 * 通用的原子数据运算接口，针对不可修改的一般原子数据
 * {@link IAtomData}，如无特殊说明都会统一返回一个新的原子数据，
 * 并且此时统一返回可修改的源自数据 {@link ISettableAtomData}
 * @author liqa
 * @see IAtomData#operation()
 * @see ISettableAtomDataOperation
 */
public interface IAtomDataOperation {
    /**
     * 根据通用的过滤器 aFilter 来过滤原子数据，保留满足 Filter 的原子：
     * <pre> {@code
     * def filtered = data.opt().filter {it.x() > 10}
     * } </pre>
     * 来获取 x 坐标大于 10 的所有原子组成的原子数据
     * <p>
     * 这里会直接统一过滤一次构造完成过滤后的列表，因此不会保留过滤器的引用
     *
     * @param aFilter 自定义的过滤器，输入 {@link IAtom}，返回是否保留的 {@code boolean}
     * @return 新创建的过滤后的 {@link ISettableAtomData}
     */
    ISettableAtomData filter(IFilter<IAtom> aFilter);
    /**
     * 根据原子种类来过滤原子数据，只保留选定种类的原子，等价于：
     * <pre> {@code
     * def filtered = data.opt().filter {it.type() == aType}
     * } </pre>
     *
     * @param aType 选择保留的原子种类数
     * @return 新创建的过滤后的 {@link ISettableAtomData}
     */
    ISettableAtomData filterType(int aType);
    
    /**
     * 根据通用的切片对象 {@link ISlice} (例如 {@link IIntVector})
     * 指定的索引来引用切片原子数据，不会进行值拷贝因此返回的还是
     * {@link IAtomData}
     * @param aIndices 指定需要切片的索引列表
     * @return 引用切片 {@link IAtomData}
     * @see ISlice
     */
    IAtomData refSlice(ISlice aIndices);
    /**
     * 根据索引组成的列表 {@code List<Integer>}
     * 来引用切片原子数据，不会进行值拷贝因此返回的还是
     * {@link IAtomData}
     * @param aIndices 指定需要切片的索引列表
     * @return 引用切片 {@link IAtomData}
     */
    IAtomData refSlice(List<Integer> aIndices);
    /**
     * 根据索引组成的数组 {@code int[]}
     * 来引用切片原子数据，不会进行值拷贝因此返回的还是
     * {@link IAtomData}
     * @param aIndices 指定需要切片的索引列表
     * @return 引用切片 {@link IAtomData}
     */
    IAtomData refSlice(int[] aIndices);
    /**
     * 根据通用的索引过滤器 {@link IIndexFilter}
     * 来引用切片原子数据，不会进行值拷贝因此返回的还是
     * {@link IAtomData}
     * <p>
     * 这里会先遍历一次过滤器 {@link IIndexFilter}
     * 来构造一个索引列表，再根据列表来引用切片，因此不会保留过滤器的引用
     * <p>
     * 可以通过：
     * <pre> {@code
     * def sliced = data.opt().refSlice {data.atom(it).x() > 10}
     * } </pre>
     * 来获取 x 坐标大于 10 的所有原子组成的引用切片原子数据
     * (而不是值拷贝的原子数据)
     *
     * @param aIndices 指定需要切片的索引过滤器
     * @return 引用切片 {@link IAtomData}
     * @see IIndexFilter
     * @see #filter(IFilter)
     */
    IAtomData refSlice(IIndexFilter aIndices);
    
    
    /**
     * 根据通用的原子映射 {@code IUnaryFullOperator<IAtom, IAtom>} 来遍历映射修改原子：
     * <pre> {@code
     * def mapped = data.opt().map {
     *     def atom = new Atom(it)
     *     atom.x += 1.0
     *     return atom
     * }
     * } </pre>
     * 来统一让所有原子 x 坐标增加 {@code 1.0}；
     * 这个操作不会修改原始的原子数据，而是会将修改应用到新的原子数据后返回
     * <p>
     * 这里闭包中获取到的原子是不可修改的原子 {@link IAtom}，因此修改只能通过创建一个新的原子
     * {@link Atom} 来实现；一般来说直接通过 {@link IAtomData#copy()}
     * 来复制得到一份可修改的原子数据，然后直接遍历修改会更加简洁方便。
     *
     * @param aOperator 自定义的原子映射运算，输入 {@link IAtom} 输出修改后的 {@link IAtom}
     * @return 新创建的修改后的 {@link ISettableAtomData}
     * @see IUnaryFullOperator
     */
    default ISettableAtomData map(IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator) {return map(1, aOperator);}
    /**
     * 增加一个最小种类数目参数的映射修改，可以避免所有映射后，实际最大原子种类编号达不到需要的种类数目的问题
     * @param aMinTypeNum 需要的最小种类数目
     * @param aOperator 自定义的原子映射运算，输入 {@link IAtom} 输出修改后的 {@link IAtom}
     * @return 新创建的修改后的 {@link ISettableAtomData}
     * @see #map(IUnaryFullOperator)
     */
    ISettableAtomData map(int aMinTypeNum, IUnaryFullOperator<? extends IAtom, ? super IAtom> aOperator);
    
    /**
     * 根据特殊的原子种类映射 {@code IUnaryFullOperator<Integer, IAtom>} 来直接遍历映射修改原子种类：
     * <pre> {@code
     * def typeMap = [0, 2, 1, 4, 3]
     * def mapped = data.opt().mapType {typeMap[it.type()]}
     * } </pre>
     * 来让所有种类按照给定的 {@code typeMap} 来重新映射；
     * 这个操作不会修改原始的原子数据，而是会将修改应用到新的原子数据后返回
     *
     * @param aOperator 自定义的原子映射运算，输入 {@link IAtom} 输出应该分配的种类编号 {@link Integer}
     * @return 新创建的修改后的 {@link ISettableAtomData}
     * @see IUnaryFullOperator
     */
    default ISettableAtomData mapType(IUnaryFullOperator<Integer, ? super IAtom> aOperator) {return mapType(1, aOperator);}
    /**
     * 增加一个最小种类数目参数的映射修改，可以避免所有映射后，实际最大原子种类编号达不到需要的种类数目的问题
     * @param aMinTypeNum 需要的最小种类数目
     * @param aOperator 自定义的原子映射运算，输入 {@link IAtom} 输出应该分配的种类编号 {@link Integer}
     * @return 新创建的修改后的 {@link ISettableAtomData}
     * @see #mapType(IUnaryFullOperator)
     */
    ISettableAtomData mapType(int aMinTypeNum, IUnaryFullOperator<Integer, ? super IAtom> aOperator);
    
    /**
     * 根据给定的权重来随机分配原子种类，可以用于创建一个理想固溶体的初始结构
     * <p>
     * 当原子数能按照给定权重整除时，则会按照整除结果随机打乱种类；
     * 当原子数不能整除时，会取最大的能整除的原子数随机打乱，
     * 而剩下的原子会按照剩余权重随机分配原子种类，从而保证在重复调用
     * {@link #mapTypeRandom} 后生成的多个结构的最终原子种类平均占比满足给定权重。
     *
     * @param aTypeWeights 每个种类的权重
     * @return 新创建的修改后的 {@link ISettableAtomData}
     */
    default ISettableAtomData mapTypeRandom(double... aTypeWeights) {return mapTypeRandom(RANDOM, aTypeWeights);}
    /**
     * 增加一个随机数生成器参数的随机种类映射，可以用来控制内部随机使用的随机流，保证结果一致
     * <p>
     * 也可以通过 {@link jse.code.UT.Math#rng(long)} 来设置全局随机数生成器的种子，从而也能控制随机流
     *
     * @param aRandom 需要使用的随机数生成器
     * @param aTypeWeights 每个种类的权重
     * @return 新创建的修改后的 {@link ISettableAtomData}
     * @see #mapTypeRandom(double...)
     * @see IRandom
     */
    default ISettableAtomData mapTypeRandom(IRandom aRandom, double... aTypeWeights) {
        // 特殊输入直接抛出错误
        if (aTypeWeights == null || aTypeWeights.length == 0) throw new RuntimeException("TypeWeights Must be not empty");
        return mapTypeRandom(aRandom, Vectors.from(aTypeWeights));
    }
    /**
     * 传入 jse 向量 {@link IVector} 版本的随机种类映射
     * @see #mapTypeRandom(double...)
     * @see IVector
     */
    default ISettableAtomData mapTypeRandom(IVector aTypeWeights) {return mapTypeRandom(RANDOM, aTypeWeights);}
    /**
     * 传入 jse 向量 {@link IVector} 版本的随机种类映射
     * @see #mapTypeRandom(IRandom, double...)
     * @see IVector
     */
    ISettableAtomData mapTypeRandom(IRandom aRandom, IVector aTypeWeights);
    
    
    /**
     * 使用高斯分布来随机扰动原子位置；
     * 这个操作不会修改原始的原子数据，而是会将修改应用到新的原子数据后返回
     * @param aSigma 高斯分布的标准差
     * @return 新创建的扰动后的 {@link ISettableAtomData}
     */
    default ISettableAtomData perturbXYZGaussian(double aSigma) {return perturbXYZGaussian(RANDOM, aSigma);}
    /**
     * 增加一个随机数生成器参数的随机扰动，可以用来控制内部随机使用的随机流，保证结果一致
     * <p>
     * 也可以通过 {@link jse.code.UT.Math#rng(long)} 来设置全局随机数生成器的种子，从而也能控制随机流
     * @param aRandom 需要使用的随机数生成器
     * @param aSigma 高斯分布的标准差
     * @return 新创建的修改后的 {@link ISettableAtomData}
     * @see #perturbXYZGaussian(double)
     * @see IRandom
     */
    ISettableAtomData perturbXYZGaussian(IRandom aRandom, double aSigma);
    /** @see #perturbXYZGaussian(double) */
    @VisibleForTesting default ISettableAtomData perturbXYZ(double aSigma) {return perturbXYZGaussian(aSigma);}
    /** @see #perturbXYZGaussian(IRandom, double) */
    @VisibleForTesting default ISettableAtomData perturbXYZ(IRandom aRandom, double aSigma) {return perturbXYZGaussian(aRandom, aSigma);}
    
    /**
     * 使用周期边界条件将出界的原子移动回到盒内；
     * 这个操作不会修改原始的原子数据，而是会将修改应用到新的原子数据后返回
     * @see IBox#wrapPBC(XYZ)
     */
    ISettableAtomData wrapPBC();
    /** @see #wrapPBC() */
    @VisibleForTesting default ISettableAtomData wrap() {return wrapPBC();}
    
    
    /**
     * 将原子数据扩胞，即向 x,y,z 方向重复指定次数
     * <p>
     * 这里不会对出边界的原子作特殊处理
     *
     * @param aNx x 方向的重复次数
     * @param aNy Y 方向的重复次数
     * @param aNz Z 方向的重复次数
     * @return 新创建的重复后的 {@link ISettableAtomData}
     */
    ISettableAtomData repeat(int aNx, int aNy, int aNz);
    /**
     * 将原子数据扩胞，即向 x,y,z 方向重复指定次数
     * <p>
     * 这里不会对出边界的原子作特殊处理
     *
     * @param aN 向 x,y,z 三个方向的重复次数
     * @return 新创建的重复后的 {@link ISettableAtomData}
     */
    default ISettableAtomData repeat(int aN) {return repeat(aN, aN, aN);}
    
    /**
     * 将结构切分成小块
     * <p>
     * 结果按照 {@code x, y, z (i, j, k)}
     * 的顺序依次遍历，也就是说，如果需要访问给定 {@code (i, j, k)}
     * 位置的切片结果，需要使用：
     * <pre> {@code
     * def list = data.opt().slice(Nx, Ny, Nz)
     * int idx = i + j*Nx + k*Nx*Ny
     * def subData = list[idx]
     * } </pre>
     * 来获取，同理对于给定的列表位置 {@code idx}，
     * 需要使用：
     * <pre> {@code
     * int i = idx % Nx
     * int j = idx.intdiv(Nx) % Ny
     * int k = idx.intdiv(Nx).intdiv(Ny)
     * } </pre>
     * 来获取相应的空间位置 {@code (i, j, k)}
     * <p>
     * 这里会直接移除掉出边界的原子，因此如果需要保留这些原子需要先调用
     * {@link #wrapPBC()} 来将其变换到模拟盒内部
     *
     * @param aNx x 方向的切片次数
     * @param aNy Y 方向的切片次数
     * @param aNz Z 方向的切片次数
     * @return 新创建的切片后的 {@link ISettableAtomData} 组成的列表
     */
    List<? extends ISettableAtomData> slice(int aNx, int aNy, int aNz);
    default List<? extends ISettableAtomData> slice(int aN) {return slice(aN, aN, aN);}
    
    
    /**
     * 对原子数据进行团簇分析，获取成团簇的原子索引存储为
     * {@link IntVector}，所有团簇构成一个列表
     * {@code List<IntVector>}
     * <p>
     * 这里直接按照团簇分析的发现顺序排列团簇，因此原则上列表是无序的
     * <p>
     * 这里不会特定把团簇的原子放在一起，如果有这个需求则使用
     * {@link #unwrapByCluster(double)} 来获取一个按照团簇 unwrap
     * 的原子数据。如果希望一次团簇分析同时完成这两步，则需要调用可设置原子数据中的操作
     * {@link ISettableAtomDataOperation#clusterAnalyze(double, boolean)}
     * 来顺便将 unwrap 操作引用到自身。
     *
     * @param aRCut 用于判断团簇链接的截断半径
     * @return 每个团簇对应的原子索引列表 {@link IntVector} 组成的列表
     * @see IntVector
     * @see #unwrapByCluster(double)
     * @see ISettableAtomDataOperation#clusterAnalyze(double, boolean)
     */
    List<IntVector> clusterAnalyze(double aRCut);
    /**
     * 团簇分析并根据分析结果按照团簇 unwrap 原子；
     * 这个操作不会修改原始的原子数据，而是会将修改应用到新的原子数据后返回
     * <p>
     * 只会执行 unwrap 操作，而不保留团簇信息，这在很多时候都很有用
     * <p>
     * 如果需要团簇信息需要使用 {@link #clusterAnalyze(double)}，
     * 如果希望一次团簇分析同时执行两个操作则需要调用可设置原子数据中的操作
     * {@link ISettableAtomDataOperation#clusterAnalyze(double, boolean)}
     *
     * @param aRCut 用于判断团簇链接的截断半径
     * @return 新创建的根据团簇 unwrap 的 {@link ISettableAtomData}
     *
     * @see #clusterAnalyze(double)
     * @see ISettableAtomDataOperation#clusterAnalyze(double, boolean)
     */
    ISettableAtomData unwrapByCluster(double aRCut);
}
