from math import pi, cos, sin, sqrt, gamma
import numpy as np
from numba import njit


@njit("f8(i8)")
def factorial(n):
    table = np.array([
        1,
        1,
        2,
        6,
        24,
        120,
        720,
        5040,
        40320,
        362880,
        3628800,
        39916800,
        479001600,
        6227020800,
        87178291200,
        1307674368000,
        20922789888000,
        355687428096000,
        6402373705728000,
        121645100408832000,
        2432902008176640000
    ], dtype=np.float64)
    if (n <= 20):
        s = table[n]
    else:
        s = gamma(n + 1)
    return s


@njit("c16(f8[:], i8, i8)")
def rlm_cartesian(x, l, m):
    """Solid harmonics function in the cartesian form
    as obtained from "Quantum therory of angular momentum", eq(5.1.16).
    """

    res = 0

    for p in range(l + 1):
        q = p - m
        s = l - p - q
        if q >= 0 and s >= 0:
            t0 = (-0.5*(x[0] + x[1]*1j))**p/factorial(p)
            t1 = ( 0.5*(x[0] - x[1]*1j))**q/factorial(q)
            t2 = x[2]**s/factorial(s)
            res += t0*t1*t2

    res *= sqrt((2*l + 1)/(4*pi)*factorial(l + m)*factorial(l - m))

    return res


@njit("c16[:,:](f8[:], i8)")
def rlm_cartesian_all(x, lmax):
    res = np.zeros((lmax + 1, 2*lmax + 1), dtype=np.complex128)
    cm_term = np.empty(lmax + 1, dtype=np.complex128)
    cp_term = np.empty(2*lmax + 1, dtype=np.complex128)
    cz_term = np.empty(2*lmax + 1, dtype=np.complex128)
    factorials = np.empty(2*lmax + 1, dtype=np.float64)

    cm = -0.5*(x[0] + x[1]*1j)
    cp =  0.5*(x[0] - x[1]*1j)

    # p = 0 .. lmax
    for p in range(lmax + 1):
        cm_term[p] = cm**p

    # q, s = 0 .. 2*lmax
    for q in range(2*lmax + 1):
        cp_term[q] = cp**q
        cz_term[q] = x[2]**q
        factorials[q] = factorial(q)

    # main loop
    for l in range(lmax + 1):
        t3 = (2*l + 1)/(4*pi)
        for m in range(-l, l + 1):
            for p in range(l + 1):
                q = p - m
                s = l - p - q
                if q >= 0 and s >= 0:
                    t0 = cm_term[p]/factorials[p]
                    t1 = cp_term[q]/factorials[q]
                    t2 = cz_term[s]/factorials[s]
                    res[l, m + l] += t0*t1*t2
            res[l, m + l] *= sqrt(t3*factorials[l + m]*factorials[l - m])

    return res


@njit("c16(f8[:], i8, i8)")
def ylm_cartesian(x, l, m):
    t0 = np.linalg.norm(x)**(-l)

    return rlm_cartesian(x, l, m)*t0


@njit("c16[:,:](f8[:], i8)")
def ylm_cartesian_all(x, lmax):
    normx = np.linalg.norm(x)

    res = rlm_cartesian_all(x, lmax)
    for l in range(lmax + 1):
        res[l, 0:(2*l + 1)] *= normx**(-l)

    return res


@njit("c16[:](f8[:], i8, i8)")
def ylm_prime_cartesian(x, l, m):
    normx = np.linalg.norm(x)
    p0 = sqrt(factorial(l + m)*factorial(l - m)*(2*l + 1)/(4*pi))*normx**(-l)
    p1 = l*x*ylm_cartesian(x, l, m)/normx/normx

    res = np.zeros(3, dtype=np.complex128)

    for p in range(l + 1):
        q = p - m
        s = l - p - q
        if p >= 1 and q >= 0 and s >= 0:
            t0 = (-0.5*(x[0] + x[1]*1j))**(p - 1)/factorial(p - 1)
            t1 = ( 0.5*(x[0] - x[1]*1j))**q/factorial(q)
            t2 = x[2]**s/factorial(s)
            t3 = 0.5*t0*t1*t2
            res[0] += -t3
            res[1] += -t3*1j
        if p >= 0 and q >= 1 and s >= 0:
            t0 = (-0.5*(x[0] + x[1]*1j))**p/factorial(p)
            t1 = ( 0.5*(x[0] - x[1]*1j))**(q - 1)/factorial(q - 1)
            t2 = x[2]**s/factorial(s)
            t3 = 0.5*t0*t1*t2
            res[0] += t3
            res[1] += -t3*1j
        if p >= 0 and q >= 0 and s >= 1:
            t0 = (-0.5*(x[0] + x[1]*1j))**p/factorial(p)
            t1 = ( 0.5*(x[0] - x[1]*1j))**q/factorial(q)
            t2 = x[2]**(s - 1)/factorial(s - 1)
            res[2] += t0*t1*t2

    res = res*p0 - p1

    return res


