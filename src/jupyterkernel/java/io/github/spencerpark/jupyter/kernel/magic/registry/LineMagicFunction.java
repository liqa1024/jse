package io.github.spencerpark.jupyter.kernel.magic.registry;

import java.util.List;

@SuppressWarnings("UnnecessaryModifier")
@FunctionalInterface
public interface LineMagicFunction<T> {
    public T execute(List<String> args) throws Exception;
}
