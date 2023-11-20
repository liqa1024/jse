package jtool.atom;


import jtool.code.collection.AbstractRandomAccessList;
import jtool.math.MathEX;


/**
 * @author liqa
 * <p> 获取统一获取特定结构原子数据的类，现在不再使用专门的 Generator </p>
 * <p> 现在统一返回抽象的原子数据，不支持修改，减少冗余操作 </p>
 */
public class AbstractAtoms {
    protected AbstractAtoms() {}
    
    /**
     * 根据给定数据创建 FCC 的 atomData
     * @author liqa
     * @param aCellSize FCC 晶胞的晶格常数 a
     * @param aReplicateX x 方向的重复次数
     * @param aReplicateY Y 方向的重复次数
     * @param aReplicateZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData FCC(final double aCellSize, final int aReplicateX, final int aReplicateY, final int aReplicateZ) {
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new IAtom() {
                    @Override public double x() {
                        int i = index/4/aReplicateZ/aReplicateY; double tX = aCellSize*i;
                        switch (index%4) {
                        case 0: case 3: return tX;
                        case 1: case 2: return tX + aCellSize*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double y() {
                        int j = index/4/aReplicateZ%aReplicateY; double tY = aCellSize*j;
                        switch (index%4) {
                        case 0: case 2: return tY;
                        case 1: case 3: return tY + aCellSize*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double z() {
                        int k = index/4%aReplicateZ            ; double tZ = aCellSize*k;
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
                return 4*aReplicateX*aReplicateY*aReplicateZ;
            }
        }, new XYZ(aCellSize*aReplicateX, aCellSize*aReplicateY, aCellSize*aReplicateZ));
    }
    public static IAtomData FCC(double aCellSize, int aReplicate) {return FCC(aCellSize, aReplicate, aReplicate, aReplicate);}
    
    /**
     * 根据给定数据创建 BCC 的 atomData
     * @author liqa
     * @param aCellSize BCC 晶胞的晶格常数 a
     * @param aReplicateX x 方向的重复次数
     * @param aReplicateY Y 方向的重复次数
     * @param aReplicateZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData BCC(final double aCellSize, final int aReplicateX, final int aReplicateY, final int aReplicateZ) {
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new IAtom() {
                    @Override public double x() {int i = index/2/aReplicateZ/aReplicateY; double tX = aCellSize*i; return (index%2==1) ? tX + aCellSize*0.5 : tX;}
                    @Override public double y() {int j = index/2/aReplicateZ%aReplicateY; double tY = aCellSize*j; return (index%2==1) ? tY + aCellSize*0.5 : tY;}
                    @Override public double z() {int k = index/2%aReplicateZ            ; double tZ = aCellSize*k; return (index%2==1) ? tZ + aCellSize*0.5 : tZ;}
                    @Override public int id() {return index+1;}
                    @Override public int type() {return 1;}
                    @Override public int index() {return index;}
                };
            }
            @Override public int size() {
                return 2*aReplicateX*aReplicateY*aReplicateZ;
            }
        }, new XYZ(aCellSize*aReplicateX, aCellSize*aReplicateY, aCellSize*aReplicateZ));
    }
    public static IAtomData BCC(double aCellSize, int aReplicate) {return BCC(aCellSize, aReplicate, aReplicate, aReplicate);}
    
    /**
     * 根据给定数据创建 HCP 的 atomData，为了方便使用这里直接返回正交的晶胞
     * @author liqa
     * @param aCellSize HCP 晶胞底边六边形的边长 a
     * @param aCellHeight HCP 晶胞的高度 c（默认为 sqrt(8/3)）
     * @param aReplicateX x 方向的重复次数
     * @param aReplicateY Y 方向的重复次数
     * @param aReplicateZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData HCP(final double aCellSize, final double aCellHeight, final int aReplicateX, final int aReplicateY, final int aReplicateZ) {
        final double tCellSizeY = aCellSize * SQRT3;
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new IAtom() {
                    @Override public double x() {
                        int i = index/4/aReplicateZ/aReplicateY; double tX = aCellSize  *i;
                        switch (index%4) {
                        case 0: case 2: return tX;
                        case 1: case 3: return tX + aCellSize*0.5;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double y() {
                        int j = index/4/aReplicateZ%aReplicateY; double tY = tCellSizeY *j;
                        switch (index%4) {
                        case 0: return tY;
                        case 1: return tY + tCellSizeY*0.5;
                        case 2: return tY + aCellSize*2.0/3.0;
                        case 3: return tY + tCellSizeY - aCellSize/3.0;
                        default: throw new RuntimeException();
                        }
                    }
                    @Override public double z() {
                        int k = index/4%aReplicateZ            ; double tZ = aCellHeight*k;
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
                return 4*aReplicateX*aReplicateY*aReplicateZ;
            }
        }, new XYZ(aCellSize*aReplicateX, tCellSizeY*aReplicateY, aCellHeight*aReplicateZ));
    }
    public static IAtomData HCP(double aCellSize,                     int aReplicateX, int aReplicateY, int aReplicateZ) {return HCP(aCellSize, aCellSize*SQRT83, aReplicateX, aReplicateY, aReplicateZ);}
    public static IAtomData HCP(double aCellSize, double aCellHeight, int aReplicate                                   ) {return HCP(aCellSize, aCellHeight, aReplicate, aReplicate, aReplicate);}
    public static IAtomData HCP(double aCellSize,                     int aReplicate                                   ) {return HCP(aCellSize, aReplicate, aReplicate, aReplicate);}
    private final static double SQRT3 = MathEX.Fast.sqrt(3.0), SQRT83 = MathEX.Fast.sqrt(8.0/3.0);
    
    
    /**
     * 根据给定数据创建输入晶胞的 atomData
     * @author liqa
     * @param aLattice 输入的晶胞数据
     * @param aReplicateX x 方向的重复次数
     * @param aReplicateY Y 方向的重复次数
     * @param aReplicateZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IAtomData from(final IAtomData aLattice, final int aReplicateX, final int aReplicateY, final int aReplicateZ) {
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                final int tLatticeNum = aLattice.atomNum();
                final IAtom tAtom = aLattice.pickAtom(index%tLatticeNum);
                final int tRepTotal = index/tLatticeNum;
                return new IAtom() {
                    @Override public double x() {int i = tRepTotal/aReplicateZ/aReplicateY; double tX = aLattice.box().x() * i; return tX + tAtom.x();}
                    @Override public double y() {int j = tRepTotal/aReplicateZ%aReplicateY; double tY = aLattice.box().y() * j; return tY + tAtom.y();}
                    @Override public double z() {int k = tRepTotal%aReplicateZ            ; double tZ = aLattice.box().z() * k; return tZ + tAtom.z();}
                    @Override public int id() {return tRepTotal*tLatticeNum * tAtom.id();} // 现在会基于原本的 id 进行扩展，这里不考虑特殊的 id 分布问题
                    @Override public int type() {return tAtom.type();}
                    @Override public int index() {return index;}
                };
            }
            @Override public int size() {
                return aLattice.atomNum()*aReplicateX*aReplicateY*aReplicateZ;
            }
        }, aLattice.atomTypeNum(), new IXYZ() {
            @Override public double x() {return aLattice.box().x() * aReplicateX;}
            @Override public double y() {return aLattice.box().y() * aReplicateY;}
            @Override public double z() {return aLattice.box().z() * aReplicateZ;}
        });
    }
    public static IAtomData from(IAtomData aLattice, int aReplicate) {return from(aLattice, aReplicate, aReplicate, aReplicate);}
}
