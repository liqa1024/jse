package jse.code.functional;

@FunctionalInterface
public interface ITernaryConsumer<TA, TB, TC> {
    void accept(TA aA, TB aB, TC aC);
}
