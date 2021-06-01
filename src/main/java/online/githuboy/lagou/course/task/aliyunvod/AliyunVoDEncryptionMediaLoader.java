package online.githuboy.lagou.course.task.aliyunvod;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.decrypt.alibaba.EncryptUtils;
import online.githuboy.lagou.course.domain.AliyunVodPlayInfo;
import online.githuboy.lagou.course.request.HttpAPI;
import online.githuboy.lagou.course.support.AbstractRetryTask;
import online.githuboy.lagou.course.support.CmdExecutor;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.support.MediaLoader;
import online.githuboy.lagou.course.task.NamedTask;
import online.githuboy.lagou.course.utils.FileUtils;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

/**
 * 阿里云私有加密m3u8视频下载
 *
 * @author suchu
 * @since 2019年8月3日
 */
@Slf4j
public class AliyunVoDEncryptionMediaLoader extends AbstractRetryTask implements NamedTask, MediaLoader {
    private final static int maxRetryCount = 3;
    private final File baseFilePath;
    private final String fileName;
    private final String fileId;
    private final String playAuth;
    CountDownLatch latch;
    CountDownLatch hlsLatch;
    private List<String> hsList;
    /**
     * m3u8文件内容
     */
    private String raw;
    /**
     * m3u8 baseUrl
     */
    private String baseUrl = "";
    private int retryCount = 0;
    private String key;

    public AliyunVoDEncryptionMediaLoader(String playAuth, String fileName, String savePath, String fileId) {
        this.playAuth = playAuth;
        this.fileName = FileUtils.getCorrectFileName(fileName);
        this.fileId = fileId;
        baseFilePath = new File(savePath, FileUtils.getCorrectFileName(this.fileName + "_" + this.fileId + ""));
        if (!baseFilePath.exists())
            baseFilePath.mkdirs();
    }

    public File getBaseFilePath() {
        return baseFilePath;
    }

    @Override
    protected void action() {
        this.load();
    }

    @Override
    public boolean canRetry() {
        return retryCount < maxRetryCount;
    }

    @Override
    public void retryComplete() {
        super.retryComplete();
        log.info(" video:{}最大重试结束:{}", fileName, maxRetryCount);
        latch.countDown();
    }

    @Override
    protected void retry(Throwable throwable) {
        super.retry(throwable);
        log.error("下载m3u8视频数据失败：", throwable);
        retryCount += 1;
        log.info("第:{}次重试获取:{}", retryCount, fileName);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        ExecutorService.execute(this);
    }

    @Override
    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    @SneakyThrows
    public void load() {
        String rand = "test";
        String encryptRand = EncryptUtils.encryptRand(rand);
        AliyunVodPlayInfo vodPlayerInfo = HttpAPI.getVodPlayerInfo(encryptRand, playAuth, fileId);
        if (null == vodPlayerInfo) {
            throw new RuntimeException("获取阿里云视频播放信息失败");
        }
        String playURL = vodPlayerInfo.getPlayURL();
        if (!playURL.contains("m3u8")) {
            throw new RuntimeException("playURL不是m3u8");
        }
        this.key = EncryptUtils.decrypt(rand, vodPlayerInfo.getRand(), vodPlayerInfo.getPlaintext());
        byte[] m3u8ContentBytes = HttpUtils.getContent(playURL);
        log.info("获取视频:{},m3u8文件:{} 内容成功", fileName, playURL);
        FileUtils.save(m3u8ContentBytes, new File(this.baseFilePath, "video_origin.m3u8"));
        this.raw = new String(m3u8ContentBytes);
        this.baseUrl = playURL.substring(0, playURL.lastIndexOf("/") + 1);
        hsList = new ArrayList<>(16);
        this.parse();//D:\lagou\490_Spring Data JPA 原理与实战\01 | Spring Data JPA 初识_dfa387a5de484840bc6d35d55966b05d
        latch.countDown();
        //TODO 清理ts && m3u8文件
    }

    private void parse() {
        StringTokenizer tokenizer = new StringTokenizer(raw, "\n");
        StringBuilder sb = new StringBuilder(512);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!token.startsWith("#EXT-X-KEY"))
                sb.append(token).append("\n");
            if (token.startsWith("#EXTINF")) {
                if (tokenizer.hasMoreTokens()) {
                    String nextToken = tokenizer.nextToken();
                    hsList.add(baseUrl + nextToken);
                    sb.append(nextToken).append("\n");
                }
            }
        }
        this.raw = sb.toString();
        hlsLatch = new CountDownLatch(hsList.size());
        List<String> localHsList = new ArrayList<>(hsList.size());
        hsList.forEach(hsUrl -> {
            AliyunVodEncryptionHsDownloader downloader = new AliyunVodEncryptionHsDownloader(this, hsUrl, this.key);
            downloader.setLatch(hlsLatch);
            String fileName = downloader.getFileName();
            localHsList.add(fileName);
            ExecutorService.getHlsExecutor().execute(downloader);
        });
        mergeHsToMp4();

    }

    private void mergeHsToMp4() {
        try {
            hlsLatch.await();
            log.info("视频:{} HS 片段下载完成 total:{}", fileName, hsList.size());
            CmdExecutor.executeCmd(this.baseFilePath, "ffmpeg", "-y", "-allowed_extensions", "ALL", "-i", "video1.m3u8", "-c", "copy", "-bsf:a", "aac_adtstoasc", fileName + ".mp4");
            log.info("视频:{} HS片段合并完成", fileName);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }


    //ffmpeg -y -allowed_extensions ALL -i video1.m3u8 -c copy -bsf:a aac_adtstoasc out.mp4
    @Override
    public String getTaskDescription() {
        try {
            return fileName + " -> hls_size:" + hsList.size();
        } catch (Exception e) {
            System.out.println("fileName exception:" + fileName);
            throw e;
        }
    }
}
