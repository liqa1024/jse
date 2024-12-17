package jse.atom;

import jse.code.UT;
import jse.code.collection.AbstractRandomAccessList;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;

/**
 * 可以修改的原子数据抽象类，方便子类实现接口 {@link ISettableAtomData}
 * <p>
 * 具体来说，子类最少需要实现：
 * <pre>
 *    {@link #atom(int)}: 获取指定索引的可以设置的原子引用 {@link ISettableAtom}
 *    {@link #box()}: 获取模拟盒对象
 *    {@link #atomNumber()}: 获取原子总数
 *    {@link #atomTypeNumber()}: 获取总原子种类数目
 * </pre>
 * @see IAtomData IAtomData: 通用的原子数据接口
 * @see ISettableAtomData ISettableAtomData: 可以修改的原子数据接口
 * @see SettableAtomData SettableAtomData: 一般的可以修改的原子数据实现
 * @see AbstractAtomData AbstractAtomData: 一般的原子数据抽象类
 * @author liqa
 */
public abstract class AbstractSettableAtomData extends AbstractAtomData implements ISettableAtomData {
    /**
     * 直接获取指定索引的原子，可以避免一次创建匿名列表的过程；
     * 有关系 {@code atoms()[i] == atom(i)}
     * <p>
     * 由于返回的原子是引用，因此对其的修改会同时反应到原子数据内部：
     * <pre> {@code
     * def atom = data.atom(i)
     * atom.x = 3.14
     * assert data.atom(i).x() == 3.14
     * } </pre>
     *
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see ISettableAtom
     * @see AbstractSettableAtom_
     * @see SettableAtomData SettableAtomData: 关于具体实现的例子
     *
     * @implSpec 一般来说需要返回一个引用可设置的原子，并且保证内部的 {@link ISettableAtom#index()},
     * {@link ISettableAtom#hasVelocity()} 等方法调用到 {@link ISettableAtom} 自身；一般通过返回
     * {@link AbstractSettableAtom_} 的匿名类来实现：
     * <pre> {@code
     * @Override ISettableAtom atom(int idx) {
     *     // get reference of the stored atom
     *     def atom = atoms.get(idx)
     *     return new AbstractSettableAtom_() {
     *         @Override double x() {return atom.x()}
     *         @Override double y() {return atom.y()}
     *         @Override double z() {return atom.z()}
     *         @Override int id_() {return atom.id()}
     *         @Override int type_() {return atom.type()}
     *         @Override double vx_() {return atom.vx()}
     *         @Override double vy_() {return atom.vy()}
     *         @Override double vz_() {return atom.vz()}
     *         /// ISettableAtom stuffs
     *         @Override void setX_(double x) {atom.setX(x)}
     *         @Override void setY_(double y) {atom.setY(y)}
     *         @Override void setZ_(double z) {atom.setZ(z)}
     *         @Override void setID_(int id) {atom.setID(id)}
     *         @Override void setType_(int type) {atom.setType(type)}
     *         @Override void setVx_(double vx) {atom.setVx(vx)}
     *         @Override void setVy_(double vy) {atom.setVy(vy)}
     *         @Override void setVz_(double vz) {atom.setVz(vz)}
     *         // make sure index() returns the correct value
     *         @Override int index() {return idx}
     *     }
     * }
     * } </pre>
     */
    @Override public abstract ISettableAtom atom(int aIdx);
    
    /**
     * {@inheritDoc}
     * @param aAtomTypeNum {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @see #atomTypeNumber()
     * @see IAtom#type()
     * @see SettableAtomData SettableAtomData: 关于具体实现的例子
     */
    @Override public AbstractSettableAtomData setAtomTypeNumber(int aAtomTypeNum) {throw new UnsupportedOperationException("setAtomTypeNumber");}
    /**
     *{@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @see #hasVelocity()
     * @see #setHasVelocity()
     */
    @Override public AbstractSettableAtomData setNoVelocity() {throw new UnsupportedOperationException("setNoVelocity");}
    /**
     *{@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @see #hasVelocity()
     * @see #setNoVelocity()
     */
    @Override public AbstractSettableAtomData setHasVelocity() {throw new UnsupportedOperationException("setHasVelocity");}
    /**
     * {@inheritDoc}
     * @param aSymbols {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @see #symbols()
     * @see IAtom#symbol()
     * @see SettableAtomData SettableAtomData: 关于具体实现的例子
     */
    @Override public AbstractSettableAtomData setSymbols(String... aSymbols) {throw new UnsupportedOperationException("setSymbols");}
    /**
     * {@inheritDoc}
     * @see #setSymbols(String...)
     * @see Collection
     * @implSpec 需要调用 {@link #setSymbols(String...)} 或任何等价形式
     */
    @Override public AbstractSettableAtomData setSymbols(Collection<? extends CharSequence> aSymbols) {return setSymbols(UT.Text.toArray(aSymbols));}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @see #hasSymbol()
     * @see #setSymbols(String...)
     * @see SettableAtomData SettableAtomData: 关于具体实现的例子
     */
    @Override public AbstractSettableAtomData setNoSymbol() {throw new UnsupportedOperationException("setNoSymbol");}
    /**
     * {@inheritDoc}
     * @param aMasses {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @see #masses()
     * @see IAtom#mass()
     */
    @Override public AbstractSettableAtomData setMasses(double... aMasses) {throw new UnsupportedOperationException("setMasses");}
    /**
     * {@inheritDoc}
     * @see #setMasses(double...)
     * @see Collection
     * @implSpec 需要调用 {@link #setMasses(double...)} 或任何等价形式
     */
    @Override public AbstractSettableAtomData setMasses(Collection<? extends Number> aMasses) {return setMasses(Vectors.from(aMasses).internalData());}
    /**
     * {@inheritDoc}
     * @see #setMasses(double...)
     * @see IVector
     * @implSpec 需要调用 {@link #setMasses(double...)} 或任何等价形式
     */
    @Override public AbstractSettableAtomData setMasses(IVector aMasses) {return setMasses(aMasses.data());}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @see #hasMass()
     * @see #setMasses(double...)
     */
    @Override public AbstractSettableAtomData setNoMass() {throw new UnsupportedOperationException("setNoMass");}
    
