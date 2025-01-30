package com.sjxm.springbootinit.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;



    private static final Logger logger = LoggerFactory.getLogger(RocketMQConfig.class);


    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();

        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setProducerGroup("homework_producer_group");
        producer.setNamesrvAddr(nameServer);
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
        mqAdminExt.setNamesrvAddr(nameServer);
        mqAdminExt.start();

        // 创建topic
        try {
            TopicConfig topicConfig = new TopicConfig();
            topicConfig.setTopicName("homework_duplicate_check_topic");
            topicConfig.setReadQueueNums(4);
            topicConfig.setWriteQueueNums(4);

            mqAdminExt.createAndUpdateTopicConfig("localhost:10911", topicConfig);
            logger.info("Topic创建成功");
        } catch (Exception e) {
            logger.error("创建Topic失败", e);
        }

        return mqAdminExt;
    }
}