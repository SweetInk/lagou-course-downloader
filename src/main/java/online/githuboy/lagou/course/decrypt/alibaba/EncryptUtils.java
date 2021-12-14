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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

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
    private final static int[] PLAY_AUTH_SIGN1 = new int[]{52, 58, 53, 121, 116, 102};
    private final static int[] PLAY_AUTH_SIGN2 = new int[]{90, 91};

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

    public static String decodeSignedPlayAuth2B64(String playAuth) {
        String sign1 = _getSignStr(PLAY_AUTH_SIGN1);
        String sign2 = _getSignStr(PLAY_AUTH_SIGN2);
        playAuth = playAuth
                .replaceFirst(sign1, "");
        playAuth = playAuth
                .substring(0, playAuth.length() - sign2.length());
        int factor = Calendar.getInstance().get(Calendar.YEAR) / 100;
        List<Integer> newCharCodeList = new ArrayList<>();
        for (int i = 0; i < playAuth.length(); i++) {
            int code = playAuth.charAt(i);
            int r = code / factor;
            int z = factor / 10;
            newCharCodeList.add(r == z ? code : code - 1);
        }
        return newCharCodeList.stream().map(charCode -> {
            char v = (char) charCode.intValue();
            return v + "";
        }).collect(Collectors.joining());
    }

    public static String decodePlayAuth(String playAuth) {
        if (isSignedPlayAuth(playAuth)) {
            String playAuthBase64 = decodeSignedPlayAuth2B64(playAuth);
            return Base64.decodeStr(playAuthBase64);
        } else {
            return Base64.decodeStr(playAuth);
        }
    }

    private static boolean isSignedPlayAuth(String str) {
        int signPos1 = Calendar.getInstance().get(Calendar.YEAR) / 100;
        int signPos2 = str.length() - 2;
        String sign1 = _getSignStr(PLAY_AUTH_SIGN1);
        String sign2 = _getSignStr(PLAY_AUTH_SIGN2);
        String r1 = str.substring(signPos1, signPos1 + sign1.length());
        String r2 = str.substring(signPos2);
        return sign1.equals(r1) && sign2.equals(r2);
    }

    public static String _getSignStr(int[] sign) {
        StringBuilder result = new StringBuilder(sign.length);
        for (int i = 0; i < sign.length; i++) {
            result.append((char) (sign[i] - i));
        }
        return result.toString();
    }
}
