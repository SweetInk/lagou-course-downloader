package online.githuboy.lagou.course.decrypt.alibaba;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author suchu
 * @date 2020/8/7
 */
@Data
public class PlayAuth {
    private String AccessKeyId;
    private String AccessKeySecret;
    //@JSONField(name = "AuthInfo",deserializeUsing = online.githuboy.lagou.course.decrypt.alibaba.PlayAuth.AuthInfo.AuthInfoDeserializer.class)
    private String AuthInfo;
    private String CustomerId;
    private String PlayDomain;
    private String Region;
    private String SecurityToken;
    private AuthInfo authObj;
    private VideoMeta VideoMeta;


    @Data
    static class AuthInfo {
        private String CI;
        private String Caller;
        private String ExpireTime;
        private String MediaId;
        private String PlayDomain;
        private String Signature;

        static class AuthInfoSerializer {

        }

        static class AuthInfoDeserializer implements ObjectDeserializer {
            @Override
            public <T> T deserialze(DefaultJSONParser defaultJSONParser, Type type, Object o) {
                String strVal = defaultJSONParser.getLexer().stringVal();
                return (T) parseAuthInfo(strVal);
            }

            @Override
            public int getFastMatchToken() {
                return JSONToken.LITERAL_STRING;
            }

            private static AuthInfo parseAuthInfo(String text) {
                return JSON.parseObject(text, PlayAuth.AuthInfo.class);
            }
        }
    }

    @Data
    static class VideoMeta {
        private String CoverURL;
        private Double Duration;
        private String Status;
        private String Title;
        private String videoId;
    }

    public static PlayAuth from(String jsonText) {
        PlayAuth playAuth = JSON.parseObject(jsonText, PlayAuth.class);
        PlayAuth.AuthInfo authInfo = JSON.parseObject(playAuth.getAuthInfo(), PlayAuth.AuthInfo.class);
        playAuth.setAuthObj(authInfo);
        return playAuth;
    }
}
