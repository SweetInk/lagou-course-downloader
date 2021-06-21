package online.githuboy.lagou.course.support;

import lombok.Setter;

import java.util.concurrent.CountDownLatch;

/**
 * @author suchu
 * @date 2020/8/7
 */
public interface MediaLoader extends Runnable {
    /**
     * 多线程并发处理
     *
     * @param latch
     */
    default void setLatch(CountDownLatch latch) {
    }

    class EmptyMediaLoader implements MediaLoader {
        @Setter
        private CountDownLatch latch;

        @Override
        public void run() {
            latch.countDown();
        }

    }

}
