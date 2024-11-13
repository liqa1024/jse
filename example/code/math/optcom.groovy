package example.math

import static jse.code.UT.Math.*

def a = linsequence(0.0, 1.0, 10)
def b = linsequence(1.0, 1.0, 10)
println(a)
println(b)

a.opt().map2this {v -> v / 2.0} // a.div2this(2.0)
println(a)

a.opt().operate2this(b) {l, r -> l + r} // a.plus2this(b)
println(a)


//OUTPUT:
// 10-length Vector:
//    0.000   1.000   2.000   3.000   4.000   5.000   6.000   7.000   8.000   9.000
// 10-length Vector:
//    1.000   2.000   3.000   4.000   5.000   6.000   7.000   8.000   9.000   10.00
// 10-length Vector:
//    0.000   0.5000   1.000   1.500   2.000   2.500   3.000   3.500   4.000   4.500
// 10-length Vector:
//    1.000   2.500   4.000   5.500   7.000   8.500   10.00   11.50   13.00   14.50
