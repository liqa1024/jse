- [基本使用方式](usage.md)
    - [搭建 java 环境](#搭建-java-环境)
    - [通过 groovy 使用](#通过-groovy-使用)
    - [通过 python 使用](#通过-python-使用)
    - [通过 matlab 使用](#通过-matlab-使用)
    - [通过 jupyter 使用](#通过-jupyter-使用)
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


## 通过 groovy 使用

为了能方便的使用 jse 中的功能，这里使用 [groovy 脚本](http://www.groovy-lang.org/) 
来原生调用 java 程序中的功能（而不是常见的 gui 形式）。

> 关于 groovy 脚本的语法可以参考 [这篇文章](https://zhuanlan.zhihu.com/p/257969931)，
> 以及 [使用风格](https://www.groovy-lang.org/style-guide.html)，
> 它在兼容 java 语法的同时，提供了一种更加简洁的语法。
> 

由于 groovy 本身也是基于 java 实现的，因此 jse 可以很方便的将整个 groovy
解释器都包含到打包后的 `jse-all.jar` 文件中，因此这里**不需要额外安装 groovy。**

由于这里将整个 groovy 解释器包含到了 jse 当中，因此可以直接在 groovy 脚本
中导入 jse 的类，类似这样：

```groovy
import jse.lmp.Data
import jse.lmp.Dump
```

导入了 jse 中的 `jse.lmp.Data` 以及 `jse.lmp.Dump` 类，
从而可以在脚本中使用这两个类来读写 lammps 的 data 文件和 dump 文件。


### a. 将 jse 作为软件使用

-   从 [**Release**](https://github.com/CHanzyLazer/jse/releases/latest) 
    中下载 `jse-${tag}.zip` 文件，解压到希望的目录作为此软件的位置。

-   将解压到的目录添加到 `PATH` 环境变量中。
  
-   在书写好 groovy 脚本后，直接在终端执行 `jse path/to/script`
    即可运行此脚本。

> 在终端中执行 `jse -v` 后输出版本信息表示安装成功。
> 

### b. 将 jse 作为独立项目使用

-   从 [**Release**](https://github.com/CHanzyLazer/jse/releases/latest) 
    中下载 `jse-full-${tag}.zip` 文件，解压到希望的目录作为项目目录。

- **a. 使用 VScode 管理项目：** 
    
    使用 vscode 打开解压后的文件夹，
    切换到希望运行的 groovy 脚本后，通过左侧栏的 `运行和调试` 选择 
    `jse-RunCurrentScript` 选项后运行即可。

- **b. 使用 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 管理项目：** 
    
    使用 idea 打开解压后的文件夹，第一次打开可能需要设置 jdk 的路径：
    
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
    
    切换到希望运行的 groovy 脚本后，通过右上角的 `选择运行/调试配置` 切换到
    `jse-RunCurrentScript`，然后运行即可。

> 在 idea 中支持语法检查，代码补全提示，以及直接查看源码定义。
> 
> 具体项目文件的结构以及解释参看 [项目文件结构](filestructure.md)。
>
> 对于独立项目，也可以直接使用 `./jse path/to/script` 来运行脚本。
> 


## 通过 python 使用

在 python 中，可以使用第三方库来使用调用 java 代码
（例如 [py4j](https://www.py4j.org/) ）；
在 jse 中也可以通过内部的 [jep](https://github.com/ninia/jep)
库来运行 python 脚本。


### a. 通过 py4j 使用 jse：

通过在 python 中使用 py4j 库直接导入 `jse-all.jar`
文件的方式来使用 jse 提供的方法。

-   确保已经安装了 [py4j](https://www.py4j.org/)

-   从 [**Release**](https://github.com/CHanzyLazer/jse/releases/latest) 
    中下载 `jse-${tag}.zip` 文件，解压到需要使用 jse 的 python 项目目录中，
    最终结构大致如下：
    
    ```
    └─pyproject
        ├─jse
        ├─jse.bat
        ├─lib
        │   └─jse-all.jar
        ├─main.py
        ...
    ```
    
    > 对于使用 py4j 来调用 jse 的不需要 `jse` 和 `jse.bat` 两个运行脚本，
    > 这里只是保留备用。
    > 
    
-   对于需要使用 jse 的 python 脚本（例如 `main.py`），
    通过类似下面的代码来导入 `jse-all.jar` 文件：
    
    ```python
    from py4j.java_gateway import JavaGateway
    GATEWAY = JavaGateway.launch_gateway(classpath='lib/jse-all.jar')
    ```
    
-   而后通过这种方式来导入 jse 中定义的类：
    
    ```python
    Data = GATEWAY.jvm.jse.lmp.Data
    Dump = GATEWAY.jvm.jse.lmp.Dump
    ```
    
-   最后记得关闭 `GATEWAY`：
    
    ```python
    GATEWAY.shutdown()
    ```

> **注意**：默认情况下 py4j 不会输出 java 的信息到控制台，
> 可以在创建 `GATEWAY` 时重新定向输出流来解决这个问题：
> 
> ```python
> import sys
> from py4j.java_gateway import JavaGateway
> GATEWAY = JavaGateway.launch_gateway(classpath='lib/jse-all.jar', redirect_stdout=sys.stdout)
> ```
> 


### b. 通过 jep 使用 jse：

也可以通过 jse 内部的 jep 库来运行 python 脚本，
其中 jep 提供了 python 中调用 java 类的支持。

这种方法不再需要依赖第三方库，并且由于 jse 已经包含了 jep，
因此也**不需要手动安装 jep**。

-   参考 [通过 groovy 使用](#通过-groovy-使用) 中的任意做法，
    只是将其中需要运行的 groovy 脚本改为 python 脚本；
    并将脚本置于 `script/python` 目录下（非必须）。
  
-   对于需要使用 jse 的 python 脚本（例如 `script/python/main.py`），
    可以直接通过 `import` 的方式导入 java 以及 jse 中的类：
    
    ```python
    from jse.lmp import Data, Dump
    ```
    
-   通过 `jse` / `./jse` 来运行此脚本而不是 `python`，从而让导入有效：
    
    ```shell
    jse script/python/main.py
    ```
    
    当然也可以通过 vscode 或者 idea 中已经配置好的
    `jse-RunCurrentScript` 来运行。


## 通过 matlab 使用

matlab 原生支持调用 java 程序，因此可以比较简单的使用 jse。

-   从 [**Release**](https://github.com/CHanzyLazer/jse/releases/latest) 
    中下载 `jse-${tag}.zip` 文件，解压到需要使用 jse 的 matlab 项目目录中，
    最终结构大致如下：
    
    ```
    └─matproject
        ├─jse
        ├─jse.bat
        ├─lib
        │   └─jse-all.jar
        ├─main.m
        ...
    ```
    
    > 对于 matlab 来调用 jse 的不需要 `jse` 和 `jse.bat` 两个运行脚本，
    > 这里只是保留备用。
    > 
    
-   对于需要使用 jse 的 matlab 脚本（例如 `main.m`），
    通过类似下面的代码来导入 `jse-all.jar` 文件：
    
    ```matlab
    javaaddpath('lib/jse-all.jar');
    ```
    
-   然后类似导入其他 java 包一样，这样导入 jse 中的 lmp 包：
    
    ```matlab
    import jse.lmp.*
    ```
    
    > **注意**：matlab 似乎不能直接使用 `import jse.lmp.Data` 这种写法来导入单个类
    
-   最后记得移除 java 路径：
    
    ```matlab
    clear;
    javarmpath('lib/jse-all.jar');
    ```

> **注意**：不能重复导入同一个 java 路径，否则会出现报错（虽然目前看起来不影响使用），
> 因此实际项目中一般会将此方法进行包装，检测避免重复导入。
> 在 `jse-full-${tag}.zip` 中提供了两个包装函数，包装后的函数为：
> 
> ```matlab
> addjpath('lib/jse-all.jar');
> rmjpath('lib/jse-all.jar');
> ```
> 
> 两个函数都位于目录 `script/matlab/include`
> 


## 通过 jupyter 使用

jse 从 `2.7.0` 开始支持在 jupyter 中直接使用（不需要通过 py4j）。

-   参考 [通过 groovy 使用](#通过-groovy-使用) 中的任意做法安装 jse。
    
-   通过 `jse` / `./jse` 来执行命令安装当前 jse 到 jupyter 内核中：
    
    ```shell
    jse -jupyter
    ```
    
    输出
    
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

