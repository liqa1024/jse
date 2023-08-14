package com.jtool.lmp;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

@VisibleForTesting
public final class Dump extends Lammpstrj {
    public Dump() {super();}
    public Dump(SubLammpstrj... aData) {super(aData);}
    public Dump(List<SubLammpstrj> aData) {super(aData);}
}
