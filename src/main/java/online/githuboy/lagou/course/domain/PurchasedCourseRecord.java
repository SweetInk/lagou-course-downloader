package online.githuboy.lagou.course.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author suchu
 * @date 2021/6/30
 */
@Data
public class PurchasedCourseRecord {
    /**
     * 训练营课程
     */
    private List<CourseInfo> trainingCamp = new ArrayList<>();
    /**
     * 专栏课程
     */
    private List<CourseInfo> columns = new ArrayList<>();

    @AllArgsConstructor
    @Getter
    public static class CourseInfo {
        /**
         * 课程名称
         */
        private final String name;
        /**
         * 课程ID
         */
        private final Integer id;
    }
}
