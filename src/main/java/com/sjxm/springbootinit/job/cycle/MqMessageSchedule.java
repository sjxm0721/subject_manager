//package com.sjxm.springbootinit.job.cycle;
//
//import cn.hutool.core.collection.CollUtil;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.sjxm.springbootinit.model.dto.task.RetryTask;
//import com.sjxm.springbootinit.model.entity.MqMessage;
//import com.sjxm.springbootinit.model.enums.CheckStatusEnum;
//import com.sjxm.springbootinit.service.MqMessageService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.client.producer.SendCallback;
//import org.apache.rocketmq.client.producer.SendResult;
//import org.apache.rocketmq.spring.core.RocketMQTemplate;
//import org.apache.rocketmq.spring.support.RocketMQHeaders;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.Resource;
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.List;
//
///**
// * @Author: 四季夏目
// * @Date: 2025/1/2
// * @Description:
// */
//@Component
//public class MqMessageSchedule {
//
//    private static final Logger logger = LoggerFactory.getLogger(MqMessageSchedule.class);
//
//    @Resource
//    private MqMessageService mqMessageService;
//
//
//
//}
