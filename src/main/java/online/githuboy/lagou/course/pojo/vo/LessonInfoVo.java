package online.githuboy.lagou.course.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class LessonInfoVo implements Serializable {
    private Integer lessonId;
    private String lessonName;
    private String fileId;
    private String fileUrl;
    private String encryptedFileId;
    private String type;
    private Integer lessonDayId;
    private String resourceUrl;
}
