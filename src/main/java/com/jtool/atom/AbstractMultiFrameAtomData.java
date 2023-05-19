package com.jtool.atom;

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
    @Override public final String[]          atomDataKeys          ()            {return defaultFrame().atomDataKeys();}
    @Override public final double[][]        atomData              ()            {return defaultFrame().atomData();}
    @Override public final double[]          boxLo                 ()            {return defaultFrame().boxLo();}
    @Override public final double[]          boxHi                 ()            {return defaultFrame().boxHi();}
    @Override public final int               atomTypeNum           ()            {return defaultFrame().atomTypeNum();}
    @Override public final int               atomNum               ()            {return defaultFrame().atomNum();}
    @Override public final double[][]        atomData              (int aType)   {return defaultFrame().atomData(aType);}
    @Override public final double[]          atomData              (String aKey) {return defaultFrame().atomData(aKey);}
    @Override public final double[][]        atomDataXYZ           ()            {return defaultFrame().atomDataXYZ();}
    @Override public final double[][]        atomDataXYZ           (int aType)   {return defaultFrame().atomDataXYZ(aType);}
    @Override public final double            volume                ()            {return defaultFrame().volume();}
    @Override public final double[][]        orthogonalXYZ         ()            {return defaultFrame().orthogonalXYZ();}
    @Override public final double[][]        orthogonalXYZ         (int aType)   {return defaultFrame().orthogonalXYZ(aType);}
    @Override public final IHasOrthogonalXYZ getIHasOrthogonalXYZ  ()            {return defaultFrame().getIHasOrthogonalXYZ();}
    @Override public final IHasOrthogonalXYZ getIHasOrthogonalXYZ  (int aType)   {return defaultFrame().getIHasOrthogonalXYZ(aType);}
    @Override public double[][]              atomDataXYZID         ()            {return defaultFrame().atomDataXYZID();}
    @Override public double[][]              atomDataXYZID         (int aType)   {return defaultFrame().atomDataXYZID(aType);}
    @Override public double[][]              orthogonalXYZID       ()            {return defaultFrame().orthogonalXYZID();}
    @Override public double[][]              orthogonalXYZID       (int aType)   {return defaultFrame().orthogonalXYZID(aType);}
    @Override public IHasOrthogonalXYZID     getIHasOrthogonalXYZID()            {return defaultFrame().getIHasOrthogonalXYZID();}
    @Override public IHasOrthogonalXYZID     getIHasOrthogonalXYZID(int aType)   {return defaultFrame().getIHasOrthogonalXYZID(aType);}
    
    @Override public int                     idCol                 ()            {return defaultFrame().idCol();}
    @Override public int                     typeCol               ()            {return defaultFrame().typeCol();}
    @Override public int                     xCol                  ()            {return defaultFrame().xCol();}
    @Override public int                     yCol                  ()            {return defaultFrame().yCol();}
    @Override public int                     zCol                  ()            {return defaultFrame().zCol();}
    @Override public int                     key2idx               (String aKey) {return defaultFrame().key2idx(aKey);}
    @Override public int[]                   xyzCol                ()            {return defaultFrame().xyzCol();}
    @Override public int[]                   xyzidCol              ()            {return defaultFrame().xyzidCol();}
    
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType), aThreadNum);}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (                         ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()                 );}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (           int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()     , aThreadNum);}
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType)            );}
    public MonatomicParameterCalculator getMPC                             (                         ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()                 );}
    public MonatomicParameterCalculator getMPC                             (           int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()     , aThreadNum);}
    public MonatomicParameterCalculator getTypeMPC                         (int aType                ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType)            );}
    public MonatomicParameterCalculator getTypeMPC                         (int aType, int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType), aThreadNum);}
}
