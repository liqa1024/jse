package jse.atom;

/** 现在认为原子无论怎样都会拥有这些属性 */
public interface IAtom extends IXYZ {
    /** 转为兼容性更高的 double[] */
    @Override default double[] data() {return dataSTD();}
    default double[] dataXYZ() {return new double[] {x(), y(), z()};}
    default double[] dataXYZID() {return new double[] {x(), y(), z(), id()};}
    default double[] dataSTD() {return new double[] {id(), type(), x(), y(), z()};}
    default double[] dataAll() {return new double[] {id(), type(), x(), y(), z(), vx(), vy(), vz()};}
    default double[] dataVelocities() {return new double[] {vx(), vy(), vz()};}
    
    double x();
    double y();
    double z();
    int id();
    int type();
    /** 增加一项专门用于获取在 AtomData 中的位置，可能存在某些结构在修改后位置会发生改变 */
    default int index() {return -1;}
    
    default double vx() {return 0.0;}
    default double vy() {return 0.0;}
    default double vz() {return 0.0;}
    default boolean hasVelocities() {return false;}
}
