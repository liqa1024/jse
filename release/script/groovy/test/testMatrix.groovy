package test

import com.jtool.math.matrix.Matrices

def mat = Matrices.from(5, 3, {row, col -> (row*10 + col) as double;});

println(mat);
println(mat.col(0));
println(mat.col(1));

def si = mat.colSetIterator(1);
while (si.hasNext()) {
    double v = si.next();
    if (v >= 20) si.set(v-20);
}

println(mat);
