package jse.code.timer;

/**
 * 累计时间的计时器，用来计算某些过程的时间并累加
 * @author liqa
 */
public class AccumulatedTimer implements ITimer {
    private long mTime = 0;
    
    private long mStart;
    public void from() {mStart = System.nanoTime();}
    public void to() {mTime += (System.nanoTime() - mStart);}
    
    @Override public long getMillis() {return Math.round(mTime / 1.0e6);}
    @Override public double get() {return mTime / 1.0e9;}
    @Override public void reset() {mTime = 0;}
}
