package online.githuboy.lagou.course.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BigCourseLessonDto {
    private String courseId;
    private String stageId; // part
    private String weekId;  // module
    private String dayId; // sub module
    private String lessonId;
    private String lessonName;
    private String videoName;
    private String pathName;
    private String type;
    private String resourceUrl;
}
