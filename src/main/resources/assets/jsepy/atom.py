from ase.calculators.calculator import Calculator, all_changes, PropertyNotImplementedError

class PotentialCalculator(Calculator):
    implemented_properties = ['energy', 'free_energy', 'energies', 'forces', 'stress', 'stresses']
    
    def __init__(self, pot, **kwargs):
        super().__init__(**kwargs)
        self.pot = pot
        
    def release(self):
        self.pot.shutdown()
    def shutdown(self):
        self.pot.shutdown()
    
    def calculate(self, atoms=None, properties=['energy'], system_changes=all_changes):
        super().calculate(atoms, properties, system_changes)
        for p in properties:
            if p not in self.implemented_properties:
                raise PropertyNotImplementedError(p)
            if not self.pot.perAtomEnergySupport() and p=='energies':
                raise PropertyNotImplementedError(p)
            if not self.pot.perAtomStressSupport() and p=='stresses':
                raise PropertyNotImplementedError(p)
        self.results = self.pot.calculate_(self.results, self.atoms, properties, len(system_changes)>0)

