package online.githuboy.lagou.course.task;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.*;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static online.githuboy.lagou.course.support.ExecutorService.COUNTER;

/**
 * 训练营课程资料 loader
 *
 * @author jack
 * @since 2021年5月20日
 */
@Slf4j
public class ResourceLoader implements Runnable, NamedTask, MediaLoader {

    private final static int MAX_RETRY_COUNT = 3;
    private final String videoName;
    private final String lessonId;
    private final String weekId;
    private final String resourceUrl;

    private int retryCount = 0;
    @Setter
    private String basePath;

    @Setter
    private CountDownLatch latch;
    private volatile long startTime = 0;

    public ResourceLoader(String videoName, String lessonId, String weekId, String resourceUrl) {
        this.videoName = videoName;
        this.lessonId = lessonId;
        this.weekId = weekId;
        this.resourceUrl = resourceUrl;
    }

    @Override
    public void run() {
        try {
            if (this.resourceUrl!=null){
                HttpResponse httpResponse = HttpUtils.get(this.resourceUrl, CookieStore.getCookie()).header("x-l-req-header", " {deviceType:1}").execute(true);
                String contentHeader = httpResponse.header("Content-Disposition");
                if (contentHeader!=null && contentHeader.contains("filename=")) {
                    String filename = contentHeader.substring(contentHeader.indexOf("filename=") + 9);
                    httpResponse.writeBody(new File(this.basePath, filename), new StreamProgress() {
                        @Override
                        public void start() {
                            log.info("开始下载课程资料【{}】 lessonId={} weekId={}", videoName, lessonId, weekId);
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
                            log.info("====>课程资料下载完成【{}】,耗时:{} s，剩余{}", videoName, (System.currentTimeMillis() - startTime) / 1000, count);
                        }
                    });
                }
            } else {
                log.warn("没有获取到课程资料【{}】地址:", videoName);
                latch.countDown();
            }
        } catch (Exception e) {
            log.error("获取课程资料:【{}】信息失败:", this.videoName, e);
            if (this.retryCount < MAX_RETRY_COUNT) {
                this.retryCount += 1;
                log.info("第:{}次重试获取:{}", this.retryCount, this.videoName);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    log.error("", e1);
                }
                ExecutorService.execute(this);
            } else {
                log.info("课程资料:【{}】最大重试结束:{}", this.videoName, MAX_RETRY_COUNT);
                COUNTER.incrementAndGet();
                this.latch.countDown();
            }
        }
    }

    @Override
    public String getTaskDescription() {
        return this.videoName;
    }
}
