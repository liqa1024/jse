package com.jtool.atom;

import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.RowMatrix;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jtool.code.CS.*;
import static com.jtool.code.UT.Code.newBox;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractAtomData implements IAtomData {
    /** stuff to override */
    public abstract List<IAtom> atoms();
    public abstract IXYZ box();
    public abstract int atomNum();
    public abstract int atomTypeNum();
    
    @Override public double volume() {return box().prod();}
    
    @Override public final IAtomDataOperation operation() {return new AbstractAtomDataOperation() {
        @Override protected IAtomData thisAtomData_() {return AbstractAtomData.this;}
    };}
    
    
    /** 会利用 atomNum() 来得到初始的容量 */
    @Override public ITable dataXYZ() {
        IMatrix rData = RowMatrix.zeros(atomNum(), ATOM_DATA_KEYS_XYZ.length);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, XYZ_X_COL, tAtom.x());
            rData.set_(row, XYZ_Y_COL, tAtom.y());
            rData.set_(row, XYZ_Z_COL, tAtom.z());
            ++row;
        }
        return new Table(Arrays.copyOf(ATOM_DATA_KEYS_XYZ, ATOM_DATA_KEYS_XYZ.length), rData);
    }
    @Override public ITable dataXYZID() {
        IMatrix rData = RowMatrix.zeros(atomNum(), ATOM_DATA_KEYS_XYZID.length);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, XYZID_X_COL, tAtom.x());
            rData.set_(row, XYZID_Y_COL, tAtom.y());
            rData.set_(row, XYZID_Z_COL, tAtom.z());
            rData.set_(row, XYZID_ID_COL, tAtom.id());
            ++row;
        }
        return new Table(Arrays.copyOf(ATOM_DATA_KEYS_XYZID, ATOM_DATA_KEYS_XYZID.length), rData);
    }
    @Override public ITable dataSTD() {
        IMatrix rData = RowMatrix.zeros(atomNum(), STD_ATOM_DATA_KEYS.length);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, STD_ID_COL, tAtom.id());
            rData.set_(row, STD_TYPE_COL, tAtom.type());
            rData.set_(row, STD_X_COL, tAtom.x());
            rData.set_(row, STD_Y_COL, tAtom.y());
            rData.set_(row, STD_Z_COL, tAtom.z());
            ++row;
        }
        return new Table(STD_ATOM_DATA_KEYS, rData);
    }
    @Override public ITable dataAll() {
        IMatrix rData = RowMatrix.zeros(atomNum(), ALL_ATOM_DATA_KEYS.length);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, ALL_ID_COL, tAtom.id());
            rData.set_(row, ALL_TYPE_COL, tAtom.type());
            rData.set_(row, ALL_X_COL, tAtom.x());
            rData.set_(row, ALL_Y_COL, tAtom.y());
            rData.set_(row, ALL_Z_COL, tAtom.z());
            rData.set_(row, ALL_VX_COL, tAtom.vx());
            rData.set_(row, ALL_VY_COL, tAtom.vy());
            rData.set_(row, ALL_VZ_COL, tAtom.vz());
            ++row;
        }
        return new Table(Arrays.copyOf(ALL_ATOM_DATA_KEYS, ALL_ATOM_DATA_KEYS.length), rData);
    }
    @Override public ITable dataVelocities() {
        IMatrix rData = RowMatrix.zeros(atomNum(), ATOM_DATA_KEYS_VELOCITY.length);
        int row = 0;
        for (IAtom tAtom : atoms()) {
            rData.set_(row, STD_VX_COL, tAtom.vx());
            rData.set_(row, STD_VY_COL, tAtom.vy());
            rData.set_(row, STD_VZ_COL, tAtom.vz());
            ++row;
        }
        return new Table(Arrays.copyOf(ATOM_DATA_KEYS_VELOCITY, ATOM_DATA_KEYS_VELOCITY.length), rData);
    }
    /** 默认没有速度信息，这样不会在输出时进行输出 */
    @Override public boolean hasVelocities() {return false;}
    
    /**  默认拷贝会直接使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存并且会抹除速度信息 */
    @Override public AbstractAtomData copy() {
        List<IAtom> rAtoms = new ArrayList<>(atomNum());
        for (IAtom tAtom : atoms()) rAtoms.add(new Atom(tAtom));
        return new AtomData(rAtoms, atomTypeNum(), newBox(box()));
    }
}