    /**
     * 对于 {@link ISettableAtomData} 内部的原子的一个一般原子实现，帮助实现重复的部分；
     * 主要转发了 {@link IAtom#hasVelocity()}, {@link IAtom#symbol()},
     * {@link IAtom#hasSymbol()}, {@link IAtom#mass()} 以及 {@link IAtom#hasMass()}
     * 到相对应的 {@link ISettableAtomData} 内的方法；并且对于一些边界情况进行自动处理
     * @see #atom(int)
     */
    protected abstract class AbstractSettableAtom_ extends AbstractSettableAtom {
        /** 转发 {@link AbstractSettableAtomData#hasVelocity()} */
        @Override public boolean hasVelocity() {return AbstractSettableAtomData.this.hasVelocity();}
        /** 转发 {@link AbstractSettableAtomData#symbol(int)} */
        @Override public @Nullable String symbol() {return AbstractSettableAtomData.this.symbol(type());}
        /** 转发 {@link AbstractSettableAtomData#hasSymbol()} */
        @Override public boolean hasSymbol() {return AbstractSettableAtomData.this.hasSymbol();}
        /** 转发 {@link AbstractSettableAtomData#mass(int)} */
        @Override public double mass() {return AbstractSettableAtomData.this.mass(type());}
        /** 转发 {@link AbstractSettableAtomData#hasMass()} */
        @Override public boolean hasMass() {return AbstractSettableAtomData.this.hasMass();}
        
        /**
         * 为内部 id {@link #id_()} 包装一层检测 id
         * 是否存在，如果不存在则自动返回 {@link #index()}
         */
        @Override public int id() {int tID = id_(); return tID<=0 ? (index()+1) : tID;}
        /**
         * 为内部 type {@link #type_()} 包装一层检测 type
         * 是否会超过 {@link #atomTypeNumber()}，如果超过了则自动截断
         */
        @Override public int type() {return Math.min(type_(), atomTypeNumber());}
        
        /**
         * 为内部 vx {@link #vx_()} 包装一层检测是否存在确实速度
         * ({@link #hasVelocity()})，如果不存在则直接返回 {@code 0.0}
         */
        @Override public double vx() {return hasVelocity() ? vx_() : 0.0;}
        /**
         * 为内部 vy {@link #vy_()} 包装一层检测是否存在确实速度
         * ({@link #hasVelocity()})，如果不存在则直接返回 {@code 0.0}
         */
        @Override public double vy() {return hasVelocity() ? vy_() : 0.0;}
        /**
         * 为内部 vz {@link #vz_()} 包装一层检测是否存在确实速度
         * ({@link #hasVelocity()})，如果不存在则直接返回 {@code 0.0}
         */
        @Override public double vz() {return hasVelocity() ? vz_() : 0.0;}
        
