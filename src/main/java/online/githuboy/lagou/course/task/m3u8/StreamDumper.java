package online.githuboy.lagou.course.task.m3u8;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.CmdExecutor;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.task.m3u8.support.M3U8ContentLoader;
import online.githuboy.lagou.course.utils.FileUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * m3u8流下载器
 *
 * @author suchu
 * @date 2021/6/23
 */
@Slf4j
public class StreamDumper implements Runnable {

    @Setter
    private File root;
    @Setter
    private M3U8ContentLoader m3U8ContentLoader;

    public StreamDumper(File root, M3U8ContentLoader loader) {
        this.root = root;
        this.m3U8ContentLoader = loader;
    }

    @SneakyThrows
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        M3U8Content m3u8 = m3U8ContentLoader.loadContent();
        //save original file
        FileUtils.save(m3u8.getRaw().getBytes(), new File(root, "1.m3u8"));
        String s = m3u8.rebuildM3U8Raw();
        //save rebuild file
        FileUtils.save(s.getBytes(), new File(root, "1_copy.m3u8"));
        //try save key file
        if (null != m3u8.getKey()) {
            byte[] key = m3u8.getKey();
            FileUtils.save(key, new File(root, m3u8.getLocalKeyName()));
        }
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failCounter = new AtomicInteger(0);
        //download ts
        CountDownLatch latch = new CountDownLatch(m3u8.getTsList().size());
        m3u8.getTsList().forEach(tsInfo -> {
            TsDownloader tsDownloader = new TsDownloader(latch, root, tsInfo.getUrl(), tsInfo.getIndexName());
            tsDownloader.setCompleteHandler(new TsDownloader.CompleteHandler() {
                @Override
                public void success() {
                    successCounter.incrementAndGet();
                }

                @Override
                public void error(Exception e) {
                    failCounter.incrementAndGet();
                }
            });
            ExecutorService.getHlsExecutor().submit(tsDownloader);
        });
        latch.await();
        if (successCounter.get() == m3u8.getTsList().size()) {
            log.info("所有TS下载完毕");
            //merge
            CmdExecutor.executeCmd(this.root, "ffmpeg", "-y", "-allowed_extensions", "ALL", "-loglevel", "repeat+level+trace", "-i", "1_copy.m3u8", "-c", "copy", "-bsf:a", "aac_adtstoasc", "1" + ".mp4");
            log.info("视频合并完毕,耗时: {}", (System.currentTimeMillis() - start) + " ms");
        } else {
            log.warn("TS没有全部下载成功，失败:{}", failCounter.get());
        }
        ExecutorService.getHlsExecutor().shutdown();
    }
}
