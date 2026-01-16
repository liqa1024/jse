# jse - Mathematics Library

The `jse` math library provides a high-performance, primitive-backed environment for scientific computing in Java/Groovy.

**Core Architecture Concepts:**
1.  **Strict Primitive Types:** Unlike generic collections, `jse` separates data structures by primitive type to ensure memory locality and avoid boxing overhead.
    *   `IVector` (double)
    *   `IIntVector` (int)
    *   `ILogicalVector` (boolean)
    *   `IComplexVector` (complex double)
2.  **Implementation vs. View:**
    *   **Standard Implementations** (`Vector`, `IntVector`, `RowMatrix`): Store data in contiguous arrays starting from index 0.
    *   **Views** (`ShiftVector`, `ShiftIntVector`): Lightweight wrappers around existing arrays with an offset/stride.
3.  **Storage Layout:** Matrices are explicitly defined as `RowMatrix` (Row-Major) or `ColumnMatrix` (Column-Major).

---

## 1. Vector Types & Creation

Factory methods in specific helper classes (`Vectors`, `Matrices`) are the preferred way to instantiate objects.

| Interface | Default Impl | Helper Class | NumPy Dtype |
| :--- | :--- | :--- | :--- |
| `IVector` | `Vector` | `Vectors` | `float64` |
| `IIntVector` | `IntVector` | `Vectors` | `int32` |
| `ILogicalVector` | `LogicalVector` | `Vectors` | `bool` |
| `IComplexVector` | `ComplexVector` | `Vectors` | *N/A* |

### 1.1 Creation Examples
```groovy
import jse.math.vector.Vectors

// 1. Double Vector
def v1 = Vectors.zeros(100)
def v2 = Vectors.linspace(0.0d, 1.0d, 11)

// 2. Integer Vector (0, 1, 2...9)
def vint = Vectors.range(10)

// 3. From Array/List
def v3 = Vectors.from([1.0d, 2.0d, 3.0d])
```

### 1.2 Access & Slicing (Views)
Methods like `subVec` return **Reference Views**. Modifying the view modifies the original data.

```groovy
import jse.math.vector.Vectors

def data = Vectors.linspace(0, 10, 11)

// Get a view from index 1 to 5 (exclusive end)
// shiftVec is a ShiftVector pointing to data's array
def shiftVec = data.subVec(1, 5)

// Modifying the view affects 'data'
shiftVec.set(0, 99.0d) 
assert data.get(1) == 99.0d
```

---

## 2. Matrix Types & Creation

### 2.1 Storage Formats
*   **`RowMatrix`**: Backed by a single `double[]` in row-major order. Preferred for general use.
*   **`ColumnMatrix`**: Backed by a single `double[]` in column-major order. Often returned by reference transposing a `RowMatrix`.

### 2.2 Creation Examples
```groovy
import jse.math.matrix.Matrices

// 3x3 Zeros, RowMatrix
def m1 = Matrices.zeros(3, 3)

// Identity, RowMatrix
def identity = Matrices.diag(1.0d, 1.0d, 1.0d)

// Standard Transpose (Returns View)
// rowMat.refTranspose() returns a ColumnMatrix view of the same data
def colMat = m1.refTranspose()
```

---

## 3. Operations & Performance Strategy

There are three distinct ways to perform math in `jse`. Agents must choose the strategy based on the trade-off between **readability** and **performance**.

### Tier 1: Operator Overloading (Readability)
Groovy allows operator overloading (`+`, `-`, `*`, `/`).
*   **Behavior:** Creates a **NEW** object (deep copy) for every result.
*   **Use case:** Prototypes, non-critical sections, small data.

```groovy
import jse.math.vector.Vectors

def a = Vectors.ones(1000)
def b = Vectors.ones(1000)

// Allocates new memory for (a+b), then allocates new memory for result * 2.0
def c = (a + b) * 2.0 
```

### Tier 2: In-Place Methods (Balanced)
Suffix `2this` indicates an in-place mutation.
*   **Behavior:** Modifies the object directly. Zero allocation.
*   **Use case:** Moderate performance requirements, large arrays.

```groovy
import jse.math.vector.Vectors

def a = Vectors.ones(1000)
def b = Vectors.ones(1000)

// No allocation overhead
a.plus2this(b)      // a = a + b
a.multiply2this(2.0) // a = a * 2.0
```

### Tier 3: C-Style Loops (Peak Performance)
For "kernel-level" performance (e.g., inside complex loops), bypass the object wrappers and use `@CompileStatic` with integer loops. This compiles to raw Java bytecode similar to C.

*   **Behavior:** Direct array access. Fastest possible execution on JVM.
*   **Use case:** Inner loops of heavy simulations.

```groovy
import groovy.transform.CompileStatic
import jse.math.vector.Vector

@CompileStatic
void fastCalculation(Vector vec) {
    // Access raw array (unsafe but fast)
    def data = vec.internalData()
    int size = vec.size() // or internalDataSize()
    
    // Standard C-style loop
    for (int i = 0; i < size; ++i) {
        data[i] = Math.sqrt(data[i]) + 1.0d
    }
}
```

---

## 4. Python/NumPy Interoperability

Conversion between `jse` types and NumPy is **manual** and strictly typed. Conversions perform a memory copy (memcpy).

### 4.1 Java to NumPy
Call `.numpy()` on any supported vector/matrix.
*   **Result:** A `jep.NDArray` (which JEP converts to `numpy.ndarray` in Python).
*   **Constraint:** Not supported for `ComplexVector` due to JEP limitations.

```groovy
import jse.math.vector.Vectors

def vec = Vectors.linspace(0, 1, 100)

// Explicit conversion required to send to Python context
SP.Python.set('py_vec', vec.numpy())
```

### 4.2 NumPy to Java
Use `Vectors.fromNumpy(...)` or `Matrices.fromNumpy(...)`.
*   **Result:** An `Object` (must be cast or stored in `def`).
*   **Optimization:** This reads the `jep.NDArray` from Python.

```groovy
import jse.code.SP
import jse.math.vector.Vectors
import jse.math.vector.Vector

SP.Python.exec('import numpy as np; arr = np.zeros(10)')

// Explicit conversion required to pull from Python
// The boolean 'true' enables unsigned warnings
def javaVec = (Vector)Vectors.fromNumpy(SP.Python.get('arr'), true)
```

---

## 5. Discrete Functions (`jse.math.function`)

Represents $f(x)$ discretized on a grid ($x_0, dx$). Used for potentials and distributions.

*   **Implementations:**
    *   `ConstBoundFunc1`: Extrapolates boundary values.
    *   `ZeroBoundFunc1`: Returns 0.0 outside bounds.
    *   `PBCFunc1`: Periodic boundary conditions.
    *   `UnequalIntervalFunc1`: For non-uniform grids (binary search lookup).

```groovy
import jse.math.function.Func1

// Create standard zero-bounded function
def func = Func1.zeros(0.0d, 0.1d, 100) // x0=0, dx=0.1, N=100

// Assign via lambda
func.assign {x -> Math.sin(x)}

// Interpolate
double val = func.subs(1.55d)
```

---

## 6. Static Math Utilities (`UT.Math`)

The `jse.code.UT.Math` class bridges standard `java.lang.Math` and `jse` vector operations.

```groovy
import static jse.code.UT.Math.*

double s = sin(0.5d)     // Scalar math
def v = sin(myVector)   // Returns new Vector with sin applied
```
