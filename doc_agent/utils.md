# jse - Utilities Library Documentation

This document outlines the core utility classes within `jse` used for system interaction, mathematical operations, parallel processing, and physical constants.

**Import Strategy:**
For concise scripts, static imports are highly recommended.
```groovy
import jse.code.Conf
import jse.code.OS
import static jse.code.CS.*        // Constants (Units, Mass, etc.)
import static jse.code.UT.Par.*    // Parallel tools
import static jse.code.UT.Math.*   // Math functions (sin, cos, sqrt...)
import static jse.code.UT.Timer.*  // Timing and Progress bars
```

---

## 1. Global Configuration (`jse.code.Conf`)

Controls the global behavior of the `jse` runtime.

| Field / Method | Type | Description |
| :--- | :--- | :--- |
| `DEBUG` | `boolean` | If `true`, enables verbose error stacks. Default via env `JSE_DEBUG`. |
| `PARFOR_THREAD_NUMBER` | `int` | Default threads for `parfor`. Default is CPU thread count. |
| `WORKING_DIR_OF(name)` | `String` | Returns a path to a temporary working directory ending with `/`. |

**Usage Skill: Creating Temporary Directories**
To create a safe, non-conflicting temporary directory for intermediate files (e.g., LAMMPS logs):
```groovy
import jse.code.Conf
import jse.code.IO
import jse.code.UT

// Generate a path: .temp/{random_hex}/
String tempDir = Conf.WORKING_DIR_OF(UT.Code.randID())

// Use it
IO.write(tempDir + 'data.txt', 'content')

// Clean up is manual if required
```

---

## 2. System & Environment (`jse.code.OS`)

Provides OS-independent access to file paths and system commands.

**Note on Paths:** All directory constants in `OS` return an **"internal valid dir"**, meaning the string always ends with a file separator (`/` or `\`). You can safely append filenames: `OS.WORKING_DIR + 'file.txt'`.

### 2.1 Path Constants

| Constant | Description |
| :--- | :--- |
| `IS_WINDOWS` | `true` if running on Windows. |
| `WORKING_DIR` | Current working directory (where script runs). Ends with `/`. |
| `USER_HOME_DIR` | User home directory. Ends with `/`. |
| `JAR_DIR` | Directory containing the `jse` jar file. |
| `NO_LOG` | Platform specific null device (`NUL` or `/dev/null`). |

### 2.2 System Commands
Uses `bash` on Linux/macOS and `powershell` on Windows.

```groovy
import jse.code.OS

// Execute and wait. Returns exit code (0 = success)
int code = OS.system('ls -l')

// Execute and capture output as List<String>
def files = OS.system_str('ls')

// Write stdout to file
OS.system('echo "hello"', 'output.txt')
```

---

## 3. Physical Constants & Data (`jse.code.CS`)

A collection of static constants for physics and chemistry.

### 3.1 Physical Constants

| Constant | Unit | Description |
| :--- | :--- | :--- |
| `K_B` | eV/K | Boltzmann constant. |
| `E_V` | J | Electron volt value in Joules. |
| `H_BAR` | eV·ps | Reduced Planck constant. |
| `N_A` | - | Avogadro constant. |

### 3.2 Atomic Data
Maps contain data for elements H (1) through Og (118).

*   **Symbols:** `CS.SYMBOLS` (Array, `SYMBOLS[0] == 'H'`)
*   **Symbols ↔ Atomic number:** `CS.ATOMIC_NUMBER_TO_SYMBOL` (List, `ATOMIC_NUMBER_TO_SYMBOL[1] == 'H'`) and `CS.SYMBOL_TO_ATOMIC_NUMBER` (Map, `SYMBOL_TO_ATOMIC_NUMBER.H == 1`)
*   **Mass:** `CS.MASS` (Map, g/mol). **Tip:** Use property access for readability.
    ```groovy
    import static jse.code.CS.MASS
    
    double massCu = MASS.Cu  // 63.546
    double massH = MASS.H // 1.008
    ```

### 3.3 Units (`CS.UNITS`)
A Map containing unit conversion factors based on 2018 CODATA, for default values in ASE, use `CS.UNITS_ASE`.

```groovy
import static jse.code.CS.*

assert EV_TO_KCAL == UNITS.mol / UNITS.kcal
```

---

## 4. Utilities (`jse.code.UT`)

The primary toolkit for scripting logic.

### 4.1 Parallel Processing (`UT.Par`)

Provides simple shared-memory parallelism.

*   **`parfor(int n) {i -> ... }`**
    Parallel for-loop. Automatically handles thread pooling.
    ```groovy
    import static jse.code.UT.Par.parfor
    
    // Compact style
    parfor(100) {i ->
        // Logic for index i
    }

    // With Thread ID (useful for thread-local buffers)
    parfor(100) {i, threadID ->
        // i: 0..99, threadID: 0..nThreads-1
    }
    ```

*   **`splitRandoms(int n)`**: Creates `n` independent random number generators. Essential for thread safety in `parfor`.

### 4.2 Math Wrapper (`UT.Math`)

Provides an API style similar to **NumPy / MATLAB** for `jse` logic.
While `jse` vectors support OOP style (e.g., `vec.mean()`), this class allows functional style.

**Scalar Operations:**
`sin`, `cos`, `tan`, `exp`, `log`, `log10`, `sqrt`, `pow`, `abs`, `floor`, `ceil`.

**Vector/Matrix Operations:**
These accept `IVector` or `IMatrix` and return a new object (element-wise operation).

```groovy
import static jse.code.UT.Math.*

def vec = linspace(0, 1, 10)
def res = sqrt(vec) + pow(vec, 2.1)
```

**Generators:**
*   `zeros(n)`, `ones(n)`: Create vectors.
*   `linspace(start, end, n)`: Linear spacing.
*   `rand(n)`, `randn(n)`: Uniform (0-1) or Gaussian vectors.

### 4.3 Timing & Progress (`UT.Timer`)

*   **`tic()` / `toc()`**: Simple stopwatch.
    ```groovy
    import static jse.code.UT.Timer.*
    
    tic()
    // ... heavy calculation ...
    toc() // Prints: "Total time: 1.23 sec"
    ```
*   **`pbar(long total)`**: Displays a console progress bar.
    ```groovy
    import static jse.code.UT.Timer.*
    
    pbar(1000)
    for (i in 0..<1000) {
        // ... calculation ...
        pbar() // Update step
    }
    ```

### 4.4 Plotting (`UT.Plot`)
A wrapper around JFreeChart for quick data visualization. For publication-quality plots or complex control, use the Python interface (`matplotlib`).

```groovy
import static jse.code.UT.Math.*
import static jse.code.UT.Plot.*

def x = linspace(0, 10, 100)
def y = sin(x)

plot(x, y)
// Helper: loglog, semilogx, semilogy
```

### 4.5 Code Helpers (`UT.Code`)

*   **`randID()`**: Returns a random 16-char hex string (useful for filenames).
*   **`randSeed()`**: Returns a random integer (useful for LAMMPS seeds).

---

## 5. Summary of Other Primary Classes

### `jse.code.IO`
(Separate documentation) Handles all file reading/writing.

### `jse.code.SP`
(See Python documentation) Handles "Scripting Polyglot" features.
*   `SP.Python`: Access the embedded Python interpreter.
*   `SP.Groovy`: Access Groovy class loaders (rarely needed directly in scripts).
