package com.jtool.atom;

import com.jtool.code.UT;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;

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
    
    @Override public double volume() {return boxHi().minus(boxLo()).product();}
    
    
    /** 直接使用过滤器过滤掉不符合的种类 */
    @Override public Iterable<IAtom> atoms(final int aType) {return UT.Code.filter(atoms(), atom -> atom.type()==aType);}
    
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
}
