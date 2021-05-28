package online.githuboy.lagou.course.domain;

/**
 * download type for video or text
 *
 * @author eric
 */
public enum DownloadType {
    VIDEO(0, "下载视频"),
    //    TEXT(1,"text"),
    ALL(3, "下载视频和文档");

    int code;
    String description;

    private DownloadType(int code, String description) {
        this.code = code;
        this.description = description;
    }

}
