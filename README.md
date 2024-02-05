# 简介
jse (Java Simulation Environment) 是
使用 [java](https://en.wikipedia.org/wiki/Java_(programming_language))
编写的模拟环境，目前提供了：
- 通用的任务提交接口（bash，powershell，wsl，ssh，...）
- 针对 lammps 输出文件的读写（data，dump，log）以及输入文件支持
- 通用的 lammps 运行器以及原生调用 lammps（不经过 python）
- 通用的原子参量计算（RDF，SF，Q4/Q6/Ql，Voronoi）
- 简单高效的 MPI 支持（不经过 mpi4py）
- 其他常见操作（文件读写，读写 csv、json、yaml，压缩解压，简单的绘图，...）
- 高效的纯 java 数学库实现
- 简单跨语言编程支持（python，matlab，java/groovy）
- 进阶的模拟相关算法（FFS，KMC）


# 如何使用
参考 [基本使用方式](doc/usage.md)，
详细接口介绍参考 [使用文档（正在编写）](doc/contents.md)。


# 编译项目
本项目使用 [Gradle](https://gradle.org/) 进行管理（不需要安装 Gradle）。

在根目录运行 `./gradlew build` 即可进行编译，
默认会将 jar 文件输出到 `release/lib` 文件夹。

