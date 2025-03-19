from jse.lmp import Lmpdat

data = Lmpdat.read('lmp/data/CuFCC108.lmpdat')
dataNp = data.numpy()
print(dataNp.shape)
print(dataNp)
