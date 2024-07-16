package jse.atom;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractSettableAtomData extends AbstractAtomData implements ISettableAtomData {
    /** stuff to override */
    public abstract ISettableAtom atom(int aIdx);
    public AbstractSettableAtomData setAtomTypeNumber(int aAtomTypeNum) {throw new UnsupportedOperationException("setAtomTypeNumber");}
    public AbstractSettableAtomData setNoVelocity() {throw new UnsupportedOperationException("setNoVelocity");}
    public AbstractSettableAtomData setHasVelocity() {throw new UnsupportedOperationException("setHasVelocity");}
    public AbstractSettableAtomData setSymbols(String... aSymbols) {throw new UnsupportedOperationException("setSymbols");}
    public AbstractSettableAtomData setNoSymbol() {throw new UnsupportedOperationException("setNoSymbol");}
    public AbstractSettableAtomData setMasses(double... aMasses) {throw new UnsupportedOperationException("setMasses");}
    public AbstractSettableAtomData setMasses(Collection<? extends Number> aMasses) {throw new UnsupportedOperationException("setMasses");}
    public AbstractSettableAtomData setMasses(IVector aMasses) {throw new UnsupportedOperationException("setMasses");}
    public AbstractSettableAtomData setNoMass() {throw new UnsupportedOperationException("setNoMass");}
    /** @deprecated use {@link #setNoVelocity} */
    @Deprecated public AbstractSettableAtomData setNoVelocities() {return setNoVelocity();}
    /** @deprecated use {@link #setHasVelocity} */
    @Deprecated public AbstractSettableAtomData setHasVelocities() {return setHasVelocity();}
    
    /** Groovy stuffs */
    @VisibleForTesting public @Nullable List<@Nullable String> getSymbols() {return symbols();}
    @VisibleForTesting public @Nullable IVector getMasses() {return masses();}
    
    /** 提供一个一般的原子实现，帮助实现重复的部分 */
    protected abstract class AbstractSettableAtom_ extends AbstractSettableAtom {
        @Override public boolean hasVelocity() {return AbstractSettableAtomData.this.hasVelocity();}
        @Override public @Nullable String symbol() {return AbstractSettableAtomData.this.symbol(type());}
        @Override public boolean hasSymbol() {return AbstractSettableAtomData.this.hasSymbol();}
        @Override public double mass() {return AbstractSettableAtomData.this.mass(type());}
        @Override public boolean hasMass() {return AbstractSettableAtomData.this.hasMasse();}
    }
    
    
    @Override @Deprecated @SuppressWarnings("deprecation") public List<? extends ISettableAtom> asList() {return atoms();}
    @Override public void setAtom(int aIdx, IAtom aAtom) {
        ISettableAtom tAtom = this.atom(aIdx);
        tAtom.setXYZ(aAtom).setID(aAtom.id()).setType(aAtom.type());
        if (aAtom.hasVelocity()) tAtom.setVxyz(aAtom.vx(), aAtom.vy(), aAtom.vz());
    }
    
    @Override public List<? extends ISettableAtom> atoms() {
        return new AbstractRandomAccessList<ISettableAtom>() {
            @Override public ISettableAtom get(int index) {return AbstractSettableAtomData.this.atom(index);}
            @Override public ISettableAtom set(final int index, ISettableAtom element) {
                ISettableAtom oAtom = AbstractSettableAtomData.this.atom(index).copy();
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
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum, IBox aBox) {return newZeros_(aAtomNum, aBox);}
    };}
    @VisibleForTesting @Override public ISettableAtomDataOperation opt() {return operation();}
}
