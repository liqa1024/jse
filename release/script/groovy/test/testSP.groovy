package test

import com.jtool.code.SP

SP.Python.importModule('testPy');

def pyClass = SP.Python.newInstance('testPy.PyClass', 3, 4);

println(pyClass.a);
println(pyClass.b);
println(pyClass.add());

println(pyClass.minus(7, 2));
