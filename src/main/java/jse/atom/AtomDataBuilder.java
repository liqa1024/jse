package jse.atom;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

/**
 * 用于方便构造 {@link AtomData} 或 {@link SettableAtomData} 的一个构造器，通过
 * {@link #addAtom} 来添加一个原子，然后通过 {@link #build()}
 * 来返回一个构造完成的原子数据
 * <p>
 * 使用构造器可以简化原子数据的创建过程，并且可以避免可能存在的引用问题
 *
 * @author liqa
 * @see AtomData#builder()
 * @see SettableAtomData#builder()
 */
public abstract class AtomDataBuilder<R> {
    private ArrayList<ISettableAtom> mAtoms;
    private int mAtomTypeNum = 1;
    private IBox mBox = null;
    private boolean mHasID = false;
    private boolean mHasVelocity = false;
    private String @Nullable[] mSymbols = null;
    
    AtomDataBuilder() {mAtoms = new ArrayList<>();}
    AtomDataBuilder(int aInitSize) {mAtoms = new ArrayList<>(aInitSize);}
    
    /// stuff to override
    abstract R newAtomData(List<ISettableAtom> aAtoms, int aAtomTypeNum, IBox aBox, boolean aHasID, boolean aHasVelocity, String[] aSymbols);
    
    /**
     * 构造一个 {@link AtomData} 或 {@link SettableAtomData}，调用后此构造器便会失效
     * @return 构造完成的 {@link AtomData} 或 {@link SettableAtomData}
     */
    public R build() {
        // 需要将内部的 mAtoms, mBox, mSymbols 制空来避免构造后的修改
        ArrayList<ISettableAtom> aAtoms = Objects.requireNonNull(mAtoms);
        mAtoms = null;
        IBox aBox = mBox;
        mBox = null;
        String[] aSymbols = mSymbols;
        mSymbols = null;
        // 自动设置 box
        if (aBox == null) {
            if (aAtoms.isEmpty()) {
                aBox = new Box(0.0, 0.0, 0.0);
            } else {
                double tMaxX = Double.NEGATIVE_INFINITY, tMinX = 0.0;
                double tMaxY = Double.NEGATIVE_INFINITY, tMinY = 0.0;
                double tMaxZ = Double.NEGATIVE_INFINITY, tMinZ = 0.0;
                for (IAtom tAtom : aAtoms) {
                    double tX = tAtom.x();
                    if (tX > tMaxX) tMaxX = tX;
                    if (tX < tMinX) tMinX = tX;
                    double tY = tAtom.y();
                    if (tY > tMaxY) tMaxY = tY;
                    if (tY < tMinY) tMinY = tY;
                    double tZ = tAtom.z();
                    if (tZ > tMaxZ) tMaxZ = tZ;
                    if (tZ < tMinZ) tMinZ = tZ;
                }
                aBox = new Box(tMaxX-tMinX, tMaxY-tMinY, tMaxZ-tMinZ);
            }
        }
        // 填充 symbols 中的 null
        if (aSymbols != null) {
            for (int typeMM = 0; typeMM < aSymbols.length; ++typeMM) {
                if (aSymbols[typeMM] == null) aSymbols[typeMM] = "T"+(typeMM+1);
            }
        }
        // 构造 AtomData
        return newAtomData(aAtoms, mAtomTypeNum, aBox, mHasID, mHasVelocity, aSymbols);
    }
    
