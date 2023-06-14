package test

import com.jtool.math.matrix.Matrices

def mat = Matrices.from(5, {row, col -> (row*10 + col) as double;});

println(mat);
println(mat.refSlicer().diag());
println(mat.refSlicer().diag(1));
println(mat.refSlicer().diag(-1));