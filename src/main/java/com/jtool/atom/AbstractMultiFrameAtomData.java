package com.jtool.atom;

import com.jtool.math.table.ITable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;

/**
 * @author liqa
 * <p> 抽象的拥有多个帧的原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractMultiFrameAtomData<T extends IHasAtomData> extends AbstractList<T> implements IHasAtomData {
    /** AbstractList stuffs */
    @Override public abstract int size();
    @Override public abstract T get(int index);
    
    /** IHasAtomData 的接口，将本身作为 atomData 时则会返回第一帧的结果 */
    public T defaultFrame() {return get(0);}
    @Override public final ITable           dataXYZ     ()          {return defaultFrame().dataXYZ();}
    @Override public final ITable           dataXYZ     (int aType) {return defaultFrame().dataXYZ(aType);}
    @Override public final ITable           dataXYZID   ()          {return defaultFrame().dataXYZID();}
    @Override public final ITable           dataXYZID   (int aType) {return defaultFrame().dataXYZID(aType);}
    @Override public final ITable           dataSTD     ()          {return defaultFrame().dataSTD();}
    @Override public final ITable           dataSTD     (int aType) {return defaultFrame().dataSTD(aType);}
    @Override public final Iterable<IAtom>  atoms       ()          {return defaultFrame().atoms();}
    @Override public final Iterable<IAtom>  atoms       (int aType) {return defaultFrame().atoms(aType);}
    @Override public final int              atomNum     ()          {return defaultFrame().atomNum();}
    @Override public final int              atomTypeNum ()          {return defaultFrame().atomTypeNum();}
    @Override public final IHasXYZ          boxLo       ()          {return defaultFrame().boxLo();}
    @Override public final IHasXYZ          boxHi       ()          {return defaultFrame().boxHi();}
    
    
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, int aThreadNum) {return new MonatomicParameterCalculator(atoms(aType), aThreadNum);}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (                         ) {return new MonatomicParameterCalculator(atoms()                 );}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (           int aThreadNum) {return new MonatomicParameterCalculator(atoms()     , aThreadNum);}
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                ) {return new MonatomicParameterCalculator(atoms(aType)            );}
    @VisibleForTesting public MonatomicParameterCalculator getMPC          (                         ) {return new MonatomicParameterCalculator(atoms()                 );}
    @VisibleForTesting public MonatomicParameterCalculator getMPC          (           int aThreadNum) {return new MonatomicParameterCalculator(atoms()     , aThreadNum);}
    @VisibleForTesting public MonatomicParameterCalculator getTypeMPC      (int aType                ) {return new MonatomicParameterCalculator(atoms(aType)            );}
    @VisibleForTesting public MonatomicParameterCalculator getTypeMPC      (int aType, int aThreadNum) {return new MonatomicParameterCalculator(atoms(aType), aThreadNum);}
}
