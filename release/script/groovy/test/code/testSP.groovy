package test.code

import jtool.code.SP
import jtool.math.MathEX
import jtool.math.vector.Vectors


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
import static jtool.code.UT.Math.*

plt = SP.Python.importModule('matplotlib.pyplot');

def x = linspace(0.0, 2.0*pi, 20);
def y = sin(x);

plt.plot(x.data(), y.data());
plt.show();


/** ase 库的使用 */
//SP.Python.installAse();
//SP.Python.runText('from ase import Atoms');
//double a = 3.55;
//def Atoms = SP.Python.getClass('Atoms');
//def atoms = Atoms('Ni4',
//                  [cell: [MathEX.Fast.sqrt(2.0) * a, MathEX.Fast.sqrt(2.0) * a, 1.0, 90, 90, 120] as double[],
//                   pbc: [1, 1, 0] as double[],
//                   scaled_positions: [[0, 0, 0],
//                                      [0.5, 0, 0],
//                                      [0, 0.5, 0],
//                                      [0.5, 0.5, 0]] as double[][]
//                  ]);
//atoms.center(vacuum: 5.0, axis: 2)
//
//println(atoms.cell);
//println(atoms.positions);
//println(atoms[0]);
//SP.Python.setValue('atoms', atoms.unwrap());
//SP.Python.runText('print(atoms.positions)');


