from numba import njit


@njit("UniTuple(f8, 2)(f8, f8)")
def polymial_cutoff(r, rc):
    rr = r/rc
    f  = (1 - rr*rr)**4
    df = -8*rr*(1 - rr*rr)**3/rc

    return f, df