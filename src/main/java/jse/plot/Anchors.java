package jse.plot;

public class Anchors {
    /** Anchor stuffs */
    public enum AnchorType {
          NULL
        , CENTER
        , TOP
        , TOP_LEFT
        , TOP_RIGHT
        , BOTTOM
        , BOTTOM_LEFT
        , BOTTOM_RIGHT
        , LEFT
        , RIGHT
    }
    public static AnchorType toAnchorType(String aAnchorType) {
        switch (aAnchorType) {
        case "c": case "center": case "centre": case "CENTER":
            return AnchorType.CENTER;
        case "t": case "top": case "n": case "north":case "TOP":
            return AnchorType.TOP;
        case "tl": case "topleft": case "nw": case "northwest": case "TOP_LEFT":
            return AnchorType.TOP_LEFT;
        case "tr": case "topright": case "ne": case "northeast": case "TOP_RIGHT":
            return AnchorType.TOP_RIGHT;
        case "b": case "bottom": case "s": case "south":case "BOTTOM":
            return AnchorType.BOTTOM;
        case "bl": case "bottomleft": case "bw": case "southwest": case "BOTTOM_LEFT":
            return AnchorType.BOTTOM_LEFT;
        case "br": case "bottomright": case "be": case "southeast": case "BOTTOM_RIGHT":
            return AnchorType.BOTTOM_RIGHT;
        case "l": case "left": case "w": case "west":case "LEFT":
            return AnchorType.LEFT;
        case "r": case "right": case "e": case "east":case "RIGHT":
            return AnchorType.RIGHT;
        case "none": case "null": case "NULL":
            return AnchorType.NULL;
        default:
            throw new IllegalArgumentException(aAnchorType);
        }
    }
    
    /** 全局常量记录默认值 */
    public final static AnchorType DEFAULT_ANCHOR_TYPE = AnchorType.NULL;
}
