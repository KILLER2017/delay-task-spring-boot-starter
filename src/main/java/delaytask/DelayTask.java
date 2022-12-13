package delaytask;

/**
 * @author ALVIN
 */
public interface DelayTask {

    /**
     * 获取延迟任务类型
     * @return 任务类型
     */
    String getTaskType();

    /**
     * 同一个任务的toString结果要确保相同
     * @return 延迟任务的字符串表示
     */
    @Override
    String toString();

    /**
     * 同一个任务的hashCode结果要确保相同
     * @return 哈希值
     */
    @Override
    int hashCode();
}
