package jse.code.functional;

import java.util.Objects;

@FunctionalInterface
public interface IBooleanConsumer {
    void accept(boolean aValue);
    
    default IBooleanConsumer andThen(IBooleanConsumer aAfter) {
        Objects.requireNonNull(aAfter);
        return (boolean t) -> {accept(t); aAfter.accept(t);};
    }
}
