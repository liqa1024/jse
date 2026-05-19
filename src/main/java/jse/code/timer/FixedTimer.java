package jse.code.timer;

/**
 * 固定时间的计时器，用来计算单一过程的简单计时器
 * @author liqa
 */
public class FixedTimer implements ITimer {
    private long mTime;
    public FixedTimer() {mTime = System.nanoTime();}
    @Override public long getNanos() {return System.nanoTime()-mTime;}
    @Override public long getMillis() {return Math.round((System.nanoTime()-mTime) / 1.0e6);}
    @Override public double get() {return (System.nanoTime()-mTime) / 1.0e9;}
    @Override public void reset() {mTime = System.nanoTime();}
}
