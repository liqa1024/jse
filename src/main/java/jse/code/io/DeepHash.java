package jse.code.io;

import jse.code.UT;
import jse.math.MathEX;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 递归计算 hash 值算法，用于避免 {@code Object.hashCode()}
 * 过短且不稳定的问题
 * <p>
 * 部分代码由 ChatGPT 生成
 * @author liqa
 */
public class DeepHash {
    
    /** Get the unique id in Base16, 16 length */
    public static String uniqueID16(Object... aObjects) {
        try {
            MessageDigest rMd = MessageDigest.getInstance("SHA-256");
            for (Object tObj : aObjects) update(rMd, tObj);
            return Long.toHexString(UT.Serial.bytes2long(rMd.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /** 递归更新 hash 值 */
    static void update(MessageDigest rMd, Object aObj) {
        if (aObj == null) {
            rMd.update((byte)0x00);
            return;
        }
        if (aObj instanceof CharSequence) {
            rMd.update((byte)0x01);
            rMd.update(aObj.toString().getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (aObj instanceof Number) {
            rMd.update((byte)0x02);
            rMd.update(ByteBuffer.allocate(8).putDouble(((Number)aObj).doubleValue()).array());
            return;
        }
        if (aObj instanceof Boolean) {
            rMd.update((byte)0x03);
            rMd.update((byte)((Boolean)aObj?1:0));
            return;
        }
        if (aObj instanceof List) {
            rMd.update((byte)0x10);
            for (Object e : (List<?>)aObj) update(rMd, e);
            return;
        }
        if (aObj instanceof Set) {
            rMd.update((byte)0x11);
            for (Object e : (Set<?>)aObj) update(rMd, e);
            return;
        }
        if (aObj instanceof Map) {
            rMd.update((byte)0x12);
            for (Map.Entry<?, ?> e : ((Map<?, ?>)aObj).entrySet()) {
                update(rMd, e.getKey());
                update(rMd, e.getValue());
            }
            return;
        }
        if (aObj.getClass().isArray()) {
            rMd.update((byte)0x13);
            int tLen = Array.getLength(aObj);
            for (int i = 0; i < tLen; ++i) update(rMd, Array.get(aObj, i));
            return;
        }
        // fallback：只要 toString 稳定即可
        rMd.update((byte)0x7F);
        rMd.update(aObj.toString().getBytes(StandardCharsets.UTF_8));
    }
}
