# jse - Python Interface Guide

**jse (Java Simulation Environment)** integrates a high-performance Java/JVM backend with the flexible Python ecosystem via **JEP (Java Embedded Python)**. This document acts as a comprehensive guide for materials scientists and developers to mix Groovy, Java, and Python effectively within the jse framework.

## 1. Running Python Scripts
You can execute Python scripts directly using the `jse` command. The relevant JNI library will automatically compile to allow seamless two-way communication.

### Execution Modes

*   **Execute a script:**
    ```bash
    jse myscript.py
    # or explicitly:
    jse --python myscript.py
    ```

*   **Interactive Shell (REPL):**
    ```bash
    jse --python
    ```

*   **Inline Text Code:**
    ```bash
    jse --pythontext "print(sum([i for i in range(10)]))"
    ```

---

## 2. Using Java/Groovy Classes in Python

jse allows Python to import and instantiate Java classes. This is useful for utilizing jse’s  libraries like utility functions (`jse.code.IO`, `jse.code.UT`).

### 2.1 Basic Imports
jse allows you to import and use Java classes directly in Python.

```python
# Import standard Java classes
from java.util import ArrayList, HashMap

# Import jse core classes
from jse.math.vector import Vectors

# Use typical Java methods
vec = Vectors.zeros(5)
vec.fill(3.14)
print(vec) 
```

### 2.2 Using Groovy Classes in Python

There are two methods to import Groovy classes into the Python environment.

#### Method A: The Standard JSE Way (Recommended)
To avoid the overhead of scanning the entire classpath, access Groovy classes via the `SP.Groovy` helper methods:

```python
from jse.code import SP

# Retrieve a class reference
SomeClass = SP.Groovy.getClass('my.custom.package.SomeClass')

# create an instance, like `new SomeClass('arg1', 123)` in groovy
instance = SomeClass('arg1', 123)

# Use the instance
instance.someMethod()
```

#### Method B: Direct Import Hook (Slow Startup)
If you prefer standard Python syntax (e.g., `from my.package import MyClass`), you can enable the import hook. 
*   **Enable:** Set environment variable `JSE_JEP_ADD_GROOVY_HOOK=1` before running jse.
*   **Warning:** This forces jse to scan directories, may leading to a **slower startup**.

```bash
export JSE_JEP_ADD_GROOVY_HOOK=1
jse script.py
```

```python
# Now standard imports work for Groovy classes
from my.custom.package import SomeClass

obj = SomeClass()
```

---

## 3. Embedding Python in Groovy

Groovy is the primary scripting language for jse, but you can embed Python for libraries like NumPy, Matplotlib, or ASE.

### 3.1 Seamless Object Wrapper
jse features an optimized `PyObject` wrapper. You do **not** need to call `exec()` for every operation. Once a Python module or object is retrieved into Groovy, you can use it with native dot notation.

**Workflow:**
1.  **Import** the module once using `exec`.
2.  **Get** the module into a Groovy variable.
3.  **Use** it as if it were a Groovy object.

```groovy
import jse.code.SP

// 1. Import numpy in the Python Interpreter
SP.Python.exec('import numpy as np')

// 2. Retrieve the 'np' module wrapper
def np = SP.Python.get('np')

// 3. Use standard OOP syntax
// Call numpy functions directly from Groovy
def pyArray = np.linspace(0.0, 10.0, 11)
double meanVal = (double)np.mean(pyArray)

println("Mean value from Python: $meanVal")
```

### 3.2 Passing Variables
Data is shared via the interpreter's global context.

```groovy
def javaData = [1.0, 2.0, 3.0, 4.0] as double[]

// Set variable in Python scope
SP.Python.set('raw_data', javaData)

// Perform calculation in Python
SP.Python.exec('import numpy as np; std_dev = np.std(raw_data)')

// Retrieve result
double result = (double)SP.Python.get('std_dev')
```

---

## 4. Data Type Conversion (Zero-Copy & Memcpy)

Efficient data transfer between the JVM (Java Heap) and CPython (Native Memory) is critical for performance.

### 4.1 Type Mapping Table

| Java Type | Python Type | Note |
| :--- | :--- | :--- |
| `boolean` | `bool` | **auto-convert** |
| `int`, `long`, `double` | `int`, `float` | **auto-convert** |
| `String` | `str` | **auto-convert** |
| `java.util.List` | `list` (wrapper) | **auto-convert**, changes might reflect on both sides. |
| `java.util.Map` | `dict` (wrapper) | **auto-convert**, changes might reflect on both sides. |
| `jep.NDArray` | `numpy.ndarray` | **auto-convert**, **copy** |
| `jse...IVector` | `numpy.ndarray` | **manual** (via `.numpy()`), **copy** |
| `jse...IMatrix` | `numpy.ndarray` | **manual** (via `.numpy()`), **copy** |

