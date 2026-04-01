package jse.code;

import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;

/**
 * 专门针对读取时文件已经结束时的异常，目前主要为 {@link jse.atom.IAtomData}
 * 相关读取提供，用来和常规异常区分从而方便 dump 中的多帧读取实现，并可以附加额外信息。
 * @author liqa
 */
@ApiStatus.Experimental
public class FileEndException extends IOException {
    public FileEndException() {super();}
    public FileEndException(String aMsg) {super(aMsg);}
}
