package jse.plot;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @author liqa
 * <p> {@link IPlotter#show()} 得到的图像的对象 </p>
 * <p> 主要用于方便的管理图像等操作 </p>
 */
public interface IFigure {
    boolean isShowing();
    void dispose();
    
    IFigure name(String aName);
    IFigure size(int aWidth, int aHeight);
    IFigure location(int aX, int aY);
    
    IFigure insets(double aTop, double aLeft, double aBottom, double aRight);
    IFigure insetsTop(double aTop);
    IFigure insetsLeft(double aLeft);
    IFigure insetsBottom(double aBottom);
    IFigure insetsRight(double aRight);
    
    void save(@Nullable String aFilePath, int aWidth, int aHeight) throws IOException;
    void save(@Nullable String aFilePath) throws IOException;
    default void save() throws IOException {save(null);}
}
