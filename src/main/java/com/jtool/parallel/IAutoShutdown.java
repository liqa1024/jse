package com.jtool.parallel;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * {@link AutoCloseable} 的另外版本，让线程池也可以使用 try-with-resources 自动关闭
 * @author liqa
 */
public interface IAutoShutdown extends AutoCloseable {
    /** AutoClosable stuffs */
    void shutdown();
    @VisibleForTesting default void close() {shutdown();}
}
