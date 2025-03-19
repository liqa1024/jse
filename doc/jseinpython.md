- 交叉编程
    - [groovy 中调用 python](pythoningroovy.md)
    - [python 中调用 jse](jseinpython.md)
        - [基本使用方法](#基本使用方法)
        - [和 numpy 的结合使用](#和-numpy-的结合使用)
        - [安装仅 jse 使用的 python 库](#安装仅-jse-使用的-python-库)
    - [matlab 中调用 jse]()
- [**⟶ 目录**](contents.md)


# python 中调用 jse

jse 支持通过内部的 [jep](https://github.com/ninia/jep)
库来直接使用 python，其中 jep 提供了 python 中调用 java 类的支持。

jse 已经内置了完整的 jep，并且会在需要时自动安装 jni 部分，
因此**不需要**手动安装 jep。

对于环境需求和 [groovy 中调用 python 的环境要求](pythoningroovy.md#环境要求)
一致，这里不再赘述。


## 基本使用方法

对于需要使用 jse 的 python 脚本（例如 `main.py`），
可以直接通过 `import` 的方式导入 java 以及 jse 中的类：

```python
from jse.lmp import Lmpdat
```

通过 `jse` 来运行此脚本而不是 `python`，从而让导入有效：

```shell
jse main.py
```

> - 在环境配置完成后，第一次通过 jse 运行 python 脚本时会自动编译需要的 jni 库。
> 
> - 在 idea 中也可以通过自动配置好的 `jse-RunCurrentScript` 来运行。
> 

具体 jse 中的类的使用和原本 groovy 中的使用几乎一致。

- 输入脚本（`jse code/cross/jse1.py`
  [⤤](../example/code/cross/jse1.py)）：
    
    ```python
    from jse.lmp import Lmpdat

    data = Lmpdat.read('lmp/data/data-glass')
    print(type(data))
    print('natoms:', data.natoms())
    ```
    
- 输出：
    
    ```
    <class 'jse.lmp.Lmpdat'>
    natoms: 4000
    ```


## 和 numpy 的结合使用

jse 提供了对 numpy 数组 `ndarray` 的支持，对于需要 `ndarray`
的情况，许多 jse 的类型提供了 `numpy()` 方法来直接转换成
`ndarray`。目前 `jse.atom.IXYZ`, `jse.atom.IAtom`, `jse.atom.IAtomData`, 
`jse.math.vector.IVector`, `jse.math.matrix.IMatrix`, `jse.math.table.ITable`
都支持直接转换成 numpy 的数组：


- 输入脚本（`jse code/cross/jse2.py`
  [⤤](../example/code/cross/jse2.py)）：
    
    ```python
    from jse.lmp import Lmpdat
    
    data = Lmpdat.read('lmp/data/CuFCC108.lmpdat')
    dataNp = data.numpy()
    print(dataNp.shape)
    print(dataNp)
    ```
    
    > 数据按行排列，顺序为 `x, y, z, id, type, vx, vy, vz`
    > 
    
- 输出：
    
    ```
    (108, 8)
    [[  0.      0.      0.      1.      1.      0.      0.      0.   ]
     [  1.805   1.805   0.      2.      1.      0.      0.      0.   ]
     [  1.805   0.      1.805   3.      1.      0.      0.      0.   ]
     [  0.      1.805   1.805   4.      1.      0.      0.      0.   ]
     ...
     [  9.025   7.22    9.025 107.      1.      0.      0.      0.   ]
     [  7.22    9.025   9.025 108.      1.      0.      0.      0.   ]]
    ```

对于需要 jse 向量或矩阵的输入的函数，可以通过
`jse.math.vector.Vectors` 或 `jse.math.matrix.Matrices`
中的 `fromNumpy` 方法将 numpy 数组转换成对应的
jse 向量或矩阵：

- 输入脚本（`jse code/cross/jse3.py`
  [⤤](../example/code/cross/jse3.py)）：
    
    ```python
    import numpy as np
    from jse.code import IO
    from jse.math.vector import Vectors
    
    x = np.linspace(0.0, 2.0*np.pi, 20)
    y = np.sin(x)
    
    IO.cols2csv([Vectors.fromNumpy(x), Vectors.fromNumpy(y)], '.temp/example/cross/sin.csv')
    ```


## 安装仅 jse 使用的 python 库

一般情况下，jse 可以直接使用当前 python 环境下所有的第三方库，
但有时可能希望为 jse 安装专门的 python 库，则可以将其置于
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

有时直接在 jse 目录下添加文件是不太现实的，
从 `2.8.2` 版本后支持通过环境变量 `JSE_PYTHON_EXLIB_DIRS`
来增加自定义路径的 python 库。
例如设置这样设置环境变量：

```shell
export JSE_PYTHON_EXLIB_DIRS=/software/pypkg
```

将需要安装的 python 库置于上述目录即可，例如对于上述 ase 库：

```
software
    └─pypkg
        ├─ase
        └─bin
```

`JSE_PYTHON_EXLIB_DIRS` 也支持设置多个路径，和 `PATH`
类似，在 linux 下使用 `:` 分隔，在 windows 下使用 `;`
分隔：

```shell
export JSE_PYTHON_EXLIB_DIRS=\
/software/pypkg1:/software/pypkg2:$JSE_PYTHON_EXLIB_DIRS
```

