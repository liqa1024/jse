package com.guan.code;

import com.google.common.collect.ImmutableMapBuilder;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Random;

/**
 * @author liqa
 * <p> Class containing useful Constants </p>
 */
public class CS {
    /** a Random generator so I don't need to instantiate a new one all the time. */
    public static final Random RNGSUS = new Random(), RANDOM = RNGSUS;
    
    public static final Object NULL = null, SEPARATOR = null, SP = SEPARATOR;
    
    public static final double[] BOX_ONE  = new double[] {1.0, 1.0, 1.0};
    public static final double[] BOX_ZERO = new double[] {0.0, 0.0, 0.0};
    
    /** Relative atomic mass in this project */
    public static final @Unmodifiable Map<String, Double> MASS = (new ImmutableMapBuilder<String, Double>())
        .put("Cu", 63.546)
        .put("Zr", 91.224)
        .getMap();
}
