package com.guan.jobs;


import com.guan.io.ISavable;

/**
 * @author liqa
 * <p> 长时任务专用的任务池，提供一些专门需要的方法来支持任务的中断和继续 </p>
 */
public interface ILongTimeJobPool extends IHasJobPool, ISavable {
    /** 建议此时终止的接口 */
    boolean killRecommended();
    /** 直接终止的接口 */
    void kill();
    /** 正常关闭的接口 */
    void shutdown();
}
