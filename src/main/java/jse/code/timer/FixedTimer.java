package jse.code.timer;

/**
 * 固定时间的计时器，用来计算单一过程的简单计时器
 * @author liqa
 */
public class FixedTimer implements ITimer {
    private long mTime;
    public FixedTimer() {mTime = System.currentTimeMillis();}
    @Override public long getMillis() {return mTime;}
    @Override public double get() {return (System.currentTimeMillis() - mTime) / 1000.0;}
    @Override public void reset() {mTime = System.currentTimeMillis();}
}
