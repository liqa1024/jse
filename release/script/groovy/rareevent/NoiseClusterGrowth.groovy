package rareevent

import jtool.math.MathEX
import jtool.math.random.LocalRandom
import jtoolex.rareevent.IParameterCalculator
import jtoolex.rareevent.IPathGenerator
import groovy.transform.CompileStatic


/**
 * 用来测试 FFS 准确性的实例，
 * 带有噪音的结晶，主要结晶过程较缓慢
 */
@CompileStatic
class NoiseClusterGrowth {
    static class Point {
        final long value, valueNoise, time;
        Point(long value, long valueNoise, long time) {this.value = value; this.valueNoise = valueNoise; this.time = time;}
        
        @Override String toString() {return value+valueNoise;}
    }
    
    
    static class PathGenerator implements IPathGenerator<Point> {
        final long initValue;
        final int pathLen;
        final double plusProb, minusProb;
        final double noiseProb, noiseScale;
        final int skipNum;
        PathGenerator(int pathLen, double plusProb, double minusProb, double noiseProb, double noiseScale, int skipNum=1, long initValue=0) {
            this.pathLen = pathLen;
            this.plusProb = plusProb;
            this.minusProb = minusProb;
            this.noiseProb = noiseProb;
            this.noiseScale = noiseScale;
            this.skipNum = skipNum;
            this.initValue = initValue;
        }
        
        @Override Point initPoint(long seed) {return new Point(initValue, 0, 0);}
        @Override List<Point> pathFrom(Point point, long seed) {
            def RNG = new LocalRandom(seed);
            
            def path = new ArrayList<Point>(pathLen);
            path.add(point);
            for (i in 1..<pathLen*skipNum) {
                // 晶体直接增长
                long value = point.value;
                def rand = RNG.nextDouble();
                if (rand < plusProb) {
                    ++value;
                } else
                if ((value > 0) && (rand < minusProb+plusProb)) {
                    --value;
                }
                // 噪音变化
                long valueNoise = point.valueNoise;
                rand = RNG.nextDouble();
                double scale = MathEX.Fast.pow(Math.abs(valueNoise)+1, noiseScale);
                boolean positive = valueNoise==0 ? RNG.nextBoolean() : valueNoise>0;
                if (rand < noiseProb*scale) {
                    if (positive) ++valueNoise;
                    else --valueNoise;
                } else
                if (Math.abs(valueNoise) > 0) {
                    if (positive) --valueNoise;
                    else ++valueNoise;
                }
                point = new Point(value, valueNoise, point.time+1);
                if (i%skipNum == skipNum-1) path.add(point);
            }
            return path;
        }
        @Override double timeOf(Point point) {return point.time;}
    }
    
    static class ParameterCalculator implements IParameterCalculator<Point> {
        @Override double lambdaOf(Point point) {return Math.abs(point.value+point.valueNoise);}
    }
    
}
