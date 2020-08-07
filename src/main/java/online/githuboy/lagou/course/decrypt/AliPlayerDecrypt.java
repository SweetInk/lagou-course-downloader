package online.githuboy.lagou.course.decrypt;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.internal.objects.NativeString;
import online.githuboy.lagou.course.CookieStore;
import online.githuboy.lagou.course.utils.HttpUtils;
import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author suchu
 * @date 2020/8/6
 */
public class AliPlayerDecrypt {

    public static class WordCodec {
        public static String stringify(EncryptedData e) {
            StringBuilder r = new StringBuilder();
            int[] temp = e.word;
            for (int i = 0; i < e.sigBytes; i++) {
                int n = temp[i >>> 2] >>> 24 - i % 4 * 8 & 255;
                r.append(NativeString.fromCharCode(r, n));
            }
            return r.toString();
        }
    }

    public static EncryptedData authKeyToEncryptData(String key) {
        int keyLength = key.length();
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        int l = str.length();
        int[] r = new int[256];
        for (int i = 0; i < l; i++) {
            r[str.charAt(i)] = i;
        }
        int charCode = str.charAt(64);
        {
            int cIdx = key.indexOf(charCode);
            if (-1 != cIdx)
                keyLength = cIdx;
        }
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

    public static void main(String[] args) throws UnsupportedEncodingException {

        String body = HttpUtils.get("https://gate.lagou.com/v1/neirong/kaiwu/getLessonPlayHistory?lessonId=300&isVideo=true", CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject jsonObject = JSON.parseObject(body);
        System.out.println(body);
        if (jsonObject.getInteger("state") != 1) throw new RuntimeException(body);
        String aliPlayAuth = jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").getString("aliPlayAuth");
        EncryptedData d = AliPlayerDecrypt.authKeyToEncryptData(aliPlayAuth);
        String stringify = WordCodec.stringify(d);
        PlayAuth playAuth = PlayAuth.from(stringify);
        //playAuth.setAuthInfo("{\"CI\":\"whrsIqgklOAeOzIO2Je2QiRLI0GFb67v3dBjWhuO+E88YmRX5KcF4K3+lXLJWBQXZvBuAbvf/aSE\\r\\nPpafNqvWwD2/6Kk4uKMCqlT1WDxdHY0=\\r\\n\",\"Caller\":\"bAH/mdSd46DoiXGEigibc+uyl8KQ9iGhImfyS9ksgRc=\\r\\n\",\"ExpireTime\":\"2020-08-07T05:00:48Z\",\"MediaId\":\"218edfbc8849496ab579d1d76e5cb135\",\"PlayDomain\":\"vod.lagou.com\",\"Signature\":\"mbut5a8K2CQWVxHiWgO6LikK/U4=\"}");
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
        privateParam.put("VideoId", "218edfbc8849496ab579d1d76e5cb135");
        List<String> allParams = getAllParams(publicParam, privateParam);
        String cqs = getCQS(allParams);
        String stringToSign =
                "GET" + "&" +
                        percentEncode("/") + "&" +
                        percentEncode(cqs);
        byte[] bytes = hmacSHA1Signature(playAuth.getAccessKeySecret(), stringToSign);
        String signature = newStringByBase64(bytes);
        String queryString = cqs + "&Signature=" + signature;
        String api = "https://vod.cn-shanghai.aliyuncs.com/?" + queryString;
        String body1 = HttpRequest.get(api).execute().body();

        System.out.println(stringToSign);
        System.out.println(api);
        System.out.println(stringify);
        System.out.println("\n\nAPI request result:\n"+body1);
    }

    /*特殊字符替换为转义字符*/
    public static String percentEncode(String value) {
        try {
            String urlEncodeOrignStr = URLEncoder.encode(value, "UTF-8");
            String plusReplaced = urlEncodeOrignStr.replace("+", "%20");
            String starReplaced = plusReplaced.replace("*", "%2A");
            String waveReplaced = starReplaced.replace("%7E", "~");
            return waveReplaced;
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
        Collections.sort(allParams, paramsComparator);
        String cqString = "";
        for (int i = 0; i < allParams.size(); i++) {
            cqString += allParams.get(i);
            if (i != allParams.size() - 1) {
                cqString += "&";
            }
        }
        return cqString;
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

    public static String newStringByBase64(byte[] bytes)
            throws UnsupportedEncodingException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new String(new BASE64Encoder().encode(bytes));
    }

    /*生成当前UTC时间戳Time*/
    public static String generateTimestamp() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }

    public static String generateRandom() {
        String signatureNonce = UUID.randomUUID().toString();
        return signatureNonce;
    }
}
