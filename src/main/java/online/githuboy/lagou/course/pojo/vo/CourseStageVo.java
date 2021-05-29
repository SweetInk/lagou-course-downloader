package online.githuboy.lagou.course.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class CourseStageVo implements Serializable {
    private Integer stageId;
    private String stageName;
    private String stageDesc;
}
