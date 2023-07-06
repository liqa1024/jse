package com.jtool.atom;

import com.jtool.code.UT;
import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.RowMatrix;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;

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
        IMatrix rData = RowMatrix.zeros(atomNum(), 3);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, 0, tAtom.x());
            rData.set_(row, 1, tAtom.y());
            rData.set_(row, 2, tAtom.z());
            ++row;
        }
        return new Table(ATOM_DATA_KEYS_XYZ, rData);
    }
    @Override public ITable dataXYZ(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), 3);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
            rData.set_(row, 0, tAtom.x());
            rData.set_(row, 1, tAtom.y());
            rData.set_(row, 2, tAtom.z());
            ++row;
        }
        return new Table(ATOM_DATA_KEYS_XYZ, rData);
    }
    @Override public ITable dataXYZID() {
        IMatrix rData = RowMatrix.zeros(atomNum(), 4);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, 0, tAtom.x());
            rData.set_(row, 1, tAtom.y());
            rData.set_(row, 2, tAtom.z());
            rData.set_(row, 3, tAtom.id());
            ++row;
        }
        return new Table(ATOM_DATA_KEYS_XYZID, rData);
    }
    @Override public ITable dataXYZID(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), 4);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
            rData.set_(row, 0, tAtom.x());
            rData.set_(row, 1, tAtom.y());
            rData.set_(row, 2, tAtom.z());
            rData.set_(row, 3, tAtom.id());
            ++row;
        }
        return new Table(ATOM_DATA_KEYS_XYZID, rData);
    }
    @Override public ITable dataSTD() {
        IMatrix rData = RowMatrix.zeros(atomNum(), 5);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, 0, tAtom.id());
            rData.set_(row, 1, tAtom.type());
            rData.set_(row, 2, tAtom.x());
            rData.set_(row, 3, tAtom.y());
            rData.set_(row, 4, tAtom.z());
            ++row;
        }
        return new Table(STD_ATOM_DATA_KEYS, rData);
    }
    @Override public ITable dataSTD(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), 5);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
            rData.set_(row, 0, tAtom.id());
            rData.set_(row, 1, tAtom.type());
            rData.set_(row, 2, tAtom.x());
            rData.set_(row, 3, tAtom.y());
            rData.set_(row, 4, tAtom.z());
            ++row;
        }
        return new Table(STD_ATOM_DATA_KEYS, rData);
    }
    
    
    /** 用来方便子类直接使用 */
    protected static class TableAtoms extends AbstractRandomAccessList<IAtom> {
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
