package online.githuboy.lagou.course.task;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.CmdExecutor;
import online.githuboy.lagou.course.support.CookieStore;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.support.MediaLoader;
import online.githuboy.lagou.course.utils.ConfigUtil;
import online.githuboy.lagou.course.utils.FileUtils;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * m3u8视频下载
 *
 * @author suchu
 * @since 2019年8月3日
 */
@Slf4j
@Deprecated
public class M3U8MediaLoader implements NamedTask, MediaLoader {
    private final static Pattern pattern = Pattern.compile("(URI=\")(.*)(\")");
    private final static int maxRetryCount = 3;
    CountDownLatch latch;
    /**
     * m3u8视频地址
     */
    private final String url;
    private List<String> hsList;
    /**
     * m3u8文件内容
     */
    private String raw;
    /**
     * m3u8 baseUrl
     */
    private String baseUrl = "";
    /**
     * 视频解密key byte array
     */
    private byte[] key;
    private String keyUrlPath;
    private final File baseFilePath;
    private final String fileName;
    private final String fileId;
    private int retryCount = 0;
    @Setter
    private String url2;

    public M3U8MediaLoader(String m3u8Url, String fileName, String savePath, String fileId) {
        this.url = m3u8Url;
        this.fileName = fileName;
        this.fileId = fileId;
        baseFilePath = new File(savePath, this.fileName + "_" + this.fileId + "");
        if (!baseFilePath.exists())
            baseFilePath.mkdirs();
    }

    public File getBaseFilePath() {
        return baseFilePath;
    }

    public byte[] getKey() {
        return key;
    }

    /**
     * 获取视频加密key
     *
     * @throws IOException
     */
    private boolean tryGetVideoKey() throws IOException {
        Matcher matcher = pattern.matcher(this.raw);
        while (matcher.find()) {
            String group = matcher.group(2);
            if (null != group) {
                keyUrlPath = group;
                break;
            }
        }
        if (null != keyUrlPath) {
            log.info("解析到视频:{},加密key url:{}", fileName, keyUrlPath);
            try {
                byte[] bytes = HttpUtils.getContentWithCookie(keyUrlPath, CookieStore.getCookie());
                this.key = bytes;
                if (bytes.length > 0) {
                    File keySavePath = new File(baseFilePath, "video.key");
                    FileUtils.save(key, keySavePath);
                    log.info("视频解密key下载成功:savePath:{}", keySavePath.getAbsolutePath());
                    return true;
                } else {
                    log.error("获取视频:{},解密key url：{}\n失败:{}", fileName, keyUrlPath, "可能cookie过期或者没有购买视频");
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("获取视频解密Key失败：" + e.getMessage());
            }
        }
        return true;
    }

    public void load() throws IOException {
        byte[] download = HttpUtils.getContent(url);
        log.info("获取视频:{},m3u8文件:{} 内容成功", fileName, url);
        FileUtils.save(download, new File(this.baseFilePath, "video_origin.m3u8"));
        FileUtils.save(HttpUtils.getContent(url2), new File(this.baseFilePath, "video_encrypted.m3u8"));
        this.raw = new String(download);
        this.baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
        hsList = new ArrayList<>(16);
        this.parse();
    }

    private void parse() throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(raw, "\n");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("#EXTINF")) {
                if (tokenizer.hasMoreTokens()) {
                    hsList.add(baseUrl + tokenizer.nextToken());
                }
            }
        }
        if (!tryGetVideoKey()) {
            return;
        }
        latch = new CountDownLatch(hsList.size());
        List<String> localHsList = new ArrayList<>(hsList.size());
        hsList.forEach(hsUrl -> {
            HsDownloader downloader = new HsDownloader(this, hsUrl);
            downloader.setLatch(latch);
            String fileName = downloader.getFileName();
            localHsList.add(fileName);
            ExecutorService.getHlsExecutor().execute(downloader);
        });
        rebuildM3U8(localHsList);
        mergeHsToMp4();

    }

    private void mergeHsToMp4() {
        try {
            latch.await();
            log.info("视频:{} HS 片段下载完成 total:{}", fileName, hsList.size());
            CmdExecutor.executeCmd(this.baseFilePath, "ffmpeg", "-y", "-allowed_extensions", "ALL", "-i", "video1.m3u8", "-c", "copy", "-bsf:a", "aac_adtstoasc", fileName + ".mp4");
            log.info("视频:{} HS片段合并完成", fileName);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void rebuildM3U8(List<String> localHsList) throws IOException {
        String s = raw.replaceAll("(URI=\")(.*)(\")", "$1video.key$3");
        StringBuilder stringBuilder = new StringBuilder();
        StringTokenizer newTokenizer = new StringTokenizer(s, "\n");
        int index = 0;
        while (newTokenizer.hasMoreTokens()) {
            String token = newTokenizer.nextToken();
            stringBuilder.append(token).append("\n");
            if (token.startsWith("#EXTINF")) {
                if (newTokenizer.hasMoreTokens()) {
                    String nextToken = newTokenizer.nextToken();
                    if (index < localHsList.size()) {
                        stringBuilder.append(localHsList.get(index)).append("\n");
                        index++;
                    } else {
                        stringBuilder.append(nextToken).append("\n");
                    }
                }
            }
        }
        FileUtils.save(stringBuilder.toString().getBytes(), new File(this.baseFilePath, "video1.m3u8"));
    }

    //ffmpeg -y -allowed_extensions ALL -i video1.m3u8 -c copy -bsf:a aac_adtstoasc out.mp4

    @Override
    public void run() {
        try {
            this.load();
        } catch (Exception e) {
            log.error("下载m3u8视频数据失败：", e);
            if (retryCount < maxRetryCount) {
                retryCount += 1;
                log.info("第:{}次重试获取:{}", retryCount, fileName);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                ExecutorService.execute(this);
            } else {
                //ConfigUtil.addRetryCourse();
                log.info(" video:{}  最大重试结束:{}", fileName, maxRetryCount);
            }
        }
    }

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
