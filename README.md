# 简介
jse (Java Simulation Environment) 是
使用 [java](https://en.wikipedia.org/wiki/Java_(programming_language))
编写的模拟环境，目前提供了：
- 通用的任务提交接口（bash，powershell，ssh，...）
- 高效的 lammps 数据文件读写（data，dump，log）
- 高效的 vasp 数据文件读写（POSCAR，XDATCAR）
- *原生* 调用 lammps（不经过 python）
- *原生* 的 MPI 支持（不经过 mpi4py）
- 原子结构参量计算（RDF，SF，Q4/Q6/Ql，Voronoi）
- 其他常见操作（文件读写，读写 csv、json、yaml，压缩解压，简单的绘图，...）
- 高效的纯 java 数学库实现
- 跨语言编程支持（python，matlab，java/groovy）
- jupyter 支持（将 jse 作为 jupyter 内核）

> *原生（Native）* 指编写 C 代码并通过 [jni](https://www.baeldung.com/jni)
> 来直接调用相关的动态库（`*.dll` / `*.so`）方法。
> 


# 软件特点

- **全栈**：
    
    jse 提供了：
    
    ```
    原子结构创建 ⟶ 上传文件 ⟶ 提交任务 ⟶ 执行任务
    ⟶ 下载文件 ⟶ 读取文件 ⟶ 数据后处理 ⟶ 绘制结果
    ⟶ 保存结果到文件
    ```
    
    全部过程的支持，实现了“全栈”的原子模拟支持，不需要频繁切换软件。
    
- **易用**：
    
    易安装：
    - jse 通过 java 编写，确保了跨平台的兼容性。
    - jse 90% 的功能不需要额外安装第三方依赖，这对于没有网络环境的地方尤为重要。
    - jse 剩下的 jni 部分会在使用时自动编译确保兼容性。
    
    易使用：
    - jse 通过 [groovy 语言](http://www.groovy-lang.org/)
    使用，相比 C/fortran/java 更加简单，相比 python 更加高效。
    - jse 支持使用现代的 IDE（[IntelliJ IDEA](https://www.jetbrains.com/idea/)）
    调试 groovy 脚本，拥有完整的代码提示。
    - jse 支持通过 python 或 matlab 使用，帮助习惯这些语言的人快速上手。
    - jse 支持 jupyter 中使用，可以分段运行和调试。
    
    易扩展：
    - jse 支持直接调用 python 脚本，因此可以通过现有的 python 库直接扩展功能。
    - jse 支持直接扩展 jar 包，因此可以通过
    [maven 仓库](https://mvnrepository.com/)
    中现有的 java 库直接扩展功能。
    
- **高性能**：
    
    jse 通过 java 编写，理论上性能和 C/C++ 基本一致（大致比 python 快 100 倍，
    一般情况下会比 fortran 快）
    向量运算上考虑了
    [SIMD](https://en.wikipedia.org/wiki/Single_instruction,_multiple_data)
    优化，对于近邻搜索使用了类似
    [Cell lists](https://en.wikipedia.org/wiki/Cell_lists)
    的方法进行加速，最终会比常规的实现快 1000 倍以上。
    
    jse 基于
    [java nio](https://en.wikipedia.org/wiki/Non-blocking_I/O_%28Java%29)
    实现文件读写，通常可以利用完全硬盘的读写速度，
    会比常规的 python 实现快 10 倍以上。


# 如何使用
参考 [基本使用方式](doc/usage.md)，
详细接口介绍参考 [使用文档](doc/contents.md) 。

现在也可以直接参考 [自动生成 JavaDoc](https://chanzylazer.github.io/jse-API/) 。


# 编译项目
本项目使用 [Gradle](https://gradle.org/) 进行管理（不需要安装 Gradle）。

在根目录运行 `./gradlew build` 即可进行编译，
默认会将 jar 文件输出到 `release/lib` 文件夹。

