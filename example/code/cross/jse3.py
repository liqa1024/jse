import numpy as np
from jse.code import IO
from jse.math.vector import Vectors

x = np.linspace(0.0, 2.0*np.pi, 20)
y = np.sin(x)

IO.cols2csv([Vectors.fromNumpy(x), Vectors.fromNumpy(y)], '.temp/example/cross/sin.csv')
