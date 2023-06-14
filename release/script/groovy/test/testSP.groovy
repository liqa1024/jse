package test

import com.jtool.code.SP
import com.jtool.math.MathEX
import com.jtool.math.vector.Vectors

/** 自定义 python 类的使用 */
//SP.Python.importModule('testPy');
//
//def pyClass = SP.Python.newInstance('testPy.PyClass', 3, 4);
//
//println(pyClass.a);
//println(pyClass.b);
//println(pyClass.add());
//
//println(pyClass.minus(7, 2));


/** 系统中自带的 python 库使用 */
//SP.Python.runText('import matplotlib.pyplot as plt');
//
//def x = Vectors.sequence(0.0, 0.1, 2.0*MathEX.PI);
//def y = x.opt().mapDo(x, {MathEX.Fast.sin(it)});
//
//SP.Python.invoke('plt.plot', x.data(), y.data());
//SP.Python.runText('plt.show()');


/** ase 库的使用 */
SP.Python.installAse();
SP.Python.runText('from ase import Atoms');
double a = 3.55;
def Atoms = SP.Python.getClass('Atoms');
def atoms = Atoms('Ni4',
                  [cell: [MathEX.Fast.sqrt(2.0) * a, MathEX.Fast.sqrt(2.0) * a, 1.0, 90, 90, 120] as double[],
                   pbc: [1, 1, 0] as double[],
                   scaled_positions: [[0, 0, 0],
                                      [0.5, 0, 0],
                                      [0, 0.5, 0],
                                      [0.5, 0.5, 0]] as double[][]
                  ]);
atoms.center(vacuum: 5.0, axis: 2)

println(atoms.cell);
println(atoms.positions);
println(atoms[0]);
SP.Python.setValue('atoms', atoms.unwrap());
SP.Python.runText('print(atoms.positions)');