    /**
     * 添加一个拥有速度信息的原子
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aID 原子的 id
     * @param aType 原子的种类编号
     * @param aVx 原子的 x 方向速度值
     * @param aVy 原子的 y 方向速度值
     * @param aVz 原子的 z 方向速度值
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtom(double aX, double aY, double aZ, int aID, int aType, double aVx, double aVy, double aVz) {
        if (aID > 0) mHasID = true;
        mHasVelocity = true;
        if (mAtomTypeNum < aType) mAtomTypeNum = aType;
        mAtoms.add(new AtomFull(aX, aY, aZ, aID, aType, aVx, aVy, aVz));
        return this;
    }
    /**
     * 添加一个拥有 id 信息的原子
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aID 原子的 id
     * @param aType 原子的种类编号
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtom(double aX, double aY, double aZ, int aID, int aType) {
        if (aID > 0) mHasID = true;
        if (mAtomTypeNum < aType) mAtomTypeNum = aType;
        mAtoms.add(new AtomID(aX, aY, aZ, aID, aType));
        return this;
    }
    /**
     * 添加一个原子
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aType 原子的种类编号
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtom(double aX, double aY, double aZ, int aType) {
        if (mAtomTypeNum < aType) mAtomTypeNum = aType;
        mAtoms.add(new Atom(aX, aY, aZ, aType));
        return this;
    }
    /**
     * 添加一个不含种类信息的原子，{@code type==1}
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtom(double aX, double aY, double aZ) {
        return addAtom(aX, aY, aZ, 1);
    }
    /**
     * 添加一个拥有 id 以及元素符号信息的原子
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aID 原子的 id
     * @param aSymbol 原子的元素符号
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtom(double aX, double aY, double aZ, int aID, String aSymbol) {
        if (aID > 0) mHasID = true;
        mAtoms.add(new AtomID(aX, aY, aZ, aID, validSymbolAndGetType(aSymbol)));
        return this;
    }
    /**
     * 添加一个拥有元素符号信息的原子
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aSymbol 原子的元素符号
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtom(double aX, double aY, double aZ, String aSymbol) {
        mAtoms.add(new Atom(aX, aY, aZ, validSymbolAndGetType(aSymbol)));
        return this;
    }
    /**
     * 直接添加一个原子，会获取此原子的信息进行值拷贝，从而避免引用
     * @param aAtom 需要添加的任意原子
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtom(IAtom aAtom) {
        int tType = aAtom.type();
        if (aAtom.hasSymbol()) {
            tType = validSymbolAndGetType(aAtom.symbol(), tType);
        }
        if (aAtom.hasVelocity()) {
            return addAtom(aAtom.x(), aAtom.y(), aAtom.z(), aAtom.id(), tType, aAtom.vx(), aAtom.vy(), aAtom.vz());
        } else
        if (aAtom.hasID()) {
            return addAtom(aAtom.x(), aAtom.y(), aAtom.z(), aAtom.id(), tType);
        } else {
            return addAtom(aAtom.x(), aAtom.y(), aAtom.z(), tType);
        }
    }
    /**
     * 批量添加多个原子，对于每个原子都会获取信息并值拷贝，从而避免引用
     * @param aAtomList 输入的原子列表
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> addAtomList(Collection<? extends IAtom> aAtomList) {
        mAtoms.ensureCapacity(mAtoms.size() + aAtomList.size());
        for (IAtom aAtom : aAtomList) addAtom(aAtom);
        return this;
    }
    
    private int validSymbolAndGetType(String aSymbol) {return validSymbolAndGetType(aSymbol, 1);}
    private int validSymbolAndGetType(String aSymbol, int aExpectedType) {
        if (mSymbols == null) {
            if (mAtomTypeNum < aExpectedType) mAtomTypeNum = aExpectedType;
            mSymbols = new String[mAtomTypeNum];
            mSymbols[mAtomTypeNum-1] = aSymbol;
            return mAtomTypeNum;
        }
        // 如果已经存在相同元素，则不考虑 aExpectedType；
        // 注意需要考虑 mSymbols 比 mAtomTypeNum 还要长的情况
        int tType = IAtomData.typeOf_(AbstractCollections.from(mAtomTypeNum, i->mSymbols[i]), aSymbol);
        if (tType > 0) {
            return tType;
        }
        // 如果 aExpectedType 部分为 null 则设置 symbol
        if (aExpectedType<=mAtomTypeNum && mSymbols[aExpectedType-1]==null) {
            mSymbols[aExpectedType-1] = aSymbol;
            return aExpectedType;
        }
        // 此时无论如何也不能塞进去了，直接扩容
        mAtomTypeNum = Math.max(mAtomTypeNum+1, aExpectedType);
        if (mSymbols.length < mAtomTypeNum) {
            String[] oSymbols = mSymbols;
            mSymbols = new String[mAtomTypeNum];
            System.arraycopy(oSymbols, 0, mSymbols, 0, oSymbols.length);
        }
        mSymbols[mAtomTypeNum-1] = aSymbol;
        return mAtomTypeNum;
    }
    
    /**
     * 使用 {@link IXYZ} 版本的添加原子
     * @see #addAtom(double, double, double, int, int, double, double, double)
     */
    public AtomDataBuilder<R> addAtom(IXYZ aXYZ, int aID, int aType, IXYZ aVxyz) {return addAtom(aXYZ.x(), aXYZ.y(), aXYZ.z(), aID, aType, aVxyz.x(), aVxyz.y(), aVxyz.z());}
    /**
     * 使用 {@link IXYZ} 版本的添加原子
     * @see #addAtom(double, double, double, int, int)
     */
    public AtomDataBuilder<R> addAtom(IXYZ aXYZ, int aID, int aType) {return addAtom(aXYZ.x(), aXYZ.y(), aXYZ.z(), aID, aType);}
    /**
     * 使用 {@link IXYZ} 版本的添加原子
     * @see #addAtom(double, double, double, int)
     */
    public AtomDataBuilder<R> addAtom(IXYZ aXYZ, int aType) {return addAtom(aXYZ.x(), aXYZ.y(), aXYZ.z(), aType);}
    /**
     * 使用 {@link IXYZ} 版本的添加原子
     * @see #addAtom(double, double, double)
     */
    public AtomDataBuilder<R> addAtom(IXYZ aXYZ) {return addAtom(aXYZ.x(), aXYZ.y(), aXYZ.z());}
    /**
     * 使用 {@link IXYZ} 版本的添加原子
     * @see #addAtom(double, double, double, int, String)
     */
    public AtomDataBuilder<R> addAtom(IXYZ aXYZ, int aID, String aSymbol) {return addAtom(aXYZ.x(), aXYZ.y(), aXYZ.z(), aID, aSymbol);}
    /**
     * 使用 {@link IXYZ} 版本的添加原子
     * @see #addAtom(double, double, double, String)
     */
    public AtomDataBuilder<R> addAtom(IXYZ aXYZ, String aSymbol) {return addAtom(aXYZ.x(), aXYZ.y(), aXYZ.z(), aSymbol);}
    
