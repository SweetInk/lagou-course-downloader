package online.githuboy.lagou.course.domain;

import cn.hutool.core.util.StrUtil;

/**
 * download type for video or text
 *
 * @author eric
 */
public enum DownloadType {
    VIDEO(0, "下载视频"),
    TEXT(1,"text"),
    ALL(3, "下载视频和文档")
    ;

    int code;
    String description;

    private DownloadType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean needVideo(DownloadType downloadType) {
        if (downloadType == DownloadType.ALL || downloadType == DownloadType.VIDEO) {
            return true;
        }
        return false;
    }

    public static boolean needText(DownloadType downloadType) {
        if (downloadType == DownloadType.ALL || downloadType == DownloadType.TEXT) {
            return true;
        }
        return false;
    }

    public static DownloadType loadByCode(Integer code) {
        DownloadType[] values = values();
        for (DownloadType value : values) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
