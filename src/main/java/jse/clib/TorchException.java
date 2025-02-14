package jse.clib;

import jse.code.UT;

import java.io.IOException;

/**
 * 调用 {@link Torch} 库中相关接口可能会抛出的异常的包装，
 * 这里顺便移除了编译后抹除符号导致的无效栈信息
 * @author liqa
 */
public final class TorchException extends Exception {
    public TorchException(String aMessage) {
        super(initMsg(aMessage));
    }
    
    private static String initMsg(String aMessage) {
        // 需要修剪一下无意义的 c++ 中的栈信息
        StringBuilder rMessage = new StringBuilder();
        try {
            UT.Text.eachLine(aMessage, (line, idx) -> {
                if (idx == 0) {rMessage.append(line); return;}
                if (line.isEmpty() || UT.Text.isBlank(line)) return;
                if (line.contains("<unknown symbol address>") && line.contains("<unknown symbol>") && line.contains("<unknown file>") && line.contains("<unknown line number>")) return;
                rMessage.append('\n'); rMessage.append(line);
            });
        } catch (IOException e) {
            return aMessage;
        }
        return rMessage.toString();
    }
}
