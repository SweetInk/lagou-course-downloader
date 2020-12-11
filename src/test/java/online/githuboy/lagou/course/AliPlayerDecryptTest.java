package online.githuboy.lagou.course;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import online.githuboy.lagou.course.support.CookieStore;
import online.githuboy.lagou.course.utils.HttpUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static online.githuboy.lagou.course.decrypt.alibaba.AliPlayerDecrypt.*;

/**
 * @author suchu
 * @date 2020/12/11
 */
public class AliPlayerDecryptTest {
    @Test
    @SneakyThrows
    public void testSign() {
        String str = "AccessKeyId:STS.NTbUvqcykS6HszcMdH514JWYz\n" +
                "Action:GetPlayInfo\n" +
                "AuthTimeout:7200\n" +
                "Channel:HTML5\n" +
                "Definition:240\n" +
                "Format:JSON\n" +
                "Formats:mp4\n" +
                "PlayConfig:{}\n" +
                "PlayerVersion:2.9.0\n" +
                "Rand:a43877ce-55da-418f-be18-8cd27a49152c\n" +
                "ReAuthInfo:{}\n" +
                "SignatureMethod:HMAC-SHA1\n" +
                "SignatureNonce:91510923-3653-4a75-9420-c862fcd5893c\n" +
                "SignatureVersion:1.0\n" +
                "StreamType:video\n" +
                "Version:2017-03-21\n" +
                "VideoId:dfa387a5de484840bc6d35d55966b05d";
        Map<String, String> collect = Arrays.stream(str.split("\n")).map(a -> a.split(":")).collect(Collectors.toMap(a -> a[0], a -> a[1]));
        collect.put("AuthInfo", "{\"CI\":\"89eDkFRoH7DE9JSHMKOoG0wMLD3INudVmkSQlfWzPTjj92bemTlISO4dx4CmSuCPZ3+Qfs1m3xMBzxxK8QxwEnz1kIj/O2d64lurRZyuY0c=\",\"Caller\":\"jonndG2Rj+h7u/FVWyR+2zQFGMOD2QOn8/L0WgJqGOo=\",\"ExpireTime\":\"2020-12-11T03:39:03Z\",\"MediaId\":\"dfa387a5de484840bc6d35d55966b05d\",\"PlayDomain\":\"vod.lagou.com\",\"Signature\":\"G5nPuYsdgh46XecQTJf6thPfLZc=\"}");
        collect.put("SecurityToken", "CAIShwN1q6Ft5B2yfSjIr5fXHszFjqZK5PSjcVzSqWQdOb4YpZLymDz2IH9IdHVoAO8fvvU0m2tY7PsZlrMqFcYVHBeVPJUsssgHrF/xJpLFst2J6r8JjsUUtrx7hlipsvXJasDVEfl2E5XEMiIR/00e6L/+cirYpTXHVbSClZ9gaPkOQwC8dkAoLdxKJwxk2t14UmXWOaSCPwLShmPBLUxmvWgGl2Rzu4uy3vOd5hfZp1r8xO4axeL0PoP2V81lLZplesqp3I4Sc7baghZU4glr8qlx7spB5SyVktyWGUhJ/zaLIoit7NpjfiB0eoQAPopFp/X6jvAawPLUm9bYxgphB8R+Xj7DZYaux7GzeoWTO80+aKzwNlnUz9mLLeOViQ4/Zm8BPw44ELhIaF0IUE1yGmCCd/X4oguRP1z7EpLoiv9mjcBHqHzz5sePKlS1RLGU7D0VIJdUbTlzaEJGgTS4LfZWIlcTKAM9Wu2PMax3bQFDr53vsTbbXzZb0mptuPnzd14JOBKg11KUGoABPq2AZ0mHqZHNPJemu+iUgfS4Pws7iZmxnDay6U1A7XjX0WPBiwP4qgw2RFfIBhJcziJ9+utgVpqynWfX+v25yhKfofMFooFiLRb1t3KxUY94tqaEbGSzkzuAwGDavMW95YStqU1jXl+T7yyawjwxyDm3wsxcz6G0UjdsITbiStM=");
        List<String> allParams = getAllParams(collect, null);
        String cqs = getCQS(allParams);
        String stringToSign =
                "GET" + "&" +
                        percentEncode("/") + "&" +
                        percentEncode(cqs);
        byte[] bytes = hmacSHA1Signature("BiCJzqCgoSbpZDYdsgnAnANSNXnswGNXpSqrjuL1XQoL", stringToSign);
        String s = newStringByBase64(bytes);
        System.out.println(1);
    }


    @Test
    @SneakyThrows
    public void testFullRequest() {
        String body = HttpUtils.get("https://gate.lagou.com/v1/neirong/kaiwu/getLessonPlayHistory?lessonId=4701&isVideo=true", CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject jsonObject = JSON.parseObject(body);
//        System.out.println(body);
        if (jsonObject.getInteger("state") != 1) throw new RuntimeException(body);
        String aliPlayAuth = jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").getString("aliPlayAuth");
        String fileId = jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").getString("fileId");
        String api = getPlayInfoRequestUrl(aliPlayAuth, fileId);
        String body1 = HttpRequest.get(api).execute().body();
//        System.out.println(api);
        System.out.println("\n\nAPI request result:\n" + body1);
    }
}