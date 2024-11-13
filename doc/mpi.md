- [MPI 支持](mpi.md)
    - [环境要求](#环境要求)
    - [使用方法](#使用方法)
    - [可选配置](#可选配置)
- [**⟶ 目录**](contents.md)

# MPI 支持

在 jse 中提供了调用原生的 MPI 的支持，
具体为通过 [jni](https://www.baeldung.com/jni) 
来调用本地的 MPI 库（如 `libmpich.so`），
然后通过类似 [mpi4py](https://mpi4py.readthedocs.io/en/stable/mpi4py.html)
或 [mpj express](http://www.mpjexpress.org/)
或 [openmpi java binding](https://docs.open-mpi.org/en/v5.0.x/features/java.html)
的方法原生调用 mpi。

> **注意**：考虑到其他的库要么存在兼容问题，要么已经非常过时难用，
> 这里重新通过 jni 实现了一套独立的方案，效率和兼容性都会更高。
> 

## 环境要求

- **C & C++ 编译器**
    
    需要系统至少拥有 C 编译器（对于 MSVC 需要 C++ 编译器）。
    
    例如对于 windows 需要
    [MSVC](https://visualstudio.microsoft.com/zh-hans/vs/features/cplusplus/)
    ，对于 linux 需要
    [GCC](https://gcc.gnu.org/)
    （当然也可以选择其他的编译器）
    
- [**CMake**](https://cmake.org/) (`>= 3.14`)
    
    为了实现跨平台，jse 将调用 mpi 库需要的 jni 源码直接封装到
    `jse-all.jar` 中，并在调用到原生方法时自动编译。
    为了保证不同平台下编译流程一致，以及能使用
    [CLion](https://www.jetbrains.com/clion/) 进行调试，
    这里使用 cmake 来管理这部分项目。
    
    这里借助了 cmake 的 `find_package` 方法来自动找到 jni 和 mpi 依赖，
    此功能至少需要 cmake 版本为 `3.14`。
    
- **MPI**
    
    jse 会通过 cmake 的 `find_package(MPI)` 来自动检测系统当前的 MPI 环境，
    可以通过将需要的 MPI 路径设置到最上方来手动指定 MPI。
    
    例如对于 windows 可以使用
    [Microsoft MPI](https://www.microsoft.com/download/details.aspx?id=105289)
    （`msmpisdk.msi` 和 `msmpisetup.exe` 都需要安装），对于 linux 可以使用
    [MPICH](https://www.mpich.org/)
    （建议直接通过 `apt-get install mpich` 安装）


### 使用方法

在上述环境配置完成后，第一次调用 `jse.parallel.MPI`
相关方法时会自动编译需要的 jni 库。
而后可以使用类似 `mpiexec -np 4 jse path/to/script`
的方法来通过 MPI 运行脚本。

> **注意**：
> 
> 1. 在 MPI 环境下编译 jni 库会导致多个进程同时访问相同目录，
> 最终导致编译失败（并且这是不可能从程序中预先知道的，因为这时还没有 MPI 环境！），
> 因此第一次运行 MPI 代码时需要在“串行”环境下运行，保证相关
> jni 库正常编译。
>
> 2. 在 windows 下，可能需要手动指定指定的脚本才能正常使用 mpiexec 运行，例如
> `mpiexec -np 4 jse.bat path/to/script`
> 

原生 MPI 类位于 [`jse.parallel.MPI`](../src/main/java/jse/parallel/MPI.java)，
jse 提供了两套接口逻辑，一种位于 `jse.parallel.MPI.Native`，
可以通过静态导入，然后直接使用和 MPI 标准中类似的方法：

```groovy
import static jse.parallel.MPI.Native.*

MPI_Init(args)
int me = MPI_Comm_rank(MPI_COMM_WORLD)
println('Hi from <'+me+'>')
MPI_Finalize()
```

另一种则直接位于 `jse.parallel.MPI`，为一套更加面向对象的接口：

```groovy
import jse.parallel.MPI

MPI.init(args)
int me = MPI.Comm.WORLD.rank()
println('Hi from <'+me+'>')
MPI.shutdown() // `finalize()` has been used in java
```

> jse 和大多数的提供给 java 的 MPI 绑定不同，没有使用 java 的 `finalize()`
> 方法来关闭 MPI，因为此名称已经被 java 使用，且已经弃用，具体可参考
> [Effective Java - Item 7 Avoid finalizers](https://raysxysun.github.io/java/2016/08/06/Effective-Java7/)
> 

在上述环境配置完成后，第一次调用 `MPI` 相关方法时会自动编译需要的 jni 库。
而后可以使用类似 `mpiexec -np 4 jse path/to/script`
的方法来通过 MPI 运行脚本中的 MPI 程序。

> **注意**：在 MPI 环境下编译 jni 库会导致多个进程同时访问相同目录，
> 最终导致编译失败（并且这是不可能从程序中预先知道的，因为这时还没有 MPI 环境！），
> 因此第一次运行 MPI 代码时需要在“串行”环境下运行，保证相关
> jni 库正常编译。
>
> 在 windows 下，可能需要手动指定指定的脚本才能正常使用 mpiexec 运行，例如
> `mpiexec -np 4 jse.bat path/to/script`
> 

由于 MPI 中接口较多，并且源码中已经包含了
[Microsoft MPI](https://learn.microsoft.com/message-passing-interface/microsoft-mpi)
使用的文档，这里只列出几点 jse 中的 MPI 和 MPI 标准中的区别：

-   所有收发的数据类型（`byte[]`, `double[]`, `int[]`, ...）都会自动检测，
    而不需要额外输入一个数据类型的参数（数据长度依旧需要输入）。
  
-   取消了 `MPI_IN_PLACE` 参数，所有 `MPI_IN_PLACE`
    选项直接通过只指定一个收发的数据即可，jse
    会自动认为在进行 `MPI_IN_PLACE` 的传输，并处理好具体需要设置成
    `MPI_IN_PLACE` 的位置。
    
-   为了符合 jse 中的标准，对于面向对象的接口的部分函数进行了修改名称：
    
    | MPI         | jse          |
    | ----------- | ------------ |
    | `dup`       | `copy`       |
    | `finalize`  | `shutdown`   |
    | `finalized` | `isShutdown` |
    | `free`      | `shutdown`   |
    
-   为了避免重复创建数组，以及 java 数组和 C 数组之间的转换，
    这里还提供了一系列单个数据的收发函数，使用大写字母结尾（如 `bcastD`，`sendL`），
    具体对应的类型如下：
    
    | MARKER  | type      |
    | ------- | --------- |
    | `B`     | `byte`    |
    | `D`     | `double`  |
    | `Z`     | `boolean` |
    | `C`     | `char`    |
    | `S`     | `short`   |
    | `I`     | `int`     |
    | `L`     | `long`    |
    | `F`     | `float`   |
    | `Str`\* | `String`  |
    
    > **\***：没有做特殊优化，仅用于方便使用。
    >

具体实例可参看脚本 `example/mpi/bcast`
[⤤](../example/code/mpi/bcast.groovy)
以及 `example/mpi/split`
[⤤](../example/code/mpi/split.groovy)。


## 可选配置

jse 在 [`jse.parallel.MPI.Conf`](../src/main/java/jse/parallel/MPI.java)
中提供了一些可选的配置来适应更加复杂的情况。

可以在脚本中手动设置这些配置从而实现不同脚本使用不同的配置，
也可以通过环境变量的方式进行设置，从而实现全局的默认配置。

- **`CMAKE_C_COMPILER`**
    
    描述：自定义使用 cmake 构建 MPI jni 部分使用的 C 编译器。
    
    类型：`String`
    
    默认值：`null`（cmake 自动检测）
    
    环境变量名称：`JSE_CMAKE_C_COMPILER_MPI` / `JSE_CMAKE_C_COMPILER`
    
    -----------------------------
    
- **`CMAKE_CXX_COMPILER`**
    
    描述：自定义使用 cmake 构建 MPI jni 部分使用的 C++ 编译器。
    
    类型：`String`
    
    默认值：`null`（cmake 自动检测）
    
    环境变量名称：`JSE_CMAKE_CXX_COMPILER_MPI` / `JSE_CMAKE_CXX_COMPILER`
    
    -----------------------------
    
- **`CMAKE_C_FLAGS`**
    
    描述：自定义使用 cmake 构建 MPI jni 部分使用的 C 的 flags。
    
    类型：`String`
    
    默认值：`null`（不添加 flags）
    
    环境变量名称：`JSE_CMAKE_C_FLAGS_MPI` / `JSE_CMAKE_C_FLAGS`
    
    -----------------------------
    
- **`CMAKE_CXX_FLAGS`**
    
    描述：自定义使用 cmake 构建 MPI jni 部分使用的 C++ 的 flags。
    
    类型：`String`
    
    默认值：`null`（不添加 flags）
    
    环境变量名称：`JSE_CMAKE_C_FLAGS_MPI` / `JSE_CMAKE_CXX_FLAGS`
    
    -----------------------------
    
- **`USE_MIMALLOC`**
    
    描述：是否使用 [mimalloc](https://github.com/microsoft/mimalloc)
    来加速 C 的内存分配，这对于 MPI 过程中频繁出现的
    java 数组和 C 数组的转换很有效。
    
    类型：`boolean`
    
    默认值：`true`
    
    环境变量名称：`JSE_USE_MIMALLOC_MPI` / `JSE_USE_MIMALLOC`
    
    > **注意**：mimalloc 的编译需要较高版本的编译器，在一些旧平台上可能会编译失败
    > 
    
    -----------------------------
    
- **`REDIRECT_MPIJNI_LIB`**
    
    描述：重定向 MPI jni 动态库的路径，
    设置后会完全关闭 jse 关于 MPI jni 动态库的查找和构建并直接使用此动态库。
    
    类型：`String`
    
    默认值：`null`
    
    环境变量名称：`JSE_REDIRECT_MPIJNI_LIB`


