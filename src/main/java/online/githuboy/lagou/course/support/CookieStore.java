package online.githuboy.lagou.course.support;

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
    private final static String cookie = "smidV2=20210509201800c1b7d7b654e528ed0292d4cbacca8423006c8028f05f563c0; EDUJSESSIONID=ABAAABAAAECABEH5D3BE34A894504B6BF04965E1550ACC2; thirdDeviceIdInfo=[{\"channel\":1,\"thirdDeviceId\":\"WHJMrwNw1k/ExAkdfpqOkzv76N6AbPZ+swDqMcjQ2SfxhRv8vHlYd8uQI8AtqiKnyp0XxGcYkTNZ9o97rR3Ok2pL/tY7Fd1kXdCW1tldyDzmQI99+chXEitiKiQE+axSG9lCUKKcsmkTaFO8webhNijYmmmXo8LlTkQE5YcNLqNriNYPfoOP/bo5r5oTms6dU6xLKOfWrHaeoVkUyJZv2Iu8Vg+5pExYX95+yihGCLgA/WFwcVoSmXXO9AH/2wZpLSBMZJRjFRB4=1487582755342\"},{\"channel\":2,\"thirdDeviceId\":\"140#EFFo8XrOzzPmlzo2+izu4pN8s7apeqMNbZYmE0IYHAVegyc2PUsKEYJd8oJ/8/BH4Qvflp1zz/lYngWshFzxrxkLOth/zzrb22U3lp1xzX1vV2EqlaOz2PD+Vd+F6lcI1wba7X5mYLgoO6kDnyOCbxtFVH1FOXu+EstVGAXfW0GBNaZJGbsMsD03nzc/0FfOTkbRGx01b1IwIDv1aMMvs4KDvqD6Cxyiv9iX8iPEJFmc422eQ4Bzig/NQvz80SnYxy3Nm3he0/oQVHR1lm9Wd2o7ilmGr9K9xPDKeMuwHTJUzgm/LSYPrNRa+8t/dghQeL/UsKkFia/wtmc3r2sjVm6E9yIbLVkcJ6iUL9XHpia7OAtTgiYr5HNwa2zL6ExujTE0bNLKaM504UrA8CrDPEPJLuhD1xNJc48p6DSD4k7ceNZf93ZvySxRmN+zontQRg5ax7kPWGRxuN/TZ/PcuoVB2T9uJRFl11VJMr7SzNEkiqvfQCT1TaCXk1vMvFJtKYxoIhKCltp4TYQZ8OvKQPygVZjA0hOiGhuXd/ogTr9n+K5TXC8SwJ9UXhcD4NINLDE7njd0IYlsqKSr+mfX2FVvrNFh50sjROk0GvZPJc6JrrlqYfiQZaUBjqAO0Ef/z8Hs9eJriLPe8JP0lA/YgKxpmlfF0m242y6LRbJKRkuOiKL2/TL/o5jQhD6luCsBpo+aA8FyPWX+bZdjkbk1mkt3tpOCFzW3fo5IF628lPFJRxSCQxQyrrRtvJitLHezfqDz3PCffF/WREot8Vpg5s4EZR8w8x6Q1MdkgbUxYYChmo1wPCPi8brF7+DKyyVTV1Ag+y+XwKl0Rt1mv9arw/VkHk/c4OJDoyZdkLsINihnQaYZ7Ri/haf=,undefined\"}]; sensorsdata2015session={}; user-finger=dfeea5f9b7b042289907e226aff7d081; _putrc=3CF90EDBC33870FB123F89F2B170EADC; login=true; unick=捕风的逍遥侯; kw_login_authToken=\"ZGwUdlZy7jEaBgB/tj/mC2VMxCV7RSsfRcruneVyll4fDlB4G4jVETqsOMq4i5VZN9y/AZ5OQEAXk2tF0Hfu1Cr3KylMe0a8QxAyhv3A+PU02NTzpRlTgFd8lqqWuqgs8HdXpCh+bjdB4zvT71W80pIT/Xxk7D38pf0WvL2GvbN4rucJXOpldXhUiavxhcCELWDotJ+bmNVwmAvQCptcy5e7czUcjiQC32Lco44BMYXrQ+AIOfEccJKHpj0vJ+ngq/27aqj1hWq8tEPFFjdnxMSfKgAnjbIEAX3F9CIW8BSiMHYmPBt7FDDY0CCVFICHr2dp5gQVGvhfbqg7VzvNsw==\"; gate_login_token=04fbc8019a9e58aa0d1b7a16718540460ce9fe9a707bc217e11141273d235804; sensorsdata2015jssdkcross={\"distinct_id\":\"19640064\",\"first_id\":\"1797fd3c2cb93-09c0dafe220ade-185d444b-2073600-1797fd3c2cc1059\",\"props\":{\"$latest_traffic_source_type\":\"直接流量\",\"$latest_search_keyword\":\"未取到值_直接打开\",\"$latest_referrer\":\"\",\"$os\":\"UNIX\",\"$browser\":\"Chrome\",\"$browser_version\":\"91.0.4472.38\"},\"$device_id\":\"1795110197529-00f7f8a1cd3e29-185d444b-2073600-17951101976caf\"}; X_HTTP_TOKEN=42daf4b72327b2814727431261bf5e71415983ed09";

    public static String getCookie() {
        return cookie;
    }

    public static void setCookie(String cookieStr) {
        cookie = cookieStr;
    }
}
