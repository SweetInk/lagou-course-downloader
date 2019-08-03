package online.githuboy.lagou.course;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.domain.LessonInfo;
import online.githuboy.lagou.course.task.M3U8MediaLoader;
import online.githuboy.lagou.course.task.VideoInfoLoader;
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
    /**
     * 拉钩视频课程地址
     */
    @Getter
    private String courseUrl;
    /**
     * 视频保存路径
     */
    @Getter
    private String savePath;

    private File basePath;

    private CountDownLatch latch;
    private List<LessonInfo> lessonInfoList = new ArrayList<>();
    private volatile List<M3U8MediaLoader> mediaLoaders;

    private long start;

    public Downloader(String courseUrl, String savePath) {
        this.courseUrl = courseUrl;
        this.savePath = savePath;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String courseUrl = "视频课程首页";
        String savePath = "视频保存位置";
        Downloader downloader = new Downloader(courseUrl, savePath);
        downloader.start();
    }

    public void start() throws IOException, InterruptedException {
        start = System.currentTimeMillis();
        parseLessonInfo();
        parseVideoInfo();
        downloadMedia();
    }

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
                LessonInfo lessonInfo = new LessonInfo(theme, appId, fileId);
                lessonInfoList.add(lessonInfo);
                log.info("解析到课程信息：name：{},appId:{},fileId:{}", theme, appId, fileId);
            }
        }
    }

    private void parseVideoInfo() {
        latch = new CountDownLatch(lessonInfoList.size());
        mediaLoaders = new Vector<>();
        lessonInfoList.forEach(lessonInfo -> {
            VideoInfoLoader loader = new VideoInfoLoader(lessonInfo.getLessonName(), lessonInfo.getAppId(), lessonInfo.getFileId());
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
        log.info("所有视频META信息获取成功 total：", mediaLoaders.size());
        for (M3U8MediaLoader loader : mediaLoaders) {
            loader.run();
        }
        long end = System.currentTimeMillis();
        log.info("所有视频处理完成:{} s", (end - start) / 1000);
        tryTerminal();
    }

    private void tryTerminal() throws InterruptedException {
        log.info("程序将在{}s后退出", 5);
        ExecutorService.getExecutor().shutdown();
        ExecutorService.getExecutor().awaitTermination(5, TimeUnit.SECONDS);
    }

}
