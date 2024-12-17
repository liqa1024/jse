package jse.atom;

import jse.code.CS;
import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;

/**
 * 可以设置的原子数据接口，此接口开放了更多修改的的接口
 * <p>
 * 对于一般的使用，则可以通过 {@link #atom(int)} 以及 {@link #atoms()}
 * 获取的原子类型为 {@link ISettableAtom}，从而可以修改具体原子属性
 *
 * @see IAtomData IAtomData: 通用的原子数据接口
 * @see SettableAtomData SettableAtomData: 可修改的原子数据的默认实现
 * @see ISettableAtom ISettableAtom: 可以修改的原子接口
 * @author liqa
 */
public interface ISettableAtomData extends IAtomData {
    /// IAtomData stuffs
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
    List<? extends ISettableAtom> atoms();
    /** @deprecated use {@link #atoms()} */
    @Override @Deprecated @SuppressWarnings("deprecation") default List<? extends ISettableAtom> asList() {return atoms();}
    /**
     * {@inheritDoc}
     * <p>
     * 由于返回的原子是引用，因此对其的修改会同时反应到原子数据内部：
     * <pre> {@code
     * def atom = data.atom(i)
     * atom.x = 3.14
     * assert data.atom(i).x() == 3.14
     * } </pre>
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see ISettableAtom
     * @see #atoms()
     */
    ISettableAtom atom(int aIdx);
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtomDataOperation
     * @see ISettableAtomDataOperation
     */
    ISettableAtomDataOperation operation();
    /** @see #operation() */
    @VisibleForTesting default ISettableAtomDataOperation opt() {return operation();}
    
    /**
     * 直接修改指定索引位置的原子，会读取输入原子的属性然后进行设置
     * <p>
     * 相对于 {@code atoms().set(aIdx, aAtom)}
     * 可以避免创建一个匿名列表
     *
     * @param aIdx 需要设置的原子索引
     * @param aAtom 输入的任意原子
     * @see #atoms()
     * @see #atom(int)
     */
    void setAtom(int aIdx, IAtom aAtom);
    
    /**
     * 设置此原子数据的总原子种类数目，可以大于实际的原子种类数目，
     * 当设置小于实际原子种类数目时，内部大于种类数目的原子种类编号都会截断到设置的种类数目。
     *
     * @param aAtomTypeNum 需要的原子总类数目
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置总原子种类数目操作
     * @see #atomTypeNumber()
     * @see IAtom#type()
     */
    ISettableAtomData setAtomTypeNumber(int aAtomTypeNum);
    /** @deprecated use {@link #setAtomTypeNumber(int)} */
    @Deprecated default ISettableAtomData setAtomTypeNum(int aAtomTypeNum) {return setAtomTypeNumber(aAtomTypeNum);}
    
    /**
     * 设置此原子数据不再包含速度信息，在调用过后
     * {@link #hasVelocity()} 总是会返回 {@code false}
     * <p>
     * 调用 {@link #setHasVelocity()} 来重新拥有速度信息
     *
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持此操作
     * @see #hasVelocity()
     * @see #setHasVelocity()
     */
    ISettableAtomData setNoVelocity();
    /** @deprecated use {@link #setNoVelocity()} */
    @Deprecated default ISettableAtomData setNoVelocities() {return setNoVelocity();}
    /**
     * 设置此原子数据会包含速度信息，在调用过后
     * {@link #hasVelocity()} 总是会返回 {@code true}
     * <p>
     * 调用 {@link #setNoVelocity()} 移除速度信息
     *
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持此操作
     * @see #hasVelocity()
     * @see #setNoVelocity()
     */
    ISettableAtomData setHasVelocity();
    /** @deprecated use {@link #setHasVelocity()} */
    @Deprecated default ISettableAtomData setHasVelocities() {return setHasVelocity();}
    
    /**
     * 设置此原子数据的元素符号，按照元素编号排序进行设置（和
     * {@link #symbols()} 返回值保持一致）
     * <p>
     * 一般来说，如果设置数量少于 {@link #ntypes()}
     * 不会对其余编号产生影响，如果大于 {@link #ntypes()}
     * 则会同时设置总原子种类数目
     * <p>
     * 如果恰好是元素周期表中的元素，会同时按照
     * {@link CS#MASS} 更新其质量
     *
     * @param aSymbols 需要设置的元素符号数组
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置元素符号操作
     * @see #symbols()
     * @see IAtom#symbol()
     */
    ISettableAtomData setSymbols(String... aSymbols);
    /**
     * 传入列表形式元素符号的设置元素符号实现
     * @see #setSymbols(String...)
     * @see Collection
     */
    ISettableAtomData setSymbols(Collection<? extends CharSequence> aSymbols);
    /**
     * 设置此原子数据不再包含元素符号信息，在调用过后
     * {@link #hasSymbol()} 总是会返回 {@code false}
     * <p>
     * 调用 {@link #setSymbols(String...)}
     * 来重新拥有元素符号信息
     *
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持此操作
     * @see #hasSymbol()
     * @see #setSymbols(String...)
     */
    ISettableAtomData setNoSymbol();
    
    /**
     * 设置此原子数据的原子质量，按照元素编号排序进行设置（和
     * {@link #masses()} 返回值保持一致）
     * <p>
     * 一般来说，如果设置数量少于 {@link #ntypes()}
     * 不会对其余编号产生影响，如果大于 {@link #ntypes()}
     * 则会同时设置总原子种类数目
     * <p>
     * 一般来说，对于内部直接存储元素符号的原子数据，并不支持直接设置原子质量，
     * 对于直接存储质量的数据（如 lammps data 类型 {@link jse.lmp.Lmpdat}）
     * 则会检测其质量是否接近元素周期表中的质量，与此同时按照
     * {@link CS#MASS} 更新其元素符号
     *
     * @param aMasses 需要设置的质量数组
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置原子质量操作
     * @see #masses()
     * @see IAtom#mass()
     */
    ISettableAtomData setMasses(double... aMasses);
    /**
     * 传入列表形式质量的设置质量实现
     * @see #setMasses(double...)
     * @see Collection
     */
    ISettableAtomData setMasses(Collection<? extends Number> aMasses);
    /**
     * 传入 jse 向量形式质量的设置质量实现
     * @see #setMasses(double...)
     * @see IVector
     */
    ISettableAtomData setMasses(IVector aMasses);
    /**
     * 设置此原子数据不再包含质量信息，在调用过后
     * {@link #hasMass()} 总是会返回 {@code false}
     * <p>
     * 调用 {@link #setMasses} 来重新拥有质量信息
     *
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持此操作
     * @see #hasMass()
     * @see #setMasses(double...)
     */
    ISettableAtomData setNoMass();
    
    
    /// Groovy stuffs
    /** @see #atomTypeNumber() */
    @VisibleForTesting default int getAtomTypeNumber() {return atomTypeNumber();}
    /** @see #symbols() */
    @VisibleForTesting default @Nullable List<@Nullable String> getSymbols() {return symbols();}
    /** @see #masses() */
    @VisibleForTesting default @Nullable IVector getMasses() {return masses();}
}
