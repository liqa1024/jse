package code.cross

import jse.code.SP
import jse.math.vector.Vectors

SP.Python.exec('import numpy as np')
SP.Python.exec('a = np.array([1, 2, 3, 4, 5, 6])')
SP.Python.exec('print(a)')

def a = Vectors.fromNumpy(SP.Python.get('a'))
println(a)


//OUTPUT:
// [1 2 3 4 5 6]
// 6-length Integer Vector:
//  1 2 3 4 5 6
