package jse.system;


import jse.code.collection.AbstractRandomAccessList;
import jse.parallel.MergedFuture;

import java.util.List;
import java.util.concurrent.Future;

/**
 * 为提交任务的 Future 提供一个统一的接口，可以获取到更多信息
 * @author liqa
 */
public class ListFutureJob extends MergedFuture<Integer, IFutureJob> implements Future<List<Integer>> {
    private final List<IFutureJob> mListFutureJob;
    public ListFutureJob(List<IFutureJob> aListFutureJob) {
        super(aListFutureJob);
        mListFutureJob = aListFutureJob;
    }
    
    
    /** Groovy stuff */
    public IFutureJob getAt(int index) {return mListFutureJob.get(index);}
    public int size() {return mListFutureJob.size();}
    
    /** Future Job stuff */
    public List<IFutureJob.StateType> state() {
        return new AbstractRandomAccessList<IFutureJob.StateType>() {
            @Override public IFutureJob.StateType get(int index) {return mListFutureJob.get(index).state();}
            @Override public int size() {return mListFutureJob.size();}
        };
    }
    public List<Integer> jobID() {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return mListFutureJob.get(index).jobID();}
            @Override public int size() {return mListFutureJob.size();}
        };
    }
}
