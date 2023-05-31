package com.jtool.atom;

import com.jtool.math.table.ITable;

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
    
    
    /** 改为直接获取 {@link IAtom} 的容器 */
    Iterable<IAtom> atoms();
    Iterable<IAtom> atoms(int aType);
    
    /** 保留获取原子总数的接口，但是特定种类的原子数目现在不能直接获取 */
    int atomNum();
    int atomTypeNum();
    
    
    /** 获取模拟盒信息的接口 */
    IHasXYZ boxLo();
    IHasXYZ boxHi();
}
