- [命令行参数](commandline.md)
    - [*None*](#none)
    - [-t/-text](#t-text)
    - [-f/-file](#f-file)
    - [-i/-invoke](#i-invoke)
    - [-v/-version](#v-version)
    - [-?/-help]()
- [**⟶ 目录**](contents.md)

# 命令行参数

用于在命令行中运行 jse，对于作为软件安装（将 jse 设置到了环境变量），
可以使用指令方法来运行：

```shell
jse -t "println('hello world')"
```

对于项目中独立安装，则可以在项目目录中运行：

```shell
./jse -t "println('hello world')"
```

后续介绍统一采用软件安装作为例子，对于独立安装将 `jse` 替换成 `./jse` 即可。

## *None*

当不增加任何输入参数时，则会启动一个
[Groovy Shell](https://docs.groovy-lang.org/latest/html/documentation/groovysh.html)，
用于在 shell 中执行 groovy 脚本（类似直接输入 `python`）。

- **执行**
  
  ```shell
  jse
  ```
  
- **输出**
  ```
  JSE Shell (2.6.0, Groovy: 4.0.16, JVM: 17.0.8.1)
  Type ':help' or ':h' for help.
  --------------------------------------------------
  groovy:000>
  ```

> 在 shell 模式中，会增加一些默认导入来方便使用，
> 在 groovy shell 中执行 `:show imports` 来查看这些导入。
> 


## -t/-text

增加 `-t` 或 `-text` 后，jse 会将后续输入的字符串当作 groovy 指令执行。
注意这里一般需要将整个指令使用双引号 `"` 包围，
保证整个指令被当作一个字符串传入。

- **执行**
  
  ```shell
  jse -t "println('hello world')"
  ```
  
- **输出**
  ```
  hello world
  ```

> 这里不会增加默认导入，因此如果需要使用 jse 的方法则需要使用完整包名或者手动导入，
> 如：
>
> ```shell
> jse -t "println(jse.code.CS.VERSION)"
> ```
>
> 或：
>
> ```shell
> jse -t "import static jse.code.CS.*; println(VERSION)"
> ```
>


## -f/-file

增加 `-f` 或 `-file` 后，jse 会将输入的参数作为 groovy 脚本文件的路径，
会尝试找到此脚本文件并执行。

此参数可以省略。

- **执行**
  
  ```shell
  jse -f script/groovy/example/helloWorld.groovy
  ```
  
  或：
  
  ```shell
  jse script/groovy/example/helloWorld.groovy
  ```
  
- **输出**
  ```
  hello world
  ```


> 可以省略 groovy 脚本的 `.groovy` 后缀，以及前面的 `script/groovy/`
> 目录（如果有的话），例如：
>
> ```shell
> jse example/helloWorld
> ```
>


## -i/-invoke

增加 `-f` 或 `-file` 后，jse 会将输入的参数作为一个 java 的静态函数
（或者静态成员的函数）直接调用，而后续参数则作为函数的参数传入。

此行为不通过 groovy 而是直接使用 java 的反射机制来实现，
避免了 groovy 的初始化可以让指令迅速执行，但是也需要使用完整的包名。

- **执行**
  
  ```shell
  jse -i java.lang.System.out.println "hello world"
  ```
  
- **输出**
  ```
  hello world
  ```

> 此行为偏向内部使用，因此目前仅支持调用 String 类型输入的方法。
> 

## -v/-version

增加 `-v` 或 `-version` 后 jse 会输出当前的 jse 版本以及
groovy 版本以及 java 版本并退出。

- **执行**
  
  ```shell
  jse -v
  ```
  
- **输出**
  ```
  jse version: 2.6.0 (groovy: 4.0.16, java: 17.0.8.1)
  ```


## -?/-help

增加 `-?`， `-help` 或任意非法的参数后，jse 会输出帮助信息。

- **执行**
  
  ```shell
  jse -?
  ```
  
- **输出**
  ```
  Usage:    jse [-option] value [args...]
  Such as:  jse path/to/script.groovy [argsOfGroovyScript...]
  Or:       jse -t "println('hello world')"
  
  The options can be:
      -t -text      Run the groovy text script
      -f -file      Run the groovy file script (default behavior when left blank)
      -i -invoke    Invoke the internal java static method directly
      -v -version   Print version number
      -? -help      Print help message
  
  You can also using another scripting language such as MATLAB or Python with Py4J and import jse-*.jar
  ```

