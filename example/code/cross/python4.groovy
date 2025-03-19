package code.cross

import jse.code.SP
import static jse.code.UT.Math.*

def x = linspace(0.0, 2.0*pi, 20)
def y = sin(x)

SP.Python.exec('import matplotlib.pyplot')
def plt = SP.Python.getClass('matplotlib.pyplot')

plt.figure(figsize: [4, 3])
plt.plot(x.numpy(), y.numpy())
plt.show()


//OUTPUT:
// sys:1: UserWarning: Starting a Matplotlib GUI outside of the main thread will likely fail.

