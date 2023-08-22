package com.jtool.atom;


import com.jtool.code.collection.AbstractRandomAccessList;


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
                    @Override public double x() {int i = index/4/aReplicateZ/aReplicateY; double tX = aCellSize*i; return (index%4==1 || index%4==2) ? tX + aCellSize*0.5 : tX;}
                    @Override public double y() {int j = index/4/aReplicateZ%aReplicateY; double tY = aCellSize*j; return (index%4==1 || index%4==3) ? tY + aCellSize*0.5 : tY;}
                    @Override public double z() {int k = index/4%aReplicateZ            ; double tZ = aCellSize*k; return (index%4==2 || index%4==3) ? tZ + aCellSize*0.5 : tZ;}
                    @Override public int id() {return index+1;}
                    @Override public int type() {return 1;}
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
                };
            }
            @Override public int size() {
                return 2*aReplicateX*aReplicateY*aReplicateZ;
            }
        }, new XYZ(aCellSize*aReplicateX, aCellSize*aReplicateY, aCellSize*aReplicateZ));
    }
    public static IAtomData BCC(double aCellSize, int aReplicate) {return BCC(aCellSize, aReplicate, aReplicate, aReplicate);}
}
