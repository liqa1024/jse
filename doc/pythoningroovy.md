- 交叉编程
    - [groovy 中调用 python](pythoningroovy.md)
        - [环境要求](#环境要求)
        - [基本使用方法](#基本使用方法)
        - [NDArray 创建和使用](#ndarray-创建和使用)
        - [安装仅 jse 使用的 python 库](#安装仅-jse-使用的-python-库)
    - [python 中调用 jse（jep）]()
    - [python 中调用 jse（py4j）]()
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

- 输入脚本（`jse example/cross/python1`
  [⤤](../release/script/groovy/example/cross/python1.groovy)）：
    
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

- 输入脚本（`jse example/cross/python2`
  [⤤](../release/script/groovy/example/cross/python2.groovy)）：
    
    ```groovy
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


## NDArray 创建和使用

许多 python 中的方法会使用 numpy 的 ndarray 作为输入，
jep 提供了更加快速直接的方法创建一个 ndarray：

```
任意 java 中的数据 ⟶ java 中的 double[] 数据 ⟶
java 中的 jep.NDArray 对象 ⟶ 自动转换为 python 中的 ndarray 数据
```

例如直接创建一个 `jep.NDArray` 对象并使用：

- 输入脚本（`jse example/cross/python3`
  [⤤](../release/script/groovy/example/cross/python3.groovy)）：
    
    ```groovy
    import jep.NDArray
    import jse.code.SP
    
    def a = new NDArray([1, 2, 3, 4, 5, 6] as double[], 3, 2)

    SP.Python.set('a', a)
    SP.Python.exec('print(type(a))')
    SP.Python.exec('print(a)')
    SP.Python.exec('print(a.shape)')
    ```
    
    > 注意 `as double[]` 必要，因为 groovy 默认会创建 `ArrayList` 而不是 `double[]`
    
- 输出：
    
    ```
    <class 'numpy.ndarray'>
    [[1. 2.]
     [3. 4.]
     [5. 6.]]
    (3, 2)
    ```

将 `jse.math.vector.IVector` 和 `jse.math.matrix.IMatrix`
转为 `jep.NDArray` 对象并使用：

- 输入脚本（`jse example/cross/python4`
  [⤤](../release/script/groovy/example/cross/python4.groovy)）：
    
    ```groovy
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
    ```
    
- 输出：
    
    ```
    [1. 2. 3. 4. 5. 6.]
    (6,)
    [[11. 12. 13. 14. 15.]
     [21. 22. 23. 24. 25.]
     [31. 32. 33. 34. 35.]
     [41. 42. 43. 44. 45.]]
    (4, 5)
    ```


## 安装仅 jse 使用的 python 库

一般情况下，jse 可以直接使用当前 python 环境下所有的第三方库，
但和一般的 python 脚本不同的是，jse 不能直接使用运行目录（`pwd`）
下的 python 库，作为替代，jse 会将 `script/python`
目录添加到 python 的 `sys.path` 中，
因此对于项目独立使用的 python 脚本或库需要添加到此目录下。

有时可能希望为 jse 安装专门的 python 库，则可以将其置于
`${JSE_HOME}/lib/python`，例如安装了 ase 后的文件结构大致为：

```
${JSE_HOME}
    ├─lib
    │   ├─jep
    │   ├─python
    │   │   ├─ase
    │   │   └─bin
    │   └─jse-all.jar
    └─jse
```

此时所有使用此目录下 jse 的 groovy 脚本都能检测到这个库，
而一般的 python 环境不会检测到此库。

也可以通过 `SP.Python.downloadPackage`
和 `SP.Python.installPackage` 两个方法来通过 pip
自动下载和安装 python 包到 `${JSE_HOME}/lib/python`，
`downloadPackage` 会联网下载 pip 安装包到
`${JSE_HOME}/lib/.pypkg`，而 `installPackage`
会在此目录查找符合要求的跑进行安装。

