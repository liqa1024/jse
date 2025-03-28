package jsex.rareevent;

import jse.atom.IAtomData;
import jse.code.random.IRandom;
import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 各种稀有事件采样方法所采样的对象，需要能够从给定的输出点随机生成一个路径。
 * 路径长度和分辨率都是任意的
 * <p>
 * 要求这些方法是线程安全的，可以同一个实例并行运行同一个方法
 * @author liqa
 * @param <T> 路径上每个点的类型，对于 lammps 模拟则是原子结构信息 {@link IAtomData}
 */
@ApiStatus.Experimental
public interface IPathGenerator<T> extends IAutoShutdown {
    /** 获取初始点，不需要任何输入参数 */
    T initPoint(IRandom aRNG);
    /** 获取从给定位置开始的路径，可以指定种子；注意这里约定获取到的路径的第一个点是 aStart（或等价于 aStart）*/
    List<? extends T> pathFrom(T aStart, IRandom aRNG);
    /** 获取指定点的时间，用来采样方法需要据此获得反应速率 */
    double timeOf(T aPoint);
    /** 获取精简一个点的数据，用于减少内存占用，也可以用于区分一条路径上继续和从存储的点继续两种不同的情况；对于lammps则需要返回没有速率的点来重新分配速率 */
    default T reducedPoint(T aPoint) {return aPoint;}
    
    default void shutdown() {/**/}
}
