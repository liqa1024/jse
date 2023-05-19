package com.jtool.plot;


import java.awt.*;

import static java.awt.Color.*;

/**
 * @author liqa
 * <p> {@link java.awt.Color} 中的一些用到的实例 </p>
 * <p> 目前用于 plot 的使用 </p>
 */
public class Colors {
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
    
    
    public static Color toColor(String aColor) {
        switch (aColor) {
        case "w": case "white": case "WHITE":
            return WHITE;
        case "lightGray": case "LIGHT_GRAY":
            return LIGHT_GRAY;
        case "gray": case "GRAY":
            return GRAY;
        case "darkGray": case "DARK_GRAY":
            return DARK_GRAY;
        case "k": case "black": case "BLACK":
            return BLACK;
        case "r": case "red": case "RED":
            return RED;
        case "pink": case "PINK":
            return PINK;
        case "orange": case "ORANGE":
            return ORANGE;
        case "y": case "yellow": case "YELLOW":
            return YELLOW;
        case "g": case "green": case "GREEN":
            return GREEN;
        case "m": case "magenta": case "MAGENTA":
            return MAGENTA;
        case "c": case "cyan": case "CYAN":
            return CYAN;
        case "b": case "blue": case "BLUE":
            return BLUE;
        case "none": case "null": case "NULL":
            return COLOR_NULL;
        default:
            return DEFAULT_COLOR;
        }
    }
    
    /** 全局常量记录默认值 */
    public final static Color DEFAULT_COLOR = COLOR_(0);
}
