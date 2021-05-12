package online.githuboy.lagou.course.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.domain.LessonInfo;
import online.githuboy.lagou.course.task.VideoInfoLoader;
import online.githuboy.lagou.course.utils.DownloadType;
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
    //    private final List<LessonInfo> lessonInfoList = new ArrayList<>();
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

    public void start() throws IOException, InterruptedException {
        start = System.currentTimeMillis();
        List<LessonInfo> i1 = parseLessonInfo2();
        if (i1.size() > 0) {
            int i = parseVideoInfo(i1, this.downloadType);
            if (i > 0) {
                downloadMedia(i);
            } else {
                log.info("===>所有课程都下载完成了");
            }
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private List<LessonInfo> parseLessonInfo2() throws IOException {
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
        log.info("\n\n\n");
        if (!basePath.exists()) {
            basePath.mkdirs();
            log.info("视频存放文件夹{}", basePath.getAbsolutePath());
        }

        this.textPath = new File(this.basePath, "文档");
        if (!textPath.exists()) {
            textPath.mkdirs();
            log.info("文档存放文件夹{}", textPath.getAbsolutePath());
        }

        log.info("====>正在下载《{}》 courseId={}", courseName, this.courseId);
        for (int i = 0; i < courseSections.size(); i++) {
            JSONObject courseSection = courseSections.getJSONObject(i);
            JSONArray courseLessons = courseSection.getJSONArray("courseLessons");
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
                log.info("解析到课程信息：【{}】,appId:{},fileId:{}", lessonName, appId, fileId);
            }
        }
        return lessonInfoList;
    }

    private int parseVideoInfo(List<LessonInfo> lessonInfoList, DownloadType downloadType) {
        AtomicInteger videoSize = new AtomicInteger();
        latch = new CountDownLatch(lessonInfoList.size());
        mediaLoaders = new Vector<>();
        lessonInfoList.forEach(lessonInfo -> {
            if (!Mp4History.contains(lessonInfo.getLessonId())) {
                videoSize.getAndIncrement();
                VideoInfoLoader loader = new VideoInfoLoader(lessonInfo.getLessonName(), lessonInfo.getAppId(), lessonInfo.getFileId(), lessonInfo.getFileUrl(), lessonInfo.getLessonId(), downloadType);
                loader.setMediaLoaders(mediaLoaders);
                loader.setBasePath(this.basePath);
                loader.setTextPath(this.textPath);
                loader.setLatch(latch);
                ExecutorService.execute(loader);
            } else {
                log.info("课程【{}】已经下载过了", lessonInfo.getLessonName());
                latch.countDown();
                COUNTER.incrementAndGet();
            }
        });
        return videoSize.intValue();
    }

    /**
     * @param i 需要下载的视频数量
     * @throws InterruptedException
     */
    private void downloadMedia(int i) throws InterruptedException {
        log.info("等待《{}》获取视频信息任务完成...", courseName);
        System.out.println(ExecutorService.COUNTER);
        latch.await();
        if (mediaLoaders.size() != i) {
            String message = String.format("《{}》视频META信息没有全部下载成功: success:%s,total:%s", courseName, mediaLoaders.size(), i);
            log.error("{}", message);
//            ExecutorService.tryTerminal
            File file = new File(basePath, "下载失败.txt");
            ReadTxt readTxt = new ReadTxt();
            readTxt.writeFile(file.getAbsolutePath(), message);

//            for (MediaLoader mediaLoader:mediaLoaders){
//                ReadTxt.writeFile(file.getAbsolutePath(),mediaLoader.);
//            }
            return;
        }
        log.info("《{}》所有视频META信息获取成功 total：{}", courseName, mediaLoaders.size());
        CountDownLatch all = new CountDownLatch(mediaLoaders.size());

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
            e.printStackTrace();
        }

        if (!Stats.isEmpty()) {
            log.info("\n\n失败统计信息\n\n");
            Stats.failedCount.forEach((key, value) -> System.out.println(key + " -> " + value.get()));
        }
//        tryTerminal();
    }


}
