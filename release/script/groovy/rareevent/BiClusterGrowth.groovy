package rareevent

import jtool.math.MathEX
import jtool.parallel.LocalRandom
import jtoolex.rareevent.IParameterCalculator
import jtoolex.rareevent.IPathGenerator


/**
 * 用来测试 FFS 准确性的实例，
 * 拥有两种结晶（前驱体）的模型，但是前驱体不能检测到
 */
class BiClusterGrowth {
    static class Point {
        final int value, valuePre, time;
        Point(int value, int valuePre, int time) {this.value = value; this.valuePre = valuePre; this.time = time;}
        
        @Override String toString() {return value;}
    }
    
    
    static class PathGenerator implements IPathGenerator<Point> {
        private final int pathLen;
        private final double plusProb, minusProb;
        private final double convertProb;
        PathGenerator(int pathLen, double plusProb, double minusProb, double convertProb) {
            this.pathLen = pathLen;
            this.plusProb = plusProb;
            this.minusProb = minusProb;
            this.convertProb = convertProb;
        }
        
        @Override Point initPoint(long seed) {return new Point(0, 0, 0);}
        @Override List<Point> pathFrom(Point point, long seed) {
            def RNG = new LocalRandom(seed);
            
            def path = new ArrayList<Point>(pathLen);
            path.add(point);
            for (_ in 1..<pathLen) {
                // 前驱体增长
                int valuePre = point.valuePre;
                if (valuePre < 50) {
                    if (RNG.nextDouble() < 0.455) {
                        ++valuePre;
                    } else
                    if (valuePre > 0) {
                        --valuePre;
                    }
                }
                int value = point.value;
                // 晶体直接增长
                if (RNG.nextDouble() < plusProb) {
                    ++value;
                }
                if ((value > 0) && (RNG.nextDouble() < minusProb)) {
                    --value;
                }
                // 晶体通过前驱体转换
                if ((valuePre >= 50) && (RNG.nextDouble() < convertProb)) {
                    ++value;
                }
                point = new Point(value, valuePre, point.time+1);
                path.add(point);
            }
            return path;
        }
        @Override double timeOf(Point point) {return point.time;}
    }
    
    static class ParameterCalculator implements IParameterCalculator<Point> {
        @Override double lambdaOf(Point point) {return point.value;}
    }
    
}
