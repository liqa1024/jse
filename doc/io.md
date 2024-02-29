- [通用文件操作](io.md)
    - [文件读写](#文件读写)
    - [文件操作](#文件操作)
    - [数据结构文本文件读写](#数据结构文本文件读写)
        - [json 文件读写](#json-文件读写)
        - [yaml 文件读写](#yaml-文件读写)
        - [csv 文件读写](#csv-文件读写)
- [**⟶ 目录**](contents.md)

# 通用文件操作

尽管 [java 的 `java.nio.file.Files`](https://www.w3schools.com/java/java_files.asp)
已经提供了用于操作文件和目录的方法，并且
[groovy 的 IO 接口](https://www.groovy-lang.org/groovy-dev-kit.html#_working_with_io)
也提供了更加丰富的功能，在 jse 中还是封装了一套自己的 IO 接口，
位于 [`jse.code.UT.IO`](../src/main/java/jse/code/UT.java)。
相比于这些现成的方法，主要的区别有：

- 直接基于 `String` 表示的路径进行操作，而不需要转为 `java.nio.file.Path`。
  
- 在创建文件或目录时，对应目录不存在时会自动创建，并且会自动递归创建子目录。
  
- 文本文件编码格式统一为 [UTF-8](https://en.wikipedia.org/wiki/UTF-8)，
  换行符统一为 `\n`（即 `LF`），因此不用考虑文件格式的问题。

> `jse.code.UT.IO` 中的静态方法在 shell 模式下是默认导入的，
> 因此在 shell 模式下可以直接执行 `readAllLines()` 之类的方法而不需要导入。
> 


## 文件读写

- **`UT.IO.readAllLines`**
    
    描述：读取输入路径对应文件中的所有行，作为一个 `List<String>` 返回
    
    输入：`String`，字符串表示的文件路径
    
    输出：`List<String>`，字符串组成的列表，每个元素为文件中的一行
    
    例子：`example/io/readwrite1`
    [⤤](../release/script/groovy/example/io/readwrite1.groovy)
    
    > 注意：文件不存在或者触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.readAllText`**
    
    描述：读取输入路径对应文件中的所有文本，作为一个 `String` 返回
    
    输入：`String`，字符串表示的文件路径
    
    输出：`String`，文件中所有字符串文本
    
    例子：`example/io/readwrite1`
    [⤤](../release/script/groovy/example/io/readwrite1.groovy)
    
    > 注意：文件不存在或者触发权限不够时会抛出异常
    > 
    
    -----------------------------
  
- **`UT.IO.readAllBytes`**
    
    描述：读取输入路径对应二进制文件，作为一个 `byte[]` 返回
    
    输入：`String`，字符串表示的二进制文件路径
    
    输出：`byte[]`，二进制文件的数据
    
    例子：`example/io/readwrite2`
    [⤤](../release/script/groovy/example/io/readwrite2.groovy)
    
    > 注意：文件不存在或者触发权限不够时会抛出异常
    > 
    
    -----------------------------
  
- **`UT.IO.write`**
    
    描述：将输入的字符串或数据写入到输入的路径中
    
    输入1：`String`，字符串表示的文件路径
    
    输入2：根据输入类型重载，具体为：
    
    - `String[]`/`Iterable<? extends CharSequence>`，按行分隔的字符串
    - `String`，单个字符串文本
    - `byte[]`，二进制数据
    
    输入3（可选）：`OpenOption...`，设置写入的模式
    
    例子：`example/io/readwrite1`
    [⤤](../release/script/groovy/example/io/readwrite1.groovy)，
    `example/io/readwrite2`
    [⤤](../release/script/groovy/example/io/readwrite2.groovy)
    
    > 注意：默认情况下会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 给定的路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 
    > 当输入为 `String` 是会将其看作单行的文文，实际最后会添加一个换行符
    > `\n`，如果希望和 `UT.IO.readAllText` 的结果完全一致，可以使用
    > `UT.IO.writeText` 方法
    >
    
    -----------------------------
    
- **`UT.IO.writeText`**
    
    描述：将输入的字符串文本写入到输入的路径中
    
    输入1：`String`，字符串表示的文件路径
    
    输入2：`String`，字符串文本
    
    输入3（可选）：`OpenOption...`，设置写入的模式
    
    > 注意：默认情况下会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 给定的路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 
    > 不会在最后添加换行符，保证和 `UT.IO.readAllText` 的结果完全一致
    >


## 文件操作

- **`UT.IO.mkdir`/`UT.IO.makeDir`**
    
    描述：创建目录
    
    输入：`String`，字符串表示的目录路径
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：会递归创建子目录，触发权限不够时会抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.rmdir`/`UT.IO.removeDir`**
    
    描述：移除目录
    
    输入：`String`，字符串表示的目录路径
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：会递归删除嵌套的目录，触发权限不够时会抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.isdir`/`UT.IO.isDir`**
    
    描述：判断输入路径是否是目录
    
    输入：`String`，字符串表示的路径
    
    输出：`boolean`，`true` 表示是目录，`false` 表示其他情况
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：触发权限不够导致不能确定的情况也会返回 `false` 而不是抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.isfile`/`UT.IO.isFile`**
    
    描述：判断输入路径是否是文件
    
    输入：`String`，字符串表示的路径
    
    输出：`boolean`，`true` 表示是文件，`false` 表示其他情况
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：触发权限不够导致不能确定的情况也会返回 `false` 而不是抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.exists`**
    
    描述：判断输入路径是否存在
    
    输入：`String`，字符串表示的路径
    
    输出：`boolean`，`true` 表示存在，`false` 表示其他情况
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：触发权限不够导致不能确定的情况也会返回 `false` 而不是抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.delete`**
    
    描述：如果给定的文件存在则移除给定的文件
    
    输入：`String`，字符串表示的文件路径
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：不能移除有内容的文件夹（使用 `rmdir`），
    > 触发权限不够时会抛出异常，路径不存在则什么都不会做
    >
    
    -----------------------------
    
- **`UT.IO.copy`**
    
    描述：复制指定文件到另一个位置
    
    输入1：`String`，字符串表示的源文件路径
    
    输入2：`String`，字符串表示的目标文件路径
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：不支持复制有内容的文件夹，
    > 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 目标路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.move`**
    
    描述：移动指定文件或目录到另一个位置
    
    输入1：`String`，字符串表示的源文件或目录路径
    
    输入2：`String`，字符串表示的目标文件或目录路径
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：一般情况下也支持直接移动有内容的文件夹，
    > 会覆盖已有文件，如果目录不存在会递归创建，
    > 目标路径有同名的非空目录时会抛出异常；触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.list`**
    
    描述：列出输入目录下的所有文件或目录的名称
    
    输入：`String`，字符串表示的目录路径
    
    输出：`String[]`，目录下的文件或目录名称组成的字符串数组，仅包含名称
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：触发权限不够时会抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.map`**
    
    描述：通过输入的行映射器，映射指定文件到另一个位置；
    主要用于将给定文本文件特定字符串批量替换。
    
    输入1：`String`，字符串表示的源文件路径
    
    输入2：`String`，字符串表示的目标文件路径
    
    输入3: `IUnaryFullOperator<? extends CharSequence, ? super String>`
    (`{String -> String}`)，一行源文件的字符串到一行目标文件字符串的映射
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    > 注意：会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 目标路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.samePath`**
    
    描述：判断输入的两个路径是否相同。
    
    输入1：`String`，字符串表示的路径1
    
    输入2：`String`，字符串表示的路径2
    
    输出：`boolean`，`true` 表示相同，`false` 表示不相同
    
    例子：`example/io/fileopt`
    [⤤](../release/script/groovy/example/io/fileopt.groovy)
    
    -----------------------------
    
- **`UT.IO.validPath`**
    
    描述：合法化输入的路径（创建需要的文件夹）。
    
    输入：`String`，字符串表示的路径
    
    例子：`UT.IO.validPath('path/to/output/file')`
    
    > 注意：一般用于在调用第三方工具时，对方输出文件在目录不存在时会报错时使用，
    > jse 内部的文件操作由于会自动创建文件夹，因此不需要手动调用；
    > 触发权限不够时会抛出异常
    >
    
    -----------------------------
    
- **`UT.IO.toAbsolutePath`**
    
    描述：将输入的路径转为绝对路径。
    
    输入：`String`，字符串表示的路径
    
    输出：`String`，字符串表示的绝对路径
    
    例子：`def absPath = UT.IO.toAbsolutePath('path/to/something')`
    
    > 注意：尽量使用 `UT.IO.toAbsolutePath` 而不是 java 自带的
    > `Path.toAbsolutePath` 之类的方法，jse 考虑了在 matlab
    > 之类的环境下工作目录被修改的环境，使用 `UT.IO.toAbsolutePath`
    > 可以确保获取到正确的绝对路径。
    >


## 数据结构文本文件读写

### json 文件读写

jse 直接基于 [`groovy-json`](https://www.groovy-lang.org/processing-json.html)
实现 json 文件的读写。

- **`UT.IO.json2map`**
    
    描述：读取输入 json 文件路径并存储为 java 中的 `Map` 对象。
    
    输入：`String`，字符串表示的 json 文件路径
    
    输出：`Map<?, ?>`，解析 json 文件得到的 `Map`
    
    例子：`example/io/datajsonyaml`
    [⤤](../release/script/groovy/example/io/datajsonyaml.groovy)
    
    > 注意：文件不存在或触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.map2json`**
    
    描述：通过输入的 `Map` 对象创建 json 文件。
    
    输入1：`Map<?, ?>`，存储 json 信息的 `Map`
    
    输入2：`String`，字符串表示的输出 json 文件的路径
    
    例子：`example/io/datajsonyaml`
    [⤤](../release/script/groovy/example/io/datajsonyaml.groovy)
    
    > 注意：会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 目标路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 

### yaml 文件读写

jse 直接基于 [`groovy-yaml`](https://www.groovy-lang.org/processing-yaml.html)
实现 yaml 文件的读写。

- **`UT.IO.yaml2map`**
    
    描述：读取输入 yaml 文件路径并存储为 java 中的 `Map` 对象。
    
    输入：`String`，字符串表示的 yaml 文件路径
    
    输出：`Map<?, ?>`，解析 yaml 文件得到的 `Map`
    
    例子：`example/io/datajsonyaml`
    [⤤](../release/script/groovy/example/io/datajsonyaml.groovy)
    
    > 注意：文件不存在或触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.map2yaml`**
    
    描述：通过输入的 `Map` 对象创建 yaml 文件。
    
    输入1：`Map<?, ?>`，存储 yaml 信息的 `Map`
    
    输入2：`String`，字符串表示的输出 yaml 文件的路径
    
    例子：`example/io/datajsonyaml`
    [⤤](../release/script/groovy/example/io/datajsonyaml.groovy)
    
    > 注意：会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 目标路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 

### csv 文件读写

jse 对于内部的纯数字的矩阵和表格做了简单 csv 读写支持，
而更一般的情况则使用
[Apache Commons CSV](https://commons.apache.org/proper/commons-csv/)
实现。

- **`UT.IO.csv2data`**
    
    描述：读取输入 csv 文件路径并存储为 jse 矩阵 `IMatrix`。
    
    输入：`String`，字符串表示的 csv 文件路径
    
    输出：`IMatrix`，解析 csv 文件得到的矩阵数据
    
    例子：`example/io/datacsv`
    [⤤](../release/script/groovy/example/io/datacsv.groovy)
    
    > 注意：只接受用 `,` 分隔的纯数字 csv 文件，
    > 这个文件的第一行可以是由字符串组成的头（不支持引号 `'` 包围的字符串），
    > 如果检测到第一行是头则会自动跳过这一行的读取；
    > 文件不存在或触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.data2csv`**
    
    描述：通过输入的数据创建 csv 文件。
    
    输入1：根据输入类型重载，具体为：
    
    - `double[][]`，按行分隔的浮点数组数据
    - `double[]`，单列浮点数组数据
    - `Iterable<?>`，按行分隔的数据或者单列数据，
      `?` 可以是 `double[]`， `Iterable`， `IVecetor` 或任意单个数据 `Object`
    - `IMatrix`，jse 的矩阵数据
    - `IVecetor`，单列的 jse 向量数据
    - `IFunc1`，jse 的单元函数数据，第一列为函数值第二列为变量值，
      默认第一列会添加头 `f`，第二列会添加头 `x`
    
    输入2：`String`，字符串表示的输出 csv 文件的路径
    
    输入3（可选）：`String...`，自定义输出 csv 文件的头，
    如果设置要求数目和数据列数相同
    
    例子：`example/io/datacsv`
    [⤤](../release/script/groovy/example/io/datacsv.groovy)
    
    > 注意：会使用 `,` 分隔数字，第一行可能是数据的头
    > （保证 `csv2data`/`csv2table` 能直接读取）；
    > 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 目标路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.csv2table`**
    
    描述：读取输入 csv 文件路径并存储为 jse 表格 `ITable`。
    
    输入：`String`，字符串表示的 csv 文件路径
    
    输出：`ITable`，解析 csv 文件得到的表格数据
    
    例子：`example/io/datacsv`
    [⤤](../release/script/groovy/example/io/datacsv.groovy)
    
    > 注意：只接受用 `,` 分隔的纯数字 csv 文件，
    > 这个文件的第一行可以是由字符串组成的头（不支持引号 `'` 包围的字符串），
    > 如果检测到第一行是头则会将其设为表格 `ITable` 的头，
    > 否则会自动生成头（`C0`, `C1`, `C2`, ...）；
    > 文件不存在或触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.table2csv`**
    
    描述：通过输入的 jse 表格 `ITable` 创建 csv 文件。
    
    输入1：`ITable`，jse 表格数据
    
    输入2：`String`，字符串表示的输出 csv 文件的路径
    
    例子：`example/io/datacsv`
    [⤤](../release/script/groovy/example/io/datacsv.groovy)
    
    > 注意：会使用 `,` 分隔数字，第一行为表格数据的头
    > （保证 `csv2data`/`csv2table` 能直接读取）；
    > 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 目标路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.csv2str`**
    
    描述：读取输入 csv 文件路径并按行分隔存储为 `List<String[]>`。
    
    输入1：`String`，字符串表示的 csv 文件路径
    
    输入2（可选）：`CSVFormat`，手动指定 csv 文件的格式，具体可以参考 
    [Apache Commons CSV 文档](https://commons.apache.org/proper/commons-csv/user-guide.html#Parsing_files)
    
    例子：`example/io/datacsv`
    [⤤](../release/script/groovy/example/io/datacsv.groovy)
    
    > 注意：此方法基于 
    > [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/)
    > 实现，旨在提供更加一般的 csv 文件读写；
    > 默认情况下使用 `,` 分隔；
    > 文件不存在或触发权限不够时会抛出异常
    > 
    
    -----------------------------
    
- **`UT.IO.str2csv`**
    
    描述：将输入的按行排列的字符串写入 csv 文件。
    
    输入1：`Iterable<?>`，按行排列的字符串，
    `?` 可以是 `Iterable`， `Object[]` 或任意单个数据 `Object`
    
    输入2：`String`，字符串表示的输出 csv 文件的路径
    
    输入3（可选）：`CSVFormat`，手动指定 csv 文件的格式，具体可以参考 
    [Apache Commons CSV 文档](https://commons.apache.org/proper/commons-csv/user-guide.html#Parsing_files)
    
    例子：`example/io/datacsv`
    [⤤](../release/script/groovy/example/io/datacsv.groovy)
    
    > 注意：此方法基于 
    > [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/)
    > 实现，旨在提供更加一般的 csv 文件读写；
    > 默认情况下使用 `,` 分隔，并采用 `\n` 作为换行符（`LF`）；
    > 会覆盖已有文件，如果文件不存在会创建，如果目录不存在会递归创建，
    > 目标路径有同名的目录时会抛出异常；触发权限不够时会抛出异常
    > 

