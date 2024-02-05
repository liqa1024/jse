package jse.system;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;

@VisibleForTesting
@ApiStatus.Obsolete
public final class SLURM extends SLURMSystemExecutor {
    public SLURM(Map<?, ?> aArgs) throws Exception {super(aArgs);}
}
