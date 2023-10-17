package jtool.iofile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * @author liqa
 * 将 String 或者数据进行加密或者解密的类
 */
@Deprecated
public class Decryptor {
    private final Cipher mCipher;
    
    public Decryptor(String aKey                                           ) throws Exception {this(aKey.getBytes(StandardCharsets.UTF_8));}
    public Decryptor(String aKey, String aAlgorithm                        ) throws Exception {this(aKey.getBytes(StandardCharsets.UTF_8), aAlgorithm);}
    public Decryptor(String aKey, String aAlgorithm, String aTransformation) throws Exception {this(aKey.getBytes(StandardCharsets.UTF_8), aAlgorithm, aTransformation);}
    public Decryptor(byte[] aKey                                           ) throws Exception {this(aKey, "AES");}
    public Decryptor(byte[] aKey, String aAlgorithm                        ) throws Exception {this(aKey, aAlgorithm, "AES/CBC/PKCS5Padding");}
    public Decryptor(byte[] aKey, String aAlgorithm, String aTransformation) throws Exception {
        // 使用 key 的 hash 值来获取合法的 key
        SecretKeySpec tKeySpec = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(aKey), aAlgorithm);
        this.mCipher = Cipher.getInstance(aTransformation);
        byte[] tIV = "1234567890ABCDEF".getBytes(StandardCharsets.UTF_8);
        
        mCipher.init(Cipher.DECRYPT_MODE, tKeySpec, new IvParameterSpec(tIV));
    }
    
    // 将 Base64 字符串转换成 byte[]
    public byte[] getData(String aStr)  throws IllegalBlockSizeException, BadPaddingException {return getData(Base64.getDecoder().decode(aStr));}
    public byte[] getData(byte[] aData) throws IllegalBlockSizeException, BadPaddingException {return mCipher.doFinal(aData);}
    
    // 将 byte[] 编码成 String
    public String get(String aStr)  throws IllegalBlockSizeException, BadPaddingException {return new String(getData(aStr),  StandardCharsets.UTF_8);}
    public String get(byte[] aData) throws IllegalBlockSizeException, BadPaddingException {return new String(getData(aData), StandardCharsets.UTF_8);}
}
