package com.guan.atom;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liqa
 * <p> 通用的拥有原子数据的类使用的接口，主要用于相互转换 </p>
 */
public interface IHasAtomData extends IHasOrthogonalXYZ, IHasOrthogonalXYZID {
    int atomNum();
    int atomTypeNum();
    String[] atomDataKeys();
    double[][] atomData();
    double[][] atomData(int aType);
    double[]   atomData(String aKey);
    
    /** 这些理论上是可选的，抛出错误或者返回 null 表明这个类型没有这些属性，使得非法调用能够被检测到 */
    double[][] atomDataXYZ();
    double[][] atomDataXYZ(int aType);
    double[][] atomDataXYZID();
    double[][] atomDataXYZID(int aType);
    
    int idCol();
    int typeCol();
    int xCol();
    int yCol();
    int zCol();
    int key2idx(String aKey);
    int[] xyzCol();
    int[] xyzidCol();
    
    
    /** OrthogonalXYZ stuffs */
    double volume();
    double[] boxLo();
    double[] boxHi();
    double[][] orthogonalXYZ();
    double[][] orthogonalXYZ(int aType);
    IHasOrthogonalXYZ getIHasOrthogonalXYZ();
    IHasOrthogonalXYZ getIHasOrthogonalXYZ(int aType);
    
    /** OrthogonalXYZID stuffs */
    double[][] orthogonalXYZID();
    double[][] orthogonalXYZID(int aType);
    IHasOrthogonalXYZID getIHasOrthogonalXYZID();
    IHasOrthogonalXYZID getIHasOrthogonalXYZID(int aType);
    
    class Util {
        public final static String[] STD_ATOM_DATA_KEYS = new String[] {"id", "type", "x", "y", "z"}; // 标准 AtomData 包含信息格式为 id type x y z，和 Lmpdat 保持一致
        
        public static double[][] toStandardAtomData(IHasAtomData aHasAtomData) {
            // 一般的将 IHasAtomData 转换成标准 AtomData 的方法，一定要求是正交的 XYZ 才能作为输入的数据，只有 id，type 和 xyz 信息
            List<double[]> rAtomDataList = new ArrayList<>(aHasAtomData.atomNum());
            int tAtomTypeNum = aHasAtomData.atomTypeNum();
            
            // 如果包含 id 则使用 orthogonalXYZID 保留 id 信息，如果没有则重新生成
            if (aHasAtomData.idCol() < 0) {
                int tID = 1; // 注意 id 从 1 开始
                for (int tType = 1; tType <= tAtomTypeNum; ++tType) {
                    double[][] tXYZData = aHasAtomData.orthogonalXYZ(tType);
                    for (double[] tXYZ : tXYZData) {
                        rAtomDataList.add(new double[]{tID, tType, tXYZ[0], tXYZ[1], tXYZ[2]});
                        ++tID;
                    }
                }
            } else {
                for (int tType = 1; tType <= tAtomTypeNum; ++tType) {
                    double[][] tXYZIDData = aHasAtomData.orthogonalXYZID(tType);
                    for (double[] tXYZID : tXYZIDData) {
                        rAtomDataList.add(new double[]{tXYZID[3], tType, tXYZID[0], tXYZID[1], tXYZID[2]});
                    }
                }
            }
            return rAtomDataList.toArray(new double[0][]);
        }
    }
}
