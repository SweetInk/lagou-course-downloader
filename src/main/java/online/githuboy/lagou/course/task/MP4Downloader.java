package online.githuboy.lagou.course.task;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.*;
import online.githuboy.lagou.course.utils.FileUtils;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;

import static online.githuboy.lagou.course.decrypt.alibaba.AliPlayerDecrypt.getPlayInfoRequestUrl;

/**
 * MP4下载器
 *
 * @author suchu
 * @date 2020/8/7
 */
@Builder
@Slf4j
public class MP4Downloader extends AbstractRetryTask implements NamedTask, MediaLoader {
    private static final String API_TEMPLATE = "https://gate.lagou.com/v1/neirong/kaiwu/getLessonPlayHistory?lessonId={0}&isVideo=true";

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
    @Setter
    private CountDownLatch latch;
    private volatile long startTime = 0;

    private void initDir() {
        String fileName = FileUtils.getCorrectFileName(videoName);
        workDir = basePath;
//        workDir = new File(basePath, fileName.replaceAll("/", "_") + "_" + lessonId);
//        if (!workDir.exists()) {
//            workDir.mkdirs();
//        }
    }

    @Override
    protected void action() {
        initDir();
        String url = MessageFormat.format(API_TEMPLATE, this.lessonId);
        log.debug("获取课程:{}信息，url：{}", lessonId, url);
        String body = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
        JSONObject jsonObject = JSON.parseObject(body);
        if (jsonObject.getInteger("state") != 1) throw new RuntimeException("获取课程信息失败:" + body);
        String aliPlayAuth = jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").getString("aliPlayAuth");
        String fileId = jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").getString("fileId");
        String playInfoRequestUrl = getPlayInfoRequestUrl(aliPlayAuth, fileId);
        String response = HttpRequest.get(playInfoRequestUrl).execute().body();
        log.debug("\nAPI request result:\n\n" + response);
        JSONObject mediaObj = JSON.parseObject(response);
        if (mediaObj.getString("Code") != null) throw new RuntimeException("获取媒体信息失败:");
        JSONObject playInfoList = mediaObj.getJSONObject("PlayInfoList");
        JSONArray playInfos = playInfoList.getJSONArray("PlayInfo");
        if (playInfos != null && playInfos.size() > 0) {
            JSONObject playInfo = playInfos.getJSONObject(0);
            String mp4Url = playInfo.getString("PlayURL");
            log.info("解析出【{}】MP4播放地址:{}", videoName, mp4Url);

            HttpRequest.get(mp4Url).execute(true).writeBody(new File(workDir, FileUtils.getCorrectFileName(videoName) + ".mp4"), new StreamProgress() {
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
                    long count = latch.getCount();
                    log.info("====>视频下载完成【{}】,耗时:{} s，剩余{}", videoName, (System.currentTimeMillis() - startTime) / 1000, count - 1);
                    latch.countDown();
                }
            });

        } else {
            log.warn("没有获取到视频【{}】播放地址:", videoName);
            latch.countDown();
        }

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
            Thread.sleep(200);
        } catch (InterruptedException e1) {
            log.error("{}", e1);
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
