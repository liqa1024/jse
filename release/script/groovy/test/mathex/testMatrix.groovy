package test.mathex

import jse.math.matrix.Matrices
import static jse.code.CS.*


def mat = Matrices.from(5, 3, {row, col -> (row*10 + col)});

println(mat);
println(mat.col(0));
println(mat.col(1));

def si = mat.setIteratorColAt(1);
while (si.hasNext()) {
    double v = si.next();
    if (v >= 20) si.set(v-20);
}

println(mat);

println(mat[ALL][1]);
println(mat[ALL][1].greaterOrEqual(11));
