package jse.compat.OS;

import jse.code.OS;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

@VisibleForTesting
public final class Slurm extends OS.Slurm {
    public final static boolean IS_SLURM = OS.Slurm.IS_SLURM;
    public final static int PROCID = OS.Slurm.PROCID;
    public final static int NTASKS = OS.Slurm.NTASKS;
    public final static int CORES_PER_NODE = OS.Slurm.CORES_PER_NODE;
    public final static int CORES_PER_TASK = OS.Slurm.CORES_PER_TASK;
    public final static int MAX_STEP_COUNT = OS.Slurm.MAX_STEP_COUNT;
    public final static int JOB_ID = OS.Slurm.JOB_ID;
    public final static int NODEID = OS.Slurm.NODEID;
    public final static String NODENAME = OS.Slurm.NODENAME;
    public final static List<String> NODE_LIST = OS.Slurm.NODE_LIST;
    public final static ResourcesManager RESOURCES_MANAGER = OS.Slurm.RESOURCES_MANAGER;
}
