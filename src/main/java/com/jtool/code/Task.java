package com.jtool.code;

import java.util.concurrent.Callable;

/**
 * @author liqa
 * <p> General task for this project </p>
 */
public class Task extends TaskCall<Boolean> {
    public Task(Callable<Boolean> aCall) {super(aCall);}
    
    public static Task get(final Runnable aRun) {return new Task(() -> {aRun.run(); return true;});}
    public static Task get(final Callable<?> aCall) {return new Task(() -> {Object tOut = aCall.call(); return (tOut instanceof Boolean) ? (Boolean)tOut : tOut!=null;});}
}
