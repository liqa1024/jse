package jse.parallel;

import org.jetbrains.annotations.ApiStatus;

/**
 * {@link AutoCloseable} 的另外版本，让线程池也可以使用 try-with-resources 自动关闭
 * @author liqa
 */
public interface IAutoShutdown extends IShutdownable, AutoCloseable {
    /** AutoClosable stuffs */
    void shutdown();
    @ApiStatus.Internal default void close() {shutdown();}
}
