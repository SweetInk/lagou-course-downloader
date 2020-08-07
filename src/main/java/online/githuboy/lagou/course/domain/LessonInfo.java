package online.githuboy.lagou.course.domain;

import lombok.Builder;
import lombok.Data;

@Data
//@AllArgsConstructor
@Builder
public class LessonInfo {
    private String lessionId;
    private String lessonName;
    private String appId;
    private String fileId;
    private String fileUrl;
    private String fileEdk;
}
