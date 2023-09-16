package test.mathex

import com.jtool.math.vector.ComplexVector

import static com.jtool.code.CS.*


def vec = ComplexVector.zeros(5);
println(vec);

vec.fill {int i -> I*i + 10*i}
println(vec);

println(-vec);
