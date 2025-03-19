- 交叉编程
    - [groovy 中调用 python](pythoningroovy.md)
        - [环境要求](#环境要求)
        - [基本使用方法](#基本使用方法)
        - [NDArray 的使用](#ndarray-的使用)
    - [python 中调用 jse](jseinpython.md)
    - [matlab 中调用 jse]()
- [**⟶ 目录**](contents.md)


# groovy 中调用 python

jse 支持通过内部的 [jep](https://github.com/ninia/jep)
库来直接使用 python，这里专门针对 groovy 脚本的情况做了更多的适配，
使其使用起来更加方便。

jse 已经内置了完整的 jep，并且会在需要时自动安装 jni 部分，
因此**不需要**手动安装 jep。


## 环境要求

- **C & C++ 编译器**
    
    需要系统至少拥有 C 编译器（对于 MSVC 需要 C++ 编译器）。
    
    例如对于 windows 需要
    [MSVC](https://visualstudio.microsoft.com/zh-hans/vs/features/cplusplus/)
    ，对于 linux 需要
    [GCC](https://gcc.gnu.org/)
    （当然也可以选择其他的编译器）
    
- [**CMake**](https://cmake.org/) (`>= 3.14`)
    
    为了实现跨平台，jse 将 jep 库的 jni 源码部分直接封装到
    `jse-all.jar` 中，并在调用到原生方法时自动编译。
    为了保证不同平台下编译流程一致，以及能使用
    [CLion](https://www.jetbrains.com/clion/) 进行调试，
    这里使用 cmake 来管理这部分项目。
    
    这里借助了 cmake 的 `find_package` 方法来自动找到 jni 和 python 相关依赖，
    此功能至少需要 cmake 版本为 `3.14`。
    
- **Python** (`>= 3.6`)
    
    jse 会通过 cmake 的 `find_package(Python COMPONENTS Development)`
    来自动检测系统当前的 python 环境，
    根据 [jep 的文档](https://github.com/ninia/jep)，要求
    python 版本至少为 `3.6`。
    
    暂未测试对于拥有多个 python 虚拟环境时是否能正常工作。
    
- **NumPy** (`>= 1.7`，可选)
    
    jse 会通过 cmake 的 `find_package(Python COMPONENTS Development NumPy)`
    来自动检测系统当前的 numpy 环境，
    根据 [jep 的文档](https://github.com/ninia/jep)，要求
    numpy 版本至少为 `1.7`。
    
    如果没有检测到 numpy 则不会有对 ndarray 的相关支持。


## 基本使用方法

在上述环境配置完成后，第一次调用 `jse.code.SP.Python`
相关方法时会自动编译需要的 jni 库。

jse 中所有对 python 的支持都位于
[jse.code.SP.Python](../src/main/java/jse/code/SP.java)，
可以通过 `SP.Python.exec` 来执行 python 语句，
通过 `SP.Python.set` / `SP.Python.setValue`
来设置 python 的值（自动转换为 python 中的对象），
通过 `SP.Python.get` / `SP.Python.getValue`
类获取 python 的值（自动转换为 groovy 中的对象）：

- 输入脚本（`jse code/cross/python1`
  [⤤](../example/code/cross/python1.groovy)）：
    
    ```groovy
    import jse.code.SP
    
    SP.Python.set('a', [0, 1, 2, 3, 4, 5])
    SP.Python.exec('print(type(a))')
    SP.Python.exec('print(a[1:-1])')
    def a = SP.Python.get('a')
    println(a.class.name)
    println(a)
    ```
    
- 输出：
    
    ```
    <class 'java.util.ArrayList'>
    [1, 2, 3, 4]
    java.util.ArrayList
    [0, 1, 2, 3, 4, 5]
    ```

jse 中对于在 groovy 中调用 python 对象的属性提供了简化，
在绝大多数情况下可以像原生对象一样使用：

- 输入脚本（`jse code/cross/python2`
  [⤤](../example/code/cross/python2.groovy)）：
    
    ```groovy
    import jse.code.SP

    // 导入 numpy 和 matplotlib.pyplot，并作为 np 和 plt
    SP.Python.exec('import numpy')
    SP.Python.exec('import matplotlib.pyplot')
    def np = SP.Python.getClass('numpy')
    def plt = SP.Python.getClass('matplotlib.pyplot')

    // 可以像在 python 中一样的方法来使用，输出会自动转为 groovy 对象
    def x = np.linspace(0.0, 2.0*np.pi, 20)
    def y = np.sin(x)
    println(x.class.name)
    println(x.data.class.name)
    println(x.data)

    // groovy 的 Map 输入自动转为 python 的 kwargs 输入
    plt.figure(figsize: [4, 3])
    plt.plot(x, y)
    plt.show()
    ```
    
- 输出：
    
    ```
    jep.NDArray
    [D
    [0.0, 0.3306939635357677, 0.6613879270715354, 0.992081890607303, ..., 6.283185307179586]
    [20]
    sys:1: UserWarning: Starting a Matplotlib GUI outside of the main thread will likely fail.
    ```


## NDArray 的使用

许多 python 中的方法会使用 numpy 的数组 `ndarray`，jep
在 java 侧提供了一个等价的 `jep.NDArray` 类。现在
jse 提供了更方便的接口来将 jse 内的向量和 `jep.NDArray` 进行转换。

对于输出 `ndarray` 的 python 方法，确认其维数后，调用
`jse.math.vector.Vectors` 或 `jse.math.matrix.Matrices`
中的 `fromNumpy` 方法即可创建对应的 jse 向量或矩阵：

- 输入脚本（`jse code/cross/python3`
  [⤤](../example/code/cross/python3.groovy)）：
    
    ```groovy
    import jse.code.SP
    import jse.math.vector.Vectors

    SP.Python.exec('import numpy as np')
    SP.Python.exec('a = np.array([1, 2, 3, 4, 5, 6])')
    SP.Python.exec('print(a)')

    def a = Vectors.fromNumpy(SP.Python.get('a'))
    println(a)
    ```
     
- 输出：
    
    ```
    [1 2 3 4 5 6]
    6-length Integer Vector:
    1 2 3 4 5 6
    ```


对于需要输入 `ndarray` 的方法，可以通过调用 `jse.math.vector.IVector`
或 `jse.math.matrix.IMatrix` 中的 `numpy()` 方法将其转换为
`jep.NDArray`，即可自动被 jep 识别并转换使用：

- 输入脚本（`jse code/cross/python4`
  [⤤](../example/code/cross/python4.groovy)）：
    
    ```groovy
    import jse.code.SP
    import static jse.code.UT.Math.*

    def x = linspace(0.0, 2.0*pi, 20)
    def y = sin(x)

    SP.Python.exec('import matplotlib.pyplot')
    def plt = SP.Python.getClass('matplotlib.pyplot')

    plt.figure(figsize: [4, 3])
    plt.plot(x.numpy(), y.numpy())
    plt.show()
    ```