### 4.2 Java to NumPy
JSE math objects (`IVector`, `IMatrix`) implement a `.numpy()` method.

**Important:** This operation performs a **memory copy (memcpy)**. Modifying the resulting NumPy array in Python *does not* affect the original Java vector. This ensures memory safety.

```python
# In a Python script running inside jse
from jse.math.vector import Vectors

# Create Java Vector
j_vec = Vectors.linspace(0.0, 1.0, 5)

# Convert to NumPy (Copy)
np_arr = j_vec.numpy()

# np_arr is now a standard numpy.ndarray
```

### 4.3 NumPy to Java
Use the `Vectors` or `Matrices` helper classes to convert NumPy arrays back to Java structures.

```python
from jse.math.vector import Vectors
from jse.math.matrix import Matrices
import numpy as np

py_data = np.array([1.0, 2.0, 3.0])

# Convert to Java Vector (Copy)
# Vectors.fromNumpy(ndarray, warn_if_unsigned_bool)
j_vec = Vectors.fromNumpy(py_data)
```

---

## 5. Jupyter Notebook Support

jse includes a custom Jupyter kernel that supports mixed Groovy and Python cells in the same notebook.

### 5.1 Installation
```bash
jse --jupyter
# Then launch:
jupyter notebook
```

### 5.2 Variable Scoping Rules
Sharing variables between Groovy cells and Python magic cells (`%%python`) relies on the Groovy **Binding** (Global Context).

**The `def` Keyowrd Trap:**
*   Defining a variable with `def` or a type (e.g., `String s = ...`) makes it **local** to the script execution. It is **hidden** from the global context and cannot be retrieved by Python.
*   Defining a variable **without** a keyword puts it into the global Binding.

**Correct Usage Example:**

**Cell 1 (Groovy):**
```groovy
// Local scope only
// def myVar = [1, 2, 3]

// Global Binding scope
myData = [1.0, 2.0, 3.0, 4.0] as double[]
```

**Cell 2 (Python Magic):**
```python
%%python
from jse.code import SP
import numpy as np

# Retrieve from global binding
data = SP.Groovy.get('myData')

# Process
print(f'Mean: {np.mean(data)}')
```

---

## 6. Package Management & Environment

### 6.1 Recommended Environment Strategy
While jse provides internal pip installation tools, **using an external environment is strongly recommended** for stability.

1.  Create Python venv:
    ```bash
    python -m venv jse_venv
    source jse_venv/bin/activate
    ```
    or a Conda environment
    ```bash
    conda create -n jse_env python=3.12 numpy matplotlib
    conda activate jse_env
    ```
    
2.  Run `jse`.
    *   jse automatically detects the active `python` executable from your PATH and auto-compile the JNI library.
    *   It will utilize the installed site-packages (NumPy, SciPy, ASE, etc.) from that environment.

### 6.2 Internal Library Directories
If you must install packages or place custom scripts inside jse without an environment manager, verify the directory locations defined in `SP.java`:

*   **Groovy Libs:** `jse/lib/groovy/` (Mapped to `SP.GROOVY_LIB_DIR`)
*   **Python Libs:** `jse/lib/python/` (Mapped to `SP.PYTHON_LIB_DIR`)
    *   *Note:* On Windows, this is `lib/python-win/`. Python packages containing C-extensions are platform-specific.

### 6.3 Internal Installer (Legacy)
You can use the internal command to install packages into `SP.PYTHON_LIB_DIR`, but this is less flexible than Conda/venv:
```groovy
SP.Python.installPackage('pandas')
```

---

## 7. Parallelism & Thread Safety

### 7.1 The GIL and JEP Limits
JSE and JEP **cannot bypass the CPython Global Interpreter Lock (GIL)** for CPU-bound Python tasks within a single process. 

However, JEP places a strict restriction: **A `PyObject` created in one thread cannot be accessed in another.** 

### 7.2 Parallel Execution (`parforWithInterpreter`)
To perform parallel processing involving Python (e.g., parallel data preprocessing or independent file I/O), jse provides `parforWithInterpreter`. This method handles the complex setup of initializing valid Sub-interpreters for every thread.

**Key Concepts:**
1.  **Isolation:** Each thread gets a fresh, independent Python interpreter.
2.  **No Sharing:** You **cannot** share Python objects (Variables, Modules, PyObjects) between threads.
3.  **Java Sharing:** You **can** share thread-safe Java objects (e.g., `AtomicInteger`, `ConcurrentHashMap`, or jse `Vector`s if synchronized).

