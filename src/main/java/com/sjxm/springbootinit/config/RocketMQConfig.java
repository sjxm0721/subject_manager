package com.sjxm.springbootinit.config;

import io.lettuce.core.dynamic.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RocketMQConfig {


    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();

        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setProducerGroup("homework_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        producer.setVipChannelEnabled(false);
        producer.setRetryTimesWhenSendAsyncFailed(2);
        producer.setSendMsgTimeout(3000);

        rocketMQTemplate.setProducer(producer);
        return rocketMQTemplate;
    }

    // 如果需要创建Topic，使用单独的Bean
    @Bean
    public DefaultMQAdminExt mqAdminExt() throws MQClientException {
        DefaultMQAdminExt mqAdminExt = new DefaultMQAdminExt();
        mqAdminExt.setNamesrvAddr("localhost:9876");
        mqAdminExt.start();

        // 创建topic
        try {
            TopicConfig topicConfig = new TopicConfig();
            topicConfig.setTopicName("homework_duplicate_check_topic");
            topicConfig.setReadQueueNums(4);
            topicConfig.setWriteQueueNums(4);

            mqAdminExt.createAndUpdateTopicConfig("localhost:10911", topicConfig);
            log.info("Topic创建成功");
        } catch (Exception e) {
            log.error("创建Topic失败", e);
        }

        return mqAdminExt;
    }
}