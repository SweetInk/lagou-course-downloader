package online.githuboy.lagou.course.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import online.githuboy.lagou.course.utils.HttpUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author eric
 */
public class Course {

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
        if (jsonObject.getInteger("state") != 1) {
            throw new RuntimeException("获取所有课程信息出错:" + strContent);
        }

        jsonObject = jsonObject.getJSONObject("content");
        JSONArray allCoursePurchasedRecord = jsonObject.getJSONArray("allCoursePurchasedRecord");
        if (allCoursePurchasedRecord != null && allCoursePurchasedRecord.size() > 0) {
            for (int i = 0; i < allCoursePurchasedRecord.size(); i++) {
                JSONObject o = allCoursePurchasedRecord.getJSONObject(i);
                JSONArray courseRecordList = o.getJSONArray("courseRecordList");
                for (int j = 0; j < courseRecordList.size(); j++) {
                    String id = courseRecordList.getJSONObject(j).getString("id");
                    courseIds.add(id);
                }
            }
        }

        return courseIds;
    }
}
