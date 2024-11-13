package example.math

import static jse.code.UT.Math.*

def a = linsequence(0.0, 1.0, 10)
def b = linsequence(1.0, 1.0, 10)
println(a)
println(b)

println(a.opt().dot()) // a · a
println(a.opt().dot(b)) // a · b
println(a.opt().cumsum())

//OUTPUT:
// 10-length Vector:
//    0.000   1.000   2.000   3.000   4.000   5.000   6.000   7.000   8.000   9.000
// 10-length Vector:
//    1.000   2.000   3.000   4.000   5.000   6.000   7.000   8.000   9.000   10.00
// 285.0
// 330.0
// 10-length Vector:
//    0.000   1.000   3.000   6.000   10.00   15.00   21.00   28.00   36.00   45.00

