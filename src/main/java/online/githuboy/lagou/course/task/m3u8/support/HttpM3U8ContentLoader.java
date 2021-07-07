package online.githuboy.lagou.course.task.m3u8.support;

import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.net.URL;

public class HttpM3U8ContentLoader extends AbstractM3U8ContentLoader {
    private final String m3u8Url;
    private String baseUrl;

    public HttpM3U8ContentLoader(String m3u8Url) {
        this.m3u8Url = m3u8Url;
    }

    @SneakyThrows
    @Override
    public String loadInternal() {
        if (StrUtil.isBlank(m3u8Url))
            throw new IllegalArgumentException("url不能为空");
        URL url = new URL(m3u8Url);
        String urlPath = url.getPath();
        String substring = urlPath.substring(0, urlPath.lastIndexOf('/'));
        byte[] content = HttpUtils.getContent(m3u8Url);
        this.baseUrl = url.getProtocol() + "://" + url.getHost() + substring;
        return new String(content);
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrl;
    }
}