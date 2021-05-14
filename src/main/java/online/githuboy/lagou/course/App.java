package online.githuboy.lagou.course;

import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.CmdExecutor;
import online.githuboy.lagou.course.support.Downloader;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.domain.DownloadType;

import java.io.File;
import java.io.IOException;

/**
 * 启动类
 * 下载指定课程
 *
 * @author suchu
 * @date 2020/12/11
 */
@Slf4j
public class App {
    public static void main(String[] args) throws IOException, InterruptedException {

        try {
            int status = CmdExecutor.executeCmd(new File("."), "ffmpeg", "-version");
            log.debug("检查ffmpeg是否存在,{}", status);
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            // return;
        }
        //拉钩课程ID
        String courseId = "1";
        //视频保存的目录
//        String savePath = "D:\\lagou";
        String savePath = "course";
        Downloader downloader = new Downloader(courseId, savePath, DownloadType.ALL);
        Thread logThread = new Thread(() -> {
            while (true) {
                log.info("Thread pool:{}", ExecutorService.getExecutor());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("{}", e);
                }
            }

        }, "log-thread");
        logThread.setDaemon(true);
//        logThread.start();
        downloader.start();
        ExecutorService.tryTerminal();

    }
}
