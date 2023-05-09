package com.guan.code;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.Unmodifiable;

import java.awt.*;
import java.util.Map;
import java.util.Random;

/**
 * @author liqa
 * <p> Class containing useful Constants </p>
 */
public class CS {
    /** a Random generator so I don't need to instantiate a new one all the time. */
    public static final Random RNGSUS = new Random(), RANDOM = RNGSUS;
    
    public static final Object NULL = null, SEPARATOR = null, SEP = SEPARATOR;
    
    public static final double[] BOX_ONE  = new double[] {1.0, 1.0, 1.0};
    public static final double[] BOX_ZERO = new double[] {0.0, 0.0, 0.0};
    
    /** Relative atomic mass in this project */
    public static final @Unmodifiable Map<String, Double> MASS = (new ImmutableMap.Builder<String, Double>())
        .put("Cu", 63.546)
        .put("Zr", 91.224)
        .build();
    
    /** colors used for plot */
    private static final Color[] COLORS = new Color[] {
        new Color(0  , 0  , 0  ),
        new Color(255, 51 , 0  ),
        new Color(76 , 178, 0  ),
        new Color(51 , 102, 204),
        new Color(178, 0  , 178),
        new Color(204, 153, 0  ),
        new Color(0  , 178, 153),
    };
    public static final Color COLOR_NULL = new Color(255, 255, 255, 0);
    
    public static Color COLOR_(int aIdx) {return COLORS[aIdx%COLORS.length];}
    public static double[] COLOR(int aIdx) {
        Color tColor = COLOR_(aIdx);
        return new double[] {tColor.getRed()/255.0, tColor.getGreen()/255.0, tColor.getBlue()/255.0};
    }
    private static int sCIdx = 0;
    public static double[] COLOR() {return COLOR(sCIdx++);}
}
