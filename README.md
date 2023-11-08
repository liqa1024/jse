# 简介
使用 [java](https://en.wikipedia.org/wiki/Java_%28programming_language%29)
编写的项目管理工具，目前提供了：
- 通用的任务提交接口（本地，WSL，powershell，mpi，srun，ssh，...）
- 针对 Lammps 输出文件的读写（data，dump，log）
- 针对 Lammps 运行的输入文件支持并内置常用的 Lammps 运行方式的实现
- 通用的原子参量计算（RDF，SF，Q4/Q6/Ql）
- 其他常见操作（文件读写，读写 json、csv、yaml，常见的数学运算，简单的绘图，...）
- 简单跨语言编程支持（python，matlab，java/groovy）
- 进阶的模拟相关算法（FFS，KMC）

<!-- # 为何选择 java
- **较高性能**：由于经过了编译操作，性能更加接近 C++ 而不是 python 这种脚本语言。并且有成熟的 api 实现并行。
- **高兼容性**：相比 C++，其不受平台和编译器的影响，不用在迁移平台时考虑兼容性的问题。
- **其他语言的兼容性**：在 matlab 中可以原生的兼容 java 程序，在 python 中可以使用类似。
  [py4j](https://www.py4j.org/) 这种库来简单的实现对 java 程序的支持，实际使用体验和原生程序基本没有区别。
- **成熟的编辑器支持**：[IntelliJ IDEA](https://www.jetbrains.com/idea/) 等编辑器都对 java 提供了成熟的支持，
  开发会更加高效。
- **丰富的第三方库**：[Maven 仓库](https://mvnrepository.com/) 中有大量的 java 的第三方库可供使用，
  可以避免重复造轮子。
- **成熟的项目管理工具**：[Gradle](https://gradle.org/) 或者 [Maven](https://maven.apache.org/)
  工具现在可以非常方便的管理项目，（相比 C++ 中的 cmake）不需要担心其他人无法成功完成编译。 -->


# 如何使用
首先从 [**Release**](https://github.com/CHanzyLazer/jtool/releases/latest) 
中下载 `release.zip` 文件，为使用此工具包的项目的一般格式。

需要系统拥有 jdk，要求版本至少为 jdk8，建议直接使用 jdk17，
可以从 [**这里**](https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/) 下载 Adoptium 版本的 jdk，
或者从 [**这里**](https://www.oracle.com/java/technologies/downloads/#java17)
下载 Oracle 版本的 jdk。


## 0. 项目结构
具体项目结构可以参考 `release.zip` 文件，大致如下：

```
└─release
    ├─jtool
    ├─jtool.bat
    ├─lib
    │   └─jtool-all.jar
    ├─script
    │   ├─groovy
    │   │   ├─obj
    │   │   ├─test
    │   │   ...
    │   │
    │   ├─python
    │   │   ├─test
    │   │   ...
    │   │
    │   └─matlab
    │       └─include
    │           ├─addjpath.m
    │           └─rmjpath.m
    ├─lmp
    │   ├─data
    │   └─potential
    ├─.idea
    ├─.run
    └─.vscode
```

- `release` 目录作为项目的根目录，所有相对路径都会从这个目录开始，
  并且**所有脚本都需要在这个目录下运行**（即使脚本本身可能不在这个目录）。

- `jtool` 和 `jtool.bat` 分别为用来在 linux 和 windows 下运行此程序的脚本，
  这样可以在两个系统下都可以使用同样的类似 `./jtool xxx` 的指令来运行此程序。

- `lib` 目录存放库文件，默认情况下只会有一个 `jtool-all.jar` 文件。

- `script` 目录存放项目所有的脚本文件，并且如上不同语言分配不同的文件夹。
  **注意不同语言只有放在上述目录指定文件夹中才能被程序检测到，如对于 groovy 脚本为 `script/groovy/*`，对于 python 脚本为 `script/python/*`。**

- `lmp` 存放 lammps 相关的数据，例如 lammps 的 data 文件则在 `lmp/data/*`，
  力场文件则在 `lmp/potential/*`（非硬性）。

- `.idea` 和 `.run` 为 IntelliJ IDEA 识别此项目文件夹以及运行脚本需要的设置。

- `.vscode` 为 VScode 运行脚本需要的设置。


## 1. 使用 python
在 python 中，可以使用第三方库来使用调用 java 代码，例如使用 [py4j](https://www.py4j.org/)：

```python
from py4j.java_gateway import JavaGateway
GATEWAY = JavaGateway.launch_gateway(classpath='lib/jtool-all.jar')
```

创建了一个 `GATEWAY`，然后通过 `GATEWAY` 来 "导入" java 的类，例如 lammps 的相关类 `Lmpdat` 和 `Dump`：

```python
Lmpdat = GATEWAY.jvm.jtool.lmp.Lmpdat
Dump = GATEWAY.jvm.jtool.lmp.Dump
```

之后就像使用一个 python 的内部类一样使用上述 `Lmpdat` 和 `Dump` 类，
例如读取 lammps 的 data 文件并转换成 dump 文件输出：

```python
lmpdat = Lmpdat.read('path/to/lammps/data/file')
dump = Dump.fromAtomData(lmpdat)
dump.write('path/to/lammps/dump/file')
```

最后记得关闭 GATEWAY：

```python
GATEWAY.shutdown()
```

> **注意**：默认情况下 py4j 不会输出 java 的信息到控制台，可以在创建 `GATEWAY` 时重新定向输出流来解决这个问题：
> 
> ```python
> import sys
> from py4j.java_gateway import JavaGateway
> GATEWAY = JavaGateway.launch_gateway(classpath='lib/jtool-all.jar', redirect_stdout=sys.stdout)
> ```
> 

## 2. 使用 Groovy 脚本（推荐）
[Groovy](http://www.groovy-lang.org/) 是一款原生支持 java 的脚本语言，并且也提供了很多很方便的语法糖
（具体可以参考 [这篇文章](https://zhuanlan.zhihu.com/p/257969931)），因此本工具包原生支持 Groovy。

> **注意**：本工具包将整个 Groovy 需要的运行库都包含到一起，因此运行 Groovy 脚本**不需要**额外安装 Groovy。

在项目根目录中直接运行：

```shell
./jtool -t "print('hello world')"
```

即可将输入的文本当作 Groovy 脚本进行执行，也可使用：

```shell
./jtool -f path/to/script.groovy
```

直接执行位于 `path/to/script.groovy` 的 Groovy 脚本，在这个情况下 `-f` 参数可以省略

> 上述将 lammps 的 data 文件并转换成 dump 文件的例子在 Groovy 中则是：
> 
> ```groovy
> import jtool.lmp.Lmpdat;
> import jtool.lmp.Dump;
> 
> lmpdat = Lmpdat.read('path/to/lammps/data/file');
> dump = Dump.fromAtomData(lmpdat);
> dump.write('path/to/lammps/dump/file');
> ```
> 
> 可能会更倾向于在一行中完成：
> 
> ```groovy
> import jtool.lmp.*;
> Dump.fromAtomData(Lmpdat.read('path/to/lammps/data/file')).write('path/to/lammps/dump/file');
> ```
> 

<br>

--------------------------------

这里也支持在编辑器下直接运行 Groovy 脚本，目前提供了对 VScode 和 IntelliJ IDEA 的支持：

### 2.1 在 VScode 中
提供了一个默认的运行和调试 `jtool-RunCurrentScript` 可以直接运行当前打开的脚本，底层也是直接调用的
`./jtool` 指令，因此输出会在控制台部分。

### 2.2 在 IntelliJ IDEA 中（推荐）
也提供了一个运行配置 `jtool-RunCurrentScript`（请不要使用自带的运行当前脚本，因为本项目实际不依赖 Groovy），
底层使用的直接运行 jar 包，因此会有更加准确的调试。

IntelliJ IDEA 中会有代码补全提示以及正确的高亮，因此更推荐使用。
第一次打开可能需要设置 jdk 的路径（已经为 IntelliJ 安装了汉化插件）：

```
左上角“文件” ⟶ 项目结构 ⟶ 左边栏选择“项目设置-项目” ⟶ SDK 选择本地安装的 JDK
⟶ 语言级别：8 - lambda、类型注解等 ⟶ 右下角“确定”
```

<!-- $$\begin{align*} &
\text{左上角“文件”}\longrightarrow 
\text{项目结构}\longrightarrow 
\text{左边栏选择“项目设置-项目”}\longrightarrow 
\text{SDK 选择本地安装的 JDK} \\& \longrightarrow
\text{语言级别：8 - lambda、类型注解等}\longrightarrow
\text{右下角“确定”}
\end{align*}$$ -->


## 3. 使用 Matlab
在 matlab 中，首先需要将项目的根目录（`release` 目录）作为工作目录，然后导入 java 包的路径：

```matlab
javaaddpath('lib/jtool-all.jar');
```

软件包为 `jtool.xxx`，如果希望使用方便可以先进行 import 需要的包，
例如 lammps 相关的包：

```matlab
import jtool.lmp.*
```

> **注意**：matlab 似乎不能直接使用 `import jtool.lmp.Lmpdat` 这种写法来导入单个类

同样对于上述读取 lammps 的 data 文件并转换成 dump 文件：

```matlab
lmpdat = Lmpdat.read('path/to/lammps/data/file');
dump = Dump.fromAtomData(lmpdat);
dump.write('path/to/lammps/dump/file');
```

处理完成后最后记得移除 java 路径：

```matlab
clear;
javarmpath('lib/jtool-all.jar');
```

> **注意**：不能重复导入同一个 java 路径，否则会出现报错（虽然目前看起来不影响使用），
> 因此实际项目中我会将此方法进行包装，检测避免重复导入，包装后的函数为：
> 
> ```matlab
> addjpath('lib/jtool-all.jar');
> rmjpath('lib/jtool-all.jar');
> ```
> 
> 两个函数都位于目录 `script/matlab/include`
> 


# 使用文档
详细接口介绍参考 [使用文档](doc/contents.md)（正在编写）。


# 编译项目
本项目使用 [Gradle](https://gradle.org/) 进行管理（不需要安装 Gradle）。

在根目录运行 `./gradlew build` 即可进行编译，默认会将 jar 文件输出到 `release/lib` 文件夹。

