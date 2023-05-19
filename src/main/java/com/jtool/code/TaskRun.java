package com.jtool.code;

import java.util.concurrent.Callable;

/**
 * @author liqa
 * <p> Runnable task for matlab usage </p>
 */
public class TaskRun implements Runnable {
    private final Runnable mRun;
    public TaskRun(Runnable aRun) {mRun = aRun;}
    
    @Override public void run() {mRun.run();}
    
    public static TaskRun get(final Callable<?> aCall) {return new TaskRun(() -> {try {aCall.call();} catch (Exception e) {e.printStackTrace();}});}
}
