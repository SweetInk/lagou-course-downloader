package online.githuboy.lagou.course.task;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.*;
import online.githuboy.lagou.course.utils.ConfigUtil;
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
public class BigCourseMp4Downloader extends AbstractRetryTask implements NamedTask, MediaLoader {

    private final static int MAX_RETRY_COUNT = 3;
    private final String videoName;
    private final String lessonId;
    private final String playUrl;
    private int retryCount = 0;
    @Setter
    private String basePath;
    private CountDownLatch fileDownloadFinishedLatch;


    @Setter
    private CountDownLatch latch;
    private volatile long startTime = 0;

    public BigCourseMp4Downloader(String videoName, String lessonId, String playUrl) {
        this.videoName = videoName;
        this.lessonId = lessonId;
        this.playUrl = playUrl;
    }

    @Override
    protected void action() {
        if (this.playUrl != null) {
            File mp4File = new File(basePath, "[" + lessonId + "] " + FileUtils.getCorrectFileName(videoName) + ".!mp4");
            fileDownloadFinishedLatch = new CountDownLatch(1);
            try {
                HttpRequest.get(this.playUrl).timeout(Integer.parseInt(ConfigUtil.readValue("mp4_download_timeout")) * 60 * 1000).execute(true).writeBody(mp4File, new StreamProgress() {
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
                        fileDownloadFinishedLatch.countDown();
                    }
                });
                fileDownloadFinishedLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread interrupted", e);
            } finally {
                fileDownloadFinishedLatch.countDown();
            }
            Stats.remove(videoName);
            Mp4History.append(lessonId);
            latch.countDown();
            long count = latch.getCount();
            FileUtils.replaceFileName(mp4File, ".!mp4", ".mp4");
            log.info("====>视频下载完成【{}】,耗时:{} s，剩余{}", videoName, (System.currentTimeMillis() - startTime) / 1000, count);
        } else {
            log.warn("没有获取到视频【{}】播放地址:", videoName);
            latch.countDown();
        }
    }

    @Override
    protected void retry(Throwable throwable) {
        super.retry(throwable);
        Stats.incr(videoName);
        retryCount += 1;
        log.info("第:{}次重试获取:{}", retryCount, videoName, throwable);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        ExecutorService.execute(this);
    }

    @Override
    public void retryComplete() {
        super.retryComplete();
        log.error(" video:{}最大重试结束:{}", videoName, MAX_RETRY_COUNT);
        latch.countDown();
    }

    @Override
    public boolean canRetry() {
        return retryCount < MAX_RETRY_COUNT;
    }

}
