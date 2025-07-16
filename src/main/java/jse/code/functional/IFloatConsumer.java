package jse.code.functional;

import java.util.Objects;

@FunctionalInterface
public interface IFloatConsumer {
    void accept(float aValue);
    
    default IFloatConsumer andThen(IFloatConsumer aAfter) {
        Objects.requireNonNull(aAfter);
        return (float t) -> {accept(t); aAfter.accept(t);};
    }
}
