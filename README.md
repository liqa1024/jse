# Introduction

jse (Java Simulation Environment) is a simulation environment written in Java, 
designed to leverage Java's advanced language features and JIT runtime compilation 
to provide a high-performance yet flexible program package. Current features include:

- LAMMPS file I/O (data, dump, log)
- VASP file I/O (POSCAR, XDATCAR)
- Atomic structure parameter calculations (RDF, SF, BOOP)
- Efficient implementations of common interatomic potentials (LJ, EAM, soft)
- Efficient implementations of ML interatomic potentials (NNAP, NEP)
- Generic parallel interface (parfor)
- Slurm resource partitioning and high-throughput task management
- Native LAMMPS calls and custom pair/fix implementations
- MPI support
- Python support
- Jupyter support
- Other common operations (csv/yaml/toml I/O, compression/decompression, basic plotting, ...)


# Requirements

## Core Functionality
For basic operations, ensure you have a Java environment. jse requires at least JDK 8, 
but newer JDK 21 is recommended for optimal performance. Download the Oracle JDK 
[**here**](https://www.oracle.com/java/technologies/downloads/#java21).

## Advanced Features (JNI)

Features involving C/C++ calls via JNI require the following for automatic source compilation:

- **C/C++ Compiler**
    
    - Windows: [MSVC](https://visualstudio.microsoft.com/vs/features/cplusplus/)  
    - Linux: [GCC](https://gcc.gnu.org/)

- **CMake** (`>= 3.15`)
    
    Download from [cmake.org](https://cmake.org/)

When these dependencies are installed, required libraries will compile automatically 
during runtime. Additional software is needed for specific features:

### MPI Support

Requires an MPI development environment (for header files):  

- Windows: [Microsoft MPI](https://www.microsoft.com/download/details.aspx?id=105289) 
  (install both `msmpisdk.msi` and `msmpisetup.exe`)  
- Linux (e.g., Ubuntu): `sudo apt install mpich`  

### Python Support

Requires Python development headers:  

- Linux (e.g., Ubuntu): `sudo apt install python3-dev`,
  a dedicated virtual environment is recommended:  
  
    ```shell
    python -m venv jsepyenv
    ```

### LAMMPS Support

LAMMPS is automatically downloaded and compiled by default. For custom installations:  

1. Compile LAMMPS as a shared library:
    
    ```shell
    cmake -D BUILD_SHARED_LIBS=ON .
    ```

2. For custom fixes/pairs (including NNAP support), enable the plugin package: 
    
    ```shell
    cmake -D PKG_PLUGIN=ON .
    ```

3. Ensure your LAMMPS build directory has this structure:  
   
    ```text
    build
        ├─lib
        │   └─liblammps.so
        └─includes
            └─lammps
                ├─library.h
                ...
    ```

4. Set the `JSE_LMP_HOME` environment variable:  
    
    ```shell
    export JSE_LMP_HOME="path/to/lammps/build"
    ```


# Usage

## Installation

1. Download the latest release from [**Releases**](https://github.com/CHanzyLazer/jse/releases/latest)  
2. Extract the package and add the directory to your `PATH` environment variable.  

> Verify with `jse -v` (prints version info if successful).
> 

## Basic Usage

Execute Groovy scripts via: 

```shell
jse path/to/script.groovy
```

Python scripts are also supported:

```shell
jse path/to/script.py
```

> jse auto-detects Groovy/Python by file extension.
> 

## IntelliSense
For Groovy IntelliSense in [IntelliJ IDEA](https://www.jetbrains.com/idea/):  

1. Initialize the project directory (with IDEA closed):  
    
    ```shell
    jse -idea
    ```

2. Configure JDK in IDEA:  
    
    ```text
    File → Project Structure → Project Settings → Project → SDK 
    → Select installed JDK → Set language level → Apply
    ```

# Compilation

The project uses [Gradle](https://gradle.org/). Build with:

```shell
./gradlew build  # Outputs JAR to release/lib
```


# Citation
*(To be added)*


# License
*(To be added)*


# Acknowledgments
Special thanks to:  
- [jep](https://github.com/ninia/jep) for Python integration  
- [SpencerPark/jupyter-jvm-basekernel](https://github.com/SpencerPark/jupyter-jvm-basekernel) for Jupyter support  
- [mwiede/jsch](https://github.com/mwiede/jsch) for SSH connectivity  
- [JFreeChart](https://www.jfree.org/jfreechart/) for plotting capabilities  

