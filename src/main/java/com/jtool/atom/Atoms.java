package com.jtool.atom;


import java.util.ArrayList;
import java.util.List;

import static com.jtool.code.CS.BOX_ZERO;

/**
 * @author liqa
 * <p> 获取统一获取特定结构原子数据的类，现在不再使用专门的 Generator </p>
 * <p> 统一使用 {@code List<IAtom>} 来存储，尽管这会占据更多的内存 </p>
 */
public class Atoms {
    private Atoms() {}
    
    /**
     * 根据给定数据创建 FCC 的 atomData
     * @author liqa
     * @param aCellSize FCC 晶胞的晶格常数 a
     * @param aReplicateX x 方向的重复次数
     * @param aReplicateY Y 方向的重复次数
     * @param aReplicateZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public static IHasAtomData FCC(double aCellSize, int aReplicateX, int aReplicateY, int aReplicateZ) {
        final XYZ tBoxHi = new XYZ(aCellSize*aReplicateX, aCellSize*aReplicateY, aCellSize*aReplicateZ);
        final List<IAtom> rAtoms = new ArrayList<>(4*aReplicateX*aReplicateY*aReplicateZ);
        
        int tID = 1;
        for (int i = 0; i < aReplicateX; ++i) for (int j = 0; j < aReplicateY; ++j) for (int k = 0; k < aReplicateZ; ++k) {
            double tX = aCellSize*i, tY = aCellSize*j, tZ = aCellSize*k;
            double tS = aCellSize*0.5;
            rAtoms.add(new Atom(tX   , tY   , tZ   , tID)); ++tID;
            rAtoms.add(new Atom(tX+tS, tY+tS, tZ   , tID)); ++tID;
            rAtoms.add(new Atom(tX+tS, tY   , tZ+tS, tID)); ++tID;
            rAtoms.add(new Atom(tX   , tY+tS, tZ+tS, tID)); ++tID;
        }
        
        return new AbstractAtomData() {
            @Override public List<IAtom> atoms() {return rAtoms;}
            @Override public IHasXYZ boxLo() {return BOX_ZERO;}
            @Override public IHasXYZ boxHi() {return tBoxHi;}
            @Override public int atomNum() {return rAtoms.size();}
            @Override public int atomTypeNum() {return 1;}
        };
    }
    public static IHasAtomData FCC(double aCellSize, int aReplicate) {return FCC(aCellSize, aReplicate, aReplicate, aReplicate);}
}
