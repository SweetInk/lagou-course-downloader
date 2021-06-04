package online.githuboy.lagou.course.support;

/**
 * 抽象重试任务
 *
 * @author suchu
 * @date 2021/6/1
 */
public abstract class AbstractRetryTask implements Retryable {
    protected int retryCNT = 0;

    /**
     * 这里写具体的业务逻辑
     */
    protected void action() {
    }

    /**
     * 具体重试逻辑
     *
     * @param throwable 任务执行时抛出的异常
     */
    protected void retry(Throwable throwable) {
        retryCNT++;
    }

    @Override
    public boolean canRetry() {
        return false;
    }

    @Override
    public void retryComplete() {
    }


    @Override
    public void run() {
        try {
            action();
        } catch (Exception e) {
            if (canRetry()) {
                retry(e);
            } else {
                retryComplete();
            }
        }
    }
}
