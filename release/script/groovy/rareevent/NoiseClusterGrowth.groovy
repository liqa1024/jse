package rareevent

import jtool.math.MathEX
import jtool.rareevent.IParameterCalculator
import jtool.rareevent.IPathGenerator
import groovy.transform.CompileStatic

/**
 * 用来测试 FFS 准确性的实例，
 * 带有噪音的结晶，主要结晶过程较缓慢
 */
@CompileStatic
class NoiseClusterGrowth {
    static class Point {
        final int value, valueNoise, time;
        Point(int value, int valueNoise, int time) {this.value = value; this.valueNoise = valueNoise; this.time = time;}
        
        @Override String toString() {return value+valueNoise;}
    }
    
    
    static class PathGenerator implements IPathGenerator<Point> {
        final int initValue;
        final int pathLen;
        final double plusProb, minusProb;
        final double noiseProb, noiseScale;
        final int skipNum;
        final Random RNG = new Random();
        PathGenerator(int pathLen, double plusProb, double minusProb, double noiseProb, double noiseScale, int skipNum=1, int initValue=0) {
            this.pathLen = pathLen;
            this.plusProb = plusProb;
            this.minusProb = minusProb;
            this.noiseProb = noiseProb;
            this.noiseScale = noiseScale;
            this.skipNum = skipNum;
            this.initValue = initValue;
        }
        
        @Override Point initPoint() {return new Point(initValue, 0, 0);}
        @Override List<Point> pathFrom(Point point) {
            def path = new ArrayList<Point>(pathLen);
            path.add(point);
            for (i in 1..<pathLen*skipNum) {
                // 晶体直接增长
                int value = point.value;
                if (RNG.nextDouble() < plusProb) {
                    ++value;
                } else
                if ((value > 0) && (RNG.nextDouble() < minusProb)) {
                    --value;
                }
                // 噪音变化
                int valueNoise = point.valueNoise;
                def rand = RNG.nextDouble();
                double scale = MathEX.Fast.pow(Math.abs(valueNoise)+1, noiseScale);
                boolean positive = valueNoise==0 ? RNG.nextBoolean() : valueNoise>0;
                if (rand < noiseProb*scale) {
                    if (positive) ++valueNoise;
                    else --valueNoise;
                } else
                if (rand >= noiseProb) {
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
