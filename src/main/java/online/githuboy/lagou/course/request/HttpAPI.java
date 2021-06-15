package online.githuboy.lagou.course.request;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.decrypt.alibaba.AliyunApiUtils;
import online.githuboy.lagou.course.domain.AliyunVodPlayInfo;
import online.githuboy.lagou.course.domain.CourseInfo;
import online.githuboy.lagou.course.domain.CourseLessonDetail;
import online.githuboy.lagou.course.domain.PlayHistory;
import online.githuboy.lagou.course.support.CookieStore;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

/**
 * 所有http 请求接口放这里
 *
 * @author suchu
 * @date 2021/6/1
 */
@Slf4j
public class HttpAPI {
    private static final String PLAY_HISTORY_API = "https://gate.lagou.com/v1/neirong/kaiwu/getLessonPlayHistory?lessonId={0}&isVideo=true";
    private static final String PLAY_INFO_API = "https://gate.lagou.com/v1/neirong/kaiwu/getPlayInfo?lessonId=0&courseId=0&sectionId=0&vid={0}";
    private static final String COURSE_DETAIL_API = "https://gate.lagou.com/v1/neirong/kaiwu/getCourseLessonDetail?lessonId={0}";
    private final static String COURSE_INFO_API = "https://gate.lagou.com/v1/neirong/kaiwu/getCourseLessons?courseId={0}";

    public static CourseInfo getCourseInfo(String courseId) {
        String url = MessageFormat.format(COURSE_INFO_API, courseId);
        log.debug("获取课程:{}信息，url：{}", courseId, url);
        String body = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject jsonObject = JSON.parseObject(body);
        if (jsonObject.getInteger("state") != 1) throw new RuntimeException("获取课程信息失败:" + body);
        return jsonObject.getJSONObject("content").toJavaObject(CourseInfo.class);
    }

    public static PlayHistory getPlayHistory(String lessonId) {
        String url = MessageFormat.format(PLAY_HISTORY_API, lessonId);
        log.debug("获取课程历史播放信息:{}信息，url：{}", lessonId, url);
        HttpRequest httpRequest = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}");
        String body;
        try {
            body = httpRequest.execute().body();
        } catch (Exception e) {
            try {
                Thread.sleep(RandomUtil.randomLong(TimeUnit.SECONDS.toMillis(1),
                        TimeUnit.SECONDS.toMillis(2)));
            } catch (InterruptedException interruptedException) {
                log.error(interruptedException.getMessage(), interruptedException);
            }
            log.info("获取课程历史播放信息 重试1次");
            body = httpRequest.execute().body();
        }

        JSONObject jsonObject = JSON.parseObject(body);
        if (jsonObject.getInteger("state") != 1) throw new RuntimeException("获取课程信息失败:" + body);
        return jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").toJavaObject(PlayHistory.class);
    }

    public static AliyunVodPlayInfo getVodPlayerInfo(String rand, String playAuth, String fileId) {
        String playInfoRequestUrl = AliyunApiUtils.getPlayInfoRequestUrl(rand, playAuth, fileId);
        String response = HttpRequest.get(playInfoRequestUrl).execute().body();
        log.debug("\nAliyun API request result:\n\n" + response);
        JSONObject mediaObj = JSON.parseObject(response);
        if (mediaObj.getString("Code") != null) throw new RuntimeException("获取媒体信息失败:");
        JSONObject playInfoList = mediaObj.getJSONObject("PlayInfoList");
        JSONArray playInfos = playInfoList.getJSONArray("PlayInfo");
        if (playInfos != null && playInfos.size() > 0) {
            JSONObject playInfo = playInfos.getJSONObject(0);
            return playInfo.toJavaObject(AliyunVodPlayInfo.class);
        }
        return null;
    }

    public static CourseLessonDetail getCourseLessonDetail(String courseId, String courseName) {
        String url = MessageFormat.format(COURSE_DETAIL_API, courseId);
        log.debug("获取课程详情URL:【{}】url：{}", courseId, url);
        String videoJson = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject json = JSON.parseObject(videoJson);
        Integer state = json.getInteger("state");
        if (state != null && state != 1) {
            log.info("获取视频视频信息失败:【{}】,json：{}", courseName, videoJson);
            throw new RuntimeException("获取视频信息失败:" + json.getString("message"));
        }
        JSONObject result = json.getJSONObject("content");
        return result.toJavaObject(CourseLessonDetail.class);
    }

    public static String tryGetPlayUrlFromKaiwu(String fileId) {
        String url = MessageFormat.format(PLAY_INFO_API, fileId);
        String body = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject jsonObject = JSON.parseObject(body);
        if (jsonObject.getInteger("state") != 1) {
            log.error("获取mp4播放地址失败:{}", body);
            return null;
        }
        return jsonObject.getJSONObject("content").getString("playURL");
    }
}
