package rareevent

import jtoolex.rareevent.IParameterCalculator
import jtoolex.rareevent.IPathGenerator


/**
 * 用来测试 FFS 准确性的简单实例，只有两个情况（+1，-1），
 * 计算最终结果到达某个数字的概率
 */
class RandomWalk {
    static class Point {
        final int value, time;
        Point(int value, int time) {this.value = value; this.time = time;}
        
        @Override String toString() {return value;}
    }
    
    
    static class PathGenerator implements IPathGenerator<Point> {
        private final int pathLen;
        private final def RNG = new Random();
        PathGenerator(int pathLen) {this.pathLen = pathLen;}
        
        @Override Point initPoint() {return new Point(0, 0);}
        @Override List<Point> pathFrom(Point point) {
            def path = new ArrayList<Point>(pathLen);
            path.add(point);
            for (_ in 1..<pathLen) {
                point = new Point(point.value + (RNG.nextInt(2)*2-1), point.time+1);
                path.add(point);
            }
            return path;
        }
        @Override double timeOf(Point point) {return point.time;}
    }
    
    static class ParameterCalculator implements IParameterCalculator<Point> {
        @Override double lambdaOf(Point point) {
            return Math.abs(point.value);
        }
    }
    
}
