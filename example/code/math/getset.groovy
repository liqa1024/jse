package code.math

import static jse.code.UT.Math.*

def vec = zeros(5)
def list = [0.0d] * 5
println('=========INIT=========')
println('vec:')
println(vec)
println('list:')
println(list)

vec[1] = 10.0d
list[1] = 10.0d
println('=========SET=========')
println('vec:')
println(vec)
println('list:')
println(list)

vec[2..<5] = 20.0d
list[2..<5] = 20.0d
println('=========PUT=========')
println('vec:')
println(vec)
println('list:')
println(list)

vec += 3.0d
list += 3.0d
println('=========PLUS=========')
println('vec:')
println(vec)
println('list:')
println(list)


//OUTPUT:
// =========INIT=========
// vec:
// 5-length Vector:
//    0.000   0.000   0.000   0.000   0.000
// list:
// [0.0, 0.0, 0.0, 0.0, 0.0]
// =========SET=========
// vec:
// 5-length Vector:
//    0.000   10.00   0.000   0.000   0.000
// list:
// [0.0, 10.0, 0.0, 0.0, 0.0]
// =========PUT=========
// vec:
// 5-length Vector:
//    0.000   10.00   20.00   20.00   20.00
// list:
// [0.0, 10.0, 20.0]
// =========PLUS=========
// vec:
// 5-length Vector:
//    3.000   13.00   23.00   23.00   23.00
// list:
// [0.0, 10.0, 20.0, 3.0]
