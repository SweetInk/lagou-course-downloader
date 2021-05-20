package online.githuboy.lagou.course.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CourseDayInfoVo implements Serializable {
    private Integer dayId;
    private String dayName;
    private List<LessonInfoVo> lessonInfoVos;
}
