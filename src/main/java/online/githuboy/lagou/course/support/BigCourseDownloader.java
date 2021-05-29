package online.githuboy.lagou.course.support;

import online.githuboy.lagou.course.constants.ResourceType;
import online.githuboy.lagou.course.constants.RespCode;
import online.githuboy.lagou.course.pojo.dto.BigCourseLessonDto;
import online.githuboy.lagou.course.pojo.vo.CourseDayInfoVo;
import online.githuboy.lagou.course.pojo.vo.CourseStageVo;
import online.githuboy.lagou.course.pojo.vo.LessonInfoVo;
import online.githuboy.lagou.course.pojo.vo.StageModuleVo;
import online.githuboy.lagou.course.task.BigCourseVideoInfoLoader;
import online.githuboy.lagou.course.utils.HttpUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 训练营下载器
 *
 * @author jack
 * @since 2021年5月20日
 */
@Slf4j
public class BigCourseDownloader {

    /**
     * 获取训练营课程大纲
     */
    private final static String COURSE_OUTLINE_API = "https://gate.lagou.com/v1/neirong/edu/bigcourse/getCourseOutline?courseId={0}";

    /**
     * 获取各个Part里面的模块
     */
    private final static String STAGE_WEEKS_API = "https://gate.lagou.com/v1/neirong/edu/bigcourse/getStageWeeks?courseId={0}&stageId={1}";

    /**
     * 获取模块里面的课程
     */
    private final static String WEEK_LESSONS_API = "https://gate.lagou.com/v1/neirong/edu/bigcourse/getWeekLessons?courseId={0}&weekId={1}";

    @Getter
    private final String courseId;

    /**
     * 视频保存根路径
     */
    @Getter
    private final String savePath;

    private final String courseOutlineUrl;

    private CountDownLatch latch;

    private volatile List<MediaLoader> mediaLoaders;

    private long startTime;

    List<CourseStageVo> courseStageVoList;

    // stageId <=> Module
    private Map<Integer, List<StageModuleVo>> stageNModuleVoMap;

    // weekId(ModuleId) => Day(include Lesson)
    private Map<Integer, List<CourseDayInfoVo>> moduleNLessonVoMap;

    public BigCourseDownloader(String courseId, String savePath) {
        this.courseId = courseId;
        this.savePath = savePath;
        this.courseOutlineUrl = MessageFormat.format(COURSE_OUTLINE_API, courseId);

        this.courseStageVoList = new ArrayList<>();
        this.stageNModuleVoMap = new LinkedHashMap<>();
        this.moduleNLessonVoMap = new LinkedHashMap<>();
    }

    public void start() throws IOException, InterruptedException {
        this.startTime = System.currentTimeMillis();

        this.parseCourseOutline();

        this.parseStageModules();

        this.parseWeekLessons();

        List<BigCourseLessonDto> bigCourseLessonDtoList = this.parseBigCourseLessonInfo();

        int videoSize = this.parseVideoInfo(bigCourseLessonDtoList);

        if (videoSize > 0) {
            this.downloadMedia(videoSize);
        }
    }

    private String getRespContent(String apiUrl) {
        return HttpUtils
                .get(apiUrl, CookieStore.getCookie())
                .header("x-l-req-header", " {deviceType:1}")
                .execute()
                .body();
    }

    private void parseCourseOutline() {
        String strContent = this.getRespContent(this.courseOutlineUrl);
        JSONObject jsonRespObject = JSONObject.parseObject(strContent);

        if (jsonRespObject.getInteger("state") != RespCode.SUCCESS) {
            throw new RuntimeException("获取训练营大纲信息出错:" + strContent);
        } else {
            JSONObject jsonContentObject = jsonRespObject.getJSONObject("content");
            JSONArray jsonCourseStageVos = jsonContentObject.getJSONArray("courseStageVos");

            if (jsonCourseStageVos != null && !jsonCourseStageVos.isEmpty()) {
                this.courseStageVoList = jsonCourseStageVos.toJavaList(CourseStageVo.class);
                // filter 2086 (无效Part)
                this.courseStageVoList.removeIf(courseStageVo -> courseStageVo.getStageId() == 2086);
            }
        }
    }

