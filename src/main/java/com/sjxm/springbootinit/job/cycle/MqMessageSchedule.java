package com.sjxm.springbootinit.job.cycle;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sjxm.springbootinit.model.entity.MqMessage;
import com.sjxm.springbootinit.model.enums.CheckStatusEnum;
import com.sjxm.springbootinit.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 四季夏目
 * @Date: 2025/1/2
 * @Description:
 */
@Component
public class MqMessageSchedule {

    private static final Logger logger = LoggerFactory.getLogger(MqMessageSchedule.class);

    @Resource
    private MqMessageService mqMessageService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final int MAX_RETRY_TIMES = 3;
    private static final long[] RETRY_INTERVALS = {5 * 60 * 1000, 15 * 60 * 1000, 30 * 60 * 1000}; // 5分钟、15分钟、30分钟

    @Scheduled(fixedRate = 300000) // 5分钟执行一次
    @Transactional(rollbackFor = Exception.class)
    public void handleFailedMessages() {
        // 1. 查询失败的消息
        LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MqMessage::getStatus, CheckStatusEnum.FAILED.getCode())
                .le(MqMessage::getRetryTimes, MAX_RETRY_TIMES)  // 未超过最大重试次数
                .le(MqMessage::getNextRetryTime, LocalDateTime.now()); // 到达重试时间

        List<MqMessage> failedMessages = mqMessageService.list(queryWrapper);

        if (CollUtil.isEmpty(failedMessages)) {
            return;
        }

        logger.info("开始处理失败消息，消息数量：{}", failedMessages.size());

        for (MqMessage message : failedMessages) {
            try {
                // 2. 更新消息状态为处理中
                message.setStatus(CheckStatusEnum.PROCESSING.getCode());
                message.setRetryTimes(message.getRetryTimes() + 1);

                // 3. 计算下次重试时间
                long nextInterval = RETRY_INTERVALS[Math.min(message.getRetryTimes() - 1, RETRY_INTERVALS.length - 1)];
                message.setNextRetryTime(LocalDateTime.now().plusSeconds(nextInterval / 1000));

                mqMessageService.updateById(message);

                // 4. 重新发送消息
                Message<String> rocketMessage = MessageBuilder
                        .withPayload(message.getMessageBody())
                        .setHeader(RocketMQHeaders.KEYS, message.getBusinessKey())
                        .build();

                rocketMQTemplate.asyncSend("homework_duplicate_check_topic", rocketMessage, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        // 5. 更新消息发送成功状态
                        message.setStatus(CheckStatusEnum.WAITING.getCode());
                        message.setMsgId(sendResult.getMsgId());
                        mqMessageService.updateById(message);

                        logger.info("失败消息重试发送成功，messageId={}, retryTimes={}",
                                message.getId(), message.getRetryTimes());
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        // 6. 更新消息发送失败状态
                        handleRetryFailure(message, throwable);
                    }
                });

            } catch (Exception e) {
                logger.error("处理失败消息异常，messageId=" + message.getId(), e);
                handleRetryFailure(message, e);
            }
        }
    }

    /**
     * 处理重试失败的情况
     */
    private void handleRetryFailure(MqMessage message, Throwable throwable) {
        try {
            message.setStatus(CheckStatusEnum.FAILED.getCode());
            message.setErrorMsg(throwable.getMessage());

            // 如果超过最大重试次数，标记为死信消息
            if (message.getRetryTimes() >= MAX_RETRY_TIMES) {
                message.setStatus(CheckStatusEnum.DEAD.getCode());
                // 发送告警通知
                sendAlert(message);
            }

            mqMessageService.updateById(message);
        } catch (Exception e) {
            logger.error("更新消息状态失败，messageId=" + message.getId(), e);
        }
    }

    /**
     * 发送告警通知
     */
    private void sendAlert(MqMessage message) {
        String alertMessage = String.format(
                "消息处理失败，已达到最大重试次数\n" +
                        "消息ID: %s\n" +
                        "业务Key: %s\n" +
                        "Topic: %s\n" +
                        "重试次数: %d\n" +
                        "错误信息: %s",
                message.getId(),
                message.getBusinessKey(),
                message.getTopic(),
                message.getRetryTimes(),
                message.getErrorMsg()
        );

        logger.error(alertMessage);
    }

    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void cleanExpiredMessages() {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(MqMessage::getCreateTime, beforeTime);
        mqMessageService.remove(queryWrapper);
    }
}
