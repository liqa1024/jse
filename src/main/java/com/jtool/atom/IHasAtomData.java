package com.jtool.atom;

import com.jtool.math.table.ITable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * @author liqa
 * <p> 通用的拥有原子数据的类使用的接口，主要用于相互转换 </p>
 * <p> 移除过多的无用的接口，只保留实际使用会用到的部分 </p>
 */
public interface IHasAtomData {
    /** 获取所有的数据组成的 {@link ITable}，约定数据按行排列，每行一个原子，会在通用抽象类中自动生成不需要子类手动实现 */
    ITable dataXYZ();
    ITable dataXYZ(int aType);
    ITable dataXYZID();
    ITable dataXYZID(int aType);
    ITable dataSTD();
    ITable dataSTD(int aType);
    ITable dataAll();
    ITable dataAll(int aType);
    /** 获取速度数据, vx, vy, vz */
    ITable dataVelocities();
    ITable dataVelocities(int aType);
    boolean hasVelocities();
    
    
    /** 改为直接获取 {@link IAtom} 的容器 */
    Iterable<IAtom> atoms();
    Iterable<IAtom> atoms(int aType);
    
    /** 保留获取原子总数的接口，但是特定种类的原子数目现在不能直接获取 */
    int atomNum();
    int atomTypeNum();
    
    
    /** 获取模拟盒信息的接口 */
    IHasXYZ boxLo();
    IHasXYZ boxHi();
    double volume();
    
    
    /// 实用功能，这里依旧保留这种写法
    /**
     * 获取单原子参数的计算器，支持使用 MPC 的简写来调用
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MPC 的线程数目
     * @return 获取到的 MPC
     */
    default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, int aThreadNum) {return new MonatomicParameterCalculator(atoms(aType), boxLo(), boxHi(), aThreadNum);}
    default MonatomicParameterCalculator getMonatomicParameterCalculator    (                         ) {return new MonatomicParameterCalculator(atoms()     , boxLo(), boxHi()            );}
    default MonatomicParameterCalculator getMonatomicParameterCalculator    (           int aThreadNum) {return new MonatomicParameterCalculator(atoms()     , boxLo(), boxHi(), aThreadNum);}
    default MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                ) {return new MonatomicParameterCalculator(atoms(aType), boxLo(), boxHi()            );}
    @VisibleForTesting default MonatomicParameterCalculator getMPC          (                         ) {return new MonatomicParameterCalculator(atoms()     , boxLo(), boxHi()            );}
    @VisibleForTesting default MonatomicParameterCalculator getMPC          (           int aThreadNum) {return new MonatomicParameterCalculator(atoms()     , boxLo(), boxHi(), aThreadNum);}
    @VisibleForTesting default MonatomicParameterCalculator getTypeMPC      (int aType                ) {return new MonatomicParameterCalculator(atoms(aType), boxLo(), boxHi()            );}
    @VisibleForTesting default MonatomicParameterCalculator getTypeMPC      (int aType, int aThreadNum) {return new MonatomicParameterCalculator(atoms(aType), boxLo(), boxHi(), aThreadNum);}
}
