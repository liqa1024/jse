package jse.atom;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.table.Table;

import java.util.List;


/**
 * @author liqa
 * <p> 抽象的拥有多个帧的原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractMultiFrameSettableAtomData<T extends ISettableAtomData> extends AbstractRandomAccessList<T> implements ISettableAtomData {
    /** AbstractList stuffs */
    @Override public abstract int size();
    @Override public abstract T get(int index);
    
    /** IHasAtomData 的接口，将本身作为 atomData 时则会返回第一帧的结果 */
    public T defaultFrame() {return get(0);}
    @Override public final Table dataXYZ() {return defaultFrame().dataXYZ();}
    @Override public final Table dataXYZID() {return defaultFrame().dataXYZID();}
    @Override public final Table dataSTD() {return defaultFrame().dataSTD();}
    @Override public final Table dataAll() {return defaultFrame().dataAll();}
    @Override public final Table dataVelocities() {return defaultFrame().dataVelocities();}
    
    @Override public final boolean hasVelocities() {return defaultFrame().hasVelocities();}
    
    @Override public final ISettableAtom pickAtom(int aIdx) {return defaultFrame().pickAtom(aIdx);}
    @Override public final void setAtom(int aIdx, IAtom aAtom) {defaultFrame().setAtom(aIdx, aAtom);}
    @Override public final List<? extends ISettableAtom> asList() {return defaultFrame().asList();}
    @Override public final int atomNum() {return defaultFrame().atomNum();}
    @Override public final int atomTypeNum() {return defaultFrame().atomTypeNum();}
    @Override public final ISettableAtomData setAtomTypeNum(int aAtomTypeNum) {return defaultFrame().setAtomTypeNum(aAtomTypeNum);}
    
    @Override public final IXYZ box() {return defaultFrame().box();}
    @Override public final double volume() {return defaultFrame().volume();}
    
    @Override public final ISettableAtomDataOperation operation() {return defaultFrame().operation();}
    
    /** stuff to override */
    @Override public ISettableAtomData copy() {return defaultFrame().copy();}
}
