package com.guan.code;

import com.google.common.collect.ImmutableMap;
import com.guan.io.IHasIOFiles;
import com.guan.io.IOFiles;
import com.guan.parallel.CompletedFuture;
import com.guan.system.CompletedFutureJob;
import com.guan.system.IFutureJob;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> Class containing useful Constants </p>
 */
public class CS {
    /** a Random generator so I don't need to instantiate a new one all the time. */
    public static final Random RNGSUS = new Random(), RANDOM = RNGSUS;
    
    public static final Object NULL = null;
    
    public static final double[] BOX_ONE  = new double[] {1.0, 1.0, 1.0};
    public static final double[] BOX_ZERO = new double[] {0.0, 0.0, 0.0};
    
    /** const arrays */
    public final static Object[] ZL_OBJ = new Object[0];
    public final static double[][] ZL_MAT = new double[0][];
    public final static double[]   ZL_VEC = new double[0];
    
    /** IOFiles Keys */
    public final static String OUTPUT_FILE_KEY = "<out>", INFILE_SELF_KEY = "<self>", OFILE_KEY = "<o>", IFILE_KEY = "<i>", LMP_LOG_KEY = "<lmp>";
    
    /** SystemExecutor Stuffs */
    public final static IHasIOFiles EPT_IOF = new IOFiles();
    public final static IFutureJob ERR_FUTURE = new CompletedFutureJob(-1);
    public final static Future<List<Integer>> ERR_FUTURES = new CompletedFuture<>(Collections.singletonList(-1));
    
    
    /** Relative atomic mass in this project */
    public static final Map<String, Double> MASS = (new ImmutableMap.Builder<String, Double>())
        .put("Cu", 63.546)
        .put("Zr", 91.224)
        .build();
}
