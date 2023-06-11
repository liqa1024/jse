package test

import com.jtool.code.SP
import com.jtool.math.MathEX
import com.jtool.math.vector.Vectors

//SP.Python.importModule('testPy');
//
//def pyClass = SP.Python.newInstance('testPy.PyClass', 3, 4);
//
//println(pyClass.a);
//println(pyClass.b);
//println(pyClass.add());
//
//println(pyClass.minus(7, 2));


SP.Python.runText('import matplotlib.pyplot as plt');

def x = Vectors.sequence(0.0, 0.1, 2.0*MathEX.PI);
def y = x.opt().mapDo(x, {MathEX.Fast.sin(it)});

SP.Python.invoke('plt.plot', x.data(), y.data());
SP.Python.runText('plt.show()');

