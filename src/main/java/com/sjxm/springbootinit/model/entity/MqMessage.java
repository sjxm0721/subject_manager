package com.sjxm.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 
 * @TableName mq_message
 */
@TableName(value ="mq_message")
@Data
public class MqMessage {
    @TableId
    private Long id;

    private String msgId;

    private String businessKey;  // 业务关键字，如 "homework_123"

    private String topic;  // 消息主题

    private String messageBody;  // 消息内容

    private Integer status;  // 消息状态

    private Integer retryTimes;  // 重试次数

    private Date createTime;

    private Date updateTime;

    private LocalDateTime nextRetryTime;  // 下次重试时间

    private String errorMsg;  // 错误信息
}