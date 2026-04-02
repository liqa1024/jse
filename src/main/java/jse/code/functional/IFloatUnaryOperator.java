package jse.code.functional;

import java.util.Objects;

@FunctionalInterface
public interface IFloatUnaryOperator {
        float applyAsFloat(float aInput);
    
    default IFloatUnaryOperator compose(IFloatUnaryOperator aBefore) {
        Objects.requireNonNull(aBefore);
        return (float v) -> applyAsFloat(aBefore.applyAsFloat(v));
    }
    default IFloatUnaryOperator andThen(IFloatUnaryOperator aAfter) {
        Objects.requireNonNull(aAfter);
        return (float t) -> aAfter.applyAsFloat(applyAsFloat(t));
    }
    static IFloatUnaryOperator identity() {
        return t -> t;
    }
}
