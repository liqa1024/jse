package code.math

import static jse.code.UT.Math.*

def x = linspace(-1.0d, 1.0d, 10)
def y1 = sin(x * pi)
def y2 = x * x

println(x)
println(y1)
println(y2)


//OUTPUT:
// 10-length Vector:
//    -1.000   -0.7778   -0.5556   -0.3333   -0.1111   0.1111   0.3333   0.5556   0.7778   1.000
// 10-length Vector:
//    -1.225e-16   -0.6428   -0.9848   -0.8660   -0.3420   0.3420   0.8660   0.9848   0.6428   5.666e-16
// 10-length Vector:
//    1.000   0.6049   0.3086   0.1111   0.01235   0.01235   0.1111   0.3086   0.6049   1.000
