package online.githuboy.lagou.course.domain;

import lombok.Builder;
import lombok.Data;

/**
 * 解析后的课程信息
 *
 * @author eric
 */
@Data
@Builder
public class LessonInfo {
    private String lessonId;
    private String lessonName;
    private String appId;
    private String fileId;
    private String fileUrl;
    private String fileEdk;
}
