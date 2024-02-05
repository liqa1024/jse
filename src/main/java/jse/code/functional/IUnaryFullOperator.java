package jse.code.functional;

import java.util.function.Function;

@FunctionalInterface
public interface IUnaryFullOperator<R, T> extends Function<T, R> {
    R apply(T aInput);
    
    static <T> IUnaryFullOperator<T, T> identity() {return t -> t;}
}
