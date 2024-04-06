package example.math

import static jse.code.UT.Math.*

rng(123456789)

def x = rand(10)
x.plus2this(2*pi) // x = u * 2π
println(x)

def sinx = sin(x)
def cosx = cos(x)
println(sinx)
println(cosx)

// sin(x)^2 + cos(x)^2
sinx.multiply2this(sinx)
cosx.multiply2this(cosx)
sinx.plus2this(cosx)
println(sinx)


//OUTPUT:
// 10-length Vector:
//    6.947   6.740   6.674   7.177   7.033   6.643   6.759   6.770   6.398   6.532
// 10-length Vector:
//    0.6163   0.4412   0.3807   0.7792   0.6814   0.3518   0.4577   0.4682   0.1145   0.2463
// 10-length Vector:
//    0.7875   0.8974   0.9247   0.6268   0.7319   0.9361   0.8891   0.8836   0.9934   0.9692
// 10-length Vector:
//    1.000   1.000   1.000   1.000   1.000   1.000   1.000   1.000   1.000   1.000

