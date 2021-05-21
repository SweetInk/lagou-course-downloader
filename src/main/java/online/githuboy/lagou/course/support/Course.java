package online.githuboy.lagou.course.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import online.githuboy.lagou.course.utils.HttpUtils;
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

//        如果这里获取失败，注意后面这个参数t是怎么回事？可以登录网站获取下
        String allCourseUrl = "https://gate.lagou.com/v1/neirong/kaiwu/getAllCoursePurchasedRecordForPC?t=1620562912369";
        List<String> courseIds = new ArrayList<>();
        String strContent = HttpUtils
                .get(allCourseUrl, CookieStore.getCookie())
                .header("x-l-req-header", " {deviceType:1}")
                .execute().body();

        JSONObject jsonObject = JSONObject.parseObject(strContent);
        Integer state = jsonObject.getInteger("state");
        String message = jsonObject.getString("message");
        if (state != 1) {
            if (state == 1004) {
                String cookie = CookieStore.getCookie();
                throw new RuntimeException("身份认证失败cookie" + cookie);
            } else {
                throw new RuntimeException("获取所有课程信息出错:" + strContent);
            }
        }

        jsonObject = jsonObject.getJSONObject("content");
        JSONArray allCoursePurchasedRecord = jsonObject.getJSONArray("allCoursePurchasedRecord");
        if (allCoursePurchasedRecord != null && allCoursePurchasedRecord.size() > 0) {
            for (int i = 0; i < allCoursePurchasedRecord.size(); i++) {
                JSONObject o = allCoursePurchasedRecord.getJSONObject(i);
                JSONArray courseRecordList = o.getJSONArray("courseRecordList");
                for (int j = 0; j < courseRecordList.size(); j++) {
                    JSONObject jsonObject1 = courseRecordList.getJSONObject(j);
                    String name = jsonObject1.getString("name");
                    String id = jsonObject1.getString("id");
                    String updateProgress = jsonObject1.getString("updateProgress");
                    // 打印所有课程信息，用户依据视频id来下载或者排除
                    log.info("《{}》\t id={}\t {}", name, id, updateProgress);
                    courseIds.add(id);
                }
            }
        }

        log.info("一共有{}门课程", courseIds.size());

        return courseIds;
    }
}
