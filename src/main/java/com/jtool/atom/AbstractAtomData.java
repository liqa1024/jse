package com.jtool.atom;

import com.jtool.code.UT;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.RowMatrix;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;

import java.util.Arrays;

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
    
    @Override public double volume() {return boxHi().minus(boxLo()).prod();}
    
    @Override public final IAtomDataFilter filter() {return new AbstractAtomDataFilter() {
        @Override protected IHasAtomData thisAtomData_() {return AbstractAtomData.this;}
    };}
    
    /** 直接使用过滤器过滤掉不符合的种类 */
    @Override public Iterable<IAtom> atoms(final int aType) {return UT.Code.filter(atoms(), atom -> atom.type()==aType);}
    
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
    @Override public ITable dataXYZ(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), ATOM_DATA_KEYS_XYZ.length);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
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
    @Override public ITable dataXYZID(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), ATOM_DATA_KEYS_XYZID.length);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
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
    @Override public ITable dataSTD(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), STD_ATOM_DATA_KEYS.length);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
            rData.set_(row, STD_ID_COL, tAtom.id());
            rData.set_(row, STD_TYPE_COL, tAtom.type());
            rData.set_(row, STD_X_COL, tAtom.x());
            rData.set_(row, STD_Y_COL, tAtom.y());
            rData.set_(row, STD_Z_COL, tAtom.z());
            ++row;
        }
        return new Table(Arrays.copyOf(STD_ATOM_DATA_KEYS, STD_ATOM_DATA_KEYS.length), rData);
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
    @Override public ITable dataAll(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), ALL_ATOM_DATA_KEYS.length);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
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
    @Override public ITable dataVelocities(int aType) {
        IMatrix rData = RowMatrix.zeros(atomNum(), ATOM_DATA_KEYS_VELOCITY.length);
        int row = 0;
        for (IAtom tAtom : atoms(aType)) {
            rData.set_(row, STD_VX_COL, tAtom.vx());
            rData.set_(row, STD_VY_COL, tAtom.vy());
            rData.set_(row, STD_VZ_COL, tAtom.vz());
            ++row;
        }
        return new Table(Arrays.copyOf(ATOM_DATA_KEYS_VELOCITY, ATOM_DATA_KEYS_VELOCITY.length), rData);
    }
    /** 默认没有速度信息，这样不会在输出时进行输出 */
    @Override public boolean hasVelocities() {return false;}
}
