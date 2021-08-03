package online.githuboy.lagou.course.support;

import online.githuboy.lagou.course.domain.PurchasedCourseRecord;
import online.githuboy.lagou.course.request.HttpAPI;
import online.githuboy.lagou.course.utils.ConfigUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /**
     *  获取指定类别的课程id
     * @param classifyIds 课程类别集合
     * @return
     */
	public static Set<String> getAllCourseVipForPC(List<String> classifyIds) {
		Set<String> set = new HashSet<String>();
		Map<String, List<String>> courses = HttpAPI.listCourse();

		if (classifyIds.size() == 1 && classifyIds.get(0).equals("0")) {
			for (List<String> list : courses.values()) {
				set.addAll(list);
			}
		} else {
			for (String classify : classifyIds) {
				set.addAll(courses.get(classify));
			}
		}

		return set;
	}
    /**
     *  vip自动订阅指定类别的课程
     * @return   成功订阅的课程id
     */
    public static Set<String> drawCourse() {
    	
    	List<String> allCoursePurchasedRecordForPC = Course.getAllCoursePurchasedRecordForPC(); 
    	// 所有指定订阅的课程
    	List<String> classifyIds = ConfigUtil.getClassifyIds(); 
		Set<String> courseIds = Course.getAllCourseVipForPC(classifyIds); 
		courseIds.removeAll(allCoursePurchasedRecordForPC) ; 
		for (String courseId : courseIds) {
			HttpAPI.drawCourse(courseId);
		}
		return courseIds ; 
    }
}
