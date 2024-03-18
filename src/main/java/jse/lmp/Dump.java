package jse.lmp;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

@VisibleForTesting
public final class Dump extends Lammpstrj {
    private Dump() {super();}
    private Dump(SubLammpstrj... aData) {super(aData);}
    private Dump(List<SubLammpstrj> aData) {super(aData);}
}
