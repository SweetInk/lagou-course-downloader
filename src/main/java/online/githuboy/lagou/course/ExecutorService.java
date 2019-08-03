package online.githuboy.lagou.course;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工具
 *
 * @author suchu
 * @since 2019年8月3日
 */
public class ExecutorService {
    private final static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public static ThreadPoolExecutor getExecutor() {
        return executor;
    }
}
