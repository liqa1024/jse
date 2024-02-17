package jse.atom;

import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.NewCollections;
import jse.math.matrix.IMatrix;
import jse.math.table.Table;
import jse.math.table.Tables;

import java.util.List;

import static jse.code.CS.*;
import static jse.code.UT.Code.newBox;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractAtomData implements IAtomData {
    /** stuff to override */
    public abstract IAtom pickAtom(int aIdx);
    public abstract IXYZ box();
    public abstract int atomNumber();
    public abstract int atomTypeNumber();
    
    
    @Override public List<? extends IAtom> asList() {
        return new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(int index) {return pickAtom(index);}
            @Override public int size() {return atomNumber();}
        };
    }
    
    @Override public double volume() {return box().prod();}
    
    @Override public IAtomDataOperation operation() {return new AbstractAtomDataOperation() {
        @Override protected IAtomData thisAtomData_() {return AbstractAtomData.this;}
        @Override protected ISettableAtomData newSameSettableAtomData_() {return newSame_();}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum) {return newZeros_(aAtomNum);}
    };}
    
    
    /** 会利用 atomNum() 来得到初始的容量 */
    @Override public Table dataXYZ() {
        Table rData = Tables.zeros(atomNumber(), ATOM_DATA_KEYS_XYZ);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set(row, XYZ_X_COL, tAtom.x());
            rMat.set(row, XYZ_Y_COL, tAtom.y());
            rMat.set(row, XYZ_Z_COL, tAtom.z());
            ++row;
        }
        return rData;
    }
    @Override public Table dataXYZID() {
        Table rData = Tables.zeros(atomNumber(), ATOM_DATA_KEYS_XYZID);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set(row, XYZID_X_COL, tAtom.x());
            rMat.set(row, XYZID_Y_COL, tAtom.y());
            rMat.set(row, XYZID_Z_COL, tAtom.z());
            rMat.set(row, XYZID_ID_COL, tAtom.id());
            ++row;
        }
        return rData;
    }
    @Override public Table dataSTD() {
        Table rData = Tables.zeros(atomNumber(), STD_ATOM_DATA_KEYS);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set(row, STD_ID_COL, tAtom.id());
            rMat.set(row, STD_TYPE_COL, tAtom.type());
            rMat.set(row, STD_X_COL, tAtom.x());
            rMat.set(row, STD_Y_COL, tAtom.y());
            rMat.set(row, STD_Z_COL, tAtom.z());
            ++row;
        }
        return rData;
    }
    @Override public Table dataAll() {
        Table rData = Tables.zeros(atomNumber(), ALL_ATOM_DATA_KEYS);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set(row, ALL_ID_COL, tAtom.id());
            rMat.set(row, ALL_TYPE_COL, tAtom.type());
            rMat.set(row, ALL_X_COL, tAtom.x());
            rMat.set(row, ALL_Y_COL, tAtom.y());
            rMat.set(row, ALL_Z_COL, tAtom.z());
            rMat.set(row, ALL_VX_COL, tAtom.vx());
            rMat.set(row, ALL_VY_COL, tAtom.vy());
            rMat.set(row, ALL_VZ_COL, tAtom.vz());
            ++row;
        }
        return rData;
    }
    @Override public Table dataVelocities() {
        Table rData = Tables.zeros(atomNumber(), ATOM_DATA_KEYS_VELOCITY);
        IMatrix rMat = rData.asMatrix();
        int row = 0;
        for (IAtom tAtom : asList()) {
            rMat.set(row, STD_VX_COL, tAtom.vx());
            rMat.set(row, STD_VY_COL, tAtom.vy());
            rMat.set(row, STD_VZ_COL, tAtom.vz());
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
            atomTypeNumber(), newBox(box()), tHasVelocities
        );
    }
    /** 这两个方法返回结果要保证一定可以进行修改 */
    protected ISettableAtomData newSame_() {
        final boolean tHasVelocities = hasVelocities();
        return new SettableAtomData(
            NewCollections.map(asList(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNumber(), newBox(box()), tHasVelocities
        );
    }
    protected ISettableAtomData newZeros_(int aAtomNum) {
        final boolean tHasVelocities = hasVelocities();
        return new SettableAtomData(
            NewCollections.from(aAtomNum, tHasVelocities ? (i -> new AtomFull()) : (i -> new Atom())),
            atomTypeNumber(), newBox(box()), tHasVelocities
        );
    }
}
