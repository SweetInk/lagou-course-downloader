package online.githuboy.lagou.course.domain;

import lombok.Data;

import java.util.List;

/**
 * 课程信息
 *
 * @author suchu
 * @date 2021/6/4
 */
@Data
public class CourseInfo {
    private Boolean hasBuy = false;
    private String courseName;
    private String coverImage;
    private Integer videoChannelCode;
    private List<Section> courseSectionList;

    /**
     * 章节信息
     */
    @Data
    public static class Section {
        private Integer courseId;
        private String description;
        private Integer id;
        private String sectionName;
        private Integer sectionSortNum;
        private Boolean visible;
        private List<Lesson> courseLessons;
    }

    /**
     * 具体课程信息
     */
    @Data
    public static class Lesson {
        private Integer id;
        private String appId;
        private Boolean canPlay;
        private Boolean hasVideo;
        private Integer sectionId;
        private String status;
        private String textContent;
        private String textUrl;
        private String theme;
        private VideoMedia videoMediaDTO;
    }

    @Data
    public static class VideoMedia {
        private Integer channel;
        private String fileId;
        private String encryptedFileId;//
        private String fileUrl;
        private String fileEdk;
    }
}
