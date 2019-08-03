package online.githuboy.lagou.course;

import online.githuboy.lagou.course.utils.PublicUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 执行命令脚本
 *
 * @author suchu
 * @since 2019年8月2日
 */
public class CmdExecutor {

    public static int executeCmd(File workPath, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.directory(workPath);
        builder.redirectErrorStream(true);
        Process exec = builder.start();
        InputStream inputStream = exec.getInputStream();
        PublicUtils.dumpInputStream(inputStream);
        int i = exec.waitFor();
        inputStream.close();
        return i;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        //ffmpeg -allowed_extensions ALL -i d:\lagou\0\video1.m3u8 -c copy -bsf:a aac_adtstoasc ALL.mp4
        ProcessBuilder builder = new ProcessBuilder("ffmpeg", "-y", "-allowed_extensions", "ALL", "-i", "video1.m3u8", "-c", "copy", "-bsf:a", "aac_adtstoasc", "d:\\lagou\\32_java_skills\\ALL_TEST_FUCK.mp4");
        builder.directory(new File("d:\\lagou\\0\\"));
        builder.redirectErrorStream(true);
        Process exec = builder.start();
        InputStream inputStream = exec.getInputStream();
        PublicUtils.dumpInputStream(inputStream);
        int i = exec.waitFor();
        inputStream.close();
        System.out.println(i);
    }
}
