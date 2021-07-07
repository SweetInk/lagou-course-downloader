package online.githuboy.lagou.course.task.m3u8.support;

import cn.hutool.core.io.FileUtil;
import lombok.AllArgsConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author suchu
 * @date 2021/7/7
 */
@AllArgsConstructor
public class LocalFileM3U8ContentLoader extends AbstractM3U8ContentLoader {
    private final File file;

    @Override
    public String loadInternal() {
        String content = FileUtil.readString(file, StandardCharsets.UTF_8);
        return content;
    }

    @Override
    public String getBaseUrl() {
        return "";
    }
}
