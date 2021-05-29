package online.githuboy.lagou.course.task;

import online.githuboy.lagou.course.constants.ResourceType;
import online.githuboy.lagou.course.constants.RespCode;
import online.githuboy.lagou.course.pojo.vo.PlayInfoVo;
import online.githuboy.lagou.course.support.CookieStore;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.support.MediaLoader;
import online.githuboy.lagou.course.utils.HttpUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static online.githuboy.lagou.course.decrypt.alibaba.AliPlayerDecrypt.getPlayInfoRequestUrl;
import static online.githuboy.lagou.course.support.ExecutorService.COUNTER;

/**
 * 训练营视频 Info 加载器
 *
 * @author jack
 * @since 2021年5月20日
 */
@Slf4j
public class BigCourseVideoInfoLoader implements Runnable, NamedTask {

    private static final String API_TEMPLATE = "https://gate.lagou.com/v1/neirong/edu/bigcourse/getPlayRecord?courseId={0}&weekId={1}&dayId={2}&lessonId={3}&bigCourseMediaUseType=LUBO";
    private final static int MAX_RETRY_COUNT = 3;
    private final String videoName;
    private final String courseId;
    private final String weekId;
    private final String dayId;
    private final String lessonId;

    private int retryCount = 0;
    @Setter
    private String basePath;

    @Setter
    private List<MediaLoader> mediaLoaders;

    @Setter
    private CountDownLatch latch;

    @Setter
    private String type;

    @Setter
    private String resourceUrl;

    public BigCourseVideoInfoLoader(String videoName, String courseId, String weekId, String dayId, String lessonId) {
        this.videoName = videoName;
        this.courseId = courseId;
        this.weekId = weekId;
        this.dayId = dayId;
        this.lessonId = lessonId;
    }

    @Override
    public void run() {
        String url = MessageFormat.format(API_TEMPLATE, this.courseId, this.weekId, this.dayId, this.lessonId);
        try {
            if (ResourceType.RESOURCE.equals(this.type)) {
                if (this.resourceUrl != null) {
                    log.info("获取课程资料:【{}】资料地址成功:{}", this.lessonId, this.resourceUrl);
                    ResourceLoader resourceLoader = new ResourceLoader(this.videoName, this.lessonId, this.weekId, this.resourceUrl);
                    resourceLoader.setBasePath(this.basePath);
                    mediaLoaders.add(resourceLoader);
                    latch.countDown();
                    COUNTER.incrementAndGet();
                }
            } else if (ResourceType.MEDIA.equals(this.type)) {
                log.info("获取视频信息URL:【{}】url：{}", this.lessonId, url);
                String strContent = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
                JSONObject jsonRespObject = JSONObject.parseObject(strContent);

                if (jsonRespObject.getInteger("state") != RespCode.SUCCESS) {
                    log.info("获取播放视频信息失败:【{}】,json:{}", this.videoName, strContent);
                    throw new RuntimeException("获取播放视频信息失败:" + jsonRespObject.getString("message"));
                }

                JSONObject mediaPlayInfoVo = jsonRespObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo");
                String aliPlayAuth = mediaPlayInfoVo.getString("aliPlayAuth");
                String fileId = mediaPlayInfoVo.getString("fileId");

                String playInfoRequestUrl = getPlayInfoRequestUrl(aliPlayAuth, fileId);
                String playInfoContent = HttpUtils.get(playInfoRequestUrl, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
                JSONObject playInfoJsonObject = JSONObject.parseObject(playInfoContent);

                JSONArray playInfoJsonArray = playInfoJsonObject.getJSONObject("PlayInfoList").getJSONArray("PlayInfo");
                List<PlayInfoVo> playInfoVoList = new ArrayList<>();
                if (!playInfoJsonArray.isEmpty()) {
                    playInfoVoList = playInfoJsonArray.toJavaList(PlayInfoVo.class);
                }

                if (!playInfoVoList.isEmpty()) {
                    PlayInfoVo playInfoVo = playInfoVoList.get(0);
                    if ("UNRELEASE".equals(playInfoVo.getStatus())) {
                        log.info("视频:【{}】待更新", this.videoName);
                        latch.countDown();
                        COUNTER.incrementAndGet();
                        return;
                    } else {
                        String playUrl = playInfoVo.getPlayURL();
                        if (playUrl != null) {
                            log.info("获取视频:【{}】播放地址成功:{}", this.videoName, playUrl);
                        }

                        if ("mp4".equals(playInfoVo.getFormat())) {
                            BigCourseMp4Downloader mp4Downloader = new BigCourseMp4Downloader(this.videoName, this.lessonId, playUrl);
                            mp4Downloader.setBasePath(this.basePath);
                            mediaLoaders.add(mp4Downloader);
                        }
                        latch.countDown();
                        COUNTER.incrementAndGet();
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取视频:【{}】信息失败:", this.videoName, e);
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
                log.info(" video:【{}】最大重试结束:{}", this.videoName, MAX_RETRY_COUNT);
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
