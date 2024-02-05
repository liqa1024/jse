package test.code

import jse.code.SP
import jse.math.MathEX
import jse.math.vector.Vectors


/** 自定义 python 类的使用 */
//SP.Python.runScript('testJava');

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
np = SP.Python.importModule('numpy');
plt = SP.Python.importModule('matplotlib.pyplot');

def x = np.linspace(0.0, 2.0*np.pi, 20);
def y = np.sin(x);

plt.plot(x, y);
plt.show();


/** ase 库的使用 */
////SP.Python.installAse();
//SP.Python.runText('from ase import Atoms');
//double a = 3.55;
//def Atoms = SP.Python.getClass('Atoms');
//def atoms = Atoms('Ni4',
//                  [cell: [MathEX.Fast.sqrt(2.0) * a, MathEX.Fast.sqrt(2.0) * a, 1.0, 90, 90, 120],
//                   pbc: [1, 1, 0],
//                   scaled_positions: [[0, 0, 0],
//                                      [0.5, 0, 0],
//                                      [0, 0.5, 0],
//                                      [0.5, 0.5, 0]]
//                  ]);
//atoms.center(vacuum: 5.0, axis: 2)
//
//println(atoms.cell);
//println(atoms.positions);
//println(atoms[0]);
//SP.Python.setValue('atoms', atoms);
//SP.Python.runText('print(atoms.positions)');

