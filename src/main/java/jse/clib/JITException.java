package jse.clib;

public final class JITException extends Exception {
    public JITException(String aMessage) {
        super(aMessage);
    }
    public JITException(int aErrCode, String aMessage) {
        super(aMessage+", error=" + aErrCode);
    }
}
