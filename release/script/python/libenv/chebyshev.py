import numpy as np
from numba import njit


@njit("f8[:](f8, f8, f8, i8)")
def chebyshev_polynomial(r, r0, r1, n):
    T = np.empty(n + 1, dtype=np.float64)

    x = (2.0*r - r0 - r1)/(r1 - r0)
    T[0] = 1.0
    if n > 0:
       T[1] = x
       for i in range(2, n + 1):
          T[i] = 2.0*x*T[i - 1] - T[i - 2]

    return T


@njit("f8[:](f8, f8, f8, i8)")
def chebyshev_polynomial_d1(r, r0, r1, n):
    dT = np.empty(n + 1, dtype=np.float64)

    x = (2.0*r - r0 - r1)/(r1 - r0)
    dT[0] = 0.0
    if n > 0:
        t1 = 1.0
        t2 = 2.0*x
        dT[1] = t1
        for i in range(2, n + 1):
            dT[i] = i*t2
            t3 = 2.0*x*t2 - t1
            t1, t2 = t2, t3

    return dT*2.0/(r1 - r0)


if __name__ == "__main__":
    from numpy.polynomial.chebyshev import chebval, chebder

    r = 5
    r0 = 0.0
    r1 = 8.0
    n = 10

    x = (2.0*r - r0 - r1)/(r1 - r0)
    t0 = chebyshev_polynomial(r, r0, r1, n)
    dt0 = chebyshev_polynomial_d1(r, r0, r1, n)
    t1 = np.zeros(n + 1)
    for i in range(n + 1):
        c = [0 for it in range(n + 1)]
        c[i] = 1
        t1[i] = chebval(x, c)
    assert np.allclose(t0, t1)

    eps = 1e-4
    tm = chebyshev_polynomial(r - eps, r0, r1, n)
    tp = chebyshev_polynomial(r + eps, r0, r1, n)
    dt1 = (tp - tm)/(2.0*eps)

    assert np.allclose(dt0, dt1)
