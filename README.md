# jse - Java Simulation Environment

**jse (Java Simulation Environment)** is a high-performance, extensible simulation framework for atomistic and materials modeling.  
It is written in Java and leverages **JIT compilation**, **modern JVM features**, and **native extensions (JNI)** to combine performance with flexibility.

jse is designed for **materials scientists, computational physicists, and chemists**, including users who may *not* have a strong programming background, while still providing powerful low-level control for advanced users.


## Key Features

### Atomic & Materials Modeling
- Atomic structure construction and manipulation
- Structure analysis:
  - Radial Distribution Function (RDF)
  - Structure Factor (SF)
  - Bond-Orientational Order Parameters (BOOP, multi-type supported)

### Interatomic Potentials
- Classical potentials:
  - Lennard-Jones (LJ)
  - Embedded Atom Method (EAM)
  - Soft potentials
- Machine-learning potentials:
  - Neural Network Atomic Potential (NNAP)
  - Neuroevolution Potential (NEP)

### File Format Support
- **LAMMPS**: `data`, `dump`, `log`
- **VASP**: `POSCAR`, `XDATCAR`
- Common formats: `xyz`, `csv`, `json`, `yaml`, `toml`

### High-Performance Computing
- MPI support
- Native LAMMPS integration (custom pair/fix supported)
- Generic parallel interface (`parfor`)
- Slurm resource partitioning and high-throughput task management

### Multi-language & Interactive Support
- Groovy scripting (primary interface)
- Python scripting
- Jupyter Notebook support
- ASE compatibility

### Utilities
- Basic plotting
- Basic math
- Compression / decompression
- SSH connectivity


## Installation

jse provides **one-command installation scripts** for Linux and Windows.

### Using cURL
```bash
bash <(curl -fsSL https://raw.githubusercontent.com/liqa1024/jse/main/scripts/get.sh)
```

### Using Wget

```bash
bash <(wget https://raw.githubusercontent.com/liqa1024/jse/main/scripts/get.sh -O -)
```

### Using PowerShell (Windows)

```powershell
Invoke-Expression (Invoke-Webrequest 'https://raw.githubusercontent.com/liqa1024/jse/main/scripts/get.ps1' -UseBasicParsing).Content
```


### Manual Installation

