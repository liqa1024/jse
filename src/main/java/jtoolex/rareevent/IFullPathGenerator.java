package jtoolex.rareevent;


import jtool.atom.IAtomData;
import jtool.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;

import static jtool.code.CS.RANDOM;

/**
 * 各种稀有事件采样方法内部使用的类，需要能够从给定的输出点随机生成一个完整路径，
 * 即可以永远遍历下去的无限长的路径，因此直接遍历会造成死循环
 * <p>
 * 要求这些方法是线程安全的，可以同一个实例并行运行同一个方法，注意获取到的容器一般是线程不安全的（不同实例间线程安全）；
 * 内部会利用 {@link BufferedFullPathGenerator} 将 {@link IPathGenerator} 转换成这个类型，方便快速遍历；
 * @author liqa
 * @param <T> 获取到点的类型，对于 lammps 模拟则是原子结构信息 {@link IAtomData}
 */
@ApiStatus.Experimental
public interface IFullPathGenerator<T> extends IAutoShutdown {
    /** 由于路径具有随机性，不能返回可以重复访问的 Iterable */
    ITimeAndParameterIterator<? extends T> fullPathInit(long aSeed);
    ITimeAndParameterIterator<? extends T> fullPathFrom(T aStart, long aSeed);
    default ITimeAndParameterIterator<? extends T> fullPathInit() {return fullPathInit(RANDOM.nextLong());}
    default ITimeAndParameterIterator<? extends T> fullPathFrom(T aStart) {return fullPathFrom(aStart, RANDOM.nextLong());}
    
    default void shutdown() {/**/}
}
