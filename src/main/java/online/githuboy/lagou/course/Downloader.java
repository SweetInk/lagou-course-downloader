package online.githuboy.lagou.course;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.domain.LessonInfo;
import online.githuboy.lagou.course.task.VideoInfoLoader;
import online.githuboy.lagou.course.utils.HttpUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 下载器
 *
 * @author suchu
 * @since 2019年8月2日
 */
@Slf4j
public class Downloader {
    private final static String COURSE_INFO_API = "https://gate.lagou.com/v1/neirong/kaiwu/getCourseLessons?courseId={0}";
    /**
     * 拉钩视频课程地址
     */
    @Getter
    private String courseId;
    /**
     * 视频保存路径
     */
    @Getter
    private String savePath;

    private File basePath;

    private String courseUrl;

    private CountDownLatch latch;
    private List<LessonInfo> lessonInfoList = new ArrayList<>();
    private volatile List<MediaLoader> mediaLoaders;

    private long start;

    public Downloader(String courseId, String savePath) {
        this.courseId = courseId;
        this.savePath = savePath;
        this.courseUrl = String.format(COURSE_INFO_API, courseId);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            log.info("检查ffmpeg是否存在");
            int status = CmdExecutor.executeCmd(new File("."), "ffmpeg", "-version");
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            return;
        }
        String courseId = "拉钩课程ID";
        String savePath = "下载好的视频保存目录";
        Downloader downloader = new Downloader(courseId, savePath);
        Thread logThread = new Thread(() -> {
            while (true) {
                log.info("Thread pool:{}", ExecutorService.getExecutor());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }, "log-thread");
        logThread.setDaemon(true);
        //logThread.start();
        downloader.start();
    }

    public void start() throws IOException, InterruptedException {
        start = System.currentTimeMillis();
        parseLessonInfo2();
        parseVideoInfo();
        downloadMedia();

    }

    private void parseLessonInfo2() throws IOException {
        String strContent = HttpUtils
                .get(courseUrl, CookieStore.getCookie())
                .header("x-l-req-header", " {deviceType:1}")
                .execute().body();
        JSONObject jsonObject = JSONObject.parseObject(strContent);
        if (jsonObject.getInteger("state") != 1) {
            throw new RuntimeException("访问课程信息出错:" + strContent);
        }
        jsonObject = jsonObject.getJSONObject("content");
        Integer courseId = jsonObject.getInteger("id");
        String courseName = jsonObject.getString("courseName");
        JSONArray courseSections = jsonObject.getJSONArray("courseSectionList");
        this.basePath = new File(savePath, courseId + "_" + courseName);
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        for (int i = 0; i < courseSections.size(); i++) {
            JSONObject courseSection = courseSections.getJSONObject(i);
            JSONArray courseLessons = courseSection.getJSONArray("courseLessons");
            for (int j = 0; j < courseLessons.size(); j++) {
                JSONObject lesson = courseLessons.getJSONObject(j);
                String lessonName = lesson.getString("theme");
//                if (!lessonName.contains("62讲")) continue;
                String lessonId = lesson.getString("id");
                String fileId = "";
                String fileUrl = "";
                String fileEdk = "";
                JSONObject videoMediaDTO = lesson.getJSONObject("videoMediaDTO");
                if (null != videoMediaDTO) {

                    fileId = videoMediaDTO.getString("fileId");
                    fileUrl = videoMediaDTO.getString("fileUrl");
                    fileEdk = videoMediaDTO.getString("fileEdk");
                }
                String appId = lesson.getString("appId");
                LessonInfo lessonInfo = LessonInfo.builder().lessionId(lessonId).lessonName(lessonName).fileId(fileId).appId(appId).fileEdk(fileEdk).fileUrl(fileUrl).build();
                lessonInfoList.add(lessonInfo);
                log.info("解析到课程信息：name：{},appId:{},fileId:{}", lessonName, appId, fileId);
            }
        }
        System.out.println(1);
    }

