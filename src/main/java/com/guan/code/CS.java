package com.guan.code;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Random;

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
    
    /** Relative atomic mass in this project */
    public static final @Unmodifiable Map<String, Double> MASS = (new ImmutableMap.Builder<String, Double>())
        .put("Cu", 63.546)
        .put("Zr", 91.224)
        .build();
}