    /// groovy stuff
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(double, double, double, int, int, double, double, double)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(double aX, double aY, double aZ, int aID, int aType, double aVx, double aVy, double aVz) {return addAtom(aX, aY, aZ, aID, aType, aVx, aVy, aVz);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(double, double, double, int, int)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(double aX, double aY, double aZ, int aID, int aType) {return addAtom(aX, aY, aZ, aID, aType);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(double, double, double, int)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(double aX, double aY, double aZ, int aType) {return addAtom(aX, aY, aZ, aType);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(double, double, double)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(double aX, double aY, double aZ) {return addAtom(aX, aY, aZ);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(double, double, double, int, String)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(double aX, double aY, double aZ, int aID, String aSymbol) {return addAtom(aX, aY, aZ, aID, aSymbol);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(double, double, double, String)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(double aX, double aY, double aZ, String aSymbol) {return addAtom(aX, aY, aZ, aSymbol);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(IXYZ, int, int, IXYZ)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(IXYZ aXYZ, int aID, int aType, IXYZ aVxyz) {return addAtom(aXYZ, aID, aType, aVxyz);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(IXYZ, int, int)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(IXYZ aXYZ, int aID, int aType) {return addAtom(aXYZ, aID, aType);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(IXYZ, int)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(IXYZ aXYZ, int aType) {return addAtom(aXYZ, aType);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(IXYZ)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(IXYZ aXYZ) {return addAtom(aXYZ);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(IXYZ, int, String)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(IXYZ aXYZ, int aID, String aSymbol) {return addAtom(aXYZ, aID, aSymbol);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(IXYZ, String)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(IXYZ aXYZ, String aSymbol) {return addAtom(aXYZ, aSymbol);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtom(IAtom)
     */
    @VisibleForTesting public AtomDataBuilder<R> add(IAtom aAtom) {return addAtom(aAtom);}
    /**
     * 简化的添加原子接口名称
     * @see #addAtomList(Collection)
     */
    @VisibleForTesting public AtomDataBuilder<R> addAll(Collection<? extends IAtom> aAtomList) {return addAtomList(aAtomList);}
    /**
     * 实现 {@code leftShift} 从而支持在 groovy
     * 中通过 {@code builder << atom1 << atom2}
     * 的方式类添加原子
     * @see #addAtom(IAtom)
     */
    @VisibleForTesting public AtomDataBuilder<R> leftShift(IAtom aAtom) {return addAtom(aAtom);}
    
    /**
     * 设置原子数据的模拟盒信息，这会创建一个正交的模拟盒 {@link Box}
     * @param aX aX 模拟盒 x 方向边长
     * @param aY aY 模拟盒 y 方向边长
     * @param aZ aZ 模拟盒 z 方向边长
     * @return 自身方便链式调用
     * @see Box
     */
    public AtomDataBuilder<R> setBox(double aX, double aY, double aZ) {mBox = new Box(aX, aY, aZ); return this;}
    /**
     * 设置原子数据的模拟盒信息，这会创建一个三斜的模拟盒 {@link BoxPrism}
     * @param aA 模拟盒第一个基向量
     * @param aB 模拟盒第二个基向量
     * @param aC 模拟盒第三个基向量
     * @return 自身方便链式调用
     * @see BoxPrism
     */
    public AtomDataBuilder<R> setBox(@NotNull IXYZ aA, @NotNull IXYZ aB, @NotNull IXYZ aC) {mBox = new BoxPrism(aA, aB, aC); return this;}
    /**
     * 设置原子数据的模拟盒信息，这会创建一个三斜的模拟盒 {@link BoxPrism}
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
     * @see BoxPrism
     */
    public AtomDataBuilder<R> setBox(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {mBox = new BoxPrism(aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz); return this;}
    /**
     * 通过给定的模拟盒来设置自身的模拟盒信息，会读取输入然后进行一次值拷贝，从而避免引用
     * @param aBox 输入的任意模拟盒
     * @return 自身方便链式调用
     * @see IBox
     */
    public AtomDataBuilder<R> setBox(@NotNull IBox aBox) {
        return aBox.isPrism() ?
            setBox(aBox.ax(), aBox.ay(), aBox.az(),
                   aBox.bx(), aBox.by(), aBox.bz(),
                   aBox.cx(), aBox.cy(), aBox.cz()) :
            setBox(aBox.x(), aBox.y(), aBox.z());
    }
    
    /**
     * 设置原子数据的种类数目，会自动合法化内部元素列表的长度（如果存在）
     * @param aAtomTypeNum 需要的元素种类数目
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> setAtomTypeNumber(int aAtomTypeNum) {
        mAtomTypeNum = aAtomTypeNum;
        if (mSymbols!=null && mAtomTypeNum>mSymbols.length) {
            String[] oSymbols = mSymbols;
            mSymbols = new String[mAtomTypeNum];
            System.arraycopy(oSymbols, 0, mSymbols, 0, oSymbols.length);
        }
        return this;
    }
    /**
     * 设置原子数据的元素符号数组，会读取输入值并进行值拷贝，从而避免引用
     * <p>
     * 如果长度超过了原本的种类数目，则会自动同步更新种类数目；
     * 如果小于原本的种类数目，而不会对后面的元素符号进行修改（只会增长）
     *
     * @param aSymbols 需要设置的元素符号数组
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> setSymbols(String... aSymbols) {
        if (aSymbols.length > mAtomTypeNum) {
            mAtomTypeNum = aSymbols.length;
            mSymbols = Arrays.copyOf(aSymbols, mAtomTypeNum);
            return this;
        }
        if (mSymbols == null) mSymbols = new String[mAtomTypeNum];
        System.arraycopy(aSymbols, 0, mSymbols, 0, aSymbols.length);
        return this;
    }
    /**
     * 设置元素符号的列表版本
     * @see #setSymbols(String...)
     */
    public AtomDataBuilder<R> setSymbols(Collection<? extends CharSequence> aSymbols) {
        if (aSymbols.size() > mAtomTypeNum) {
            mAtomTypeNum = aSymbols.size();
            mSymbols = UT.Text.toArray(aSymbols);
            return this;
        }
        if (mSymbols == null) mSymbols = new String[mAtomTypeNum];
        int tIdx = 0;
        for (CharSequence tSymbol : aSymbols) {
            mSymbols[tIdx] = tSymbol.toString();
            ++tIdx;
        }
        return this;
    }
    
    /**
     * 设置原子数据是否存在 id 信息
     * @param aHasID 是否存在 id 信息输入
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> setHasID(boolean aHasID) {mHasID = aHasID; return this;}
    /**
     * {@code setHasID(true)}
     * @see #setHasID(boolean)
     */
    public AtomDataBuilder<R> setHasID() {return setHasID(true);}
    /**
     * 设置原子数据是否存在速度信息
     * @param aHasVelocity 是否存在速度信息输入
     * @return 自身方便链式调用
     */
    public AtomDataBuilder<R> setHasVelocity(boolean aHasVelocity) {mHasVelocity = aHasVelocity; return this;}
    /**
     * {@code setHasVelocity(true)}
     * @see #setHasVelocity(boolean)
     */
    public AtomDataBuilder<R> setHasVelocity() {return setHasVelocity(true);}
}
