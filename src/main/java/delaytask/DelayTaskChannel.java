package delaytask;

/**
 * 延迟任务通道
 * @author ALVIN
 */
public interface DelayTaskChannel {

    /**
     * 发布延迟任务
     * @param task 延迟任务
     * @param wakeTime 期望任务开始处理的时间（时间戳）
     */
    void publish(DelayTask task, long wakeTime);

    /**
     * 撤销任务
     * @param task 延迟任务
     */
    void withdraw(DelayTask task);
}
