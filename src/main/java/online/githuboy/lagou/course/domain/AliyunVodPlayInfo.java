package online.githuboy.lagou.course.domain;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * AliyunVOD播放信息
 *
 * @author suchu
 * @date 2021/6/1
 */
@Data
public class AliyunVodPlayInfo {
    /**
     * 是否加密:1
     */
    @JSONField(name = "Encrypt")
    private Integer encrypt;
    /**
     * 加密类型:AliyunVoDEncryption
     */
    @JSONField(name = "EncryptType")
    private String encryptType;
    /**
     * 播放URL
     */
    @JSONField(name = "PlayURL")
    private String playURL;
    /**
     * 私有加密rand
     */
    @JSONField(name = "Rand")
    private String rand;
    /**
     * 私有加密plainText
     */
    @JSONField(name = "Plaintext")
    private String plaintext;
    /**
     * 媒体格式 eg：MP4,M3U8
     */
    @JSONField(name = "Format")
    private String format;
}
