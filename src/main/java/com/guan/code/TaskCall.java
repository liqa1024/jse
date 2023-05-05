package com.guan.code;

import java.util.concurrent.Callable;

/**
 * @author liqa
 * <p> Callable task for matlab usage </p>
 */
public class TaskCall<T> implements Callable<T> {
    private final Callable<T> mCall;
    public TaskCall(Callable<T> aCall) {mCall = aCall;}
    
    @Override public T call() throws Exception {return mCall.call();}
}
