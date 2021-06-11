package online.githuboy.lagou.course;

import com.alibaba.fastjson.JSON;
import online.githuboy.lagou.course.domain.CourseInfo;
import online.githuboy.lagou.course.domain.CourseLessonDetail;
import online.githuboy.lagou.course.request.HttpAPI;
import org.junit.Before;
import org.junit.Test;

/**
 * HttpAPITest
 *
 * @author suchu
 * @date 2021/6/4
 */
public class HttpAPITest {
    @Before
    public void setup() {
//        String cookie = ResourceUtil.readStr("cookie", StandardCharsets.UTF_8);
//        CookieStore.setCookie(cookie);
    }

    @Test
    public void getCourseInfoTest() {
        CourseInfo courseInfo = HttpAPI.getCourseInfo("490");
        System.out.println(JSON.toJSONString(courseInfo));
    }

    @Test
    public void getCourseLessonDetailTest() {
        CourseLessonDetail courseDetail = HttpAPI.getCourseLessonDetail("4704", "04 | 如何利用 Repository 中的方法返回值解决实际问题？");
        System.out.println(JSON.toJSONString(courseDetail));
    }
}
