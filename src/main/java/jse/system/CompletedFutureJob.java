package jse.system;

import org.jetbrains.annotations.ApiStatus;

public class CompletedFutureJob implements IFutureJob {
    private final int mExitValue;
    public CompletedFutureJob(int aExitValue) {mExitValue = aExitValue;}
    
    @Override public boolean isCancelled() {return false;}
    @Override public boolean isDone() {return true;}
    @Override public StateType state() {return StateType.DONE;}
    @ApiStatus.Internal @Override public int getExitValue_() {return mExitValue;}
    @Override public int jobID() {return mExitValue;}
    @Override public boolean cancel(boolean mayInterruptIfRunning) {return false;}
}
