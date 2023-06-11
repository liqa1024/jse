package rareevent

import com.jtool.rareevent.IParameterCalculator
import com.jtool.rareevent.IPathGenerator


/**
 * 用来测试 FFS 准确性的简单实例，只有两个情况（+1，-1），
 * 计算最终结果到达某个数字的概率
 */
class RandomWalk {
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
        
        @Override List<PointWithTime> pathInit() {
            return pathFrom(new PointWithTime(0, 0));
        }
        @Override List<PointWithTime> pathFrom(PointWithTime point) {
            def path = new ArrayList<PointWithTime>(pathLen);
            path.add(point);
            for (_ in 1..<pathLen) {
                point = new PointWithTime(point.value + (RNG.nextInt(2)*2-1), point.time+1);
                path.add(point);
            }
            return path;
        }
        @Override double timeOf(PointWithTime point) {return point.time;}
    }
    
    static class ParameterCalculator implements IParameterCalculator<Point> {
        @Override double lambdaOf(Point point) {
            return Math.abs(point.value);
        }
    }
    
}
