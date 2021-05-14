package online.githuboy.lagou.course.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.domain.LessonInfo;
import online.githuboy.lagou.course.task.VideoInfoLoader;
import online.githuboy.lagou.course.domain.DownloadType;
import online.githuboy.lagou.course.utils.HttpUtils;
import online.githuboy.lagou.course.utils.ReadTxt;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static online.githuboy.lagou.course.support.ExecutorService.COUNTER;

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
    private final String courseId;

    /**
     * 课程名字
     */
    private String courseName;
    /**
     * 视频保存路径
     */
    @Getter
    private final String savePath;

    private File basePath;

    private File textPath;


    private final String courseUrl;

    private CountDownLatch latch;

    /**
     * 需要使用线程安全的List
     */
    private volatile List<MediaLoader> mediaLoaders;

    private long start;

    private DownloadType downloadType = DownloadType.VIDEO;

    public Downloader(String courseId, String savePath) {
        this.courseId = courseId;
        this.savePath = savePath;
        this.courseUrl = MessageFormat.format(COURSE_INFO_API, courseId);
    }

    public Downloader(String courseId, String savePath, DownloadType downloadType) {
        this.courseId = courseId;
        this.savePath = savePath;
        this.courseUrl = MessageFormat.format(COURSE_INFO_API, courseId);
        this.downloadType = downloadType;
    }

    public void start() throws InterruptedException {
        start = System.currentTimeMillis();
        List<LessonInfo> i1 = parseLessonInfo();
        if (i1 != null && i1.size() > 0) {
            int i = parseVideoInfo(i1, this.downloadType);
            if (i > 0) {
                downloadMedia(i);
            } else {
                log.info("===>《{}》所有课程都下载完成了", courseName);
            }
        }
    }

    /**
     * @return 解析课程列表信息
     */
    private List<LessonInfo> parseLessonInfo() {
        List<LessonInfo> lessonInfoList = new ArrayList<>();

        String strContent = HttpUtils
                .get(courseUrl, CookieStore.getCookie())
                .header("x-l-req-header", " {deviceType:1}")
                .execute().body();
        JSONObject jsonObject = JSONObject.parseObject(strContent);
        if (jsonObject.getInteger("state") != 1) {
            throw new RuntimeException("访问课程信息出错:" + strContent);
        }
        jsonObject = jsonObject.getJSONObject("content");
        courseName = jsonObject.getString("courseName");
        JSONArray courseSections = jsonObject.getJSONArray("courseSectionList");

        this.basePath = new File(savePath, this.courseId + "_" + courseName);
        if (!basePath.exists()) {
            basePath.mkdirs();
            log.info("视频存放文件夹{}", basePath.getAbsolutePath());
        }

        this.textPath = new File(this.basePath, "文档");
        if (!textPath.exists()) {
            textPath.mkdirs();
            log.info("文档存放文件夹{}", textPath.getAbsolutePath());
        }

        if (courseSections == null || courseSections.size() <= 0) {
            log.error("《{}》课程为空", courseName);
            return null;
        }

        log.info("====>正在下载《{}》 courseId={}", courseName, this.courseId);
        for (int i = 0; i < courseSections.size(); i++) {
            JSONObject courseSection = courseSections.getJSONObject(i);
            JSONArray courseLessons = courseSection.getJSONArray("courseLessons");
            if (courseLessons != null) {
                for (int j = 0; j < courseLessons.size(); j++) {
                    JSONObject lesson = courseLessons.getJSONObject(j);
                    String lessonName = lesson.getString("theme");
                    String status = lesson.getString("status");
                    if (!"RELEASE".equals(status)) {
                        log.info("课程:【{}】 [未发布]", lessonName);
                        continue;
                    }
                    //insert your filter code,use for debug
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
                    LessonInfo lessonInfo = LessonInfo.builder().lessonId(lessonId).lessonName(lessonName).fileId(fileId).appId(appId).fileEdk(fileEdk).fileUrl(fileUrl).build();
                    if (!Mp4History.contains(lessonInfo.getLessonId())) {
                        lessonInfoList.add(lessonInfo);
                    } else {
                        log.info("课程【{}】已经下载过了", lessonInfo.getLessonName());
                    }
                    log.debug("解析到课程信息：【{}】,appId:{},fileId:{}", lessonName, appId, fileId);
                }
            } else {
                log.error("获取课程视频列表信息失败");
            }
        }
        return lessonInfoList;
    }

    /**
     * 解析课程得到视频信息
     *
     * @param lessonInfoList
     * @param downloadType
     * @return
     */
    private int parseVideoInfo(List<LessonInfo> lessonInfoList, DownloadType downloadType) {
        AtomicInteger videoSize = new AtomicInteger();
        latch = new CountDownLatch(lessonInfoList.size());
        // 这里使用的线程安全的容器，否则多线程添加应该会出现问题. Vector的啊add()方法加了锁synchronized
        mediaLoaders = new Vector<>();
        lessonInfoList.forEach(lessonInfo -> {
            String lessonId = lessonInfo.getLessonId();
            String lessonName = lessonInfo.getLessonName();
            if (!Mp4History.contains(lessonId)) {
                videoSize.getAndIncrement();
                VideoInfoLoader loader = new VideoInfoLoader(lessonName, lessonInfo.getAppId(), lessonInfo.getFileId(), lessonInfo.getFileUrl(), lessonId, downloadType);
                loader.setMediaLoaders(mediaLoaders);
                loader.setBasePath(this.basePath);
                loader.setTextPath(this.textPath);
                loader.setLatch(latch);
                ExecutorService.execute(loader);
            } else {
                log.warn("课程【{}】已经下载过了", lessonName);
                latch.countDown();
                COUNTER.incrementAndGet();
            }
        });
        return videoSize.intValue();
    }

    /**
     * 下载课程解析后的视频
     *
     * @param i 理论需要下载的视频数量
     * @throws InterruptedException
     */
    private void downloadMedia(int i) throws InterruptedException {
        log.debug("等待《{}》获取视频信息任务完成...", courseName);
        latch.await();
        int mediaLoadersSize = mediaLoaders.size();
        if (mediaLoadersSize != i) {
            String message = String.format("《%s》视频META信息没有全部下载成功: success:%s,total:%s", courseName, mediaLoaders.size(), i);
            log.error("{}", message);
            File file = new File(basePath, "下载失败.txt");
            ReadTxt readTxt = new ReadTxt();
            readTxt.writeFile(file.getAbsolutePath(), message);

            if (mediaLoadersSize <= 0) {
                return;
            }
        } else {
            log.info("《{}》所有视频META信息获取成功 total：{}", courseName, mediaLoadersSize);
        }

        // 执行下载视频的工作单元
        CountDownLatch all = new CountDownLatch(mediaLoadersSize);
        for (MediaLoader loader : mediaLoaders) {
            loader.setLatch(all);
            ExecutorService.getExecutor().execute(loader);
        }
        all.await();

        long end = System.currentTimeMillis();
        log.info("《{}》所有视频处理耗时:{} s", courseName, (end - start) / 1000);
        log.info("《{}》视频输出目录:{}\n\n", courseName, this.basePath.getAbsolutePath());
        File file = new File(basePath, "下载完成.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            log.error("{}", e);
        }

        if (!Stats.isEmpty()) {
            log.info("\n\n失败统计信息\n\n");
            Stats.failedCount.forEach((key, value) -> System.out.println(key + " -> " + value.get()));
        }
    }
}
