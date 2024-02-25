
## java 代码编译

实际使用了更加成熟的 [Gradle](https://gradle.org/) 来管理项目，
可以方便的控制程序构建的细节，添加第三方依赖，并将第三方依赖一并打包到 
`jse-all.jar` 文件中。

实际构建时**不需要手动安装 Gradle**，直接在根目录运行 `./gradlew build` 即可进行编译，
默认会将编译得到的 `jse-all.jar` 文件输出到 `release/lib` 文件夹，
并将用于开发的源码文件 `jse-src.jar` 输出到 `release/src` 文件夹。

> 第一次运行构建会自动下载 Gradle 以及依赖的第三方库，因此构建时间会比较长。

可以使用 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 来管理源码，
使用 idea 直接打开项目文件夹，会自动识别此 Gradle 项目。
第一次打开可能需要设置 jdk 的路径（已经为 IntelliJ 安装了汉化插件）：

```
左上角“文件” ⟶ 项目结构 ⟶ 左边栏选择“项目设置-项目” ⟶ SDK 选择本地安装的 JDK
⟶ 语言级别：8 - lambda、类型注解等 ⟶ 右下角“确定”
```

然后右上角通过 `编辑配置` 来增加一个 Gradle 的运行/调试配置，
设置运行 build 任务来进行构建。


## Getter/Setter

jse 中约定，对于获取属性的方法，直接省略开头的 `get` 而是直接使用属性名称定义函数；
对于设置属性的方法，保留开头的 `set`，并可以返回自身从而支持链式调用。

- **使用：**
    ```java
    public class Person {
        private String mName;
        
        public String name() {
            return mName;
        }
        public Person setName(String aName) {
            mName = aName;
            return this;
        }
    }
    ```
  
- **不要使用：**
    ```java
    public class Person {
        private String name;
        
        public String getName() {
            return this.name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
    ```


