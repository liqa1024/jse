package jtool.atom;

import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.collection.NewCollections;
import jtool.math.matrix.IMatrix;
import jtool.math.table.ITable;
import jtool.math.table.Tables;

import java.util.List;

import static jtool.code.CS.*;
import static jtool.code.UT.Code.newBox;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractAtomData implements IAtomData {
    /** stuff to override */
    public abstract IAtom pickAtom(int aIdx);
    public abstract IXYZ box();
    public abstract int atomNum();
    public abstract int atomTypeNum();
    
    
    @Override public List<? extends IAtom> asList() {
        return new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(int index) {return pickAtom(index);}
            @Override public int size() {return atomNum();}
        };
    }
    
    @Override public double volume() {return box().prod();}
    
    @Override public IAtomDataOperation operation() {return new AbstractAtomDataOperation() {
        @Override protected IAtomData thisAtomData_() {return AbstractAtomData.this;}
        @Override protected ISettableAtomData newSameSettableAtomData_() {return newSame_();}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum) {return newZeros_(aAtomNum);}
    };}
    
    
    /** 会利用 atomNum() 来得到初始的容量 */
    @Override public ITable dataXYZ() {
        ITable rData = Tables.zeros(atomNum(), ATOM_DATA_KEYS_XYZ);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set_(row, XYZ_X_COL, tAtom.x());
            rMat.set_(row, XYZ_Y_COL, tAtom.y());
            rMat.set_(row, XYZ_Z_COL, tAtom.z());
            ++row;
        }
        return rData;
    }
    @Override public ITable dataXYZID() {
        ITable rData = Tables.zeros(atomNum(), ATOM_DATA_KEYS_XYZID);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set_(row, XYZID_X_COL, tAtom.x());
            rMat.set_(row, XYZID_Y_COL, tAtom.y());
            rMat.set_(row, XYZID_Z_COL, tAtom.z());
            rMat.set_(row, XYZID_ID_COL, tAtom.id());
            ++row;
        }
        return rData;
    }
    @Override public ITable dataSTD() {
        ITable rData = Tables.zeros(atomNum(), STD_ATOM_DATA_KEYS);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set_(row, STD_ID_COL, tAtom.id());
            rMat.set_(row, STD_TYPE_COL, tAtom.type());
            rMat.set_(row, STD_X_COL, tAtom.x());
            rMat.set_(row, STD_Y_COL, tAtom.y());
            rMat.set_(row, STD_Z_COL, tAtom.z());
            ++row;
        }
        return rData;
    }
    @Override public ITable dataAll() {
        ITable rData = Tables.zeros(atomNum(), ALL_ATOM_DATA_KEYS);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set_(row, ALL_ID_COL, tAtom.id());
            rMat.set_(row, ALL_TYPE_COL, tAtom.type());
            rMat.set_(row, ALL_X_COL, tAtom.x());
            rMat.set_(row, ALL_Y_COL, tAtom.y());
            rMat.set_(row, ALL_Z_COL, tAtom.z());
            rMat.set_(row, ALL_VX_COL, tAtom.vx());
            rMat.set_(row, ALL_VY_COL, tAtom.vy());
            rMat.set_(row, ALL_VZ_COL, tAtom.vz());
            ++row;
        }
        return rData;
    }
    @Override public ITable dataVelocities() {
        ITable rData = Tables.zeros(atomNum(), ATOM_DATA_KEYS_VELOCITY);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set_(row, STD_VX_COL, tAtom.vx());
            rMat.set_(row, STD_VY_COL, tAtom.vy());
            rMat.set_(row, STD_VZ_COL, tAtom.vz());
            ++row;
        }
        return rData;
    }
    /** 默认没有速度信息，这样不会在输出时进行输出 */
    @Override public boolean hasVelocities() {return false;}
    
    
    /**  默认拷贝会直接使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存并且会抹除速度信息 */
    @Override public ISettableAtomData copy() {
        final boolean tHasVelocities = hasVelocities();
        return new SettableAtomData(
            NewCollections.map(asList(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNum(), newBox(box()), tHasVelocities
        );
    }
    /** 这两个方法返回结果要保证一定可以进行修改 */
    protected ISettableAtomData newSame_() {
        final boolean tHasVelocities = hasVelocities();
        return new SettableAtomData(
            NewCollections.map(asList(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNum(), newBox(box()), tHasVelocities
        );
    }
    protected ISettableAtomData newZeros_(int aAtomNum) {
        final boolean tHasVelocities = hasVelocities();
        return new SettableAtomData(
            NewCollections.from(aAtomNum, tHasVelocities ? (i -> new AtomFull()) : (i -> new Atom())),
            atomTypeNum(), newBox(box()), tHasVelocities
        );
    }
}
