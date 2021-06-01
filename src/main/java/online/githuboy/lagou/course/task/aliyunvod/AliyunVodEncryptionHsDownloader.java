package online.githuboy.lagou.course.task.aliyunvod;

import cn.hutool.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.decrypt.alibaba.TSParser;
import online.githuboy.lagou.course.task.NamedTask;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * M3U8视频片段下载器
 * //TODO 下载失败重试
 *
 * @author suchu
 * @since 2019年8月2日
 */
@Slf4j
public class AliyunVodEncryptionHsDownloader implements Runnable, NamedTask {
    private final String url;
    private final AliyunVoDEncryptionMediaLoader m3U8;
    private final Map<String, String> params = new HashMap<>();
    private final String key;
    private CountDownLatch latch;
    private String fileName;

    public AliyunVodEncryptionHsDownloader(AliyunVoDEncryptionMediaLoader m3U8, String url, String key) {
        this.url = url;
        this.m3U8 = m3U8;
        this.key = key;
        URI uri = null;
        try {
            uri = new URI(this.url);
            if (null != uri.getQuery())
                Arrays.stream(uri.getQuery().split("&")).forEach(in -> {
                    if (null != in && in.length() > 0) {
                        String[] split = in.split("=");
                        if (split.length >= 2) {
                            params.put(split[0], split[1]);
                        }
                    }
                });
            String tempFileName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
            fileName = tempFileName;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public void run() {
        try {
            byte[] download = HttpUtils.getContent(this.url);
            TSParser parser = new TSParser();
            TSParser.TSStream tsStream = parser.fromByteArray(download);
            parser.decrypt(tsStream, HexUtil.decodeHex(key));
            tsStream.dumpToFile(new File(m3U8.getBaseFilePath(), fileName));
            latch.countDown();
        } catch (IOException e) {
            latch.countDown();
            log.info("ts文件下载失败:{}", this.url, e);
        }
    }

    @Override
    public String getTaskDescription() {
        return fileName;
    }
}
