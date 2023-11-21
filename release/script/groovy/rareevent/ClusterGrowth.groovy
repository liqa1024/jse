package rareevent

import jtool.math.MathEX
import jtoolex.rareevent.IParameterCalculator
import jtoolex.rareevent.IPathGenerator


/**
 * 用来测试 FFS 准确性的实例
 */
class ClusterGrowth {
    static class Point {
        final int value, time;
        Point(int value, int time) {this.value = value; this.time = time;}
        
        @Override String toString() {return value;}
    }
    
    
    static class PathGenerator implements IPathGenerator<Point> {
        private final int pathLen;
        private final double smallProb, largeProb;
        private final def RNG = new Random();
        PathGenerator(int pathLen, double smallProb, double largeProb) {
            this.pathLen = pathLen;
            this.smallProb = smallProb;
            this.largeProb = largeProb;
        }
        
        @Override Point initPoint() {return new Point(0, 0);}
        @Override List<Point> pathFrom(Point point) {
            def path = new ArrayList<Point>(pathLen);
            path.add(point);
            for (_ in 1..<pathLen) {
                def rand = RNG.nextDouble();
                double scale = MathEX.Fast.pow(point.value+1, -0.2);
                if (rand < largeProb*scale) {
                    point = new Point(point.value+5, point.time+1);
                } else
                if (rand < (largeProb+smallProb)*scale) {
                    point = new Point(point.value+1, point.time+1);
                } else
                if (point.value > 0) {
                    point = new Point(point.value-1, point.time+1);
                }
                path.add(point);
            }
            return path;
        }
        @Override double timeOf(Point point) {return point.time;}
    }
    
    static class ParameterCalculator implements IParameterCalculator<Point> {
        @Override double lambdaOf(Point point) {
            return point.value;
        }
    }
    
}
