package jse.code.io;

import java.util.Map;

/**
 * @author liqa
 * <p> 本项目使用的序列化和反序列化通用接口，序列化通过自身继承 {@link ISavable}，反序列化则通过 {@link ILoader} </p>
 */
@FunctionalInterface
public interface ILoader<T> {
    T load(Map<?, ?> aLoadFrom);
}
