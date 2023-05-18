package com.guan.jobs;



/**
 * @author liqa
 * <p> 通用的任务池，使用可以独立提交任务和获取结果的写法 </p>
 * <p> 提交任务：putJob(key, args...) </p>
 * <p> 获取结果：getResult(key) </p>
 * <p> 获取 Future：getJob(key) </p>
 */
public interface IHasJobPool {
    /** 这些现在也是 IHasJobPool 的东西 */
    void waitUntilDone() throws InterruptedException;
    int nJobs();
}
