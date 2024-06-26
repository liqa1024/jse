package jse.math.function;

public abstract class AbstractFunc2 implements IFunc2 {
    static void rangeCheck(int aI, int aJ, int aNx, int aNy) {
        if (aI<0 || aI>=aNx) throw new IndexOutOfBoundsException("i = " + aI + ", Nx = " + aNx);
        if (aJ<0 || aJ>=aNy) throw new IndexOutOfBoundsException("j = " + aJ + ", Ny = " + aNy);
    }
    
    /** stuff to override */
    public abstract double get(int aI, int aJ);
    public abstract void set(int aI, int aJ, double aV);
}
