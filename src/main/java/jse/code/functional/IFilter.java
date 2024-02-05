package jse.code.functional;

@FunctionalInterface
public interface IFilter<T> {
    boolean accept(T aInput);
}
