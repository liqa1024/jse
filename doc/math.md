- [简单的数学库](math.md)
    - [基本使用方法](#基本使用方法)
    - [运算符重载](#运算符重载)
    - [其他向量运算](#其他向量运算)
- [**⟶ 目录**](contents.md)

# 简单的数学库

jse 在 [`jse.code.UT.Math`](../src/main/java/jse/code/UT.java)
中提供了一套类似 numpy 简单数学库，用于方便的进行向量化运算。

对于各种向量和矩阵结构，jse 提供了：

- [`jse.math.vector.IVector`](../src/main/java/jse/math/vector/IVector.java)
存储浮点数 `double` 的向量类

- [`jse.math.matrix.IMatrix`](../src/main/java/jse/math/matrix/IMatrix.java)
存储浮点数 `double` 的矩阵类

- [`jse.math.vector.IComplexVector`](../src/main/java/jse/math/vector/IComplexVector.java)
存储复数 `ComplexDouble` 的向量类

- [`jse.math.matrix.IComplexMatrix`](../src/main/java/jse/math/matrix/IComplexMatrix.java)
存储复数 `ComplexDouble` 的矩阵类

- [`jse.math.vector.IIntVector`](../src/main/java/jse/math/vector/IIntVector.java)
存储整数 `int` 的向量类

- [`jse.math.matrix.IIntMatrix`](../src/main/java/jse/math/matrix/IIntMatrix.java)
存储整数 `int` 的矩阵类

- [`jse.math.vector.ILogicalVector`](../src/main/java/jse/math/vector/ILogicalVector.java)
存储逻辑值 `boolean` 的向量类


## 基本使用方法

- **创建和运算**
    
    一般的向量创建和运算可以直接使用 `jse.code.UT.Math`
    中的方法以及重载运算符实现，基本使用方法和 numpy 或 matlab 类似：
    
    ```groovy
    import static jse.code.UT.Math.*
    
    def x = linspace(-1.0, 1.0, 10) // #1.
    def y1 = sin(x * pi) // #2. #3.
    def y2 = x * x // #2.
    
    // x = [-1.000, -0.7778, -0.5556, -0.3333, -0.1111, 0.1111, 0.3333, 0.5556, 0.7778, 1.000]
    // y1 = [-1.225e-16, -0.6428, -0.9848, -0.8660, -0.3420, 0.3420, 0.8660, 0.9848, 0.6428, 5.666e-16]
    // y2 = [1.000, 0.6049, 0.3086, 0.1111, 0.01235, 0.01235, 0.1111, 0.3086, 0.6049, 1.000]
    ```
    
    > 脚本位置：`jse example/math/basic`
    > [⤤](../release/script/groovy/example/math/basic.groovy)
    > 
    > 1. 其中 `linspace` 方法会创建一个线性间隔点的向量 `IVector`
    > 
    > 2. 向量类 `IVector` 重载了常见的运算符，因此可以直接运算（统一为标量运算）
    > 
    > 3. `UT.Math` 还包含许多常用常量，如 `pi`, `e`, `nan`, `inf`
    >
    
- **索引**
    
    向量类 `IVector` 在 groovy 中可以像一般的 `List`
    一样访问和修改，但长度是固定的因此不能直接扩容：
    
    ```groovy
    import static jse.code.UT.Math.*
    
    def vec = zeros(5)
    def list = [0.0] * 5
    // vec = [0.000, 0.000, 0.000, 0.000, 0.000]
    // list = [0.000, 0.000, 0.000, 0.000, 0.000]
    
    vec[1] = 10.0 // #1.
    list[1] = 10.0 // #1.
    // vec = [0.000, 10.00, 0.000, 0.000, 0.000]
    // list = [0.000, 10.00, 0.000, 0.000, 0.000]
    
    vec[2..<5] = 20.0 // #2.
    list[2..<5] = 20.0 // #2.
    // vec = [0.000, 10.00, 20.00, 20.00, 20.00]
    // list = [0.000, 10.00, 20.00]
    
    vec += 3.0 // #3.
    list += 3.0 // #3.
    // vec = [3.000, 13.00, 23.00, 23.00, 23.00]
    // list = [0.000, 10.00, 20.00, 3.000]
    ```
    
    > 脚本位置：`jse example/math/getset`
    > [⤤](../release/script/groovy/example/math/getset.groovy)
    > 
    > 1. 当索引只有一个元素时，`IVector` 和 `List` 的行为一致，都是修改此位置的值
    > 
    > 2. 当索引为一个 `Range` 的时候，`IVector` 会直接修改整个区域的值，而 `List`
    > 会将整个区域设置为输入值
    > 
    > 3. 加法运算 `+` 对于 `IVector` 会直接进行数值运算，而 `List` 会像 python
    > 中那样将值添加到最后
    >
    > **注意**：和 python 或 c++ 不同，groovy 并没有提供专门的 `+=`
    > 运算符重载，因此这里的 `vec += 3.0` 会严格等价于 `vec = vec + 3.0`，
    > 也就是说，会先执行 `vec + 3.0`，创建一个新的临时向量存储结果，
    > 然后将结果赋值给 `vec`，对于 `list` 也是如此。
    > 这往往是非常低效的，因此如果希望向 `list` 中添加元素，需要使用
    > `list.add(3.0)` 或者 `list << 3.0`；
    > 如果希望将值数值计算增加到 `vec`，需要使用 `vec.plus2this(3.0)`。
    >
    
- **遍历**
    
    向量类 `IVector` 不支持直接使用增强 for 循环遍历，
    这里提供几种遍历的方法：
    
    ```groovy
    import static jse.code.UT.Math.*
    
    def vec = linsequence(0.0, 1.0, 3)
    
    // 索引遍历
    for (i in 0..<vec.size()) {
        println(vec[i])
    }
    // 转为 Iterable 遍历
    for (v in vec.iterable()) {
        println(v)
    }
    // 转为 List 遍历
    for (v in vec.asList()) {
        println(v)
    }
    // for-each 遍历
    vec.forEach {v ->
        println(v)
    }
    ```
    
    > 上述所有方法理论上效率几乎一致（特别是在增加 `@CompileStatic`
    > 注解后），并且都不会有冗余的值拷贝
    >


## 运算符重载

为了方便 groovy 中使用，jse 对 `IVector`
提供了许多运算符重载，这里将其列出（`v` 代表向量，`x` 代表浮点数，`i, j` 代表整数）：

| operator         | method                | 描述                                                                                                       |
| ---------------- | --------------------- | ---------------------------------------------------------------------------------------------------------- |
| `v + x`          | `v.plus(x)`           | `v` 每个元素加上 `x`，将结果设置到新的向量                                                                 |
| `v1 + v2`        | `v.plus(v2)`          | `v1` 每个元素分别加上 `v2` 对应位置的元素，将结果设置到新的向量                                            |
| `v - x`          | `v.minus(x)`          | `v` 每个元素减去 `x`，将结果设置到新的向量                                                                 |
| `v1 - v2`        | `v.minus(v2)`         | `v1` 每个元素分别减去 `v2` 对应位置的元素，将结果设置到新的向量                                            |
| `v * x`          | `v.multiply(x)`       | `v` 每个元素乘以 `x`，将结果设置到新的向量                                                                 |
| `v1 * v2`        | `v.multiply(v2)`      | `v1` 每个元素分别乘以 `v2` 对应位置的元素，将结果设置到新的向量                                            |
| `v / x`          | `v.div(x)`            | `v` 每个元素除以 `x`，将结果设置到新的向量                                                                 |
| `v1 / v2`        | `v.div(v2)`           | `v1` 每个元素分别除以 `v2` 对应位置的元素，将结果设置到新的向量                                            |
| `v % x`          | `v.mod(x)`            | `v` 每个元素对 `x` 取余数，将结果设置到新的向量                                                            |
| `v1 % v2`        | `v.mod(v2)`           | `v1` 每个元素分别对 `v2` 对应位置的元素取余数，将结果设置到新的向量                                        |
| `-v`             | `v.negative()`        | `v` 每个元素取负数，将结果设置到新的向量                                                                   |
| `v[i]`           | `v.getAt(i)`          | 获取 `v` 的第 `i` 个元素，支持负数索引                                                                     |
| `v(i)`           | `v.call(i)`           | 获取 `v` 的第 `i` 个元素                                                                                   |
| `v[i] = x`       | `v.putAt(i, x)`       | 设置 `v` 的第 `i` 个元素为 `x`，支持负数索引                                                               |
| `v[i..<j]`       | `v.getAt(i..<j)`      | 获取 `v` 的第 `i`（包含）到第 `j`（不包含）元素组成的向量，会进行值拷贝                                    |
| `v[i..<j] = x`   | `v.putAt(i..<j, x)`   | 将 `v` 的第 `i`（包含）到第 `j`（不包含）的元素设置为 `x`                                                  |
| `v1[i..<j] = v2` | `v1.putAt(i..<j, v2)` | 将 `v1` 的第 `i`（包含）到第 `j`（不包含）的元素分别设置为 `v2` 的第 `0`（包含）到第 `j-i`（不包含）的元素 |



## 其他向量运算

除了运算符重载的基础运算，对于 `IVector` 也提供了许多tita
