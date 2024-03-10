package jsex.voronoi;

import jse.atom.IXYZ;
import jse.atom.XYZ;
import jse.math.MathEX;


/**
 * 采用这种方式将 voronoi 的一些算法注入到 MathEX 中，
 * 这种做法可以降低代码的耦合，并且保证 jse 不会依赖 jsex
 * @author liqa
 */
@SuppressWarnings("SameParameterValue")
public class VoronoiStaticExtensions {
    /**
     * 确定点 D 是否位于由点 A、B 和 C 定义的平面的左侧。假定从平面的右侧看，ABC 满足逆时针的顺序。
     * @return 如果在左边则为正，右边则为负，刚好在平面上则为 0
     */
    public static double leftOfPlane(MathEX.Graph self, IXYZ aA, IXYZ aB, IXYZ aC, IXYZ aD) {
        return Geometry.leftOfPlane(
              aA.x(), aA.y(), aA.z()
            , aB.x(), aB.y(), aB.z()
            , aC.x(), aC.y(), aC.z()
            , aD.x(), aD.y(), aD.z());
    }
    /**
     * 确定点 E 是否位于由点 A、B、C 和 D 定义的球体的内部。假定 {@code leftOfPlane(A, B, C, D) > 0}。
     * @return 如果在内部则为正，外部则为负，刚好在球面上则为 0
     */
    public static double inSphere(MathEX.Graph self, IXYZ aA, IXYZ aB, IXYZ aC, IXYZ aD, IXYZ aE) {
        return Geometry.inSphere(
              aA.x(), aA.y(), aA.z()
            , aB.x(), aB.y(), aB.z()
            , aC.x(), aC.y(), aC.z()
            , aD.x(), aD.y(), aD.z()
            , aE.x(), aE.y(), aE.z());
    }
    /**
     * 计算由点 A，B，C 和 D 定义的球的中心。假定 {@code leftOfPlane(A, B, C, D) > 0}。
     * @return 球心 XYZ 坐标
     */
    public static XYZ centerSphere(MathEX.Graph self, IXYZ aA, IXYZ aB, IXYZ aC, IXYZ aD) {
        XYZ rCenter = new XYZ(0.0, 0.0, 0.0);
        Geometry.centerSphere(
              aA.x(), aA.y(), aA.z()
            , aB.x(), aB.y(), aB.z()
            , aC.x(), aC.y(), aC.z()
            , aD.x(), aD.y(), aD.z()
            , rCenter);
        return rCenter;
    }
}
