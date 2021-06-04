package online.githuboy.lagou.course.support;

/**
 * 表示是否可重试
 *
 * @author suchu
 * @date 2021/6/1
 */
public interface Retryable extends Runnable {

    /**
     * 是否可已重试
     *
     * @return true 可以重试，false不能重试
     */
    boolean canRetry();

    /**
     * 重试完成
     */
    void retryComplete();

}
