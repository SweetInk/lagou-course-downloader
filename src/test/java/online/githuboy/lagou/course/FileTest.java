package online.githuboy.lagou.course;

import cn.hutool.core.io.FileUtil;
import online.githuboy.lagou.course.utils.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class FileTest {

    /**
     * 将文章，单独拷贝到一个目录
     */
    @Test
    public void copyDoc() {
        List<File> mp4_dir = FileUtil.loopFiles(ConfigUtil.readValue("mp4_dir"));

        mp4_dir.stream().filter(file -> file.getName().contains(".md"))
                .forEach(
                        file -> {
                            String path = file.getPath();
                            path = StringUtils.replace(path, "/文档", "");
                            path = StringUtils.replace(path, "拉钩教育-全部专栏(不包含前端)", "拉钩教育-全部专栏(不包含前端)文档");
                            System.out.println(path);
                            FileUtil.copyFile(file, new File(path));
                        }
                );
    }

}
