package com.jtool.atom;

import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.math.MathEX;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.RefVector;

import java.util.List;

/**
 * 通用的 XYZ 坐标接口，使用此接口还有一重含义时此 XYZ 是不建议修改的
 * <p>
 * 现在运算统一返回 {@link XYZ} 方便使用，并且也暗示这些方法会返回新的对象
 * @author liqa
 */
public interface IXYZ {
    double x();
    double y();
    double z();
    
    /** 提供一些运算，由于 XYZ 本身就很轻量，为了避免方法调用的损失，并且让实现起来比较简单，这里不增加中间层 operation */
    default double[] data() {return new double[] {x(), y(), z()};}
    default IVector asVec() {
        return new RefVector() {
            @Override public double get_(int aIdx) {
                switch(aIdx) {
                case 0: return x();
                case 1: return y();
                case 2: return z();
                default: throw new RuntimeException();
                }
            }
            @Override public int size() {return 3;}
        };
    }
    default List<Double> asList() {
        return new AbstractRandomAccessList<Double>() {
            @Override public Double get(int index) {
                switch(index) {
                case 0: return x();
                case 1: return y();
                case 2: return z();
                default: throw new IndexOutOfBoundsException(String.format("Index: %d", index));
                }
            }
            @Override public int size() {return 3;}
        };
    }
    
    default double prod() {return x() * y() * z();}
    default double min() {return Math.min(Math.min(x(), y()), z());}
    default double max() {return Math.max(Math.max(x(), y()), z());}
    
    /** 使用和 Groovy 重载运算符相同的名称，可以顺便实现重载运算符操作 */
    default XYZ plus(IXYZ aRHS) {return new XYZ(x()+aRHS.x(), y()+aRHS.y(), z()+aRHS.z());}
    default XYZ plus(double aX, double aY, double aZ) {return new XYZ(x()+aX, y()+aY, z()+aZ);}
    default XYZ plus(double aRHS) {return new XYZ(x()+aRHS, y()+aRHS, z()+aRHS);}
    
    default XYZ minus(IXYZ aRHS) {return new XYZ(x()-aRHS.x(), y()-aRHS.y(), z()-aRHS.z());}
    default XYZ minus(double aX, double aY, double aZ) {return new XYZ(x()-aX, y()-aY, z()-aZ);}
    default XYZ minus(double aRHS) {return new XYZ(x()-aRHS, y()-aRHS, z()-aRHS);}
    
    default XYZ multiply(IXYZ aRHS) {return new XYZ(x()*aRHS.x(), y()*aRHS.y(), z()*aRHS.z());}
    default XYZ multiply(double aX, double aY, double aZ) {return new XYZ(x()*aX, y()*aY, z()*aZ);}
    default XYZ multiply(double aRHS) {return new XYZ(x()*aRHS, y()*aRHS, z()*aRHS);}
    
    default XYZ div(IXYZ aRHS) {return new XYZ(x()/aRHS.x(), y()/aRHS.y(), z()/aRHS.z());}
    default XYZ div(double aX, double aY, double aZ) {return new XYZ(x()/aX, y()/aY, z()/aZ);}
    default XYZ div(double aRHS) {return new XYZ(x()/aRHS, y()/aRHS, z()/aRHS);}
    
    
    
    default double distance(IXYZ aRHS) {
        double tX = x() - aRHS.x();
        double tY = y() - aRHS.y();
        double tZ = z() - aRHS.z();
        return MathEX.Fast.sqrt(tX*tX + tY*tY + tZ*tZ);
    }
    default double distance(double aX, double aY, double aZ) {
        aX -= x();
        aY -= y();
        aZ -= z();
        return MathEX.Fast.sqrt(aX*aX + aY*aY + aZ*aZ);
    }
    
    /**
     * MHT: ManHaTtan distance
     * 曼哈顿距离
     */
    default double distanceMHT(IXYZ aRHS) {
        return Math.abs(x() - aRHS.x()) + Math.abs(y() - aRHS.y()) + Math.abs(z() - aRHS.z());
    }
    default double distanceMHT(double aX, double aY, double aZ) {
        aX -= x();
        aY -= y();
        aZ -= z();
        return Math.abs(aX) + Math.abs(aY) + Math.abs(aZ);
    }
}
