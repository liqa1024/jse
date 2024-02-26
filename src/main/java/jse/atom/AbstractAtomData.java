package jse.atom;

import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.NewCollections;

import java.util.List;

import static jse.code.UT.Code.newBox;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractAtomData implements IAtomData {
    /** stuff to override */
    public abstract IAtom atom(int aIdx);
    public abstract IXYZ box();
    public abstract int atomNumber();
    public abstract int atomTypeNumber();
    
    
    @Override @Deprecated @SuppressWarnings("deprecation") public List<? extends IAtom> asList() {return atoms();}
    @Override public List<? extends IAtom> atoms() {
        return new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(int index) {return atom(index);}
            @Override public int size() {return atomNumber();}
        };
    }
    
    @Override public double volume() {return box().prod();}
    
    @Override public IAtomDataOperation operation() {return new AbstractAtomDataOperation() {
        @Override protected IAtomData thisAtomData_() {return AbstractAtomData.this;}
        @Override protected ISettableAtomData newSameSettableAtomData_() {return newSame_();}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum) {return newZeros_(aAtomNum);}
    };}
    
    
    /** 会利用 atomNumber() 来得到初始的容量 */
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
    /** 默认没有速度信息，这样不会在输出时进行输出 */
    @Override public boolean hasVelocities() {return false;}
    
    
    /**  默认拷贝会直接使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存并且会抹除速度信息 */
    @Override public ISettableAtomData copy() {
        final boolean tHasVelocities = hasVelocities();
        return new SettableAtomData(
            NewCollections.map(atoms(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNumber(), newBox(box()), tHasVelocities
        );
    }
    /** 这两个方法返回结果要保证一定可以进行修改 */
    protected ISettableAtomData newSame_() {
        final boolean tHasVelocities = hasVelocities();
        return new SettableAtomData(
            NewCollections.map(atoms(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
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
