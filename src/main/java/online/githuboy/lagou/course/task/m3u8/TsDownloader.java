package online.githuboy.lagou.course.task.m3u8;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.utils.FileUtils;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * TS片段下载器
 *
 * @author suchu
 * @date 2021/6/23
 */
@Slf4j
public class TsDownloader implements Runnable {
    private final CountDownLatch latch;
    private final File root;
    private final String url;
    private final String fileName;
    @Setter
    private CompleteHandler completeHandler;

    public TsDownloader(CountDownLatch latch, File root, String url, String fileName) {
        this.latch = latch;
        this.root = root;
        this.url = url;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try {
            File tsFile = new File(root, fileName);
            if (tsFile.exists() && tsFile.length() > 0) {
                return;
            }
            byte[] content = HttpUtils.getContent(url);
            FileUtils.save(content, new File(root, fileName));
            log.info("ts文件:{}下载完成", url);
            if (null != completeHandler)
                completeHandler.success();
        } catch (Exception e) {
            if (null != completeHandler) completeHandler.error(e);
            log.error("ts文件:{}下载出错", url, e);
        } finally {
            latch.countDown();
        }
    }

    public interface CompleteHandler {
        default void success() {
        }

        default void error(Exception e) {
        }
    }

}
