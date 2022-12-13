package delaytask;

import jodd.util.concurrent.ThreadFactoryBuilder;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author ALVIN
 */
@Configuration
public class DelayTaskAutoConfiguration {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private Set<DelayTaskSubscriber> subscriberSet;

    public static final Logger LOGGER = LoggerFactory.getLogger(DelayTaskAutoConfiguration.class);

    /**
     * 创建默认的延迟任务处理线程池
     * @return 线程池
     */
    @Bean(name = "delayTaskExecutor")
    public Executor executor() {
        ThreadFactoryBuilder threadFactoryBuilder = ThreadFactoryBuilder.create();
        threadFactoryBuilder.setDaemon(true);
        threadFactoryBuilder.setNameFormat("延迟任务处理线程-%d");
        threadFactoryBuilder.setPriority(Thread.NORM_PRIORITY);
        threadFactoryBuilder.setUncaughtExceptionHandler((thread, exception) -> {
            LOGGER.error("延迟任务处理线程[{}]出现异常，原因：{}", thread.getName(), exception);
        });

        ThreadFactory threadFactory = threadFactoryBuilder.get();
        ThreadPoolExecutor.CallerRunsPolicy callerRunsPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
        return new ThreadPoolExecutor(10, 30, 30,
                TimeUnit.MINUTES, new LinkedBlockingQueue<>(2000), threadFactory, callerRunsPolicy);
    }

    /**
     * 创建延迟任务通道
     * @return 延迟任务通道
     */
    @Bean
    public DelayTaskChannel delayTaskChannel() {
        DefaultDelayTaskChannel delayTaskChannel = new DefaultDelayTaskChannel(redisTemplate, redissonClient, executor(), subscriberSet);
        Executors.newSingleThreadExecutor().submit(delayTaskChannel.new EventDispatcher());
        return delayTaskChannel;
    }
}
