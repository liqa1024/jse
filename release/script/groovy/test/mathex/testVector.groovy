package test.mathex

import jse.math.vector.ComplexVector

import static jse.code.UT.Math.*


def vec = ComplexVector.zeros(5);
println(vec);

vec.fill {int i -> i1*i + 10*i}
println(vec);

println(-vec);
