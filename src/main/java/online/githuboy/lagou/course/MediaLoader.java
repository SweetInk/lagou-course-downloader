package online.githuboy.lagou.course;

import java.util.concurrent.CountDownLatch;

/**
 * @author suchu
 * @date 2020/8/7
 */
public interface MediaLoader extends Runnable {
    default void setLatch(CountDownLatch latch) {
    }

    ;
}
