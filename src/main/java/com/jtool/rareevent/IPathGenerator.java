package com.jtool.rareevent;

import com.jtool.atom.IHasAtomData;
import com.jtool.parallel.IAutoShutdown;

import java.util.List;

/**
 * 各种稀有事件采样方法所采样的对象，需要能够从给定的输出点随机生成一个路径。
 * 路径长度和分辨率都是任意的
 * <p>
 * 要求这些方法是线程安全的，可以同一个实例并行运行同一个方法
 * @author liqa
 * @param <T> 路径上每个点的类型，对于 lammps 模拟则是原子结构信息 {@link IHasAtomData}
 */
public interface IPathGenerator<T> extends IAutoShutdown {
    /** 获取初始点，不需要任何输入参数 */
    T initPoint();
    /** 获取从给定位置开始的路径，注意这里约定获取到的路径的第一个点是 aStart（或等价于 aStart）*/
    List<? extends T> pathFrom(T aStart);
    /** 获取指定点的时间，用来采样方法需要据此获得反应速率 */
    double timeOf(T aPoint);
}
