# jse - IO Utilities Documentation

The `jse.code.IO` class is

 the comprehensive file input/output toolkit for the **jse** framework. It is designed to replace standard Java `File` and `Files` interaction with a simplified, string-path-based API optimized for scientific scripting.

**Key Design Philosophies:**
1.  **String Paths:** All methods accept `String` paths. You rarely need `java.io.File` or `java.nio.file.Path` objects.
2.  **Auto-Creation:** Writing to a file automatically creates the file and any missing parent directories.
3.  **UTF-8 Everywhere:** All text operations default to `UTF-8` encoding.
4.  **LF Line Endings:** All writing operations force `\n` (LF) line endings, ensuring consistency across Windows and Linux HPC environments.

**Import Strategy:**
```groovy
import jse.code.IO
// OR for heavy usage
import static jse.code.IO.*
```

---

## 1. Basic File Operations

These methods handle standard filesystem manipulations. They operate on `String` paths relative to the script's working directory.

| Method | Description |
| :--- | :--- |
| `exists(String path)` | Returns `boolean`. Checks if a file or directory exists. |
| `isfile(String path)` | Returns `boolean`. Checks if path is a regular file. |
| `isdir(String path)` | Returns `boolean`. Checks if path is a directory. |
| `delete(String path)` | Deletes a file or an *empty* directory. Does nothing if path doesn't exist. |
| `copy(String src, String dest)` | Copies a file (or stream/URL). Overwrites existing files. Auto-creates parent dirs. |
| `move(String src, String dest)` | Moves a file or directory. |
| `list(String dir)` | Returns `String[]` of filenames in a directory. Throws error if dir is missing. |

**Example:**
```groovy
import jse.code.IO

String src = 'data/raw.txt'
if (IO.exists(src)) {
    // Automatically creates 'backup/' folder if missing
    IO.copy(src, 'backup/raw_copy.txt') 
    IO.delete(src)
}
```

---

## 2. Reading & Writing Text

The core functionality for processing configuration files, logs, and simulation data.

### 2.1 Writing
*   **`write(path, content)`**: Writes content to a file. **Append newline (`\n`) at the end.**
*   **`writeText(path, text)`**: Writes exact string content. **No trailing newline added.**

```groovy
import jse.code.IO

// Write a single string (auto-adds \n)
IO.write('log.txt', 'Simulation started')

// Write a list of lines
def lines = ['Atom 1', 'Atom 2', 'Atom 3']
IO.write('atoms.txt', lines)

// Write binary data
byte[] data = ...
IO.write('binary.dat', data)
```

### 2.2 Reading
All read methods strictly use UTF-8.

```groovy
import jse.code.IO

// Read entire file into a String
def content = IO.readAllText('config.json')

// Read into List<String> (strips trailing newline of file)
def lines = IO.readAllLines('trajectory.xyz')

// Read first N lines (efficient for large files)
def header = IO.readLines('huge_file.dump', 100)
```

---

## 3. Directory Management

Unlike standard Java `File.mkdir()`, `jse` methods work recursively.

| Method | Description |
| :--- | :--- |
| `mkdir(String dir)` | Creates a directory and all necessary parent directories. |
| `rmdir(String dir)` | **Recursive delete.** Deletes a directory and all its contents (like `rm -rf`). |
| `cpdir(String src, String dest)` | **Recursive copy.** Copies a directory structure to a new location. |

---

## 4. Structured Data (CSV, JSON, YAML, TOML)

`IO` provides extensive support for common data formats, seamlessly integrating with `jse` math objects (`IVector`, `IMatrix`, `ITable`).

### 4.1 CSV & Simulation Data
Designed for scientific data (matrices, vectors, xy columns).

*   **Writing:** `data2csv`, `rows2csv`, `cols2csv`.
*   **Reading:** `csv2data`, `csv2table`.

**Writing Matrix/Vector Data:**
```groovy
import jse.code.IO
import jse.math.vector.IVector
import jse.math.vector.Vectors

// 1. Save a Vector as a single column
def vec = Vectors.linspace(0.0d, 10.0d, 100)
IO.data2csv(vec, 'output.csv', 'Time') // 'Time' is the header

// 2. Save a Matrix (default is row-based)
def matrix = ... // Some IMatrix
IO.data2csv(matrix, 'matrix.csv', 'x', 'y', 'z')

// 3. Save raw 2D arrays by column (useful for x, y plotting data)
def x = [1.0d, 2.0d, 3.0d] as double[]
def y = [0.1d, 0.4d, 0.9d] as double[]
// Creates file with columns: A, B
IO.cols2csv([x, y], 'plot.csv', 'A', 'B')
```

