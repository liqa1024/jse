package jse.code.functional;

import java.util.Objects;

@FunctionalInterface
public interface IBooleanUnaryOperator {
        boolean applyAsBoolean(boolean aInput);
    
    default IBooleanUnaryOperator compose(IBooleanUnaryOperator aBefore) {
        Objects.requireNonNull(aBefore);
        return (boolean v) -> applyAsBoolean(aBefore.applyAsBoolean(v));
    }
    default IBooleanUnaryOperator andThen(IBooleanUnaryOperator aAfter) {
        Objects.requireNonNull(aAfter);
        return (boolean t) -> aAfter.applyAsBoolean(applyAsBoolean(t));
    }
    static IBooleanUnaryOperator identity() {
        return t -> t;
    }
}
