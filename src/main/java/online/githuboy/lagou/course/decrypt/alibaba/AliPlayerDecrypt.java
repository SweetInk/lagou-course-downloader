package online.githuboy.lagou.course.decrypt.alibaba;

import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.internal.objects.NativeString;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.UUID;

/**
 * @author suchu
 * @date 2020/8/6
 */
public class AliPlayerDecrypt {
    public static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    public static class WordCodec {
        public static String stringify(EncryptedData e) {
            StringBuilder r = new StringBuilder();
            int[] temp = e.word;
            for (int i = 0; i < e.sigBytes; i++) {
                int n = temp[i >>> 2] >>> 24 - i % 4 * 8 & 255;
                r.append(NativeString.fromCharCode(r, n));
//                r.append(Character.toString((char) (n & '\uffff')));
            }
            return r.toString();
        }
    }

    public static EncryptedData authKeyToEncryptData(String key) {
        int keyLength = key.length();
        int l = CHARS.length();
        int[] r = new int[256];
        for (int i = 0; i < l; i++) {
            r[CHARS.charAt(i)] = i;
        }
        int charCode = CHARS.charAt(64);
        int cIdx = key.indexOf(charCode);
        if (-1 != cIdx)
            keyLength = cIdx;
        int[] result = new int[keyLength * 2];
        int i = 0;
        for (int j = 0; j < keyLength; j++) {
            if (j % 4 != 0) {
                int a = r[key.charAt(j - 1)] << j % 4 * 2;
                int s = r[key.charAt(j)] >>> 6 - j % 4 * 2;
                result[i >>> 2] |= (a | s) << 24 - i % 4 * 8;
                i++;
            }
        }
        return new EncryptedData(result, i > 0 ? i : result.length * 4);
    }

    public static class EncryptedData {
        public EncryptedData(int[] word, int sigBytes) {
            this.word = word;
            this.sigBytes = sigBytes;
        }

        public int sigBytes;
        public int[] word;
    }

    public static String prettyJson(String json) {
        if (null == json || json.length() <= 0) {
            return json;
        }
        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.parseObject(json);
        } catch (Exception e) {
            return json;
        }
        return JSONObject.toJSONString(jsonObject, true);
    }

    @SneakyThrows
    public static String getPlayInfoRequestUrl(String aliPlayAuth, String fileId) {
        EncryptedData d = AliPlayerDecrypt.authKeyToEncryptData(aliPlayAuth);
        String stringify = WordCodec.stringify(d);
        PlayAuth playAuth = PlayAuth.from(stringify);
        Map<String, String> publicParam = new HashMap<>();
        Map<String, String> privateParam = new HashMap<>();
        publicParam.put("AccessKeyId", playAuth.getAccessKeyId());
        publicParam.put("Timestamp", generateTimestamp());
        publicParam.put("SignatureMethod", "HMAC-SHA1");
        publicParam.put("SignatureVersion", "1.0");
        publicParam.put("SignatureNonce", generateRandom());
        publicParam.put("Format", "JSON");
        publicParam.put("Version", "2017-03-21");

        privateParam.put("Action", "GetPlayInfo");
        privateParam.put("AuthInfo", playAuth.getAuthInfo());
        privateParam.put("AuthTimeout", "7200");
        privateParam.put("Definition", "240");
        privateParam.put("PlayConfig", "{}");
        privateParam.put("ReAuthInfo", "{}");
        privateParam.put("SecurityToken", playAuth.getSecurityToken());
        privateParam.put("VideoId", fileId);
        List<String> allParams = getAllParams(publicParam, privateParam);
        String cqs = getCQS(allParams);
        String stringToSign =
                "GET" + "&" +
                        percentEncode("/") + "&" +
                        percentEncode(cqs);
        byte[] bytes = hmacSHA1Signature(playAuth.getAccessKeySecret(), stringToSign);
        String signature = newStringByBase64(bytes);
       /* System.out.println("StringTOSign:\n" + stringToSign);
        System.out.println("\nSignature       :" + signature);
        System.out.println("\nSignatureEncoded:" + percentEncode(signature));*/
        String queryString = cqs + "&Signature=" + percentEncode(signature);
        return "https://vod.cn-shanghai.aliyuncs.com/?" + queryString;
    }

    /*特殊字符替换为转义字符*/
    public static String percentEncode(String value) {
        try {
            String urlEncodeOriginalStr = URLEncoder.encode(value, "UTF-8");
            String plusReplaced = urlEncodeOriginalStr.replace("+", "%20");
            String starReplaced = plusReplaced.replace("*", "%2A");
            return starReplaced.replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return value;
    }

    /*对所有参数名称和参数值做URL编码*/
    public static List<String> getAllParams(Map<String, String> publicParams, Map<String, String> privateParams) {
        List<String> encodeParams = new ArrayList<String>();
        if (publicParams != null) {
            for (String key : publicParams.keySet()) {
                String value = publicParams.get(key);
                //将参数和值都urlEncode一下。
                String encodeKey = percentEncode(key);
                String encodeVal = percentEncode(value);
                encodeParams.add(encodeKey + "=" + encodeVal);
            }
        }
        if (privateParams != null) {
            for (String key : privateParams.keySet()) {
                String value = privateParams.get(key);
                //将参数和值都urlEncode一下。
                String encodeKey = percentEncode(key);
                String encodeVal = percentEncode(value);
                encodeParams.add(encodeKey + "=" + encodeVal);
            }
        }
        return encodeParams;
    }

    /*获取 CanonicalizedQueryString*/
    public static String getCQS(List<String> allParams) {
        ParamsComparator paramsComparator = new ParamsComparator();
        allParams.sort(paramsComparator);
        StringBuilder cqString = new StringBuilder();
        for (int i = 0; i < allParams.size(); i++) {
            cqString.append(allParams.get(i));
            if (i != allParams.size() - 1) {
                cqString.append("&");
            }
        }
        return cqString.toString();
    }

    /*字符串参数比较器，按字母序升序*/
    public static class ParamsComparator implements Comparator<String> {
        @Override
        public int compare(String lhs, String rhs) {
            return lhs.compareTo(rhs);
        }
    }

    public static byte[] hmacSHA1Signature(String accessKeySecret, String stringToSign) {
        try {
            String key = accessKeySecret + "&";
            try {
                SecretKeySpec signKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(signKey);
                return mac.doFinal(stringToSign.getBytes());
            } catch (Exception e) {
                throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
            }
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String newStringByBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return Base64.encodeBase64String(bytes);
    }

    /*生成当前UTC时间戳Time*/
    public static String generateTimestamp() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }

    public static String generateRandom() {
        return UUID.randomUUID().toString();
    }
}