    private void parseStageModules() {
        List<StageModuleVo> stageModuleVoList;
        if (this.courseStageVoList != null && !this.courseStageVoList.isEmpty()) {
            for (CourseStageVo courseStageVo : this.courseStageVoList) {
                String stageWeeksApi = MessageFormat.format(STAGE_WEEKS_API, this.courseId, courseStageVo.getStageId().toString());
                String moduleContent = this.getRespContent(stageWeeksApi);
                JSONObject jsonModuleRespObject = JSONObject.parseObject(moduleContent);
                if (jsonModuleRespObject.getInteger("state") != RespCode.SUCCESS) {
                    throw new RuntimeException("获取训练营模块信息出错:" + moduleContent);
                } else {
                    JSONArray jsonModuleContents = jsonModuleRespObject.getJSONArray("content");
                    if (!jsonModuleContents.isEmpty()) {
                        stageModuleVoList = jsonModuleContents.toJavaList(StageModuleVo.class);
                        this.stageNModuleVoMap.put(courseStageVo.getStageId(), stageModuleVoList);
                    }
                }
            }
        }
    }

    private void parseWeekLessons() {
        List<CourseDayInfoVo> courseDayInfoVoList;

        if (!this.stageNModuleVoMap.isEmpty()) {
            for (List<StageModuleVo> stageModuleVoList : this.stageNModuleVoMap.values()) {
                if (stageModuleVoList != null && !stageModuleVoList.isEmpty()) {
                    for (StageModuleVo stageModuleVo : stageModuleVoList) {
                        String api = MessageFormat.format(WEEK_LESSONS_API, this.courseId, stageModuleVo.getWeekId().toString());
                        String respContent = this.getRespContent(api);

                        JSONObject jsonObject = JSONObject.parseObject(respContent);
                        if (jsonObject.getInteger("state") != RespCode.SUCCESS) {
                            throw new RuntimeException("获取训练营课程信息出错:" + respContent + ", weekId: " + stageModuleVo.getWeekId().toString());
                        } else {
                            JSONObject content = jsonObject.getJSONObject("content");
                            JSONArray jsonCourseDayInfoVos = content.getJSONArray("courseDayInfoVos");
                            if (!jsonCourseDayInfoVos.isEmpty()) {
                                courseDayInfoVoList = JSONArray.parseArray(jsonCourseDayInfoVos.toJSONString(), CourseDayInfoVo.class);
                                this.moduleNLessonVoMap.put(stageModuleVo.getWeekId(), courseDayInfoVoList);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createFilePath(String parentPath, String childPath) {
        File dir = new File(parentPath, childPath);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("视频存放文件夹{}", dir.getAbsolutePath());
        }
    }

    private List<BigCourseLessonDto> parseBigCourseLessonInfo() {
        List<BigCourseLessonDto> bigCourseLessonDtoList = new ArrayList<>();
        if (!this.courseStageVoList.isEmpty()) {
            // 1st Stage
            for (CourseStageVo courseStageVo : this.courseStageVoList) {
                Integer stageId = courseStageVo.getStageId();
                String topPathName = stageId.toString() + "_" + courseStageVo.getStageName();
                this.createFilePath(this.savePath, topPathName);

                // 2nd Module
                List<StageModuleVo> stageModuleVoList = this.stageNModuleVoMap.get(stageId);
                if (!stageModuleVoList.isEmpty()) {
                    for (StageModuleVo stageModuleVo : stageModuleVoList) {
                        Integer weekId = stageModuleVo.getWeekId();
                        String modulePathName = weekId.toString() + "_" + stageModuleVo.getWeekTag() + "_" + stageModuleVo.getWeekName();
                        this.createFilePath(this.savePath + File.separator + topPathName, modulePathName);

                        // 3rd sub module
                        List<CourseDayInfoVo> courseDayInfoVoList = this.moduleNLessonVoMap.get(weekId);
                        if (!courseDayInfoVoList.isEmpty()) {
                            for (CourseDayInfoVo courseDayInfoVo : courseDayInfoVoList) {
                                Integer dayId = courseDayInfoVo.getDayId();
                                String subModulePathName = dayId + "_" + courseDayInfoVo.getDayName();
                                this.createFilePath(this.savePath + File.separator + topPathName + File.separator + modulePathName, subModulePathName);

                                // 4th lesson
                                List<LessonInfoVo> lessonInfoVoList = courseDayInfoVo.getLessonInfoVos();
                                if (!lessonInfoVoList.isEmpty()) {
                                    for (int i = 0; i < lessonInfoVoList.size(); i++) {
                                        LessonInfoVo lessonInfoVo = lessonInfoVoList.get(i);
                                        if (ResourceType.MEDIA.equals(lessonInfoVo.getType()) || ResourceType.RESOURCE.equals(lessonInfoVo.getType())) {
                                            String lessonParentPath = this.savePath + File.separator + topPathName + File.separator + modulePathName + File.separator + subModulePathName;
                                            String videoName = (i + 1) + "_" + lessonInfoVo.getLessonId() + "_" + lessonInfoVo.getLessonName();
                                            bigCourseLessonDtoList.add(new BigCourseLessonDto(this.courseId, stageId.toString(), weekId.toString(), dayId.toString(), lessonInfoVo.getLessonId().toString(), videoName, lessonParentPath, lessonInfoVo.getType(), lessonInfoVo.getResourceUrl()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return bigCourseLessonDtoList;
    }

    private int parseVideoInfo(List<BigCourseLessonDto> bigCourseLessonDtoList) {
        AtomicInteger videoSize = new AtomicInteger();
        this.latch = new CountDownLatch(bigCourseLessonDtoList.size());
        this.mediaLoaders = new Vector<>();

        bigCourseLessonDtoList.forEach(lessonInfo -> {
            if (!Mp4History.contains(lessonInfo.getLessonId())) {
                BigCourseVideoInfoLoader videoInfoLoader = new BigCourseVideoInfoLoader(lessonInfo.getVideoName(), lessonInfo.getCourseId(), lessonInfo.getWeekId(), lessonInfo.getDayId(), lessonInfo.getLessonId());
                videoInfoLoader.setMediaLoaders(mediaLoaders);
                videoInfoLoader.setBasePath(lessonInfo.getPathName());
                videoInfoLoader.setLatch(this.latch);
                videoInfoLoader.setType(lessonInfo.getType());
                videoInfoLoader.setResourceUrl(lessonInfo.getResourceUrl());
                ExecutorService.execute(videoInfoLoader);
                videoSize.getAndIncrement();
            } else {
                log.info("课程【{}】已经下载过了", lessonInfo.getVideoName());
                latch.countDown();
                ExecutorService.COUNTER.incrementAndGet();
            }
        });
        return videoSize.intValue();
    }

    /**
     * @param i 需要下载的数量
     */
    private void downloadMedia(int i) throws InterruptedException {
        log.info("等待获取媒体信息任务完成...");
        System.out.println(ExecutorService.COUNTER);
        BlockingQueue<Runnable> queue = ExecutorService.getExecutor().getQueue();
        System.out.println(queue.size());
        this.latch.await();
        if (this.mediaLoaders.size() != i) {
            log.info("媒体META信息没有全部下载成功: success:{},total:{}", this.mediaLoaders.size(), i);
            ExecutorService.tryTerminal();
            return;
        }
        log.info("所有媒体META信息获取成功 total：{}", this.mediaLoaders.size());
        CountDownLatch all = new CountDownLatch(this.mediaLoaders.size());

        for (MediaLoader loader : this.mediaLoaders) {
            loader.setLatch(all);
            ExecutorService.getExecutor().execute(loader);
        }
        all.await();
        long end = System.currentTimeMillis();
        log.info("所有媒体处理耗时:{} s", (end - startTime) / 1000);
        log.info("媒体输出目录:{}", this.savePath);
        File file = new File(this.savePath, "下载完成.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!Stats.isEmpty()) {
            log.info("\n\n失败统计信息\n\n");
            Stats.failedCount.forEach((key, value) -> System.out.println(key + " -> " + value.get()));
        }
//        tryTerminal();
    }


}
