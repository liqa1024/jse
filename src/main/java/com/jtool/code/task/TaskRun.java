package com.jtool.code.task;

import java.util.concurrent.Callable;

/**
 * @author liqa
 * <p> Runnable task for matlab usage </p>
 */
public class TaskRun implements Runnable {
    private final Runnable mRun;
    public TaskRun(Runnable aRun) {mRun = aRun;}
    
    @Override public void run() {mRun.run();}
    
    public TaskCall<Void> toCallable() {return new TaskCall<>(() -> {run(); return null;});}
}
