package online.githuboy.lagou.course.support;

import online.githuboy.lagou.course.utils.ConfigUtil;

/**
 * CookieStore
 *
 * @author suchu
 * @since 2019年8月2日
 */
public class CookieStore {
    /**
     * 这里填写登入成功后，拉勾网的cookie
     * 开发时可以把cookie这里，提交时别把cookie提交上来
     * https://kaiwu.lagou.com/
     */
    private static String cookie = ConfigUtil.readValue("cookie");
    public static String getCookie() {
        return cookie;
    }

    public static void setCookie(String cookieStr) {
        cookie = cookieStr;
    }
}
