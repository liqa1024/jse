package jsex.rareevent;

import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;

import java.util.Iterator;

/**
 * 内部使用的迭代器，除了一般的迭代，还可以返回到此步时经过的时间以及此步对应的参数
 * @author liqa
 */
@ApiStatus.Experimental
public interface ITimeAndParameterIterator<E> extends Iterator<E>, IAutoShutdown {
    double timeConsumed();
    double lambda();
    
    default void shutdown() {/**/}
}
