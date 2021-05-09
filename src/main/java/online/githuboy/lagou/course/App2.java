package online.githuboy.lagou.course;

import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.Course;
import online.githuboy.lagou.course.support.Downloader;

import java.io.IOException;
import java.util.List;

/**
 * 启动类
 *
 * @author suchu
 * @date 2020/12/11
 */
@Slf4j
public class App2 {
    public static void main(String[] args) throws IOException, InterruptedException {

        List<String> allCoursePurchasedRecordForPC = Course.getAllCoursePurchasedRecordForPC();

        allCoursePurchasedRecordForPC.remove("1");
        allCoursePurchasedRecordForPC.remove("287");
        allCoursePurchasedRecordForPC.remove("490");
        allCoursePurchasedRecordForPC.remove("615");
        allCoursePurchasedRecordForPC.remove("640");
        allCoursePurchasedRecordForPC.remove("668");
        allCoursePurchasedRecordForPC.remove("685");
        allCoursePurchasedRecordForPC.remove("716");
        allCoursePurchasedRecordForPC.remove("729");
        allCoursePurchasedRecordForPC.remove("753");
        allCoursePurchasedRecordForPC.remove("822");
        allCoursePurchasedRecordForPC.remove("837");
        allCoursePurchasedRecordForPC.remove("869"); // 这个视频有毒

        //视频保存的目录
        String savePath = "D:\\lagou";

//        Thread logThread = new Thread(() -> {
//            while (true) {
//                log.info("Thread pool:{}", ExecutorService.getExecutor());
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }, "log-thread");
//        logThread.setDaemon(true);

        for (String courseId : allCoursePurchasedRecordForPC) {
            Downloader downloader = new Downloader(courseId, savePath);
            downloader.start();
        }
        log.info("\n====>程序运行完成");

//        logThread.start();
    }
}
