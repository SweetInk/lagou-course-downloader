package online.githuboy.lagou.course.domain;

import lombok.Data;

/**
 * @author suchu
 * @date 2021/6/1
 */
@Data
public class PlayHistory {

    /**
     * 阿里云点播的playAuth
     */
    private String aliPlayAuth;

    private Boolean encryptMedia = false;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 媒体渠道：ALIYUN、腾讯云
     */
    private String mediaChannel;
}
