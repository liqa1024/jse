from jse.lmp import Lmpdat

data = Lmpdat.read('lmp/data/data-glass')
print(type(data))
print('natoms:', data.natoms())
