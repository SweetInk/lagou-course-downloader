package online.githuboy.lagou.course.support;

import online.githuboy.lagou.course.task.NamedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 线程池工具
 *
 * @author suchu
 * @since 2019年8月3日
 */
public class ExecutorService {

    static final Logger logger = LoggerFactory.getLogger(ExecutorService.class);

    public static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final static TaskNamedThreadPoolExecutor executor = new TaskNamedThreadPoolExecutor(16, 16,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(64),new ThreadPoolExecutor.CallerRunsPolicy());

    private final static TaskNamedThreadPoolExecutor hlsExecutor = new TaskNamedThreadPoolExecutor(16, 16,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public static ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public static ThreadPoolExecutor getHlsExecutor() {
        return hlsExecutor;
    }

    static class TaskNamedThreadPoolExecutor extends ThreadPoolExecutor {
        Map<String, Map<NamedTask, Object>> m1 = new ConcurrentHashMap<>();

        public TaskNamedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        public TaskNamedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        public TaskNamedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        }

        public TaskNamedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (r instanceof NamedTask) {
                Map<NamedTask, Object> runnables = m1.computeIfAbsent(((NamedTask) r).getTaskName(), s -> new HashMap<>());
                runnables.put((NamedTask) r, new Object());
            }
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            if (r instanceof NamedTask) {
                Map<NamedTask, Object> runnables = m1.computeIfAbsent(((NamedTask) r).getTaskName(), s -> new HashMap<>());
                runnables.remove(r);
            }
            super.afterExecute(r, t);
        }

        @Override
        public String toString() {
            synchronized (this) {
                return super.toString() + "\n taskQueueMapping finished: " + COUNTER.get() + "-> \n" + new HashMap<>(m1).entrySet().stream().map(e -> e.getKey() + "__\n\t" + e.getValue().keySet().stream().map(NamedTask::getTaskDescription).collect(Collectors.joining("\n\t", "\t", "\n"))).collect(Collectors.joining("\n"));
            }
        }
    }

    /**
     * 主动退出程序
     *
     * @throws InterruptedException
     */
    public static void tryTerminal() throws InterruptedException {
        logger.info("程序将在{}s后退出", 5);
        ExecutorService.getExecutor().shutdown();
        ExecutorService.getHlsExecutor().shutdown();
        ExecutorService.getHlsExecutor().awaitTermination(5, TimeUnit.SECONDS);
        ExecutorService.getExecutor().awaitTermination(5, TimeUnit.SECONDS);
    }

}