        /** 为内部修改 x 值 {@link #setX_(double)} 包装一层返回自身 */
        @Override public ISettableAtom setX(double aX) {setX_(aX); return this;}
        /** 为内部修改 y 值 {@link #setY_(double)} 包装一层返回自身 */
        @Override public ISettableAtom setY(double aY) {setY_(aY); return this;}
        /** 为内部修改 z 值 {@link #setZ_(double)} 包装一层返回自身 */
        @Override public ISettableAtom setZ(double aZ) {setZ_(aZ); return this;}
        /** 为内部修改 id 值 {@link #setID_(int)} 包装一层返回自身 */
        @Override public ISettableAtom setID(int aID) {setID_(aID); return this;}
        /**
         * 为内部修改 type 值 {@link #setType_(int)} 包装一层返回自身，
         * 并且自动检测 aType 过大后调用 {@link #setAtomTypeNumber(int)}
         * 自动调整原子种类数目
         */
        @Override public ISettableAtom setType(int aType) {
            // 对于设置种类需要特殊处理，设置种类同时需要更新内部的原子种类计数
            if (aType > atomTypeNumber()) setAtomTypeNumber(aType);
            setType_(aType);
            return this;
        }
        /**
         * 为内部修改 vx 值 {@link #setVx_(double)} 包装一层检测是否存在确实速度
         * ({@link #hasVelocity()})，如果不存在则直接抛出错误 {@link UnsupportedOperationException}
         */
        @Override public ISettableAtom setVx(double aVx) {if (!hasVelocity()) throw new UnsupportedOperationException("setVx"); setVx_(aVx); return this;}
        /**
         * 为内部修改 vy 值 {@link #setVy_(double)} 包装一层检测是否存在确实速度
         * ({@link #hasVelocity()})，如果不存在则直接抛出错误 {@link UnsupportedOperationException}
         */
        @Override public ISettableAtom setVy(double aVy) {if (!hasVelocity()) throw new UnsupportedOperationException("setVy"); setVy_(aVy); return this;}
        /**
         * 为内部修改 vz 值 {@link #setVz_(double)} 包装一层检测是否存在确实速度
         * ({@link #hasVelocity()})，如果不存在则直接抛出错误 {@link UnsupportedOperationException}
         */
        @Override public ISettableAtom setVz(double aVz) {if (!hasVelocity()) throw new UnsupportedOperationException("setVz"); setVz_(aVz); return this;}
        
        /// stuff to override
        /** 可以直接实现的返回内部 id 值 */
        protected abstract int id_();
        /** 可以直接实现的返回内部 type 值 */
        protected abstract int type_();
        /** 可以直接实现的返回内部 vx 值 */
        protected double vx_() {return 0.0;}
        /** 可以直接实现的返回内部 vy 值 */
        protected double vy_() {return 0.0;}
        /** 可以直接实现的返回内部 vz 值 */
        protected double vz_() {return 0.0;}
        /** 可以直接实现的修改内部 x 值 */
        protected abstract void setX_(double aX);
        /** 可以直接实现的修改内部 y 值 */
        protected abstract void setY_(double aY);
        /** 可以直接实现的修改内部 z 值 */
        protected abstract void setZ_(double aZ);
        /** 可以直接实现的修改内部 id 值 */
        protected abstract void setID_(int aID);
        /** 可以直接实现的修改内部 type 值 */
        protected abstract void setType_(int aType);
        /** 可以直接实现的修改内部 vx 值 */
        protected void setVx_(double aVx) {throw new RuntimeException();}
        /** 可以直接实现的修改内部 vy 值 */
        protected void setVy_(double aVy) {throw new RuntimeException();}
        /** 可以直接实现的修改内部 vz 值 */
        protected void setVz_(double aVz) {throw new RuntimeException();}
        /** 对于 {@link IAtomData} 内部的原子一定要复写掉内部的 index 数据 */
        @Override public abstract int index();
    }
    
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @param aAtom {@inheritDoc}
     * @see #atoms()
     * @see #atom(int)
     */
    @Override public void setAtom(int aIdx, IAtom aAtom) {
        ISettableAtom tAtom = this.atom(aIdx);
        tAtom.setXYZ(aAtom).setID(aAtom.id()).setType(aAtom.type());
        if (aAtom.hasVelocity()) tAtom.setVxyz(aAtom.vx(), aAtom.vy(), aAtom.vz());
    }
    
    /**
     * 对于 {@link ISettableAtomData}，这里会获取到一个可以设置的原子对象
     * {@link ISettableAtom}，由于返回的原子是引用，因此对其的修改会同时反应到原子数据内部。
     * <p>
     * 这里返回的列表本身同样也是一个引用对象，对列表的修改也会反应到原子数据内部，即
     * {@code this.atoms().set(idx, atom)} 和 {@code this.setAtom(idx, atom)}
     * 操作等价
     *
     * @return {@inheritDoc}
     * @see ISettableAtom
     * @see #atom(int)
     * @see #setAtom(int, IAtom)
     */
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
    // 需要这里重新实现一下两者共有的接口避免冲突
    /** @deprecated use {@link #atoms()} */
    @Override @Deprecated @SuppressWarnings("deprecation") public List<? extends ISettableAtom> asList() {return atoms();}
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtomDataOperation
     * @see ISettableAtomDataOperation
     */
    @Override public ISettableAtomDataOperation operation() {return new AbstractSettableAtomDataOperation() {
        @Override protected ISettableAtomData thisAtomData_() {return AbstractSettableAtomData.this;}
        @Override protected ISettableAtomData newSameSettableAtomData_() {return newSame_();}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum) {return newZeros_(aAtomNum);}
        @Override protected ISettableAtomData newSettableAtomData_(int aAtomNum, IBox aBox) {return newZeros_(aAtomNum, aBox);}
    };}
    // 需要这里重新实现一下两者共有的接口避免冲突
    /** @see #operation() */
    @VisibleForTesting @Override public ISettableAtomDataOperation opt() {return operation();}
}
