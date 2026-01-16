# jse - Python Interface Guide

**jse (Java Simulation Environment)** efficiently and seamlessly integrates Python via **JEP (Java Embedded Python)**. This allows users to access tools like Matplotlib (visualization), NumPy (specific array macros), or ASE io/calculators without leaving the jse environment.

## 1. Quick Start

### 1.1 Execution
jse automatically detects the language by file extension.

*   **Run a Python script:**
    ```bash
    jse script.py
    # or explicitly:
    jse --python myscript.py
    ```
*   **Python Interactive Shell:**
    ```bash
    jse --python
    ```
*   **Run a Groovy script (that uses Python):**
    ```bash
    jse script.groovy
    ```

### 1.2 Environment Management
**Recommendation:** Do not rely on internal jse package installation. logic.
1. Create a standard Conda or Venv environment.
2. Activate it (`source venv/bin/activate` or `conda activate env`).
3. Run `jse`. It will automatically compiles the necessary JNI libraries and use the active Python executable and its installed packages (NumPy, SciPy, etc.).

---

## 2. Calling Python from Groovy (The Primary Workflow)

This is the **primary recommended way** to use Python ecosystem tools (NumPy, Matplotlib, ASE) within jse.

### 2.1 The Seamless Object Wrapper (Recommended)
Instead of executing strings of code via `exec()`, use the **Seamless Object Wrapper**. This treats Python modules and objects as native Groovy objects using **dot notation**.

**Usage Pattern:**
1.  **Import** the module in the interpreter foundation.
2.  **Get** the module object into a Groovy variable.
3.  **Use** it naturally.

```groovy
import jse.code.SP

// 1. Initialize
SP.Python.exec('import numpy as np')
SP.Python.exec('import matplotlib.pyplot as plt')

// 2. Retrieve Wrappers (Use 'def' implies dynamic PyObject)
def np = SP.Python.get('np')
def plt = SP.Python.get('plt')

// 3. Use with Native Syntax
// Arguments are automatically converted (Java int/double -> Python types)
def x = np.linspace(0.0d, 10.0d, 100)
def y = np.sin(x)

plt.plot(x, y, [label: 'Sine Wave']) // Named params become kwargs
plt.savefig('plot.png')
```

### 2.2 Standard Sharing Data
For standard Python-Groovy interaction, use `set()` to pass data to Python and `get()` to retrieve results.

```groovy
import jse.code.SP

double[] rawData = [1.5d, 2.5d, 3.5d]

// Java -> Python
SP.Python.set('data', rawData)

// Execute logic (using standard Python syntax)
SP.Python.exec('result = sum(data) / len(data)')

// Python -> Java
double mean = (double)SP.Python.get('result')
```

---

## 3. Writing Python Scripts (Using jse from Python)

You can write pure Python scripts to utilize jse directly.

### 3.1 Importing Java Classes
Standard Java imports work natively. For Groovy classes, the `SP.Groovy` helper is needed in default to avoid classpath scanning overhead.

```python
# Standard Java
from java.util import ArrayList
from jse.math.vector import Vectors

# Groovy Classes
from jse.code import SP
SomeGroovyClass = SP.Groovy.getClass('my.custom.package.SomeGroovyClass')

# Usage
v = Vectors.zeros(5)
v.fill(3.14)
print(v)
```

### 3.2 Legacy Import Hook (Optional)
If you require standard Python import syntax (`from my.custom.package import SomeGroovyClass`) instead of `SP.Groovy.getClass`, set this environment variable. *Note: This may slows down startup.*

```bash
export JSE_JEP_ADD_GROOVY_HOOK=1
```

---

## 4. Data Types & Math Interoperability

### 4.1 Type Conversion Table

| Java Type | Python Type | Behavior |
| :--- | :--- | :--- |
| `boolean`, `int`, `double` | `bool`, `int`, `float` | **Auto-convert** |
| `String` | `str` | **Auto-convert** |
| `List`, `Map` | `list`, `dict` | **Wrapped** (Mirror changes) |
| `jse...IVector`, `IMatrix` | `numpy.ndarray` | **Manual Copy** (via `.numpy()`) |

### 4.2 Handling NumPy Arrays
Transferring large arrays requires a memory copy (JNI memcpy).

**Java → NumPy:**
Call `.numpy()` on the jse `IVector` / `IMatrix`.
```python
# In Python
from jse.math.vector import Vectors

j_vec = Vectors.linspace(0.0, 1.0, 1000)
np_arr = j_vec.numpy()  # Explicit copy creates ndarray
```

