package online.githuboy.lagou.course.domain;

import lombok.Data;

/**
 * 课程详情
 *
 * @author suchu
 * @date 2021/6/1
 */
@Data
public class CourseLessonDetail {
    private String id;
    private String courseId;
    private String sectionId;
    /**
     * 标题
     */
    private String theme;
    private VideoMedia videoMedia;
    /**
     * 课程发布状态:RELEASE,UNRLEASE
     */
    private String status;
    /**
     * 课程文本内容
     */
    private String textContent;

    /**
     * 视频西悉尼
     */
    @Data
    public static class VideoMedia {
        private String id;
        private Integer channel;
        private Integer mediaType;
        private String fileId;
        private String fileUrl;
    }
}
