package com.jtool.atom;

import com.jtool.code.UT;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static com.jtool.code.CS.*;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractAtomData implements IHasAtomData {
    /** stuff to override */
    public abstract Iterable<IAtom> atoms();
    public abstract IHasXYZ boxLo();
    public abstract IHasXYZ boxHi();
    public abstract int atomNum();
    public abstract int atomTypeNum();
    
    
    /** 直接使用过滤器过滤掉不符合的种类 */
    @Override public Iterable<IAtom> atoms(final int aType) {return UT.Code.filterIterable(atoms(), atom -> atom.type()==aType);}
    
    /** 会利用 atomNum() 来得到初始的容量 */
    @Override public ITable dataXYZ() {
        List<double[]> rData = new ArrayList<>(atomNum());
        for (IAtom tAtom : atoms()) rData.add(new double[] {tAtom.x(), tAtom.y(), tAtom.z()});
        return new Table(ATOM_DATA_KEYS_XYZ, rData);
    }
    @Override public ITable dataXYZ(int aType) {
        List<double[]> rData = new ArrayList<>();
        for (IAtom tAtom : atoms(aType)) rData.add(new double[] {tAtom.x(), tAtom.y(), tAtom.z()});
        return new Table(ATOM_DATA_KEYS_XYZ, rData);
    }
    @Override public ITable dataXYZID() {
        List<double[]> rData = new ArrayList<>(atomNum());
        for (IAtom tAtom : atoms()) rData.add(new double[] {tAtom.x(), tAtom.y(), tAtom.z(), tAtom.id()});
        return new Table(ATOM_DATA_KEYS_XYZID, rData);
    }
    @Override public ITable dataXYZID(int aType) {
        List<double[]> rData = new ArrayList<>();
        for (IAtom tAtom : atoms(aType)) rData.add(new double[] {tAtom.x(), tAtom.y(), tAtom.z(), tAtom.id()});
        return new Table(ATOM_DATA_KEYS_XYZID, rData);
    }
    @Override public ITable dataSTD() {
        List<double[]> rData = new ArrayList<>(atomNum());
        for (IAtom tAtom : atoms()) rData.add(new double[] {tAtom.id(), tAtom.type(), tAtom.x(), tAtom.y(), tAtom.z()});
        return new Table(STD_ATOM_DATA_KEYS, rData);
    }
    @Override public ITable dataSTD(int aType) {
        List<double[]> rData = new ArrayList<>();
        for (IAtom tAtom : atoms(aType)) rData.add(new double[] {tAtom.id(), tAtom.type(), tAtom.x(), tAtom.y(), tAtom.z()});
        return new Table(STD_ATOM_DATA_KEYS, rData);
    }
    
    
    /** 用来方便子类直接使用 */
    protected static class TableAtoms extends AbstractList<IAtom> {
        protected final ITable mTable;
        public TableAtoms(ITable aTable) {mTable = aTable;}
        
        @Override public IAtom get(final int index) {
            return new IAtom() {
                @Override public double x() {return mTable.get(index, "x");}
                @Override public double y() {return mTable.get(index, "y");}
                @Override public double z() {return mTable.get(index, "z");}
                
                @Override public int id() {return (int)mTable.get(index, "id");}
                @Override public int type() {return (int)mTable.get(index, "type");}
            };
        }
        @Override public int size() {return mTable.rowNumber();}
    }
    
    
    /// 实用功能，这里依旧保留这种写法
    /**
     * 获取单原子参数的计算器，支持使用 MPC 的简写来调用
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MPC 的线程数目
     * @return 获取到的 MPC
     */
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, int aThreadNum) {return new MonatomicParameterCalculator(atoms(aType), aThreadNum);}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (                         ) {return new MonatomicParameterCalculator(atoms()                 );}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (           int aThreadNum) {return new MonatomicParameterCalculator(atoms()     , aThreadNum);}
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                ) {return new MonatomicParameterCalculator(atoms(aType)            );}
    @VisibleForTesting public MonatomicParameterCalculator getMPC          (                         ) {return new MonatomicParameterCalculator(atoms()                 );}
    @VisibleForTesting public MonatomicParameterCalculator getMPC          (           int aThreadNum) {return new MonatomicParameterCalculator(atoms()     , aThreadNum);}
    @VisibleForTesting public MonatomicParameterCalculator getTypeMPC      (int aType                ) {return new MonatomicParameterCalculator(atoms(aType)            );}
    @VisibleForTesting public MonatomicParameterCalculator getTypeMPC      (int aType, int aThreadNum) {return new MonatomicParameterCalculator(atoms(aType), aThreadNum);}
}
