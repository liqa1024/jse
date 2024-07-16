package jse.atom;

import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.NewCollections;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractAtomData implements IAtomData {
    /** stuff to override */
    public abstract IAtom atom(int aIdx);
    public abstract IBox box();
    public abstract int atomNumber();
    public abstract int atomTypeNumber();
    /** 默认没有这些附加信息信息 */
    @Override public boolean hasVelocity() {return false;}
    @Override public boolean hasSymbol() {return false;}
    @Override public @Nullable String symbol(int aType) {return null;}
    @Override public boolean hasMasse() {return false;}
    @Override public double mass(int aType) {return Double.NaN;}
    
    @Override public @Nullable List<@Nullable String> symbols() {
        if (!hasSymbol()) return null;
        return new AbstractRandomAccessList<@Nullable String>() {
            @Override public @Nullable String get(int index) {return symbol(index+1);}
            @Override public int size() {return atomTypeNumber();}
        };
    }
    @Override public @Nullable IVector masses() {
        if (!hasMasse()) return null;
        return new RefVector() {
            @Override public double get(int aIdx) {return mass(aIdx+1);}
            @Override public int size() {return atomTypeNumber();}
        };
    }
    
    /** 提供一个一般的原子实现，帮助实现重复的部分 */
    protected abstract class AbstractAtom_ extends AbstractAtom {
        @Override public boolean hasVelocity() {return AbstractAtomData.this.hasVelocity();}
        @Override public @Nullable String symbol() {return AbstractAtomData.this.symbol(type());}
        @Override public boolean hasSymbol() {return AbstractAtomData.this.hasSymbol();}
        @Override public double mass() {return AbstractAtomData.this.mass(type());}
        @Override public boolean hasMass() {return AbstractAtomData.this.hasMasse();}
    }
    
    
    @Override @Deprecated @SuppressWarnings("deprecation") public List<? extends IAtom> asList() {return atoms();}
    @Override public List<? extends IAtom> atoms() {
        return new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(int index) {return atom(index);}
            @Override public int size() {return atomNumber();}
        };
    }
    
    @Override public IAtomDataOperation operation() {return new AbstractAtomDataOperation() {
        @Override protected IAtomData thisAtomData_() {return AbstractAtomData.this;}
        @Override protected ISettableAtomData newSameSettableAtomData_() {return newSame_();}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum) {return newZeros_(aAtomNum);}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum, IBox aBox) {return newZeros_(aAtomNum, aBox);}
    };}
    
    
    /** 会利用 atomNumber() 来得到初始的容量 */
    @Override public double[][] data() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).data();
        }
        return rData;
    }
    @Override public double[][] dataXYZ() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataXYZ();
        }
        return rData;
    }
    @Override public double[][] dataXYZID() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataXYZID();
        }
        return rData;
    }
    @Override public double[][] dataSTD() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataSTD();
        }
        return rData;
    }
    @Override public double[][] dataAll() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataAll();
        }
        return rData;
    }
    @Override public double[][] dataVelocities() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataVelocities();
        }
        return rData;
    }
    
    
    /**  默认拷贝会直接使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存并且会抹除速度信息 */
    @Override public ISettableAtomData copy() {
        final boolean tHasVelocities = hasVelocity();
        return new SettableAtomData(
            NewCollections.map(atoms(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNumber(), box().copy(), tHasVelocities
        );
    }
    /** 这两个方法返回结果要保证一定可以进行修改 */
    protected ISettableAtomData newSame_() {
        final boolean tHasVelocities = hasVelocity();
        return new SettableAtomData(
            NewCollections.map(atoms(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNumber(), box().copy(), tHasVelocities
        );
    }
    protected ISettableAtomData newZeros_(int aAtomNum) {return newZeros_(aAtomNum, box().copy());}
    protected ISettableAtomData newZeros_(int aAtomNum, IBox aBox) {
        final boolean tHasVelocities = hasVelocity();
        return new SettableAtomData(
            NewCollections.from(aAtomNum, tHasVelocities ? (i -> new AtomFull()) : (i -> new Atom())),
            atomTypeNumber(), aBox, tHasVelocities
        );
    }
}