@njit("c16[:,:,:](f8[:], i8)")
def ylm_prime_cartesian_all(x, lmax):
    res = np.zeros((lmax + 1, 2*lmax + 1, 3), dtype=np.complex128)
    cm_term = np.empty(lmax + 1, dtype=np.complex128)
    cp_term = np.empty(2*lmax + 1, dtype=np.complex128)
    cz_term = np.empty(2*lmax + 1, dtype=np.complex128)
    factorials = np.empty(2*lmax + 1, dtype=np.float64)

    normx = np.linalg.norm(x)

    cm = -0.5*(x[0] + x[1]*1j)
    cp =  0.5*(x[0] - x[1]*1j)

    # p = 0 .. lmax
    for p in range(lmax + 1):
        cm_term[p] = cm**p

    # q, s = 0 .. 2*lmax
    for q in range(2*lmax + 1):
        cp_term[q] = cp**q
        cz_term[q] = x[2]**q
        factorials[q] = factorial(q)

    ylm = ylm_cartesian_all(x, lmax)
    for l in range(lmax + 1):
        for m in range(-l, l + 1):
            for p in range(l + 1):
                q = p - m
                s = l - p - q
                if p >= 1 and q >= 0 and s >= 0:
                    t0 = cm_term[p - 1]/factorials[p - 1]
                    t1 = cp_term[q]/factorials[q]
                    t2 = cz_term[s]/factorials[s]
                    t3 = 0.5*t0*t1*t2
                    res[l, m + l, 0] += -t3
                    res[l, m + l, 1] += -t3*1j
                if p >= 0 and q >= 1 and s >= 0:
                    t0 = cm_term[p]/factorials[p]
                    t1 = cp_term[q - 1]/factorials[q - 1]
                    t2 = cz_term[s]/factorials[s]
                    t3 = 0.5*t0*t1*t2
                    res[l, m + l, 0] += t3
                    res[l, m + l, 1] += -t3*1j
                if p >= 0 and q >= 0 and s >= 1:
                    t0 = cm_term[p]/factorials[p]
                    t1 = cp_term[q]/factorials[q]
                    t2 = cz_term[s - 1]/factorials[s - 1]
                    res[l, m + l, 2] += t0*t1*t2
            p0 = sqrt(factorials[l + m]*factorials[l - m]*(2*l + 1)/(4*pi))*normx**(-l)
            p1 = l*x*ylm[l, m + l]/normx/normx
            res[l, m + l, :] = p0*res[l, m + l, :] - p1

    return res


def test_ylm():
    from scipy.special import sph_harm

    x, y, z = 0.3, 0.4, 0.1
    v = np.array([x, y, z])
    theta = np.arctan2(y, x)
    phi = np.arctan2(np.hypot(x, y), z)
    lmax = 15
    ylm_all = ylm_cartesian_all(v, lmax)
    for l in range(lmax + 1):
        for m in range(-l, l + 1):
            ref = sph_harm(m, l, theta, phi)
            t0  = ylm_cartesian(v, l, m)
            t1  = ylm_all[l, m + l]
            if not (np.allclose(ref, t0) and np.allclose(ref, t1)):
                s = f"ylm test failed with: l={l} m={m} ref={ref} val={t0}"
                raise AssertionError(s)


def test_ylm_prime():
    from scipy.optimize import check_grad

    x, y, z = 0.3, 0.4, 0.1
    v = np.array([x, y, z])

    def test_f(x, l, m, real):
        r = ylm_cartesian(x, l, m)
        if real:
            return r.real
        else:
            return r.imag

    def test_df(x, l, m, real):
        r = ylm_prime_cartesian(x, l, m)
        if real:
            return r.real
        else:
            return r.imag

    eps = 1e-5
    lmax = 15
    ylm_prime = ylm_prime_cartesian_all(v, lmax)
    for l in range(lmax):
        for m in range(-l, l + 1):
            ddf_r = check_grad(test_f, test_df, v, l, m, True)
            ddf_i = check_grad(test_f, test_df, v, l, m, False)
            df = ylm_prime_cartesian(v, l, m)
            if not (ddf_r < eps and ddf_i < eps):
                s = (f"l={l} m={m} g_real={ddf_r} g_imag={ddf_i}")
                raise AssertionError(s)
            if not np.allclose(df, ylm_prime[l, m + l]):
                s = (f"l={l} m={m} ref={df} val={ylm_prime[l, m + l]}")
                raise AssertionError(s)


if __name__ == "__main__":
    test_ylm()
    test_ylm_prime()
