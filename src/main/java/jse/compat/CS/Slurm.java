package jse.compat.CS;

import jse.code.CS;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

@VisibleForTesting
public final class Slurm extends CS.Slurm {
    public final static boolean IS_SLURM = CS.Slurm.IS_SLURM;
    public final static int PROCID = CS.Slurm.PROCID;
    public final static int NTASKS = CS.Slurm.NTASKS;
    public final static int CORES_PER_NODE = CS.Slurm.CORES_PER_NODE;
    public final static int CORES_PER_TASK = CS.Slurm.CORES_PER_TASK;
    public final static int MAX_STEP_COUNT = CS.Slurm.MAX_STEP_COUNT;
    public final static int JOB_ID = CS.Slurm.JOB_ID;
    public final static int NODEID = CS.Slurm.NODEID;
    public final static String NODENAME = CS.Slurm.NODENAME;
    public final static List<String> NODE_LIST = CS.Slurm.NODE_LIST;
    public final static ResourcesManager RESOURCES_MANAGER = CS.Slurm.RESOURCES_MANAGER;
}
