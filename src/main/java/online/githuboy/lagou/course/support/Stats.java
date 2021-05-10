package online.githuboy.lagou.course.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author suchu
 * @date 2020/8/7
 */
public class Stats {
    static Map<String, AtomicInteger> failedCount = new ConcurrentHashMap<>();

    public static int incr(String key) {
        AtomicInteger integer = failedCount.computeIfAbsent(key, (k) -> new AtomicInteger(0));
        integer.getAndIncrement();
        return integer.get();
    }

    public static void remove(String key) {
        failedCount.remove(key);
    }

    public static boolean isEmpty(){
        return failedCount.isEmpty();
    }
}
