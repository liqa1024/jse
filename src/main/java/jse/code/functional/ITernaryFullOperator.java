package jse.code.functional;

@FunctionalInterface
public interface ITernaryFullOperator<R, TA, TB, TC> {
    R apply(TA aA, TB aB, TC aC);
}
