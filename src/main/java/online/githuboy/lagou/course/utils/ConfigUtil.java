package online.githuboy.lagou.course.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.Setting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ConfigUtil {
    private final static Setting settings = new Setting("config/config.properties", CharsetUtil.CHARSET_UTF_8, true);

    public static String readValue(String key) {
        return settings.getStr(key, "");
    }

    public static void addDelCourse(String courseId) {
        String removeCourseStr = readValue("remove_course");
        String[] splitStr = StrUtil.split(removeCourseStr, ",");
        Set<String> courseIdSet = Arrays.stream(splitStr).collect(Collectors.toSet());
        courseIdSet.add(courseId);
        String courseIdStr = String.join(",", courseIdSet);
        settings.set("remove_course", courseIdStr);
        settings.store();

    }

    public static void addRetryCourse(String courseId) {
        String courseIds = readValue("courseIds");
        Set<String> courseIdSet = Arrays.stream(StrUtil.split(courseIds, ",")).map(String::trim).collect(Collectors.toSet());
        courseIdSet.add(courseId);
        String courseIdsStr = String.join(",", courseIdSet);
        settings.set("courseIds", courseIdsStr);
        settings.store();
    }

    public static boolean checkDelCourse(String courseId) {
        String remove_course = readValue("remove_course");
        String[] split = StringUtils.split(remove_course, ",");
        Set<String> set = ArrayUtils.isEmpty(split) ? new HashSet<>() : new HashSet<>(Arrays.asList(split));
        return set.stream().map(String::trim).collect(Collectors.toSet()).contains(courseId);
    }

    public static Set<String> getDelCourse() {
        String remove_course = readValue("remove_course");
        String[] split = StringUtils.split(remove_course, ",");
        Set<String> set = ArrayUtils.isEmpty(split) ? new HashSet<>() : new HashSet<>(Arrays.asList(split));
        return set.stream().map(String::trim).collect(Collectors.toSet());
    }

    public static List<String> getCourseIds() {
        String courseIds = readValue("courseIds");
        String[] split = StringUtils.split(courseIds, ",");
        Set<String> set = ArrayUtils.isEmpty(split) ? new HashSet<>() : new HashSet<>(Arrays.asList(split));
        return set.stream().map(String::trim).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        System.out.println(ConfigUtil.readValue("mp4_dir"));
        System.out.println(ConfigUtil.readValue("cookie"));

    }

}
