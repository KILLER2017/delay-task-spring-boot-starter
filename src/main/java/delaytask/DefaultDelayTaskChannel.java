package delaytask;


import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author ALVIN
 */

public class DefaultDelayTaskChannel implements DelayTaskChannel {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultDelayTaskChannel.class);

    private final static String KEY = "delayTask";

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final Executor executor;

    private final Set<DelayTaskSubscriber> subscribers;

    public DefaultDelayTaskChannel(RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient, Executor executor,
                                   Set<DelayTaskSubscriber> subscribers) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.executor = executor;
        this.subscribers = subscribers;
    }

    @Override
    public void publish(DelayTask task, long wakeTime) {
        RLock taskLock = redissonClient.getLock(KEY + ":dispatcher:lock:" + task.hashCode());
        try {
            taskLock.lock();
            // 用于任务排序
            redisTemplate.opsForZSet().add(KEY, task, wakeTime);
            // 用于双重校验
            redisTemplate.opsForValue().set(KEY + ":" + task, wakeTime);
        } finally {
            taskLock.unlock();
        }
    }

    @Override
    public void withdraw(DelayTask task) {
        RLock taskLock = redissonClient.getLock(KEY + ":dispatcher:lock:" + task.hashCode());
        try {
            redisTemplate.opsForZSet().remove(KEY, task);
            redisTemplate.delete(KEY + ":" + task);
        } finally {
            taskLock.unlock();
        }
    }

    public final class TaskDispatcher implements Runnable {

        @Override
        public void run() {
            try {
                Set<Object> delayTasks = redisTemplate.opsForZSet().rangeByScore(KEY, 0L, System.currentTimeMillis());
                if (delayTasks != null) {
                    for (Object task : delayTasks)
                    {
                        Assert.notNull(task, "任务出现NULL");
                        DelayTask delayEvent = (DelayTask) task;
                        RLock taskLock = redissonClient.getLock(KEY + ":dispatcher:lock:" + task.hashCode());
                        try {
                            // 尝试获取任务锁，若获取锁失败，则取下一个任务
                            if (!taskLock.tryLock()) {
                                continue;
                            }

                            Long wakeTime = (Long) redisTemplate.opsForValue().get(KEY + ":" + task);
                            if (wakeTime == null || System.currentTimeMillis() < wakeTime) {
                                continue;
                            }

                            for (DelayTaskSubscriber subscriber : subscribers) {
                                if (subscriber.isSupport(delayEvent)) {
                                    // 开启新线程处理事件
                                    CompletableFuture.supplyAsync(() -> subscriber.handleTask(delayEvent), executor).whenComplete((result, throwable) -> {
                                        if (throwable != null || DelayTaskHandleResult.FAILED.equals(result)) {
                                            LOGGER.error("延迟任务处理异常，任务：{}，异常：{}", delayEvent, throwable);
                                            Long retry = redisTemplate.opsForValue().increment(KEY + ":retry:" + delayEvent);
                                            redisTemplate.expire(KEY + ":retry:" + delayEvent, 30, TimeUnit.MINUTES);
                                            if (retry != null && retry > 5) {
                                                redisTemplate.opsForZSet().add(KEY + ":failedTask", delayEvent, System.currentTimeMillis());
                                            } else if (retry != null) {
                                                publish(delayEvent, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(retry));
                                            }
                                        } else if (DelayTaskHandleResult.SUCCESS.equals(result)) {
                                            withdraw(delayEvent);
                                        }
                                    });
                                }
                            }
                        } finally {
                            if (taskLock.isHeldByCurrentThread()) {
                                taskLock.unlock();
                            }
                        }

                    }
                }
            } catch (Exception e) {
                LOGGER.error("延迟任务分发器异常：{}，{}", e.getMessage(), e.toString());
            }
        }
    }
}
