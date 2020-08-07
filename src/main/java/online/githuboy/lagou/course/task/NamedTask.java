package online.githuboy.lagou.course.task;

/**
 * 用于调试
 *
 * @author suchu
 * @date 2020/8/6
 */
public interface NamedTask {
    /**
     * Task name
     *
     * @return
     */
    default String getTaskName() {
        return this.getClass().getSimpleName();
    }

    /**
     * More task info
     *
     * @return
     */
    default String getTaskDescription() {
        return null;
    }
}
