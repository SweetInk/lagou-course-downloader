package online.githuboy.lagou.course.support;

/**
 * @author suchu
 * @date 2021/6/1
 */
public interface Retryable extends Runnable {

    boolean canRetry();

    void retryComplete();

}
