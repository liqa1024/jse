package jtool.io;

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
public class Encryptor {
    private final Cipher mCipher;
    
    public Encryptor(String aKey                                           ) throws Exception {this(aKey.getBytes(StandardCharsets.UTF_8));}
    public Encryptor(String aKey, String aAlgorithm                        ) throws Exception {this(aKey.getBytes(StandardCharsets.UTF_8), aAlgorithm);}
    public Encryptor(String aKey, String aAlgorithm, String aTransformation) throws Exception {this(aKey.getBytes(StandardCharsets.UTF_8), aAlgorithm, aTransformation);}
    public Encryptor(byte[] aKey                                           ) throws Exception {this(aKey, "AES");}
    public Encryptor(byte[] aKey, String aAlgorithm                        ) throws Exception {this(aKey, aAlgorithm, "AES/CBC/PKCS5Padding");}
    public Encryptor(byte[] aKey, String aAlgorithm, String aTransformation) throws Exception {
        // 使用 key 的 hash 值来获取合法的 key
        SecretKeySpec tKeySpec = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(aKey), aAlgorithm);
        this.mCipher = Cipher.getInstance(aTransformation);
        byte[] tIV = "1234567890ABCDEF".getBytes(StandardCharsets.UTF_8);
        
        mCipher.init(Cipher.ENCRYPT_MODE, tKeySpec, new IvParameterSpec(tIV));
    }
    
    // 输入正常的 string 转换为 byte
    public byte[] getData(String aStr)  throws IllegalBlockSizeException, BadPaddingException {return getData(aStr.getBytes(StandardCharsets.UTF_8));}
    public byte[] getData(byte[] aData) throws IllegalBlockSizeException, BadPaddingException {return mCipher.doFinal(aData);}
    
    // 将 byte 转换成 Base64 字符串
    public String get(String aStr)  throws IllegalBlockSizeException, BadPaddingException {return Base64.getEncoder().encodeToString(getData(aStr));}
    public String get(byte[] aData) throws IllegalBlockSizeException, BadPaddingException {return Base64.getEncoder().encodeToString(getData(aData));}
}
