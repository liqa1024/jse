package jse.code.functional;

@FunctionalInterface
public interface IBinaryFullOperator<R, TL, TR> {
    R apply(TL aLHS, TR aRHS);
}
