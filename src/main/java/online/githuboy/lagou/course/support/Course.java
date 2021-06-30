package online.githuboy.lagou.course.support;

import online.githuboy.lagou.course.domain.PurchasedCourseRecord;
import online.githuboy.lagou.course.request.HttpAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author eric
 * @date 2021/05/09
 */
public class Course {
    private static final Logger log = LoggerFactory.getLogger(Course.class);

    /**
     * @return 当前账户下的所有课程的id
     */
    public static List<String> getAllCoursePurchasedRecordForPC() {
        log.info("查询已购课程信息");
        PurchasedCourseRecord purchasedCourseRecord = HttpAPI.getPurchasedCourseRecord();
        log.info("训练营课程:");
        purchasedCourseRecord.getTrainingCamp().forEach(courseInfo -> {
            log.info("[{}] {}", courseInfo.getId(), courseInfo.getName());
        });
        log.info("专栏课程:");
        //这里只处理专栏课程
        List<String> courseIdSets = new ArrayList<>(purchasedCourseRecord.getTrainingCamp().size());
        purchasedCourseRecord.getColumns().forEach(courseInfo -> {
            courseIdSets.add(courseInfo.getId() + "");

            log.info("[{}] {}", courseInfo.getId(), courseInfo.getName());
        });
        log.info("一共有{}门课程", courseIdSets.size());
        return courseIdSets;
    }
}
