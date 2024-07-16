package jse.atom;

import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 通用的拥有原子数据的类使用的接口，主要用于相互转换 </p>
 * <p> 移除过多的无用的接口，只保留实际使用会用到的部分 </p>
 */
public interface IAtomData {
    /** 转为兼容性更高的 double[][]，约定数据按行排列，每行一个原子；现在默认 data() 顺序调整，改为 x, y, z, id, type, vx, vy, vz，将最基础的放在最前面可以保证最好的兼容性 */
    double[][] data();
    double[][] dataXYZ();
    double[][] dataXYZID();
    double[][] dataSTD();
    double[][] dataAll();
    /** 获取速度数据, vx, vy, vz */
    double[][] dataVelocities();
    boolean hasVelocity();
    /** @deprecated use {@link #hasVelocity} */
    @Deprecated default boolean hasVelocities() {return hasVelocity();}
    
    /** 现在改为 atoms 保证一致性 */
    List<? extends IAtom> atoms();
    /** 现在统一提供随机访问获取一个原子的接口；改为 atom 保证逻辑一致，旧名称由于外部使用较少不再保留 */
    IAtom atom(int aIdx);
    
    /** @deprecated use {@link #atoms} */
    @Deprecated default List<? extends IAtom> asList() {return atoms();}
    
    /** 保留获取原子总数的接口，但是特定种类的原子数目现在不能直接获取 */
    int atomNumber();
    int atomTypeNumber();
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */     @Deprecated default int atomNum() {return atomNumber();}
    /** @deprecated use {@link #atomTypeNumber} or {@link #ntypes} */ @Deprecated default int atomTypeNum() {return atomTypeNumber();}
    /** 提供简写版本 */
    @VisibleForTesting default int natoms() {return atomNumber();}
    @VisibleForTesting default int ntypes() {return atomTypeNumber();}
    
    /** 获取模拟盒信息的接口，现在为了使用方便，移除了 boxLo 的设定 */
    IBox box();
    default double volume() {return box().volume();}
    default boolean isPrism() {return box().isPrism();}
    default boolean isLmpStyle() {return box().isLmpStyle();}
    
    /** 现在都能获取到元素信息和质量，不存在的会返回 null */
    boolean hasSymbol();
    @Nullable List<@Nullable String> symbols();
    @Nullable String symbol(int aType);
    boolean hasMasse();
    @Nullable IVector masses();
    double mass(int aType);
    
    /** 统一提供拷贝接口 */
    ISettableAtomData copy();
    
    /** 返回原子数据计算器，现在不再使用专门的 Generator */
    IAtomDataOperation operation();
    @VisibleForTesting default IAtomDataOperation opt() {return operation();}
    
    
    /// 实用功能，这里依旧保留这种写法
    /**
     * 获取单原子参数的计算器，支持使用 MPC 的简写来调用
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MPC 的线程数目
     * @return 获取到的 MPC
     * @deprecated use {@link MonatomicParameterCalculator#of} or {@link MPC#of}
     */
    @Deprecated default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return MonatomicParameterCalculator.of(operation().filterType(aType), aThreadNum);}
    /** @deprecated use {@link MonatomicParameterCalculator#of}*/ @Deprecated default MonatomicParameterCalculator getMonatomicParameterCalculator    (                                                              ) {return MonatomicParameterCalculator.of(this                                     );}
    /** @deprecated use {@link MonatomicParameterCalculator#of}*/ @Deprecated default MonatomicParameterCalculator getMonatomicParameterCalculator    (           @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return MonatomicParameterCalculator.of(this                         , aThreadNum);}
    /** @deprecated use {@link MonatomicParameterCalculator#of}*/ @Deprecated default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                                                     ) {return MonatomicParameterCalculator.of(operation().filterType(aType)            );}
    /** @deprecated use {@link MPC#of}*/ @Deprecated @VisibleForTesting default MonatomicParameterCalculator       getMPC                             (                                                              ) {return MPC.of(this                                     );}
    /** @deprecated use {@link MPC#of}*/ @Deprecated @VisibleForTesting default MonatomicParameterCalculator       getMPC                             (           @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return MPC.of(this                         , aThreadNum);}
    /** @deprecated use {@link MPC#of}*/ @Deprecated @VisibleForTesting default MonatomicParameterCalculator       getTypeMPC                         (int aType                                                     ) {return MPC.of(operation().filterType(aType)            );}
    /** @deprecated use {@link MPC#of}*/ @Deprecated @VisibleForTesting default MonatomicParameterCalculator       getTypeMPC                         (int aType, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return MPC.of(operation().filterType(aType), aThreadNum);}
}
