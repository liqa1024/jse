package jse.lmp;

public final class LmpException extends Exception {
    public LmpException(String aMessage) {
        super(initMsg(aMessage));
    }
    private static String initMsg(String aMessage) {
        // 移除信息最后的换行符号
        if (aMessage.endsWith("\n")) return aMessage.substring(0, aMessage.length()-1);
        return aMessage;
    }
}
