package jse.lmp;

import jse.math.table.ITable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

@VisibleForTesting
public final class MultiLog extends MultiThermo {
    private MultiLog(ITable... aTableList) {super(aTableList);}
    private MultiLog(List<ITable> aTableList) {super(aTableList);}
}
