package com.sjxm.springbootinit.model.dto.task;

import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/30
 * @Description:
 */
@Data
public class RetryTask implements Delayed {
    private Long homeworkId;
    private Long messageId;
    private long startTime = System.currentTimeMillis();  // 添加 startTime
    private int retryCount = 0;
    private static final long RETRY_INTERVAL = 60000; // 基础重试间隔（5秒）
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数

    public RetryTask(Long homeworkId, Long messageId) {
        this.homeworkId = homeworkId;
        this.messageId = messageId;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = startTime + RETRY_INTERVAL - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this) {
            return 0;
        }
        long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
        return Long.compare(diff, 0);
    }

    public boolean canRetry() {
        return retryCount < MAX_RETRY_COUNT;
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.startTime = System.currentTimeMillis();
    }
}