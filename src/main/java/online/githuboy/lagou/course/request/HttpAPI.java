package online.githuboy.lagou.course.request;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.decrypt.alibaba.AliyunApiUtils;
import online.githuboy.lagou.course.domain.*;
import online.githuboy.lagou.course.support.CookieStore;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
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
    private final static String COURSE_COMMENT_LIST_API = "https://gate.lagou.com/v1/neirong/course/comment/getCourseCommentList?courseId={0}&lessonId={1}&pageNum={2}&needCount=true";
    private final static String PURCHASED_COURSE_API = "https://gate.lagou.com/v1/neirong/kaiwu/getAllCoursePurchasedRecordForPC?t={0}";

    public static CourseInfo getCourseInfo(String courseId) {
        String url = MessageFormat.format(COURSE_INFO_API, courseId);
        log.debug("获取课程:{}信息，url：{}", courseId, url);
        HttpRequest httpRequest = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}");

        String body;
        try {
            body = httpRequest.execute().body();
        } catch (Exception e) {
            try {
                Thread.sleep(RandomUtil.randomLong(500L,
                        TimeUnit.SECONDS.toMillis(1)));
            } catch (InterruptedException interruptedException) {
                log.error(interruptedException.getMessage(), interruptedException);
            }
            log.info("获取课程 重试1次");
            body = httpRequest.execute().body();
        }

        JSONObject jsonObject = JSON.parseObject(body);
        if (jsonObject.getInteger("state") != 1) throw new RuntimeException("获取课程信息失败:" + body);
        return jsonObject.getJSONObject("content").toJavaObject(CourseInfo.class);
    }

    /**
     * 获取精选留言
     *
     * @param courseId
     * @param lessonId
     * @return
     */
    public static List<CourseCommentListInfo.CourseCommentList> getCourseCommentList(String courseId, String lessonId) {
        List<CourseCommentListInfo.CourseCommentList> list = new ArrayList<>();
        boolean hasNextPage = true;
        int i = 1;
        while (hasNextPage) {
            String url = MessageFormat.format(COURSE_COMMENT_LIST_API, courseId, lessonId, i++);
            log.debug("获取课程精选留言:{}信息，url：{}", lessonId, url);
            HttpRequest httpRequest = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}");

            String body;
            try {
                body = httpRequest.execute().body();
            } catch (Exception e) {
                try {
                    Thread.sleep(RandomUtil.randomLong(500L,
                            TimeUnit.SECONDS.toMillis(1)));
                } catch (InterruptedException interruptedException) {
                    log.error(interruptedException.getMessage(), interruptedException);
                }
                log.info("获取课程 重试1次");
                body = httpRequest.execute().body();
            }

            JSONObject jsonObject = JSON.parseObject(body);
            if (jsonObject.getInteger("state") != 1) throw new RuntimeException("获取课精选留言失败:" + body);
            CourseCommentListInfo content = jsonObject.getJSONObject("content").toJavaObject(CourseCommentListInfo.class);
            //是否有下一页
            hasNextPage = content.isHasNextPage();
            if (CollectionUtil.isNotEmpty(content.getCourseCommentList())) {
                list.addAll(content.getCourseCommentList());
            }
        }

        return list;
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
                Thread.sleep(RandomUtil.randomLong(500L,
                        TimeUnit.SECONDS.toMillis(1)));
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
        return getVodPlayerInfo(rand, playAuth, fileId, "");
    }

    public static AliyunVodPlayInfo getVodPlayerInfo(String rand, String playAuth, String fileId, String formats) {
        String playInfoRequestUrl = AliyunApiUtils.getPlayInfoRequestUrl(rand, playAuth, fileId, formats);
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

    public static CourseLessonDetail getCourseLessonDetail(String lessonId, String lessonName) {
        String url = MessageFormat.format(COURSE_DETAIL_API, lessonId);
        log.debug("获取课程详情URL:【{}】url：{}", lessonId, url);
        String videoJson = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject json = JSON.parseObject(videoJson);
        Integer state = json.getInteger("state");
        if (state != null && state != 1) {
            log.info("获取视频视频信息失败:【{}】,json：{}", lessonName, videoJson);
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

    /**
     * 获取已购课程信息
     *
     * @return
     */
    public static PurchasedCourseRecord getPurchasedCourseRecord() {
        String url = MessageFormat.format(PURCHASED_COURSE_API, System.currentTimeMillis());
        String body = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject json = JSON.parseObject(body);
        Integer state = json.getInteger("state");
        if (state != null && state != 1) {
            log.info("获取已购课程失败:json：{}", body);
            throw new RuntimeException("获取已购课程失败:" + json.getString("message"));
        }
        JSONObject result = json.getJSONObject("content");
        JSONArray jsonArray = result.getJSONArray("allCoursePurchasedRecord");
        PurchasedCourseRecord record = new PurchasedCourseRecord();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject courseListObject = jsonArray.getJSONObject(i);
            Integer courseType = courseListObject.getInteger("courseType");
            if (1 == courseType) {
                List<PurchasedCourseRecord.CourseInfo> bigCourseRecordList = courseListObject.getJSONArray("bigCourseRecordList").toJavaList(PurchasedCourseRecord.CourseInfo.class);
                record.getTrainingCamp().addAll(bigCourseRecordList);
            } else if (2 == courseType) {
                List<PurchasedCourseRecord.CourseInfo> courseList = courseListObject.getJSONArray("courseRecordList").toJavaList(PurchasedCourseRecord.CourseInfo.class);
                record.getColumns().addAll(courseList);
            } else {
                log.warn("未知的课程类型:{},json:{}", courseType, json.toJSONString());
            }
        }
        return record;
    }
}
