package jse.atom;


import jse.code.collection.AbstractRandomAccessList;
import jse.math.MathEX;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

import static jse.code.CS.ZL_STR;


/**
 * @author liqa
 * <p> 获取统一获取特定结构原子数据的类，现在不再使用专门的 Generator </p>
 * <p> 现在统一返回抽象的原子数据，不支持修改，减少冗余操作 </p>
 */
public class Structures {
    protected Structures() {}
    
    /**
     * 根据给定数据创建 FCC (Face Center Cubic) 的 atomData
     * @author liqa
     * @param aCellSize FCC 晶胞的晶格常数 a
     * @param aRepeatX x 方向的重复次数
     * @param aRepeatY Y 方向的重复次数
     * @param aRepeatZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData FCC(final double aCellSize, final int aRepeatX, final int aRepeatY, final int aRepeatZ) {
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new AbstractAtom() {
                    @Override public double x() {
                        int i = index/4%aRepeatX; double tX = aCellSize*i;
                        switch (index%4) {
                        case 0: case 3: return tX;
                        case 1: case 2: return tX + aCellSize*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double y() {
                        int j = index/4/aRepeatX%aRepeatY; double tY = aCellSize*j;
                        switch (index%4) {
                        case 0: case 2: return tY;
                        case 1: case 3: return tY + aCellSize*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double z() {
                        int k = index/4/aRepeatX/aRepeatY; double tZ = aCellSize*k;
                        switch (index%4) {
                        case 0: case 1: return tZ;
                        case 2: case 3: return tZ + aCellSize*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public int id() {return index+1;}
                    @Override public int type() {return 1;}
                    @Override public int index() {return index;}
                };
            }
            @Override public int size() {
                return 4*aRepeatX*aRepeatY*aRepeatZ;
            }
        }, new Box(aCellSize*aRepeatX, aCellSize*aRepeatY, aCellSize*aRepeatZ));
    }
    public static IAtomData FCC(double aCellSize, int aRepeat) {return FCC(aCellSize, aRepeat, aRepeat, aRepeat);}
    @VisibleForTesting public static IAtomData fcc(double aCellSize, int aRepeatX, int aRepeatY, int aRepeatZ) {return FCC(aCellSize, aRepeatX, aRepeatY, aRepeatZ);}
    @VisibleForTesting public static IAtomData fcc(double aCellSize, int aRepeat) {return FCC(aCellSize, aRepeat);}
    
    /**
     * 根据给定数据创建 BCC (Body Center Cubic) 的 atomData
     * @author liqa
     * @param aCellSize BCC 晶胞的晶格常数 a
     * @param aRepeatX x 方向的重复次数
     * @param aRepeatY Y 方向的重复次数
     * @param aRepeatZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData BCC(final double aCellSize, final int aRepeatX, final int aRepeatY, final int aRepeatZ) {
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new AbstractAtom() {
                    @Override public double x() {int i = index/2%aRepeatX         ; double tX = aCellSize*i; return (index%2==1) ? tX + aCellSize*0.5 : tX;}
                    @Override public double y() {int j = index/2/aRepeatX%aRepeatY; double tY = aCellSize*j; return (index%2==1) ? tY + aCellSize*0.5 : tY;}
                    @Override public double z() {int k = index/2/aRepeatX/aRepeatY; double tZ = aCellSize*k; return (index%2==1) ? tZ + aCellSize*0.5 : tZ;}
                    @Override public int id() {return index+1;}
                    @Override public int type() {return 1;}
                    @Override public int index() {return index;}
                };
            }
            @Override public int size() {
                return 2*aRepeatX*aRepeatY*aRepeatZ;
            }
        }, new Box(aCellSize*aRepeatX, aCellSize*aRepeatY, aCellSize*aRepeatZ));
    }
    public static IAtomData BCC(double aCellSize, int aRepeat) {return BCC(aCellSize, aRepeat, aRepeat, aRepeat);}
    @VisibleForTesting public static IAtomData bcc(double aCellSize, int aRepeatX, int aRepeatY, int aRepeatZ) {return BCC(aCellSize, aRepeatX, aRepeatY, aRepeatZ);}
    @VisibleForTesting public static IAtomData bcc(double aCellSize, int aRepeat) {return BCC(aCellSize, aRepeat);}
    
    /**
     * 根据给定数据创建 SC (Simple Cubic) 的 atomData
     * @author liqa
     * @param aCellSize SC 晶胞的晶格常数 a
     * @param aRepeatX x 方向的重复次数
     * @param aRepeatY Y 方向的重复次数
     * @param aRepeatZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData SC(final double aCellSize, final int aRepeatX, final int aRepeatY, final int aRepeatZ) {
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new AbstractAtom() {
                    @Override public double x() {int i = index%aRepeatX         ; return aCellSize*i;}
                    @Override public double y() {int j = index/aRepeatX%aRepeatY; return aCellSize*j;}
                    @Override public double z() {int k = index/aRepeatX/aRepeatY; return aCellSize*k;}
                    @Override public int id() {return index+1;}
                    @Override public int type() {return 1;}
                    @Override public int index() {return index;}
                };
            }
            @Override public int size() {
                return aRepeatX*aRepeatY*aRepeatZ;
            }
        }, new Box(aCellSize*aRepeatX, aCellSize*aRepeatY, aCellSize*aRepeatZ));
    }
    public static IAtomData SC(double aCellSize, int aRepeat) {return SC(aCellSize, aRepeat, aRepeat, aRepeat);}
    @VisibleForTesting public static IAtomData sc(double aCellSize, int aRepeatX, int aRepeatY, int aRepeatZ) {return SC(aCellSize, aRepeatX, aRepeatY, aRepeatZ);}
    @VisibleForTesting public static IAtomData sc(double aCellSize, int aRepeat) {return SC(aCellSize, aRepeat);}
    
    /**
     * 根据给定数据创建 HCP (Hexagonal Close Packed) 的 atomData，
     * 为了方便使用这里直接返回正交的晶胞
     * @author liqa
     * @param aCellSize HCP 晶胞底边六边形的边长 a
     * @param aCellHeight HCP 晶胞的高度 c（默认为 sqrt(8/3)）
     * @param aRepeatX x 方向的重复次数
     * @param aRepeatY Y 方向的重复次数
     * @param aRepeatZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData HCP(final double aCellSize, final double aCellHeight, final int aRepeatX, final int aRepeatY, final int aRepeatZ) {
        final double tCellSizeY = aCellSize * SQRT3;
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new AbstractAtom() {
                    @Override public double x() {
                        int i = index/4%aRepeatX; double tX = aCellSize  *i;
                        switch (index%4) {
                        case 0: case 2: return tX;
                        case 1: case 3: return tX + aCellSize*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double y() {
                        int j = index/4/aRepeatX%aRepeatY; double tY = tCellSizeY *j;
                        switch (index%4) {
                        case 0: return tY;
                        case 1: return tY + tCellSizeY*0.5;
                        case 2: return tY + aCellSize*2.0/3.0;
                        case 3: return tY + tCellSizeY - aCellSize/3.0;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double z() {
                        int k = index/4/aRepeatX/aRepeatY; double tZ = aCellHeight*k;
                        switch (index%4) {
                        case 0: case 1: return tZ;
                        case 2: case 3: return tZ + aCellHeight*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public int id() {return index+1;}
                    @Override public int type() {return 1;}
                    @Override public int index() {return index;}
                };
            }
            @Override public int size() {
                return 4*aRepeatX*aRepeatY*aRepeatZ;
            }
        }, new Box(aCellSize*aRepeatX, tCellSizeY*aRepeatY, aCellHeight*aRepeatZ));
    }
    public static IAtomData HCP(double aCellSize,                     int aRepeatX, int aRepeatY, int aRepeatZ) {return HCP(aCellSize, aCellSize*SQRT83, aRepeatX, aRepeatY, aRepeatZ);}
    public static IAtomData HCP(double aCellSize, double aCellHeight, int aRepeat                             ) {return HCP(aCellSize, aCellHeight, aRepeat, aRepeat, aRepeat);}
    public static IAtomData HCP(double aCellSize,                     int aRepeat                             ) {return HCP(aCellSize, aRepeat, aRepeat, aRepeat);}
    private final static double SQRT3 = MathEX.Fast.sqrt(3.0), SQRT83 = MathEX.Fast.sqrt(8.0/3.0);
    @VisibleForTesting public static IAtomData hcp(double aCellSize, double aCellHeight, int aRepeatX, int aRepeatY, int aRepeatZ) {return HCP(aCellSize, aCellHeight, aRepeatX, aRepeatY, aRepeatZ);}
    @VisibleForTesting public static IAtomData hcp(double aCellSize,                     int aRepeatX, int aRepeatY, int aRepeatZ) {return HCP(aCellSize, aRepeatX, aRepeatY, aRepeatZ);}
    @VisibleForTesting public static IAtomData hcp(double aCellSize, double aCellHeight, int aRepeat                             ) {return HCP(aCellSize, aCellHeight, aRepeat);}
    @VisibleForTesting public static IAtomData hcp(double aCellSize,                     int aRepeat                             ) {return HCP(aCellSize, aRepeat);}
    
    /**
     * 根据给定数据创建输入晶胞的 atomData
     * @author liqa
     * @param aLattice 输入的晶胞数据
     * @param aRepeatX x 方向的重复次数
     * @param aRepeatY Y 方向的重复次数
     * @param aRepeatZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData from(final IAtomData aLattice, final int aRepeatX, final int aRepeatY, final int aRepeatZ) {
        final IBox aBox = aLattice.box();
        final IBox tBox = aLattice.isPrism() ? new BoxPrism(
            aBox.ax()*aRepeatX, aBox.ay()*aRepeatX, aBox.az()*aRepeatX,
            aBox.bx()*aRepeatY, aBox.by()*aRepeatY, aBox.bz()*aRepeatY,
            aBox.cx()*aRepeatZ, aBox.cy()*aRepeatZ, aBox.cz()*aRepeatZ
            ) : new Box(aBox.x()*aRepeatX, aBox.y()*aRepeatY, aBox.z()*aRepeatZ);
        final int tLatticeNum = aLattice.atomNumber();
        @Nullable List<@Nullable String> tSymbols = aLattice.symbols();
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                final IAtom tAtom = aLattice.atom(index%tLatticeNum);
                final int tRepTotal = index/tLatticeNum;
                // 获取平移次数
                int i = tRepTotal%aRepeatX         ;
                int j = tRepTotal/aRepeatX%aRepeatY;
                int k = tRepTotal/aRepeatX/aRepeatY;
                final XYZ tSXYZ;
                if (!aBox.isPrism()) {
                    tSXYZ = aBox.multiply(i, j, k);
                } else {
                    tSXYZ = new XYZ(
                        aBox.ax()*i + aBox.bx()*j + aBox.cx()*k,
                        aBox.ay()*i + aBox.by()*j + aBox.cy()*k,
                        aBox.az()*i + aBox.bz()*j + aBox.cz()*k
                    );
                }
                return new AbstractAtom() {
                    @Override public double x() {return tSXYZ.mX + tAtom.x();}
                    @Override public double y() {return tSXYZ.mY + tAtom.y();}
                    @Override public double z() {return tSXYZ.mZ + tAtom.z();}
                    @Override public int id() {return tRepTotal*tLatticeNum + tAtom.id();} // 现在会基于原本的 id 进行扩展，这里不考虑特殊的 id 分布问题
                    @Override public boolean hasID() {return tAtom.hasID();}
                    @Override public int type() {return tAtom.type();}
                    @Override public int index() {return index;}
                };
            }
            @Override public int size() {
                return tLatticeNum*aRepeatX*aRepeatY*aRepeatZ;
            }
        }, aLattice.atomTypeNumber(), tBox, aLattice.hasID(), aLattice.hasVelocity(), tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    public static IAtomData from(IAtomData aLattice, int aRepeat) {return from(aLattice, aRepeat, aRepeat, aRepeat);}
}