**Recommended Usage:**

Instead of the standard `UT.Par.parfor`, use the specialized Python version. You generally ignore the `interpreter` argument and call `SP.Python` static methods directly; jse automatically routes calls to the correct thread-local interpreter.

```groovy
import jse.code.SP
import jse.math.vector.Vectors

// Parallel loop over 100 items, 4 threads
int ntasks = 100
int nthreads = 4
// Thread safe buffer
def buf = Vectors.zeros(nthreads)

SP.Python.parforWithInterpreter(ntasks, nthreads) {i, threadID ->
    // This code runs in a thread-local Python environment.
    // 1. Initialize environment (must be done per thread if using modules)
    SP.Python.exec('import numpy as np')
    
    // 2. Set local data
    SP.Python.set('val', i * 1.5)
    
    // 3. Compute
    SP.Python.exec('res = np.sqrt(val) + np.sin(val)')
    
    // 4. Get result (Double) and aggregate to Java object safely
    double res = (double)SP.Python.get('res')
    // 5. Thread independent writing
    buf.add(threadID, res)
}

println("Total: ${buf.sum()}")
```

Based on your input, here is the new section regarding the strategic choice between Groovy and Python for production scripts.

---

## 8. Recommendation: Choosing Between Groovy and Python

While jse supports both languages, choosing the right one for your specific task can significantly impact performance, stability, and development efficiency.

### 8.1 When to use Groovy (Recommended Default)
*   **Core JSE Usage:** If your task relies primarily on jse's internal algorithms and does not strictly require Python libraries, **Groovy is the preferred choice**. It is the native language of the framework.
*   **IDE Support:** Groovy offers superior development experience with explicit types, static compilation checks, and excellent auto-completion/navigation in IntelliJ IDEA (via `jse --idea`).
*   **Parallelism & Multithreading:**
    *   If your workflow involves heavy multi-threading (e.g., analyzing thousands of trajectory frames via `parfor`), **use Groovy**.
    *   Java's threading model is robust and efficient.
    *   JSE's parallel syntax (`parfor { ... }`) is specifically designed for Groovy closures.
    *   Stability: Attempting to use Python's native parallel libraries (`multiprocessing`, `threading`) inside the JEP environment can be unstable or undefined.

### 8.2 When to use Python
*   **Heavy NumPy Operations:** If your algorithm involves massive matrix manipulations or vectorization that *must* happen in NumPy, write it in Python.
    *   **Reason:** Every transfer of data between Java (`jep.NDArray`) and Python (`numpy.ndarray`) incurs a **Deep Copy (Memcpy)** overhead via JNI. Minimizing this boundary crossing is crucial for performance.
*   **Complex Python APIs:** If you heavily utilize libraries with complex syntax sugars (e.g., advanced Matplotlib configurations with context managers, decorators, or keyword-heavy arguments), write in Python.
    *   **Reason:** While Groovy *can* call these via the `PyObject` wrapper, translating complex Python syntax into Groovy method calls increases cognitive load and reduces code readability.

### 8.3 Architectural Advice
*   **Simple/Ad-hoc Tasks:** For quick scripts or data converters, keeping everything in a single file (regardless of language) reduces maintenance overhead.
*   **Complex Production Workflows:**
    *   **Separation of Concerns:** It is often best to separate logic. Write the high-performance core or parallel scheduling in detailed **Groovy Classes**, and write the specific mathematical modeling or visualization part in independent **Python Modules**.
    *   **Invocation:** Let the Groovy script act as the controller that prepares data, calls the specific Python function to process it, and retrieves the result.

---

## 9. Summary of API Methods

### `jse.code.SP.Python` (Static)
| Method | Description |
| :--- | :--- |
| `exec(String code)` | Executes Python code string. |
| `run(String path)` | Runs a `.py` file. |
| `set(String name, Object val)` | Sets a variable in the Python global scope. |
| `get(String name)` | Retrieves a variable wrapper (`PyObject` or primitive). |
| `parforWithInterpreter(...)` | `UT.Par.parfor` for python code. |

### `jse.code.SP.Groovy` (Static, usually used in Python)
| Method | Description |
| :--- | :--- |
| `getClass(String name)` | Returns a Groovy/Java Class object. |
| `exec(String code)` | Executes Groovy code string. |
| `run(String path)` | Runs a `.groovy` file. |
| `set(String name, Object val)` | Sets a variable in the Groovy Global Binding. |
| `get(String name)` | Retrieves a variable from the Groovy Global Binding. |
