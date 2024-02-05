package jse.code.functional;

@FunctionalInterface
public interface IBinaryConsumer<TL, TR> {
    void accept(TL aLHS, TR aRHS);
}
