package delaytask;

/**
 * 延迟任务订阅处理器
 * @author ALVIN
 */
public interface DelayTaskSubscriber {
    /**
     * 是否支持处理指定任务
     * @param task 延迟任务
     * @return 是否支持
     */
    boolean isSupport(DelayTask task);

    /**
     * 任务处理逻辑
     * @param task 延迟任务
     * @return 是否处理成功
     */
    DelayTaskHandleResult handleTask(DelayTask task);
}
