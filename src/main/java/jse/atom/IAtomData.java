package jse.atom;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 通用的拥有原子数据的类使用的接口，主要用于相互转换 </p>
 * <p> 移除过多的无用的接口，只保留实际使用会用到的部分 </p>
 */
public interface IAtomData {
    /** 转为兼容性更高的 double[][]，约定数据按行排列，每行一个原子 */
    default double[][] data() {return dataSTD();}
    double[][] dataXYZ();
    double[][] dataXYZID();
    double[][] dataSTD();
    double[][] dataAll();
    /** 获取速度数据, vx, vy, vz */
    double[][] dataVelocities();
    boolean hasVelocities();
    
    /** 现在改为 atoms 保证一致性 */
    List<? extends IAtom> atoms();
    /** 现在统一提供随机访问获取一个原子的接口；改为 atom 保证逻辑一致，旧名称由于外部使用较少不再保留 */
    IAtom atom(int aIdx);

    /** asList 接口保留兼容 */
    @Deprecated default List<? extends IAtom> asList() {return atoms();}
    
    /** 保留获取原子总数的接口，但是特定种类的原子数目现在不能直接获取 */
    int atomNumber();
    int atomTypeNumber();
    /** 保留旧名称兼容，当时起名太随意了，居然这么久都没发现 */
    @Deprecated default int atomNum() {return atomNumber();}
    @Deprecated default int atomTypeNum() {return atomTypeNumber();}
    /** 提供简写版本 */
    @VisibleForTesting default int natoms() {return atomNumber();}
    @VisibleForTesting default int ntypes() {return atomTypeNumber();}
    
    /** 获取模拟盒信息的接口，现在为了使用方便，移除了 boxLo 的设定 */
    IXYZ box();
    double volume();
    
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
     */
    @ApiStatus.Obsolete default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return new MonatomicParameterCalculator(operation().filterType(aType), aThreadNum);}
    @ApiStatus.Obsolete default MonatomicParameterCalculator getMonatomicParameterCalculator    (                                                              ) {return new MonatomicParameterCalculator(this                                     );}
    @ApiStatus.Obsolete default MonatomicParameterCalculator getMonatomicParameterCalculator    (           @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return new MonatomicParameterCalculator(this                         , aThreadNum);}
    @ApiStatus.Obsolete default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                                                     ) {return new MonatomicParameterCalculator(operation().filterType(aType)            );}
    @ApiStatus.Obsolete @VisibleForTesting default MPC getMPC    (                                                              ) {return new MPC(this                                     );}
    @ApiStatus.Obsolete @VisibleForTesting default MPC getMPC    (           @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return new MPC(this                         , aThreadNum);}
    @ApiStatus.Obsolete @VisibleForTesting default MPC getTypeMPC(int aType                                                     ) {return new MPC(operation().filterType(aType)            );}
    @ApiStatus.Obsolete @VisibleForTesting default MPC getTypeMPC(int aType, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return new MPC(operation().filterType(aType), aThreadNum);}
}
