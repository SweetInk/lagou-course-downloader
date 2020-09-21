package online.githuboy.lagou.course.task;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.CookieStore;
import online.githuboy.lagou.course.ExecutorService;
import online.githuboy.lagou.course.MediaLoader;
import online.githuboy.lagou.course.Stats;
import online.githuboy.lagou.course.decrypt.AliPlayerDecrypt;
import online.githuboy.lagou.course.decrypt.PlayAuth;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static online.githuboy.lagou.course.decrypt.AliPlayerDecrypt.*;

/**
 * MP4下载器
 *
 * @author suchu
 * @date 2020/8/7
 */
@Builder
@Slf4j
public class MP4Downloader implements Runnable, NamedTask, MediaLoader {
    private static final String API_TEMPLATE = "https://gate.lagou.com/v1/neirong/kaiwu/getLessonPlayHistory?lessonId={0}&isVideo=true";

    private final static int maxRetryCount = 3;
    private String videoName;
    private String appId;
    private String fileId;
    private String fileUrl;
    private String lessonId;
    private volatile int retryCount = 0;
    @Setter
    private File basePath;

    private File workDir;
    @Setter
    private CountDownLatch latch;

    private void initDir() {
        String fileName = videoName.replaceAll("[\\\\:*?\"<>|]", "");
        workDir = new File(basePath, fileName.replaceAll("/", "_") + "_" + lessonId);
        if (!workDir.exists()) {
            workDir.mkdirs();
        }
    }

    @Override
    public void run() {
        initDir();
        String url = MessageFormat.format(API_TEMPLATE, this.lessonId);
        try {
            log.info("获取视频:{},信息，url：{}", lessonId, url);
            String body = HttpUtils.get(url, CookieStore.getCookie()).header("x-l-req-header", "{deviceType:1}").execute().body();
            JSONObject jsonObject = JSON.parseObject(body);
            System.out.println(body);
            if (jsonObject.getInteger("state") != 1) throw new RuntimeException(body);
            String aliPlayAuth = jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").getString("aliPlayAuth");
            String fileId = jsonObject.getJSONObject("content").getJSONObject("mediaPlayInfoVo").getString("fileId");
            AliPlayerDecrypt.EncryptedData d = AliPlayerDecrypt.authKeyToEncryptData(aliPlayAuth);
            String stringify = AliPlayerDecrypt.WordCodec.stringify(d);
            PlayAuth playAuth = PlayAuth.from(stringify);
            Map<String, String> publicParam = new HashMap<>();
            Map<String, String> privateParam = new HashMap<>();
            publicParam.put("AccessKeyId", playAuth.getAccessKeyId());
            publicParam.put("Timestamp", generateTimestamp());
            publicParam.put("SignatureMethod", "HMAC-SHA1");
            publicParam.put("SignatureVersion", "1.0");
            publicParam.put("SignatureNonce", generateRandom());
            publicParam.put("Format", "JSON");
            publicParam.put("Version", "2017-03-21");

            privateParam.put("Action", "GetPlayInfo");
            privateParam.put("AuthInfo", playAuth.getAuthInfo());
            privateParam.put("AuthTimeout", "7200");
            privateParam.put("Definition", "240");
            privateParam.put("PlayConfig", "{}");
            privateParam.put("ReAuthInfo", "{}");
            privateParam.put("SecurityToken", playAuth.getSecurityToken());
            privateParam.put("VideoId", fileId);
            List<String> allParams = getAllParams(publicParam, privateParam);
            String cqs = getCQS(allParams);
            String stringToSign =
                    "GET" + "&" +
                            percentEncode("/") + "&" +
                            percentEncode(cqs);
            byte[] bytes = hmacSHA1Signature(playAuth.getAccessKeySecret(), stringToSign);
            String signature = newStringByBase64(bytes);
            String queryString = cqs + "&Signature=" + signature;
            String api = "https://vod.cn-shanghai.aliyuncs.com/?" + queryString;
            String body1 = HttpRequest.get(api).execute().body();

            System.out.println(stringToSign);
            System.out.println(api);
//            System.out.println(stringify);
            System.out.println("\n\nAPI request result:\n\n" + body1);
            JSONObject mediaObj = JSON.parseObject(body1);
            if (mediaObj.getString("Code") != null) throw new RuntimeException("获取媒体信息失败");
            JSONObject playInfoList = mediaObj.getJSONObject("PlayInfoList");
            JSONArray playInfos = playInfoList.getJSONArray("PlayInfo");
            if (playInfos.size() > 0) {
                JSONObject playInfo = playInfos.getJSONObject(0);
                String mp4Url = playInfo.getString("PlayURL");
                log.info("解析到MP4播放地址:{}", mp4Url);
                HttpRequest.get(mp4Url).execute().writeBody(new File(workDir, videoName + ".mp4"), new StreamProgress() {
                    @Override
                    public void start() {
                        System.out.println("开始下载视频:" + videoName);
                    }

                    @Override
                    public void progress(long l) {
                    }

                    @Override
                    public void finish() {
                        System.out.println("视频下载完成:" + videoName);
                        Stats.remove(videoName);
                        latch.countDown();
                    }
                });
            }
            //latch.countDown();
        } catch (Exception e) {
            log.error("获取视频:{}信息失败:", videoName, e);
            if (retryCount < maxRetryCount) {
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
                log.info(" video:{}最大重试结束:{}", videoName, maxRetryCount);
                latch.countDown();
            }
        }
    }
}
