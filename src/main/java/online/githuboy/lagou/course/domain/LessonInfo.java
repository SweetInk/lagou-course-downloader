package online.githuboy.lagou.course.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LessonInfo {
    private String lessonName;
    private String appId;
    private String fileId;
}
