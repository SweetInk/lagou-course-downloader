package online.githuboy.lagou.course.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author eric
 */
public class ReadTxt {

    private static final Logger logger = LoggerFactory.getLogger(ReadTxt.class);

    /**
     * 读入TXT文件
     */
    public Set<String> readFile(String pathname) {
        Set<String> set = new HashSet<>();
        FileUtils.createNewFile(pathname);
        //防止文件建立或读取失败，用catch捕捉错误并打印，也可以throw;
        //不关闭文件会导致资源的泄露，读写文件都同理
        //Java7的try-with-resources可以优雅关闭文件，异常时自动关闭文件；详细解读https://stackoverflow.com/a/12665271
        try (FileReader reader = new FileReader(pathname);
             BufferedReader br = new BufferedReader(reader)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                // 一次读入一行数据
                set.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return set;
    }

    /**
     * 写入TXT文件
     */
    public void writeFile(String pathname, String id) {
        try {
            File writeName = new File(pathname);
            FileUtils.createNewFile(pathname);
            try (FileWriter writer = new FileWriter(writeName, true);
                 BufferedWriter out = new BufferedWriter(writer)
            ) {
                out.append(id + "\r\n"); // \r\n即为换行
                out.flush(); // 把缓存区内容压入文件
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