**Reading Matrix/Table Data:**
```groovy
import jse.code.IO

// Automatically ignores '#' comments and headers
// Returns a RowMatrix (jse.math.matrix.RowMatrix)
def matrix = IO.csv2data('data.csv')

// Automatically read headers or 'C0', 'C1', ... for no head csv
// Returns a Table (jse.math.table.Table)
def table = IO.csv2table('table.csv')
```

### 4.2 Configuration Formats
Supports automatic conversion between Strings/Files and Maps/Lists.

| Format | Map to File | File to Map |
| :--- | :--- | :--- |
| **JSON** | `map2json(map, path, pretty)` | `json2map(path)` |
| **YAML** | `map2yaml(map, path)` | `yaml2map(path)` |
| **TOML** | `map2toml(map, path)` | `toml2map(path)` |

**Example:**
```groovy
import jse.code.IO

def config = IO.json2map('settings.json')
config['temperature'] = 300.0 // or config.temperature = 300.0
IO.map2json(config, 'settings_new.json', true) // true = pretty print
```

## 4.3 Domain-Specific Formats (LAMMPS, VASP, XYZ)

While `IO` provides generic text/CSV tools, **do not manually parse** scientific file formats. `jse` includes specialized classes optimized for performance and structure validation.

**Recommendation:** Always check specific packages before writing custom parsers.

| Format | Recommended Class | Description |
| :--- | :--- | :--- |
| **LAMMPS Dump** | `jse.lmp.Lammpstrj` / `jse.lmp.Dump` | Efficient reading of atom trajectories (`.dump`, `.lammpstrj`). |
| **LAMMPS Data** | `jse.lmp.Lmpdat` / `jse.lmp.Data` | Reading/Writing structure files (`data.*`, `*.data`). |
| **LAMMPS Log** | `jse.lmp.Thermo` / `jse.lmp.Log` | Extracting thermodynamic data from logs (`log.lammps`). |
| **Single XYZ** | `jse.atom.data.DataXYZ` | Standard / Extended XYZ format. |
| **Multiple XYZ** | `jse.atom.data.DumpXYZ` | Multiple XYZ data. |
| **VASP POSCAR** | `jse.vasp.POSCAR` | Reading/Writing VASP structure files (`POSCAR`, `CONTCAR`). |

**Example:**
```groovy
// DON'T process a POSCAR line-by-line using IO.readAllLines
// DO use the specialized class:
import jse.vasp.POSCAR
import jse.lmp.Lmpdat

def structure = POSCAR.read('POSCAR')
// Convert to LAMMPS data format easily
Lmpdat.of(structure).write('data.new')
```

---

## 5. Streaming & Processing

For large files that cannot fit in memory, use stream wrappers.

### 5.1 Writers (`IO.IWriteln`)
A simplified wrapper around `BufferedWriter` that handles buffering and standardizes newlines.

```groovy
import jse.code.IO

// 'toWriteln' creates a buffered writer
try (def writer = IO.toWriteln('massive_output.txt')) {
    for (i in 0..<1000000) {
        // Efficiently writes line + \n
        writer.writeln("Step ${i} data...") 
    }
}
```

### 5.2 Readers & Mappers
Efficient line-by-line processing.

*   `IO.toReader(path)`: Returns a `BufferedReader`.
*   `IO.map(src, dest) {line -> ...}`: Reads `src`, transforms line, writes to `dest`.

```groovy
import jse.code.IO

// Replace text in a file without loading it all into RAM
IO.map('input.in', 'output.in') {String line ->
    return line.replace('TEMP_PLACEHOLDER', '300.0')
}
```

---

## 6. Compression

Native Java-based zip manipulation.

*   `zip2dir(zipPath, outDir)`: Unzips archive to directory.
*   `dir2zip(inDir, zipPath)`: Zips a directory.
*   `files2zip(filesList, zipPath)`: Zips specific files/folders into an archive.

---

## 7. String Utilities (`IO.Text`)

Helper class for string manipulation, often useful for customize parsing simulation outputs.

```groovy
import jse.code.IO

// 1. Parse space-separated numbers (handles multiple spaces better than split())
// Useful for parsing LAMMPS data lines
String line = '  1    1.00   2.05   -0.5 '
def vec = IO.Text.str2data(line, 4) // Returns Vector of length 4

// 2. Colored Terminal Output (for scripts)
println(IO.Text.red('Error: Convergence failed'))
println(IO.Text.green('Success'))

// 3. String Splitting, return String[]
def parts = IO.Text.splitStr('a, b  c') // Splits by comma OR space
```

---

## 8. IO Path Helpers

Pure string manipulation methods (does not touch filesystem).

*   `toAbsolutePath(path)`: Resolves path relative to `WORKING_DIR`. Supports `~` for user home.
*   `toParentPath(path)`: Returns the directory string of a file.
*   `toFileName(path)`: Returns the filename only.
*   `toRelativePath(path)`: Calculates path relative to `WORKING_DIR`.
