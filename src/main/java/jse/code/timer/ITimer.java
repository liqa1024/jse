package jse.code.timer;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface ITimer {
    long getMillis();
    double get();
    void reset();
}
