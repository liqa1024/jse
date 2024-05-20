
- [NativeLmp 配置常见问题以及解决方法](nativelmp.md)
    - [在本地的 Windows 系统上配置](#在本地的-windows-系统上配置)
    - [在本地的 Linux 系统上配置](#在本地的-linux-系统上配置)
    - [离线情况下配置](#离线情况下配置)
    - [老版本 lammps 的配置](#老版本-lammps-的配置)
    - [将 jni 接口合并到 lammps 一起编译](#将-jni-接口合并到-lammps-一起编译)


# NativeLmp 配置常见问题以及解决方法

这里详细介绍配置 `NativeLmp` 可能会遇到的问题和解决方法，
在开始阅读之前确保已经阅读了 [原生运行 lammps 部分](lammps.md#原生运行-lammps)。
这里按照问题常见程度和解决的困难程度综合排序。


## 在本地的 Windows 系统上配置

原则上按照 [原生运行 lammps 部分](lammps.md#原生运行-lammps)
即可顺利完成配置，这里列出具体的步骤：

1. 确保系统已经安装 [**CMake**](https://cmake.org/) (`>= 3.14`)，可以通过在终端中运行：

    ```shell
    cmake --version
    ```
    
    输出：

    ```
    cmake version 3.28.1
    
    CMake suite maintained and supported by Kitware (kitware.com/cmake).
    ```
    
    而不是报错，即表示安装成功。
    
    **可能的问题**：
    
    - 无法使用终端：
        
        对于 windows11，直接在桌面右键可以看到在 `在终端中打开`，即可开启终端；
        对于 windows10，按住 `shift` 后在桌面右键，可以看到 `在此处打开 PowerShell 窗口`，即可开启终端。
        
    - 已经安装 CMake，但是依旧报错：
        
        一般是环境变量没有配置，在安装时注意勾选添加到环境变量（`PATH`）相关的选项。

2. 确保系统拥有 C/C++ 编译器，对于 windows 这里建议**直接使用微软的**
[**MSVC**](https://visualstudio.microsoft.com/zh-hans/vs/features/cplusplus/)
**编译器**（而不是 mingw 之类的编译器，可以避免很多问题）。
默认情况下 MSVC 不会将安装路径添加到环境变量中（并且这里实际上也不需要这么做），
因此不能直接通过终端来检测是否已经安装；可以通过在开始菜单中检查是否存在 `Visual Studio`
相关软件即可（注意区分 `Visual Studio Code`，两者不是同一个东西）。

3. 确保系统拥有 **MPI** 环境，对于 windows 这里建议**直接使用微软的**
[**Microsoft MPI**](https://www.microsoft.com/download/details.aspx?id=105289)
（而不是 MPICH 之类的库），注意 `msmpisdk.msi` 和 `msmpisetup.exe` 都需要安装。
可以通过在终端中运行：

    ```shell
    mpiexec -help
    ```
    
    输出：
    
    ```
    Microsoft MPI Startup Program [Version 10.1.12498.18]
    ...
    ```
    
    而不是报错，即表示安装成功。

    **可能的问题**：
    
    - `mpiexec -help` 成功执行，但 `NativeLmp` 依旧没有成功开启 MPI，或者
    MPI 初始化时找不到 MPI 库：
    
        确保同时安装了 `msmpisdk.msi` 和 `msmpisetup.exe`，通过删除 `lib`
        目录下的 `lmp` 文件夹和 `mpi` 文件夹来实现重新编译（可以只删除编译出来的动态库文件）。

4. 确保在有网络的环境下使用 `NativeLmp` 相关方法，从而让 jse 自动完成初始化的编译操作；
也可以通过直接调用方法 `NativeLmp.InitHelper.init()` 来手动初始化。
或者通过在终端中执行：

    ```shell
    jse -t 'jse.lmp.NativeLmp.InitHelper.init()'
    ```
    
    来初始化。
    
    一般会也会希望初始化一下 MPI 库，可以通过 `MPI.InitHelper.init()` 方法来实现手动初始化，
    也就是可以直接在终端中执行：

    ```shell
    jse -t 'jse.parallel.MPI.InitHelper.init(); jse.lmp.NativeLmp.InitHelper.init()'
    ```
    
    **可能的问题**：
    
    - `NativeLmp` 初始化时下载 lammps 失败：
    
        由于实际是通过 github 的链接下载的 lammps 源码，因此需要确保网络环境能够连接到
        github。如果使用代理注意需要通过开启 TUN 模式之类的方式，确保命令行终端也能成功代理。
        实在不行可以参考后续 [**离线情况下配置**](#离线情况下配置) 的思路。


## 在本地的 Linux 系统上配置

这里对应各种 linux 发行版或者是 WSL，并且要求较为现代的 linux，以及存在网络环境。

1. 确保系统已经安装 [**CMake**](https://cmake.org/) (`>= 3.14`)，可以通过在终端中运行：

    ```shell
    cmake --version
    ```
    
    输出：

    ```
    cmake version 3.16.3
    
    CMake suite maintained and supported by Kitware (kitware.com/cmake).
    ```
    
    而不是报错，即表示安装成功。
    
    对于 ubuntu 之类的发行版，这里当然建议直接使用：

    ```shell
    sudo apt-get install cmake
    ```
    
    来安装 CMake，一般情况下安装的版本肯定会高于 `3.14`，如果低于则需要**换源**（注意不是手动安装）。

2. 确保系统拥有 C/C++ 编译器，这里可以通过在终端中运行：

    ```shell
    gcc --version
    ```
    
    输出：
    
    ```
    gcc (Ubuntu 9.4.0-1ubuntu1~20.04.2) 9.4.0
    Copyright (C) 2019 Free Software Foundation, Inc.
    This is free software; see the source for copying conditions.  There is NO
    warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    ```
    
    以及：

    ```shell
    g++ --version
    ```
    
    输出：
    
    ```
    g++ (Ubuntu 9.4.0-1ubuntu1~20.04.2) 9.4.0
    Copyright (C) 2019 Free Software Foundation, Inc.
    This is free software; see the source for copying conditions.  There is NO
    warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    ```
    
    而不是报错，即表示安装成功。
    
    一般来说 linux 会自带 gcc 编译器，而 g++ 需要安装：

    ```shell
    sudo apt-get install g++
    ```
    
    **可能的问题**：
    
    - mimalloc 初始化时编译失败：
    
        此问题一般是 gcc/g++ 版本过低导致的（因此这里要求的是较为现代的 linux），
        经过测试 **`7.x.x` 以上的 gcc** 或者 **`9.x.x` 以上的 g++**
        就已经足够编译 mimalloc（看具体编译的是 C 的版本还是 C++ 版本），
        如果低于这个版本则需要**更新 gcc 和 g++**。

3. 确保系统拥有 **MPI** 环境，对于 linux 可以使用 [MPICH](https://www.mpich.org/)
或者 [OpenMPI](https://www.open-mpi.org/)，可以通过在终端中运行：

    ```shell
    mpiexec -version
    ```
    
    输出：
    
    ```
    HYDRA build details:
        Version:                                 3.3.2
        Release Date:                            Tue Nov 12 21:23:16 CST 2019
    ...
    ```
    
    而不是报错，即表示安装成功。
    
    对于 ubuntu 之类的发行版，这里当然建议直接使用：

    ```shell
    sudo apt-get install mpich
    ```
    
    来安装 MPICH。

4. 确保在有网络的环境下使用 `NativeLmp` 相关方法，从而让 jse 自动完成初始化的编译操作；
也可以通过直接调用方法 `NativeLmp.InitHelper.init()` 来手动初始化。
或者通过在终端中执行：

    ```shell
    jse -t 'jse.lmp.NativeLmp.InitHelper.init()'
    ```
    
    来初始化。
    
    一般会也会希望初始化一下 MPI 库，可以通过 `MPI.InitHelper.init()` 方法来实现手动初始化，
    也就是可以直接在终端中执行：

    ```shell
    jse -t 'jse.parallel.MPI.InitHelper.init(); jse.lmp.NativeLmp.InitHelper.init()'
    ```
    
    **可能的问题**：
    
    - `NativeLmp` 初始化时下载 lammps 失败：
    
        由于实际是通过 github 的链接下载的 lammps 源码，因此需要确保网络环境能够连接到
        github。实在不行可以参考后续 [**离线情况下配置**](#离线情况下配置) 的思路。


## 离线情况下配置

在离线情况下则需要手动下载并编译 lammps 动态库，当然这也可以让 lammps
版本的选择更加自由。

1. 手动从 github 上下载 [lammps 的源码](https://github.com/lammps/lammps/releases)
并解压到本地（也可以通过 `git clone` 之类的方式自动完成），尽量选择 **`Stable` 的版本**。

2. 确保已经存在 C++ 编译器环境后即可参考
[lammps 官方文档](https://docs.lammps.org/Build_basics.html#build-the-lammps-executable-and-library)
提供的方法来编译出 lammps 的动态库 `liblammps.so`（windows 上为 `liblammps.dll`）。
原则上可以使用 cmake 或者 make 编译，这里统一展示 cmake 编译的方法：
    
    - 在 lammps 源码目录下（包含 `src`, `LICENSE`, `README` 的目录）下创建一个 `build`
    目录并进入：

        ```shell
        mkdir build; cd build
        ```
    
    - 初始化 cmake 项目：

        ```shell
        cmake ../cmake
        ```
    
    - 设置 cmake 参数，通过此步来设置开启动态库编译：

        ```shell
        cmake -D BUILD_SHARED_LIBS=ON .
        ```
    
    - 设置编译类型以及库的输出路径为 `lib`：

        ```shell
        cmake -D CMAKE_BUILD_TYPE=Release -D CMAKE_ARCHIVE_OUTPUT_DIRECTORY=lib -D CMAKE_LIBRARY_OUTPUT_DIRECTORY=lib -D CMAKE_RUNTIME_OUTPUT_DIRECTORY=lib -D CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE=lib -D CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE=lib -D CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE=lib .
        ```
    
    - （可选）设置需要开启的包，如 `MANYBODY`：

        ```shell
        cmake -D PKG_MANYBODY=ON .
        ```
    
    - 开始编译 lammps：

        ```shell
        cmake --build .
        ```
    
    **可能的问题**：
    
    - 设置 cmake 参数时失败：
    
        注意命令最后的点 `.`（表示当前目录）。

3. 如果完全按照上述流程使用 cmake 编译，那么最终应该得到这样的文件结构：

    ```
    build
        ├─lib
        │   └─liblammps.so
        └─includes
            └─lammps
                ├─library.h
                ...
    ```
    
    而后只需将上述 build 目录设置到设置环境变量 `JSE_LMP_HOME` 即可：
    
    ```bash
    export JSE_LMP_HOME="path/to/lammps/build"
    ```
    
    之后 jse 会自动检测到此 lammps 的动态库，并不再尝试下载和编译 lammps。
    
    如果不符合上述文件结构，也可以手动调整至符合，或者直接将动态库放在
    `lib/lmp/native/build/lib`，头文件放在 `lib/lmp/native/build/includes`，
    此时**不再需要**手动设置环境变量 `JSE_LMP_HOME`。
    
    **可能的问题**：
    
    - `NativeLmp` 不能工作或部分功能不能正常工作：
    
        如果不是使用的最新的 lammps，参看 [**老版本 lammps 的配置**](#老版本-lammps-的配置)。
    
    - 直接运行 `NativeLmp` 没有问题，但和 `MPI`
    结合使用时会报错：
    
        确保编译 lammps 动态库时的 MPI 环境和初始化 jse 的 MPI
        时使用的相同的 MPI。如果依旧无法解决可以尝试
        [**将 jni 接口合并到 lammps 一起编译**](#将-jni-接口合并到-lammps-一起编译)。


## 老版本 lammps 的配置

因为各种原因可能会使用老版本的 lammps，除了需要通过上述
[**离线情况下配置**](#离线情况下配置) 的方法手动编译 lammps
动态库外，还需要进行一些环境变量的设置。

- 对于 `18Sep2020` 之前版本的 lammps，需要在环境变量中开启
`JSE_LMP_IS_OLD`，从而让 jni 部分会按照旧版的 lammps 接口来编译：

    ```bash
    export JSE_LMP_IS_OLD=ON
    ```

- 对于 `2Aug2023` 之前版本的 lammps，虽然支持 exception，但是 exception
接口并不支持 `NULL` 输入，为了避免崩溃需要在环境变量中关闭 `JSE_LMP_EXCEPTIONS_NULL_SUPPORT`：

    ```bash
    export JSE_LMP_EXCEPTIONS_NULL_SUPPORT=OFF
    ```

- 对于较旧的 lammps 版本编译时可能不会默认开启 exception 支持，需要在 cmake
编译之前通过 `cmake -D LAMMPS_EXCEPTIONS=ON .` 手动开启，或者在环境变量中关闭
`JSE_LMP_HAS_EXCEPTIONS`：

    ```bash
    export JSE_LMP_HAS_EXCEPTIONS=OFF
    ```


## 将 jni 接口合并到 lammps 一起编译

有些特殊环境下，无论如何设置，lammps 使用的 MPI 库都会和 jse 中的 MPI
库有所出入，导致不能混合 `NativeLmp` 和 `MPI` 一起编程，此时可以尝试直接将
jse 中的 jni 接口直接合并到 lammps 源码中，然后一次性完成编译。
具体操作如下：

1. 想办法获取到需要的 jse 中 jni 库的源码，较为简单的操作为（这里展示 `NativeLmp` 和 `MPI`）：

    - 通过各种压缩软件打开 `jse-all.jar` 包
    
    - 进入 `assets/lmp/src` 可以看到 `NativeLmp` 的源码文件
    `jse_lmp_NativeLmp.h` 和 `jse_lmp_NativeLmp.c`
    
    - 进入 `assets/mpi/src` 可以看到 `MPI` 的源码文件
    `jse_parallel_MPI_Native.h` 和 `jse_parallel_MPI_Native.c`
    
    - 进入 `assets/jniutil/src` 可以看到 jni 的通用源码文件 `jniutil.h`（header-only）

2. 直接将上述所有源码文件（`jse_lmp_NativeLmp.h`, `jse_lmp_NativeLmp.c`,
`jse_parallel_MPI_Native.h`, `jse_parallel_MPI_Native.c`, `jniutil.h`）拷贝到
lammps 源码的 `src` 目录下，和 `library.cpp` 位于相同的目录。

3. 将 `jse_lmp_NativeLmp.c` 文件重命名为 `jse_lmp_NativeLmp.cpp`，`jse_parallel_MPI_Native.c`
文件重命名为 `jse_parallel_MPI_Native.cpp`。这些源码统一和 lammps 使用 C++ 编译。
    
4. 打开 `jse_lmp_NativeLmp.cpp`，将第 7 行的 `#include "lammps/library.h"`
修改为 `#include "library.h"`。直接依赖相同目录下的 `library.h`。
    
5. 打开 lammps 的 cmake 配置文件 `cmake/CMakeLists.txt`，在
`target_include_directories(lammps PUBLIC $<BUILD_INTERFACE:${LAMMPS_SOURCE_DIR}>)`
（大致在 316 行）后添加 jni 相关配置以及必要的 define（理论上具体位置并不关键）：

    ```bash
    # add jni
    find_package(JNI QUIET)
    if(JNI_FOUND)
        include_directories(${JNI_INCLUDE_DIRS})
        target_link_libraries(lammps PUBLIC ${JNI_LIBRARIES})
    else()
        message(FATAL_ERROR "No Java Environment Found")
    endif()

    # mpi define for lammps
    add_definitions(-DLAMMPS_LIB_MPI)

    # this lammps exception support null
    add_definitions(-DLAMMPS_EXCEPTIONS_NULL_SUPPORT)
    ```
    
    其中对于 exception 接口不支持 `NULL` 输入的 lammps 版本不需要最后一行的定义。
    
6. 此时已经基本完成和 lammps 的合并，为了提高 C 中内存分配速度，
这里建议在 `cmake/CMakeLists.txt` 中再添加一个 mimalloc 相关的依赖：

    ```bash
    # add mimalloc
    option(JSE_USE_MIMALLOC "Use mimalloc to accelerate `malloc` operation" ON)
    if(JSE_USE_MIMALLOC)
        add_definitions(-DUSE_MIMALLOC)
        include_directories($ENV{JSE_MIMALLOC_INCLUDE_DIR})
        target_link_libraries(lammps PUBLIC $ENV{JSE_MIMALLOC_LIB_PATH})
    endif()
    ```
    
    然后先单独编译好 mimalloc 库，最为简单的方式是通过在终端中执行：

    ```shell
    jse -t 'jse.clib.MiMalloc.InitHelper.init()'
    ```
    
    来单独编译 mimalloc，并通过在终端中执行：

    ```shell
    jse -t 'println(jse.clib.MiMalloc.INCLUDE_DIR)'
    jse -t 'println(jse.clib.MiMalloc.LIB_PATH)'
    ```
    
    来获取编译后的 `MIMALLOC_INCLUDE_DIR` 以及 `MIMALLOC_LIB_PATH`。
    根据上述输出，在环境变量中设置 `JSE_MIMALLOC_INCLUDE_DIR` 和 `JSE_MIMALLOC_LIB_PATH`：

    ```bash
    export JSE_MIMALLOC_INCLUDE_DIR="dir/to/output/mimalloc/include/dir"
    export JSE_MIMALLOC_LIB_PATH="path/to/output/mimalloc/lib/path"
    ```
    
    从而可以让 cmake 编译 lammps 时，通过上述的 `$ENV{}` 来从环境变量中获取到路径。
    
7. 按照上述 [**离线情况下配置**](#离线情况下配置) 的方法编译 lammps，此时得到的
lammps 包含了 jse 中 `NativeLmp` 和 `MPI` 的 jni 部分。

8. 通过 `JSE_REDIRECT_XXX` 环境变量的设置，来直接指定 jse 不再自动编译相关 jni
库，而是直接使用输入的库：

    ```bash
    # of course, lammps home is necessary
    export JSE_LMP_HOME="path/to/lammps/build"
    # redirect stuffs
    export JSE_REDIRECT_MIMALLOC_LIB="$JSE_MIMALLOC_LIB_PATH"
    export JSE_REDIRECT_LMP_LIB="$JSE_LMP_HOME/lib/liblammps.so"
    export JSE_REDIRECT_LMPJNI_LIB="$JSE_REDIRECT_LMP_LIB"
    export JSE_REDIRECT_MPIJNI_LIB="$JSE_REDIRECT_LMP_LIB"
    ```
    
    由于这里将 `NativeLmp` 和 `MPI` 的 jni 库都合并到 lammps 的动态库 `liblammps.so`
    一起了，所以这里直接设置到同一个动态库即可。
    
