package jtoolex.rareevent.atom;

import jtool.atom.MonatomicParameterCalculator;
import jtool.math.vector.ILogicalVector;

@FunctionalInterface
public interface ISolidChecker {ILogicalVector checkSolid(MonatomicParameterCalculator aMPC);}
