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
    @Deprecated @SuppressWarnings("deprecation") default List<? extends ISettableAtom> asList() {return atoms();}
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
    @VisibleForTesting default ISettableAtomDataOperation op() {return operation();}
    
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
     * @param aNumTypes 需要的原子总类数目
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置总原子种类数目操作
     * @see #ntypes()
     * @see IAtom#type()
     */
    ISettableAtomData setNtypes(int aNumTypes);
    
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBox(boolean, double, double, double)}
     * <p>
     * 此方法会直接抹去倾斜数据，直接让原子数据的模拟盒强制变为正交的；
     * 如果希望保留倾斜数据，则调用 {@link #setBoxXYZ(double, double, double)}；
     * 如果只希望抹去倾斜数据，则调用 {@link #setBoxNormal()}
     *
     * @param aX 模拟盒 x 方向边长
     * @param aY 模拟盒 y 方向边长
     * @param aZ 模拟盒 z 方向边长
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBox(double aX, double aY, double aZ) {return setBox(false, aX, aY, aZ);}
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 此方法会直接抹去倾斜数据，直接让原子数据的模拟盒强制变为正交的；
     * 如果希望保留倾斜数据，则调用 {@link #setBoxXYZ(boolean, double, double, double)}；
     * 如果只希望抹去倾斜数据，则调用 {@link #setBoxNormal(boolean)}
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aX 模拟盒 x 方向边长
     * @param aY 模拟盒 y 方向边长
     * @param aZ 模拟盒 z 方向边长
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBox(boolean aKeepAtomPosition, double aX, double aY, double aZ);
    
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBox(boolean, IXYZ, IXYZ, IXYZ)}
     * <p>
     * 此方法会直接让原子数据的模拟盒变为三斜的；
     * 如果希望保留 xyz 数据，则调用 {@link #setBoxPrism(boolean, double, double, double, double, double, double)}；
     * 如果只希望将模拟盒设置成三斜的，则调用 {@link #setBoxPrism()}
     * <p>
     * 根据实现的不同，实际的模拟盒属性值可能会不同，但应当是等价的
     *
     * @param aA 模拟盒第一个基向量
     * @param aB 模拟盒第二个基向量
     * @param aC 模拟盒第三个基向量
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBox(IXYZ aA, IXYZ aB, IXYZ aC) {return setBox(false, aA, aB, aC);}
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 此方法会直接让原子数据的模拟盒变为三斜的；
     * 如果希望保留 xyz 数据，则调用 {@link #setBoxPrism(boolean, double, double, double, double, double, double)}；
     * 如果只希望将模拟盒设置成三斜的，则调用 {@link #setBoxPrism()}
     * <p>
     * 根据实现的不同，实际的模拟盒属性值可能会不同，但应当是等价的
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aA 模拟盒第一个基向量
     * @param aB 模拟盒第二个基向量
     * @param aC 模拟盒第三个基向量
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBox(boolean aKeepAtomPosition, IXYZ aA, IXYZ aB, IXYZ aC);
    
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBox(boolean, double, double, double, double, double, double)}
     * <p>
     * 此方法会直接让原子数据的模拟盒变为三斜的；
     * 如果希望保留 xyz 数据，则调用 {@link #setBoxPrism(double, double, double)}；
     * 如果只希望将模拟盒设置成三斜的，则调用 {@link #setBoxPrism()}
     *
     * @param aX 模拟盒 x 长度
     * @param aY 模拟盒 y 长度
     * @param aZ 模拟盒 z 长度
     * @param aXY 模拟盒 xy 倾斜因子，对应 {@link IBox#bx()}
     * @param aXZ 模拟盒 xz 倾斜因子，对应 {@link IBox#cx()}
     * @param aYZ 模拟盒 yz 倾斜因子，对应 {@link IBox#cy()}
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBox(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {return setBox(false, aX, aY, aZ, aXY, aXZ, aYZ);}
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 此方法会直接让原子数据的模拟盒变为三斜的；
     * 如果希望保留 xyz 数据，则调用 {@link #setBoxPrism(boolean, double, double, double)}；
     * 如果只希望将模拟盒设置成三斜的，则调用 {@link #setBoxPrism()}
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aX 模拟盒 x 长度
     * @param aY 模拟盒 y 长度
     * @param aZ 模拟盒 z 长度
     * @param aXY 模拟盒 xy 倾斜因子，对应 {@link IBox#bx()}
     * @param aXZ 模拟盒 xz 倾斜因子，对应 {@link IBox#cx()}
     * @param aYZ 模拟盒 yz 倾斜因子，对应 {@link IBox#cy()}
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBox(boolean aKeepAtomPosition, double aX, double aY, double aZ, double aXY, double aXZ, double aYZ);
    
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBox(boolean, double, double, double, double, double, double, double, double, double)}
     * <p>
     * 此方法会直接让原子数据的模拟盒变为三斜的；
     * 如果希望保留 xyz 数据，则调用 {@link #setBoxPrism(double, double, double, double, double, double)}；
     * 如果只希望将模拟盒设置成三斜的，则调用 {@link #setBoxPrism()}
     * <p>
     * 根据实现的不同，实际的模拟盒属性值可能会不同，但应当是等价的
     *
     * @param aAx 模拟盒第一个基向量的 x 方向
     * @param aAy 模拟盒第一个基向量的 y 方向
     * @param aAz 模拟盒第一个基向量的 z 方向
     * @param aBx 模拟盒第二个基向量的 x 方向
     * @param aBy 模拟盒第二个基向量的 y 方向
     * @param aBz 模拟盒第二个基向量的 z 方向
     * @param aCx 模拟盒第三个基向量的 x 方向
     * @param aCy 模拟盒第三个基向量的 y 方向
     * @param aCz 模拟盒第三个基向量的 z 方向
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBox(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {return setBox(false, aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);}
    /**
     * 设置此原子数据的模拟盒属性
     * <p>
     * 此方法会直接让原子数据的模拟盒变为三斜的；
     * 如果希望保留 xyz 数据，则调用 {@link #setBoxPrism(boolean, double, double, double, double, double, double)}；
     * 如果只希望将模拟盒设置成三斜的，则调用 {@link #setBoxPrism()}
     * <p>
     * 根据实现的不同，实际的模拟盒属性值可能会不同，但应当是等价的
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aAx 模拟盒第一个基向量的 x 方向
     * @param aAy 模拟盒第一个基向量的 y 方向
     * @param aAz 模拟盒第一个基向量的 z 方向
     * @param aBx 模拟盒第二个基向量的 x 方向
     * @param aBy 模拟盒第二个基向量的 y 方向
     * @param aBz 模拟盒第二个基向量的 z 方向
     * @param aCx 模拟盒第三个基向量的 x 方向
     * @param aCy 模拟盒第三个基向量的 y 方向
     * @param aCz 模拟盒第三个基向量的 z 方向
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBox(boolean aKeepAtomPosition, double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz);
    
    /**
     * 通过给定的模拟盒来设置此原子数据的模拟盒属性，会读取输入然后进行一次值拷贝，从而避免引用
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBox(boolean, IBox)}
     *
     * @param aBox 输入的任意模拟盒
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBox(IBox aBox) {return setBox(false, aBox);}
    /**
     * 通过给定的模拟盒来设置此原子数据的模拟盒属性，会读取输入然后进行一次值拷贝，从而避免引用
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aBox 输入的任意模拟盒
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBox(boolean aKeepAtomPosition, IBox aBox);
    
    /**
     * 设置此原子数据的模拟盒为正交的，会直接抹去倾斜数据并让
     * {@link #isPrism()} 返回 {@code false}，如果已经是正交的则不进行任何操作
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBoxNormal(boolean)}
     *
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBoxNormal() {return setBoxNormal(false);}
    /**
     * 设置此原子数据的模拟盒为正交的，会直接抹去倾斜数据并让
     * {@link #isPrism()} 返回 {@code false}，如果已经是正交的则不进行任何操作
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBoxNormal(boolean aKeepAtomPosition);
    
    /**
     * 设置此原子数据的模拟盒为三斜的，即让 {@link #isPrism()} 返回
     * {@code true}，如果已经是三斜的则不进行任何操作
     *
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBoxPrism();
    
    /**
     * 设置此原子数据的模拟盒为三斜的，并设置对应的倾斜因子（不会修改 xyz 的数据），让
     * {@link #isPrism()} 返回 {@code true}
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBoxPrism(boolean, double, double, double)}
     *
     * @param aXY 模拟盒 xy 倾斜因子，对应 {@link IBox#bx()}
     * @param aXZ 模拟盒 xz 倾斜因子，对应 {@link IBox#cx()}
     * @param aYZ 模拟盒 yz 倾斜因子，对应 {@link IBox#cy()}
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBoxPrism(double aXY, double aXZ, double aYZ) {return setBoxPrism(false, aXY, aXZ, aYZ);}
    /**
     * 设置此原子数据的模拟盒为三斜的，并设置对应的倾斜因子（不会修改 xyz 的数据），让
     * {@link #isPrism()} 返回 {@code true}
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aXY 模拟盒 xy 倾斜因子，对应 {@link IBox#bx()}
     * @param aXZ 模拟盒 xz 倾斜因子，对应 {@link IBox#cx()}
     * @param aYZ 模拟盒 yz 倾斜因子，对应 {@link IBox#cy()}
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBoxPrism(boolean aKeepAtomPosition, double aXY, double aXZ, double aYZ);
    /**
     * 设置此原子数据的模拟盒为三斜的，并设置对应的倾斜因子（不会修改 xyz 的数据），让
     * {@link #isPrism()} 返回 {@code true}
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBoxPrism(boolean, double, double, double, double, double, double)}
     * <p>
     * 根据实现的不同，实际的模拟盒属性值可能会不同，但应当是等价的
     *
     * @param aAy 模拟盒第一个基向量的 y 方向
     * @param aAz 模拟盒第一个基向量的 z 方向
     * @param aBx 模拟盒第二个基向量的 x 方向
     * @param aBz 模拟盒第二个基向量的 z 方向
     * @param aCx 模拟盒第三个基向量的 x 方向
     * @param aCy 模拟盒第三个基向量的 y 方向
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBoxPrism(double aAy, double aAz, double aBx, double aBz, double aCx, double aCy) {return setBoxPrism(false, aAy, aAz, aBx, aBz, aCx, aCy);}
    /**
     * 设置此原子数据的模拟盒为三斜的，并设置对应的倾斜因子（不会修改 xyz 的数据），让
     * {@link #isPrism()} 返回 {@code true}
     * <p>
     * 根据实现的不同，实际的模拟盒属性值可能会不同，但应当是等价的
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aAy 模拟盒第一个基向量的 y 方向
     * @param aAz 模拟盒第一个基向量的 z 方向
     * @param aBx 模拟盒第二个基向量的 x 方向
     * @param aBz 模拟盒第二个基向量的 z 方向
     * @param aCx 模拟盒第三个基向量的 x 方向
     * @param aCy 模拟盒第三个基向量的 y 方向
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBoxPrism(boolean aKeepAtomPosition, double aAy, double aAz, double aBx, double aBz, double aCx, double aCy);
    
    /**
     * 只设置此原子数据的模拟盒的 xyz 三个方向的长度，不会修改倾斜的数据（如果有的话）
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBoxXYZ(boolean, double, double, double)}
     *
     * @param aX 模拟盒 x 长度
     * @param aY 模拟盒 y 长度
     * @param aZ 模拟盒 z 长度
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBoxXYZ(double aX, double aY, double aZ) {return setBoxXYZ(false, aX, aY, aZ);}
    /**
     * 只设置此原子数据的模拟盒的 xyz 三个方向的长度，不会修改倾斜的数据（如果有的话）
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aX 模拟盒 x 长度
     * @param aY 模拟盒 y 长度
     * @param aZ 模拟盒 z 长度
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBoxXYZ(boolean aKeepAtomPosition, double aX, double aY, double aZ);
    
    /**
     * 只设置此原子数据的模拟盒的缩放比例，现在无论何种实现都会完全应用缩放，
     * 进而重复设置会进一步缩放（保证行为一致）
     * <p>
     * 默认会同步拉伸内部原子位置，如果需要保持原子位置不同，则需要调用
     * {@link #setBoxScale(boolean, double)}
     *
     * @param aScale 需要设置的缩放比例
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    default ISettableAtomData setBoxScale(double aScale) {return setBoxScale(false, aScale);}
    /**
     * 只设置此原子数据的模拟盒的缩放比例，现在无论何种实现都会完全应用缩放，
     * 进而重复设置会进一步缩放（保证行为一致）
     *
     * @param aKeepAtomPosition 是否保持原子的位置不随模拟盒的设置而拉伸，默认为 {@code false}
     * @param aScale 需要设置的缩放比例
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置模拟盒操作
     * @see IBox
     */
    ISettableAtomData setBoxScale(boolean aKeepAtomPosition, double aScale);
    
    
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
    
    /**
     * 设置此原子数据元素符号排列顺序，从而调整元素符号对应的 {@code type} 值
     * <p>
     * 一般来说，如果设置数量少于 {@link #ntypes()} 则其余种类会按照旧值排序，
     * 如果大于 {@link #ntypes()} 则会同时设置总原子种类数目
     * <p>
     * 这里总是会进行去重并忽略相同的键，即使对于支持相同 symbol 的类型。这是因为相同符号在
     * symbol order 中不存在准确语义
     *
     * @param aSymbolOrder 需要设置的元素符号数组
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 如果原子数据不支持设置元素符号操作
     * @see #setSymbols(String...)
     */
    ISettableAtomData setSymbolOrder(String... aSymbolOrder);
    /**
     * 传入列表形式元素符号的设置顺序实现
     * @see #setSymbolOrder(String...)
     * @see Collection
     */
    ISettableAtomData setSymbolOrder(Collection<? extends CharSequence> aSymbolOrder);
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
    /** @see #ntypes() */
    @VisibleForTesting default int getNtypes() {return ntypes();}
    /** @see #box() */
    @VisibleForTesting default IBox getBox() {return box();}
    /** @see #symbols() */
    @VisibleForTesting default @Nullable List<@Nullable String> getSymbols() {return symbols();}
    /** @see #masses() */
    @VisibleForTesting default @Nullable IVector getMasses() {return masses();}
}
