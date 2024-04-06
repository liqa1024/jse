package example.cross

import jep.NDArray
import jse.code.SP
import jse.math.matrix.Matrices
import jse.math.vector.Vectors

def vec = Vectors.from([1, 2, 3, 4, 5, 6])

def mat = Matrices.from([
    [11, 12, 13, 14, 15],
    [21, 22, 23, 24, 25],
    [31, 32, 33, 34, 35],
    [41, 42, 43, 44, 45]
])

// 转为 NDArray，使用 data() 方法数据拷贝到 double[]
def a = new NDArray(vec.data(), vec.size())
// NDArray 的矩阵按行排列，需要先使用 asVecRow() 按行转为向量
def b = new NDArray(mat.asVecRow().data(), mat.nrows(), mat.ncols())

SP.Python.set('a', a)
SP.Python.exec('print(a)')
SP.Python.exec('print(a.shape)')

SP.Python.set('b', b)
SP.Python.exec('print(b)')
SP.Python.exec('print(b.shape)')


//OUTPUT:
// [1. 2. 3. 4. 5. 6.]
// (6,)
// [[11. 12. 13. 14. 15.]
//  [21. 22. 23. 24. 25.]
//  [31. 32. 33. 34. 35.]
//  [41. 42. 43. 44. 45.]]
// (4, 5)
