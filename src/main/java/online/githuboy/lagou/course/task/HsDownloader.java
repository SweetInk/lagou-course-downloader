package online.githuboy.lagou.course.task;

import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * M3U8视频片段下载器
 *
 * @author suchu
 * @since 2019年8月2日
 */
public class HsDownloader implements Runnable {
    CountDownLatch latch;
    private String url;
    private M3U8MediaLoader m3U8;
    private Map<String, String> params = new HashMap<>();
    private String fileName;

    public HsDownloader(M3U8MediaLoader m3U8, String url) {
        this.url = url;
        this.m3U8 = m3U8;
        URI uri = null;
        try {
            uri = new URI(this.url);
            Arrays.stream(uri.getQuery().split("&")).forEach(in -> {
                if (null != in && in.length() > 0) {
                    String[] split = in.split("=");
                    if (split.length >= 2) {
                        params.put(split[0], split[1]);
                    }
                }
            });
            String tempFileName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
            fileName = params.get("start") + "_" + params.get("end") + "_" + tempFileName;
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
            FileOutputStream fos = new FileOutputStream(new File(m3U8.getBaseFilePath(), fileName));
            fos.write(download);
            fos.flush();
            fos.close();
            latch.countDown();
        } catch (IOException e) {
            latch.countDown();
            e.printStackTrace();
        }

    }
}
