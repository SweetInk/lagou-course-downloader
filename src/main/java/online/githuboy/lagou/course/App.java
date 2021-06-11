package online.githuboy.lagou.course;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.support.CmdExecutor;
import online.githuboy.lagou.course.support.Course;
import online.githuboy.lagou.course.support.Downloader;
import online.githuboy.lagou.course.support.ExecutorService;
import online.githuboy.lagou.course.domain.DownloadType;
import online.githuboy.lagou.course.utils.ConfigUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 启动类
 * 批量下载，下载账号下所有视频
 *
 * @author eric
 * @date 2021/05/09
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

        List<String> allCoursePurchasedRecordForPC = CollectionUtil.isNotEmpty(ConfigUtil.getCourseIds()) ?
                ConfigUtil.getCourseIds() :
                Course.getAllCoursePurchasedRecordForPC();

        allCoursePurchasedRecordForPC.removeAll(ConfigUtil.getDelCourse());

        //视频保存的目录
        String savePath = ConfigUtil.readValue("mp4_dir");

        // 开始下载所有课程
        int i = 1;
        for (String courseId : allCoursePurchasedRecordForPC) {
            Downloader downloader = new Downloader(courseId, savePath,
                    DownloadType.loadByCode(Integer.valueOf(ConfigUtil.readValue("downloadType"))));
            downloader.start();
            log.info("\n\n\n");
            log.info("开始下载{}课程", i++);
//            Thread.sleep(5000);
        }
        log.info("\n====>程序运行完成");
        ExecutorService.tryTerminal();

    }
}
