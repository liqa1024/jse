package test.mathex

import jtool.math.vector.ComplexVector

import static jtool.code.UT.Math.*


def vec = ComplexVector.zeros(5);
println(vec);

vec.fill {int i -> i1*i + 10*i}
println(vec);

println(-vec);
