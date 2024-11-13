package example.cross

import jep.NDArray
import jse.code.SP

// 注意 as double[] 必要，groovy 默认会创建 ArrayList 而不是 double[]
def a = new NDArray([1, 2, 3, 4, 5, 6] as double[], 3, 2)

SP.Python.set('a', a)
SP.Python.exec('print(type(a))')
SP.Python.exec('print(a)')
SP.Python.exec('print(a.shape)')


//OUTPUT:
// <class 'numpy.ndarray'>
// [[1. 2.]
//  [3. 4.]
//  [5. 6.]]
// (3, 2)