**NumPy → Java:**
Use factory methods.
```python
import numpy as np
from jse.math.vector import Vectors

py_arr = np.linspace(0.0, 1.0, 1000)
j_vec = Vectors.fromNumpy(py_arr) # Explicit copy creates Vector
```

---

## 5. Parallelism (`parforWithInterpreter`)

Standard Python threading or `multiprocessing` often fails within valid JNI contexts. jse provides a specialized parallel loop that manages **Thread-Local Interpreters**.

**Key Constraints for LLMs Generating Code:**
1.  Use `SP.Python.parforWithInterpreter`, not standard `UT.Par.parfor`.
2.  **Zero Python Sharing:** You cannot share `PyObject` (Python variables) between threads.
3.  **Share Java Objects:** Use thread-safe Java containers (e.g., `ConcurrentHashMap`), Manual invoke `synchronized`, or just *write in different position* to aggregate results.

```groovy
import jse.code.SP
import jse.math.vector.Vectors

int ntasks = 100
int nthreads = 4
// Normal buffer
def buf = Vectors.zeros(nthreads)

SP.Python.parforWithInterpreter(ntasks, nthreads) {i, threadID ->
    // 1. Each thread must import its own modules
    SP.Python.exec('import math')
  
    // 2. Perform calculation in isolated Python env
    SP.Python.set('val', i as double)
    SP.Python.exec('res = math.sqrt(val)')
  
    // 3. Extract primitive result to Java
    double res = (double)SP.Python.get('res')
  
    // 4. Store in Java, write in different position
    buf.add(threadID, res)
}
// Get result
double result = buf.sum()
```

> For must standard `UT.Par.parfor` usage, *write in different position* is recommend for performance. For the case of calling Python, since performance is often not critical, any simplest method can be used.

---

## 6. Jupyter Notebook & Scoping

jse provides a custom Jupyter Kernel supporting mixed cells.

1.  **Install:** `jse --jupyter`
2.  **Run:** `jupyter notebook`

To share variables between Groovy cells and `%%python` magic cells, variables must be in the **Global Binding**.

*   **Local Scope (`def`):** Variables defined with `def` (e.g., `def s = 'abc'`) are local to the cell/script. Python (and other Groovy cells) **cannot** see them.
*   **Global Binding:** Variables defined *without* a type definition are placed in the Global Binding. Python **can** see them via `SP.Groovy.get()`.

**Correct Pattern:**

**Cell 1 (Groovy):**
```groovy
// Do NOT use 'def' or 'double[]' if you want to share this variable
myData = [10.0d, 20.0d, 30.0d] as double[]
```

**Cell 2 (Python Magic):**
```python
%%python
from jse.code import SP
import numpy as np

# Retrieve from Global Binding
data = SP.Groovy.get('myData')
print(np.mean(data))
```

---

## 7. Choosing Between Groovy and Python

jse supports both, but the choice strongly affects performance and stability, and development efficiency.

### 7.1 Groovy (Recommended Default)

Use Groovy when:

* Your workflow mainly uses **jse’s own APIs** (structures, analysis, potentials, `parfor`, etc.).
* You want **IDE support** (types, static checks, initial via `jse --idea`).
* You need **heavy multithreading / parallel loops** (e.g. large trajectory analysis).
  Java’s threading model and `UT.Par.parfor` are designed for this; Python’s `multiprocessing` / `threading` are **not** reliable inside JEP.

### 7.2 Python

Use Python when:

* The core of your algorithm **must** run in NumPy / SciPy (large dense linear algebra, heavy vectorization).
* You rely on **complex Python libraries** (Matplotlib with intricate configs, ASE workflows, etc.) where calling them via Groovy + `PyObject` would hurt readability.

Keep in mind: every Java↔Python data transfer (e.g. `IVector` ↔ `numpy.ndarray`) is a **deep copy** over JNI, so you should minimize crossing the boundary.

### 7.3 Structuring Larger Projects

* For **small or ad-hoc** scripts: using a single language file is often simplest.
* For **larger workflows**:
  * put core logic, performance-critical parts and parallel scheduling in **Groovy classes**;
  * put complex Python-library usage (plots, NumPy / SciPy, ASE calculators) into separate **Python modules**, and connect them via the Python bridge.

---

## 8. Summary of API Methods

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
