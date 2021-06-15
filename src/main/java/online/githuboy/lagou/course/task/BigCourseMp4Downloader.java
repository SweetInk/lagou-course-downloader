package online.githuboy.lagou.course.task;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.support.MediaLoader;
import online.githuboy.lagou.course.support.Mp4History;
import online.githuboy.lagou.course.support.Stats;
import online.githuboy.lagou.course.utils.FileUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * 训练营课程MP4 loader
 *
 * @author jack
 * @since 2021年5月20日
 */
@Slf4j
public class BigCourseMp4Downloader implements Runnable, NamedTask, MediaLoader {

    private final static int MAX_RETRY_COUNT = 3;
    private final String videoName;
    private final String lessonId;
    private final String playUrl;
    private volatile int retryCount = 0;
    @Setter
    private String basePath;

    @Setter
    private CountDownLatch latch;
    private volatile long startTime = 0;

    public BigCourseMp4Downloader(String videoName, String lessonId, String playUrl) {
        this.videoName = videoName;
        this.lessonId = lessonId;
        this.playUrl = playUrl;
    }

    @Override
    public void run() {
        try {
            if (this.playUrl != null) {
                File mp4File = new File(basePath, "[" + lessonId + "] " + FileUtils.getCorrectFileName(videoName) + ".!mp4");
                HttpRequest.get(this.playUrl).execute(true).writeBody(mp4File, new StreamProgress() {
                    @Override
                    public void start() {
                        log.info("开始下载视频【{}】lessonId={}", videoName, lessonId);
                        if (startTime == 0) {
                            startTime = System.currentTimeMillis();
                        }
                    }

                    @Override
                    public void progress(long l) {
                    }

                    @Override
                    public void finish() {
                        Stats.remove(videoName);
                        Mp4History.append(lessonId);
                        latch.countDown();
                        long count = latch.getCount();
                        FileUtils.replaceFileName(mp4File, ".!mp4", ".mp4");
                        log.info("====>视频下载完成【{}】,耗时:{} s，剩余{}", videoName, (System.currentTimeMillis() - startTime) / 1000, count);
                    }
                });

            } else {
                log.warn("没有获取到视频【{}】播放地址:", videoName);
                latch.countDown();
            }
        } catch (Exception e) {
            log.error("获取视频:{}信息失败:", videoName, e);
            if (retryCount < MAX_RETRY_COUNT) {
                Stats.incr(videoName);
                retryCount += 1;
                log.info("第:{}次重试获取:{}", retryCount, videoName);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                ExecutorService.execute(this);
            } else {
                log.info(" video:{}最大重试结束:{}", videoName, MAX_RETRY_COUNT);
                latch.countDown();
            }
        }
    }
}
