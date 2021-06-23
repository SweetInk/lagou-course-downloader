package online.githuboy.lagou.course.task.m3u8;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.Setter;
import lombok.SneakyThrows;
import online.githuboy.lagou.course.support.CmdExecutor;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.utils.FileUtils;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * m3u8流下载器
 *
 * @author suchu
 * @date 2021/6/23
 */
public class StreamDumper implements Runnable {
    @Setter
    private String m3u8Url;
    @Setter
    private File root;

    public static void main(String[] args) {
        File root = new File("D:\\Aproject\\ddDump\\s2");
        if (!root.exists()) {
            root.mkdirs();
        }
        StreamDumper dumper = new StreamDumper();
        dumper.setM3u8Url("https://1252524126.vod2.myqcloud.com/2919df88vodtranscq1252524126/e28c32d23701925919770850299/v.f146750.m3u8");
        dumper.setRoot(root);
        new Thread(dumper).start();
    }

    @SneakyThrows
    @Override
    public void run() {
        URL url = new URL(m3u8Url);
        String urlPath = url.getPath();
        long start = System.currentTimeMillis();
        String substring = urlPath.substring(0, urlPath.lastIndexOf('/'));
        byte[] content = HttpUtils.getContent(m3u8Url);
        FileUtils.save(content, new File(root, "1.m3u8"));
        M3U8Content m3u8 = new M3U8Content(url.getProtocol() + "://" + url.getHost() + substring, new String(content, StandardCharsets.UTF_8));
        String s = m3u8.rebuildM3U8Raw();
        FileUtils.save(s.getBytes(), new File(root, "1_copy.m3u8"));
        //try parse key
        if (StrUtil.isNotBlank(m3u8.getKeyUrl())) {
            byte[] key = HttpUtil.downloadBytes(m3u8.getKeyUrl());
            FileUtils.save(key, new File(root, m3u8.getLocalKeyName()));
        }
        //download ts
        CountDownLatch latch = new CountDownLatch(m3u8.getTsList().size());
        m3u8.getTsList().forEach(tsInfo -> {
            ExecutorService.getHlsExecutor().submit(new TsDownloader(latch, root, tsInfo.getUrl(), tsInfo.getIndexName()));
        });
        latch.await();
        System.out.println("所有TS下载完毕");
        //merge

        CmdExecutor.executeCmd(this.root, "ffmpeg", "-y", "-allowed_extensions", "ALL", "-loglevel", "repeat+level+trace", "-i", "1_copy.m3u8", "-c", "copy", "-bsf:a", "aac_adtstoasc", "1" + ".mp4");
        System.out.println("视频合并完毕,耗时:" + (System.currentTimeMillis() - start) + " ms");
        ExecutorService.getHlsExecutor().shutdown();
    }
}