    @Deprecated
    private void parseLessonInfo() throws IOException {
        Connection connect = Jsoup.connect(courseUrl);
        Document document = connect.get();
        Elements scripts = document.select("script");
        JSONObject jsonObject = null;
        for (int i = 0; i < scripts.size(); i++) {
            if (scripts.get(i).data().contains("courseInfo")) {
                String text = scripts.get(i).data();
                String substring = text.substring(text.indexOf("window.courseInfo"));
                String js = substring.split("\n")[0].replaceAll("window.courseInfo\\s*?=\\s*?", "");
                js = js.substring(0, js.lastIndexOf(";"));
                jsonObject = JSON.parseObject(js);
                break;
            }
        }
        if (null == jsonObject) {
            throw new RuntimeException("没有解析到课程信息");
        }
        JSONArray courseSections = jsonObject.getJSONArray("courseSections");
        Integer courseId = jsonObject.getInteger("id");
        String courseName = jsonObject.getString("courseName");
        this.basePath = new File(savePath, courseId + "_" + courseName);
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        for (int i = 0; i < courseSections.size(); i++) {
            JSONObject courseSection = courseSections.getJSONObject(i);
            JSONArray courseLessons = courseSection.getJSONArray("courseLessons");
            for (int j = 0; j < courseLessons.size(); j++) {
                JSONObject lesson = courseLessons.getJSONObject(j);
                String theme = lesson.getString("theme");
                String fileId = lesson.getString("fileId");
                String appId = lesson.getString("appId");
                LessonInfo lessonInfo = LessonInfo.builder().lessonName(theme).appId(appId).fileId(fileId).build();
                lessonInfoList.add(lessonInfo);
                log.info("解析到课程信息：name：{},appId:{},fileId:{}", theme, appId, fileId);
            }
        }
    }

    private void parseVideoInfo() {
        latch = new CountDownLatch(lessonInfoList.size());
        mediaLoaders = new Vector<>();
        lessonInfoList.forEach(lessonInfo -> {
            VideoInfoLoader loader = new VideoInfoLoader(lessonInfo.getLessonName(), lessonInfo.getAppId(), lessonInfo.getFileId(), lessonInfo.getFileUrl(), lessonInfo.getLessionId());
            loader.setM3U8MediaLoaders(mediaLoaders);
            loader.setBasePath(this.basePath);
            loader.setLatch(latch);
            ExecutorService.execute(loader);
        });
    }

    private void downloadMedia() throws InterruptedException {
        latch.await();
        if (mediaLoaders.size() != lessonInfoList.size()) {
            log.info("视频META信息没有全部下载成功: success:{},total:{}", mediaLoaders.size(), lessonInfoList.size());
            tryTerminal();
            return;
        }
        log.info("所有视频META信息获取成功 total：{}", mediaLoaders.size());
        CountDownLatch all = new CountDownLatch(mediaLoaders.size());

        for (MediaLoader loader : mediaLoaders) {
            loader.setLatch(all);
            ExecutorService.getExecutor().execute(loader);
        }
        all.await();
        long end = System.currentTimeMillis();
        log.info("所有视频处理完成:{} s", (end - start) / 1000);
        log.info("视频输出目录:{}", this.basePath.getAbsolutePath());
        System.out.println("\n\n失败统计信息\n\n");
        Stats.failedCount.forEach((key, value) -> System.out.println(key + " -> " + value.get()));
        tryTerminal();
    }

    private void tryTerminal() throws InterruptedException {
        log.info("程序将在{}s后退出", 5);
        ExecutorService.getExecutor().shutdown();
        ExecutorService.getHlsExecutor().shutdown();
        ExecutorService.getHlsExecutor().awaitTermination(5, TimeUnit.SECONDS);
        ExecutorService.getExecutor().awaitTermination(5, TimeUnit.SECONDS);
    }

}
