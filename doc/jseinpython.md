- 交叉编程
    - [groovy 中调用 python](pythoningroovy.md)
    - [python 中调用 jse](jseinpython.md)
        - [基本使用方法](#基本使用方法)
        - [数据类型转换](#数据类型转换)
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


## 数据类型转换

由于 python 中并不强调类型，而 java 中强调，因此在交叉使用时可能会出现一些问题。
jep 已经自动处理大部分常见的数据类型转换，但是不是所有（特别是对于 jse 中的类型），
这里列出一些常见的问题和处理方法。

-   **NDArray 的处理**
    
    在 python 中常常会使用 numpy 的 ndarray
    存储数组信息，而绝大部分 jse 的函数不会接受这个类型，最简单的方法是使用：
    
    ```python
    a_list = list(a)
    ```
    
    将其转为 python 的 `list` 类型，此时会自动转换成 java
    的 `List` 类型。
    
-   **jse Vector 的处理**
    
    许多 jse 函数会返回 jse 的向量 `Vector` 类型，其当然可以像正常的 java
    类一样在 python 中直接使用，但需要注意这并不是 numpy 的
    ndarray，因此不能作为 numpy 或者 matplotlib 的输入。
    可以通过：
    
    ```python
    a_np = np.array(a.asList())
    ```
    
    将其先转为 list，然后再转换成 numpy 的 ndarray。
    
    > 注意这里 jse 的 Vector 并没有专门重载 python
    > 的运算符，因此不能通过方括号 `[]` 进行索引，因此需要直接调用方法：
    >
    > ```python
    > a.set(1, 3.14)
    > print(a.get(0))
    > ```
    >


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

