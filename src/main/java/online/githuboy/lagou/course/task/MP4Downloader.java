package online.githuboy.lagou.course.task;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.decrypt.alibaba.EncryptUtils;
import online.githuboy.lagou.course.domain.AliyunVodPlayInfo;
import online.githuboy.lagou.course.domain.PlayHistory;
import online.githuboy.lagou.course.request.HttpAPI;
import online.githuboy.lagou.course.support.*;
import online.githuboy.lagou.course.utils.ConfigUtil;
import online.githuboy.lagou.course.utils.FileUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * MP4下载器
 *
 * @author suchu
 * @date 2020/8/7
 */
@Builder
@Slf4j
public class MP4Downloader extends AbstractRetryTask implements NamedTask, MediaLoader {
    /**
     * 0 -> videoId
     */
    private final static int maxRetryCount = 3;
    private final String videoName;
    private final String appId;
    private final String fileId;
    private final String fileUrl;
    private final String lessonId;
    private int retryCount = 0;
    @Setter
    private File basePath;

    private File workDir;
    //所有下载任务的Latch
    @Setter
    private CountDownLatch latch;
    private CountDownLatch fileDownloadFinishedLatch;
    private volatile long startTime = 0;

    private void initDir() {
        workDir = basePath;
    }

    @Override
    protected void action() {
        initDir();
        PlayHistory playHistory = HttpAPI.getPlayHistory(lessonId);
        //优先从拉钩视频平台获取可直接播放的URL
        String playUrl = HttpAPI.tryGetPlayUrlFromKaiwu(playHistory.getFileId());
        if (StrUtil.isBlank(playUrl)) {
            String rand = "test";
            String encryptRand = EncryptUtils.encryptRand(rand);
            AliyunVodPlayInfo vodPlayerInfo = HttpAPI.getVodPlayerInfo(encryptRand, playHistory.getAliPlayAuth(), playHistory.getFileId());
            if (null != vodPlayerInfo) {
                playUrl = vodPlayerInfo.getPlayURL();
                log.info("解析出【{}】MP4播放地址:{}", videoName, playUrl);
            } else {
                log.warn("没有获取到视频【{}】播放地址:", videoName);
                latch.countDown();
                return;
            }
        }
        File mp4File = new File(workDir, FileUtils.getCorrectFileName(videoName) + ".!mp4");
        fileDownloadFinishedLatch = new CountDownLatch(1);
        try {
            HttpUtil.downloadFile(playUrl, mp4File, Integer.parseInt(ConfigUtil.readValue("mp4_download_timeout")) * 60 * 1000, new StreamProgress() {
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
        long count = latch.getCount();
        FileUtils.replaceFileName(mp4File, ".!mp4", ".mp4");
        log.info("====>视频下载完成【{}】,耗时:{} s，剩余{}", videoName, (System.currentTimeMillis() - startTime) / 1000, count - 1);
        latch.countDown();
    }

    @Override
    public boolean canRetry() {
        return retryCount < maxRetryCount;
    }

    @Override
    protected void retry(Throwable throwable) {
        super.retry(throwable);
        log.error("获取视频:{}信息失败:", videoName, throwable);
        Stats.incr(videoName);
        retryCount += 1;
        log.info("第:{}次重试获取:{}", retryCount, videoName);
        try {
            Thread.sleep(RandomUtil.randomLong(500L,
                    TimeUnit.SECONDS.toMillis(2)));
        } catch (InterruptedException e1) {
            log.error("线程休眠异常", e1);
        }
        ExecutorService.execute(this);
    }

    @Override
    public void retryComplete() {
        super.retryComplete();
        log.error(" video:{}最大重试结束:{}", videoName, maxRetryCount);
        latch.countDown();
    }
}
