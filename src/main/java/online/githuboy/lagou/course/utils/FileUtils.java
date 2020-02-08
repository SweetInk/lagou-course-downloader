package online.githuboy.lagou.course.utils;

import cn.hutool.core.io.FileUtil;

import java.io.File;

public class FileUtils {
    public static void save(byte[] bytes, File path) {
        FileUtil.writeBytes(bytes, path);
    }
}
