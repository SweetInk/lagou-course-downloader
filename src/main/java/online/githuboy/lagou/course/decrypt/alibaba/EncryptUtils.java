package online.githuboy.lagou.course.decrypt.alibaba;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;

/**
 * 阿里云点播私有加密工具
 *
 * @author suchu
 * @date 2021/5/13
 */
public class EncryptUtils {
    public static final RSA rsa = new RSA();
    private final static String PUBLIC_KEY_STR = "-----BEGIN PUBLIC KEY-----\n" +
            "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIcLeIt2wmIyXckgNhCGpMTAZyBGO+nk0/IdOrhIdfRR\n" +
            "gBLHdydsftMVPNHrRuPKQNZRslWE1vvgx80w9lCllIUCAwEAAQ==\n" +
            "-----END PUBLIC KEY-----\n";
    private static final PublicKey publicKey = PemUtil.readPemPublicKey(new ByteArrayInputStream(PUBLIC_KEY_STR.getBytes()));

    static {
        rsa.setPublicKey(publicKey);
    }


    /**
     * 解密视频的AES key
     * <p>
     * * r0 = cmMeyfzJWyZcSwyH //内存随机数
     * * r1 = md5(内存随机数)
     * * r1 = r1.substring(8,24)
     * * iv = base64(r1).getBytes();
     * * key1 = aes.decrypt(rnd,iv,iv)
     * * seed1 = md5(r0+key1)
     * * seed1 = seed1.substring(8,24)
     * * k2 = base64(seed1).getBytes();
     * * key2 = aes.decrypt(plain,k2,iv);
     * * result = hex.encodeHexStr(base64.decode(key2))
     *
     * @param r1    初始随机数
     * @param rand  服务端返回的rand
     * @param plain 服务端返回的plainText
     * @return
     */
    public static String decrypt(String r1, String rand, String plain) {
        String r1MD5 = SecureUtil.md5(r1);
        String tempKey = r1MD5.substring(8, 24);
        byte[] iv = convert(tempKey);
        AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, iv, iv);
        String randDecrypted = aes.decryptStr(rand);
        String r2 = r1 + randDecrypted;
        String r2MD5 = SecureUtil.md5(r2);
        String tempKey2 = r2MD5.substring(8, 24);
        byte[] key2 = convert(tempKey2);
        AES aes2 = new AES(Mode.CBC, Padding.PKCS5Padding, key2, iv);
        String finalKey = aes2.decryptStr(plain);
        return HexUtil.encodeHexStr(Base64.decode(finalKey));
    }

    public static String encryptRand(String rand) {
        return rsa.encryptBase64(rand, KeyType.PublicKey);
    }

    public static byte[] convert(String str) {
        return Base64.decode(Base64.encode(str));
    }
}
