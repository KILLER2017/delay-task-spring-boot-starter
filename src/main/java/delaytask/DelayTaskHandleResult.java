package delaytask;

/**
 * @author ALVIN
 */

public enum DelayTaskHandleResult {
    /**
     * 处理成功
     * <p>会自动从任务集合中移除该任务</p>
     */
    SUCCESS,

    /**
     * 处理失败
     * <p>会自动延期重新尝试执行任务，最多尝试5次，5次失败后放入失败任务集合</p>
     */
    FAILED,

    /**
     * 推迟处理
     * <P>必须手动重新发布任务以推迟处理，新发布的任务会覆盖旧的任务</P>
     * <p>没有重新发布会让任务立即被重新执行</p>
     */
    DELAY,
}
