package online.githuboy.lagou.course.utils;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;

public class HttpUtils {
    public static byte[] getContent(String url) {
        return HttpRequest.get(url).execute().bodyBytes();
    }

    public static byte[] getContentWithCookie(String url, String cookie) {
        return HttpRequest.get(url).header(Header.COOKIE, cookie).execute().bodyBytes();
    }
}
