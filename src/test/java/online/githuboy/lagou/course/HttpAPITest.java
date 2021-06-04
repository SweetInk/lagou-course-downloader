package online.githuboy.lagou.course;

import cn.hutool.core.io.resource.ResourceUtil;
import online.githuboy.lagou.course.domain.CourseInfo;
import online.githuboy.lagou.course.request.HttpAPI;
import online.githuboy.lagou.course.support.CookieStore;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * HttpAPITest
 *
 * @author suchu
 * @date 2021/6/4
 */
public class HttpAPITest {
    @Before
    public void setup() {
        String cookie = ResourceUtil.readStr("cookie", StandardCharsets.UTF_8);
        CookieStore.setCookie(cookie);
    }

    @Test
    public void getCourseInfoTest() {
        CourseInfo courseInfo = HttpAPI.getCourseInfo("490");
        System.out.println(courseInfo);
    }
}
