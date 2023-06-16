package test

import com.jtool.math.MathEX
import com.jtool.math.matrix.Matrices

// 测试一下 Legendre 多项式的正确性

def P = Matrices.from(3, {row, col ->
    MathEX.Func.legendre(2, row, col*0.1);
})

println(P);
