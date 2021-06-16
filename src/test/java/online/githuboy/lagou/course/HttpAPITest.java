package online.githuboy.lagou.course;

import com.alibaba.fastjson.JSON;
import online.githuboy.lagou.course.domain.CourseCommentListInfo;
import online.githuboy.lagou.course.domain.CourseInfo;
import online.githuboy.lagou.course.domain.CourseLessonDetail;
import online.githuboy.lagou.course.request.HttpAPI;
import online.githuboy.lagou.course.utils.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        CourseInfo courseInfo = HttpAPI.getCourseInfo("960");
        System.out.println(JSON.toJSONString(courseInfo));
    }

    @Test
    public void getCourseLessonDetailTest() {
        CourseLessonDetail courseDetail = HttpAPI.getCourseLessonDetail("1026", "第02讲：大厂面试题：你不得不掌握的 JVM 内存管理");
        List<CourseCommentListInfo.CourseCommentList> courseCommentList = HttpAPI.getCourseCommentList("31", "1026");
        System.out.println(JSON.toJSONString(courseDetail));

        String textContent = courseDetail.getTextContent();
        if (textContent != null) {

            String commentContent = courseCommentList.stream().map(courseComment -> {
                        String text = String.format("##### %s：\n> %s\n",
                                courseComment.getNickName(), courseComment.getComment());
                        CourseCommentListInfo.CourseCommentList replayComment = courseComment.getReplayComment();
                        if (Objects.nonNull(replayComment)) {
                            text = text + String.format("\n ###### &nbsp;&nbsp;&nbsp; %s：\n> &nbsp;&nbsp;&nbsp; %s\n", replayComment.getNickName(), replayComment.getComment());
                        }
                        return text;
                    })
                    .collect(Collectors.joining("\n"));
            commentContent = "\n\n---\n\n### 精选评论\n\n" + commentContent + "\n";

            //追加精选留言类型
            textContent += commentContent;

            String textFileName = FileUtils.getCorrectFileName("第02讲：大厂面试题：你不得不掌握的 JVM 内存管理") + ".md";
            FileUtils.writeFile(new File("."), textFileName, textContent);
        }

    }

    @Test
    public void tryGetPlayUrlFromKaiwuTest() {
        String url = HttpAPI.tryGetPlayUrlFromKaiwu("84fa978fd4ad463b82092f38b3239743");
        System.out.println(JSON.toJSONString(url));
    }
}
