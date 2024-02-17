package jse.atom;

import jse.code.collection.AbstractRandomAccessList;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractSettableAtomData extends AbstractAtomData implements ISettableAtomData {
    /** stuff to override */
    public abstract ISettableAtom pickAtom(int aIdx);
    public abstract AbstractSettableAtomData setAtomTypeNumber(int aAtomTypeNum);
    
    @Override public void setAtom(int aIdx, IAtom aAtom) {
        ISettableAtom tAtom = pickAtom(aIdx);
        try {tAtom.setX(aAtom.x());} catch (Exception ignored) {}
        try {tAtom.setY(aAtom.y());} catch (Exception ignored) {}
        try {tAtom.setZ(aAtom.z());} catch (Exception ignored) {}
        try {tAtom.setID(aAtom.id());} catch (Exception ignored) {}
        try {tAtom.setType(aAtom.type());} catch (Exception ignored) {}
        try {tAtom.setVx(aAtom.vx());} catch (Exception ignored) {}
        try {tAtom.setVy(aAtom.vy());} catch (Exception ignored) {}
        try {tAtom.setVz(aAtom.vz());} catch (Exception ignored) {}
    }
    
    @Override public List<? extends ISettableAtom> asList() {
        return new AbstractRandomAccessList<ISettableAtom>() {
            @Override public ISettableAtom get(int index) {return pickAtom(index);}
            @Override public ISettableAtom set(final int index, ISettableAtom element) {
                ISettableAtom oAtom = hasVelocities() ?
                    new AtomFull(pickAtom(index)) {
                    @Override public int index() {return index;}
                } : new Atom(pickAtom(index)) {
                    @Override public int index() {return index;}
                };
                setAtom(index, element);
                return oAtom;
            }
            @Override public int size() {return atomNumber();}
        };
    }
    
    @Override public ISettableAtomDataOperation operation() {return new AbstractSettableAtomDataOperation() {
        @Override protected ISettableAtomData thisAtomData_() {return AbstractSettableAtomData.this;}
        @Override protected ISettableAtomData newSameSettableAtomData_() {return newSame_();}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum) {return newZeros_(aAtomNum);}
    };}
    @VisibleForTesting @Override public ISettableAtomDataOperation opt() {return operation();}
}
