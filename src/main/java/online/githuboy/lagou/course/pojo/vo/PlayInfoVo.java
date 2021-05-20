package online.githuboy.lagou.course.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class PlayInfoVo implements Serializable {
    private String Status;
    private String StreamType;
    private String Format;
    private String PlayURL;
    private String JobId;
}
