package com.guan.atom;

import com.guan.math.MathEX;
import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 抽象的拥有原子数据的类，方便子类实现接口 </p>
 */
public abstract class AbstractAtomData implements IHasAtomData {
    /** stuff to override */
    @Override public abstract String[] atomDataKeys();
    @Override public abstract double[][] atomData();
    @Override public abstract double[] boxLo();
    @Override public abstract double[] boxHi();
    @Override public int atomTypeNum() {return 1;}
    
    /** override to optimize */
    @Override public int key2idx(String aKey) {
        String[] tKeys = atomDataKeys();
        for (int i = 0; i < tKeys.length; ++i) if (tKeys[i].equals(aKey)) return i;
        return -1;
    }
    @Override public int @NotNull[] xyzCol() {
        int[] tXYZCol = new int[3];
        int
        tIdx = xCol(); if (tIdx < 0) throw new RuntimeException("Do NOT has 'x' in this AtomData");
        tXYZCol[0] = tIdx;
        tIdx = yCol(); if (tIdx < 0) throw new RuntimeException("Do NOT has 'y' in this AtomData");
        tXYZCol[1] = tIdx;
        tIdx = zCol(); if (tIdx < 0) throw new RuntimeException("Do NOT has 'z' in this AtomData");
        tXYZCol[2] = tIdx;
        return tXYZCol;
    }
    @Override public int @NotNull[] xyzidCol() {
        int[] tXYZIDCol = new int[4];
        int
        tIdx = xCol();  if (tIdx < 0) throw new RuntimeException("Do NOT has 'x' in this AtomData");
        tXYZIDCol[0] = tIdx;
        tIdx = yCol();  if (tIdx < 0) throw new RuntimeException("Do NOT has 'y' in this AtomData");
        tXYZIDCol[1] = tIdx;
        tIdx = zCol();  if (tIdx < 0) throw new RuntimeException("Do NOT has 'z' in this AtomData");
        tXYZIDCol[2] = tIdx;
        tIdx = idCol(); if (tIdx < 0) throw new RuntimeException("Do NOT has 'id' in this AtomData");
        tXYZIDCol[3] = tIdx;
        return tXYZIDCol;
    }
    
    @Override public int idCol()   {return key2idx("id");}
    @Override public int typeCol() {return key2idx("type");}
    @Override public int xCol()    {return key2idx("x");}
    @Override public int yCol()    {return key2idx("y");}
    @Override public int zCol()    {return key2idx("z");}
    
    
    /** 默认的实现 */
    @Override public final int atomNum() {return atomData().length;}
    @Override public final double[][] atomData(final int aType)      {final int tTypeCol = typeCol(); return tTypeCol < 0 ? atomData() : MathEX.Mat.getSubMatrix(atomData(), aRow -> aRow[tTypeCol] == aType, MathEX.Mat.ALL);}
    @Override public final double[]   atomData(String aKey)          {return MathEX.Mat.getColumn(atomData(), MathEX.Mat.ALL, key2idx(aKey));}
    @Override public final double[][] atomDataXYZ()                  {return MathEX.Mat.getSubMatrix(atomData(), MathEX.Mat.ALL, xyzCol());}
    @Override public final double[][] atomDataXYZ(final int aType)   {final int tTypeCol = typeCol(); return tTypeCol < 0 ? MathEX.Mat.getSubMatrix(atomData(), MathEX.Mat.ALL, xyzCol()) : MathEX.Mat.getSubMatrix(atomData(), aRow -> aRow[tTypeCol] == aType, xyzCol());}
    @Override public final double[][] atomDataXYZID()                {return MathEX.Mat.getSubMatrix(atomData(), MathEX.Mat.ALL, xyzidCol());}
    @Override public final double[][] atomDataXYZID(final int aType) {final int tTypeCol = typeCol(); return tTypeCol < 0 ? MathEX.Mat.getSubMatrix(atomData(), MathEX.Mat.ALL, xyzidCol()) : MathEX.Mat.getSubMatrix(atomData(), aRow -> aRow[tTypeCol] == aType, xyzidCol());}
    
    /** OrthogonalXYZ stuffs */
    public double[][] toOrthogonalXYZ_(double[][] aAtomDataXYZ) {return aAtomDataXYZ;} // 重写来对非正交的 aAtomDataXYZ 正交化
    @Override public double volume() {
        double[] tBoxLo = boxLo();
        double[] tBoxHi = boxHi();
        if (tBoxHi == null) return 1.0;
        if (tBoxLo == null) return tBoxHi[0]*tBoxHi[1]*tBoxHi[2];
        return (tBoxHi[0]-tBoxLo[0])*(tBoxHi[1]-tBoxLo[1])*(tBoxHi[2]-tBoxLo[2]);
    }
    @Override public final double[][] orthogonalXYZ() {return toOrthogonalXYZ_(atomDataXYZ());}
    @Override public final double[][] orthogonalXYZ(final int aType) {return toOrthogonalXYZ_(atomDataXYZ(aType));}
    @Override public final IHasOrthogonalXYZ getIHasOrthogonalXYZ() {return this;}
    @Override public final IHasOrthogonalXYZ getIHasOrthogonalXYZ(final int aType) {
        return new IHasOrthogonalXYZ() {
            @Override public double[][] orthogonalXYZ() {return AbstractAtomData.this.orthogonalXYZ(aType);}
            @Override public double[] boxLo() {return AbstractAtomData.this.boxLo();}
            @Override public double[] boxHi() {return AbstractAtomData.this.boxHi();}
        };
    }
    
    /** OrthogonalXYZID stuffs */
    public double[][] toOrthogonalXYZID_(double[][] aAtomDataXYZID) {return aAtomDataXYZID;} // 重写来对非正交的 aAtomDataXYZ 正交化
    @Override public final double[][] orthogonalXYZID() {return toOrthogonalXYZID_(atomDataXYZID());}
    @Override public final double[][] orthogonalXYZID(final int aType) {return toOrthogonalXYZID_(atomDataXYZID(aType));}
    @Override public final IHasOrthogonalXYZID getIHasOrthogonalXYZID() {return this;}
    @Override public final IHasOrthogonalXYZID getIHasOrthogonalXYZID(final int aType) {
        return new IHasOrthogonalXYZID() {
            @Override public double[][] orthogonalXYZID() {return AbstractAtomData.this.orthogonalXYZID(aType);}
            @Override public double[] boxLo() {return AbstractAtomData.this.boxLo();}
            @Override public double[] boxHi() {return AbstractAtomData.this.boxHi();}
        };
    }
    
    
    /// 实用功能
    /**
     * 获取单原子参数的计算器，支持使用 MPC 的简写来调用
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MPC 的线程数目
     * @return 获取到的 MPC
     */
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType), aThreadNum);}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (                         ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()                 );}
    public MonatomicParameterCalculator getMonatomicParameterCalculator    (           int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()     , aThreadNum);}
    public MonatomicParameterCalculator getTypeMonatomicParameterCalculator(int aType                ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType)            );}
    public MonatomicParameterCalculator getMPC                             (                         ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()                 );}
    public MonatomicParameterCalculator getMPC                             (           int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ()     , aThreadNum);}
    public MonatomicParameterCalculator getTypeMPC                         (int aType                ) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType)            );}
    public MonatomicParameterCalculator getTypeMPC                         (int aType, int aThreadNum) {return new MonatomicParameterCalculator(getIHasOrthogonalXYZ(aType), aThreadNum);}
}