1. Install a **JDK (Java Development Kit)**

   * Windows users are strongly recommended to use
     [Microsoft Build of OpenJDK](https://learn.microsoft.com/java/openjdk/download)
     to avoid CRT conflicts.

2. Download the latest release from
   **[GitHub Releases](https://github.com/liqa1024/jse/releases/latest)**

3. Extract the package and add the directory to your `PATH`.

4. Verify installation:
   
   ```bash
   jse -v
   ```


## Basic Usage

All jse functionality is accessed via **scripts**, not Java source code.

You can write scripts in **Groovy** or **Python** — no Java experience is required.

### Example: Create an FCC Cu Structure

#### Groovy

```groovy
import jse.atom.AtomData
import jse.atom.data.DataXYZ

double a = 3.8
def data = AtomData.builder()
    .add(0.0, 0.0, 0.0, 'Cu')
    .add(a/2, a/2, 0.0, 'Cu')
    .add(a/2, 0.0, a/2, 'Cu')
    .add(0.0, a/2, a/2, 'Cu')
    .setBox(a, a, a)
    .build()

DataXYZ.of(data).write('Cu-fcc.xyz')
```

#### Python

```python
from jse.atom import AtomData
from jse.atom.data import DataXYZ

a = 3.8
data = AtomData.builder() \
    .add(0.0, 0.0, 0.0, 'Cu') \
    .add(a/2, a/2, 0.0, 'Cu') \
    .add(a/2, 0.0, a/2, 'Cu') \
    .add(0.0, a/2, a/2, 'Cu') \
    .setBox(a, a, a) \
    .build()

DataXYZ.of(data).write('Cu-fcc.xyz')
```

Run the script:

```bash
jse path/to/script.groovy
```

or

```bash
jse path/to/script.py
```

> jse automatically detects the scripting language by file extension.


## IDE Support (Groovy IntelliSense)

For **IntelliJ IDEA** users:

1. Initialize in the project directory (with IDEA closed):
   
   ```bash
   jse --idea
   ```

2. Open the directory in IntelliJ and configure the JDK:
   
   ```text
   File → Project Structure → Project → SDK
   ```


## Advanced Features

Some advanced features (MPI, native LAMMPS, NNAP, Python embedding) require **JNI libraries**.

A C/C++ compiler is required:

* **Windows**: [MSVC](https://visualstudio.microsoft.com/vs/features/cplusplus/)
* **Linux**: [GCC](https://gcc.gnu.org/)

jse automatically builds required native libraries on demand, or you can build all at once:

```bash
jse --jnibuild
```

> Missing dependencies will be clearly reported.


### MPI Support

Recommended MPI environments:

* Windows:
  
  [Microsoft MPI](https://www.microsoft.com/download/details.aspx?id=105289)
  (install both SDK and runtime)
  
* Linux:
  
  ```bash
  sudo apt install libopenmpi-dev
  ```

Run jse with MPI:

```bash
mpiexec -np 4 jse script.groovy
```

On Windows:

```bash
mpiexec -np 4 jse.bat script.groovy
```

⚠️ Ensure JNI libraries are initialized *before* MPI execution to avoid race conditions.


### LAMMPS Integration

LAMMPS is automatically downloaded and compiled when required.

Run LAMMPS via jse:

```bash
jse -lmp -in melt.in -log lammps.log
```

MPI is fully supported:

```bash
mpiexec -np 4 jse -lmp -in melt.in
```

Specify LAMMPS packages:

```bash
export JSE_LMP_PKG="EXTRA-COMPUTE EXTRA-FIX EXTRA-PAIR MANYBODY"
```


### Python Support

A Python development environment is required.

On Linux:

```bash
sudo apt install python3-dev
```

or use a [Conda/Miniconda environment](https://www.anaconda.com/download/success).

Run Python scripts with jse:

```bash
jse --python script.py
```

> Omitting `--python` is allowed for `.py` files.


## Citation

If you use jse in academic work, please cite:

* **NNAP (`jsex.nnap`)**:
  Rui Su *et al.*
  [Efficient and accurate simulation of vitrification in multi-component metallic 
  liquids with neural-network potentials](https://link.springer.com/article/10.1007/s40843-024-2953-9),
  *Science China Materials* **67**, 3298–3308 (2024)

* **FFS & Multi-type BOOP (`jsex.rareevent`)**:
  Qing-an Li *et al.*
  [Revealing Crystal Nucleation Behaviors in Metallic Glass-Forming Liquids 
  via Parallel Forward Flux Sampling with Multi-Type Bond-Orientational Order 
  Parameter](https://www.sciencedirect.com/science/article/abs/pii/S1359645425011589),
  *Acta Materialia* **(2025)**


## License

This project is licensed under **GNU GPL v3**.
See [LICENSE](LICENSE) for details.


## Acknowledgments

jse builds upon a number of excellent open-source projects. We gratefully acknowledge the following tools and libraries:

* **[jep](https://github.com/ninia/jep)**:
  Enables efficient and reliable integration between the JVM and CPython, forming the foundation of Python scripting support.

* **[jupyter-jvm-basekernel](https://github.com/SpencerPark/jupyter-jvm-basekernel)**:
  Provides the JVM-based Jupyter kernel that allows jse to be used interactively within Jupyter notebooks.

* **[mwiede/jsch](https://github.com/mwiede/jsch)**:
  Used for SSH-based remote connectivity, supporting interaction with remote servers and HPC environments.

* **[JFreeChart](https://www.jfree.org/jfreechart/)**:
  Supplies lightweight plotting capabilities for quick data visualization.

* **[Hellblazer/Voronoi-3D](https://github.com/Hellblazer/Voronoi-3D)**:
  Provides 3D Voronoi tessellation algorithms used in structural and local-environment analysis.

* **[xmake](https://github.com/xmake-io/xmake)**:
  Inspired parts of one-command installation scripts.

