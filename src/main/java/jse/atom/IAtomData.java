package jse.atom;

import jse.math.table.ITable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 通用的拥有原子数据的类使用的接口，主要用于相互转换 </p>
 * <p> 移除过多的无用的接口，只保留实际使用会用到的部分 </p>
 */
public interface IAtomData {
    /** 获取所有的数据组成的 {@link ITable}，约定数据按行排列，每行一个原子，会在通用抽象类中自动生成不需要子类手动实现 */
    ITable dataXYZ();
    ITable dataXYZID();
    ITable dataSTD();
    ITable dataAll();
    /** 获取速度数据, vx, vy, vz */
    ITable dataVelocities();
    boolean hasVelocities();
    
    /** 现在改为 asList，让其 set 接口更加合理 */
    List<? extends IAtom> asList();
    /** 现在统一提供随机访问获取一个原子的接口 */
    IAtom pickAtom(int aIdx);
    
    /** 保留获取原子总数的接口，但是特定种类的原子数目现在不能直接获取 */
    int atomNum();
    int atomTypeNum();
    
    
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
    default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, int aThreadNum) {return new MonatomicParameterCalculator(operation().filterType(aType), aThreadNum);}
    default MonatomicParameterCalculator getMonatomicParameterCalculator    (                         ) {return new MonatomicParameterCalculator(this                                     );}
    default MonatomicParameterCalculator getMonatomicParameterCalculator    (           int aThreadNum) {return new MonatomicParameterCalculator(this                         , aThreadNum);}
    default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                ) {return new MonatomicParameterCalculator(operation().filterType(aType)            );}
    @VisibleForTesting default MonatomicParameterCalculator getMPC          (                         ) {return new MonatomicParameterCalculator(this                                     );}
    @VisibleForTesting default MonatomicParameterCalculator getMPC          (           int aThreadNum) {return new MonatomicParameterCalculator(this                         , aThreadNum);}
    @VisibleForTesting default MonatomicParameterCalculator getTypeMPC      (int aType                ) {return new MonatomicParameterCalculator(operation().filterType(aType)            );}
    @VisibleForTesting default MonatomicParameterCalculator getTypeMPC      (int aType, int aThreadNum) {return new MonatomicParameterCalculator(operation().filterType(aType), aThreadNum);}
}
