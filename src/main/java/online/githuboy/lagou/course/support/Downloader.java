package online.githuboy.lagou.course.support;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.domain.CourseInfo;
import online.githuboy.lagou.course.domain.DownloadType;
import online.githuboy.lagou.course.domain.LessonInfo;
import online.githuboy.lagou.course.request.HttpAPI;
import online.githuboy.lagou.course.task.VideoInfoLoader;
import online.githuboy.lagou.course.utils.ReadTxt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static online.githuboy.lagou.course.support.ExecutorService.COUNTER;

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
    }

    public Downloader(String courseId, String savePath, DownloadType downloadType) {
        this.courseId = courseId;
        this.savePath = savePath;
        this.downloadType = downloadType;
    }

    public void start() throws InterruptedException {
        start = System.currentTimeMillis();
        List<LessonInfo> lessons = parseLessonInfo();
        if (!CollectionUtil.isEmpty(lessons)) {
            int i = parseVideoInfo(lessons, this.downloadType);
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
        // TODO retry
        CourseInfo courseInfo = HttpAPI.getCourseInfo(this.courseId);
        courseName = courseInfo.getCourseName();
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
        if (CollectionUtil.isEmpty(courseInfo.getCourseSectionList())) {
            log.error("《{}》课程为空", courseName);
            return Collections.emptyList();
        }
        log.info("====>正在下载《{}》 courseId={}", courseName, this.courseId);
        for (CourseInfo.Section section : courseInfo.getCourseSectionList()) {
            if (!CollectionUtil.isEmpty(section.getCourseLessons())) {
                List<LessonInfo> lessons = section
                        .getCourseLessons()
                        .stream()
                        .filter(lesson -> {
                            if (!"RELEASE".equals(lesson.getStatus())) {
                                log.info("课程:【{}】 [未发布]", lesson.getTheme());
                                return false;
                            }
                            return true;
                        }).filter(lesson -> {
                                    if (Mp4History.contains(lesson.getId() + "")) {
                                        log.debug("课程【{}】已经下载过了", lesson.getTheme());
                                        return false;
                                    }
                                    return true;
                                }
                        ).map(lesson -> {
                            String fileId = null;
                            String fileEdk = null;
                            String fileUrl = null;
                            if (null != lesson.getVideoMediaDTO()) {
                                fileId = lesson.getVideoMediaDTO().getFileId();
                                fileEdk = lesson.getVideoMediaDTO().getFileEdk();
                                fileUrl = lesson.getVideoMediaDTO().getFileUrl();
                            }
                            log.debug("解析到课程信息：【{}】,appId:{},fileId:{}", lesson.getTheme(), lesson.getAppId(), fileId);
                            return LessonInfo.builder().lessonId(lesson.getId() + "").lessonName(lesson.getTheme()).fileId(fileId).appId(lesson.getAppId()).fileEdk(fileEdk).fileUrl(fileUrl).build();
                        }).collect(Collectors.toList());
                lessonInfoList.addAll(lessons);
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
     * @throws InterruptedException interruptedException
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
