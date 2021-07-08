package online.githuboy.lagou.course.task.m3u8.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import online.githuboy.lagou.course.task.m3u8.M3U8Content;

/**
 * 实现了m3u8封装，解析等公共逻辑
 *
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

    /**
     * 实际加载m3u8内容的方法，需要子类重新
     *
     * @return
     */
    public abstract String loadInternal();

    /**
     * m3u8文件中的baseUrl，用于下载ts文件
     *
     * @return
     */
    public abstract String getBaseUrl();
}
