package code.math

import static jse.code.UT.Math.*

def a = linsequence(0.0d, 1.0d, 10)
def b = linsequence(1.0d, 1.0d, 10)
println(a)
println(b)

a.op().map2this {v -> v / 2.0d} // a.div2this(2.0d)
println(a)

a.op().operate2this(b) {l, r -> l + r} // a.plus2this(b)
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
