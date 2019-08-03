package online.githuboy.lagou.course.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.ExecutorService;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 视频metaInfo 加载器
 *
 * @author suchu
 * @since 2019年8月3日
 */
@Slf4j
public class VideoInfoLoader implements Runnable {
    /**
     * 0-> appId
     * 1-> fileId;
     */
    private static final String API_TEMPLATE = "https://playvideo.qcloud.com/getplayinfo/v2/{0}/{1}";
    private final static int maxRetryCount = 3;
    private String videoName;
    private String appId;
    private String fileId;
    private int retryCount = 0;
    @Setter
    private File basePath;

    @Setter
    private List<M3U8MediaLoader> m3U8MediaLoaders;

    @Setter
    private CountDownLatch latch;

    public VideoInfoLoader(String videoName, String appId, String fileId) {
        this.videoName = videoName;
        this.appId = appId;
        this.fileId = fileId;
    }

    @Override
    public void run() {
        String url = MessageFormat.format(API_TEMPLATE, this.appId, this.fileId);
        try {
            log.info("获取视频:{},信息，url：{}", videoName, url);
//            int j = 1 / 0;
            byte[] content = HttpUtils.getContent(url);
            String videoJson = new String(content);
            JSONObject json = JSON.parseObject(videoJson);
            if (json.getInteger("code") != 0) {
                log.info("视频:{},json：{}", videoName, videoJson);
                throw new RuntimeException("获取视频信息失败:" + json.getString("message"));
            }
            JSONObject videoInfo = json.getJSONObject("videoInfo");
            JSONArray transcodeList = videoInfo.getJSONArray("transcodeList");
            if (transcodeList.size() > 0) {
                JSONObject o = transcodeList.getJSONObject(transcodeList.size() - 1);
                String m3u8Url = o.getString("url");
                log.info("获取视频:{},m3u8地址成功:{}", videoName, m3u8Url);
                latch.countDown();
                M3U8MediaLoader m3U8 = new M3U8MediaLoader(m3u8Url, videoName, basePath.getAbsolutePath(), fileId);
                m3U8MediaLoaders.add(m3U8);
//                ExecutorService.execute(m3U8);
            }
        } catch (Exception e) {
            log.error("获取视频:{}信息失败:", videoName, e);
            if (retryCount < maxRetryCount) {
                retryCount += 1;
                log.info("第:{}次重试获取:{}", retryCount, videoName);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                ExecutorService.execute(this);
            } else {
                log.info(" video:{}最大重试结束:{}", videoName, maxRetryCount);
                latch.countDown();
            }


        }
    }
}
