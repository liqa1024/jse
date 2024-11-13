package example.cross

import jse.code.SP

// 导入 numpy 和 matplotlib.pyplot，并作为 np 和 plt
SP.Python.exec('import numpy')
SP.Python.exec('import matplotlib.pyplot')
np = SP.Python.getClass('numpy')
plt = SP.Python.getClass('matplotlib.pyplot')

// 可以像在 python 中一样的方法来使用，输出会自动转为 groovy 对象
def x = np.linspace(0.0, 2.0*np.pi, 20)
def y = np.sin(x)
println(x.class.name)
println(x.data.class.name)
println(x.data)
println(x.dimensions)

// groovy 的 Map 输入自动转为 python 的 kwargs 输入
plt.figure(figsize: [4, 3])
plt.plot(x, y)
plt.show()


//OUPUT:
// jep.NDArray
// [D
// [0.0, 0.3306939635357677, 0.6613879270715354, 0.992081890607303, ..., 6.283185307179586]
// [20]
// sys:1: UserWarning: Starting a Matplotlib GUI outside of the main thread will likely fail.
