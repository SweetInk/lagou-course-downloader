package online.githuboy.lagou.course.task.m3u8.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.task.m3u8.M3U8Content;

/**
 * @author suchu
 * @date 2021/7/7
 */
@Slf4j
public abstract class AbstractM3U8ContentLoader implements M3U8ContentLoader {
    @Override
    public M3U8Content loadContent() {
        String content = loadInternal();
        if (StrUtil.isBlank(content)) {
            throw new RuntimeException("无法获取m3u8内容");
        }
        String baseUrl = getBaseUrl();
        M3U8Content m3u8 = new M3U8Content(baseUrl, content);
        //try parse key
        if (StrUtil.isNotBlank(m3u8.getKeyUrl())) {
            byte[] key = HttpUtil.downloadBytes(m3u8.getKeyUrl());
            m3u8.setKey(key);
        }
        return m3u8;
    }

    public abstract String loadInternal();

    public abstract String getBaseUrl();
}
