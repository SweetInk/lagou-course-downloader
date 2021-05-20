package online.githuboy.lagou.course;

import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.BigCourseDownloader;
import online.githuboy.lagou.course.support.ExecutorService;

import java.io.IOException;

/**
 * 启动类
 * 下载训练营课程
 *
 * @author jack
 * @since 2021年5月20日
 */
@Slf4j
public class App3 {
    public static void main(String[] args) throws IOException, InterruptedException {

        // 拉钩训练营课程ID
        String courseId = "33";
        //  视频保存的目录
        String savePath = "E:\\lagou";

        BigCourseDownloader downloader = new BigCourseDownloader(courseId, savePath);

        Thread logThread = new Thread(() -> {
            while (true) {
                log.info("Thread pool:{}", ExecutorService.getExecutor());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "log-thread");

        logThread.setDaemon(true);
        downloader.start();
        ExecutorService.tryTerminal();
    }
}
