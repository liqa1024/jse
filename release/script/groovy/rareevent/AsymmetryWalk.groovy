package rareevent

import com.jtool.rareevent.IParameterCalculator
import com.jtool.rareevent.IPathGenerator


/**
 * 用来测试 FFS 准确性的另一种实例，更加倾向于回到 0，
 * 计算最终结果到达某个数字的概率
 */
class AsymmetryWalk {
    static class Point {
        final int value;
        Point(int value) {this.value = value;}
        
        @Override String toString() {return value;}
    }
    static class PointWithTime extends Point {
        final int time;
        PointWithTime(int value, int time) {super(value); this.time = time;}
    }
    
    
    static class PathGenerator implements IPathGenerator<PointWithTime> {
        private final int pathLen;
        private final def RNG = new Random();
        PathGenerator(int pathLen) {this.pathLen = pathLen;}
        
        @Override PointWithTime initPoint() {return new PointWithTime(0, 0);}
        @Override List<PointWithTime> pathFrom(PointWithTime point) {
            def path = new ArrayList<PointWithTime>(pathLen);
            path.add(point);
            for (_ in 1..<pathLen) {
                double increaseProb = 1.0 / (1.0+point.value);
                if (RNG.nextDouble() < increaseProb) {
                    point = new PointWithTime(point.value+1, point.time+1);
                } else
                if (point.value > 0) {
                    point = new PointWithTime(point.value-1, point.time+1);
                } else {
                    point = new PointWithTime(point.value, point.time+1);
                }
                path.add(point);
            }
            return path;
        }
        @Override double timeOf(PointWithTime point) {return point.time;}
    }
    
    static class ParameterCalculator implements IParameterCalculator<Point> {
        @Override double lambdaOf(Point point) {
            return point.value;
        }
    }
}
