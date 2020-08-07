package online.githuboy.lagou.course;

import online.githuboy.lagou.course.task.NamedTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 线程池工具
 *
 * @author suchu
 * @since 2019年8月3日
 */
public class ExecutorService {
    private final static TaskNamedThreadPoolExecutor executor = new TaskNamedThreadPoolExecutor(16, 16,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

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
            return super.toString() + "\n taskQueueMapping -> \n" + m1.entrySet().stream().map(e -> e.getKey() + "__\n\t" + e.getValue().keySet().stream().map(NamedTask::getTaskDescription).collect(Collectors.joining("\n\t", "\t", "\n"))).collect(Collectors.joining("\n"));
        }
    }
}
