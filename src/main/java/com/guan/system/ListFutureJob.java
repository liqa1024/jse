package com.guan.system;


import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 为提交任务的 Future 提供一个统一的接口，可以获取到更多信息
 * @author liqa
 */
public class ListFutureJob extends AbstractList<IFutureJob> implements Future<List<Integer>> {
    private final List<IFutureJob> mListFutureJob;
    public ListFutureJob(List<IFutureJob> aListFutureJob) {mListFutureJob = aListFutureJob;}
    
    
    /** List stuff */
    @Override public IFutureJob get(int index) {return mListFutureJob.get(index);}
    @Override public int size() {return mListFutureJob.size();}
    
    
    /** Future stuff */
    @Override public boolean cancel(boolean thisParameterIsNoUseHere) {return cancel();}
    @Override public boolean isCancelled() {
        for (IFutureJob tFutureJob : mListFutureJob) if (!tFutureJob.isCancelled()) return false;
        return true;
    }
    @Override public boolean isDone() {
        for (IFutureJob tFutureJob : mListFutureJob) if (!tFutureJob.isDone()) return false;
        return true;
    }
    @Override public List<Integer> get() throws InterruptedException, ExecutionException {
        List<Integer> tOut = new ArrayList<>();
        for (IFutureJob tFutureJob : mListFutureJob) tOut.add(tFutureJob.get());
        return tOut;
    }
    @Override public List<Integer> get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<Integer> tOut = new ArrayList<>();
        for (IFutureJob tFutureJob : mListFutureJob) tOut.add(tFutureJob.get(timeout, unit));
        return tOut;
    }
    
    /** Future Job stuff */
    public List<IFutureJob.StateType> state() {
        return new AbstractList<IFutureJob.StateType>() {
            @Override public IFutureJob.StateType get(int index) {return mListFutureJob.get(index).state();}
            @Override public int size() {return mListFutureJob.size();}
        };
    }
    public List<Integer> jobID() {
        return new AbstractList<Integer>() {
            @Override public Integer get(int index) {return mListFutureJob.get(index).jobID();}
            @Override public int size() {return mListFutureJob.size();}
        };
    }
    public boolean cancel() {
        for (IFutureJob tFutureJob : mListFutureJob) if (!tFutureJob.cancel()) return false;
        return true;
    }
}
