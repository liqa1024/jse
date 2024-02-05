
# 项目文件结构

具体项目结构可以参考 `jse-full-${tag}.zip` 文件，大致如下：

```
└─release
    ├─jse
    ├─jse.bat
    ├─lib
    │   └─jse-all.jar
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

- `jse` 和 `jse.bat` 分别为用来在 linux 和 windows 下运行此程序的脚本，
  这样可以在两个系统下都可以使用同样的类似 `./jse xxx` 的指令来运行此程序。

- `lib` 目录存放库文件，默认情况下只会有一个 `jse-all.jar` 文件。

- `script` 目录存放项目所有的脚本文件，并且如上不同语言分配不同的文件夹。
  **注意不同语言只有放在上述目录指定文件夹中才能被程序检测到，
  如对于 groovy 脚本为 `script/groovy/*`，
  对于 python 脚本为 `script/python/*`。**

- `lmp` 存放 lammps 相关的数据，例如 lammps 的 data 文件则在 `lmp/data/*`，
  力场文件则在 `lmp/potential/*`（非硬性）。

- `.idea` 和 `.run` 为 IntelliJ IDEA 识别此项目文件夹以及运行脚本需要的设置。

- `.vscode` 为 VScode 运行脚本需要的设置。

