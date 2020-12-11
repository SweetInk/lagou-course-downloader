package online.githuboy.lagou.course.utils;

import cn.hutool.core.io.FileUtil;

import java.io.File;

public class FileUtils {
    public static void save(byte[] bytes, File path) {
        FileUtil.writeBytes(bytes, path);
    }

    public static String getCorrectFileName(String originFileName) {
        return originFileName.replaceAll("[\\\\s/:*?\"<>|]",
                "");
    }

    public static void main(String[] args) {
        String str = "\\\\/////////////A:*B??:::\"<C\">||||";
        String r = str.replaceAll("[\\\\s/:*?\"<>|]", "");
        System.out.println(FileUtils.getCorrectFileName("01 | Spring Data JPA 初识"));
        System.out.println(r);
    }
}
