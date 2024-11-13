- [基本使用方式](usage.md)
    - [搭建 java 环境](#搭建-java-环境)
    - [安装 jse](#安装-jse)
    - [在 idea 中使用](#在-idea-中使用)
    - [在 jupyter 中使用](#在-jupyter-中使用)
    - [通过 groovy 使用](#通过-groovy-使用)
    - [通过 python 使用](#通过-python-使用)
    - [通过 matlab 使用](#通过-matlab-使用)
- [**⟶ 目录**](contents.md)

# 基本使用方式

## 搭建 java 环境

jse 基本功能需要且只需要 java 环境，这里要求至少拥有 jdk8，
建议直接使用 jdk17（新版本会有更多优化，程序运行效率会更高），
可以从 [**这里**](https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/) 
下载 Adoptium 版本的 jdk，
或者从 [**这里**](https://www.oracle.com/java/technologies/downloads/#java17)
下载 Oracle 版本的 jdk。

**Windows**下，安装中注意勾选添加到 `PATH` 环境变量中，
**Linux** 下直接解压到期望的位置后，类似下面手动添加环境变量：

```bash
export JAVA_HOME="$HOME/java/jdk-17.0.10" # replace to your jdk path
export PATH="$JAVA_HOME/bin:$PATH"
```

> 在终端中执行 `java` 后输出各种用法而不是报错表示环境搭建完成。
>


## 安装 jse

目前 jse 采用类似 groovy 的安装方式：

-   从 [**Release**](https://github.com/CHanzyLazer/jse/releases/latest) 
    中下载 `jse-${tag}.zip` 文件，解压到希望的目录作为此软件的位置。

-   将解压到的目录添加到 `PATH` 环境变量中。

> 在终端中执行 `jse -v` 后输出版本信息表示安装成功。
> 

详细命令说明可以查看 [命令行参数](commandline.md)。


## 在 idea 中使用

为了有正确的代码高亮，补全提示以及查看源码定义，这里建议使用 
[IntelliJ IDEA](https://www.jetbrains.com/idea/) 来管理项目。
在项目目录通过终端执行：

```shell
jse -idea
```

即可初始化此目录作为一个 idea 的项目文件夹，之后可通过 idea 打开此文件夹。
第一次打开需要设置 jdk 的路径：
    
```
左上角“文件” ⟶ 项目结构 ⟶ 左边栏选择“项目设置-项目” ⟶ SDK 选择本地安装的 JDK
⟶ 语言级别：8 - lambda、类型注解等 ⟶ 右下角“确定”
```

切换到希望运行的 groovy 脚本后，通过右上角的 `选择运行/调试配置` 切换到
`jse-RunCurrentScript`，然后运行即可。

> 目前只有 groovy 脚本支持正确的代码高亮。
> 


## 在 jupyter 中使用

为了实现代码分段执行，jse 支持在 jupyter 中直接使用。
    
-   执行命令：
    
    ```shell
    jse -jupyter
    ```
    
    安装当前 jse 到 jupyter 内核中，输出：
    
    ```
    The jupyter kernel for JSE has been initialized,
    now you can open the jupyter notebook through `jupyter notebook`
    ```
    
    表示安装成功，如果遇到权限问题，可以尝试 *管理员权限运行*，或者通过：
    
    ```shell
    jse -jupyter user=True
    ```
    
    来仅为当前用户安装。

-   通过 `jupyter notebook` 或者 `jupyter lab` 运行 jupyter，
    这时应该能够看到名为 `jse` 的内核。

详细使用细节可以参看 [jupyter 支持](jupyter.ipynb)。


## 通过 groovy 使用

为了能方便的使用 jse 中的功能，这里使用 [groovy 脚本](http://www.groovy-lang.org/) 
来原生调用 java 程序中的功能。

> 关于 groovy 脚本的语法可以参考 [这篇文章](https://zhuanlan.zhihu.com/p/257969931)，
> 以及 [使用风格](https://www.groovy-lang.org/style-guide.html)，
> 它在兼容 java 语法的同时，提供了一种更加简洁的语法。
> 

由于 groovy 本身也是基于 java 实现的，因此 jse 可以很方便的将整个 groovy
解释器都包含到打包后的 `jse-all.jar` 文件中，因此这里**不需要额外安装 groovy。**
在书写好 groovy 脚本后，直接在终端执行：

```shell
jse path/to/script
```

即可运行此脚本。


## 通过 python 使用

在 python 中，可以通过内部的 [jep](https://github.com/ninia/jep)
库来运行 python 脚本，其中 jep 提供了 python 中调用 java 类的支持。

这种方法不再需要依赖第三方库，并且由于 jse 已经包含了 jep，
因此也**不需要手动安装 jep**。

-   对于需要使用 jse 的 python 脚本（例如 `main.py`），
    可以直接通过 `import` 的方式导入 java 以及 jse 中的类：
    
    ```python
    from jse.lmp import Data, Dump
    ```
    
-   通过 `jse` 来运行此脚本而不是 `python`，从而让导入有效：
    
    ```shell
    jse main.py
    ```
    
    在 idea 中也可以通过自动配置好的 `jse-RunCurrentScript` 来运行。


## 通过 matlab 使用

matlab 原生支持调用 java 程序，因此可以比较简单的使用 jse。

-   获取到 jse 的核心 jar（`jse-all.jar`）路径，这里可以通过执行：
    
    ```shell
    jse -t 'println(jse.code.OS.JAR_PATH)'
    ```
    
    来输出此 jse 的 jar 路径。
    
-   对于需要使用 jse 的 matlab 脚本（例如 `main.m`），
    通过类似下面的代码来导入 `jse-all.jar` 文件：
    
    ```matlab
    javaaddpath('path/to/jse-all.jar');
    ```
    
-   然后类似导入其他 java 包一样，这样导入 jse 中的 lmp 包：
    
    ```matlab
    import jse.lmp.*
    ```
    
    > **注意**：matlab 似乎不能直接使用 `import jse.lmp.Data` 这种写法来导入单个类
    
-   最后记得移除 java 路径：
    
    ```matlab
    clear;
    javarmpath('path/to/jse-all.jar');
    ```

> **注意**：不能重复导入同一个 java 路径，否则会出现报错（虽然目前看起来不影响使用），
> 因此实际项目中一般会将此方法进行包装，检测避免重复导入。
> 在 `jse-example-${tag}.zip` 的 `code/include`
> 目录中提供了两个包装函数，包装后的函数为：
> 
> ```matlab
> addjpath('path/to/jse-all.jar');
> rmjpath('path/to/jse-all.jar');
> ```
> 
