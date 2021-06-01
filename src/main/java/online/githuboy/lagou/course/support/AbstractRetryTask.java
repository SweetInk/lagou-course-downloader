package online.githuboy.lagou.course.support;

/**
 * @author suchu
 * @date 2021/6/1
 */
public abstract class AbstractRetryTask implements Retryable {
    protected int retryCNT = 0;

    protected void action() {
    }

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
