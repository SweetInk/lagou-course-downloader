package online.githuboy.lagou.course.utils;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class FileUtils {
    public static void save(byte[] bytes, File path) {
        FileUtil.writeBytes(bytes, path);
    }

    public static String getCorrectFileName(String originFileName) {
        return originFileName.replaceAll("[\\\\s/:*?\"<>|]",
                "");
    }

    /**
     * 不存在就新建文件
     */
    public static void createNewFile(String filepath) {
        File file = new File(filepath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeFile(File workdir, String pathname, String text) {
        try {
            File writeName = new File(workdir, pathname);
            if (!writeName.exists()) {
                try {
                    writeName.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try (
                    FileWriter writer = new FileWriter(writeName, true);
                    BufferedWriter out = new BufferedWriter(writer)
            ) {
                out.write(text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void replaceFileName(File file, String searchStr, String destStr) {
        File destFile = new File(StringUtils.replace(file.getPath(), searchStr, destStr));
        try {
            Files.deleteIfExists(destFile.toPath());
            file.renameTo(destFile);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void delete(File path, String suffix) {
        File[] files = path.listFiles();
        for (File file : files) {
            if (!file.isDirectory() && file.getName().endsWith(suffix)) {
                boolean delete = file.delete();
                if (!delete)
                    System.out.println(file.getAbsoluteFile().getPath() + " 删除失败");
            }
        }
    }

    public static void main(String[] args) {
        String str = "\\\\/////////////A:*B??:::\"<C\">||||";
        String r = str.replaceAll("[\\\\s/:*?\"<>|]", "");
        System.out.println(FileUtils.getCorrectFileName("01 | Spring Data JPA 初识"));
        System.out.println(r);
    }

}
