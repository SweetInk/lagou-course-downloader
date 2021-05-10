package online.githuboy.lagou.course.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.CookieStore;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.support.MediaLoader;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static online.githuboy.lagou.course.support.ExecutorService.COUNTER;

/**
 * 视频metaInfo 加载器
 *
 * @author suchu
 * @since 2019年8月3日
 */
@Slf4j
public class VideoInfoLoader implements Runnable, NamedTask {
    /**
     * 0-> appId
     * 1-> fileId;
     */
    private static final String API_TEMPLATE = "https://gate.lagou.com/v1/neirong/kaiwu/getCourseLessonDetail?lessonId={0}";
    private final static int maxRetryCount = 3;
    private final String videoName;
    private final String appId;
    private final String fileId;
    private final String fileUrl;
    private final String lessonId;
    private int retryCount = 0;
    @Setter
    private File basePath;
    @Setter
    private String mediaType = "mp4";
    @Setter
    private List<MediaLoader> m3U8MediaLoaders;

    @Setter
    private CountDownLatch latch;

    public VideoInfoLoader(String videoName, String appId, String fileId, String fileUrl, String lessonId) {
        this.videoName = videoName;
        this.appId = appId;
        this.fileId = fileId;
        this.fileUrl = fileUrl;
        this.lessonId = lessonId;
    }

    @Override
    public void run() {
        String url = MessageFormat.format(API_TEMPLATE, this.lessonId);
        try {
            log.info("获取视频信息URL:【{}】url：{}", lessonId, url);
            String videoJson = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
            JSONObject json = JSON.parseObject(videoJson);
            Integer state = json.getInteger("state");
            if (state != null && state != 1) {
                log.info("获取视频视频信息失败:【{}】,json：{}", videoName, videoJson);
                throw new RuntimeException("获取视频信息失败:" + json.getString("message"));
            }
            JSONObject result = json.getJSONObject("content");
            JSONObject videoMedia = result.getJSONObject("videoMedia");
            String status = result.getString("status");
            if ("UNRELEASE".equals(status)) {
                log.info("视频:【{}】待更新", videoName);
                latch.countDown();
                COUNTER.incrementAndGet();
                return;
            }
            if (videoMedia != null) {
                String m3u8Url = videoMedia.getString("fileUrl");
                if (m3u8Url != null) {
                    log.info("获取视频:【{}】m3u8播放地址成功:{}", videoName, m3u8Url);
                }

                if ("m3u8".equals(mediaType)) {
                    M3U8MediaLoader m3U8 = new M3U8MediaLoader(m3u8Url, videoName, basePath.getAbsolutePath(), fileId);
                    m3U8.setUrl2(fileUrl);
                    m3U8MediaLoaders.add(m3U8);
                    // ExecutorService.execute(m3U8);
                } else if ("mp4".equals(mediaType)) {
                    MP4Downloader mp4Downloader = MP4Downloader.builder().appId(appId).basePath(basePath.getAbsoluteFile()).videoName(videoName).fileId(fileId).lessonId(lessonId).build();
                    m3U8MediaLoaders.add(mp4Downloader);
                    // ExecutorService.execute(mp4Downloader);
                }
                latch.countDown();
                COUNTER.incrementAndGet();
            }
        } catch (Exception e) {
            log.error("获取视频:【{}】信息失败:", videoName, e);
            if (retryCount < maxRetryCount) {
                retryCount += 1;
                log.info("第:{}次重试获取:{}", retryCount, videoName);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    log.error("", e1);
                }
                ExecutorService.execute(this);
            } else {
                log.info(" video:【{}】最大重试结束:{}", videoName, maxRetryCount);
                COUNTER.incrementAndGet();
                latch.countDown();
            }
        }
    }

    @Override
    public String getTaskDescription() {
        return videoName;
    }
}
