package code.math

import static jse.code.UT.Math.*

def a = linsequence(0.0d, 1.0d, 10)
def b = linsequence(1.0d, 1.0d, 10)
println(a)
println(b)

println(a.op().dot()) // a · a
println(a.op().dot(b)) // a · b
println(a.op().cumsum())

//OUTPUT:
// 10-length Vector:
//    0.000   1.000   2.000   3.000   4.000   5.000   6.000   7.000   8.000   9.000
// 10-length Vector:
//    1.000   2.000   3.000   4.000   5.000   6.000   7.000   8.000   9.000   10.00
// 285.0
// 330.0
// 10-length Vector:
//    0.000   1.000   3.000   6.000   10.00   15.00   21.00   28.00   36.00   45.00

