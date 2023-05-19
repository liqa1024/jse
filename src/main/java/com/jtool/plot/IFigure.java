package com.jtool.plot;

/**
 * @author liqa
 * <p> {@link IPlotter}.show 得到的图像的对象 </p>
 * <p> 主要用于方便的管理图像等操作 </p>
 */
public interface IFigure {
    IFigure name(String aName);
    IFigure size(int aWidth, int aHeight);
    IFigure location(int aX, int aY);
}
