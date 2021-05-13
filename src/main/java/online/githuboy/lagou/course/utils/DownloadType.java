package online.githuboy.lagou.course.utils;

/**
 * download type for video or text
 * @author eric
 */

public enum DownloadType {
    VIDEO(0,"video"),
//    TEXT(1,"text"),
    ALL(3,"video and ");

    int code;
    String description;

    private DownloadType(int code,String description){
        this.code = code;
        this.description = description;
    }

}
