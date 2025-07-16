package jse.code.functional;

import java.util.Objects;

@FunctionalInterface
public interface IShortConsumer {
    void accept(short aValue);
    
    default IShortConsumer andThen(IShortConsumer aAfter) {
        Objects.requireNonNull(aAfter);
        return (short t) -> {accept(t); aAfter.accept(t);};
    }
}
