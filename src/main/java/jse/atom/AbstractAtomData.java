package jse.atom;

import jse.code.CS;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.NewCollections;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static jse.code.CS.ZL_STR;

/**
 * 一般原子数据抽象类，方便子类实现接口 {@link IAtomData}
 * <p>
 * 具体来说，子类最少需要实现：
 * <pre>
 *    {@link #atom(int)}: 获取指定索引的原子引用 {@link IAtom}
 *    {@link #box()}: 获取模拟盒对象
 *    {@link #atomNumber()}: 获取原子总数
 *    {@link #atomTypeNumber()}: 获取总原子种类数目
 * </pre>
 * @see IAtomData IAtomData: 通用的原子数据接口
 * @see AtomData AtomData: 一般的原子数据实现
 * @see AbstractSettableAtomData AbstractSettableAtomData: 可以修改的原子数据抽象类
 * @author liqa
 */
public abstract class AbstractAtomData implements IAtomData {
    /**
     * {@inheritDoc}
     *
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom
     * @see AbstractAtom_
     * @see AtomData AtomData: 关于具体实现的例子
     *
     * @implSpec 一般来说需要返回一个引用原子，并且保证内部的 {@link IAtom#index()},
     * {@link IAtom#hasVelocity()} 等方法调用到 {@link IAtomData} 自身；一般通过返回
     * {@link AbstractAtom_} 的匿名类来实现：
     * <pre> {@code
     * @Override IAtom atom(int idx) {
     *     // get reference of the stored atom
     *     def atom = atoms.get(idx)
     *     return new AbstractAtom_() {
     *         @Override double x() {return atom.x()}
     *         @Override double y() {return atom.y()}
     *         @Override double z() {return atom.z()}
     *         @Override int id() {return atom.id()}
     *         @Override int type() {return atom.type()}
     *         // make sure index() returns the correct value
     *         @Override int index() {return idx}
     *         @Override double vx() {return atom.vx()}
     *         @Override double vy() {return atom.vy()}
     *         @Override double vz() {return atom.vz()}
     *     }
     * }
     * } </pre>
     */
    public abstract IAtom atom(int aIdx);
    /**
     * @return {@inheritDoc}
     * @see IBox
     * @see AtomData AtomData: 关于具体实现的例子
     */
    public abstract IBox box();
    /**
     * @return {@inheritDoc}
     * @see AtomData AtomData: 关于具体实现的例子
     */
    public abstract int atomNumber();
    /**
     * @return {@inheritDoc}
     * @see AtomData AtomData: 关于具体实现的例子
     */
    public abstract int atomTypeNumber();
    
