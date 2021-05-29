package online.githuboy.lagou.course.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class StageModuleVo implements Serializable {
    private Integer weekId;
    private String weekTag;
    private String weekName;
    private String weekDesc;
}
