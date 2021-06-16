package online.githuboy.lagou.course.domain;

import lombok.Data;

import java.util.List;

@Data
public class CourseCommentListInfo {

    private int currentPageNum;
    private boolean hasNextPage;
    private List<CourseCommentList> courseCommentList;
    private int commentCount;

    /**
     * 精选留言列表
     */
    @Data
    public static class CourseCommentList {
        private int commentId;
        private String comment;
        private String likeCount;
        private boolean like;
        private String nickName;
        private String label;
        private boolean expand;
        private boolean showTeacher;
        private int lessonId;
        private String lessonName;
        private boolean owner;
        private boolean top;
        private CourseCommentList replayComment;
    }

}
