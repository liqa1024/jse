package test.mathex

import jse.math.matrix.Matrices
import static jse.code.CS.*


def mat = Matrices.from(5, 3, {row, col -> (row*10 + col)});

println(mat);
// 5 x 3 Matrix:
//    0.000   1.000   2.000
//    10.00   11.00   12.00
//    20.00   21.00   22.00
//    30.00   31.00   32.00
//    40.00   41.00   42.00
println(mat.col(1));
// 5-length Vector:
//    1.000   11.00   21.00   31.00   41.00
mat.col(1).minus2this(10);
println(mat);
// 5 x 3 Matrix:
//    0.000   -9.000   2.000
//    10.00   1.000   12.00
//    20.00   11.00   22.00
//    30.00   21.00   32.00
//    40.00   31.00   42.00
mat.col(1).opt().refReverse().plus2this(10);
println(mat);
// 5 x 3 Matrix:
//   0.000   1.000   2.000
//   10.00   11.00   12.00
//   20.00   21.00   22.00
//   30.00   31.00   32.00
//   40.00   41.00   42.00

