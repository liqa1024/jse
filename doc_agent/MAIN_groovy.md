# jse Groovy Scripting Style Guide

## 1. Syntax & Formatting

### 1.1 Mandatory Parentheses
Unlike standard Groovy style, **always use parentheses** for function calls. This reduces ambiguity for beginners coming from Python/C-style languages.

*   **Do:** `println('Hello World')`, `vec.add(10)`, `run_calculation()`
*   **Don't:** `println 'Hello World'`, `vec.add 10`

### 1.2 Naming Conventions
Do **not** follow the internal `jse` Java source conventions (like Hungarian notation `mVariable`, `aInput`). Use standard Java/Groovy CamelCase.
*   **Variables:** `atomType`, `boxVolume`
*   **Functions:** `calEnergy()`, `wrapPBC()`, `setSymbols(...)`
*   **Classes:** `MyAtomData`, `CustomPotential`

### 1.3 Script Structure
Leverage Groovy's scripting capabilities. Place the **main execution logic at the top** of the file and define helper functions at the bottom. This allows users to see *what* the script does immediately without scrolling.

```groovy
// Main Logic
def count = 100
runSimulation(count)

// Helper Functions (at the bottom)
void runSimulation(int n) { ... }
```

## 2. Typing & Variables

### 2.1 Type Inference (`def`) vs. Explicit Types
*   **Use `def`:** When the type is obvious, inferred from a Java return value, or is a literal.
    *   `def list = [1, 2, 3]`
    *   `def atoms = jse.atom.AtomData.builder().build()`
*   **Use Explicit Types:** When initializing empty collections or when generics matter.
    *   `List<String> names = []` (Prevents it from being `List<Object>`)
*   **Use Primitives:** Always prefer primitive types (`double`, `int`, `boolean`) over wrappers (`Double`, `Integer`, `Boolean`) for performance.

### 2.2 Numeric Types (The `d` Suffix)
To ensure high performance and avoid accidental `BigDecimal` conversion (which is slow), almost **always add the `d` suffix** to floating-point literals to force them into `double`.

*   **Standard:** `double cutoff = 3.5d`
*   **Exception:** For very simple educational examples intended for absolute beginners, `double cutoff = 3.5` is acceptable.

### 2.3 Variable Scope & Globals
Be mindful of Groovy script variable lifecycles.

*   **Script Global (Static):** If a variable needs to be accessed globally across functions within a script, use `@Field`.
    ```groovy
    import groovy.transform.Field
    @Field static double GLOBAL_CONST = 1.23d
    ```
*   **Interactive Shell / Console:** When running in the interactive shell (entering `jse` without arguments), **do not use `def`**.
    *   `a = 10` (Correct in shell, preserves state)
    *   `def a = 10` (Incorrect in shell, variable lost after execution scope)

## 3. Functions & Parameters

### 3.1 Strict Input Typing
Almost all function parameters must have explicit types. This ensures static analysis works correctly and prevents runtime errors.

*   **Good:** `void updatePosition(double dt, IXYZ velocity)`
*   **Bad:** `def updatePosition(dt, velocity)`
*   **Exception:** If the function is intentionally dynamic or polymorphic, use `def`.

## 4. Loops & Control Flow

### 4.1 Standard Loops (Readability)
For most logic, use the Groovy range style. It is concise and Python-friendly.
```groovy
for (i in 0..<100) {
    // Logic here
}
```

### 4.2 Performance Loops
For heavy computational loops (e.g., iterating over millions of atoms), use the C-style loop combined with `@CompileStatic`.
```groovy
import groovy.transform.CompileStatic

@CompileStatic
void heavyCalculation(int size) {
    for (int i = 0; i < size; ++i) {
        // High performance logic
    }
}
```

## 5. jse-Specific Best Practices

### 5.1 Prefer jse Libraries
Prioritize `jse` internal libraries over raw Java IO or Math where available:
*   **IO:** `jse.code.IO`
*   **System:** `jse.code.OS`
*   **Math:** `jse.math.vector.IVector`, `jse.math.vector.IMatrix`

### 5.2 Operators & Performance
Use Groovy operators (`[]`, `<<`) when they improve readability, but be aware of performance tradeoffs in mathematical operations.

*   **Safe/Recommended Ops:**
    *   List addition: `list << item`
    *   Map/List access: `val = map['key']`, `val = vec[i]`
    *   Vector Assignment: `vec[i] = 3.14d`

*   **Performance Warning (Read-Modify-Write):**
    *   Avoid `vec[i] += 1` on `IVector`. In Groovy, this causes double-invocations and can fail or be slow.
    *   **Alternative:** Use `vec.increment(i)`, `vec.add(i, 1)`  or just `vec[i] = vec[i] + 1`.

### 5.3 String Handling
While Groovy `GString` (`"$var"`) is powerful, it can sometimes cause issues with strict Java APIs.
*   If an API expects a strict `String` and you encounter issues, explictly cast or define it: `String filename = "data-${i}.txt"`.
*   Note: `jse` handles most of this internally, but it serves as a troubleshooting step.

## 6. Guidelines for AI Agents (Code Generation)

When generating code for users:

1.  **Explanation Strategy:**
    *   Always explain `jse`-specific shorthands (like `<<` or `0..<n`) effectively "real-time teaching" the user.
    *   If using `@CompileStatic` or `3.14d`, briefly acknowledge this is for "performance."

2.  **Complexity vs. Readability (Closures):**
    *   Avoid overly complex closures for beginner prompts.
    *   **Strategy:** If a closure (e.g., `data.collect {it * 2}`) significantly simplifies code compared to a loop, provide the closure version **but** explain how it works.
    *   For advanced logic, you may provide two versions: a "Simple/Pythonic" version (loops) and an "Idiomatic Groovy" version (closures/functional).

## Summary Example

**Bad Style:**
```groovy
// Bad naming, missing parens, loose typing, bad loop style
def ainput = 100
for (def i = 0; i < ainput; i++) {
    println "Process " + i
}
def func(a) { return a*2 }
```

**Good jse Style:**
```groovy
// Main Script Logic
int count = 100
double step = 0.5d

processSimulation(count, step)

// Helper definition at bottom
void processSimulation(int n, double dt) {
    // Range loop for readability
    for (i in 0..<n) {
        println("Processing step: ${i}")
        // Explicit types for math
        double result = calculateMetric(i, dt)
    }
}

double calculateMetric(int index, double timeStep) {
    return index * timeStep
}
```
