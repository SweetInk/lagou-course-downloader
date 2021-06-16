package online.githuboy.lagou.course.utils;

import cn.hutool.core.collection.ListUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ConfigUtil {

    public static String readValue(String key){
        Properties properties = new Properties();
        InputStream inputStream = ConfigUtil.class.getClassLoader().getResourceAsStream("config/config.properties");
        try (InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
             BufferedReader br = new BufferedReader(isr);
        ){
            // 一定一定要 load 设置编码后的字符流
            properties.load(br);
            return properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
        return "";
    }

    public static void addDelCourse(String courseId) {
        Properties properties = new Properties();
        InputStream inputStream = ConfigUtil.class.getClassLoader().getResourceAsStream("config/config.properties");
        try (InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
             BufferedReader br = new BufferedReader(isr);
        ){
            // 一定一定要 load 设置编码后的字符流
            properties.load(br);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String properties1 = properties.getProperty("remove_course");
        String[] split = StringUtils.split(properties1, ",");
        Set<String> set = ArrayUtils.isEmpty(split) ? new HashSet<>() : new HashSet<>(Arrays.asList(split));
        set.add(String.valueOf(courseId));
        String join = String.join(",", set.stream().map(String::trim).collect(Collectors.toSet()));
        properties.setProperty("remove_course", join);

        URL resource = ConfigUtil.class.getClassLoader().getResource("config/config.properties");
        try (FileOutputStream fos = new FileOutputStream(new File(resource.toURI()))){
            properties.store(fos,"danciben");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
        }
    }

    public static void addRetryCourse(String courseId) {
        Properties properties = new Properties();
        InputStream inputStream = ConfigUtil.class.getClassLoader().getResourceAsStream("config/config.properties");
        try (InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
             BufferedReader br = new BufferedReader(isr);
        ){
            // 一定一定要 load 设置编码后的字符流
            properties.load(br);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String properties1 = properties.getProperty("courseIds");
        String[] split = StringUtils.split(properties1, ",");
        Set<String> set = ArrayUtils.isEmpty(split) ? new HashSet<>() : new HashSet<>(Arrays.asList(split));
        set.add(String.valueOf(courseId));
        String join = set.stream().map(String::trim).collect(Collectors.joining(","));
        properties.setProperty("courseIds", join);

        URL resource = ConfigUtil.class.getClassLoader().getResource("config/config.properties");
        try (FileOutputStream fos = new FileOutputStream(new File(resource.toURI()))){
            properties.store(fos,"danciben");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
        }
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
    }

}