    /**
     * @return {@inheritDoc}
     * @see AtomData AtomData: 关于具体实现的例子
     */
    @Override public boolean hasVelocity() {return false;}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasSymbol()
     * @see AtomData AtomData: 关于具体实现的例子
     */
    @Override public boolean hasSymbol() {return false;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     * @see IAtom#type()
     * @see #hasSymbol()
     * @see AtomData AtomData: 关于具体实现的例子
     */
    @Override public @Nullable String symbol(int aType) {return null;}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasMass()
     * @see AtomData AtomData: 关于具体实现的例子
     */
    @Override public boolean hasMass() {return false;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#mass()
     * @see IAtom#type()
     * @see #hasMass()
     * @see AtomData AtomData: 关于具体实现的例子
     */
    @Override public double mass(int aType) {return Double.NaN;}
    
    /**
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     */
    @Override public @Nullable List<@Nullable String> symbols() {
        if (!hasSymbol()) return null;
        return new AbstractRandomAccessList<@Nullable String>() {
            @Override public @Nullable String get(int index) {return symbol(index+1);}
            @Override public int size() {return atomTypeNumber();}
        };
    }
    /**
     * @return {@inheritDoc}
     * @see IVector
     * @see IAtom#mass()
     * @see #hasMass()
     */
    @Override public @Nullable IVector masses() {
        if (!hasMass()) return null;
        return new RefVector() {
            @Override public double get(int aIdx) {return mass(aIdx+1);}
            @Override public int size() {return atomTypeNumber();}
        };
    }
    
    /**
     * 对于 {@link IAtomData} 内部的原子的一个一般原子实现，帮助实现重复的部分；
     * 主要转发了 {@link IAtom#hasVelocity()}, {@link IAtom#symbol()},
     * {@link IAtom#hasSymbol()}, {@link IAtom#mass()} 以及 {@link IAtom#hasMass()}
     * 到相对应的 {@link IAtomData} 内的方法。
     * @see #atom(int)
     */
    protected abstract class AbstractAtom_ extends AbstractAtom {
        @Override public boolean hasVelocity() {return AbstractAtomData.this.hasVelocity();}
        @Override public @Nullable String symbol() {return AbstractAtomData.this.symbol(type());}
        @Override public boolean hasSymbol() {return AbstractAtomData.this.hasSymbol();}
        @Override public double mass() {return AbstractAtomData.this.mass(type());}
        @Override public boolean hasMass() {return AbstractAtomData.this.hasMass();}
    }
    
    
    /**
     * @return {@inheritDoc}
     * @see IAtom
     */
    @Override public List<? extends IAtom> atoms() {
        return new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(int index) {return atom(index);}
            @Override public int size() {return atomNumber();}
        };
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtomDataOperation
     * @see AbstractAtomDataOperation
     * @see ISettableAtomDataOperation
     */
    @Override public IAtomDataOperation operation() {return new AbstractAtomDataOperation() {
        @Override protected IAtomData thisAtomData_() {return AbstractAtomData.this;}
        @Override protected ISettableAtomData newSameSettableAtomData_() {return newSame_();}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum) {return newZeros_(aAtomNum);}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum, IBox aBox) {return newZeros_(aAtomNum, aBox);}
    };}
    
    
    /// data stuffs
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#data()
     * @see CS#ATOM_DATA_KEYS
     */
    @Override public double[][] data() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).data();
        }
        return rData;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#dataXYZ()
     * @see CS#ATOM_DATA_KEYS_XYZ
     */
    @Override public double[][] dataXYZ() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataXYZ();
        }
        return rData;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#dataXYZID()
     * @see CS#ATOM_DATA_KEYS_XYZID
     */
    @Override public double[][] dataXYZID() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataXYZID();
        }
        return rData;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#dataSTD()
     * @see CS#STD_ATOM_DATA_KEYS
     */
    @Override public double[][] dataSTD() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataSTD();
        }
        return rData;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#dataAll()
     * @see CS#ALL_ATOM_DATA_KEYS
     */
    @Override public double[][] dataAll() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataAll();
        }
        return rData;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#dataVelocities()
     * @see CS#ATOM_DATA_KEYS_VELOCITY
     */
    @Override public double[][] dataVelocities() {
        final int tAtomNum = atomNumber();
        double[][] rData = new double[tAtomNum][];
        for (int i = 0; i < tAtomNum; ++i) {
            rData[i] = atom(i).dataVelocities();
        }
        return rData;
    }
    
    
    /**
     * 默认拷贝会直接使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存
     * @return {@inheritDoc}
     */
    @Override public ISettableAtomData copy() {
        final boolean tHasVelocities = hasVelocity();
        @Nullable List<@Nullable String> tSymbols = symbols();
        return new SettableAtomData(
            NewCollections.map(atoms(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNumber(), box().copy(), tHasVelocities,
            tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR)
        );
    }
    /**
     * 用于 {@link IAtomDataOperation} 内部实现创建新的返回对象使用；
     * 重写来返回自定义的原子数据类型
     * @return 内部存储数据相同的原子数据类型
     * @implSpec 一定需要返回可修改的原子数据类型
     * {@link ISettableAtomData}，并且要保证可以简单遍历修改属性
     */
    protected ISettableAtomData newSame_() {
        final boolean tHasVelocities = hasVelocity();
        @Nullable List<@Nullable String> tSymbols = symbols();
        return new SettableAtomData(
            NewCollections.map(atoms(), tHasVelocities ? (AtomFull::new) : (Atom::new)),
            atomTypeNumber(), box().copy(), tHasVelocities,
            tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR)
        );
    }
    /**
     * 用于 {@link IAtomDataOperation} 内部实现创建新的返回对象使用；
     * 重写来返回自定义的原子数据类型
     * @param aAtomNum 需要的原子数目
     * @param aBox 需要的模拟盒，默认为自身模拟盒的拷贝
     * @return 内部存储数据全为零的原子数据类型
     * @implSpec 一定需要返回可修改的原子数据类型
     * {@link ISettableAtomData}，并且要保证可以简单遍历修改属性
     */
    protected ISettableAtomData newZeros_(int aAtomNum, IBox aBox) {
        final boolean tHasVelocities = hasVelocity();
        @Nullable List<@Nullable String> tSymbols = symbols();
        return new SettableAtomData(
            NewCollections.from(aAtomNum, tHasVelocities ? (i -> new AtomFull()) : (i -> new Atom())),
            atomTypeNumber(), aBox, tHasVelocities,
            tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR)
        );
    }
    /**
     * {@code newZeros_(aAtomNum, box().copy())}
     * @see #newZeros_(int, IBox)
     */
    protected ISettableAtomData newZeros_(int aAtomNum) {return newZeros_(aAtomNum, box().copy());}
}
